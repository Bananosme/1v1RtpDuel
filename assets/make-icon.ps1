# Generates Modrinth icon candidates for 1v1RtpDuel (512x512 PNG).
# Usage: powershell -ExecutionPolicy Bypass -File assets\make-icon.ps1
Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = 'Stop'
$dir = Split-Path -Parent $MyInvocation.MyCommand.Path
$S = 512

$SWORD = [string][char]0x2694                      # U+2694 CROSSED SWORDS
$DIE = [System.Char]::ConvertFromUtf32(0x1F3B2)    # dice

function New-Gfx([int]$size) {
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    return @($bmp, $g)
}

function Get-Font([string]$family, [float]$px, $style) {
    try { return New-Object System.Drawing.Font -ArgumentList $family, $px, $style, ([System.Drawing.GraphicsUnit]::Pixel) }
    catch { return New-Object System.Drawing.Font -ArgumentList 'Arial', $px, $style, ([System.Drawing.GraphicsUnit]::Pixel) }
}

function New-Brush([string]$hex, [int]$alpha = 255) {
    $c = [System.Drawing.ColorTranslator]::FromHtml($hex)
    return New-Object System.Drawing.SolidBrush -ArgumentList ([System.Drawing.Color]::FromArgb($alpha, $c.R, $c.G, $c.B))
}

function Fill-Gradient($g, [int]$size, [string]$topHex, [string]$bottomHex, [float]$angle = 90) {
    $rect = New-Object System.Drawing.Rectangle(0, 0, $size, $size)
    $b = New-Object System.Drawing.Drawing2D.LinearGradientBrush -ArgumentList $rect,
        ([System.Drawing.ColorTranslator]::FromHtml($topHex)),
        ([System.Drawing.ColorTranslator]::FromHtml($bottomHex)), $angle
    $g.FillRectangle($b, $rect)
    $b.Dispose()
}

# Text centered horizontally, with a dark outline for readability.
function Draw-Outlined($g, [string]$text, [string]$family, [float]$px, $style, [string]$colorHex, [float]$centerY, [int]$outline = 5) {
    $font = Get-Font $family $px $style
    $m = $g.MeasureString($text, $font)
    $x = ($g.VisibleClipBounds.Width - $m.Width) / 2
    $y = $centerY - $m.Height / 2
    $dark = New-Brush '#000000' 200
    for ($dx = -$outline; $dx -le $outline; $dx += 2) {
        for ($dy = -$outline; $dy -le $outline; $dy += 2) {
            $g.DrawString($text, $font, $dark, ($x + $dx), ($y + $dy))
        }
    }
    $dark.Dispose()
    $brush = New-Brush $colorHex
    $g.DrawString($text, $font, $brush, $x, $y)
    $brush.Dispose()
    $font.Dispose()
}

function Save-Icon($bmp, $g, [string]$name) {
    $g.Dispose()
    $bmp.Save((Join-Path $dir $name), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "written $name"
}

# ------------------------------------------------------- variant 1: dark + sword + accent
$r = New-Gfx $S; $bmp = $r[0]; $g = $r[1]
Fill-Gradient $g $S '#243040' '#0d1117'
$glow = New-Brush '#3ddc84' 45
$g.FillEllipse($glow, ($S / 2 - 190), ($S / 2 - 215), 380, 380)
$glow.Dispose()
Draw-Outlined $g $SWORD 'Segoe UI Symbol' 300 ([System.Drawing.FontStyle]::Regular) '#e8eef2' 235 3
$accent = New-Brush '#3ddc84'
$g.FillRectangle($accent, ($S / 2 - 110), 392, 220, 10)
$accent.Dispose()
Draw-Outlined $g '1v1' 'Segoe UI' 130 ([System.Drawing.FontStyle]::Bold) '#ffffff' 452 4
Save-Icon $bmp $g 'icon-dark.png'

# ------------------------------------------------------- variant 2: two-team diagonal split
$r = New-Gfx $S; $bmp = $r[0]; $g = $r[1]
Fill-Gradient $g $S '#2f6f3e' '#24512f'
$tri = @(
    (New-Object System.Drawing.Point(0, 0)),
    (New-Object System.Drawing.Point($S, 0)),
    (New-Object System.Drawing.Point($S, $S))
)
$orange = New-Object System.Drawing.Drawing2D.LinearGradientBrush -ArgumentList (
    (New-Object System.Drawing.Rectangle(0, 0, $S, $S))),
    ([System.Drawing.ColorTranslator]::FromHtml('#d97b2b')),
    ([System.Drawing.ColorTranslator]::FromHtml('#8d4c14')), 45
$g.FillPolygon($orange, $tri)
$orange.Dispose()
Draw-Outlined $g $SWORD 'Segoe UI Symbol' 290 ([System.Drawing.FontStyle]::Regular) '#ffffff' 230 4
Draw-Outlined $g '1v1' 'Segoe UI' 125 ([System.Drawing.FontStyle]::Bold) '#ffffff' 450 4
Save-Icon $bmp $g 'icon-split.png'

# ------------------------------------------------------- variant 3: grass block + sword + dice
$r = New-Gfx $S; $bmp = $r[0]; $g = $r[1]
Fill-Gradient $g $S '#8a6034' '#5e4023'
$grassRect = New-Object System.Drawing.Rectangle(0, 0, $S, 150)
$gb = New-Object System.Drawing.Drawing2D.LinearGradientBrush -ArgumentList $grassRect,
    ([System.Drawing.ColorTranslator]::FromHtml('#6fc04a')),
    ([System.Drawing.ColorTranslator]::FromHtml('#4e9134')), 90
$g.FillRectangle($gb, $grassRect)
$gb.Dispose()
$grid = New-Object System.Drawing.Pen -ArgumentList ([System.Drawing.Color]::FromArgb(28, 0, 0, 0)), 2.0
$step = $S / 8
for ($i = 1; $i -lt 8; $i++) {
    $g.DrawLine($grid, ($i * $step), 0, ($i * $step), $S)
    $g.DrawLine($grid, 0, ($i * $step), $S, ($i * $step))
}
$grid.Dispose()
Draw-Outlined $g $SWORD 'Segoe UI Symbol' 270 ([System.Drawing.FontStyle]::Regular) '#f4f7f9' 258 5
Draw-Outlined $g '1v1' 'Segoe UI' 120 ([System.Drawing.FontStyle]::Bold) '#ffffff' 442 4
Save-Icon $bmp $g 'icon-grass.png'

# ------------------------------------------------------- variant 4: dark + dice + sword (random!)
$r = New-Gfx $S; $bmp = $r[0]; $g = $r[1]
Fill-Gradient $g $S '#1f2b3a' '#0b0f14'
Draw-Outlined $g $SWORD 'Segoe UI Symbol' 190 ([System.Drawing.FontStyle]::Regular) '#e8eef2' 210 4
Draw-Outlined $g $DIE 'Segoe UI Emoji' 150 ([System.Drawing.FontStyle]::Regular) '#ffd166' 380 4
Draw-Outlined $g '1v1' 'Segoe UI' 110 ([System.Drawing.FontStyle]::Bold) '#ffffff' 462 4
Save-Icon $bmp $g 'icon-dice.png'

Write-Host 'done'
