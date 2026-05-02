-- ============================================================
-- V1 — Schéma initial : table complaints + support_messages
-- ============================================================

CREATE TABLE IF NOT EXISTS complaints (
    id                  VARCHAR(36)     NOT NULL,
    ticket_number       VARCHAR(50)     NOT NULL UNIQUE,
    reporter_id         VARCHAR(36)     NOT NULL,
    reported_user_id    VARCHAR(36),
    project_id          VARCHAR(36),
    category            VARCHAR(50)     NOT NULL,
    priority            VARCHAR(20)     DEFAULT 'MEDIUM',
    status              VARCHAR(30)     DEFAULT 'OPEN',
    subject             VARCHAR(255)    NOT NULL,
    description         TEXT            NOT NULL,
    attachments         LONGTEXT,
    assigned_to_id      VARCHAR(36),
    resolution          TEXT,
    resolution_type     VARCHAR(30),
    satisfaction_rating INT,
    reopen_count        INT             NOT NULL DEFAULT 0,
    last_reopened_at    DATETIME,
    reopen_reason       TEXT,
    created_at          DATETIME        NOT NULL,
    first_response_at   DATETIME,
    resolved_at         DATETIME,
    closed_at           DATETIME,
    updated_at          DATETIME,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_complaint_reporter    ON complaints (reporter_id);
CREATE INDEX idx_complaint_reported    ON complaints (reported_user_id);
CREATE INDEX idx_complaint_status      ON complaints (status);
CREATE INDEX idx_complaint_priority    ON complaints (priority);
CREATE INDEX idx_complaint_assigned    ON complaints (assigned_to_id);
CREATE INDEX idx_complaint_created     ON complaints (created_at);

-- -------------------------------------------------------

CREATE TABLE IF NOT EXISTS support_messages (
    id                  VARCHAR(36)     NOT NULL,
    complaint_id        VARCHAR(36)     NOT NULL,
    sender_id           VARCHAR(36)     NOT NULL,
    sender_type         VARCHAR(20)     NOT NULL,
    message_type        VARCHAR(30)     DEFAULT 'TEXT',
    content             TEXT            NOT NULL,
    attachments         LONGTEXT,
    is_read             TINYINT(1)      DEFAULT 0,
    read_at             DATETIME,
    created_at          DATETIME        NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_messages_complaint    ON support_messages (complaint_id);
CREATE INDEX idx_messages_sender       ON support_messages (sender_id);
