$watcher = New-Object System.IO.FileSystemWatcher
$watcher.Path = "C:\BarboYa"
$watcher.IncludeSubdirectories = $true
$watcher.EnableRaisingEvents = $true

$action = {
    $path = $Event.SourceEventArgs.FullPath
    if ($path -notmatch "\.git" -and $path -notmatch "venv" -and $path -notmatch "__pycache__") {
        Start-Sleep -Seconds 2 # Esperar a que el archivo termine de escribirse
        Set-Location "C:\BarboYa"
        git add .
        $changes = git status --porcelain
        if ($changes) {
            $date = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
            git commit -m "Auto-sync update: $date"
            git push
            Write-Host "[$date] Sincronizado con éxito en GitHub." -ForegroundColor Green
        }
    }
}

Register-ObjectEvent $watcher "Changed" -Action $action | Out-Null
Register-ObjectEvent $watcher "Created" -Action $action | Out-Null
Register-ObjectEvent $watcher "Deleted" -Action $action | Out-Null

Write-Host "Auto-sync iniciado para C:\BarboYa. Monitoreando cambios..." -ForegroundColor Cyan
try {
    while ($true) {
        Start-Sleep -Seconds 1
    }
}
finally {
    Unregister-Event -SourceIdentifier *
    Write-Host "Auto-sync detenido." -ForegroundColor Yellow
}
