# Docker-free local run: builds the frontend into the backend, then starts backend + embedded PostgreSQL on http://localhost:8080
# Usage (PowerShell, from the project root):  .\scripts\run-local.ps1
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
. "$PSScriptRoot\env.ps1" | Out-Null

Push-Location "$root\frontend"
if (-not (Test-Path node_modules)) { npm ci }
npm run build
Pop-Location

$static = "$root\backend\src\main\resources\static"
if (Test-Path $static) { Remove-Item $static -Recurse -Force }
Copy-Item "$root\frontend\dist" $static -Recurse

Push-Location "$root\backend"
mvn -q test-compile
mvn -q dependency:build-classpath "-Dmdep.includeScope=test" "-Dmdep.outputFile=target\cp.txt"
$cp = "target\classes;target\test-classes;" + (Get-Content target\cp.txt -Raw)
# DevServer starts a persistent embedded PostgreSQL in .\data\pg (port 5433) and then the application
java -cp $cp cz.metdat.DevServer --app.storage.type=filesystem
Pop-Location
