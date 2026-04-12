-- Table pour les vérifications KYC
CREATE TABLE IF NOT EXISTS kyc_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    document_type VARCHAR(50) NOT NULL CHECK (document_type IN ('IDENTITY_CARD', 'PASSPORT', 'DRIVER_LICENSE', 'PROOF_ADDRESS', 'BANK_STATEMENT')),
    document_url VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED')),
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    reviewed_by UUID,
    rejection_reason TEXT,
    expiry_date TIMESTAMP,
    
    -- Foreign keys
    CONSTRAINT fk_kyc_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_kyc_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Indexes pour améliorer les performances
CREATE INDEX IF NOT EXISTS idx_kyc_user_id ON kyc_verifications(user_id);
CREATE INDEX IF NOT EXISTS idx_kyc_status ON kyc_verifications(status);
CREATE INDEX IF NOT EXISTS idx_kyc_submitted_at ON kyc_verifications(submitted_at DESC);
CREATE INDEX IF NOT EXISTS idx_kyc_reviewed_by ON kyc_verifications(reviewed_by);

-- Commentaires
COMMENT ON TABLE kyc_verifications IS 'Table de vérifications KYC (Know Your Customer) pour les documents d''identité des utilisateurs';
COMMENT ON COLUMN kyc_verifications.user_id IS 'ID de l''utilisateur qui soumet le document';
COMMENT ON COLUMN kyc_verifications.document_type IS 'Type de document: IDENTITY_CARD, PASSPORT, DRIVER_LICENSE, PROOF_ADDRESS, BANK_STATEMENT';
COMMENT ON COLUMN kyc_verifications.document_url IS 'URL du document uploadé (chemin du fichier)';
COMMENT ON COLUMN kyc_verifications.status IS 'Statut de vérification: PENDING, APPROVED, REJECTED, EXPIRED';
COMMENT ON COLUMN kyc_verifications.reviewed_by IS 'ID de l''administrateur qui a examiné le document';
COMMENT ON COLUMN kyc_verifications.rejection_reason IS 'Raison du rejet si status = REJECTED';
COMMENT ON COLUMN kyc_verifications.expiry_date IS 'Date d''expiration du document (si applicable)';
