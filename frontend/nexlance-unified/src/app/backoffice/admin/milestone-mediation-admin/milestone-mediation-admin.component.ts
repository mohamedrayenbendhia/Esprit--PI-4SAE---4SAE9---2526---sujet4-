import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProjectService } from '../../../core/services/project.service';
import { ProjectMilestone, MilestoneStatus } from '../../../core/models/project.model';

@Component({
  selector: 'app-milestone-mediation-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="mediation-container" *ngIf="!loading && milestone">
      <button class="btn-back" (click)="goBack()">← Back</button>

      <h1>Mediation - {{ milestone.title }}</h1>

      <div class="mediation-grid">
        <div class="criteria-column">
          <h2>Acceptance Criteria</h2>
          <div class="criteria-content">
            {{ milestone.acceptanceCriteria || 'No criteria defined' }}
          </div>
        </div>

        <div class="deliverables-column">
          <h2>Submitted Deliverables</h2>
          <div class="deliverables-content">
            <p *ngIf="milestone.submittedAt">
              Submitted on {{ milestone.submittedAt | date: 'dd/MM/yyyy at HH:mm' }}
            </p>
            <p *ngIf="milestone.rejectionReason">
              <strong>Rejection reason:</strong> {{ milestone.rejectionReason }}
            </p>
          </div>
        </div>
      </div>

      <div class="admin-actions">
        <h2>Administrative Actions</h2>
        <div class="action-buttons">
          <button class="btn btn-approve" (click)="forceApprove()"
                  [disabled]="isProcessing">
            Force Approval
          </button>
          <button class="btn btn-reject" (click)="requestRevisions()"
                  [disabled]="isProcessing">
            Request Revisions
          </button>
          <button class="btn btn-cancel" (click)="cancelMilestone()"
                  [disabled]="isProcessing">
            Cancel Milestone
          </button>
        </div>

        <div class="notes-section">
          <h3>Admin Private Notes</h3>
          <textarea [(ngModel)]="adminNotes" rows="4" class="form-control"
                    placeholder="Internal notes..."></textarea>
        </div>
      </div>

      <div class="alert alert-success" *ngIf="successMessage">
        ✓ {{ successMessage }}
      </div>
      <div class="alert alert-error" *ngIf="errorMessage">
        ✗ {{ errorMessage }}
      </div>
    </div>

    <div class="loading-container" *ngIf="loading">
      <div class="spinner"></div>
      <p>Loading...</p>
    </div>
  `,
  styles: [`
    .mediation-container {
      max-width: 1400px;
      margin: 0 auto;
      padding: 2rem;

      .btn-back {
        background: transparent;
        border: none;
        color: #4299e1;
        padding: 0.5rem 0;
        cursor: pointer;
        margin-bottom: 1rem;
      }

      h1 {
        font-size: 2rem;
        margin-bottom: 2rem;
      }

      .mediation-grid {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 2rem;
        margin-bottom: 2rem;

        .criteria-column,
        .deliverables-column {
          background: white;
          padding: 2rem;
          border-radius: 8px;
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

          h2 {
            font-size: 1.25rem;
            margin-bottom: 1rem;
          }

          .criteria-content,
          .deliverables-content {
            color: #4a5568;
            line-height: 1.6;
          }
        }
      }

      .admin-actions {
        background: white;
        padding: 2rem;
        border-radius: 8px;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

        h2 {
          font-size: 1.25rem;
          margin-bottom: 1.5rem;
        }

        .action-buttons {
          display: flex;
          gap: 1rem;
          margin-bottom: 2rem;

          .btn {
            flex: 1;
            padding: 1rem;
            border: none;
            border-radius: 6px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s;

            &:disabled {
              opacity: 0.6;
              cursor: not-allowed;
            }

            &.btn-approve {
              background: #48bb78;
              color: white;

              &:hover:not(:disabled) {
                background: #38a169;
              }
            }

            &.btn-reject {
              background: #fc8181;
              color: white;

              &:hover:not(:disabled) {
                background: #f56565;
              }
            }

            &.btn-cancel {
              background: #e2e8f0;
              color: #2d3748;

              &:hover:not(:disabled) {
                background: #cbd5e0;
              }
            }
          }
        }

        .notes-section {
          h3 {
            font-size: 1rem;
            margin-bottom: 0.75rem;
          }

          .form-control {
            width: 100%;
            padding: 0.75rem;
            border: 1px solid #cbd5e0;
            border-radius: 6px;
            font-family: inherit;
            resize: vertical;
          }
        }
      }

      .alert {
        padding: 1rem 1.5rem;
        border-radius: 6px;
        margin-top: 1rem;

        &.alert-success {
          background: #c6f6d5;
          color: #22543d;
        }

        &.alert-error {
          background: #fed7d7;
          color: #742a2a;
        }
      }
    }

    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 400px;

      .spinner {
        width: 50px;
        height: 50px;
        border: 4px solid #e2e8f0;
        border-top-color: #4299e1;
        border-radius: 50%;
        animation: spin 1s linear infinite;
      }
    }

    @keyframes spin {
      to {
        transform: rotate(360deg);
      }
    }

    @media (max-width: 768px) {
      .mediation-container .mediation-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class MilestoneMediationAdminComponent implements OnInit {
  milestone: ProjectMilestone | null = null;
  loading = true;
  isProcessing = false;
  adminNotes = '';
  successMessage = '';
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private projectService: ProjectService
  ) {}

  ngOnInit(): void {
    const milestoneId = this.route.snapshot.paramMap.get('milestoneId');
    if (milestoneId) {
      this.loadMilestone(milestoneId);
    }
  }

  loadMilestone(id: string): void {
    this.loading = true;
    this.projectService.getMilestoneById(id).subscribe({
      next: (milestone) => {
        this.milestone = milestone;
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = 'Error loading milestone';
        console.error('Error loading milestone:', error);
      }
    });
  }

  forceApprove(): void {
    if (!this.milestone?.id || !confirm('Force approval of this milestone?')) return;

    this.isProcessing = true;
    this.projectService.adminApproveMilestone(this.milestone.id, this.adminNotes).subscribe({
      next: () => {
        this.successMessage = 'Milestone approved successfully';
        this.isProcessing = false;
        setTimeout(() => this.goBack(), 2000);
      },
      error: (error) => {
        this.errorMessage = 'Error during approval';
        this.isProcessing = false;
        console.error('Error approving milestone:', error);
      }
    });
  }

  requestRevisions(): void {
    if (!this.milestone?.id) return;
    const reason = prompt('Reason for revisions:');
    if (!reason) return;

    this.isProcessing = true;
    this.projectService.adminRequestRevisions(this.milestone.id, reason + (this.adminNotes ? '\n\nAdmin notes: ' + this.adminNotes : '')).subscribe({
      next: () => {
        this.successMessage = 'Revisions requested';
        this.isProcessing = false;
        setTimeout(() => this.goBack(), 2000);
      },
      error: (error) => {
        this.errorMessage = 'Error requesting revisions';
        this.isProcessing = false;
        console.error('Error rejecting milestone:', error);
      }
    });
  }

  cancelMilestone(): void {
    if (!this.milestone?.id || !confirm('Cancel this milestone?')) return;

    this.isProcessing = true;
    this.projectService.updateMilestoneStatus(
      this.milestone.id,
      MilestoneStatus.REJECTED
    ).subscribe({
      next: () => {
        this.successMessage = 'Milestone cancelled';
        this.isProcessing = false;
        setTimeout(() => this.goBack(), 2000);
      },
      error: (error) => {
        this.errorMessage = 'Error during cancellation';
        this.isProcessing = false;
        console.error('Error canceling milestone:', error);
      }
    });
  }

  goBack(): void {
    const projectId = this.route.snapshot.paramMap.get('projectId') || this.route.snapshot.paramMap.get('id');
    this.router.navigate(['/backoffice/admin/projects', projectId]);
  }
}
