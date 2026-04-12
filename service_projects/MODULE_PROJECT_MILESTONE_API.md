# Module Project & Milestone - API Documentation

## Package Structure
```
tn.esprit.pi.service_projects.module_project_Milestone/
├── entities/
│   ├── Project.java
│   ├── ProjectMilestone.java
│   ├── ProjectStatus.java (ACTIVE, ON_HOLD, COMPLETED, CANCELLED)
│   └── MilestoneStatus.java (PENDING, IN_PROGRESS, SUBMITTED, APPROVED, REJECTED)
├── repositories/
│   ├── ProjectRepository.java
│   └── ProjectMilestoneRepository.java
├── services/
│   ├── ProjectService.java
│   ├── ProjectServiceImpl.java
│   ├── ProjectMilestoneService.java
│   └── ProjectMilestoneServiceImpl.java
└── controllers/
    ├── ProjectController.java
    └── ProjectMilestoneController.java
```

## API Endpoints

### Project Endpoints

#### 1. Create Project
**POST** `/api/projects`
```json
{
  "jobOfferId": "uuid",
  "freelanceId": "uuid",
  "clientId": "uuid",
  "startDate": "2026-02-18T10:00:00",
  "endDate": "2026-06-18T10:00:00",
  "status": "ACTIVE",
  "requirements": "Project requirements text",
  "deliverables": "[\"deliverable1\", \"deliverable2\"]"
}
```

#### 2. Get All Projects
**GET** `/api/projects`

#### 3. Get Project by ID
**GET** `/api/projects/{id}`

#### 4. Get Projects by Client ID
**GET** `/api/projects/client/{clientId}`

#### 5. Get Projects by Freelance ID
**GET** `/api/projects/freelance/{freelanceId}`

#### 6. Get Projects by Job Offer ID
**GET** `/api/projects/job-offer/{jobOfferId}`

#### 7. Get Projects by Status
**GET** `/api/projects/status/{status}`

Status values: `ACTIVE`, `ON_HOLD`, `COMPLETED`, `CANCELLED`

#### 8. Update Project
**PUT** `/api/projects/{id}`
```json
{
  "jobOfferId": "uuid",
  "freelanceId": "uuid",
  "clientId": "uuid",
  "startDate": "2026-02-18T10:00:00",
  "endDate": "2026-06-18T10:00:00",
  "status": "ACTIVE",
  "requirements": "Updated requirements",
  "deliverables": "[\"deliverable1\", \"deliverable2\"]"
}
```

#### 9. Update Project Status
**PATCH** `/api/projects/{id}/status?status=ON_HOLD`

#### 10. Calculate Project Progress
**POST** `/api/projects/{id}/calculate-progress`

Automatically calculates progress based on approved milestones.

#### 11. Delete Project
**DELETE** `/api/projects/{id}`

---

### ProjectMilestone Endpoints

#### 1. Create Milestone
**POST** `/api/milestones`
```json
{
  "project": {
    "id": "project-uuid"
  },
  "title": "Milestone Title",
  "description": "Detailed description",
  "orderIndex": 1,
  "dueDate": "2026-03-18T10:00:00",
  "deliverables": "[\"deliverable1\", \"deliverable2\"]",
  "acceptanceCriteria": "Criteria for acceptance"
}
```

#### 2. Get All Milestones
**GET** `/api/milestones`

#### 3. Get Milestone by ID
**GET** `/api/milestones/{id}`

#### 4. Get Milestones by Project ID
**GET** `/api/milestones/project/{projectId}`

Returns milestones ordered by `orderIndex`.

#### 5. Get Milestones by Project ID and Status
**GET** `/api/milestones/project/{projectId}/status/{status}`

Status values: `PENDING`, `IN_PROGRESS`, `SUBMITTED`, `APPROVED`, `REJECTED`

#### 6. Get Milestones by Status
**GET** `/api/milestones/status/{status}`

#### 7. Get Overdue Milestones
**GET** `/api/milestones/overdue`

Returns all milestones past their due date that are not approved or rejected.

#### 8. Get Milestones Due Soon
**GET** `/api/milestones/due-soon?days=7`

Returns milestones due within the specified number of days (default: 7).

#### 9. Update Milestone
**PUT** `/api/milestones/{id}`
```json
{
  "title": "Updated Title",
  "description": "Updated description",
  "orderIndex": 1,
  "dueDate": "2026-03-20T10:00:00",
  "deliverables": "[\"deliverable1\"]",
  "acceptanceCriteria": "Updated criteria"
}
```

#### 10. Submit Milestone (Freelance)
**POST** `/api/milestones/{id}/submit`
```json
{
  "attachments": "[\"file1.pdf\", \"file2.zip\"]",
  "comment": "Submission comment"
}
```

Changes status to `SUBMITTED` and sets `submittedAt` timestamp.

#### 11. Approve Milestone (Client)
**POST** `/api/milestones/{id}/approve`

Changes status to `APPROVED`, sets `approvedAt` timestamp, and updates project progress.

#### 12. Reject Milestone (Client)
**POST** `/api/milestones/{id}/reject`
```json
{
  "rejectionReason": "Deliverables do not meet acceptance criteria"
}
```

Changes status to `REJECTED` and allows freelance to resubmit.

#### 13. Update Milestone Status
**PATCH** `/api/milestones/{id}/status?status=IN_PROGRESS`

#### 14. Delete Milestone
**DELETE** `/api/milestones/{id}`

Automatically updates project progress after deletion.

---

## Data Models

### Project Entity
```java
{
  "id": "UUID",
  "jobOfferId": "UUID",
  "freelanceId": "UUID",
  "clientId": "UUID",
  "startDate": "LocalDateTime",
  "endDate": "LocalDateTime",
  "status": "ProjectStatus",
  "progress": "Integer (0-100)",
  "milestones": "List<ProjectMilestone>",
  "requirements": "String",
  "deliverables": "String (JSON Array)",
  "createdAt": "LocalDateTime",
  "updatedAt": "LocalDateTime"
}
```

### ProjectMilestone Entity
```java
{
  "id": "UUID",
  "project": "Project",
  "title": "String",
  "description": "String",
  "orderIndex": "Integer",
  "dueDate": "LocalDateTime",
  "status": "MilestoneStatus",
  "deliverables": "String (JSON Array)",
  "acceptanceCriteria": "String",
  "submittedAt": "LocalDateTime",
  "approvedAt": "LocalDateTime",
  "rejectionReason": "String",
  "attachments": "String (JSON Array)",
  "createdAt": "LocalDateTime",
  "updatedAt": "LocalDateTime"
}
```

---

## Business Logic Features

### Automatic Progress Calculation
- Project progress is automatically calculated based on approved milestones
- Formula: `(approvedMilestones / totalMilestones) * 100`
- Progress is updated when:
  - A milestone is approved
  - A milestone is rejected
  - A milestone is deleted

### Milestone Workflow
1. **PENDING**: Initial state when milestone is created
2. **IN_PROGRESS**: Freelance is working on the milestone
3. **SUBMITTED**: Freelance has submitted deliverables
4. **APPROVED**: Client has approved the milestone
5. **REJECTED**: Client has rejected the milestone (can be resubmitted)

### Notifications & Alerts
- Get overdue milestones: `/api/milestones/overdue`
- Get milestones due soon: `/api/milestones/due-soon?days=3`

---

## Usage Examples

### Creating a Complete Project with Milestones

```bash
# 1. Create Project
curl -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "client-uuid",
    "freelanceId": "freelance-uuid",
    "jobOfferId": "job-offer-uuid",
    "startDate": "2026-02-18T10:00:00",
    "endDate": "2026-06-18T10:00:00",
    "requirements": "Build a web application",
    "deliverables": "[\"Source code\", \"Documentation\"]"
  }'

# 2. Create Milestones for the Project
curl -X POST http://localhost:8080/api/milestones \
  -H "Content-Type: application/json" \
  -d '{
    "project": {"id": "project-uuid"},
    "title": "Design Phase",
    "description": "Create UI/UX designs",
    "orderIndex": 1,
    "dueDate": "2026-03-18T10:00:00",
    "acceptanceCriteria": "Mockups approved by client"
  }'

# 3. Freelance Submits Milestone
curl -X POST http://localhost:8080/api/milestones/{id}/submit \
  -H "Content-Type: application/json" \
  -d '{
    "attachments": "[\"design.fig\", \"mockups.pdf\"]",
    "comment": "Design complete as per requirements"
  }'

# 4. Client Approves Milestone
curl -X POST http://localhost:8080/api/milestones/{id}/approve

# 5. Check Project Progress
curl -X GET http://localhost:8080/api/projects/{id}
```

---

## Notes
- All dates use ISO-8601 format: `yyyy-MM-dd'T'HH:mm:ss`
- JSON arrays for deliverables and attachments are stored as strings
- UUIDs are automatically generated for new entities
- Timestamps (createdAt, updatedAt) are automatically managed
- Delete operations use cascade for project-milestone relationship
