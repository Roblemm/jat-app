$ErrorActionPreference = "Stop"

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$powerShellLauncher = Join-Path $PSScriptRoot "start-jat.ps1"
$cmdLauncher = Join-Path $PSScriptRoot "Start Jat.cmd"
$shortcutInstaller = Join-Path $PSScriptRoot "install-desktop-shortcut.ps1"
$shortcutInstallerCmd = Join-Path $PSScriptRoot "Install Desktop Shortcut.cmd"

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
Assert-True -Condition (Test-Path $shortcutInstaller) -Message "Desktop shortcut installer is missing."
Assert-True -Condition (Test-Path $shortcutInstallerCmd) -Message "Desktop shortcut CMD installer is missing."

$tokens = $null
$parseErrors = $null
[System.Management.Automation.Language.Parser]::ParseFile($powerShellLauncher, [ref]$tokens, [ref]$parseErrors) | Out-Null
Assert-True -Condition ($parseErrors.Count -eq 0) -Message "PowerShell launcher has syntax errors."

$installerTokens = $null
$installerParseErrors = $null
[System.Management.Automation.Language.Parser]::ParseFile($shortcutInstaller, [ref]$installerTokens, [ref]$installerParseErrors) | Out-Null
Assert-True -Condition ($installerParseErrors.Count -eq 0) -Message "Desktop shortcut installer has syntax errors."

$scriptText = Get-Content -Raw $powerShellLauncher
$cmdText = Get-Content -Raw $cmdLauncher
$installerText = Get-Content -Raw $shortcutInstaller
$installerCmdText = Get-Content -Raw $shortcutInstallerCmd

Assert-Contains -Text $scriptText -Expected "function Test-CommandAvailable" -Message "Launcher should check whether Docker is available."
Assert-Contains -Text $scriptText -Expected "function Start-DockerDesktop" -Message "Launcher should start Docker Desktop when the engine is stopped."
Assert-Contains -Text $scriptText -Expected "function Wait-DockerEngine" -Message "Launcher should wait for the Docker engine before running Compose."
Assert-Contains -Text $scriptText -Expected "Docker Desktop.exe" -Message "Launcher should know the Docker Desktop executable name."
Assert-Contains -Text $scriptText -Expected "DockerTimeoutSeconds" -Message "Launcher should expose a Docker startup timeout."
Assert-Contains -Text $scriptText -Expected 'return $false' -Message "Docker engine probe should return false instead of crashing when Docker is stopped."
Assert-Contains -Text $scriptText -Expected "function Wait-HttpEndpoint" -Message "Launcher should wait for HTTP readiness instead of sleeping blindly."
Assert-Contains -Text $scriptText -Expected '@("up", "-d", "--build")' -Message "Launcher should start Docker Compose with rebuild support."
Assert-Contains -Text $scriptText -Expected "http://localhost:8080/actuator/health" -Message "Launcher should wait for backend health."
Assert-Contains -Text $scriptText -Expected "http://localhost:5173" -Message "Launcher should open the frontend."
Assert-Contains -Text $cmdText -Expected "ExecutionPolicy Bypass" -Message "CMD wrapper should bypass local PowerShell script policy."
Assert-Contains -Text $cmdText -Expected "start-jat.ps1" -Message "CMD wrapper should delegate to the PowerShell launcher."
Assert-Contains -Text $installerText -Expected "WScript.Shell" -Message "Shortcut installer should use the Windows shortcut API."
Assert-Contains -Text $installerText -Expected "DesktopDirectory" -Message "Shortcut installer should place the shortcut on the Desktop."
Assert-Contains -Text $installerText -Expected "IconLocation" -Message "Shortcut installer should assign a custom icon."
Assert-Contains -Text $installerText -Expected "function New-JatIcon" -Message "Shortcut installer should generate the local icon."
Assert-Contains -Text $installerText -Expected ".Path" -Message "Shortcut installer should pass string paths to the Windows shortcut API."
Assert-Contains -Text $installerCmdText -Expected "install-desktop-shortcut.ps1" -Message "Shortcut CMD installer should delegate to the PowerShell installer."

Write-Host "Launcher validation passed."
