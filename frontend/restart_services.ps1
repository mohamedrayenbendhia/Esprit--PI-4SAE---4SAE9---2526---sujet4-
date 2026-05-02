# =================================================
# Script de Redémarrage des Services NexLance
# Après corrections des interfaces
# =================================================

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "Redémarrage des Services NexLance" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

$workspace = "c:\Users\Cyrine\Downloads\PI DEV 4 EME\module_job_offers\Micro_services_benchikh_sirine"
Set-Location $workspace

# =================================================
# Fonction pour rebuild un service Spring Boot
# =================================================
function Rebuild-Service {
    param(
        [string]$ServicePath,
        [string]$ServiceName
    )
    
    Write-Host "[$ServiceName] Nettoyage et compilation..." -ForegroundColor Yellow
    Set-Location "$workspace\$ServicePath"
    
    # Nettoyer
    .\mvnw clean | Out-Null
    
    # Compiler
    $result = .\mvnw install -DskipTests 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[$ServiceName] ✓ Compilation réussie" -ForegroundColor Green
        return $true
    } else {
        Write-Host "[$ServiceName] ✗ Erreur de compilation" -ForegroundColor Red
        Write-Host $result -ForegroundColor Red
        return $false
    }
}

# =================================================
# Étape 1: Recompiler les services modifiés
# =================================================

Write-Host ""
Write-Host "Étape 1: Recompilation des services modifiés" -ForegroundColor Cyan
Write-Host "--------------------------------------------" -ForegroundColor Cyan

$servicesToRebuild = @(
    @{Path="module_certification"; Name="Module Certification"},
    @{Path="module_competence"; Name="Module Competence"},
    @{Path="module_portfolio"; Name="Module Portfolio"}
)

$rebuildSuccess = $true
foreach ($service in $servicesToRebuild) {
    if (-not (Rebuild-Service -ServicePath $service.Path -ServiceName $service.Name)) {
        $rebuildSuccess = $false
    }
    Write-Host ""
}

if (-not $rebuildSuccess) {
    Write-Host "⚠ Certains services n'ont pas pu être compilés. Vérifiez les erreurs ci-dessus." -ForegroundColor Red
    Read-Host "Appuyez sur Entrée pour continuer quand même ou Ctrl+C pour annuler"
}

# =================================================
# Étape 2: Informations de démarrage
# =================================================

Write-Host ""
Write-Host "Étape 2: Démarrage des services" -ForegroundColor Cyan
Write-Host "--------------------------------" -ForegroundColor Cyan
Write-Host ""
Write-Host "Les services doivent être démarrés dans cet ordre:" -ForegroundColor Yellow
Write-Host "1. Eureka Server (port 8761)" -ForegroundColor White
Write-Host "2. Config Server (port 8888)" -ForegroundColor White
Write-Host "3. Service User (port 8084)" -ForegroundColor White
Write-Host "4. Module Certification (port 8089)" -ForegroundColor White
Write-Host "5. Module Competence (port 8088)" -ForegroundColor White
Write-Host "6. Module Portfolio (port 8087)" -ForegroundColor White
Write-Host "7. API Gateway (port 8080)" -ForegroundColor White
Write-Host ""

$startServices = Read-Host "Voulez-vous démarrer automatiquement les services? (O/N)"

if ($startServices -eq "O" -or $startServices -eq "o") {
    
    Write-Host ""
    Write-Host "Démarrage des services..." -ForegroundColor Cyan
    
    # Tableau des services avec leurs chemins et ports
    $services = @(
        @{Path="eureka-server"; Name="Eureka Server"; Port=8761},
        @{Path="ConfigServer"; Name="Config Server"; Port=8888},
        @{Path="service_user"; Name="Service User"; Port=8084},
        @{Path="module_certification"; Name="Module Certification"; Port=8089},
        @{Path="module_competence"; Name="Module Competence"; Port=8088},
        @{Path="module_portfolio"; Name="Module Portfolio"; Port=8087},
        @{Path="api-gateway"; Name="API Gateway"; Port=8080}
    )
    
    # Démarrer chaque service dans un nouveau terminal
    foreach ($service in $services) {
        Write-Host "Démarrage de $($service.Name) sur le port $($service.Port)..." -ForegroundColor Yellow
        
        $servicePath = Join-Path $workspace $service.Path
        
        # Démarrer dans une nouvelle fenêtre PowerShell
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$servicePath'; Write-Host 'Démarrage de $($service.Name)...' -ForegroundColor Green; .\mvnw spring-boot:run"
        
        # Attendre un peu avant le prochain service
        if ($service.Name -eq "Eureka Server") {
            Write-Host "Attente de 30 secondes pour Eureka Server..." -ForegroundColor Gray
            Start-Sleep -Seconds 30
        } elseif ($service.Name -eq "Config Server") {
            Write-Host "Attente de 20 secondes pour Config Server..." -ForegroundColor Gray
            Start-Sleep -Seconds 20
        } else {
            Write-Host "Attente de 15 secondes..." -ForegroundColor Gray
            Start-Sleep -Seconds 15
        }
    }
    
    Write-Host ""
    Write-Host "✓ Tous les services ont été démarrés dans des fenêtres séparées" -ForegroundColor Green
    Write-Host ""
    Write-Host "Vérifiez Eureka: http://localhost:8761" -ForegroundColor Cyan
    
} else {
    Write-Host ""
    Write-Host "Vous pouvez démarrer manuellement chaque service avec:" -ForegroundColor Yellow
    Write-Host "cd <service-folder>" -ForegroundColor White  
    Write-Host ".\mvnw spring-boot:run" -ForegroundColor White
}

# =================================================
# Étape 3: Frontend
# =================================================

Write-Host ""
Write-Host "Étape 3: Frontend Angular" -ForegroundColor Cyan
Write-Host "-------------------------" -ForegroundColor Cyan
Write-Host ""

$startFrontend = Read-Host "Voulez-vous démarrer le frontend Angular? (O/N)"

if ($startFrontend -eq "O" -or $startFrontend -eq "o") {
    Write-Host "Démarrage du frontend..." -ForegroundColor Yellow
    
    $frontendPath = Join-Path $workspace "nexlance-unified"
    
    # Démarrer dans une nouvelle fenêtre
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$frontendPath'; Write-Host 'Installation des dépendances...' -ForegroundColor Green; npm install; Write-Host 'Démarrage du serveur de développement...' -ForegroundColor Green; ng serve"
    
    Write-Host ""
    Write-Host "✓ Frontend démarré sur http://localhost:4200" -ForegroundColor Green
}

# =================================================
# Informations finales
# =================================================

Write-Host ""
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "Résumé" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "URLs importantes:" -ForegroundColor Yellow
Write-Host "  • Eureka:    http://localhost:8761" -ForegroundColor White
Write-Host "  • Gateway:   http://localhost:8080" -ForegroundColor White
Write-Host "  • Frontend:  http://localhost:4200" -ForegroundColor White
Write-Host ""
Write-Host "Endpoints API (via Gateway):" -ForegroundColor Yellow
Write-Host "  • Skills:         http://localhost:8080/api/freelancer/skills/user-skills/me" -ForegroundColor White
Write-Host "  • Certifications: http://localhost:8080/api/freelancer/certifications/me" -ForegroundColor White
Write-Host "  • Portfolio:      http://localhost:8080/api/freelancer/portfolios/me" -ForegroundColor White
Write-Host ""
Write-Host "Prochaines étapes:" -ForegroundColor Yellow
Write-Host "  1. Vérifier que tous les services sont enregistrés sur Eureka" -ForegroundColor White
Write-Host "  2. Se connecter sur http://localhost:4200/login" -ForegroundColor White
Write-Host "  3. Créer un compte freelancer si nécessaire" -ForegroundColor White
Write-Host "  4. Ajouter des skills dans 'My Skills'" -ForegroundColor White
Write-Host "  5. Passer des tests dans 'My Certifications'" -ForegroundColor White
Write-Host "  6. Créer un portfolio dans 'My Portfolio'" -ForegroundColor White
Write-Host ""
Write-Host "Consultez CORRECTIONS_APPLIQUEES.md pour plus de détails" -ForegroundColor Cyan
Write-Host ""
Write-Host "==================================================" -ForegroundColor Cyan
