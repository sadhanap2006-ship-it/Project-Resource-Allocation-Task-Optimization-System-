$l = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -Body '{"email":"pm@prs.local","password":"password123"}' -ContentType 'application/json'
$token = $l.data.token
$h = @{ "Authorization" = "Bearer $token" }
try {
    $me = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/me' -Method Get -Headers $h
    Write-Host "Auth me SUCCESS: Role=$($me.data.role), Name=$($me.data.name)"
} catch {
    Write-Host "Auth me FAIL: $($_.Exception.Message)"
}
