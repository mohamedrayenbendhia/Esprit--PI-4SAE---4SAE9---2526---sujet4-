# Create portfolios for all freelancers
$h1 = @{"Authorization"="Bearer $global:T1";"Content-Type"="application/json"}
$h2 = @{"Authorization"="Bearer $global:T2";"Content-Type"="application/json"}
$h3 = @{"Authorization"="Bearer $global:T3";"Content-Type"="application/json"}

# F1 Portfolio
$p1 = @"
{
  "headline": "Expert Angular & Java Developer",
  "linkedinUrl": "https://linkedin.com/in/sarah-developer",
  "githubUrl": "https://github.com/sarah-dev",
  "location": "Paris, France",
  "isPublic": true,
  "projects": [
    {
      "title": "Enterprise Dashboard",
      "description": "Built comprehensive Angular dashboard for enterprise clients",
      "techStack": "Angular, TypeScript, NgRx, Material Design",
      "startDate": "2023-01-15",
      "endDate": "2024-01-15",
      "githubUrl": "https://github.com/sarah-dev/enterprise-dashboard",
      "demoUrl": "https://dashboard-demo.com"
    }
  ]
}
"@

# F2 Portfolio
$p2 = @"
{
  "headline": "Spring Boot & Python Specialist",
  "linkedinUrl": "https://linkedin.com/in/alex-morgan",
  "githubUrl": "https://github.com/alex-morgan",
  "location": "Lyon, France",
  "isPublic": true,
  "projects": [
    {
      "title": "Microservices Architecture",
      "description": "Designed and implemented microservices with Spring Boot",
      "techStack": "Spring Boot, Docker, Kubernetes, PostgreSQL",
      "startDate": "2022-06-01",
      "endDate": "2023-12-31",
      "githubUrl": "https://github.com/alex-morgan/microservices"
    }
  ]
}
"@

# F3 Portfolio
$p3 = @"
{
  "headline": "Java & AWS Cloud Architect",
  "linkedinUrl": "https://linkedin.com/in/lina-tech",
  "githubUrl": "https://github.com/lina-tech",
  "location": "Marseille, France",
  "isPublic": true,
  "projects": [
    {
      "title": "Cloud Migration Project",
      "description": "Led cloud migration from on-premise to AWS",
      "techStack": "Java, AWS, Node.js, MongoDB",
      "startDate": "2021-09-01",
      "endDate": "2023-06-30",
      "demoUrl": "https://cloud-project.com"
    }
  ]
}
"@

Write-Host "Creating F1 portfolio..."
try {
  Invoke-RestMethod -Uri "http://localhost:8080/api/freelancer/portfolios" -Method POST -Headers $h1 -Body $p1 -TimeoutSec 20 | Out-Null
  Write-Host "  OK"
} catch {
  Write-Host "  ERR or exists"
}

Write-Host "Creating F2 portfolio..."
try {
  Invoke-RestMethod -Uri "http://localhost:8080/api/freelancer/portfolios" -Method POST -Headers $h2 -Body $p2 -TimeoutSec 20 | Out-Null
  Write-Host "  OK"
} catch {
  Write-Host "  ERR or exists"
}

Write-Host "Creating F3 portfolio..."
try {
  Invoke-RestMethod -Uri "http://localhost:8080/api/freelancer/portfolios" -Method POST -Headers $h3 -Body $p3 -TimeoutSec 20 | Out-Null
  Write-Host "  OK"
} catch {
  Write-Host "  ERR or exists"
}

Write-Host ""
Write-Host "Checking portfolios..."
$portfolios = Invoke-RestMethod -Uri "http://localhost:8080/api/client/portfolios" -Method GET -TimeoutSec 10
Write-Host "Total portfolios:" $portfolios.Count
