-- V6 — Ajout de la colonne auto_escalate sur sla_rules
ALTER TABLE sla_rules
    ADD COLUMN IF NOT EXISTS auto_escalate TINYINT(1) NOT NULL DEFAULT 0;

-- Par défaut : HIGH et CRITICAL ont l'auto-escalade activée
UPDATE sla_rules SET auto_escalate = 1 WHERE priority IN ('HIGH', 'CRITICAL');
