import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ProjectService } from '../../../core/services/project.service';
import { Project, ProjectMilestone, MilestoneStatus } from '../../../core/models/project.model';

@Component({
  selector: 'app-project-detail-admin',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="admin-project-detail" *ngIf="!loading && project">
      <button class="btn-back" (click)="goBack()">← Back to list</button>

      <div class="project-header">
        <h1>Project Detail</h1>
        <span class="project-status" [class]="'status-' + project.status">
          {{ project.status }}
        </span>
      </div>

      <div class="project-info-grid">
        <div class="info-card">
          <h3>Project ID</h3>
          <p><code>{{ project.id }}</code></p>
        </div>
        <div class="info-card">
          <h3>Client</h3>
          <p><code>{{ project.clientId }}</code></p>
        </div>
        <div class="info-card">
          <h3>Freelance</h3>
          <p><code>{{ project.freelanceId }}</code></p>
        </div>
        <div class="info-card">
          <h3>Progress</h3>
          <p><strong>{{ project.progress || 0 }}%</strong></p>
        </div>
        <div class="info-card">
          <h3>Dates</h3>
          <p>{{ project.startDate | date: 'dd/MM/yyyy' }} - {{ project.endDate | date: 'dd/MM/yyyy' }}</p>
        </div>
        <div class="info-card">
          <h3>Milestones</h3>
          <p>{{ getApprovedCount() }} / {{ milestones.length }} validated</p>
        </div>
      </div>

      <div class="milestones-section">
        <h2>Project Milestones</h2>
        <div class="milestones-table">
          <table>
            <thead>
              <tr>
                <th>#</th>
                <th>Title</th>
                <th>Status</th>
                <th>Deadline</th>
                <th>Submitted Since</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let milestone of milestones; let i = index" 
                  [class.overdue]="isOverdue(milestone)">
                <td>{{ i + 1 }}</td>
                <td>{{ milestone.title }}</td>
                <td>
                  <span class="status-badge" [class]="'status-' + getStatusColor(milestone.status)">
                    {{ getStatusLabel(milestone.status) }}
                  </span>
                </td>
                <td>{{ milestone.dueDate | date: 'dd/MM/yyyy' }}</td>
                <td>
                  <span *ngIf="milestone.submittedAt">
                    {{ getDaysSinceSubmission(milestone) }} day(s)
                  </span>
                  <span *ngIf="!milestone.submittedAt">-</span>
                </td>
                <td>
                  <button class="btn-sm" (click)="examineMilestone(milestone.id!)">
                    Examine
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="requirements-section" *ngIf="project.requirements">
        <h2>Project Requirements</h2>
        <p>{{ project.requirements }}</p>
      </div>
    </div>

    <div class="loading-container" *ngIf="loading">
      <div class="spinner"></div>
      <p>Loading...</p>
    </div>
  `,
  styles: [`
    .admin-project-detail {
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

        &:hover {
          color: #3182ce;
        }
      }

      .project-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 2rem;

        h1 {
          font-size: 2rem;
          color: #1a1a1a;
        }

        .project-status {
          padding: 0.5rem 1rem;
          border-radius: 6px;
          font-weight: 500;

          &.status-ACTIVE {
            background: #c6f6d5;
            color: #22543d;
          }

          &.status-ON_HOLD {
            background: #feebc8;
            color: #7c2d12;
          }

          &.status-COMPLETED {
            background: #bee3f8;
            color: #2c5282;
          }
        }
      }

      .project-info-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
        gap: 1rem;
        margin-bottom: 2rem;

        .info-card {
          background: white;
          padding: 1.5rem;
          border-radius: 8px;
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

          h3 {
            font-size: 0.9rem;
            color: #718096;
            margin-bottom: 0.5rem;
          }

          p {
            font-size: 1.1rem;
            color: #2d3748;
            margin: 0;

            code {
              background: #edf2f7;
              padding: 0.25rem 0.5rem;
              border-radius: 4px;
              font-size: 0.9rem;
            }
          }
        }
      }

      .milestones-section {
        background: white;
        padding: 2rem;
        border-radius: 8px;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        margin-bottom: 2rem;

        h2 {
          margin-bottom: 1.5rem;
        }

        .milestones-table {
          overflow-x: auto;

          table {
            width: 100%;
            border-collapse: collapse;

            thead {
              background: #f7fafc;

              th {
                padding: 1rem;
                text-align: left;
                font-weight: 600;
                color: #2d3748;
                border-bottom: 2px solid #e2e8f0;
              }
            }

            tbody {
              tr {
                border-bottom: 1px solid #e2e8f0;

                &:hover {
                  background: #f7fafc;
                }

                &.overdue {
                  background: #fff5f5;
                }

                td {
                  padding: 1rem;
                  color: #4a5568;

                  .status-badge {
                    padding: 0.25rem 0.75rem;
                    border-radius: 12px;
                    font-size: 0.85rem;
                    font-weight: 500;

                    &.status-gray {
                      background: #edf2f7;
                      color: #4a5568;
                    }

                    &.status-blue {
                      background: #bee3f8;
                      color: #2c5282;
                    }

                    &.status-orange {
                      background: #feebc8;
                      color: #7c2d12;
                    }

                    &.status-green {
                      background: #c6f6d5;
                      color: #22543d;
                    }

                    &.status-red {
                      background: #fed7d7;
                      color: #742a2a;
                    }
                  }

                  .btn-sm {
                    padding: 0.5rem 1rem;
                    background: #4299e1;
                    color: white;
                    border: none;
                    border-radius: 4px;
                    cursor: pointer;

                    &:hover {
                      background: #3182ce;
                    }
                  }
                }
              }
            }
          }
        }
      }

      .requirements-section {
        background: white;
        padding: 2rem;
        border-radius: 8px;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

        h2 {
          margin-bottom: 1rem;
        }

        p {
          color: #4a5568;
          line-height: 1.6;
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

    @keyframes spin {
      to {
        transform: rotate(360deg);
      }
    }
  `]
})
export class ProjectDetailAdminComponent implements OnInit {
  project: Project | null = null;
  milestones: ProjectMilestone[] = [];
  loading = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private projectService: ProjectService
  ) {}

  ngOnInit(): void {
    const projectId = this.route.snapshot.paramMap.get('id');
    if (projectId) {
      this.loadProject(projectId);
    }
  }

  loadProject(id: string): void {
    this.loading = true;
    this.projectService.getProjectById(id).subscribe({
      next: (project) => {
        this.project = project;
        this.loadMilestones(id);
      },
      error: (error) => {
        this.loading = false;
        console.error('Error loading project:', error);
      }
    });
  }

  loadMilestones(projectId: string): void {
    this.projectService.getMilestonesByProjectId(projectId).subscribe({
      next: (milestones) => {
        this.milestones = milestones.sort((a, b) => a.orderIndex - b.orderIndex);
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
        console.error('Error loading milestones:', error);
      }
    });
  }

  getApprovedCount(): number {
    return this.milestones.filter(m => m.status === MilestoneStatus.APPROVED).length;
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

  isOverdue(milestone: ProjectMilestone): boolean {
    return this.projectService.isMilestoneOverdue(milestone);
  }

  getDaysSinceSubmission(milestone: ProjectMilestone): number {
    if (!milestone.submittedAt) return 0;
    const submitted = new Date(milestone.submittedAt);
    const today = new Date();
    const diffTime = today.getTime() - submitted.getTime();
    return Math.floor(diffTime / (1000 * 60 * 60 * 24));
  }

  examineMilestone(milestoneId: string): void {
    this.router.navigate(['/backoffice/admin/projects', this.project?.id, 'milestones', milestoneId, 'mediate']);
  }

  goBack(): void {
    this.router.navigate(['/backoffice/admin/projects']);
  }
}
