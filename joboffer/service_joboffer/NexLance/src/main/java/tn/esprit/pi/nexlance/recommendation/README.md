# Module de Recommandation - NexLance

## 📋 Description

Ce module gère le système de recommandations de freelances pour les offres d'emploi sur la plateforme NexLance. Il permet aux clients de recommander des freelances spécifiques pour leurs projets et de suivre l'état de ces recommandations.

## 🏗️ Architecture

### Package Structure
```
recommendation/
├── controllers/
│   └── RecommendationController.java
├── dto/
│   ├── CreateRecommendationRequest.java
│   ├── RecommendationDTO.java
│   └── RecommendationResponseRequest.java
├── entities/
│   ├── Recommendation.java
│   └── RecommendationActivity.java
├── enums/
│   ├── ActivityType.java
│   ├── RecommendationStatus.java
│   └── UserType.java
├── repositories/
│   ├── RecommendationActivityRepository.java
│   └── RecommendationRepository.java
└── services/
    ├── RecommendationActivityService.java
    ├── RecommendationActivityServiceImpl.java
    ├── RecommendationService.java
    └── RecommendationServiceImpl.java
```

## 📊 Modèle de Données

### Table: recommendations
- **Identifiants**: id (PK), client_id, freelance_id, job_offer_id
- **Contenu**: message, proposed_budget, deadline
- **Statut**: status, freelance_response, responded_at
- **Métriques**: views_count, reminders_sent, last_reminded_at
- **Métadonnées**: expires_at, cancelled_reason, cancelled_at
- **Timestamps**: created_at, updated_at

### Table: recommendation_activities
- **Identifiant**: id (PK)
- **Relations**: recommendation_id
- **Type**: activity_type
- **Acteur**: user_id, user_type
- **Détails**: activity_data (JSON)
- **Métadonnées**: ip_address, user_agent
- **Timestamp**: created_at

## 🔄 États de Recommandation

```
PENDING → ACCEPTED
        → REJECTED
        → CANCELLED
        → EXPIRED
```

## 🚀 API Endpoints

### CRUD Operations
- `POST /api/recommendations` - Créer une recommandation
- `GET /api/recommendations/{id}` - Obtenir une recommandation
- `GET /api/recommendations` - Lister toutes les recommandations
- `PUT /api/recommendations/{id}` - Mettre à jour une recommandation
- `DELETE /api/recommendations/{id}` - Supprimer une recommandation

### Query Operations
- `GET /api/recommendations/client/{clientId}` - Recommandations d'un client
- `GET /api/recommendations/freelance/{freelanceId}` - Recommandations d'un freelance
- `GET /api/recommendations/job-offer/{jobOfferId}` - Recommandations d'une offre
- `GET /api/recommendations/status/{status}` - Recommandations par statut
- `GET /api/recommendations/freelance/{freelanceId}/pending` - Recommandations en attente

### Status Operations
- `POST /api/recommendations/{id}/accept` - Accepter une recommandation
- `POST /api/recommendations/{id}/reject` - Rejeter une recommandation
- `POST /api/recommendations/{id}/cancel` - Annuler une recommandation
- `POST /api/recommendations/{id}/view` - Incrémenter les vues

### Reminder Operations
- `POST /api/recommendations/{id}/reminder` - Envoyer un rappel
- `GET /api/recommendations/reminders/pending` - Recommandations nécessitant un rappel
- `POST /api/recommendations/reminders/send-batch` - Envoyer des rappels en batch

### Statistics
- `GET /api/recommendations/stats/count/{status}` - Compter par statut
- `GET /api/recommendations/stats/freelance/{freelanceId}/{status}` - Stats freelance
- `GET /api/recommendations/stats/client/{clientId}/{status}` - Stats client
- `GET /api/recommendations/recent?days=7` - Recommandations récentes
- `GET /api/recommendations/top-viewed` - Recommandations les plus vues

### Activity Tracking
- `GET /api/recommendations/{id}/activities` - Activités d'une recommandation
- `GET /api/recommendations/activities/type/{activityType}` - Activités par type
- `GET /api/recommendations/activities/user/{userId}/{userType}` - Activités par utilisateur
- `GET /api/recommendations/activities/recent?days=7` - Activités récentes
- `GET /api/recommendations/activities/count/{activityType}` - Compter par type

### Maintenance
- `POST /api/recommendations/expire-old` - Expirer les anciennes recommandations
- `DELETE /api/recommendations/activities/cleanup?days=90` - Nettoyer les anciennes activités

## 📝 Exemples d'Utilisation

### Créer une Recommandation
```json
POST /api/recommendations
{
  "clientId": 1,
  "freelanceId": 5,
  "jobOfferId": 10,
  "message": "Je pense que ce freelance serait parfait pour ce projet",
  "proposedBudget": 5000.00,
  "deadline": "2026-03-15",
  "expiresAt": "2026-03-01T23:59:59"
}
```

### Accepter une Recommandation
```json
POST /api/recommendations/1/accept
{
  "response": "J'accepte avec plaisir cette recommandation !"
}
```

### Rejeter une Recommandation
```json
POST /api/recommendations/1/reject
{
  "response": "Merci mais je ne suis pas disponible pour ce projet"
}
```

### Annuler une Recommandation
```json
POST /api/recommendations/1/cancel
{
  "reason": "Le projet a été annulé"
}
```

## 🔧 Fonctionnalités Principales

### 1. Gestion des Recommandations
- Création de recommandations personnalisées
- Suivi du statut (pending, accepted, rejected, cancelled, expired)
- Gestion des réponses des freelances
- Système d'expiration automatique

### 2. Suivi des Métriques
- Compteur de vues
- Nombre de rappels envoyés
- Date du dernier rappel

### 3. Système d'Activités
- Traçabilité complète de toutes les actions
- Stockage des métadonnées (IP, User-Agent)
- Données JSON flexibles pour les détails

### 4. Automatisation
- Expiration automatique des recommandations
- Système de rappels intelligents
- Nettoyage automatique des anciennes activités

## 🎯 Types d'Activités

- `CREATED` - Recommandation créée
- `SENT` - Recommandation envoyée
- `VIEWED` - Recommandation vue
- `REMINDED` - Rappel envoyé
- `ACCEPTED` - Recommandation acceptée
- `REJECTED` - Recommandation rejetée
- `CANCELLED` - Recommandation annulée
- `EXPIRED` - Recommandation expirée
- `UPDATED` - Recommandation mise à jour

## 🔐 Sécurité

- Validation des données d'entrée
- Vérification des permissions (à implémenter avec Spring Security)
- Traçabilité des actions via le système d'activités
- Protection contre les duplications

## 📈 Performances

- Index sur les clés étrangères (client_id, freelance_id, job_offer_id)
- Index sur le statut pour les recherches fréquentes
- Pagination possible sur toutes les listes
- Requêtes optimisées avec @Query

## 🧪 Tests

Pour tester le module, utilisez la collection Postman fournie ou curl :

```bash
# Créer une recommandation
curl -X POST http://localhost:8080/api/recommendations \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": 1,
    "freelanceId": 5,
    "jobOfferId": 10,
    "message": "Recommandation test",
    "proposedBudget": 5000.00
  }'

# Obtenir toutes les recommandations
curl http://localhost:8080/api/recommendations

# Accepter une recommandation
curl -X POST http://localhost:8080/api/recommendations/1/accept \
  -H "Content-Type: application/json" \
  -d '{"response": "Accepté!"}'
```

## 🔄 Intégration

Ce module peut être intégré avec :
- Module d'offres d'emploi (job_offers)
- Module d'utilisateurs (pour client_id et freelance_id)
- Système de notifications (pour les rappels)
- Système d'emails (pour informer les freelances)

## 📊 Statistiques Disponibles

- Nombre de recommandations par statut
- Recommandations les plus vues
- Taux d'acceptation/rejet
- Nombre de rappels envoyés
- Activités récentes

## 🛠️ Technologies Utilisées

- Spring Boot 3.x
- Spring Data JPA
- Hibernate
- MySQL/PostgreSQL (support JSON)
- Lombok
- SLF4J pour logging

## 📝 Notes Importantes

1. Les recommandations expirées sont automatiquement marquées lors de l'appel à `/expire-old`
2. Les rappels sont envoyés au maximum une fois toutes les 24 heures
3. Une seule recommandation peut exister par combinaison (client, freelance, job_offer)
4. Toutes les activités sont tracées automatiquement

## 🚀 Améliorations Futures

- [ ] Système de notifications en temps réel
- [ ] Templates d'emails personnalisables
- [ ] Dashboard analytics
- [ ] Scoring des recommandations
- [ ] Recommandations automatiques basées sur l'IA
- [ ] Export des statistiques (PDF, Excel)
- [ ] Webhook pour notifications externes
