# BarboYa - Iniciador completo: Backend FastAPI + Frontend Flutter Web
# Uso: .\iniciar_barboya.ps1

$rootDir = "C:\BarboYa"
$backendDir = "$rootDir\backend"
$flutterDir = "$rootDir\BarboYa Flutter\mobile_app"
$buildDir = "$flutterDir\build\web"
$venvPython = "$backendDir\venv\Scripts\python.exe"

Write-Host ""
Write-Host "╔══════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║        BarboYa Superapp - Iniciando      ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# 1. Verificar Python venv
if (-not (Test-Path $venvPython)) {
    Write-Host "[ERROR] No se encontro el entorno virtual en $venvPython" -ForegroundColor Red
    Write-Host "Ejecuta primero: cd $backendDir && python -m venv venv && venv\Scripts\pip install -r requirements.txt"
    exit 1
}

# 2. Levantar backend en segundo plano
Write-Host "[1/2] Iniciando backend FastAPI en http://127.0.0.1:8010..." -ForegroundColor Yellow
$backendJob = Start-Process -FilePath $venvPython -ArgumentList "-m", "uvicorn", "app.main:app", "--reload", "--host", "127.0.0.1", "--port", "8010" -WorkingDirectory $backendDir -PassThru -WindowStyle Minimized

Start-Sleep -Seconds 3
Write-Host "      Backend PID: $($backendJob.Id)" -ForegroundColor Green

# 3. Verificar si ya existe el build web
if (Test-Path "$buildDir\index.html") {
    Write-Host "[2/2] Build web ya existe, iniciando servidor web..." -ForegroundColor Yellow
} else {
    Write-Host "[2/2] Build web no encontrado. Construyendo (esto toma ~2 min)..." -ForegroundColor Yellow
    Set-Location $flutterDir
    flutter build web --release --dart-define=API_URL=http://127.0.0.1:8010/api/v1 --dart-define=WS_URL=ws://127.0.0.1:8010/api/v1/ws
}

# 4. Servir el build web con Python
Write-Host ""
Write-Host "Iniciando servidor web en http://localhost:8080 ..." -ForegroundColor Yellow
Write-Host ""
Write-Host "╔══════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║  BarboYa Web: http://localhost:8080      ║" -ForegroundColor Green
Write-Host "║  Backend API: http://localhost:8010      ║" -ForegroundColor Green
Write-Host "║  Presiona Ctrl+C para detener            ║" -ForegroundColor Green
Write-Host "╚══════════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""

# Abrir navegador automaticamente
Start-Sleep -Seconds 1
Start-Process "http://localhost:8080"

# Servir con Python http.server
Set-Location $buildDir
& $venvPython -m http.server 8080
