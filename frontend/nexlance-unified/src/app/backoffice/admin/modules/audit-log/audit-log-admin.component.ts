import { Component, OnInit, OnDestroy, inject, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, MatSort, Sort } from '@angular/material/sort';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';
import { Subscription, forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuditLogService, AuditLog, AuditStats } from '../../../../core/services/audit-log.service';
import { UserService } from '../../../../core/services/user.service';

@Component({
  selector: 'app-audit-log-admin',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatTableModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatChipsModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatSortModule,
    TranslateModule
  ],
  templateUrl: './audit-log-admin.component.html',
  styleUrls: ['./audit-log-admin.component.scss']
})
export class AuditLogAdminComponent implements OnInit, OnDestroy {
  private auditLogService = inject(AuditLogService);
  private userService = inject(UserService);
  private wsSub?: Subscription;

  @ViewChild(MatSort) sort!: MatSort;

  logs = signal<AuditLog[]>([]);
  filteredLogs = signal<AuditLog[]>([]);
  stats = signal<AuditStats | null>(null);
  isLoading = signal(true);
  isLive = signal(true);

  // User name resolution
  userNameMap = new Map<string, string>();

  // Filters
  entityTypeFilter = '';
  userFilter = '';
  actionFilter = '';
  dateFrom = '';
  dateTo = '';

  // Sorting
  currentSort: Sort = { active: 'timestamp', direction: 'desc' };

  // Pagination
  pageSize = 20;
  pageIndex = 0;
  pagedLogs = signal<AuditLog[]>([]);

  displayedColumns = ['timestamp', 'action', 'entityType', 'entityId', 'userId', 'details'];

  entityTypes = ['JOB_OFFER', 'APPLICATION', 'RECOMMENDATION', 'INVITATION', 'PROJECT', 'MILESTONE', 'USER'];
  actions = ['CREATE', 'UPDATE', 'DELETE', 'STATUS_CHANGE', 'APPROVE', 'REJECT', 'ARCHIVE'];

  ngOnInit(): void {
    this.loadData();
    this.startLiveUpdates();
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
    this.auditLogService.disconnectWebSocket();
  }

  /** Connect to WebSocket and auto-prepend new logs in real-time */
  startLiveUpdates(): void {
    this.auditLogService.connectWebSocket();
    this.wsSub = this.auditLogService.newLog$.subscribe((newLog) => {
      // Prepend new log to the top of the list
      const updatedLogs = [newLog, ...this.logs()];
      this.logs.set(updatedLogs);

      // Resolve user name for new log if not cached
      if (newLog.userId && !this.userNameMap.has(newLog.userId)) {
        this.userService.getUserById(newLog.userId).pipe(
          catchError(() => of(null))
        ).subscribe(user => {
          if (user) {
            this.userNameMap.set(newLog.userId, `${user.firstName} ${user.lastName}`);
          } else {
            this.userNameMap.set(newLog.userId, newLog.userId.substring(0, 8) + '...');
          }
          this.applyFilters();
        });
      } else {
        this.applyFilters();
      }

      // Update stats: increment total
      const currentStats = this.stats();
      if (currentStats) {
        this.stats.set({
          ...currentStats,
          totalLogs: currentStats.totalLogs + 1,
          todayLogs: (currentStats.todayLogs || 0) + 1
        });
      }
    });
  }

  loadData(): void {
    this.isLoading.set(true);
    this.auditLogService.getRecentLogs().subscribe({
      next: (logs) => {
        this.logs.set(logs);
        this.resolveUserNames(logs);
        this.applyFilters();
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });

    this.auditLogService.getAuditStats().subscribe({
      next: (stats) => this.stats.set(stats)
    });
  }

  /** Resolve user IDs to display names */
  resolveUserNames(logs: AuditLog[]): void {
    const uniqueIds = [...new Set(logs.map(l => l.userId).filter(id => id && !this.userNameMap.has(id)))];
    if (uniqueIds.length === 0) return;

    const requests = uniqueIds.map(id =>
      this.userService.getUserById(id).pipe(
        catchError(() => of(null))
      )
    );

    forkJoin(requests).subscribe(users => {
      users.forEach((user, i) => {
        if (user) {
          this.userNameMap.set(uniqueIds[i], `${user.firstName} ${user.lastName}`);
        } else {
          this.userNameMap.set(uniqueIds[i], uniqueIds[i].substring(0, 8) + '...');
        }
      });
      // Re-apply filters to refresh display
      this.applyFilters();
    });
  }

  /** Get display name for a userId */
  getUserName(userId: string): string {
    if (!userId) return '-';
    return this.userNameMap.get(userId) || userId.substring(0, 8) + '...';
  }

  applyFilters(): void {
    let filtered = [...this.logs()];

    if (this.entityTypeFilter) {
      filtered = filtered.filter(l => l.entityType === this.entityTypeFilter);
    }
    if (this.actionFilter) {
      filtered = filtered.filter(l => l.action === this.actionFilter);
    }
    if (this.userFilter) {
      const term = this.userFilter.toLowerCase();
      filtered = filtered.filter(l => {
        const name = this.userNameMap.get(l.userId)?.toLowerCase() || '';
        const role = l.userRole?.toLowerCase() || '';
        return name.includes(term) || role.includes(term) || l.userId?.toLowerCase().includes(term);
      });
    }
    if (this.dateFrom) {
      const from = new Date(this.dateFrom);
      filtered = filtered.filter(l => new Date(l.timestamp) >= from);
    }
    if (this.dateTo) {
      const to = new Date(this.dateTo);
      to.setHours(23, 59, 59);
      filtered = filtered.filter(l => new Date(l.timestamp) <= to);
    }

    // Apply sorting
    filtered = this.sortData(filtered);

    this.filteredLogs.set(filtered);
    this.pageIndex = 0;
    this.updatePagedLogs();
  }

  onSortChange(sort: Sort): void {
    this.currentSort = sort;
    this.applyFilters();
  }

  sortData(data: AuditLog[]): AuditLog[] {
    const { active, direction } = this.currentSort;
    if (!active || direction === '') return data;

    const dir = direction === 'asc' ? 1 : -1;
    return data.sort((a, b) => {
      let valA: any, valB: any;
      switch (active) {
        case 'timestamp':
          valA = new Date(a.timestamp).getTime();
          valB = new Date(b.timestamp).getTime();
          break;
        case 'action':
          valA = a.action;
          valB = b.action;
          break;
        case 'entityType':
          valA = a.entityType;
          valB = b.entityType;
          break;
        case 'entityId':
          valA = a.entityId;
          valB = b.entityId;
          break;
        case 'userId':
          valA = this.getUserName(a.userId).toLowerCase();
          valB = this.getUserName(b.userId).toLowerCase();
          break;
        case 'details':
          valA = a.details || '';
          valB = b.details || '';
          break;
        default:
          return 0;
      }
      if (valA < valB) return -1 * dir;
      if (valA > valB) return 1 * dir;
      return 0;
    });
  }

  updatePagedLogs(): void {
    const start = this.pageIndex * this.pageSize;
    this.pagedLogs.set(this.filteredLogs().slice(start, start + this.pageSize));
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.updatePagedLogs();
  }

  clearFilters(): void {
    this.entityTypeFilter = '';
    this.userFilter = '';
    this.actionFilter = '';
    this.dateFrom = '';
    this.dateTo = '';
    this.applyFilters();
  }

  refreshLogs(): void {
    this.loadData();
  }

  getActionColor(action: string): string {
    switch (action) {
      case 'CREATE': return '#10B981';
      case 'UPDATE': return '#3B82F6';
      case 'DELETE': return '#EF4444';
      case 'STATUS_CHANGE': return '#F59E0B';
      case 'APPROVE': return '#10B981';
      case 'REJECT': return '#EF4444';
      default: return '#6B7280';
    }
  }

  getActionIcon(action: string): string {
    switch (action) {
      case 'CREATE': return 'add_circle';
      case 'UPDATE': return 'edit';
      case 'DELETE': return 'delete';
      case 'STATUS_CHANGE': return 'swap_horiz';
      case 'APPROVE': return 'check_circle';
      case 'REJECT': return 'cancel';
      default: return 'info';
    }
  }

  formatTimestamp(ts: string): string {
    return new Date(ts).toLocaleString('en-US', {
      month: 'short', day: 'numeric', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  getTopActionEntries(): [string, number][] {
    const s = this.stats();
    if (!s?.topActions) return [];
    return Object.entries(s.topActions).sort((a, b) => b[1] - a[1]).slice(0, 5);
  }

  getTopEntityEntries(): [string, number][] {
    const s = this.stats();
    if (!s?.topEntityTypes) return [];
    return Object.entries(s.topEntityTypes).sort((a, b) => b[1] - a[1]).slice(0, 5);
  }
}
