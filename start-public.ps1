$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSCommandPath

. (Join-Path $projectRoot 'load-env.ps1')

$serverPort = if ($env:SERVER_PORT) { [int]$env:SERVER_PORT } else { 8080 }
$runDirectory = Join-Path $projectRoot '.run'
$toolsDirectory = Join-Path $projectRoot '.tools'
$jar = Join-Path $projectRoot 'target\review-desk-0.1.0-SNAPSHOT.jar'
$java = 'D:\Project\jdk\bin\java.exe'
$maven = 'D:\Project\apache-maven-3.10.0\bin\mvn.cmd'
$cloudflared = Join-Path $toolsDirectory 'cloudflared.exe'
$appPidFile = Join-Path $runDirectory 'app.pid'
$tunnelPidFile = Join-Path $runDirectory 'tunnel.pid'
$appLog = Join-Path $runDirectory 'app.log'
$appErrorLog = Join-Path $runDirectory 'app-error.log'
$tunnelLog = Join-Path $runDirectory 'tunnel.log'
$tunnelErrorLog = Join-Path $runDirectory 'tunnel-error.log'
$urlFile = Join-Path $runDirectory 'public-url.txt'

New-Item -ItemType Directory -Path $runDirectory -Force | Out-Null
New-Item -ItemType Directory -Path $toolsDirectory -Force | Out-Null

if (-not (Test-Path -LiteralPath $java)) {
    throw "Java was not found: $java"
}

if (-not (Test-Path -LiteralPath $jar)) {
    if (-not (Test-Path -LiteralPath $maven)) {
        throw "Maven was not found and the application is not packaged: $maven"
    }
    Write-Host 'Packaging application...'
    & $maven -o "-Dmaven.repo.local=$projectRoot\.maven-repo" -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        throw 'Application packaging failed.'
    }
}

if (-not (Test-Path -LiteralPath $cloudflared)) {
    $downloaded = Join-Path $projectRoot 'target\cloudflared.exe'
    if (Test-Path -LiteralPath $downloaded) {
        Copy-Item -LiteralPath $downloaded -Destination $cloudflared -Force
    } else {
        Write-Host 'Downloading Cloudflare Tunnel client...'
        Invoke-WebRequest -UseBasicParsing `
            -Uri 'https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe' `
            -OutFile $cloudflared
    }
}

function Test-LocalApp {
    try {
        $response = Invoke-WebRequest -UseBasicParsing `
            -Uri "http://127.0.0.1:$serverPort/login" -TimeoutSec 2
        return $response.StatusCode -eq 200
    } catch {
        return $false
    }
}

if (-not (Test-LocalApp)) {
    $listener = Get-NetTCPConnection -LocalPort $serverPort -State Listen -ErrorAction SilentlyContinue
    if ($listener) {
        throw "Port $serverPort is used by PID $($listener[0].OwningProcess), but the app is not responding."
    }

    Write-Host 'Starting application in the background...'
    $appProcess = Start-Process `
        -FilePath $java `
        -ArgumentList "-jar `"$jar`"" `
        -WorkingDirectory $projectRoot `
        -WindowStyle Hidden `
        -RedirectStandardOutput $appLog `
        -RedirectStandardError $appErrorLog `
        -PassThru
    Set-Content -LiteralPath $appPidFile -Value $appProcess.Id

    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        Start-Sleep -Milliseconds 500
        if (Test-LocalApp) {
            $ready = $true
            break
        }
        if ($appProcess.HasExited) {
            break
        }
    }
    if (-not $ready) {
        throw "Application startup failed. Check $appLog and $appErrorLog"
    }
} else {
    Write-Host 'The local application is already running. Creating a public tunnel...'
}

if ((Test-Path -LiteralPath $tunnelPidFile) -and (Test-Path -LiteralPath $urlFile)) {
    $existingTunnelId = [int](Get-Content -LiteralPath $tunnelPidFile -Raw).Trim()
    $existingTunnel = Get-Process -Id $existingTunnelId -ErrorAction SilentlyContinue
    if ($existingTunnel -and $existingTunnel.ProcessName -eq 'cloudflared') {
        $existingUrl = (Get-Content -LiteralPath $urlFile -Raw).Trim()
        Write-Host 'The public preview is already running:'
        Write-Host $existingUrl
        try {
            Set-Clipboard -Value $existingUrl
        } catch {
            # The URL is still printed when clipboard access is unavailable.
        }
        Start-Process $existingUrl
        exit 0
    }
}

Write-Host 'Creating temporary HTTPS public URL...'
$tunnelProcess = Start-Process `
    -FilePath $cloudflared `
    -ArgumentList @('tunnel', '--url', "http://127.0.0.1:$serverPort", '--no-autoupdate') `
    -WorkingDirectory $projectRoot `
    -WindowStyle Hidden `
    -RedirectStandardOutput $tunnelLog `
    -RedirectStandardError $tunnelErrorLog `
    -PassThru
Set-Content -LiteralPath $tunnelPidFile -Value $tunnelProcess.Id

$publicUrl = $null
for ($attempt = 0; $attempt -lt 60; $attempt++) {
    Start-Sleep -Milliseconds 500
    $logParts = @(
        Get-Content -LiteralPath $tunnelLog -Raw -ErrorAction SilentlyContinue
        Get-Content -LiteralPath $tunnelErrorLog -Raw -ErrorAction SilentlyContinue
    ) | Where-Object { $_ }
    if ($logParts) {
        $logText = $logParts -join "`n"
        $match = [regex]::Match($logText, 'https://[a-z0-9-]+\.trycloudflare\.com')
        if ($match.Success) {
            $publicUrl = $match.Value
            break
        }
    }
    if ($tunnelProcess.HasExited) {
        break
    }
}

if (-not $publicUrl) {
    throw "Tunnel creation failed. Check $tunnelLog and $tunnelErrorLog"
}

Set-Content -LiteralPath $urlFile -Value $publicUrl
try {
    Set-Clipboard -Value $publicUrl
} catch {
    # The URL is still printed when clipboard access is unavailable.
}

Write-Host ''
Write-Host 'Public URL:'
Write-Host $publicUrl
Write-Host 'The URL was copied to the clipboard when possible.'

Start-Process $publicUrl
