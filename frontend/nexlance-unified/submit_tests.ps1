# Submit tests for all freelancers
$h1 = @{Authorization="Bearer $($global:T1)"; "Content-Type"="application/json"}
$h2 = @{Authorization="Bearer $($global:T2)"; "Content-Type"="application/json"}
$h3 = @{Authorization="Bearer $($global:T3)"; "Content-Type"="application/json"}

# F1 - Angular test (userSkillId=1, testId=1)
Write-Host "Submitting F1 Angular test..."
try {
  $body = '{"userSkillId":1,"answers":["ng generate component","@Component","HttpClientModule","[(ngModel)]","ngOnInit"]}'
  $r = Invoke-RestMethod -Uri "http://localhost:8080/api/freelancer/tests/submit" -Method POST -Headers $h1 -Body $body -TimeoutSec 20
  Write-Host "  ✓ F1 Angular: score=$($r.score) passed=$($r.isPassed)"
} catch {
  Write-Host "  × F1 Angular: $($_.Exception.Message)"
}

# F1 - Java test (userSkillId=2, testId=2)
Write-Host "Submitting F1 Java test..."
try {
  $body = '{"userSkillId":2,"answers":["0","extends","4 bytes"]}'
  $r = Invoke-RestMethod -Uri "http://localhost:8080/api/freelancer/tests/submit" -Method POST -Headers $h1 -Body $body -TimeoutSec 20
  Write-Host "  ✓ F1 Java: score=$($r.score) passed=$($r.isPassed)"
} catch {
  Write-Host "  × F1 Java: $($_.Exception.Message)"
}

# F2 - Spring Boot test (userSkillId=5, testId=3)
Write-Host "Submitting F2 Spring Boot test..."
try {
  $body = '{"userSkillId":5,"answers":["@SpringBootApplication","8080","@RestController"]}'
  $r = Invoke-RestMethod -Uri "http://localhost:8080/api/freelancer/tests/submit" -Method POST -Headers $h2 -Body $body -TimeoutSec 20
  Write-Host "  ✓ F2 Spring Boot: score=$($r.score) passed=$($r.isPassed)"
} catch {
  Write-Host "  × F2 Spring Boot: $($_.Exception.Message)"
}

# F2 - Python test (userSkillId=6, testId=4)
Write-Host "Submitting F2 Python test..."
try {
  $body = @'
{"userSkillId":6,"answers":["print()","def",".py"]}
'@
  $r = Invoke-RestMethod -Uri "http://localhost:8080/api/freelancer/tests/submit" -Method POST -Headers $h2 -Body $body -TimeoutSec 20
  Write-Host "  ✓ F2 Python: score=$($r.score) passed=$($r.isPassed)"
} catch {
  Write-Host "  × F2 Python: $($_.Exception.Message)"
}

# F3 - Java test (userSkillId=8, testId=2)
Write-Host "Submitting F3 Java test..."
try {
  $body = '{"userSkillId":8,"answers":["0","extends","4 bytes"]}'
  $r = Invoke-RestMethod -Uri "http://localhost:8080/api/freelancer/tests/submit" -Method POST -Headers $h3 -Body $body -TimeoutSec 20
  Write-Host "  ✓ F3 Java: score=$($r.score) passed=$($r.isPassed)"
} catch {
  Write-Host "  × F3 Java: $($_.Exception.Message)"
}

Write-Host ""
Write-Host "Done! Checking certifications..."
$certs = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/certifications" -Method GET -TimeoutSec 10
Write-Host "Total certifications created:" $certs.Count
