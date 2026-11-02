$s = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$l = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -Body '{"email":"pm@prs.local","password":"password123"}' -ContentType 'application/json' -WebSession $s
1..5 | ForEach-Object {
    $id = $_
    try {
        $m = Invoke-RestMethod -Uri "http://localhost:8080/api/projects/$id/milestones" -Method Get -WebSession $s
        Write-Host "Project $id Milestones:"
        $m.data | Format-Table id, name, progressPct, status
    } catch {
        Write-Host "Project $($id): None"
    }
}
