import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProjectService } from '../../../core/services/project.service';
import { ProjectMilestone, MilestoneStatus } from '../../../core/models/project.model';

interface MilestoneStats {
  totalMilestones: number;
  approvalRateFirstAttempt: number;
  averageValidationTime: number;
  averageRevisionsPerMilestone: number;
}

@Component({
  selector: 'app-milestone-stats-admin',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="stats-container">
      <h1>Milestone Statistics</h1>

      <div class="kpi-grid" *ngIf="!loading">
        <div class="kpi-card">
          <div class="kpi-icon">📊</div>
          <div class="kpi-content">
            <span class="kpi-label">Total Milestones</span>
            <span class="kpi-value">{{ allMilestones.length }}</span>
          </div>
        </div>

        <div class="kpi-card">
          <div class="kpi-icon">✓</div>
          <div class="kpi-content">
            <span class="kpi-label">First-Attempt Approval Rate</span>
            <span class="kpi-value">{{ getApprovalRate() }}%</span>
          </div>
        </div>

        <div class="kpi-card">
          <div class="kpi-icon">⏱️</div>
          <div class="kpi-content">
            <span class="kpi-label">Average Validation Time</span>
            <span class="kpi-value">{{ getAverageValidationTime() }} days</span>
          </div>
        </div>

        <div class="kpi-card">
          <div class="kpi-icon">🔄</div>
          <div class="kpi-content">
            <span class="kpi-label">Average Revisions/Milestone</span>
            <span class="kpi-value">{{ getAverageRevisions() }}</span>
          </div>
        </div>
      </div>

      <div class="stats-section" *ngIf="!loading">
        <div class="section-card">
          <h2>Distribution by Status</h2>
          <div class="status-distribution">
            <div class="status-item" *ngFor="let status of statusList">
              <div class="status-bar">
                <div class="status-fill"
                     [style.width.%]="getStatusPercentage(status)"
                     [class]="'fill-' + getStatusColor(status)">
                </div>
              </div>
              <div class="status-info">
                <span class="status-label">{{ getStatusLabel(status) }}</span>
                <span class="status-count">{{ getStatusCount(status) }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="section-card">
          <h2>Alerts - Milestones > 7 days</h2>
          <div class="alerts-list">
            <div *ngFor="let milestone of overdueMilestones" class="alert-item">
              <span class="alert-icon">⚠️</span>
              <div class="alert-content">
                <span class="alert-title">{{ milestone.title }}</span>
                <span class="alert-date">
                  Deadline: {{ milestone.dueDate | date: 'dd/MM/yyyy' }}
                </span>
              </div>
              <span class="alert-days">{{ getDaysOverdue(milestone) }} days</span>
            </div>

            <div class="empty-state" *ngIf="overdueMilestones.length === 0">
              <p>✓ No overdue milestones</p>
            </div>
          </div>
        </div>
      </div>

      <div class="loading-container" *ngIf="loading">
        <div class="spinner"></div>
        <p>Loading statistics...</p>
      </div>

      <div class="alert alert-error" *ngIf="errorMessage">
        {{ errorMessage }}
      </div>
    </div>
  `,
  styles: [`
    .stats-container {
      max-width: 1400px;
      margin: 0 auto;
      padding: 2rem;

      h1 {
        font-size: 2rem;
        color: #1a1a1a;
        margin-bottom: 2rem;
      }

      .kpi-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
        gap: 1.5rem;
        margin-bottom: 2rem;

        .kpi-card {
          background: white;
          padding: 2rem;
          border-radius: 8px;
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
          display: flex;
          gap: 1.5rem;
          align-items: center;

          .kpi-icon {
            font-size: 3rem;
          }

          .kpi-content {
            display: flex;
            flex-direction: column;
            gap: 0.5rem;

            .kpi-label {
              color: #718096;
              font-size: 0.9rem;
            }

            .kpi-value {
              font-size: 2rem;
              font-weight: bold;
              color: #2d3748;
            }
          }
        }
      }

      .stats-section {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 2rem;

        .section-card {
          background: white;
          padding: 2rem;
          border-radius: 8px;
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

          h2 {
            font-size: 1.25rem;
            color: #2d3748;
            margin-bottom: 1.5rem;
          }

          .status-distribution {
            display: flex;
            flex-direction: column;
            gap: 1rem;

            .status-item {
              .status-bar {
                width: 100%;
                height: 32px;
                background: #edf2f7;
                border-radius: 16px;
                overflow: hidden;
                margin-bottom: 0.5rem;

                .status-fill {
                  height: 100%;
                  transition: width 0.5s ease;

                  &.fill-gray {
                    background: #a0aec0;
                  }

                  &.fill-blue {
                    background: #4299e1;
                  }

                  &.fill-orange {
                    background: #dd6b20;
                  }

                  &.fill-green {
                    background: #48bb78;
                  }

                  &.fill-red {
                    background: #f56565;
                  }
                }
              }

              .status-info {
                display: flex;
                justify-content: space-between;
                color: #4a5568;
                font-size: 0.9rem;

                .status-count {
                  font-weight: 600;
                  color: #2d3748;
                }
              }
            }
          }

          .alerts-list {
            .alert-item {
              display: flex;
              align-items: center;
              gap: 1rem;
              padding: 1rem;
              background: #fff5f5;
              border-left: 4px solid #fc8181;
              border-radius: 6px;
              margin-bottom: 0.75rem;

              .alert-icon {
                font-size: 1.5rem;
              }

              .alert-content {
                flex: 1;
                display: flex;
                flex-direction: column;
                gap: 0.25rem;

                .alert-title {
                  font-weight: 600;
                  color: #2d3748;
                }

                .alert-date {
                  color: #718096;
                  font-size: 0.85rem;
                }
              }

              .alert-days {
                font-weight: 600;
                color: #c53030;
              }
            }

            .empty-state {
              text-align: center;
              padding: 2rem;
              color: #718096;

              p {
                margin: 0;
                font-size: 1.1rem;
              }
            }
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

        p {
          margin-top: 1rem;
          color: #718096;
        }
      }

      .alert {
        padding: 1rem 1.5rem;
        background: #fed7d7;
        color: #742a2a;
        border-left: 4px solid #e53e3e;
        border-radius: 6px;
        margin-top: 2rem;
      }
    }

    @keyframes spin {
      to {
        transform: rotate(360deg);
      }
    }

    @media (max-width: 1024px) {
      .stats-container .stats-section {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class MilestoneStatsAdminComponent implements OnInit {
  allMilestones: ProjectMilestone[] = [];
  overdueMilestones: ProjectMilestone[] = [];
  loading = true;
  errorMessage = '';

  statusList = [
    MilestoneStatus.PENDING,
    MilestoneStatus.IN_PROGRESS,
    MilestoneStatus.SUBMITTED,
    MilestoneStatus.APPROVED,
    MilestoneStatus.REJECTED
  ];

  constructor(private projectService: ProjectService) {}

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.loading = true;

    this.projectService.getAllMilestones().subscribe({
      next: (milestones) => {
        this.allMilestones = milestones;
        this.overdueMilestones = milestones.filter(m =>
          this.projectService.isMilestoneOverdue(m)
        );
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = 'Error loading statistics';
        console.error('Error loading milestones:', error);
      }
    });
  }

  getApprovalRate(): number {
    const approved = this.allMilestones.filter(m => m.status === MilestoneStatus.APPROVED).length;
    return this.allMilestones.length > 0
      ? Math.round((approved / this.allMilestones.length) * 100)
      : 0;
  }

  getAverageValidationTime(): number {
    const approved = this.allMilestones.filter(m => 
      m.status === MilestoneStatus.APPROVED && m.submittedAt && m.approvedAt
    );
    
    if (approved.length === 0) return 0;

    const totalDays = approved.reduce((sum, m) => {
      const submitted = new Date(m.submittedAt!).getTime();
      const approvedTime = new Date(m.approvedAt!).getTime();
      const days = Math.floor((approvedTime - submitted) / (1000 * 60 * 60 * 24));
      return sum + days;
    }, 0);

    return Math.round(totalDays / approved.length);
  }

  getAverageRevisions(): string {
    // Simplified: count rejected milestones
    const rejected = this.allMilestones.filter(m => m.status === MilestoneStatus.REJECTED).length;
    return this.allMilestones.length > 0
      ? (rejected / this.allMilestones.length).toFixed(1)
      : '0';
  }

  getStatusCount(status: MilestoneStatus): number {
    return this.allMilestones.filter(m => m.status === status).length;
  }

  getStatusPercentage(status: MilestoneStatus): number {
    return this.allMilestones.length > 0
      ? (this.getStatusCount(status) / this.allMilestones.length) * 100
      : 0;
  }

  getStatusColor(status: MilestoneStatus): string {
    return this.projectService.getStatusColor(status);
  }

  getStatusLabel(status: MilestoneStatus): string {
    const labels: Record<MilestoneStatus, string> = {
      [MilestoneStatus.PENDING]: 'Pending',
      [MilestoneStatus.IN_PROGRESS]: 'In Progress',
      [MilestoneStatus.SUBMITTED]: 'Submitted',
      [MilestoneStatus.APPROVED]: 'Approved',
      [MilestoneStatus.REJECTED]: 'Rejected'
    };
    return labels[status];
  }

  getDaysOverdue(milestone: ProjectMilestone): number {
    const days = this.projectService.getDaysUntilDeadline(milestone.dueDate);
    return Math.abs(days);
  }
}
