-- ============================================================
-- V6 : Collab Offers & Applications tables
-- ============================================================

-- ── collab_offers ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS collab_offers (
    id               VARCHAR(36)    NOT NULL,
    organization_id  VARCHAR(36)    NOT NULL,
    created_by       VARCHAR(36)    NOT NULL,
    title            VARCHAR(150)   NOT NULL,
    description      TEXT           NOT NULL,
    required_skills  TEXT,
    duration_label   VARCHAR(100),
    budget_estimate  DOUBLE,
    max_applicants   INT,
    deadline_date    DATE,
    status           VARCHAR(20)    NOT NULL DEFAULT 'OPEN',
    created_at       DATETIME(6),
    updated_at       DATETIME(6),

    PRIMARY KEY (id),
    INDEX idx_co_org    (organization_id),
    INDEX idx_co_status (status),
    INDEX idx_co_owner  (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── collab_applications ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS collab_applications (
    id               VARCHAR(36)    NOT NULL,
    offer_id         VARCHAR(36)    NOT NULL,
    organization_id  VARCHAR(36)    NOT NULL,
    applicant_id     VARCHAR(36)    NOT NULL,
    message          TEXT           NOT NULL,
    portfolio_url    VARCHAR(512),
    status           VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    rejection_reason TEXT,
    created_at       DATETIME(6),
    responded_at     DATETIME(6),

    PRIMARY KEY (id),
    INDEX idx_ca_offer     (offer_id),
    INDEX idx_ca_applicant (applicant_id),
    INDEX idx_ca_status    (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
