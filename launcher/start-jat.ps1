$ErrorActionPreference = "Stop"

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $projectRoot

Write-Host "Starting Jat services..."
docker compose up -d --build

$healthUrl = "http://localhost:8080/actuator/health"
$frontendUrl = "http://localhost:5173"
$maxAttempts = 60

for ($i = 1; $i -le $maxAttempts; $i++) {
    try {
        $response = Invoke-RestMethod -Uri $healthUrl -Method Get -TimeoutSec 2
        if ($response.status -eq "UP") {
            Write-Host "Jat backend is ready."
            Start-Process $frontendUrl
            exit 0
        }
    }
    catch {
        Write-Host "Waiting for backend... ($i/$maxAttempts)"
        Start-Sleep -Seconds 2
    }
}

Write-Error "Backend did not become healthy. Check logs with: docker compose logs backend"
exit 1
