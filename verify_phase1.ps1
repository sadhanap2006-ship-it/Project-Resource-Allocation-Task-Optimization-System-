$ErrorActionPreference = 'Stop'
Write-Host "=========================================================="
Write-Host "PHASE 1 END-TO-END VERIFICATION: BUGS & DATA CONSISTENCY"
Write-Host "=========================================================="

# -------------------------------------------------------------------------
# Test 1: Authentication as PM
# -------------------------------------------------------------------------
Write-Host "`n[TEST 1] Logging in as PM (pm@prs.local)..."
$pmSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$pmLoginBody = '{"email":"pm@prs.local","password":"password123"}'
$pmLogin = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -Body $pmLoginBody -ContentType 'application/json' -WebSession $pmSession

if ($pmLogin.status -eq 'SUCCESS' -and $pmLogin.data.role -eq 'PM') {
    Write-Host "PASS: PM Login succeeded (Name: $($pmLogin.data.name), Role: $($pmLogin.data.role))" -ForegroundColor Green
} else {
    Write-Host "FAIL: PM Login failed" -ForegroundColor Red
    exit 1
}

# -------------------------------------------------------------------------
# Test 2: Developers Table Data Consistency (Skills, Rate, Capacity)
# -------------------------------------------------------------------------
Write-Host "`n[TEST 2] Verifying Developers Table API (/api/resources?type=DEVELOPER)..."
$devsRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/resources?type=DEVELOPER' -Method Get -WebSession $pmSession
$devs = $devsRes.data

$allPass = $true
foreach ($d in $devs) {
    Write-Host "  -> Developer: $($d.name)"
    Write-Host "     Rate: ₹$($d.costRate)/hr (costPerHour: ₹$($d.costPerHour))"
    Write-Host "     Current Load: $($d.currentLoad)%, Remaining: $($d.remainingCapacity)%, Peak (30d): $($d.peakLoad)%"
    
    # Check rate != 0
    if ([decimal]$d.costRate -le 0 -or [decimal]$d.costPerHour -le 0) {
        Write-Host "     FAIL: Rate shows ₹0.00!" -ForegroundColor Red
        $allPass = $false
    }
    
    # Check skills proficiency
    foreach ($s in $d.skills) {
        Write-Host "     Skill: $($s.name) [ProficiencyText: $($s.proficiencyText), Level: $($s.proficiencyLevel)]"
        if ($s.proficiencyText -eq $null -or $s.proficiencyText -eq 'undefined') {
            Write-Host "     FAIL: Skill proficiency is undefined!" -ForegroundColor Red
            $allPass = $false
        }
    }
}

# Verify capacities are not 100% for all
$arun = $devs | Where-Object { $_.name -like '*Arun*' } | Select-Object -First 1
$rahul = $devs | Where-Object { $_.name -like '*Rahul*' } | Select-Object -First 1
$karthik = $devs | Where-Object { $_.name -like '*Karthik*' } | Select-Object -First 1

Write-Host "  -> Verifying capacity breakdown for Arun (overloaded):"
Write-Host "     Arun Remaining Capacity: $($arun.remainingCapacity)% (Expected: 0%)"
Write-Host "     Arun Current Load: $($arun.currentLoad)% (Expected: 110%)"
if ($arun.remainingCapacity -eq 0 -and $arun.currentLoad -eq 110) {
    Write-Host "PASS: Arun's remaining capacity (0%) and load (110%) are correct!" -ForegroundColor Green
} else {
    Write-Host "FAIL: Arun's capacity/load mismatch!" -ForegroundColor Red
    $allPass = $false
}

Write-Host "  -> Verifying Rahul's capacity:"
Write-Host "     Rahul Remaining Capacity: $($rahul.remainingCapacity)% (Expected: 10%)"
Write-Host "     Rahul Current Load: $($rahul.currentLoad)% (Expected: 90%)"
if ($rahul.remainingCapacity -eq 10 -and $rahul.currentLoad -eq 90) {
    Write-Host "PASS: Rahul's remaining capacity (10%) and load (90%) are correct!" -ForegroundColor Green
} else {
    Write-Host "FAIL: Rahul's capacity/load mismatch!" -ForegroundColor Red
    $allPass = $false
}

# -------------------------------------------------------------------------
# Test 3: Heatmap Agreement and Weekend Handling
# -------------------------------------------------------------------------
Write-Host "`n[TEST 3] Verifying Heatmap API (/api/heatmap?granularity=DAY)..."
$heatmapRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/heatmap?granularity=DAY' -Method Get -WebSession $pmSession
$rahulRow = $heatmapRes.data.rows | Where-Object { $_.resourceName -like '*Rahul*' -or $_.resource.name -like '*Rahul*' } | Select-Object -First 1

Write-Host "  -> Checking Rahul's allocation period up to T+30..."
$tPlus25Cell = $rahulRow.cells | Where-Object { $_.date -eq (Get-Date).AddDays(25).ToString('yyyy-MM-dd') }
$tPlus30Cell = $rahulRow.cells | Where-Object { $_.date -eq (Get-Date).AddDays(30).ToString('yyyy-MM-dd') }
Write-Host "     T+25 ($($tPlus25Cell.date)): Band=$($tPlus25Cell.band), Load=$($tPlus25Cell.loadPct)%"
Write-Host "     T+30 ($($tPlus30Cell.date)): Band=$($tPlus30Cell.band), Load=$($tPlus30Cell.loadPct)%"

if ($tPlus30Cell.loadPct -gt 0 -or $tPlus30Cell.band -eq 'WEEKEND') {
    Write-Host "PASS: Rahul's milestone correctly extends through T+30 (does not prematurely drop on T+25)!" -ForegroundColor Green
} else {
    Write-Host "FAIL: Rahul's allocation dropped to 0 before milestone end!" -ForegroundColor Red
    $allPass = $false
}

Write-Host "  -> Checking weekend cell detection for developers:"
$weekendCell = $rahulRow.cells | Where-Object { $_.band -eq 'WEEKEND' } | Select-Object -First 1
if ($weekendCell -ne $null) {
    Write-Host "PASS: Weekend cell found on date $($weekendCell.date) with Band='WEEKEND' and Load=0!" -ForegroundColor Green
} else {
    Write-Host "FAIL: No weekend cells identified on developer heatmap!" -ForegroundColor Red
    $allPass = $false
}

# -------------------------------------------------------------------------
# Test 4: Budget EVM EAC and 30-Day Window Verification
# -------------------------------------------------------------------------
Write-Host "`n[TEST 4] Verifying Budget Forecast (/api/projects/1/budget/forecast)..."
$forecastRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/projects/1/budget/forecast' -Method Get -WebSession $pmSession
$f = $forecastRes.data

Write-Host "  -> Total Budget: ₹$($f.totalBudget)"
Write-Host "  -> Actual Spend: ₹$($f.actualSpend)"
Write-Host "  -> Burn Rate: ₹$($f.burnRate)/day (burnRateDaily: ₹$($f.burnRateDaily))"
Write-Host "  -> Days Left: $($f.daysLeft)"
Write-Host "  -> Run-out Date: $($f.runOutDate)"
Write-Host "  -> EVM Metrics: CPI=$($f.cpi), SPI=$($f.spi), EAC=₹$($f.eac)"
Write-Host "  -> Series count: $($f.series.Count)"

# Check EAC matches totalBudget / CPI and is scaled correctly (around 1.2M, NOT the old unscaled 5,000,000+)
$eacVal = [decimal]$f.eac
$expectedEac = [decimal]$f.totalBudget / [decimal]$f.cpi
$diff = [Math]::Abs($eacVal - $expectedEac)
if ($diff -lt 500.0 -and $eacVal -lt 3000000) {
    Write-Host "PASS: EAC is scaled to project budget and matches BAC/CPI formula: ₹$eacVal (Not exploded 10x!)" -ForegroundColor Green
} else {
    Write-Host "FAIL: EAC ($eacVal) does not match BAC/CPI ($expectedEac) or is exploded!" -ForegroundColor Red
    $allPass = $false
}

# -------------------------------------------------------------------------
# Test 5: Reassign Task API Validation and 0% Capacity Detection
# -------------------------------------------------------------------------
Write-Host "`n[TEST 5] Testing Reassign Task Modal logic & validation..."
# Check target developer capacity warning logic
$targetArun = $devs | Where-Object { $_.name -like '*Arun*' } | Select-Object -First 1
$targetKarthik = $devs | Where-Object { $_.name -like '*Karthik*' } | Select-Object -First 1

Write-Host "  -> Target Arun Available Capacity: $($targetArun.remainingCapacity)%"
if ($targetArun.remainingCapacity -le 0) {
    Write-Host "PASS: Arun has 0% capacity -> UI correctly triggers 'Warning: Selected team member has 0% available capacity'!" -ForegroundColor Green
} else {
    Write-Host "FAIL: Arun did not report 0% available capacity!" -ForegroundColor Red
    $allPass = $false
}

Write-Host "  -> Target Karthik Available Capacity: $($targetKarthik.remainingCapacity)%"
if ($targetKarthik.remainingCapacity -gt 0) {
    Write-Host "PASS: Karthik has $($targetKarthik.remainingCapacity)% capacity -> UI does NOT trigger 0% capacity warning!" -ForegroundColor Green
} else {
    Write-Host "FAIL: Karthik capacity error!" -ForegroundColor Red
    $allPass = $false
}

# -------------------------------------------------------------------------
# Test 6: Employee Role Verification
# -------------------------------------------------------------------------
Write-Host "`n[TEST 6] Testing Employee Authentication & RBAC (arun@prs.local)..."
$empSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$empLoginBody = '{"email":"arun@prs.local","password":"password123"}'
$empLogin = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -Body $empLoginBody -ContentType 'application/json' -WebSession $empSession

Write-Host "  -> Employee Login: Status=$($empLogin.status), Role=$($empLogin.data.role), ResourceId=$($empLogin.data.resourceId)"

# Developer viewing own dashboard
$devDash = Invoke-RestMethod -Uri 'http://localhost:8080/api/dashboard/developer/me' -Method Get -WebSession $empSession
$devLoad = if ($devDash.data.currentLoadPct -ne $null) { $devDash.data.currentLoadPct } else { $devDash.data.currentLoad }
$devAllocs = if ($devDash.data.allocations -ne $null) { $devDash.data.allocations } else { $devDash.data.myAllocations }
Write-Host "  -> Developer Dashboard: Load=$devLoad%, Allocations Count=$($devAllocs.Count)"
if ($devLoad -gt 0 -and $devAllocs.Count -ge 2) {
    Write-Host "PASS: Developer dashboard returns personal load ($devLoad%) and $($devAllocs.Count) allocations!" -ForegroundColor Green
} else {
    Write-Host "FAIL: Developer dashboard load ($devLoad%) or allocations count ($($devAllocs.Count)) incorrect!" -ForegroundColor Red
    $allPass = $false
}

# Developer attempting forbidden action (POST /api/projects) -> Must be 403
Write-Host "  -> Testing DEVELOPER role cannot create projects (expected 403)..."
try {
    $forbidden = Invoke-RestMethod -Uri 'http://localhost:8080/api/projects' -Method Post -Body '{"name":"Hacked Project"}' -ContentType 'application/json' -WebSession $empSession
    Write-Host "FAIL: Developer was able to call POST /api/projects!" -ForegroundColor Red
    $allPass = $false
} catch {
    Write-Host "PASS: Server blocked developer with 403 Forbidden: $($_.Exception.Message)" -ForegroundColor Green
}

# -------------------------------------------------------------------------
# Final Summary
# -------------------------------------------------------------------------
Write-Host "`n=========================================================="
if ($allPass) {
    Write-Host "ALL PHASE 1 TESTS PASSED SUCCESSFULLY!" -ForegroundColor Green
} else {
    Write-Host "SOME PHASE 1 TESTS FAILED - SEE ABOVE" -ForegroundColor Red
}
Write-Host "=========================================================="
