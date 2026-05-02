import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

export interface SmartBadge {
  label: string;
  icon: string;
  color: string;
  bgColor: string;
  tooltip: string;
}

@Component({
  selector: 'app-job-smart-badges',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatTooltipModule],
  template: `
    <div class="smart-badges" *ngIf="badges.length > 0">
      <span
        *ngFor="let badge of badges"
        class="smart-badge"
        [style.color]="badge.color"
        [style.background]="badge.bgColor"
        [matTooltip]="badge.tooltip">
        <mat-icon class="badge-icon">{{ badge.icon }}</mat-icon>
        {{ badge.label }}
      </span>
    </div>
  `,
  styles: [`
    .smart-badges {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      margin-bottom: 8px;
    }

    .smart-badge {
      display: inline-flex;
      align-items: center;
      gap: 3px;
      padding: 2px 8px;
      border-radius: 12px;
      font-size: 0.688rem;
      font-weight: 700;
      letter-spacing: 0.3px;
      text-transform: uppercase;
      white-space: nowrap;

      .badge-icon {
        font-size: 12px;
        width: 12px;
        height: 12px;
      }
    }
  `]
})
export class JobSmartBadgesComponent {
  @Input() set job(value: any) {
    if (value) {
      this.badges = this.computeBadges(value);
    }
  }

  badges: SmartBadge[] = [];

  private computeBadges(job: any): SmartBadge[] {
    const badges: SmartBadge[] = [];
    const now = new Date();

    // NEW - posted within last 3 days
    if (job.createdAt || job.publishedAt) {
      const posted = new Date(job.publishedAt || job.createdAt);
      const daysSincePosted = Math.floor((now.getTime() - posted.getTime()) / (1000 * 60 * 60 * 24));
      if (daysSincePosted <= 3) {
        badges.push({
          label: 'New',
          icon: 'fiber_new',
          color: '#059669',
          bgColor: '#ECFDF5',
          tooltip: `Posted ${daysSincePosted === 0 ? 'today' : daysSincePosted + ' days ago'}`
        });
      }
    }

    // HOT - many applicants (>10)
    if (job.applicantCount && job.applicantCount >= 10) {
      badges.push({
        label: 'Hot',
        icon: 'local_fire_department',
        color: '#DC2626',
        bgColor: '#FEF2F2',
        tooltip: `${job.applicantCount} applicants already`
      });
    }

    // CLOSING SOON - deadline within 3 days
    if (job.deadline) {
      const deadline = new Date(job.deadline);
      const daysUntilDeadline = Math.floor((deadline.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
      if (daysUntilDeadline >= 0 && daysUntilDeadline <= 3) {
        badges.push({
          label: 'Closing Soon',
          icon: 'timer',
          color: '#D97706',
          bgColor: '#FFFBEB',
          tooltip: `${daysUntilDeadline === 0 ? 'Closes today' : 'Closes in ' + daysUntilDeadline + ' days'}`
        });
      }
    }

    // HIGH BUDGET - budget > 5000
    if (job.budget && job.budget >= 5000) {
      badges.push({
        label: 'High Budget',
        icon: 'payments',
        color: '#7C3AED',
        bgColor: '#F5F3FF',
        tooltip: `Budget: ${job.budget} DT`
      });
    }

    // REMOTE
    if (job.isRemote) {
      badges.push({
        label: 'Remote',
        icon: 'laptop',
        color: '#0284C7',
        bgColor: '#F0F9FF',
        tooltip: 'Remote work available'
      });
    }

    // URGENT - if marked urgent
    if (job.isUrgent) {
      badges.push({
        label: 'Urgent',
        icon: 'priority_high',
        color: '#DC2626',
        bgColor: '#FEF2F2',
        tooltip: 'Urgent hire needed'
      });
    }

    return badges.slice(0, 4); // Max 4 badges
  }
}
