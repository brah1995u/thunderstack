Add-Type -AssemblyName System.Drawing

$assetRoot = Resolve-Path "$PSScriptRoot\..\app\src\main\res\drawable-nodpi"
$maximumWidths = @{
    'logo_thunder_stack.png' = 1024
    'btn_primary_default.png' = 960
    'btn_primary_disabled.png' = 960
    'btn_square.png' = 420
    'ic_nav_back.png' = 420
    'ic_nav_home.png' = 420
    'ic_nav_pause.png' = 420
    'toggle_track_off.png' = 512
    'toggle_track_on.png' = 512
    'toggle_knob.png' = 320
    'panel_title_banner.png' = 1100
    'panel_popup.png' = 700
    'panel_card.png' = 1100
    'panel_leaderboard_row.png' = 1100
    'level_node.png' = 420
    'ic_coin.png' = 320
    'ic_crystal.png' = 320
    'platform_start.png' = 1000
    'block_stone.png' = 768
    'block_stone_moss.png' = 768
    'block_gold.png' = 768
    'block_storm.png' = 768
    'block_cracked.png' = 768
    'booster_thunder.png' = 420
    'booster_shield.png' = 420
    'booster_slow_time.png' = 420
    'booster_crystal_magnet.png' = 420
    'collectible_crystal.png' = 320
    'rush_gem_red.png' = 320
    'rush_gem_blue.png' = 320
    'rush_gem_purple.png' = 320
    'rush_gem_green.png' = 320
    'fx_lightning.png' = 420
    'fx_perfect_glow.png' = 900
}

foreach ($entry in $maximumWidths.GetEnumerator()) {
    $path = Join-Path $assetRoot $entry.Key
    $source = [System.Drawing.Image]::FromFile($path)
    try {
        if ($source.Width -le $entry.Value) { continue }
        $targetWidth = [int]$entry.Value
        $targetHeight = [int][Math]::Round($source.Height * $targetWidth / $source.Width)
        $target = New-Object System.Drawing.Bitmap($targetWidth, $targetHeight, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            $graphics = [System.Drawing.Graphics]::FromImage($target)
            try {
                $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
                $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
                $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
                $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
                $graphics.DrawImage($source, 0, 0, $targetWidth, $targetHeight)
            } finally {
                $graphics.Dispose()
            }
            $temporary = "$path.optimized.png"
            $target.Save($temporary, [System.Drawing.Imaging.ImageFormat]::Png)
        } finally {
            $target.Dispose()
        }
    } finally {
        $source.Dispose()
    }
    Move-Item -LiteralPath $temporary -Destination $path -Force
}

function Trim-TransparentBounds {
    param([string]$Name, [int]$Margin = 4)
    $path = Join-Path $assetRoot $Name
    $source = [System.Drawing.Bitmap]::FromFile($path)
    try {
        $minimumX = $source.Width
        $minimumY = $source.Height
        $maximumX = -1
        $maximumY = -1
        for ($y = 0; $y -lt $source.Height; $y++) {
            for ($x = 0; $x -lt $source.Width; $x++) {
                if ($source.GetPixel($x, $y).A -gt 18) {
                    if ($x -lt $minimumX) { $minimumX = $x }
                    if ($x -gt $maximumX) { $maximumX = $x }
                    if ($y -lt $minimumY) { $minimumY = $y }
                    if ($y -gt $maximumY) { $maximumY = $y }
                }
            }
        }
        if ($maximumX -lt 0) { return }
        $left = [Math]::Max(0, $minimumX - $Margin)
        $top = [Math]::Max(0, $minimumY - $Margin)
        $right = [Math]::Min($source.Width - 1, $maximumX + $Margin)
        $bottom = [Math]::Min($source.Height - 1, $maximumY + $Margin)
        if ($left -eq 0 -and $top -eq 0 -and $right -eq $source.Width - 1 -and $bottom -eq $source.Height - 1) { return }
        $width = $right - $left + 1
        $height = $bottom - $top + 1
        $target = [System.Drawing.Bitmap]::new($width, $height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            $graphics = [System.Drawing.Graphics]::FromImage($target)
            try {
                $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
                $graphics.DrawImage($source, [System.Drawing.Rectangle]::new(0, 0, $width, $height), [System.Drawing.Rectangle]::new($left, $top, $width, $height), [System.Drawing.GraphicsUnit]::Pixel)
            } finally { $graphics.Dispose() }
            $temporary = "$path.trimmed.png"
            $target.Save($temporary, [System.Drawing.Imaging.ImageFormat]::Png)
        } finally { $target.Dispose() }
    } finally { $source.Dispose() }
    Move-Item -LiteralPath $temporary -Destination $path -Force
}

@(
    'block_stone.png',
    'block_stone_moss.png',
    'block_gold.png',
    'block_storm.png',
    'block_cracked.png',
    'platform_start.png',
    'temple_step.png',
    'temple_marble_course.png',
    'temple_column_course.png',
    'temple_entablature.png',
    'temple_pediment.png',
    'temple_storm_altar.png',
    'temple_cracked_ruin.png'
) | ForEach-Object { Trim-TransparentBounds $_ 4 }
