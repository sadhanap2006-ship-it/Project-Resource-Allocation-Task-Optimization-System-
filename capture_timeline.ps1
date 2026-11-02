$l = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -Body '{"email":"pm@prs.local","password":"password123"}' -ContentType 'application/json'
$token = $l.data.token
Write-Host "PM logged in. Token length: $($token.Length)"

$url = "http://localhost:8080/timeline.html?token=$token"
$outPath = "C:\Users\sadha\AppData\Local\Temp\timeline_circular_rings.png"
$artifactPath = "C:\Users\sadha\.gemini\antigravity-ide\brain\5dc9c97f-ae8f-4d79-99d7-789ccb5f226b\timeline_circular_rings.png"

$chrome = "C:\Program Files\Google\Chrome\Application\chrome.exe"
$args = @(
    "--headless=new",
    "--window-size=1280,950",
    "--virtual-time-budget=6000",
    "--screenshot=$outPath",
    $url
)

Write-Host "Capturing headless Chrome screenshot of $url..."
$proc = Start-Process -FilePath $chrome -ArgumentList $args -PassThru -Wait

if (Test-Path $outPath) {
    Copy-Item -Path $outPath -Destination $artifactPath -Force
    $size = (Get-Item $artifactPath).Length
    Write-Host "SUCCESS: Screenshot saved to $artifactPath ($size bytes)"
} else {
    Write-Host "ERROR: Screenshot not found at $outPath"
}
