-- ============================================================
-- V2 : Advanced features — organization-service
-- ============================================================

-- ── Portfolio de projets ──────────────────────────────────
CREATE TABLE org_portfolio_items (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    organization_id VARCHAR(36)  NOT NULL,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    image_url       VARCHAR(512),
    project_url     VARCHAR(512),
    client_name     VARCHAR(200),
    tags            JSON,
    completed_at    DATE,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    INDEX idx_portfolio_org (organization_id),
    CONSTRAINT fk_portfolio_org FOREIGN KEY (organization_id)
        REFERENCES organizations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Candidatures spontanées ───────────────────────────────
CREATE TABLE org_applications (
    id               VARCHAR(36) NOT NULL PRIMARY KEY,
    organization_id  VARCHAR(36) NOT NULL,
    applicant_id     VARCHAR(36) NOT NULL,
    message          TEXT        NOT NULL,
    cv_url           VARCHAR(512),
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    rejection_reason TEXT,
    created_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    responded_at     DATETIME(6),

    UNIQUE KEY uq_app_org_user (organization_id, applicant_id),
    INDEX idx_app_org    (organization_id),
    INDEX idx_app_user   (applicant_id),
    INDEX idx_app_status (status),
    CONSTRAINT fk_app_org FOREIGN KEY (organization_id)
        REFERENCES organizations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Demandes de devis (RFQ) ───────────────────────────────
CREATE TABLE org_rfq (
    id               VARCHAR(36)    NOT NULL PRIMARY KEY,
    organization_id  VARCHAR(36)    NOT NULL,
    requester_id     VARCHAR(36)    NOT NULL,
    title            VARCHAR(200)   NOT NULL,
    description      TEXT           NOT NULL,
    budget_min       DECIMAL(12, 2),
    budget_max       DECIMAL(12, 2),
    deadline         DATE,
    skills_needed    JSON,
    status           VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    response_message TEXT,
    responded_by_id  VARCHAR(36),
    created_at       DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    responded_at     DATETIME(6),

    INDEX idx_rfq_org       (organization_id),
    INDEX idx_rfq_requester (requester_id),
    INDEX idx_rfq_status    (status),
    CONSTRAINT fk_rfq_org FOREIGN KEY (organization_id)
        REFERENCES organizations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Badges & niveau de confiance ─────────────────────────
ALTER TABLE organizations
    ADD COLUMN trust_level TINYINT     NOT NULL DEFAULT 1 AFTER review_count,
    ADD COLUMN badges      JSON                           AFTER trust_level;
