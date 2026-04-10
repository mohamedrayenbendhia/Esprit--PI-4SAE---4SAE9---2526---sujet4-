-- Migration pour ajouter le support 2FA (Two-Factor Authentication)
-- Date: 2024-01-15

-- Ajout des colonnes 2FA dans la table users
ALTER TABLE users 
ADD COLUMN two_factor_enabled BOOLEAN DEFAULT FALSE NOT NULL,
ADD COLUMN two_factor_secret VARCHAR(255),
ADD COLUMN backup_codes JSON;

-- Index pour améliorer les performances lors de la vérification 2FA
CREATE INDEX idx_users_two_factor_enabled ON users(two_factor_enabled);

-- Commentaires sur les colonnes
COMMENT ON COLUMN users.two_factor_enabled IS 'Indique si l''authentification à deux facteurs est activée pour cet utilisateur';
COMMENT ON COLUMN users.two_factor_secret IS 'Secret TOTP pour générer les codes de vérification';
COMMENT ON COLUMN users.backup_codes IS 'Codes de secours pour l''utilisateur (hachés)';
