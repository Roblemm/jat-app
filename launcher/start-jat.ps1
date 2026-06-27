param(
    [switch]$NoBrowser,
    [int]$TimeoutSeconds = 180
)

$ErrorActionPreference = "Stop"

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
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

try {
    & docker info | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Docker engine is not responding."
    }
}
catch {
    throw "Docker is installed, but the Docker engine is not running. Start Docker Desktop, wait until it finishes loading, then run this launcher again."
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
