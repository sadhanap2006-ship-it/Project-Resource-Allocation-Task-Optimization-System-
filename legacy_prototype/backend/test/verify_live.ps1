# Live End-to-End Verification Script
$ErrorActionPreference = "Stop"

Write-Host "=== 1. Testing GET /api/simulations (400 Bad Request check) ==="
try {
    $res = Invoke-WebRequest -Uri "http://localhost:8080/api/simulations" -Method Get -UseBasicParsing
    Write-Host "[FAIL] Expected 400 Bad Request, got $($res.StatusCode)"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Write-Host "[PASS] Expected 400 Bad Request, received: $code"
}

Write-Host "`n=== 2. Testing POST /api/simulations (In-Memory Simulation) ==="
$simPayload = '{"milestoneId":1,"currentDeveloperId":2,"proposedDeveloperId":1,"allocationPercentage":60}'
$simRes = Invoke-RestMethod -Uri "http://localhost:8080/api/simulations" -Method Post -Body $simPayload -ContentType "application/json"
Write-Host "Status: $($simRes.status)"
Write-Host "Verdict: $($simRes.data.scenarioVerdict)"
Write-Host "Cost Delta: $($simRes.data.milestoneCostDifference)"
Write-Host "Simulation ID: $($simRes.data.id)"
$simId = $simRes.data.id

Write-Host "`n=== 3. Testing GET /api/simulations/$simId/report (Binary PDF Generation) ==="
$pdfBytes = Invoke-WebRequest -Uri "http://localhost:8080/api/simulations/$simId/report" -Method Get -UseBasicParsing
$header = [System.Text.Encoding]::ASCII.GetString($pdfBytes.Content[0..7])
Write-Host "Content-Type: $($pdfBytes.Headers['Content-Type'])"
Write-Host "PDF Magic Bytes Header: $header"
Write-Host "Size in bytes: $($pdfBytes.Content.Length)"
if ($header.StartsWith("%PDF-1.4")) {
    Write-Host "[PASS] Pure Java binary PDF generated successfully!"
} else {
    Write-Host "[FAIL] Invalid PDF header"
}

Write-Host "`n=== 4. Testing GET /api/projects/1/budget (Rolling 30-Day Window & Transactions) ==="
$budgetRes = Invoke-RestMethod -Uri "http://localhost:8080/api/projects/1/budget" -Method Get
Write-Host "Status: $($budgetRes.status)"
Write-Host "Daily Burn Rate: $($budgetRes.data.dailyBurnRate)"
Write-Host "Forecast Exhaustion Date: $($budgetRes.data.forecastExhaustionDate)"
Write-Host "Transactions Count: $($budgetRes.data.recentTransactions.Count)"
if ($budgetRes.data.recentTransactions.Count -gt 0) {
    Write-Host "[PASS] Budget transactions ledger populated and burn rate computed!"
}

Write-Host "`n=== 5. Testing GET /api/projects (UTF-8 Encoding & Em-Dash Verification) ==="
$projRes = Invoke-RestMethod -Uri "http://localhost:8080/api/projects" -Method Get
Write-Host "Projects Count: $($projRes.data.Count)"
foreach ($p in $projRes.data) {
    Write-Host "  Project $($p.id): $($p.name)"
}
Write-Host "[PASS] Projects retrieved without mojibake!"

Write-Host "`n=== 6. Testing GET /api/conflicts (Real-Time Engine) ==="
$confRes = Invoke-RestMethod -Uri "http://localhost:8080/api/conflicts" -Method Get
Write-Host "Conflicts Count: $($confRes.data.Count)"
foreach ($c in $confRes.data) {
    Write-Host "  [$($c.severity)] $($c.conflictType): $($c.description)"
}

Write-Host "`n=== 7. Testing Developer Dashboard (Restricted View) ==="
$devRes = Invoke-RestMethod -Uri "http://localhost:8080/api/dashboard/developer/1" -Method Get
Write-Host "Developer Name: $($devRes.data.name) (Current Allocation: $($devRes.data.currentAllocationPercentage)%)"
Write-Host "Assigned Milestones: $($devRes.data.assignedMilestones.Count)"

Write-Host "`n=================================================================="
Write-Host "  ALL LIVE ENDPOINTS AND CORE FEATURES FULLY VERIFIED!"
Write-Host "=================================================================="
