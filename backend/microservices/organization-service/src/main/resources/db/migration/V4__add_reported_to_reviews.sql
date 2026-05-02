-- V4: Ajout colonne 'reported' sur organization_reviews
-- Nécessaire suite à l'ajout du champ boolean dans OrganizationReview.java

ALTER TABLE organization_reviews
    ADD COLUMN IF NOT EXISTS reported TINYINT(1) NOT NULL DEFAULT 0;
