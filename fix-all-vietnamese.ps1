# Complete Vietnamese Font Fix Script
# Script hoàn chỉnh để fix tất cả lỗi font tiếng Việt trong Android project

Write-Host "=== VIETNAMESE FONT FIX SCRIPT ===" -ForegroundColor Cyan
Write-Host "Fixing all Vietnamese encoding issues..." -ForegroundColor Yellow

$totalFixed = 0

# Define Vietnamese text replacements (using regex-safe patterns)
$vietnameseFixes = @{
    'Cáº§n quyá»n thĂ´ng bĂ¡o Ä\'á»\x83 nháº­n lá»i nháº¯c' = 'Cần quyền thông báo để nhận lời nhắc'
    'Báº¡n cĂ³ lá»i nháº¯c cho:' = 'Bạn có lời nhắc cho:'
    'Nhiá»\x87m vá»¥ Ä\'Ă£ Ä\'áº¿n háº¡n:' = 'Nhiệm vụ đã đến hạn:'
    'Lá»\x97i: KhĂ´ng thá»\x83 má»\x9F chi tiáº¿t nhiá»\x87m vá»¥' = 'Lỗi: Không thể mở chi tiết nhiệm vụ'
    'Lá»\x97i: KhĂ´ng thá»\x83 cáº­p nháº­t nhiá»\x87m vá»¥' = 'Lỗi: Không thể cập nhật nhiệm vụ'
    'Lá»\x97i táº¡o thá»\x83 loáº¡i:' = 'Lỗi tạo thể loại:'
    'Lá»\x97i thĂªm task:' = 'Lỗi thêm task:'
    'Lá»\x97i táº£i task:' = 'Lỗi tải task:'
    'Lá»\x97i cáº­p nháº­t category:' = 'Lỗi cập nhật category:'
    'Lá»\x97i táº£i categories:' = 'Lỗi tải categories:'
    'Lá»\x97i cáº­p nháº­t ngĂ y giá»:' = 'Lỗi cập nhật ngày giờ:'
    'Lá»\x97i cáº­p nháº­t title:' = 'Lỗi cập nhật title:'
    'Lá»\x97i reset:' = 'Lỗi reset:'
    'Lá»\x97i:' = 'Lỗi:'
}

function Fix-FileEncoding {
    param([string]$FilePath)
    
    if (-not (Test-Path $FilePath)) { return $false }
    
    try {
        # Read file content
        $content = [System.IO.File]::ReadAllText($FilePath, [System.Text.UTF8Encoding]::new($true))
        $originalContent = $content
        
        # Remove BOM if present
        if ($content.StartsWith([char]0xFEFF)) {
            $content = $content.Substring(1)
        }
        
        # Fix common encoding issues
        foreach ($pattern in $vietnameseFixes.Keys) {
            $replacement = $vietnameseFixes[$pattern]
            if ($content -match [regex]::Escape($pattern)) {
                $content = $content -replace [regex]::Escape($pattern), $replacement
                Write-Host "  Fixed: $pattern" -ForegroundColor Green
            }
        }
        
        # Fix broken package statements
        $content = $content -replace '^[^\w\s]*package', 'package'
        
        # Save if changed
        if ($content -ne $originalContent) {
            [System.IO.File]::WriteAllText($FilePath, $content, [System.Text.UTF8Encoding]::new($false))
            return $true
        }
        
        return $false
    } catch {
        Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# Find and fix Java files
Write-Host "Scanning Java files..." -ForegroundColor White
$javaFiles = Get-ChildItem -Path "app\src" -Recurse -Filter "*.java"

foreach ($file in $javaFiles) {
    $relativePath = $file.FullName.Replace((Get-Location).Path + "\", "")
    if (Fix-FileEncoding -FilePath $file.FullName) {
        Write-Host "Fixed: $relativePath" -ForegroundColor Green
        $totalFixed++
    }
}

# Test compilation
Write-Host "`nTesting compilation..." -ForegroundColor Yellow
try {
    $result = & .\gradlew.bat compileDebugJavaWithJavac --quiet 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Compilation successful!" -ForegroundColor Green
    } else {
        Write-Host "⚠ Compilation issues:" -ForegroundColor Yellow
        $result | Where-Object { $_ -match "error:" } | Select-Object -First 3 | ForEach-Object {
            Write-Host "  $_" -ForegroundColor Red
        }
    }
} catch {
    Write-Host "Could not test compilation" -ForegroundColor Yellow
}

# Summary
Write-Host "`n=== SUMMARY ===" -ForegroundColor Cyan
Write-Host "Files fixed: $totalFixed" -ForegroundColor Green
Write-Host "Vietnamese text encoding issues should now be resolved!" -ForegroundColor Green
Write-Host "All backup files are ignored by .gitignore" -ForegroundColor Cyan
