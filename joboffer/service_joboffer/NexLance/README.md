# NexLance - Module Job Offers

## Description
Module de gestion des offres d'emploi et des candidatures pour la plateforme NexLance.

## Technologies
- Java 17
- Spring Boot 4.0.2
- Spring Data JPA
- MySQL/MariaDB
- Hibernate
- Jackson (JSON serialization)
- Maven

## Structure du Module

### Package: module_job_offers

```
module_job_offers/
├── entities/
│   ├── JobOffer.java
│   └── Application.java
├── enums/
│   ├── JobCategory.java
│   ├── BudgetType.java
│   ├── JobOfferStatus.java
│   ├── ExperienceLevel.java
│   └── ApplicationStatus.java
├── converters/
│   └── StringListConverter.java
├── repositories/
│   ├── JobOfferRepository.java
│   └── ApplicationRepository.java
├── services/
│   ├── JobOfferService.java
│   ├── JobOfferServiceImpl.java
│   ├── ApplicationService.java
│   └── ApplicationServiceImpl.java
└── controllers/
    ├── JobOfferController.java
    └── ApplicationController.java
```

## Entities

### 1. JobOffer (Offre d'Emploi)

Table: job_offers

Champs principaux:
- id (UUID) - Identifiant unique
- clientId (UUID) - ID du client créateur
- title (String) - Titre de l'offre
- description (Text) - Description détaillée
- category (Enum) - Catégorie: DEVELOPMENT, DESIGN, MARKETING, WRITING, CUSTOMER_SERVICE, SALES, OTHER
- budget (Decimal) - Budget alloué
- budgetType (Enum) - Type: FIXED, HOURLY
- estimatedDuration (Integer) - Durée estimée en jours
- deadline (DateTime) - Date limite
- status (Enum) - Statut: DRAFT, PUBLISHED, CLOSED, ARCHIVED
- requiredSkills (TEXT/JSON) - Compétences requises (stockées en JSON)
- experienceLevel (Enum) - Niveau: ENTRY_LEVEL, INTERMEDIATE, SENIOR, EXPERT
- location (String) - Localisation
- isRemote (Boolean) - Travail à distance
- attachments (TEXT/JSON) - Fichiers joints (stockés en JSON)
- viewCount (Integer) - Nombre de vues
- applicantCount (Integer) - Nombre de candidatures
- publishedAt (DateTime) - Date de publication
- createdAt (DateTime) - Date de création
- updatedAt (DateTime) - Date de modification

### 2. Application (Candidature)

Table: applications

Champs principaux:
- id (UUID) - Identifiant unique
- jobOfferId (UUID) - Référence à l'offre
- freelanceId (UUID) - ID du freelance
- coverLetter (Text) - Lettre de motivation
- proposedRate (Decimal) - Tarif proposé
- estimatedDelivery (DateTime) - Date de livraison estimée
- status (Enum) - Statut: PENDING, SHORTLISTED, ACCEPTED, REJECTED, WITHDRAWN
- portfolioItems (TEXT/JSON) - Éléments de portfolio (stockés en JSON)
- availableFrom (DateTime) - Date de disponibilité
- isRead (Boolean) - Candidature lue
- submittedAt (DateTime) - Date de soumission
- respondedAt (DateTime) - Date de réponse
- createdAt (DateTime) - Date de création

## API Endpoints

### JobOffer Endpoints

```
POST   /api/job-offers                    - Créer une offre
GET    /api/job-offers                    - Liste toutes les offres
GET    /api/job-offers/{id}               - Détails d'une offre
GET    /api/job-offers/status/{status}    - Filtrer par statut
GET    /api/job-offers/client/{clientId}  - Offres d'un client
GET    /api/job-offers/active             - Offres actives
GET    /api/job-offers/category/{category} - Filtrer par catégorie
GET    /api/job-offers/remote             - Offres à distance
GET    /api/job-offers/experience-level/{level} - Filtrer par niveau
PUT    /api/job-offers/{id}               - Modifier une offre
PATCH  /api/job-offers/{id}/status        - Changer le statut
PATCH  /api/job-offers/{id}/archive       - Archiver (soft delete)
DELETE /api/job-offers/{id}               - Supprimer une offre
```

### Application Endpoints

```
POST   /api/applications                         - Soumettre une candidature
GET    /api/applications                         - Liste toutes les candidatures
GET    /api/applications/{id}                    - Détails d'une candidature
GET    /api/applications/job-offer/{jobOfferId}  - Candidatures par offre
GET    /api/applications/job-offer/{jobOfferId}/unread - Candidatures non lues
GET    /api/applications/job-offer/{jobOfferId}/count  - Compter les candidatures
GET    /api/applications/job-offer/{jobOfferId}/count/{status} - Compter par statut
GET    /api/applications/freelance/{freelanceId} - Candidatures par freelance
GET    /api/applications/status/{status}         - Filtrer par statut
PUT    /api/applications/{id}                    - Modifier une candidature
PATCH  /api/applications/{id}/status             - Changer le statut
PATCH  /api/applications/{id}/read               - Marquer comme lue
PATCH  /api/applications/{id}/withdraw           - Retirer la candidature
DELETE /api/applications/{id}                    - Supprimer une candidature
```

## Configuration

### application.properties

```properties
spring.application.name=NexLance

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/nexlance_db?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.properties.hibernate.format_sql=true

# Server Configuration
server.port=9090
```

## Fonctionnalités Principales

### Gestion des Offres d'Emploi
- Création et publication d'offres par les clients
- Recherche avancée avec filtres multiples
- Gestion du cycle de vie complet
- Statistiques (vues, candidatures)
- Soft delete (archivage)

### Gestion des Candidatures
- Soumission avec lettre de motivation
- Gestion des statuts (pending, shortlisted, accepted, rejected, withdrawn)
- Présélection par le client
- Compteurs et statistiques
- Protection contre suppression des candidatures acceptées

## Opérations CRUD

### JobOffer
- Create: Création avec validation des champs obligatoires
- Read: Liste avec filtres, détails avec incrémentation des vues
- Update: Modification partielle ou complète
- Delete: Soft delete (status = ARCHIVED)

### Application
- Create: Soumission avec vérification de l'offre
- Read: Liste par offre/freelance/statut
- Update: Modification et changement de statut
- Delete: Retrait uniquement si non acceptée

## Installation

1. Cloner le projet
2. Configurer la base de données MySQL
3. Modifier application.properties si nécessaire
4. Compiler le projet: `mvn clean install`
5. Lancer l'application: `mvn spring-boot:run`

## Testing avec Postman

Un fichier de collection Postman est disponible: `NexLance_JobOffers.postman_collection.json`

Pour l'utiliser:
1. Ouvrir Postman
2. Importer le fichier `NexLance_JobOffers.postman_collection.json`
3. La collection contient tous les endpoints avec des exemples de requêtes

### Exemple de création d'offre:

**POST** `http://localhost:9090/api/job-offers`

```json
{
  "clientId": "11111111-1111-1111-1111-111111111111",
  "title": "Full Stack Developer",
  "description": "We are looking for an experienced Full Stack Developer to join our team.",
  "category": "DEVELOPMENT",
  "budget": 2500.00,
  "budgetType": "FIXED",
  "estimatedDuration": 30,
  "deadline": "2026-03-15T23:59:59",
  "status": "DRAFT",
  "requiredSkills": ["Java", "Spring Boot", "React", "MySQL"],
  "experienceLevel": "INTERMEDIATE",
  "location": "Remote",
  "isRemote": true,
  "attachments": ["https://example.com/job-spec.pdf"]
}
```

## Notes Techniques

### Gestion des Champs JSON

Les champs `requiredSkills` et `attachments` sont stockés en format JSON dans des colonnes TEXT.
- Utilisation d'un `StringListConverter` personnalisé
- Sérialisation/Désérialisation automatique via Jackson
- Compatible avec MySQL et MariaDB

### Dépendances Importantes

Le projet nécessite Jackson pour la sérialisation JSON:
```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

## Base de Données

Assurez-vous que MySQL est installé et en cours d'exécution.
La base de données sera créée automatiquement au premier lancement.

## Troubleshooting

### Erreur: "Could not find a FormatMapper for the JSON format"
**Solution**: Assurez-vous que Jackson est présent dans les dépendances Maven

### Erreur: "SQL syntax error... cast(? as json)"
**Solution**: Le projet utilise maintenant des colonnes TEXT avec conversion personnalisée, compatible avec MySQL/MariaDB

## Port

Application disponible sur: http://localhost:9090

## Auteur

NexLance Team
