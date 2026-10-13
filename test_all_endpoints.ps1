$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$login = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -Body '{"email":"pm@prs.local","password":"password123"}' -ContentType 'application/json' -WebSession $session
$token = $login.data.token
$headers = @{ 'Authorization' = "Bearer $token" }

Write-Host "=== TEST /api/projects ==="
try {
    $projects = Invoke-RestMethod -Uri 'http://localhost:8080/api/projects' -Method Get -Headers $headers -WebSession $session
    Write-Host "Projects: $($projects.data.Count)"
} catch {
    Write-Host "Error on /api/projects: $($_.Exception.Message)"
}

Write-Host "`n=== TEST /api/milestones ==="
try {
    $milestones = Invoke-RestMethod -Uri 'http://localhost:8080/api/milestones' -Method Get -Headers $headers -WebSession $session
    Write-Host "Milestones: $($milestones.data.Count)"
} catch {
    Write-Host "Error on /api/milestones: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        Write-Host "Response Body: $($reader.ReadToEnd())"
    }
}

Write-Host "`n=== TEST /api/resources ==="
try {
    $resources = Invoke-RestMethod -Uri 'http://localhost:8080/api/resources' -Method Get -Headers $headers -WebSession $session
    Write-Host "Resources: $($resources.data.Count)"
} catch {
    Write-Host "Error on /api/resources: $($_.Exception.Message)"
}

Write-Host "`n=== TEST /api/allocations ==="
try {
    $allocs = Invoke-RestMethod -Uri 'http://localhost:8080/api/allocations' -Method Get -Headers $headers -WebSession $session
    Write-Host "Allocations: $($allocs.data.Count)"
} catch {
    Write-Host "Error on /api/allocations: $($_.Exception.Message)"
}

Write-Host "`n=== TEST /api/dashboard/pm ==="
try {
    $dash = Invoke-RestMethod -Uri 'http://localhost:8080/api/dashboard/pm' -Method Get -Headers $headers -WebSession $session
    Write-Host "PM Dashboard active projects: $($dash.data.activeProjects)"
} catch {
    Write-Host "Error on /api/dashboard/pm: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        Write-Host "Response Body: $($reader.ReadToEnd())"
    }
}
