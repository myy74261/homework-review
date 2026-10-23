param(
    [string]$Path = (Join-Path $PSScriptRoot '.env')
)

if (-not (Test-Path -LiteralPath $Path)) {
    return
}

foreach ($line in Get-Content -LiteralPath $Path -Encoding UTF8) {
    $trimmed = $line.Trim()
    if (-not $trimmed -or $trimmed.StartsWith('#')) {
        continue
    }

    $separator = $trimmed.IndexOf('=')
    if ($separator -lt 1) {
        continue
    }

    $name = $trimmed.Substring(0, $separator).Trim()
    $value = $trimmed.Substring($separator + 1).Trim()
    if ($name -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') {
        continue
    }

    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
}
