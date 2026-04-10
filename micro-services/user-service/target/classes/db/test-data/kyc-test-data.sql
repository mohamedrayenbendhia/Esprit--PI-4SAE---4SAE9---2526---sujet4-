-- Données de test pour KYC verifications
-- À utiliser uniquement en développement

-- Insérer quelques vérifications KYC de test
-- Note: Remplacez les UUIDs par des IDs d'utilisateurs réels de votre base

-- Document en attente
INSERT INTO kyc_verifications (id, user_id, document_type, document_url, status, submitted_at)
VALUES 
    (gen_random_uuid(), 
     (SELECT id FROM users LIMIT 1), 
     'IDENTITY_CARD', 
     '/uploads/kyc/sample-id-card.jpg', 
     'PENDING', 
     CURRENT_TIMESTAMP - INTERVAL '2 hours')
ON CONFLICT DO NOTHING;

-- Document approuvé
INSERT INTO kyc_verifications (id, user_id, document_type, document_url, status, submitted_at, reviewed_at, reviewed_by)
VALUES 
    (gen_random_uuid(), 
     (SELECT id FROM users WHERE type = 'CLIENT' LIMIT 1), 
     'PASSPORT', 
     '/uploads/kyc/sample-passport.jpg', 
     'APPROVED', 
     CURRENT_TIMESTAMP - INTERVAL '5 days',
     CURRENT_TIMESTAMP - INTERVAL '4 days',
     (SELECT id FROM users WHERE type = 'ADMIN' LIMIT 1))
ON CONFLICT DO NOTHING;

-- Document rejeté
INSERT INTO kyc_verifications (id, user_id, document_type, document_url, status, submitted_at, reviewed_at, reviewed_by, rejection_reason)
VALUES 
    (gen_random_uuid(), 
     (SELECT id FROM users WHERE type = 'FREELANCER' LIMIT 1), 
     'DRIVER_LICENSE', 
     '/uploads/kyc/sample-license.jpg', 
     'REJECTED', 
     CURRENT_TIMESTAMP - INTERVAL '3 days',
     CURRENT_TIMESTAMP - INTERVAL '2 days',
     (SELECT id FROM users WHERE type = 'ADMIN' LIMIT 1),
     'Document illisible, veuillez soumettre une photo plus claire')
ON CONFLICT DO NOTHING;

-- Justificatif de domicile en attente
INSERT INTO kyc_verifications (id, user_id, document_type, document_url, status, submitted_at)
VALUES 
    (gen_random_uuid(), 
     (SELECT id FROM users WHERE type = 'FREELANCER' OFFSET 1 LIMIT 1), 
     'PROOF_ADDRESS', 
     '/uploads/kyc/sample-proof-address.pdf', 
     'PENDING', 
     CURRENT_TIMESTAMP - INTERVAL '1 day')
ON CONFLICT DO NOTHING;
