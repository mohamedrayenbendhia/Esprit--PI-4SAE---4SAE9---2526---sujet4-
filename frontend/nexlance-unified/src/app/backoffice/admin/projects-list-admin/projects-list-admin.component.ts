import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProjectService } from '../../../core/services/project.service';
import { Project, ProjectStatus, MilestoneStatus } from '../../../core/models/project.model';

@Component({
  selector: 'app-projects-list-admin',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="admin-projects-container">
      <div class="page-header">
        <h1>Project Management</h1>
        <button class="btn-refresh" (click)="loadProjects()">🔄 Refresh</button>
      </div>

      <!-- Filters -->
      <div class="filters-section">
        <div class="filter-group">
          <label>Status:</label>
          <select [(ngModel)]="selectedStatus" (change)="applyFilters()" class="filter-select">
            <option value="">All</option>
            <option [value]="ProjectStatus.ACTIVE">Active</option>
            <option [value]="ProjectStatus.ON_HOLD">On Hold</option>
            <option [value]="ProjectStatus.COMPLETED">Completed</option>
            <option [value]="ProjectStatus.CANCELLED">Cancelled</option>
          </select>
        </div>

        <div class="filter-group">
          <label>Search:</label>
          <input 
            type="text" 
            [(ngModel)]="searchTerm" 
            (input)="applyFilters()"
            placeholder="Client ID ou Freelance ID..."
            class="filter-input"
          />
        </div>
      </div>

      <!-- Projects Table -->
      <div class="table-container" *ngIf="!loading">
        <table class="projects-table">
          <thead>
            <tr>
              <th>Project ID</th>
              <th>Client</th>
              <th>Freelance</th>
              <th>Milestones</th>
              <th>Progress</th>
              <th>Status</th>
              <th>Alerts</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let project of filteredProjects" [class.has-alert]="hasAlert(project)">
              <td><code>{{ project.id?.substring(0, 8) }}...</code></td>
              <td><code>{{ project.clientId?.substring(0, 8) }}</code></td>
              <td><code>{{ project.freelanceId?.substring(0, 8) }}</code></td>
              <td>
                {{ getApprovedMilestones(project) }} / {{ getTotalMilestones(project) }}
              </td>
              <td>
                <div class="progress-bar-small">
                  <div class="progress-fill-small" [style.width.%]="project.progress || 0">
                    {{ project.progress || 0 }}%
                  </div>
                </div>
              </td>
              <td>
                <span class="status-badge" [class]="'status-' + project.status">
                  {{ project.status }}
                </span>
              </td>
              <td>
                <span class="alert-badge" *ngIf="hasAlert(project)">
                  ⚠️ Overdue milestones
                </span>
              </td>
              <td class="actions-cell">
                <button class="btn-sm btn-view" (click)="viewProject(project.id!)">
                  View
                </button>
                <button class="btn-sm btn-mediate" *ngIf="hasAlert(project)" (click)="mediateProject(project.id!)">
                  Mediation
                </button>
              </td>
            </tr>
          </tbody>
        </table>

        <div class="empty-state" *ngIf="filteredProjects.length === 0">
          <p>No projects found</p>
        </div>
      </div>

      <!-- Loading -->
      <div class="loading-container" *ngIf="loading">
        <div class="spinner"></div>
        <p>Loading projects...</p>
      </div>

      <!-- Error -->
      <div class="alert alert-error" *ngIf="errorMessage">
        {{ errorMessage }}
      </div>
    </div>
  `,
  styles: [`
    .admin-projects-container {
      max-width: 1600px;
      margin: 0 auto;
      padding: 2rem;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 2rem;

      h1 {
        font-size: 2rem;
        color: #1a1a1a;
      }

      .btn-refresh {
        padding: 0.75rem 1.5rem;
        background: #4299e1;
        color: white;
        border: none;
        border-radius: 6px;
        cursor: pointer;

        &:hover {
          background: #3182ce;
        }
      }
    }

    .filters-section {
      background: white;
      padding: 1.5rem;
      border-radius: 8px;
      margin-bottom: 1.5rem;
      display: flex;
      gap: 2rem;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

      .filter-group {
        display: flex;
        flex-direction: column;
        gap: 0.5rem;

        label {
          font-weight: 500;
          color: #2d3748;
        }

        .filter-select,
        .filter-input {
          padding: 0.5rem;
          border: 1px solid #cbd5e0;
          border-radius: 4px;
          font-size: 0.95rem;
        }
      }
    }

    .table-container {
      background: white;
      border-radius: 8px;
      overflow: hidden;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

      .projects-table {
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

            &.has-alert {
              background: #fff5f5;
            }

            td {
              padding: 1rem;
              color: #4a5568;

              code {
                background: #edf2f7;
                padding: 0.25rem 0.5rem;
                border-radius: 4px;
                font-size: 0.85rem;
              }
            }

            .progress-bar-small {
              width: 100px;
              height: 24px;
              background: #e2e8f0;
              border-radius: 12px;
              overflow: hidden;

              .progress-fill-small {
                height: 100%;
                background: #48bb78;
                display: flex;
                align-items: center;
                justify-content: center;
                color: white;
                font-size: 0.75rem;
                font-weight: bold;
              }
            }

            .status-badge {
              padding: 0.25rem 0.75rem;
              border-radius: 12px;
              font-size: 0.85rem;
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

              &.status-CANCELLED {
                background: #fed7d7;
                color: #742a2a;
              }
            }

            .alert-badge {
              padding: 0.25rem 0.75rem;
              background: #fed7d7;
              color: #742a2a;
              border-radius: 12px;
              font-size: 0.85rem;
              font-weight: 500;
            }

            .actions-cell {
              display: flex;
              gap: 0.5rem;

              .btn-sm {
                padding: 0.5rem 1rem;
                border: none;
                border-radius: 4px;
                font-size: 0.85rem;
                cursor: pointer;
                font-weight: 500;

                &.btn-view {
                  background: #4299e1;
                  color: white;

                  &:hover {
                    background: #3182ce;
                  }
                }

                &.btn-mediate {
                  background: #fc8181;
                  color: white;

                  &:hover {
                    background: #f56565;
                  }
                }
              }
            }
          }
        }
      }

      .empty-state {
        padding: 3rem;
        text-align: center;
        color: #718096;
      }
    }

    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 300px;

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
      margin-top: 1.5rem;
    }

    @keyframes spin {
      to {
        transform: rotate(360deg);
      }
    }
  `]
})
export class ProjectsListAdminComponent implements OnInit {
  projects: Project[] = [];
  filteredProjects: Project[] = [];
  loading = true;
  errorMessage = '';
  
  selectedStatus = '';
  searchTerm = '';
  
  ProjectStatus = ProjectStatus;

  constructor(
    private projectService: ProjectService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadProjects();
  }

  loadProjects(): void {
    this.loading = true;
    this.projectService.getAllProjects().subscribe({
      next: (projects) => {
        this.projects = projects;
        this.applyFilters();
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = 'Error loading projects';
        console.error('Error loading projects:', error);
      }
    });
  }

  applyFilters(): void {
    let filtered = [...this.projects];

    if (this.selectedStatus) {
      filtered = filtered.filter(p => p.status === this.selectedStatus);
    }

    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(p => 
        p.clientId?.toLowerCase().includes(term) ||
        p.freelanceId?.toLowerCase().includes(term)
      );
    }

    this.filteredProjects = filtered;
  }

  getTotalMilestones(project: Project): number {
    return project.milestones?.length || 0;
  }

  getApprovedMilestones(project: Project): number {
    return project.milestones?.filter(m => m.status === MilestoneStatus.APPROVED).length || 0;
  }

  hasAlert(project: Project): boolean {
    // Check if project has overdue milestones
    return project.milestones?.some(m => 
      this.projectService.isMilestoneOverdue(m)
    ) || false;
  }

  viewProject(projectId: string): void {
    this.router.navigate(['/backoffice/admin/projects', projectId]);
  }

  mediateProject(projectId: string): void {
    this.router.navigate(['/backoffice/admin/projects', projectId]);
  }
}
