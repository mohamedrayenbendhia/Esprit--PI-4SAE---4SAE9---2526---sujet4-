-- ============================================================
-- V5 : Table d'audit immuable des événements de réclamation
-- ============================================================

CREATE TABLE IF NOT EXISTS complaint_events (
    id            VARCHAR(36)   NOT NULL,
    complaint_id  VARCHAR(36)   NOT NULL,
    ticket_number VARCHAR(50)   NULL,
    actor_id      VARCHAR(36)   NULL,
    actor_role    VARCHAR(30)   NULL,
    event_type    VARCHAR(50)   NOT NULL,
    old_value     VARCHAR(255)  NULL,
    new_value     VARCHAR(255)  NULL,
    comment       TEXT          NULL,
    occurred_at   DATETIME(6)   NOT NULL,
    CONSTRAINT pk_complaint_events PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index pour requêtes par réclamation (timeline)
CREATE INDEX idx_complaint_events_complaint_id
    ON complaint_events (complaint_id);

-- Index pour requêtes chronologiques globales
CREATE INDEX idx_complaint_events_occurred_at
    ON complaint_events (occurred_at);
