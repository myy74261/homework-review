$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSCommandPath
$jar = Join-Path $projectRoot 'target\review-desk-0.1.0-SNAPSHOT.jar'
$java = 'D:\Project\jdk\bin\java.exe'
$maven = 'D:\Project\apache-maven-3.10.0\bin\mvn.cmd'

. (Join-Path $projectRoot 'load-env.ps1')

if (-not (Test-Path -LiteralPath $java)) {
    throw "Java was not found: $java"
}

if (-not (Test-Path -LiteralPath $jar)) {
    if (-not (Test-Path -LiteralPath $maven)) {
        throw "Maven was not found and the application is not packaged: $maven"
    }
    & $maven -o "-Dmaven.repo.local=$projectRoot\.maven-repo" -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        throw 'Application packaging failed.'
    }
}

$serverPort = if ($env:SERVER_PORT) { [int]$env:SERVER_PORT } else { 8080 }
Write-Host "Starting ReviewDesk at http://localhost:$serverPort"
& $java -jar $jar
