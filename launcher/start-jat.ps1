param(
    [switch]$NoBrowser,
    [int]$TimeoutSeconds = 180,
    [int]$DockerTimeoutSeconds = 240
)

$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$backendHealthUrl = "http://localhost:8080/actuator/health"
$frontendUrl = "http://localhost:5173"

function Write-Step {
    param([string]$Message)

    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Test-CommandAvailable {
    param([string]$CommandName)

    return $null -ne (Get-Command $CommandName -ErrorAction SilentlyContinue)
}

function Test-DockerEngine {
    try {
        & docker info *> $null
        return $LASTEXITCODE -eq 0
    }
    catch {
        return $false
    }
}

function Find-DockerDesktop {
    $command = Get-Command "Docker Desktop.exe" -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $candidates = @()
    if ($env:ProgramFiles) {
        $candidates += Join-Path $env:ProgramFiles "Docker\Docker\Docker Desktop.exe"
    }
    if (${env:ProgramFiles(x86)}) {
        $candidates += Join-Path ${env:ProgramFiles(x86)} "Docker\Docker\Docker Desktop.exe"
    }
    if ($env:LocalAppData) {
        $candidates += Join-Path $env:LocalAppData "Docker\Docker Desktop.exe"
        $candidates += Join-Path $env:LocalAppData "Programs\Docker\Docker\Docker Desktop.exe"
    }

    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path $candidate)) {
            return $candidate
        }
    }

    return $null
}

function Start-DockerDesktop {
    $dockerDesktopPath = Find-DockerDesktop
    if (-not $dockerDesktopPath) {
        throw "Docker is installed, but Docker Desktop could not be found. Open Docker Desktop once from the Start menu, then run this launcher again."
    }

    Write-Host "Starting Docker Desktop..." -ForegroundColor Yellow
    Start-Process -FilePath $dockerDesktopPath -WindowStyle Minimized | Out-Null
}

function Wait-DockerEngine {
    param([int]$TimeoutSeconds)

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $attempt = 1

    while ((Get-Date) -lt $deadline) {
        if (Test-DockerEngine) {
            Write-Host "Docker engine is ready." -ForegroundColor Green
            return
        }

        Write-Host "Waiting for Docker Desktop engine... attempt $attempt" -ForegroundColor DarkGray
        $attempt++
        Start-Sleep -Seconds 3
    }

    throw "Docker Desktop started, but the Docker engine did not become ready within $TimeoutSeconds seconds. Open Docker Desktop to check its status, then run this launcher again."
}

function Invoke-DockerCompose {
    param([string[]]$Arguments)

    Push-Location $projectRoot
    try {
        & docker compose @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "docker compose $($Arguments -join ' ') failed with exit code $LASTEXITCODE."
        }
    }
    finally {
        Pop-Location
    }
}

function Wait-HttpEndpoint {
    param(
        [string]$Url,
        [string]$Name,
        [int]$TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $attempt = 1

    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -Method Get -TimeoutSec 3
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                Write-Host "$Name is ready at $Url" -ForegroundColor Green
                return
            }
        }
        catch {
            Write-Host "Waiting for $Name... attempt $attempt" -ForegroundColor DarkGray
        }

        $attempt++
        Start-Sleep -Seconds 2
    }

    throw "$Name did not become ready within $TimeoutSeconds seconds. Check logs with: docker compose logs"
}

Write-Step "Checking Docker"
if (-not (Test-CommandAvailable "docker")) {
    throw "Docker was not found on PATH. Open Docker Desktop, then try again. If Docker is not installed, install Docker Desktop first."
}

if (-not (Test-DockerEngine)) {
    Start-DockerDesktop
    Wait-DockerEngine -TimeoutSeconds $DockerTimeoutSeconds
}
else {
    Write-Host "Docker engine is ready." -ForegroundColor Green
}

Write-Step "Starting Jat services"
Invoke-DockerCompose -Arguments @("up", "-d", "--build")

Write-Step "Waiting for backend"
Wait-HttpEndpoint -Url $backendHealthUrl -Name "Jat backend" -TimeoutSeconds $TimeoutSeconds

Write-Step "Waiting for frontend"
Wait-HttpEndpoint -Url $frontendUrl -Name "Jat frontend" -TimeoutSeconds $TimeoutSeconds

if (-not $NoBrowser) {
    Write-Step "Opening Jat"
    Start-Process $frontendUrl
}

Write-Host ""
Write-Host "Jat is running." -ForegroundColor Green
Write-Host "Frontend: $frontendUrl"
Write-Host "Backend:  $backendHealthUrl"
