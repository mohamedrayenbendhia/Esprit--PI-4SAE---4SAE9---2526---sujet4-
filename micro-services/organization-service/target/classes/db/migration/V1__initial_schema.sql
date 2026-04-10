-- ============================================================
-- V1 : Initial schema — organization-service
-- DB : smart_freelance_organizations
-- ============================================================

-- ── organizations ────────────────────────────────────────────
CREATE TABLE organizations (
    id                       VARCHAR(36)    NOT NULL PRIMARY KEY,
    name                     VARCHAR(150)   NOT NULL UNIQUE,
    description              TEXT,
    logo_url                 VARCHAR(512),
    website                  VARCHAR(512),
    type                     VARCHAR(30)    NOT NULL,
    specialties              JSON,
    location                 VARCHAR(255),
    siret                    VARCHAR(14),
    size                     VARCHAR(20)    NOT NULL DEFAULT 'SMALL',
    status                   VARCHAR(30)    NOT NULL DEFAULT 'PENDING_VERIFICATION',
    visibility               VARCHAR(20)    NOT NULL DEFAULT 'PUBLIC',
    owner_id                 VARCHAR(36)    NOT NULL,
    average_rating           DOUBLE         NOT NULL DEFAULT 0.0,
    completed_projects_count INT            NOT NULL DEFAULT 0,
    review_count             INT            NOT NULL DEFAULT 0,
    admin_note               TEXT,
    created_at               DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at               DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    dissolved_at             DATETIME(6),

    INDEX idx_org_status     (status),
    INDEX idx_org_owner      (owner_id),
    INDEX idx_org_type       (type),
    INDEX idx_org_visibility (visibility)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── organization_members ─────────────────────────────────────
CREATE TABLE organization_members (
    id               VARCHAR(36) NOT NULL PRIMARY KEY,
    organization_id  VARCHAR(36) NOT NULL,
    user_id          VARCHAR(36) NOT NULL,
    role             VARCHAR(20) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    display_on_profile BOOLEAN   NOT NULL DEFAULT TRUE,
    joined_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    left_at          DATETIME(6),

    UNIQUE KEY uq_member_org_user (organization_id, user_id),
    INDEX idx_member_org    (organization_id),
    INDEX idx_member_user   (user_id),
    INDEX idx_member_status (status),

    CONSTRAINT fk_member_org FOREIGN KEY (organization_id)
        REFERENCES organizations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── invitations ──────────────────────────────────────────────
CREATE TABLE invitations (
    id                   VARCHAR(36)  NOT NULL PRIMARY KEY,
    organization_id      VARCHAR(36)  NOT NULL,
    invited_user_id      VARCHAR(36),
    invited_email        VARCHAR(255),
    invited_by_user_id   VARCHAR(36)  NOT NULL,
    proposed_role        VARCHAR(20)  NOT NULL DEFAULT 'MEMBER',
    status               VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    token                VARCHAR(36)  NOT NULL UNIQUE,
    expires_at           DATETIME(6)  NOT NULL,
    created_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    responded_at         DATETIME(6),

    INDEX idx_inv_org    (organization_id),
    INDEX idx_inv_user   (invited_user_id),
    INDEX idx_inv_token  (token),
    INDEX idx_inv_status (status),

    CONSTRAINT fk_inv_org FOREIGN KEY (organization_id)
        REFERENCES organizations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── organization_reviews ─────────────────────────────────────
CREATE TABLE organization_reviews (
    id                    VARCHAR(36) NOT NULL PRIMARY KEY,
    organization_id       VARCHAR(36) NOT NULL,
    client_id             VARCHAR(36) NOT NULL,
    project_id            VARCHAR(36),
    quality_rating        TINYINT     NOT NULL,
    communication_rating  TINYINT     NOT NULL,
    deadline_rating       TINYINT     NOT NULL,
    value_rating          TINYINT     NOT NULL,
    average_rating        DOUBLE      NOT NULL,
    comment               TEXT,
    owner_reply           TEXT,
    reply_at              DATETIME(6),
    reported              BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    UNIQUE KEY uq_review_org_project (organization_id, project_id),
    INDEX idx_review_org    (organization_id),
    INDEX idx_review_client (client_id),

    CONSTRAINT fk_review_org FOREIGN KEY (organization_id)
        REFERENCES organizations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── audit_logs ───────────────────────────────────────────────
CREATE TABLE audit_logs (
    id                    VARCHAR(36)  NOT NULL PRIMARY KEY,
    organization_id       VARCHAR(36)  NOT NULL,
    performed_by_user_id  VARCHAR(36)  NOT NULL,
    action                VARCHAR(50)  NOT NULL,
    details               TEXT,
    performed_at          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    INDEX idx_audit_org  (organization_id),
    INDEX idx_audit_user (performed_by_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
