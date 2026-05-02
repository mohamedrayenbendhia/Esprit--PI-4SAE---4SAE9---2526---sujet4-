-- Ajouter un utilisateur FREELANCE sans profil freelance pour les tests
-- Email: khalilyy@gmail.com
-- Password: password123 (hashé avec BCrypt)

USE nexlance_db;

-- Insérer l'utilisateur Khalil
INSERT INTO users (id, email, password, type, first_name, last_name, phone_number, status, subscription_type, email_verified, created_at, updated_at)
VALUES (
  UUID(),
  'khalilyy@gmail.com',
  '$2a$10$N9qo8uLOickgx2ZMRZoMye1IEbhEhHhvLdUmE7CGl5bkXKN6Gz8Vi',  -- password123
  'FREELANCE',
  'Khalil',
  'Younes',
  '+33698765432',
  'ACTIVE',
  'FREE',
  true,
  NOW(),
  NOW()
)
ON DUPLICATE KEY UPDATE email = email;

-- Vérifier que l'utilisateur a été créé
SELECT 
  id, 
  email, 
  type, 
  first_name, 
  last_name, 
  status, 
  email_verified,
  created_at
FROM users 
WHERE email = 'khalilyy@gmail.com';

-- Note: Cet utilisateur n'aura PAS de profil freelance associé
-- C'est intentionnel pour tester l'affichage des utilisateurs FREELANCE sans profil complet
