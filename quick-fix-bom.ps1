# Quick Fix for BOM and Encoding Issues
Write-Host "Fixing BOM and encoding issues..." -ForegroundColor Yellow

$filesToFix = @(
    "app\src\main\java\com\example\todolist\repository\task\TaskQueryRepository.java",
    "app\src\main\java\com\example\todolist\util\NotificationPermissionHelper.java"
)

foreach ($file in $filesToFix) {
    if (Test-Path $file) {
        Write-Host "Fixing: $file" -ForegroundColor White
        
        # Read content and remove BOM
        $content = [System.IO.File]::ReadAllText($file, [System.Text.UTF8Encoding]::new($true))
        
        # Remove BOM character if present
        if ($content.StartsWith([char]0xFEFF)) {
            $content = $content.Substring(1)
        }
        
        # Remove any strange characters before 'package'
        $content = $content -replace '^[^\w\s]*package', 'package'
        
        # Write back without BOM
        [System.IO.File]::WriteAllText($file, $content, [System.Text.UTF8Encoding]::new($false))
        
        Write-Host "  Fixed!" -ForegroundColor Green
    }
}

Write-Host "Testing compile..." -ForegroundColor Yellow
$result = & .\gradlew.bat compileDebugJavaWithJavac --quiet 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "Compile successful!" -ForegroundColor Green
} else {
    Write-Host "Still has errors:" -ForegroundColor Red
    $result | Where-Object { $_ -match "error:" } | Select-Object -First 3
}
