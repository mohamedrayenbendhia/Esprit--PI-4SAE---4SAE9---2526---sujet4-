export enum DocumentType {
  IDENTITY_CARD = 'IDENTITY_CARD',
  PASSPORT = 'PASSPORT',
  DRIVER_LICENSE = 'DRIVER_LICENSE',
  PROOF_ADDRESS = 'PROOF_ADDRESS',
  BANK_STATEMENT = 'BANK_STATEMENT'
}

export enum KYCStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  EXPIRED = 'EXPIRED'
}

export interface KYCVerification {
  id: string;
  userId: string;
  documentType: DocumentType;
  documentUrl: string;
  status: KYCStatus;
  submittedAt: Date;
  reviewedAt?: Date;
  reviewedBy?: string;
  rejectionReason?: string;
  expiryDate?: Date;
}

export interface SubmitKYCRequest {
  documentType: DocumentType;
  documentFile: File;
  expiryDate?: Date;
}

export interface ReviewKYCRequest {
  status: KYCStatus;
  rejectionReason?: string;
  expiryDate?: Date;
  notes?: string;
}
