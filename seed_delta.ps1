$l = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -Body '{"email":"pm@prs.local","password":"password123"}' -ContentType 'application/json'
$token = $l.data.token
$headers = @{ "Authorization" = "Bearer $token" }

# Check if DELTA already exists
$projects = Invoke-RestMethod -Uri 'http://localhost:8080/api/projects' -Method Get -Headers $headers
$delta = $projects.data | Where-Object { $_.code -eq 'DELTA' }

if (-not $delta) {
    $newP = '{"code":"DELTA","name":"Mobile Banking Gateway","description":"PCI-DSS certified mobile payment gateway API","startDate":"2026-07-01","endDate":"2026-10-15","totalBudget":350000}'
    $p = Invoke-RestMethod -Uri 'http://localhost:8080/api/projects' -Method Post -Body $newP -ContentType 'application/json' -Headers $headers
    $deltaId = $p.data.id
    Write-Host "Created DELTA project with ID $deltaId"
} else {
    $deltaId = $delta.id
    Write-Host "DELTA project already exists with ID $deltaId"
}

# Create 2 completed milestones for DELTA so it is 100% complete
$m1 = @{
    name = "DELTA M1 Core Security Framework"
    description = "OAuth2 and HSM integration"
    startDate = "2026-07-05"
    endDate = "2026-08-30"
    estimatedEffortHours = 200
    skills = @(
        @{ skillId = 1; minProficiency = 2 }
    )
} | ConvertTo-Json

$m2 = @{
    name = "DELTA M2 Production Compliance"
    description = "PCI-DSS certification audit and deployment"
    startDate = "2026-09-01"
    endDate = "2026-10-15"
    estimatedEffortHours = 180
    skills = @(
        @{ skillId = 2; minProficiency = 2 }
    )
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/milestones/9/progress" -Method Patch -Body '{"progressPct": 100}' -ContentType 'application/json' -Headers $headers
    Write-Host "Updated M9 to 100%"
} catch {
    Write-Host "M9 error: $($_.Exception.Message)"
}

try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/milestones/10/progress" -Method Patch -Body '{"progressPct": 100}' -ContentType 'application/json' -Headers $headers
    Write-Host "Updated M10 to 100%"
} catch {
    Write-Host "M10 error: $($_.Exception.Message)"
}

# Also ensure Project 4 has milestones or 0% progress
try {
    $p4Milestones = Invoke-RestMethod -Uri "http://localhost:8080/api/projects/4/milestones" -Method Get -Headers $headers
    Write-Host "Project 4 milestones count: $($p4Milestones.data.Count)"
} catch {
    Write-Host "P4 check error: $($_.Exception.Message)"
}

Write-Host "Done setting up project data."
