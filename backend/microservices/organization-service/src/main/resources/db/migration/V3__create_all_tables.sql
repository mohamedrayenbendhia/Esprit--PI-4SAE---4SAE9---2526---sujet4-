-- V3: Create all organization-service tables (V1 and V2 were empty)

CREATE TABLE IF NOT EXISTS organizations (
    id                       VARCHAR(36)    NOT NULL,
    name                     VARCHAR(100)   NOT NULL,
    description              TEXT,
    logo_url                 VARCHAR(500),
    website                  VARCHAR(255),
    type                     VARCHAR(30)    NOT NULL,
    specialties              TEXT,
    location                 VARCHAR(150),
    siret                    VARCHAR(14),
    size                     VARCHAR(20),
    status                   VARCHAR(30)    NOT NULL,
    visibility               VARCHAR(20)    NOT NULL,
    owner_id                 VARCHAR(36)    NOT NULL,
    average_rating           DOUBLE         NOT NULL DEFAULT 0.0,
    completed_projects_count INT            NOT NULL DEFAULT 0,
    review_count             INT            NOT NULL DEFAULT 0,
    trust_level              INT            NOT NULL DEFAULT 1,
    badges                   TEXT,
    admin_note               TEXT,
    created_at               DATETIME       NOT NULL,
    updated_at               DATETIME,
    dissolved_at             DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_organizations_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS organization_members (
    id                 VARCHAR(36)  NOT NULL,
    organization_id    VARCHAR(36)  NOT NULL,
    user_id            VARCHAR(36)  NOT NULL,
    role               VARCHAR(20)  NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    display_on_profile TINYINT(1)   NOT NULL DEFAULT 1,
    joined_at          DATETIME     NOT NULL,
    left_at            DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_member_org_user (organization_id, user_id),
    INDEX idx_member_org  (organization_id),
    INDEX idx_member_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS org_invitations (
    id              VARCHAR(36)  NOT NULL,
    organization_id VARCHAR(36)  NOT NULL,
    inviter_id      VARCHAR(36)  NOT NULL,
    invitee_id      VARCHAR(36),
    invitee_email   VARCHAR(255),
    role            VARCHAR(20)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    message         TEXT,
    token           VARCHAR(64)  UNIQUE,
    expires_at      DATETIME,
    created_at      DATETIME     NOT NULL,
    responded_at    DATETIME,
    PRIMARY KEY (id),
    INDEX idx_invitation_org     (organization_id),
    INDEX idx_invitation_invitee (invitee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS org_applications (
    id              VARCHAR(36)  NOT NULL,
    organization_id VARCHAR(36)  NOT NULL,
    applicant_id    VARCHAR(36)  NOT NULL,
    message         TEXT         NOT NULL,
    cv_url          VARCHAR(512),
    status          VARCHAR(20)  NOT NULL,
    rejection_reason TEXT,
    created_at      DATETIME     NOT NULL,
    responded_at    DATETIME,
    PRIMARY KEY (id),
    INDEX idx_application_org       (organization_id),
    INDEX idx_application_applicant (applicant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS org_rfq (
    id               VARCHAR(36)     NOT NULL,
    organization_id  VARCHAR(36)     NOT NULL,
    requester_id     VARCHAR(36)     NOT NULL,
    title            VARCHAR(200)    NOT NULL,
    description      TEXT            NOT NULL,
    budget_min       DECIMAL(12,2),
    budget_max       DECIMAL(12,2),
    deadline         DATE,
    skills_needed    TEXT,
    status           VARCHAR(20)     NOT NULL,
    response_message TEXT,
    responded_by_id  VARCHAR(36),
    created_at       DATETIME        NOT NULL,
    responded_at     DATETIME,
    PRIMARY KEY (id),
    INDEX idx_rfq_org       (organization_id),
    INDEX idx_rfq_requester (requester_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS org_portfolio_items (
    id              VARCHAR(36)  NOT NULL,
    organization_id VARCHAR(36)  NOT NULL,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    image_url       VARCHAR(512),
    project_url     VARCHAR(512),
    client_name     VARCHAR(200),
    tags            TEXT,
    completed_at    DATE,
    created_at      DATETIME     NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_portfolio_org (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS organization_reviews (
    id              VARCHAR(36)  NOT NULL,
    organization_id VARCHAR(36)  NOT NULL,
    reviewer_id     VARCHAR(36)  NOT NULL,
    project_id      VARCHAR(36),
    rating          INT          NOT NULL,
    comment         TEXT,
    reply           TEXT,
    reply_at        DATETIME,
    created_at      DATETIME     NOT NULL,
    updated_at      DATETIME,
    PRIMARY KEY (id),
    INDEX idx_review_org      (organization_id),
    INDEX idx_review_reviewer (reviewer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS org_audit_logs (
    id                   VARCHAR(36)  NOT NULL,
    organization_id      VARCHAR(36)  NOT NULL,
    performed_by_user_id VARCHAR(36)  NOT NULL,
    action               VARCHAR(100) NOT NULL,
    details              TEXT,
    created_at           DATETIME     NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_audit_org  (organization_id),
    INDEX idx_audit_user (performed_by_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
