$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$loginBody = '{"email":"pm@prs.local","password":"password123"}'
$login = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -Body $loginBody -ContentType 'application/json' -WebSession $session
Write-Host "=== 1. LOGIN RESULT ==="
$login.data | Format-List

Write-Host "=== 2. PM DASHBOARD KPI ==="
$dash = Invoke-RestMethod -Uri 'http://localhost:8080/api/dashboard/pm' -Method Get -WebSession $session
$dash.data | Select-Object totalProjects, activeProjects, atRiskProjects, averageUtilisation, skillGapsCount | Format-List

Write-Host "=== 3. PROJECT HEALTH ROWS ==="
$dash.data.projects | Format-Table id, code, name, health, progressPct, budgetUtilisationPct, runOutDate -AutoSize

Write-Host "=== 4. OPEN CONFLICTS SUMMARY ==="
$dash.data.openConflictsBySeverity | Format-List

Write-Host "=== 5. WHAT-IF SIMULATION (SWAP DEMO) ==="
$allocs = Invoke-RestMethod -Uri 'http://localhost:8080/api/allocations' -Method Get -WebSession $session
$resources = Invoke-RestMethod -Uri 'http://localhost:8080/api/resources' -Method Get -WebSession $session
$arun = $resources.data | Where-Object { $_.name -like '*Arun*' } | Select-Object -First 1
$karthik = $resources.data | Where-Object { $_.name -like '*Karthik*' } | Select-Object -First 1
$targetAlloc = $allocs.data | Where-Object { $_.resourceId -eq $arun.id -and $_.allocationPct -eq 50 } | Select-Object -First 1

$simBody = @{
    projectId = 1
    changes = @(
        @{
            type = "SWAP"
            allocationId = $targetAlloc.id
            fromResourceId = $arun.id
            toResourceId = $karthik.id
        }
    )
} | ConvertTo-Json -Depth 5

$cookies = $session.Cookies.GetCookies("http://localhost:8080")
$xsrf = ($cookies | Where-Object { $_.Name -eq 'XSRF-TOKEN' }).Value
$headers = @{}
if ($xsrf) {
    $headers['X-XSRF-TOKEN'] = $xsrf
}

$simRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/simulations/run' -Method Post -Body $simBody -ContentType 'application/json' -WebSession $session -Headers $headers
$simRes.data | Select-Object projectId, recommendation, recommendationReason, baselinePlanTotal, simulatedPlanTotal | Format-List
Write-Host "Conflicts Removed count: " $simRes.data.conflictsRemoved.Count
Write-Host "Conflicts Added count: " $simRes.data.conflictsAdded.Count
