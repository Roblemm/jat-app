param(
    [string]$ShortcutName = "Jat"
)

$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$launcherPath = Join-Path $PSScriptRoot "Start Jat.cmd"
$desktopPath = [Environment]::GetFolderPath("DesktopDirectory")
$shortcutPath = Join-Path $desktopPath "$ShortcutName.lnk"
$iconDirectory = Join-Path ([Environment]::GetFolderPath("LocalApplicationData")) "Jat"
$iconPath = Join-Path $iconDirectory "jat.ico"

function New-JatIcon {
    param([string]$Path)

    Add-Type -AssemblyName System.Drawing

    $directory = Split-Path -Parent $Path
    if (-not (Test-Path $directory)) {
        New-Item -ItemType Directory -Path $directory | Out-Null
    }

    $bitmap = New-Object System.Drawing.Bitmap 256, 256
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

    try {
        $background = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
            (New-Object System.Drawing.Rectangle 0, 0, 256, 256),
            ([System.Drawing.Color]::FromArgb(4, 10, 22)),
            ([System.Drawing.Color]::FromArgb(8, 34, 62)),
            45
        )
        $graphics.FillRectangle($background, 0, 0, 256, 256)

        $accentPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(49, 220, 255)), 10
        $graphics.DrawRectangle($accentPen, 20, 20, 216, 216)

        $glowBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(42, 120, 255))
        $graphics.FillEllipse($glowBrush, 164, 26, 54, 54)

        $font = New-Object System.Drawing.Font "Segoe UI", 130, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)
        $textBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(223, 250, 255))
        $format = New-Object System.Drawing.StringFormat
        $format.Alignment = [System.Drawing.StringAlignment]::Center
        $format.LineAlignment = [System.Drawing.StringAlignment]::Center
        $graphics.DrawString("J", $font, $textBrush, (New-Object System.Drawing.RectangleF 0, 10, 256, 226), $format)

        $iconHandle = $bitmap.GetHicon()
        $icon = [System.Drawing.Icon]::FromHandle($iconHandle)
        $stream = [System.IO.File]::Create($Path)
        try {
            $icon.Save($stream)
        }
        finally {
            $stream.Dispose()
            $icon.Dispose()
        }
    }
    finally {
        if ($format) { $format.Dispose() }
        if ($textBrush) { $textBrush.Dispose() }
        if ($font) { $font.Dispose() }
        if ($glowBrush) { $glowBrush.Dispose() }
        if ($accentPen) { $accentPen.Dispose() }
        if ($background) { $background.Dispose() }
        $graphics.Dispose()
        $bitmap.Dispose()
    }
}

if (-not (Test-Path $launcherPath)) {
    throw "Launcher not found at $launcherPath"
}

New-JatIcon -Path $iconPath

$shell = New-Object -ComObject WScript.Shell
$shortcut = $shell.CreateShortcut($shortcutPath)
$shortcut.TargetPath = $launcherPath
$shortcut.WorkingDirectory = $projectRoot
$shortcut.IconLocation = $iconPath
$shortcut.Description = "Start Jat local command center"
$shortcut.Save()

Write-Host "Desktop shortcut created:" -ForegroundColor Green
Write-Host $shortcutPath
Write-Host ""
Write-Host "Icon created:"
Write-Host $iconPath
