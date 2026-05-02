-- ============================================================
-- V4 — Ajout des colonnes de réouverture + tables avancées
--      (compensatoire : V3 était vide lors de la 1ère migration)
-- ============================================================

-- Colonnes de réouverture sur complaints
ALTER TABLE complaints
    ADD COLUMN reopen_count      INT  NOT NULL DEFAULT 0,
    ADD COLUMN last_reopened_at  DATETIME NULL,
    ADD COLUMN reopen_reason     TEXT NULL;

-- Tables avancées
CREATE TABLE IF NOT EXISTS sla_rules (
    id                          VARCHAR(36)  NOT NULL,
    priority                    VARCHAR(20)  NOT NULL UNIQUE,
    max_first_response_hours    INT          NOT NULL,
    max_resolution_hours        INT          NOT NULL,
    warning_threshold_hours     INT          NOT NULL,
    created_at                  DATETIME,
    updated_at                  DATETIME,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sla_tracking (
    id                          VARCHAR(36)  NOT NULL,
    complaint_id                VARCHAR(36)  NOT NULL UNIQUE,
    first_response_deadline     DATETIME,
    resolution_deadline         DATETIME,
    first_response_breached     TINYINT(1)   NOT NULL DEFAULT 0,
    resolution_breached         TINYINT(1)   NOT NULL DEFAULT 0,
    first_response_at           DATETIME,
    resolved_at                 DATETIME,
    created_at                  DATETIME,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS nps_surveys (
    id              VARCHAR(36)  NOT NULL,
    complaint_id    VARCHAR(36)  NOT NULL UNIQUE,
    respondent_id   VARCHAR(36)  NOT NULL,
    score           INT,
    comment         TEXT,
    category        VARCHAR(20),
    sent_at         DATETIME,
    responded_at    DATETIME,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS mediation_sessions (
    id                      VARCHAR(36)  NOT NULL,
    complaint_id            VARCHAR(36)  NOT NULL UNIQUE,
    status                  VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    evidence_deadline       DATETIME,
    decision_deadline       DATETIME,
    opened_by_admin_id      VARCHAR(36),
    decided_by_admin_id     VARCHAR(36),
    outcome                 VARCHAR(30),
    admin_reasoning         TEXT,
    created_at              DATETIME,
    closed_at               DATETIME,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS mediation_evidences (
    id                      VARCHAR(36)  NOT NULL,
    session_id              VARCHAR(36)  NOT NULL,
    submitted_by_user_id    VARCHAR(36)  NOT NULL,
    party_type              VARCHAR(20)  NOT NULL,
    description             TEXT         NOT NULL,
    attachments             LONGTEXT,
    submitted_at            DATETIME,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS response_templates (
    id                  VARCHAR(36)   NOT NULL,
    title               VARCHAR(200)  NOT NULL,
    content             TEXT          NOT NULL,
    category            VARCHAR(50),
    created_by_admin_id VARCHAR(36),
    usage_count         INT           NOT NULL DEFAULT 0,
    is_active           TINYINT(1)    NOT NULL DEFAULT 1,
    created_at          DATETIME,
    updated_at          DATETIME,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_risk_profiles (
    id                          VARCHAR(36)  NOT NULL,
    user_id                     VARCHAR(36)  NOT NULL UNIQUE,
    risk_score                  INT          NOT NULL DEFAULT 0,
    total_complaints_against    INT          NOT NULL DEFAULT 0,
    resolved_against            INT          NOT NULL DEFAULT 0,
    scam_count                  INT          NOT NULL DEFAULT 0,
    harassment_count            INT          NOT NULL DEFAULT 0,
    payment_issue_count         INT          NOT NULL DEFAULT 0,
    risk_level                  VARCHAR(20)  NOT NULL DEFAULT 'LOW',
    last_calculated_at          DATETIME,
    created_at                  DATETIME,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_sanctions (
    id                      VARCHAR(36)  NOT NULL,
    user_id                 VARCHAR(36)  NOT NULL,
    type                    VARCHAR(30)  NOT NULL,
    reason                  TEXT         NOT NULL,
    trigger_complaint_id    VARCHAR(36),
    is_active               TINYINT(1)   NOT NULL DEFAULT 1,
    expires_at              DATETIME,
    applied_at              DATETIME,
    applied_by_system       TINYINT(1)   NOT NULL DEFAULT 0,
    applied_by_admin_id     VARCHAR(36),
    lifted_at               DATETIME,
    lifted_by_admin_id      VARCHAR(36),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Données initiales SLA
INSERT IGNORE INTO sla_rules (id, priority, max_first_response_hours, max_resolution_hours, warning_threshold_hours, created_at, updated_at)
VALUES
    (UUID(), 'LOW',      48, 168, 24, NOW(), NOW()),
    (UUID(), 'MEDIUM',   24,  72, 12, NOW(), NOW()),
    (UUID(), 'HIGH',      8,  24,  4, NOW(), NOW()),
    (UUID(), 'CRITICAL',  2,   8,  1, NOW(), NOW());
