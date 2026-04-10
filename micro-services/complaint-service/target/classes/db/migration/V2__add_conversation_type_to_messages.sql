-- Migration : ajout du champ conversation_type dans support_messages
-- Sépare les deux fils de discussion d'une réclamation :
--   COMPLAINANT = plaignant ↔ support
--   REPORTED    = partie mise en cause ↔ support

ALTER TABLE support_messages
    ADD COLUMN conversation_type VARCHAR(20) NOT NULL DEFAULT 'COMPLAINANT'
        COMMENT 'COMPLAINANT=fil plaignant/support | REPORTED=fil partie mise en cause/support';

-- Index pour les requêtes filtrées par complaint + conversationType
CREATE INDEX idx_messages_complaint_conv
    ON support_messages (complaint_id, conversation_type);