-- ============================================================
-- V3 — Fonctionnalités avancées : SLA, Médiation, Risque,
--       Sanctions, Templates de réponse, Réouverture, NPS
-- ============================================================

-- ── 1. Réouverture (colonnes sur complaints) ─────────────────
ALTER TABLE complaints
    ADD COLUMN reopen_count       INT          NOT NULL DEFAULT 0    COMMENT 'Nombre de réouvertures effectuées',
    ADD COLUMN last_reopened_at   DATETIME     NULL                  COMMENT 'Horodatage de la dernière réouverture',
    ADD COLUMN reopen_reason      TEXT         NULL                  COMMENT 'Motif de la dernière réouverture';

-- ── 2. Règles SLA ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sla_rules (
    id                        VARCHAR(36)  NOT NULL PRIMARY KEY,
    priority                  VARCHAR(20)  NOT NULL UNIQUE,
    max_first_response_hours  INT          NOT NULL,
    max_resolution_hours      INT          NOT NULL,
    warning_threshold_hours   INT          NOT NULL,
    created_at                DATETIME     NOT NULL,
    updated_at                DATETIME     NOT NULL
);

-- Règles SLA par défaut
INSERT INTO sla_rules (id, priority, max_first_response_hours, max_resolution_hours, warning_threshold_hours, created_at, updated_at) VALUES
    (UUID(), 'LOW',      48, 240, 24, NOW(), NOW()),
    (UUID(), 'MEDIUM',   24, 120, 12, NOW(), NOW()),
    (UUID(), 'HIGH',      8,  48,  4, NOW(), NOW()),
    (UUID(), 'CRITICAL',  2,  24,  1, NOW(), NOW());

-- ── 3. Suivi SLA par réclamation ──────────────────────────────
CREATE TABLE IF NOT EXISTS sla_tracking (
    id                        VARCHAR(36)  NOT NULL PRIMARY KEY,
    complaint_id              VARCHAR(36)  NOT NULL UNIQUE,
    first_response_deadline   DATETIME     NULL,
    resolution_deadline       DATETIME     NULL,
    first_response_breached   TINYINT(1)   NOT NULL DEFAULT 0,
    resolution_breached       TINYINT(1)   NOT NULL DEFAULT 0,
    first_response_at         DATETIME     NULL,
    resolved_at               DATETIME     NULL,
    created_at                DATETIME     NOT NULL,
    INDEX idx_sla_complaint (complaint_id),
    CONSTRAINT fk_sla_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE
);

-- ── 4. Sessions de médiation ──────────────────────────────────
CREATE TABLE IF NOT EXISTS mediation_sessions (
    id                    VARCHAR(36)  NOT NULL PRIMARY KEY,
    complaint_id          VARCHAR(36)  NOT NULL UNIQUE,
    status                VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    evidence_deadline     DATETIME     NULL,
    decision_deadline     DATETIME     NULL,
    opened_by_admin_id    VARCHAR(36)  NULL,
    decided_by_admin_id   VARCHAR(36)  NULL,
    outcome               VARCHAR(30)  NULL,
    admin_reasoning       TEXT         NULL,
    created_at            DATETIME     NOT NULL,
    closed_at             DATETIME     NULL,
    INDEX idx_med_complaint (complaint_id),
    INDEX idx_med_status    (status),
    CONSTRAINT fk_med_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE
);

-- ── 5. Preuves de médiation ───────────────────────────────────
CREATE TABLE IF NOT EXISTS mediation_evidences (
    id                    VARCHAR(36)  NOT NULL PRIMARY KEY,
    session_id            VARCHAR(36)  NOT NULL,
    submitted_by_user_id  VARCHAR(36)  NOT NULL,
    party_type            VARCHAR(20)  NOT NULL,
    description           TEXT         NOT NULL,
    attachments           LONGTEXT     NULL,
    submitted_at          DATETIME     NOT NULL,
    INDEX idx_ev_session (session_id),
    CONSTRAINT fk_ev_session FOREIGN KEY (session_id) REFERENCES mediation_sessions(id) ON DELETE CASCADE
);

-- ── 6. Profils de risque utilisateurs ────────────────────────
CREATE TABLE IF NOT EXISTS user_risk_profiles (
    id                        VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id                   VARCHAR(36)  NOT NULL UNIQUE,
    risk_score                INT          NOT NULL DEFAULT 0,
    total_complaints_against  INT          NOT NULL DEFAULT 0,
    resolved_against          INT          NOT NULL DEFAULT 0,
    scam_count                INT          NOT NULL DEFAULT 0,
    harassment_count          INT          NOT NULL DEFAULT 0,
    payment_issue_count       INT          NOT NULL DEFAULT 0,
    risk_level                VARCHAR(20)  NOT NULL DEFAULT 'LOW',
    last_calculated_at        DATETIME     NULL,
    created_at                DATETIME     NOT NULL,
    INDEX idx_risk_user  (user_id),
    INDEX idx_risk_level (risk_level),
    INDEX idx_risk_score (risk_score)
);

-- ── 7. Sanctions utilisateurs ─────────────────────────────────
CREATE TABLE IF NOT EXISTS user_sanctions (
    id                    VARCHAR(36)   NOT NULL PRIMARY KEY,
    user_id               VARCHAR(36)   NOT NULL,
    type                  VARCHAR(30)   NOT NULL,
    reason                TEXT          NOT NULL,
    trigger_complaint_id  VARCHAR(36)   NULL,
    is_active             TINYINT(1)    NOT NULL DEFAULT 1,
    expires_at            DATETIME      NULL,
    applied_at            DATETIME      NOT NULL,
    applied_by_system     TINYINT(1)    NOT NULL DEFAULT 0,
    applied_by_admin_id   VARCHAR(36)   NULL,
    lifted_at             DATETIME      NULL,
    lifted_by_admin_id    VARCHAR(36)   NULL,
    INDEX idx_sanction_user   (user_id),
    INDEX idx_sanction_active (is_active),
    INDEX idx_sanction_expiry (expires_at)
);

-- ── 8. Templates de réponse ───────────────────────────────────
CREATE TABLE IF NOT EXISTS response_templates (
    id                    VARCHAR(36)   NOT NULL PRIMARY KEY,
    title                 VARCHAR(200)  NOT NULL,
    content               TEXT          NOT NULL,
    category              VARCHAR(50)   NULL,
    created_by_admin_id   VARCHAR(36)   NULL,
    usage_count           INT           NOT NULL DEFAULT 0,
    is_active             TINYINT(1)    NOT NULL DEFAULT 1,
    created_at            DATETIME      NOT NULL,
    updated_at            DATETIME      NOT NULL,
    INDEX idx_tmpl_category (category),
    INDEX idx_tmpl_active   (is_active)
);

-- Templates par défaut
INSERT INTO response_templates (id, title, content, category, is_active, usage_count, created_at, updated_at) VALUES
    (UUID(), 'Accusé de réception standard',
     'Bonjour,\n\nNous avons bien reçu votre réclamation et elle est en cours de traitement. Un agent vous contactera sous 24h.\n\nCordialement,\nL''équipe NexLance',
     NULL, 1, 0, NOW(), NOW()),
    (UUID(), 'Résolution problème de paiement',
     'Bonjour,\n\nSuite à l''examen de votre réclamation concernant un problème de paiement, nous avons procédé aux vérifications nécessaires. [COMPLÉTER AVEC LA RÉSOLUTION].\n\nCordialement,\nL''équipe NexLance',
     'PAYMENT_ISSUE', 1, 0, NOW(), NOW()),
    (UUID(), 'Clôture pour absence de réponse',
     'Bonjour,\n\nSans réponse de votre part depuis 7 jours, nous procédons à la clôture de votre réclamation. Vous pouvez la rouvrir dans les 7 jours si nécessaire.\n\nCordialement,\nL''équipe NexLance',
     NULL, 1, 0, NOW(), NOW());

-- ── 9. Enquêtes NPS ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS nps_surveys (
    id             VARCHAR(36)   NOT NULL PRIMARY KEY,
    complaint_id   VARCHAR(36)   NOT NULL UNIQUE,
    respondent_id  VARCHAR(36)   NOT NULL,
    score          INT           NULL,
    comment        TEXT          NULL,
    sent_at        DATETIME      NOT NULL,
    responded_at   DATETIME      NULL,
    category       VARCHAR(20)   NULL,
    INDEX idx_nps_complaint  (complaint_id),
    INDEX idx_nps_respondent (respondent_id),
    CONSTRAINT fk_nps_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE
);
