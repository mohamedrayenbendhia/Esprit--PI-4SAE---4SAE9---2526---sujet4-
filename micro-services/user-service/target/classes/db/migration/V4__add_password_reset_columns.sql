-- Migration pour ajouter le support de reset password
-- Date: 2024-01-15

-- Ajout des colonnes pour le reset password dans la table users
ALTER TABLE users 
ADD COLUMN reset_token VARCHAR(255),
ADD COLUMN reset_token_expiry DATETIME;

-- Index pour améliorer les performances lors de la vérification du token
CREATE INDEX idx_users_reset_token ON users(reset_token);

-- Commentaires sur les colonnes
COMMENT ON COLUMN users.reset_token IS 'Token temporaire pour la réinitialisation du mot de passe';
COMMENT ON COLUMN users.reset_token_expiry IS 'Date d''expiration du token de réinitialisation';
