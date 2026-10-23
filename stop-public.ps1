$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSCommandPath
$runDirectory = Join-Path $projectRoot '.run'
$appPidFile = Join-Path $runDirectory 'app.pid'
$tunnelPidFile = Join-Path $runDirectory 'tunnel.pid'

function Stop-ProcessFromPidFile {
    param(
        [string]$PidFile,
        [string[]]$ExpectedNames
    )

    if (-not (Test-Path -LiteralPath $PidFile)) {
        return
    }

    $processId = [int](Get-Content -LiteralPath $PidFile -Raw).Trim()
    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    if ($process -and $ExpectedNames -contains $process.ProcessName) {
        Stop-Process -Id $processId -Force
        Write-Host "Stopped $($process.ProcessName) PID $processId"
    }
    Remove-Item -LiteralPath $PidFile -Force
}

Stop-ProcessFromPidFile -PidFile $tunnelPidFile -ExpectedNames @('cloudflared')
Stop-ProcessFromPidFile -PidFile $appPidFile -ExpectedNames @('java', 'javaw')

$urlFile = Join-Path $runDirectory 'public-url.txt'
if (Test-Path -LiteralPath $urlFile) {
    Remove-Item -LiteralPath $urlFile -Force
}

Write-Host 'Public preview stopped.'
