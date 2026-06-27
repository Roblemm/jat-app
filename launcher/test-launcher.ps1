$ErrorActionPreference = "Stop"

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$powerShellLauncher = Join-Path $PSScriptRoot "start-jat.ps1"
$cmdLauncher = Join-Path $PSScriptRoot "Start Jat.cmd"

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Assert-Contains {
    param(
        [string]$Text,
        [string]$Expected,
        [string]$Message
    )

    Assert-True -Condition $Text.Contains($Expected) -Message $Message
}

Assert-True -Condition (Test-Path $powerShellLauncher) -Message "PowerShell launcher is missing."
Assert-True -Condition (Test-Path $cmdLauncher) -Message "CMD double-click launcher is missing."

$tokens = $null
$parseErrors = $null
[System.Management.Automation.Language.Parser]::ParseFile($powerShellLauncher, [ref]$tokens, [ref]$parseErrors) | Out-Null
Assert-True -Condition ($parseErrors.Count -eq 0) -Message "PowerShell launcher has syntax errors."

$scriptText = Get-Content -Raw $powerShellLauncher
$cmdText = Get-Content -Raw $cmdLauncher

Assert-Contains -Text $scriptText -Expected "function Test-CommandAvailable" -Message "Launcher should check whether Docker is available."
Assert-Contains -Text $scriptText -Expected "function Wait-HttpEndpoint" -Message "Launcher should wait for HTTP readiness instead of sleeping blindly."
Assert-Contains -Text $scriptText -Expected '@("up", "-d", "--build")' -Message "Launcher should start Docker Compose with rebuild support."
Assert-Contains -Text $scriptText -Expected "http://localhost:8080/actuator/health" -Message "Launcher should wait for backend health."
Assert-Contains -Text $scriptText -Expected "http://localhost:5173" -Message "Launcher should open the frontend."
Assert-Contains -Text $cmdText -Expected "ExecutionPolicy Bypass" -Message "CMD wrapper should bypass local PowerShell script policy."
Assert-Contains -Text $cmdText -Expected "start-jat.ps1" -Message "CMD wrapper should delegate to the PowerShell launcher."

Write-Host "Launcher validation passed."
