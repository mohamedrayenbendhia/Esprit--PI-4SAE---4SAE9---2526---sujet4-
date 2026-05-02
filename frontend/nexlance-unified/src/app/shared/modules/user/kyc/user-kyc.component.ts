import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { KycService } from '../../../../core/services/kyc.service';
import { KYCVerification, KYCStatus, DocumentType } from '../../../../core/models/kyc-verification.model';
import { HeaderComponent } from '../../../components/header/header.component';

@Component({
  selector: 'app-user-kyc',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatFormFieldModule,
    MatChipsModule,
    TranslateModule,
    MatProgressBarModule,
    MatTableModule,
    MatSnackBarModule,
    MatDividerModule,
    HeaderComponent
  ],
  templateUrl: './user-kyc.component.html',
  styleUrls: ['./user-kyc.component.scss']
})
export class UserKycComponent implements OnInit {
  private fb = inject(FormBuilder);
  private snackBar = inject(MatSnackBar);
  private kycService = inject(KycService);
  private translate = inject(TranslateService);

  uploadForm!: FormGroup;
  kycStatus = signal<KYCStatus>(KYCStatus.PENDING);
  documents = signal<KYCVerification[]>([]);
  isLoading = signal(false);
  isDragging = signal(false);
  selectedFile: File | null = null;

  KYCStatus = KYCStatus;
  DocumentType = DocumentType;
  displayedColumns = ['type', 'fileName', 'uploadedAt', 'status', 'actions'];

  ngOnInit(): void {
    this.initForm();
    this.loadKycData();
  }

  initForm(): void {
    this.uploadForm = this.fb.group({
      documentType: ['', Validators.required]
    });
  }

  loadKycData(): void {
    this.isLoading.set(true);
    
    // Load status and documents from the backend API
    this.kycService.getMyKYCStatus().subscribe({
      next: (response) => {
        console.log('📄 KYC Status:', response);
        this.kycStatus.set(response.status || KYCStatus.PENDING);
        this.documents.set(response.documents || []);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading KYC:', error);
        this.isLoading.set(false);
        
        // Fallback to PENDING status on error
        this.kycStatus.set(KYCStatus.PENDING);
        this.documents.set([]);
        
        if (error.status === 404) {
          // No KYC documents found - normal for new user
          console.log('No KYC documents found');
        } else {
          this.snackBar.open('Error loading KYC documents', 'Close', {
            duration: 3000,
            panelClass: ['error-snackbar']
          });
        }
      }
    });
  }

  getDocumentTypeLabel(type: DocumentType): string {
    const key = `kyc.documentTypes.${type.toLowerCase()}`;
    return this.translate.instant(key);
  }

  getStatusLabel(status: KYCStatus): string {
    const key = `kyc.status.${status.toLowerCase()}`;
    return this.translate.instant(key);
  }

  getStatusColor(status: KYCStatus): string {
    const colors = {
      [KYCStatus.PENDING]: '#F97316',
      [KYCStatus.APPROVED]: '#10B981',
      [KYCStatus.REJECTED]: '#DC2626',
      [KYCStatus.EXPIRED]: '#6B7280'
    };
    return colors[status] || '#6B7280';
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging.set(true);
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging.set(false);

    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      const file = event.dataTransfer.files[0];
      this.handleFile(file);
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      this.handleFile(input.files[0]);
    }
  }

  handleFile(file: File): void {
    // Verify file type
    const allowedTypes = ['application/pdf', 'image/jpeg', 'image/png', 'image/jpg'];
    if (!allowedTypes.includes(file.type)) {
      this.snackBar.open('File type not allowed. PDF, JPG or PNG only.', 'Close', {
        duration: 5000,
        panelClass: ['error-snackbar']
      });
      return;
    }

    // Verify size (max 5MB)
    if (file.size > 5 * 1024 * 1024) {
      this.snackBar.open('File is too large. Max size: 5MB', 'Close', {
        duration: 5000,
        panelClass: ['error-snackbar']
      });
      return;
    }

    this.selectedFile = file;
    this.snackBar.open(`File selected: ${file.name}`, 'Close', {
      duration: 3000
    });
  }

  uploadDocument(): void {
    if (!this.selectedFile || this.uploadForm.invalid) {
      this.snackBar.open('Please select a document type and file', 'Close', {
        duration: 3000,
        panelClass: ['error-snackbar']
      });
      return;
    }

    this.isLoading.set(true);

    const documentType = this.uploadForm.value.documentType as DocumentType;
    
    // Appel API backend
    this.kycService.submitDocument({
      documentType: documentType,
      documentFile: this.selectedFile
    }).subscribe({
      next: (verification) => {
        console.log('Document uploaded:', verification);
        this.isLoading.set(false);
        this.snackBar.open('Document uploaded successfully', 'Close', {
          duration: 3000,
          panelClass: ['success-snackbar']
        });
        this.selectedFile = null;
        this.uploadForm.reset();
        // Reload data
        this.loadKycData();
      },
      error: (error) => {
        console.error('Error uploading document:', error);
        this.isLoading.set(false);
        const message = error.error?.message || 'Error uploading document';
        this.snackBar.open(`${message}`, 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  deleteDocument(doc: KYCVerification): void {
    const fileName = this.getFileNameFromUrl(doc.documentUrl);
    if (confirm(`Are you sure you want to delete ${fileName}?`)) {
      this.isLoading.set(true);
      
      // API call to backend
      this.kycService.deleteDocument(doc.id).subscribe({
        next: () => {
          console.log('Document deleted:', doc.id);
          // Remove from local array
          const docs = this.documents().filter(d => d.id !== doc.id);
          this.documents.set(docs);
          this.isLoading.set(false);
          this.snackBar.open('Document deleted', 'Close', { 
            duration: 3000,
            panelClass: ['success-snackbar']
          });
        },
        error: (error) => {
          console.error('Error deleting document:', error);
          this.isLoading.set(false);
          this.snackBar.open('Error deleting document', 'Close', { 
            duration: 3000,
            panelClass: ['error-snackbar']
          });
        }
      });
    }
  }

  downloadDocument(doc: KYCVerification): void {
    const fileName = this.getFileNameFromUrl(doc.documentUrl);
    this.snackBar.open(`Downloading ${fileName}...`, 'Close', { duration: 2000 });
    
    // Open document in new tab
    const documentUrl = this.kycService.getDocumentUrl(doc.id);
    window.open(documentUrl, '_blank');
  }

  private getFileNameFromUrl(url: string): string {
    if (!url) return 'document';
    const parts = url.split('/');
    return parts[parts.length - 1] || 'document';
  }
}
