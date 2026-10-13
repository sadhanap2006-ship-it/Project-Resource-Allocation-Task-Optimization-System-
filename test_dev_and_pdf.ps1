$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$loginBody = '{"email":"arun@prs.local","password":"password123"}'
$login = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -Body $loginBody -ContentType 'application/json' -WebSession $session

Write-Host "=== 1. DEVELOPER LOGIN ==="
$login.data | Format-List

Write-Host "=== 2. DEVELOPER DASHBOARD ==="
$devDash = Invoke-RestMethod -Uri 'http://localhost:8080/api/dashboard/developer/me' -Method Get -WebSession $session
$devDash.data | Select-Object resourceName, currentLoadPct, peakLoad30DaysPct, remainingCapacityPct, unreadNotificationsCount | Format-List
Write-Host "Allocations count: " $devDash.data.allocations.Count
Write-Host "Milestones count: " $devDash.data.milestones.Count
Write-Host "Weekly Load (8 weeks) count: " $devDash.data.weeklyLoad8Weeks.Count
Write-Host "Open Conflicts count: " $devDash.data.openConflicts.Count
Write-Host "Leave Requests count: " $devDash.data.leaveRequests.Count
