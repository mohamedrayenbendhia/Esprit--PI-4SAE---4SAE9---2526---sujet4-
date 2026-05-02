import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Milestone, MilestoneStatus } from '@shared/models/contract.model';

@Component({
  selector: 'app-milestone-tracker',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="milestone-tracker">
      @for (milestone of milestones; track milestone.id) {
        <div class="milestone-item" [ngClass]="getStatusClass(milestone.status)">
          <div class="milestone-dot"></div>
          <div class="milestone-content">
            <span class="milestone-title">{{ milestone.title }}</span>
            <span class="milestone-status">{{ formatStatus(milestone.status) }}</span>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .milestone-tracker { display: flex; flex-direction: column; gap: 8px; }
    .milestone-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 8px 12px;
      border-radius: 8px;
      background: #f5f5f5;
    }
    .milestone-dot {
      width: 12px;
      height: 12px;
      border-radius: 50%;
      flex-shrink: 0;
    }
    .milestone-content { display: flex; flex-direction: column; }
    .milestone-title { font-weight: 500; font-size: 14px; }
    .milestone-status { font-size: 12px; color: #757575; }
    .status-pending .milestone-dot { background: #bdbdbd; }
    .status-in-progress .milestone-dot { background: #42a5f5; }
    .status-awaiting .milestone-dot { background: #ffa726; }
    .status-validated .milestone-dot { background: #66bb6a; }
    .status-rejected .milestone-dot { background: #ef5350; }
  `]
})
export class MilestoneTrackerComponent {
  @Input() milestones: Milestone[] = [];

  getStatusClass(status?: MilestoneStatus): string {
    switch (status) {
      case MilestoneStatus.PENDING: return 'status-pending';
      case MilestoneStatus.IN_PROGRESS: return 'status-in-progress';
      case MilestoneStatus.AWAITING_VALIDATION: return 'status-awaiting';
      case MilestoneStatus.VALIDATED: return 'status-validated';
      case MilestoneStatus.REJECTED: return 'status-rejected';
      default: return 'status-pending';
    }
  }

  formatStatus(status?: MilestoneStatus): string {
    return (status || 'PENDING').replace(/_/g, ' ');
  }
}
