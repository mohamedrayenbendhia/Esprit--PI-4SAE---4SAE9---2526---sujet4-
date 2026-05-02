import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { KycService } from '../../../core/services/kyc.service';
import { KYCVerification, DocumentType, KYCStatus } from '../../../core/models/kyc-verification.model';

@Component({
  selector: 'app-user-kyc',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './user-kyc.component.html',
  styleUrls: ['./user-kyc.component.scss']
})
export class UserKycComponent implements OnInit {
  private kycService = inject(KycService);

  verifications = signal<KYCVerification[]>([]);
  kycStatus = signal<{ verified: boolean; status: KYCStatus; documents: KYCVerification[] } | null>(null);
  loading = signal(false);
  uploading = signal(false);

  selectedDocumentType: DocumentType = DocumentType.IDENTITY_CARD;
  selectedFile: File | null = null;
  expiryDate: string = '';

  DocumentType = DocumentType;
  KYCStatus = KYCStatus;

  ngOnInit(): void {
    this.loadKYCStatus();
    this.loadVerifications();
  }

  loadKYCStatus(): void {
    this.kycService.getMyKYCStatus().subscribe({
      next: (status) => this.kycStatus.set(status),
      error: (err) => console.error('Error loading KYC status:', err)
    });
  }

  loadVerifications(): void {
    this.loading.set(true);
    this.kycService.getMyVerifications().subscribe({
      next: (verifications) => {
        this.verifications.set(verifications);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading verifications:', err);
        this.loading.set(false);
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      this.selectedFile = input.files[0];
    }
  }

  onSubmitDocument(): void {
    if (!this.selectedFile) {
      alert('Please select a file');
      return;
    }

    this.uploading.set(true);

    const request = {
      documentType: this.selectedDocumentType,
      documentFile: this.selectedFile,
      expiryDate: this.expiryDate ? new Date(this.expiryDate) : undefined
    };

    this.kycService.submitDocument(request).subscribe({
      next: () => {
        alert('Document submitted successfully');
        this.selectedFile = null;
        this.expiryDate = '';
        this.uploading.set(false);
        this.loadVerifications();
        this.loadKYCStatus();
      },
      error: (err) => {
        console.error('Error submitting document:', err);
        alert('Error submitting document');
        this.uploading.set(false);
      }
    });
  }

  deleteDocument(id: string): void {
    if (confirm('Are you sure you want to delete this document?')) {
      this.kycService.deleteDocument(id).subscribe({
        next: () => {
          alert('Document deleted successfully');
          this.loadVerifications();
          this.loadKYCStatus();
        },
        error: (err) => console.error('Error deleting document:', err)
      });
    }
  }

  getStatusBadgeClass(status: KYCStatus): string {
    switch (status) {
      case KYCStatus.APPROVED: return 'badge-success';
      case KYCStatus.PENDING: return 'badge-warning';
      case KYCStatus.REJECTED: return 'badge-danger';
      case KYCStatus.EXPIRED: return 'badge-secondary';
      default: return 'badge-secondary';
    }
  }

  getStatusLabel(status: KYCStatus): string {
    switch (status) {
      case KYCStatus.APPROVED: return 'Approved';
      case KYCStatus.PENDING: return 'Pending';
      case KYCStatus.REJECTED: return 'Rejected';
      case KYCStatus.EXPIRED: return 'Expired';
      default: return status;
    }
  }

  getDocumentTypeLabel(type: DocumentType): string {
    const labels = {
      [DocumentType.IDENTITY_CARD]: 'Identity Card',
      [DocumentType.PASSPORT]: 'Passport',
      [DocumentType.DRIVER_LICENSE]: 'Driver License',
      [DocumentType.PROOF_ADDRESS]: 'Proof of Address',
      [DocumentType.BANK_STATEMENT]: 'Bank Statement'
    };
    return labels[type] || type;
  }
}
