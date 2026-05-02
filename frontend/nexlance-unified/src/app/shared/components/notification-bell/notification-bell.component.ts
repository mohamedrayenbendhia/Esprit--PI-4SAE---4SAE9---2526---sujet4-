import {
  Component, OnInit, OnDestroy, ChangeDetectorRef, ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatBadgeModule } from '@angular/material/badge';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subscription } from 'rxjs';
// MatTabsModule and MatChipsModule removed — not used in the template (custom filter-tab buttons are used instead)
import { NotificationService } from '../../../core/services/notification.service';
import { AuthService } from '../../../core/services/auth.service';
import { AppNotification, NotificationType, isComplaintNotification } from '../../../core/models/notification.model';
import { UserRole } from '../../models/user.model';
import { trigger, state, style, transition, animate } from '@angular/animations';
import { environment } from '../../../../environments/environment';

type FilterTab = 'all' | 'unread' | 'complaints' | 'other';

@Component({
  selector: 'app-notification-bell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, RouterModule,
    MatIconModule, MatButtonModule, MatBadgeModule,
    MatMenuModule, MatDividerModule, MatTooltipModule
  ],
  animations: [
    trigger('bellShake', [
      state('idle',    style({ transform: 'rotate(0deg)' })),
      state('shake',   style({ transform: 'rotate(0deg)' })),
      transition('idle => shake', animate('600ms ease', style({ transform: 'rotate(0deg)' }))),
    ]),
    trigger('toastIn', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(-12px)' }),
        animate('250ms ease-out', style({ opacity: 1, transform: 'translateY(0)' }))
      ]),
      transition(':leave', [
        animate('200ms ease-in', style({ opacity: 0, transform: 'translateY(-8px)' }))
      ])
    ])
  ],
  template: `
    <!-- ── Toast temps-réel ──────────────────────────────── -->
    @if (toast) {
      <div class="notif-toast" [@toastIn] (click)="onToastClick()">
        <mat-icon class="toast-icon" [style.color]="getTypeColor(toast.type)">
          {{ getTypeIcon(toast.type) }}
        </mat-icon>
        <div class="toast-body">
          <span class="toast-title">{{ toast.title }}</span>
          <span class="toast-msg">{{ toast.message }}</span>
        </div>
        <button class="toast-close" (click)="dismissToast($event)">
          <mat-icon>close</mat-icon>
        </button>
      </div>
    }

    <!-- ── Cloche ─────────────────────────────────────────── -->
    <button mat-icon-button [matMenuTriggerFor]="notifMenu"
            [matBadge]="unreadCount > 0 ? unreadCount : null"
            matBadgeColor="warn" matBadgeSize="small"
            matTooltip="Notifications"
            [@bellShake]="bellState"
            (click)="onBellClick()">
      <mat-icon>notifications</mat-icon>
    </button>

    <!-- ── Panneau ────────────────────────────────────────── -->
    <mat-menu #notifMenu="matMenu" class="notification-menu" xPosition="before">
      <div (click)="$event.stopPropagation()">

        <!-- En-tête -->
        <div class="notif-header">
          <span class="notif-title-main">Notifications</span>
          @if (unreadCount > 0) {
            <button mat-button color="primary" class="mark-all-btn" (click)="markAllRead()">
              Mark all as read
            </button>
          }
        </div>

        <!-- Filtres -->
        <div class="filter-tabs">
          @for (tab of filterTabs; track tab.key) {
            <button class="filter-tab" [class.active]="activeFilter === tab.key"
                    (click)="setFilter(tab.key)">
              {{ tab.label }}
              @if (tab.count > 0) {
                <span class="tab-count">{{ tab.count }}</span>
              }
            </button>
          }
        </div>

        <mat-divider></mat-divider>

        <!-- Liste -->
        <div class="notif-list">
          @if (loading) {
            <div class="empty-state">
              <mat-icon class="spin">refresh</mat-icon>
              <p>Loading…</p>
            </div>
          } @else if (filteredNotifications.length === 0) {
            <div class="empty-state">
              <mat-icon>notifications_none</mat-icon>
              <p>No notifications</p>
            </div>
          } @else {
            @for (notif of filteredNotifications.slice(0, 12); track notif.id) {
              <div class="notif-item" [class.unread]="!notif.read"
                   (click)="onNotificationClick(notif)">
                <div class="notif-icon-wrap"
                     [style.background]="getTypeBg(notif.type)">
                  <mat-icon [style.color]="getTypeColor(notif.type)">
                    {{ getTypeIcon(notif.type) }}
                  </mat-icon>
                </div>
                <div class="notif-content">
                  <span class="notif-item-title">{{ notif.title }}</span>
                  <span class="notif-item-msg">{{ notif.message }}</span>
                  <span class="notif-time">{{ getTimeAgo(notif.createdAt) }}</span>
                </div>
                @if (!notif.read) {
                  <div class="unread-dot"></div>
                }
              </div>
            }
            @if (filteredNotifications.length > 12) {
              <div class="see-more" (click)="navigateToAll()">
                View all notifications ({{ filteredNotifications.length }})
              </div>
            }
          }
        </div>
      </div>
    </mat-menu>
  `,
  styles: [`
    :host { position: relative; display: inline-block; }

    /* ── Toast ────────────────────────────────────────────── */
    .notif-toast {
      position: fixed;
      top: 72px;
      right: 20px;
      z-index: 9999;
      display: flex;
      align-items: flex-start;
      gap: 10px;
      background: #fff;
      border-radius: 8px;
      padding: 12px 14px;
      box-shadow: 0 4px 20px rgba(0,0,0,0.15);
      max-width: 340px;
      cursor: pointer;
      border-left: 4px solid #1565C0;
      transition: box-shadow 0.2s;
      &:hover { box-shadow: 0 6px 24px rgba(0,0,0,0.2); }
    }
    .toast-icon { flex-shrink: 0; font-size: 22px; width: 22px; height: 22px; margin-top: 2px; }
    .toast-body { display: flex; flex-direction: column; gap: 2px; flex: 1; min-width: 0; }
    .toast-title { font-weight: 600; font-size: 13px; color: #1a1a1a; white-space: nowrap;
                   overflow: hidden; text-overflow: ellipsis; }
    .toast-msg { font-size: 12px; color: #555; line-height: 1.4;
                 display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
                 overflow: hidden; }
    .toast-close { background: none; border: none; cursor: pointer; padding: 0;
                   color: #999; display: flex; align-items: center;
                   mat-icon { font-size: 16px; width: 16px; height: 16px; } }

    /* ── Bell animation ───────────────────────────────────── */
    @keyframes shake {
      0%,100%  { transform: rotate(0); }
      15%      { transform: rotate(15deg); }
      30%      { transform: rotate(-12deg); }
      45%      { transform: rotate(10deg); }
      60%      { transform: rotate(-8deg); }
      75%      { transform: rotate(5deg); }
    }
    :host.bell-shake mat-icon { animation: shake 0.6s ease; }

    /* ── Panneau ──────────────────────────────────────────── */
    .notif-header {
      display: flex; justify-content: space-between; align-items: center;
      padding: 14px 16px 8px;
    }
    .notif-title-main { font-size: 15px; font-weight: 700; color: #1a1a1a; }
    .mark-all-btn { font-size: 12px !important; padding: 0 8px !important; height: 28px !important; }

    /* ── Filtres ─────────────────────────────────────────── */
    .filter-tabs {
      display: flex; gap: 4px; padding: 0 12px 8px; overflow-x: auto;
      scrollbar-width: none; &::-webkit-scrollbar { display: none; }
    }
    .filter-tab {
      padding: 4px 10px; border-radius: 20px; border: 1px solid #e0e0e0;
      background: #fff; cursor: pointer; font-size: 12px; color: #555;
      white-space: nowrap; transition: all 0.15s;
      display: flex; align-items: center; gap: 4px;
      &:hover { background: #f0f4ff; border-color: #1565C0; }
      &.active { background: #E3F2FD; border-color: #1565C0; color: #1565C0; font-weight: 600; }
    }
    .tab-count {
      background: #1565C0; color: #fff; border-radius: 10px;
      padding: 0 5px; font-size: 10px; font-weight: 700; line-height: 16px;
    }

    /* ── Liste ───────────────────────────────────────────── */
    .notif-list {
      min-width: 360px; max-height: 420px; overflow-y: auto;
      scrollbar-width: thin; scrollbar-color: #ccc transparent;
    }
    .empty-state {
      text-align: center; padding: 28px; color: #aaa;
      mat-icon { font-size: 36px; width: 36px; height: 36px; display: block; margin: 0 auto 8px; }
      p { margin: 0; font-size: 13px; }
    }
    .spin { animation: spin 1.2s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }

    .notif-item {
      display: flex; align-items: flex-start; gap: 10px;
      padding: 10px 14px; cursor: pointer; transition: background 0.15s;
      position: relative;
      &:hover { background: #fafafa; }
      &.unread { background: rgba(21, 101, 192, 0.04); }
    }
    .notif-icon-wrap {
      flex-shrink: 0; width: 36px; height: 36px; border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      mat-icon { font-size: 18px; width: 18px; height: 18px; }
    }
    .notif-content {
      display: flex; flex-direction: column; gap: 1px; flex: 1; min-width: 0;
    }
    .notif-item-title {
      font-weight: 600; font-size: 12.5px; color: #1a1a1a;
      white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
    }
    .notif-item-msg {
      font-size: 12px; color: #666; line-height: 1.4;
      display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
    }
    .notif-time { font-size: 11px; color: #999; margin-top: 2px; }
    .unread-dot {
      width: 8px; height: 8px; border-radius: 50%; background: #1565C0;
      flex-shrink: 0; margin-top: 6px;
    }
    .see-more {
      text-align: center; padding: 10px; font-size: 12px; color: #1565C0;
      cursor: pointer; font-weight: 600;
      &:hover { background: #f0f4ff; }
    }
  `]
})
export class NotificationBellComponent implements OnInit, OnDestroy {

  notifications: AppNotification[] = [];
  filteredNotifications: AppNotification[] = [];
  unreadCount = 0;
  loading = true;
  activeFilter: FilterTab = 'all';
  bellState: 'idle' | 'shake' = 'idle';
  toast: AppNotification | null = null;
  private toastTimer: ReturnType<typeof setTimeout> | null = null;

  readonly filterTabs: { key: FilterTab; label: string; count: number }[] = [
    { key: 'all',        label: 'All',        count: 0 },
    { key: 'unread',     label: 'Unread',     count: 0 },
    { key: 'complaints', label: 'Complaints', count: 0 },
    { key: 'other',      label: 'Other',      count: 0 }
  ];

  private subs: Subscription[] = [];

  constructor(
    private notificationService: NotificationService,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // ── Abonnements au service (unique source de vérité) ──────
    this.subs.push(
      this.notificationService.notifications$.subscribe(list => {
        this.notifications = list;
        if (list !== null) this.loading = false;
        this.applyFilter();
        this.cdr.markForCheck();
      }),
      this.notificationService.unreadCount$.subscribe(count => {
        this.unreadCount = count;
        this.cdr.markForCheck();
      }),
      this.notificationService.newNotification$.subscribe(notif => {
        this.showToast(notif);
        this.shakeBell();
        this.cdr.markForCheck();
      })
    );

    // ── Connexion et chargement initial ───────────────────────
    // connect() gère à la fois le chargement REST et le WebSocket
    const user = this.authService.getCurrentUser();
    if (user?.id) {
      this.notificationService.connect();
    } else {
      this.subs.push(
        this.authService.currentUser$.subscribe(u => {
          if (u?.id) this.notificationService.connect();
        })
      );
    }
  }

  // ── Filtres ────────────────────────────────────────────────

  setFilter(tab: FilterTab): void {
    this.activeFilter = tab;
    this.applyFilter();
    this.cdr.markForCheck();
  }

  private applyFilter(): void {
    const all = this.notifications;

    this.filteredNotifications = {
      all:        all,
      unread:     all.filter(n => !n.read),
      complaints: all.filter(n => isComplaintNotification(n.type)),
      other:      all.filter(n => !isComplaintNotification(n.type))
    }[this.activeFilter];

    // Mettre à jour les compteurs des tabs
    this.filterTabs[0].count = all.filter(n => !n.read).length;
    this.filterTabs[1].count = all.filter(n => !n.read).length;
    this.filterTabs[2].count = all.filter(n => isComplaintNotification(n.type) && !n.read).length;
    this.filterTabs[3].count = all.filter(n => !isComplaintNotification(n.type) && !n.read).length;
  }

  // ── Actions ────────────────────────────────────────────────

  onBellClick(): void {
    // Marquer l'affichage comme vu (pas encore lu — le clic individuel marque comme lu)
  }

  markAllRead(): void {
    const user = this.authService.getCurrentUser();
    if (!user?.id) return;

    // Mise à jour optimiste locale immédiate
    this.applyOptimisticReadAll();

    this.notificationService.markAllAsRead(user.id).subscribe({
      next: () => { this.cdr.markForCheck(); },
      error: () => {
        // Endpoint unavailable — keep optimistic local update
        if (!environment.production) console.warn('[Bell] markAllAsRead: endpoint unavailable, local state preserved');
        this.cdr.markForCheck();
      }
    });
  }

  private applyOptimisticReadAll(): void {
    this.notifications = this.notifications.map(n => ({ ...n, read: true }));
    this.unreadCount   = 0;
    this.applyFilter();
    this.cdr.markForCheck();
  }

  onNotificationClick(notif: AppNotification): void {
    this.notificationService.markAsReadAndUpdate(notif);
    const route = this.getRoute(notif);
    if (route) this.router.navigate(route);
  }

  navigateToAll(): void {
    // Page dédiée si elle existe, sinon rester sur place
  }

  // ── Toast ──────────────────────────────────────────────────

  private showToast(notif: AppNotification): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toast = notif;
    this.toastTimer = setTimeout(() => {
      this.toast = null;
      this.cdr.markForCheck();
    }, 5000);
  }

  onToastClick(): void {
    if (this.toast) {
      this.onNotificationClick(this.toast);
      this.dismissToast();
    }
  }

  dismissToast(event?: Event): void {
    event?.stopPropagation();
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toast = null;
    this.cdr.markForCheck();
  }

  // ── Bell shake ─────────────────────────────────────────────

  private shakeBell(): void {
    this.bellState = 'shake';
    setTimeout(() => { this.bellState = 'idle'; this.cdr.markForCheck(); }, 700);
  }

  // ── Routing ────────────────────────────────────────────────

  private getRoute(notif: AppNotification): string[] | null {
    const user = this.authService.getCurrentUser();
    if (!user) return null;

    const refId   = notif.referenceId;
    const type    = notif.type;
    const isAdmin = user.role === UserRole.ADMIN;
    const isAgent = user.role === UserRole.SUPPORT_AGENT;
    const isCli   = user.role === UserRole.CLIENT;

    // ── Routes réclamations ────────────────────────────────
    if (isComplaintNotification(type)) {
      if (isAdmin) {
        return refId ? ['/backoffice/admin/complaints', refId] : ['/backoffice/admin/complaints'];
      }
      if (isAgent) {
        return refId ? ['/backoffice/agent/complaints', refId] : ['/backoffice/agent/queue'];
      }
      if (isCli) {
        return refId ? ['/frontoffice/client/my-complaints', refId] : ['/frontoffice/client/my-complaints'];
      }
      // Freelancer
      return refId ? ['/frontoffice/freelancer/my-complaints', refId] : ['/frontoffice/freelancer/my-complaints'];
    }

    // ── Routes existantes ──────────────────────────────────
    switch (type) {
      case 'JOB_OFFER':
        if (isAdmin) return ['/backoffice/admin/jobs'];
        if (isCli)   return refId ? ['/frontoffice/client/my-jobs', refId] : ['/frontoffice/client/my-jobs'];
        return refId ? ['/frontoffice/freelancer/jobs', refId] : ['/frontoffice/freelancer/browse-jobs'];

      case 'PROJECT':
        if (isAdmin) return refId ? ['/backoffice/admin/projects', refId] : ['/backoffice/admin/projects'];
        if (isCli)   return refId ? ['/frontoffice/client/projects', refId, 'dashboard'] : ['/frontoffice/client/projects'];
        return refId ? ['/frontoffice/freelancer/my-projects', refId] : ['/frontoffice/freelancer/my-projects'];

      case 'APPLICATION':
        if (isCli) return refId ? ['/frontoffice/client/my-jobs', refId] : ['/frontoffice/client/my-jobs'];
        return ['/frontoffice/freelancer/my-applications'];

      case 'INVITATION':
        return refId ? ['/frontoffice/freelancer/my-invitations', refId] : ['/frontoffice/freelancer/my-invitations'];

      case 'RECOMMENDATION':
        if (isCli) return refId ? ['/frontoffice/client/my-recommendations', refId] : ['/frontoffice/client/my-recommendations'];
        return ['/frontoffice/freelancer/my-recommendations'];

      case 'MILESTONE': case 'DEADLINE': case 'OVERDUE':
        if (isAdmin) return ['/backoffice/admin/analytics/milestones'];
        if (isCli)   return refId ? ['/frontoffice/client/projects', refId, 'dashboard'] : ['/frontoffice/client/projects'];
        return refId ? ['/frontoffice/freelancer/my-projects', refId] : ['/frontoffice/freelancer/my-projects'];

      default: return null;
    }
  }

  // ── Icônes & couleurs ──────────────────────────────────────

  getTypeIcon(type: NotificationType): string {
    const icons: Partial<Record<NotificationType, string>> = {
      COMPLAINT_CREATED:   'add_circle',
      COMPLAINT_STATUS:    'sync',
      COMPLAINT_RESOLVED:  'check_circle',
      COMPLAINT_CLOSED:    'lock',
      COMPLAINT_MESSAGE:   'chat_bubble',
      COMPLAINT_INVOLVED:  'person_add',
      COMPLAINT_ASSIGNED:  'assignment_ind',
      COMPLAINT_ESCALATED: 'arrow_upward',
      APPLICATION:    'description',
      RECOMMENDATION: 'thumb_up',
      INVITATION:     'mail',
      MILESTONE:      'flag',
      DEADLINE:       'access_time',
      OVERDUE:        'warning',
      JOB_OFFER:      'work',
      PROJECT:        'folder'
    };
    return icons[type] || 'notifications';
  }

  getTypeColor(type: NotificationType): string {
    if (isComplaintNotification(type)) {
      const colors: Partial<Record<NotificationType, string>> = {
        COMPLAINT_CREATED:   '#1565C0',
        COMPLAINT_STATUS:    '#F57C00',
        COMPLAINT_RESOLVED:  '#2E7D32',
        COMPLAINT_CLOSED:    '#616161',
        COMPLAINT_MESSAGE:   '#0288D1',
        COMPLAINT_INVOLVED:  '#6A1B9A',
        COMPLAINT_ASSIGNED:  '#00838F',
        COMPLAINT_ESCALATED: '#C62828'
      };
      return colors[type] || '#1565C0';
    }
    const others: Partial<Record<NotificationType, string>> = {
      APPLICATION:    '#4CAF50',
      RECOMMENDATION: '#FF9800',
      INVITATION:     '#2196F3',
      MILESTONE:      '#9C27B0',
      DEADLINE:       '#F44336',
      OVERDUE:        '#F44336',
      JOB_OFFER:      '#00BCD4',
      PROJECT:        '#607D8B'
    };
    return others[type] || '#757575';
  }

  getTypeBg(type: NotificationType): string {
    return this.getTypeColor(type) + '18'; // couleur + 10% opacité
  }

  getTimeAgo(dateStr?: string): string {
    if (!dateStr) return '';
    const diff    = Date.now() - new Date(dateStr).getTime();
    const minutes = Math.floor(diff / 60000);
    const hours   = Math.floor(diff / 3600000);
    const days    = Math.floor(diff / 86400000);
    if (minutes < 1)  return 'Just now';
    if (minutes < 60) return `${minutes} min ago`;
    if (hours < 24)   return `${hours}h ago`;
    if (days < 7)     return `${days}d ago`;
    return new Date(dateStr).toLocaleDateString('en-US');
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
    if (this.toastTimer) clearTimeout(this.toastTimer);
  }
}
