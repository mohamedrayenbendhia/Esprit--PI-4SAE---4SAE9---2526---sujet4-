-- Migration initiale : création des tables complaints et support_messages

CREATE TABLE IF NOT EXISTS complaints (
    id                  VARCHAR(36)   NOT NULL PRIMARY KEY,
    ticket_number       VARCHAR(50)   NOT NULL UNIQUE,
    reporter_id         VARCHAR(36)   NOT NULL,
    reported_user_id    VARCHAR(36),
    project_id          VARCHAR(36),
    category            VARCHAR(50)   NOT NULL,
    priority            VARCHAR(20)   NOT NULL DEFAULT 'MEDIUM',
    status              VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    subject             VARCHAR(255)  NOT NULL,
    description         TEXT          NOT NULL,
    attachments         LONGTEXT,
    assigned_to_id      VARCHAR(36),
    resolution          TEXT,
    resolution_type     VARCHAR(50),
    satisfaction_rating INT,
    created_at          DATETIME      NOT NULL,
    first_response_at   DATETIME,
    resolved_at         DATETIME,
    closed_at           DATETIME,
    updated_at          DATETIME,
    INDEX idx_complaints_reporter  (reporter_id),
    INDEX idx_complaints_status    (status),
    INDEX idx_complaints_assigned  (assigned_to_id)
);

CREATE TABLE IF NOT EXISTS support_messages (
    id            VARCHAR(36)  NOT NULL PRIMARY KEY,
    complaint_id  VARCHAR(36)  NOT NULL,
    sender_id     VARCHAR(36)  NOT NULL,
    sender_type   VARCHAR(20)  NOT NULL,
    message_type  VARCHAR(20)  NOT NULL DEFAULT 'TEXT',
    content       TEXT         NOT NULL,
    attachments   LONGTEXT,
    is_read       TINYINT(1)   NOT NULL DEFAULT 0,
    read_at       DATETIME,
    created_at    DATETIME     NOT NULL,
    INDEX idx_messages_complaint (complaint_id),
    CONSTRAINT fk_messages_complaint
        FOREIGN KEY (complaint_id) REFERENCES complaints(id)
        ON DELETE CASCADE
);
