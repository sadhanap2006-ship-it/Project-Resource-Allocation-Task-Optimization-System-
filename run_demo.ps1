$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host "   PROJECT RESOURCE ALLOCATION & TASK OPTIMIZATION SYSTEM (PRS)" -ForegroundColor Cyan
Write-Host "========================================================================" -ForegroundColor Cyan

# 1. System Health
$health = Invoke-RestMethod -Uri 'http://localhost:8080/api/actuator/health' -Method Get
Write-Host "[1] SYSTEM HEALTH: $($health.status) | MySQL DB: $($health.components.db.status) | Disk: $($health.components.diskSpace.status)" -ForegroundColor Green

# 2. PM Authentication
$pmLogin = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -Body '{"email":"pm@prs.local","password":"password123"}' -ContentType 'application/json' -WebSession $session
Write-Host "`n[2] PM AUTHENTICATION & SECURITY:" -ForegroundColor Yellow
Write-Host "    User Profile : $($pmLogin.data.name) ($($pmLogin.data.email))"
Write-Host "    Assigned Role: $($pmLogin.data.role) (RBAC: Full Portfolio Authority)"
Write-Host "    Token Format : Stateless HMAC-SHA256 JWT Bearer"
Write-Host "    Bearer Token : $($pmLogin.data.token.Substring(0, 48))..."

# 3. Portfolio Dashboard KPIs
$pmDash = Invoke-RestMethod -Uri 'http://localhost:8080/api/dashboard/pm' -Method Get -WebSession $session
Write-Host "`n[3] PORTFOLIO & CAPACITY KPIS:" -ForegroundColor Yellow
Write-Host "    Total Projects     : $($pmDash.data.totalProjects)"
Write-Host "    Active Projects    : $($pmDash.data.activeProjects)"
Write-Host "    At-Risk Projects   : $($pmDash.data.atRiskProjects)"
Write-Host "    Average Workload   : $($pmDash.data.averageUtilisation)%"
Write-Host "    Skill Gaps Detected: $($pmDash.data.skillGapsCount)"

# 4. Project Health & Burn Rate Forecasting
Write-Host "`n[4] PROJECT DELIVERY & FINANCIAL FORECASTING (EVM & Burn Rate):" -ForegroundColor Yellow
$pmDash.data.projects | ForEach-Object {
    $ro = if ($_.runOutDate) { $_.runOutDate } else { "Within Planned Budget" }
    Write-Host "    - [$($_.code)] $($_.name)"
    Write-Host "      Health: $($_.health) | Progress: $($_.progressPct)% | Budget Spent: $($_.budgetUtilisationPct)% | Run-out: $ro"
}

# 5. Conflict Resolution Center
Write-Host "`n[5] CONFLICT RESOLUTION CENTER (Sweep-Line Engine):" -ForegroundColor Yellow
$conflicts = Invoke-RestMethod -Uri 'http://localhost:8080/api/conflicts' -Method Get -WebSession $session
$critCount = ($conflicts.data | Where-Object { $_.severity -eq 'CRITICAL' }).Count
$warnCount = ($conflicts.data | Where-Object { $_.severity -eq 'WARNING' }).Count
Write-Host "    Total Open Conflicts: $($conflicts.data.Count) (Critical: $critCount, Warning: $warnCount)"
$conflicts.data | Select-Object -First 4 | ForEach-Object {
    Write-Host "    * [$($_.severity)] $($_.type) -> $($_.description)"
}

# 6. What-If Simulation Engine
Write-Host "`n[6] WHAT-IF SCENARIO SIMULATION (In-Memory Zero-DB-Mutation):" -ForegroundColor Yellow
$allocs = Invoke-RestMethod -Uri 'http://localhost:8080/api/allocations' -Method Get -WebSession $session
$resources = Invoke-RestMethod -Uri 'http://localhost:8080/api/resources' -Method Get -WebSession $session
$arun = $resources.data | Where-Object { $_.name -like '*Arun*' } | Select-Object -First 1
$karthik = $resources.data | Where-Object { $_.name -like '*Karthik*' } | Select-Object -First 1
$targetAlloc = $allocs.data | Where-Object { $_.resourceId -eq $arun.id -and $_.allocationPct -eq 50 } | Select-Object -First 1

$simBody = @{
    projectId = 1
    changes = @(
        @{
            type = 'SWAP'
            allocationId = $targetAlloc.id
            fromResourceId = $arun.id
            toResourceId = $karthik.id
        }
    )
} | ConvertTo-Json -Depth 5

$cookies = $session.Cookies.GetCookies('http://localhost:8080')
$xsrf = ($cookies | Where-Object { $_.Name -eq 'XSRF-TOKEN' }).Value
$headers = @{}
if ($xsrf) { $headers['X-XSRF-TOKEN'] = $xsrf }

$simRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/simulations/run' -Method Post -Body $simBody -ContentType 'application/json' -WebSession $session -Headers $headers
Write-Host "    Hypothetical Action: Swap Arun Kumar -> Karthik Raman on Project Alpha"
Write-Host "    Simulation Engine  : $($simRes.data.recommendation) ($($simRes.data.recommendationReason))"
Write-Host "    Conflict Delta     : Removed $($simRes.data.conflictsRemoved.Count) conflict(s) | Added $($simRes.data.conflictsAdded.Count) conflict(s)"
Write-Host "    Plan Financials    : Baseline Plan ₹$($simRes.data.baselinePlanTotal) | Simulated Plan ₹$($simRes.data.simulatedPlanTotal)"

# 7. Developer Portal
Write-Host "`n[7] DEVELOPER WORKSPACE (Arun Kumar):" -ForegroundColor Yellow
$devLogin = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -Body '{"email":"arun@prs.local","password":"password123"}' -ContentType 'application/json' -WebSession $session
$devDash = Invoke-RestMethod -Uri 'http://localhost:8080/api/dashboard/developer/me' -Method Get -WebSession $session
Write-Host "    Assigned Engineer  : $($devDash.data.resourceName)"
Write-Host "    Current Workload   : $($devDash.data.currentLoadPct)% (Peak 30-Day: $($devDash.data.peakLoad30DaysPct)%)"
Write-Host "    Available Capacity : $($devDash.data.remainingCapacityPct)%"
Write-Host "    Active Milestones  : $($devDash.data.milestones.Count)"
Write-Host "    Unread Alerts      : $($devDash.data.unreadNotificationsCount)"

Write-Host "`n========================================================================" -ForegroundColor Cyan
Write-Host "   SYSTEM DEMONSTRATION COMPLETE - APPLICATION LIVE AT http://localhost:8080" -ForegroundColor Cyan
Write-Host "========================================================================" -ForegroundColor Cyan
