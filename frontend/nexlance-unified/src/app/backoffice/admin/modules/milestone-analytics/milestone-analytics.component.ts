import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';
import { ProjectService } from '../../../../core/services/project.service';
import { ProjectMilestone, MilestoneStatus } from '../../../../core/models/project.model';

@Component({
  selector: 'app-milestone-analytics',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    MatCardModule,
    MatIconModule,
    MatTableModule,
    MatChipsModule,
    MatProgressBarModule,
    MatPaginatorModule,
    MatSortModule,
    MatButtonModule
  ],
  templateUrl: './milestone-analytics.component.html',
  styleUrls: ['./milestone-analytics.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MilestoneAnalyticsComponent implements OnInit, AfterViewInit {

  // KPI stats — frozen after load, never changes
  stats = {
    totalMilestones: 0,
    approvalRate: 0,
    averageValidationDays: 0,
    averageRevisionsPerMilestone: 0
  };

  // Status distribution from backend
  statusDistribution: { status: string; count: number; percentage: number; color: string }[] = [];

  // Overdue milestones with precomputed days late
  overdueMilestones: (ProjectMilestone & { daysLate: number })[] = [];

  // Table data source — manages its own pagination/sorting
  dataSource = new MatTableDataSource<ProjectMilestone>([]);
  displayedColumns = ['title', 'status', 'dueDate', 'submittedAt', 'approvedAt'];
  totalMilestoneCount = 0;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  loading = true;

  constructor(private projectService: ProjectService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.loadData();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    if (this.sort) {
      this.dataSource.sort = this.sort;
    }
  }

  loadData(): void {
    this.loading = true;

    forkJoin({
      stats: this.projectService.getMilestoneStats(),
      milestones: this.projectService.getAllMilestones(),
      overdue: this.projectService.getOverdueMilestones()
    }).subscribe({
      next: ({ stats, milestones, overdue }) => {
        // 1. Process and freeze stats (completely independent from table)
        this.processStats(stats, milestones);
        this.processStatusDistribution(stats);

        // 2. Precompute overdue days
        const now = new Date();
        this.overdueMilestones = overdue.slice(0, 5).map(m => ({
          ...m,
          daysLate: Math.max(0, Math.ceil((now.getTime() - new Date(m.dueDate).getTime()) / (1000 * 60 * 60 * 24)))
        }));

        // 3. Set table data — Mat paginator handles pagination internally
        const sorted = [...milestones].sort((a, b) =>
          new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()
        );
        this.dataSource.data = sorted;
        this.totalMilestoneCount = milestones.length;

        // Wire up paginator if available
        if (this.paginator) {
          this.dataSource.paginator = this.paginator;
        }

        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (error: any) => {
        console.error('Error loading milestone analytics:', error);
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  processStats(backendStats: any, milestones: ProjectMilestone[]): void {
    const total = backendStats.total || 0;

    // Compute average validation time from milestones (submittedAt → approvedAt)
    const validationTimes: number[] = [];
    milestones.forEach(m => {
      if (m.submittedAt && m.approvedAt) {
        const submitted = new Date(m.submittedAt).getTime();
        const approvedDate = new Date(m.approvedAt).getTime();
        const diffDays = (approvedDate - submitted) / (1000 * 60 * 60 * 24);
        if (diffDays >= 0) {
          validationTimes.push(diffDays);
        }
      }
    });

    // Average revisions: rejected count / submitted+ milestones
    const submittedOrBeyond = milestones.filter(m =>
      m.status === MilestoneStatus.SUBMITTED ||
      m.status === MilestoneStatus.APPROVED ||
      m.status === MilestoneStatus.REJECTED
    );
    const totalRejected = backendStats.rejected || 0;

    // Freeze stats — they NEVER change after this point
    this.stats = Object.freeze({
      totalMilestones: total,
      approvalRate: backendStats.completionRate || 0,
      averageValidationDays: validationTimes.length > 0
        ? Math.round((validationTimes.reduce((a, b) => a + b, 0) / validationTimes.length) * 10) / 10
        : 0,
      averageRevisionsPerMilestone: submittedOrBeyond.length > 0
        ? Math.round((totalRejected / submittedOrBeyond.length) * 10) / 10
        : 0
    }) as any;
  }

  processStatusDistribution(backendStats: any): void {
    const total = backendStats.total || 1;
    const statuses = [
      { status: 'PENDING', count: backendStats.pending || 0, color: '#9e9e9e' },
      { status: 'IN_PROGRESS', count: backendStats.inProgress || 0, color: '#2196f3' },
      { status: 'SUBMITTED', count: backendStats.submitted || 0, color: '#ff9800' },
      { status: 'APPROVED', count: backendStats.approved || 0, color: '#4caf50' },
      { status: 'REJECTED', count: backendStats.rejected || 0, color: '#f44336' }
    ];

    this.statusDistribution = statuses
      .filter(s => s.count > 0)
      .map(s => ({
        ...s,
        percentage: Math.round((s.count / total) * 100)
      }));
  }

  getStatusColor(status: string): string {
    const colorMap: Record<string, string> = {
      'PENDING': 'status-pending',
      'IN_PROGRESS': 'status-progress',
      'SUBMITTED': 'status-submitted',
      'APPROVED': 'status-approved',
      'REJECTED': 'status-rejected'
    };
    return colorMap[status] || 'status-pending';
  }

  getStatusIcon(status: string): string {
    const iconMap: Record<string, string> = {
      'PENDING': 'schedule',
      'IN_PROGRESS': 'work',
      'SUBMITTED': 'send',
      'APPROVED': 'check_circle',
      'REJECTED': 'cancel'
    };
    return iconMap[status] || 'help';
  }
}
