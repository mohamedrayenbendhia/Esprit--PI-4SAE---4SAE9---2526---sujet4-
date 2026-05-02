import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatBadgeModule } from '@angular/material/badge';
import { KycService, KYCStats } from '../../../../core/services/kyc.service';
import { KYCVerification, KYCStatus } from '../../../../core/models/kyc-verification.model';
import { KYCReviewDialogComponent } from './kyc-review-dialog.component';
import { RejectReasonDialogComponent } from './reject-reason-dialog.component';
import { UserService } from '../../../../core/services/user.service';
import { User } from '../../../../shared/models/user.model';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-kyc-verification',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    ReactiveFormsModule,
    TranslateModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTableModule,
    MatBadgeModule
  ],
  templateUrl: './kyc-verification.component.html',
  styleUrls: ['./kyc-verification.component.scss']
})
export class KycVerificationComponent implements OnInit {
  private kycService = inject(KycService);
  private userService = inject(UserService);
  private router = inject(Router);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);
  private fb = inject(FormBuilder);

  pendingVerifications = signal<KYCVerification[]>([]);
  filteredVerifications = signal<KYCVerification[]>([]);
  stats = signal<KYCStats | null>(null);
  loading = signal(false);
  processing = signal<Set<string>>(new Set());
  userNames = signal<Map<string, string>>(new Map());
  searchQuery = '';
  filterStatus: string = 'all';

  KYCStatus = KYCStatus;

  ngOnInit(): void {
    this.loadStats();
    this.loadAllVerifications();
  }

  loadStats(): void {
    this.kycService.getKYCStats().subscribe({
      next: (stats) => this.stats.set(stats),
      error: (err) => console.error('Error loading KYC stats:', err)
    });
  }

  loadAllVerifications(): void {
    this.loading.set(true);
    this.kycService.getAllVerifications().subscribe({
      next: (verifications) => {
        this.pendingVerifications.set(verifications);
        this.loadUserNames(verifications);
        this.applyFilters();
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading verifications:', err);
        this.snackBar.open('Error loading verifications', 'Close', { duration: 3000 });
        this.loading.set(false);
      }
    });
  }

  loadUserNames(verifications: KYCVerification[]): void {
    const uniqueUserIds = [...new Set(verifications.map(v => v.userId))];
    const userRequests = uniqueUserIds.map(userId => 
      this.userService.getUserById(userId).pipe(
        catchError(() => of(null))
      )
    );

    if (userRequests.length > 0) {
      forkJoin(userRequests).subscribe({
        next: (users: (User | null)[]) => {
          const namesMap = new Map<string, string>();
          users.forEach((user, index) => {
            if (user) {
              const userId = uniqueUserIds[index];
              namesMap.set(userId, `${user.firstName} ${user.lastName}`);
            }
          });
          this.userNames.set(namesMap);
        },
        error: (err) => console.error('Error loading user names:', err)
      });
    }
  }

  getUserName(userId: string): string {
    return this.userNames().get(userId) || userId;
  }

  loadPendingVerifications(): void {
    this.filterStatus = 'all';
    this.searchQuery = '';
    this.loadAllVerifications();
  }

  applyFilters(): void {
    let filtered = this.pendingVerifications();
    
    // Filter by search query
    if (this.searchQuery) {
      const query = this.searchQuery.toLowerCase();
      filtered = filtered.filter(v => 
        v.userId?.toString().toLowerCase().includes(query) ||
        v.documentType?.toString().toLowerCase().includes(query)
      );
    }
    
    // Filter by status
    if (this.filterStatus !== 'all') {
      filtered = filtered.filter(v => v.status === this.filterStatus);
    }
    
    this.filteredVerifications.set(filtered);
  }

  reviewDocument(verification: KYCVerification): void {
    const dialogRef = this.dialog.open(KYCReviewDialogComponent, {
      width: '900px',
      maxHeight: '90vh',
      data: verification,
      panelClass: 'kyc-review-dialog'
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadPendingVerifications();
        this.loadStats();
      }
    });
  }

  getWaitingTime(submittedAt: Date): string {
    const now = new Date();
    const submitted = new Date(submittedAt);
    const diff = now.getTime() - submitted.getTime();
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const days = Math.floor(hours / 24);
    
    if (days > 0) {
      return `${days} jour${days > 1 ? 's' : ''}`;
    } else {
      return `${hours} heure${hours > 1 ? 's' : ''}`;
    }
  }

  getDocumentTypeLabel(type: string): string {
    const labels: { [key: string]: string } = {
      'IDENTITY_CARD': 'Carte d\'identité',
      'PASSPORT': 'Passeport',
      'DRIVER_LICENSE': 'Permis de conduire',
      'PROOF_ADDRESS': 'Justificatif de domicile',
      'BANK_STATEMENT': 'Relevé bancaire'
    };
    return labels[type] || type;
  }

  getStatusIcon(status: string): string {
    const icons: { [key: string]: string } = {
      'PENDING': 'schedule',
      'APPROVED': 'check_circle',
      'REJECTED': 'cancel'
    };
    return icons[status] || 'help_outline';
  }

  getStatusLabel(status: string): string {
    const labels: { [key: string]: string } = {
      'PENDING': 'En attente',
      'APPROVED': 'Approuvé',
      'REJECTED': 'Rejeté'
    };
    return labels[status] || status;
  }

  quickApprove(verification: KYCVerification, event: Event): void {
    event.stopPropagation();
    
    // Avoid double clicks
    if (this.processing().has(verification.id)) {
      return;
    }
    
    // Mark as processing
    const processingSet = new Set(this.processing());
    processingSet.add(verification.id);
    this.processing.set(processingSet);
    
    this.kycService.approveDocument(verification.id, undefined, 'Quick approval').subscribe({
      next: () => {
        this.snackBar.open('✓ Document approved successfully', 'Close', { 
          duration: 3000,
          panelClass: ['snackbar-success']
        });
        this.loadAllVerifications();
        this.loadStats();
        
        // Remove from processing list
        const newProcessingSet = new Set(this.processing());
        newProcessingSet.delete(verification.id);
        this.processing.set(newProcessingSet);
      },
      error: (err) => {
        console.error('Approval error:', err);
        
        // Extract error message if available
        let errorMessage = 'Error during approval';
        if (err.error && err.error.error) {
          errorMessage = err.error.error;
        } else if (err.message) {
          errorMessage = err.message;
        }
        
        this.snackBar.open(`❌ ${errorMessage}`, 'Close', { 
          duration: 5000,
          panelClass: ['snackbar-error']
        });
        
        // Remove from processing list
        const newProcessingSet = new Set(this.processing());
        newProcessingSet.delete(verification.id);
        this.processing.set(newProcessingSet);
      }
    });
  }

  quickReject(verification: KYCVerification, event: Event): void {
    event.stopPropagation();
    
    // Avoid double clicks
    if (this.processing().has(verification.id)) {
      return;
    }
    
    const dialogRef = this.dialog.open(RejectReasonDialogComponent, {
      width: '500px',
      data: { verification }
    });

    dialogRef.afterClosed().subscribe(reason => {
      if (reason) {
        // Mark as processing
        const processingSet = new Set(this.processing());
        processingSet.add(verification.id);
        this.processing.set(processingSet);
        
        this.kycService.rejectDocument(verification.id, reason).subscribe({
          next: () => {
            this.snackBar.open('✗ Document rejected', 'Close', { 
              duration: 3000,
              panelClass: ['snackbar-error']
            });
            this.loadPendingVerifications();
            this.loadStats();
            
            // Remove from processing list
            const newProcessingSet = new Set(this.processing());
            newProcessingSet.delete(verification.id);
            this.processing.set(newProcessingSet);
          },
          error: (err) => {
            console.error('Rejection error:', err);
            
            // Extract error message if available
            let errorMessage = 'Error during rejection';
            if (err.error && err.error.error) {
              errorMessage = err.error.error;
            } else if (err.message) {
              errorMessage = err.message;
            }
            
            this.snackBar.open(`❌ ${errorMessage}`, 'Close', { 
              duration: 5000,
              panelClass: ['snackbar-error']
            });
            
            // Remove from processing list
            const newProcessingSet = new Set(this.processing());
            newProcessingSet.delete(verification.id);
            this.processing.set(newProcessingSet);
          }
        });
      }
    });
  }
}
