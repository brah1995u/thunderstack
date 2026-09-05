Add-Type -AssemblyName System.Drawing

$destination = Resolve-Path "$PSScriptRoot\..\app\src\main\res\drawable-nodpi"
$uiSource = 'C:\Users\АНДРIЙ\OneDrive\Рабочий стол\ui.png'
$backgroundSource = 'C:\Users\АНДРIЙ\OneDrive\Рабочий стол\back.png'

function Export-Sprite {
    param(
        [System.Drawing.Bitmap]$Source,
        [System.Drawing.Rectangle]$Bounds,
        [string]$Name,
        [int]$Scale = 6
    )

    $crop = [System.Drawing.Bitmap]::new($Bounds.Width, $Bounds.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($crop)
    try {
        $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
        $graphics.DrawImage($Source, [System.Drawing.Rectangle]::new(0, 0, $Bounds.Width, $Bounds.Height), $Bounds, [System.Drawing.GraphicsUnit]::Pixel)
    } finally { $graphics.Dispose() }

    # ui.png has a neutral atlas background. Remove only the boundary-connected
    # neutral region; enclosed purple/navy artwork remains untouched.
    $visited = [bool[]]::new($crop.Width * $crop.Height)
    $queue = [System.Collections.Generic.Queue[System.Drawing.Point]]::new()
    for ($x = 0; $x -lt $crop.Width; $x++) {
        $queue.Enqueue([System.Drawing.Point]::new($x, 0))
        $queue.Enqueue([System.Drawing.Point]::new($x, $crop.Height - 1))
    }
    for ($y = 0; $y -lt $crop.Height; $y++) {
        $queue.Enqueue([System.Drawing.Point]::new(0, $y))
        $queue.Enqueue([System.Drawing.Point]::new($crop.Width - 1, $y))
    }
    while ($queue.Count -gt 0) {
        $point = $queue.Dequeue()
        if ($point.X -lt 0 -or $point.X -ge $crop.Width -or $point.Y -lt 0 -or $point.Y -ge $crop.Height) { continue }
        $index = $point.Y * $crop.Width + $point.X
        if ($visited[$index]) { continue }
        $visited[$index] = $true
        $color = $crop.GetPixel($point.X, $point.Y)
        $neutral = ([Math]::Abs($color.R - $color.G) -le 4) -and ([Math]::Abs($color.G - $color.B) -le 4)
        if (-not $neutral -or $color.R -gt 92) { continue }
        $crop.SetPixel($point.X, $point.Y, [System.Drawing.Color]::Transparent)
        $queue.Enqueue([System.Drawing.Point]::new($point.X - 1, $point.Y))
        $queue.Enqueue([System.Drawing.Point]::new($point.X + 1, $point.Y))
        $queue.Enqueue([System.Drawing.Point]::new($point.X, $point.Y - 1))
        $queue.Enqueue([System.Drawing.Point]::new($point.X, $point.Y + 1))
    }

    $scaled = [System.Drawing.Bitmap]::new([int]($crop.Width * $Scale), [int]($crop.Height * $Scale), [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($scaled)
    try {
        $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
        $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $graphics.DrawImage($crop, 0, 0, $scaled.Width, $scaled.Height)
    } finally { $graphics.Dispose() }
    try { $scaled.Save((Join-Path $destination $Name), [System.Drawing.Imaging.ImageFormat]::Png) }
    finally { $scaled.Dispose(); $crop.Dispose() }
}

# Only the references supplied and verified by the user are allowed here.
$ui = [System.Drawing.Bitmap]::FromFile($uiSource)
try {
    Export-Sprite $ui ([System.Drawing.Rectangle]::new(5, 3, 28, 30)) 'gem_red_reference.png' 8
    Export-Sprite $ui ([System.Drawing.Rectangle]::new(38, 3, 28, 30)) 'gem_blue_reference.png' 8
    Export-Sprite $ui ([System.Drawing.Rectangle]::new(70, 3, 28, 30)) 'gem_purple_reference.png' 8
    Export-Sprite $ui ([System.Drawing.Rectangle]::new(102, 3, 28, 30)) 'gem_green_reference.png' 8
} finally { $ui.Dispose() }

$background = [System.Drawing.Bitmap]::FromFile($backgroundSource)
try {
    $bounds = [System.Drawing.Rectangle]::new(83, 10, 72, 160)
    $column = [System.Drawing.Bitmap]::new($bounds.Width * 4, $bounds.Height * 4, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($column)
    try {
        $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.DrawImage($background, [System.Drawing.Rectangle]::new(0, 0, $column.Width, $column.Height), $bounds, [System.Drawing.GraphicsUnit]::Pixel)
    } finally { $graphics.Dispose() }
    try { $column.Save((Join-Path $destination 'bg_reference_column.png'), [System.Drawing.Imaging.ImageFormat]::Png) }
    finally { $column.Dispose() }
} finally { $background.Dispose() }
