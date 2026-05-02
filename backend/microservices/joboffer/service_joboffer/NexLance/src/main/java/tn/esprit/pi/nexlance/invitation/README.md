# Module Invitation API

Ce module gère les invitations envoyées par les clients aux freelances pour des offres d'emploi spécifiques.

## Structure

```
invitation/
├── controllers/
│   └── InvitationController.java
├── dto/
│   ├── CreateInvitationDto.java
│   └── UpdateInvitationDto.java
├── entities/
│   └── Invitation.java
├── enums/
│   └── InvitationStatus.java
├── repositories/
│   └── InvitationRepository.java
└── services/
    └── InvitationService.java
```

## Endpoints API

### Créer une invitation
```http
POST /api/invitations
Content-Type: application/json

{
  "clientId": 1,
  "freelanceId": 2,
  "jobOfferId": 10,
  "message": "Bonjour, je pense que vous seriez parfait pour ce projet...",
  "proposedBudget": 5000.00,
  "deadlineResponse": "2026-03-15T23:59:59"
}
```

### Récupérer toutes les invitations
```http
GET /api/invitations
GET /api/invitations?clientId=1
GET /api/invitations?freelanceId=2
GET /api/invitations?jobOfferId=10
GET /api/invitations?status=PENDING
```

### Récupérer une invitation
```http
GET /api/invitations/{id}
```

### Mettre à jour une invitation
```http
PUT /api/invitations/{id}
Content-Type: application/json

{
  "status": "ACCEPTED",
  "message": "Message de réponse optionnel"
}
```

### Supprimer une invitation
```http
DELETE /api/invitations/{id}
```

### Expirer les invitations dépassées
```http
POST /api/invitations/expire-old
```

## Statuts d'invitation

- `PENDING`: Invitation envoyée, en attente de réponse
- `ACCEPTED`: Invitation acceptée par le freelance
- `DECLINED`: Invitation refusée par le freelance
- `EXPIRED`: Invitation expirée (deadline dépassée)

## Modèle de données

```java
public class Invitation {
    private Long id;
    private Long clientId;
    private Long freelanceId;
    private Long jobOfferId;
    private String message;
    private BigDecimal proposedBudget;
    private LocalDateTime deadlineResponse;
    private InvitationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime respondedAt;
}
```

## Configuration CORS

Le controller est configuré avec `@CrossOrigin(origins = "*")` pour accepter les requêtes depuis n'importe quelle origine, notamment depuis l'application Angular sur `http://localhost:4200`.
