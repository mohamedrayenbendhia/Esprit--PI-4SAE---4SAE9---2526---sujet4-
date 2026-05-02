import { Component, OnInit, OnDestroy, signal, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { AuthService } from '@core/services/auth.service';
import { ComplaintService } from '@core/services/complaint.service';
import { ComplaintAdvancedService } from '@core/services/complaint-advanced.service';
import { NotificationService } from '@core/services/notification.service';
import {
  Complaint, ComplaintStatus,
  STATUS_LABELS, STATUS_COLORS, PRIORITY_LABELS, PRIORITY_COLORS, CATEGORY_LABELS
} from '@core/models/complaint.model';
import {
  AttachmentManagerComponent
} from '../../../shared/components/attachment-manager/attachment-manager.component';
import { ComplaintConversationComponent } from '../../../shared/components/conversation/complaint-conversation.component';
import { MediationPanelComponent } from '../../../shared/components/mediation-panel/mediation-panel.component';
import { NpsWidgetComponent } from '../../../shared/components/nps-widget/nps-widget.component';

@Component({
  selector: 'app-complaint-detail',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatIconModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule,
    TranslateModule,
    AttachmentManagerComponent,
    ComplaintConversationComponent,
    MediationPanelComponent,
    NpsWidgetComponent,
  ],
  templateUrl: './complaint-detail.component.html',
  styleUrls: ['./complaint-detail.component.scss']
})
export class ComplaintDetailComponent implements OnInit, OnDestroy {

  @Input() userType: 'client' | 'freelancer' = 'client';

  complaint       = signal<Complaint | null>(null);
  isLoading       = signal(true);
  isRating        = signal(false);
  hoverRating     = signal(0);
  selectedRating  = signal(0);
  isReportedParty = signal(false);

  // Reopen
  showReopenForm  = signal(false);
  reopenReason    = signal('');
  isReopening     = signal(false);

  // Mediation visibility
  showMediation   = signal(false);

  currentUserId: string = '';

  ComplaintStatus = ComplaintStatus;

  private notifSub?: Subscription;

  constructor(
    private route:               ActivatedRoute,
    private router:              Router,
    private complaintService:    ComplaintService,
    private advService:          ComplaintAdvancedService,
    private authService:         AuthService,
    private notificationService: NotificationService,
    private snackBar:            MatSnackBar
  ) {}

  ngOnInit(): void {
    this.currentUserId = this.authService.getCurrentUser()?.id || '';
    // Detect context from the actual URL — @Input() userType is never bound by the router
    // because withComponentInputBinding() is not enabled and no route data is passed.
    this.userType = this.router.url.includes('/frontoffice/freelancer') ? 'freelancer' : 'client';
    const id = this.route.snapshot.paramMap.get('id')!;
    this.loadComplaint(id);
    this.watchNotifications(id);
  }

  loadComplaint(id: string): void {
    this.complaintService.getComplaintById(id).subscribe({
      next: c  => {
        this.complaint.set(c);
        this.isReportedParty.set(c.reportedUserId === this.currentUserId);
        this.isLoading.set(false);
      },
      error: () => {
        this.snackBar.open('Complaint not found', 'Close', { duration: 3000 });
        this.goBack();
      }
    });
  }

  private watchNotifications(complaintId: string): void {
    this.notifSub = this.notificationService.newNotification$.pipe(
      filter(n =>
        n.referenceId === complaintId &&
        n.referenceType === 'COMPLAINT' &&
        ['COMPLAINT_STATUS', 'COMPLAINT_RESOLVED', 'COMPLAINT_CLOSED'].includes(n.type)
      )
    ).subscribe(() => {
      this.loadComplaint(complaintId);
      this.snackBar.open('Your complaint status has been updated.', 'View', { duration: 4000 });
    });
  }

  ngOnDestroy(): void {
    this.notifSub?.unsubscribe();
  }

  isWaitingTooLong(): boolean {
    const c = this.complaint();
    if (!c || c.firstResponseAt) return false;
    const hoursSinceCreation = (Date.now() - new Date(c.createdAt).getTime()) / (1000 * 60 * 60);
    return hoursSinceCreation > 48;
  }

  // ── Notation ─────────────────────────────────────────────────

  submitRating(): void {
    const rating = this.selectedRating();
    if (!rating || !this.complaint()) return;

    this.isRating.set(true);
    this.complaintService.rateComplaint(this.complaint()!.id, rating).subscribe({
      next: updated => {
        this.complaint.set(updated);
        this.isRating.set(false);
        this.snackBar.open('Thank you for your rating!', 'Close', {
          duration: 3000, panelClass: ['snack-success']
        });
      },
      error: () => {
        this.snackBar.open('Error submitting rating', 'Close', { duration: 3000 });
        this.isRating.set(false);
      }
    });
  }

  // ── Navigation ────────────────────────────────────────────────

  goBack(): void {
    const base = this.userType === 'freelancer'
      ? '/frontoffice/freelancer/my-complaints'
      : '/frontoffice/client/my-complaints';
    this.router.navigate([base]);
  }

  // ── Reopen ────────────────────────────────────────────────────

  canReopen(): boolean {
    const c = this.complaint();
    // Only the original reporter can contest the resolution
    if (!c || this.isReportedParty()) return false;
    const isResolved = c.status === ComplaintStatus.RESOLVED || c.status === ComplaintStatus.CLOSED;
    if (!isResolved) return false;
    const resolvedAt = c.resolvedAt ? new Date(c.resolvedAt).getTime() : 0;
    const daysSince = (Date.now() - resolvedAt) / (1000 * 60 * 60 * 24);
    return daysSince <= 7;
  }

  reopen(): void {
    if (!this.reopenReason().trim() || !this.complaint()) return;
    this.isReopening.set(true);
    this.advService.reopenComplaint(this.complaint()!.id, { reason: this.reopenReason() }).subscribe({
      next: updated => {
        this.complaint.set(updated);
        this.showReopenForm.set(false);
        this.reopenReason.set('');
        this.isReopening.set(false);
        this.snackBar.open('Complaint reopened.', 'Close', { duration: 3000, panelClass: ['snack-success'] });
      },
      error: err => {
        this.snackBar.open(err?.error?.message ?? 'Unable to reopen.', 'Close', { duration: 4000 });
        this.isReopening.set(false);
      }
    });
  }

  // ── Mediation ─────────────────────────────────────────────────

  hasMediation(): boolean {
    const c = this.complaint();
    return !!c && (c.status === 'ESCALATED' as any || this.showMediation());
  }

  // ── Guards affichage ─────────────────────────────────────────

  canRate(): boolean {
    const c = this.complaint();
    // Only the original reporter can rate — the reported party has no rating to give
    return !!c && !this.isReportedParty() && c.status === ComplaintStatus.CLOSED && !c.satisfactionRating;
  }

  showNps(): boolean {
    const c = this.complaint();
    // NPS survey is addressed to the reporter only
    return !!c && !this.isReportedParty() && (c.status === ComplaintStatus.CLOSED || c.status === ComplaintStatus.RESOLVED);
  }

  get attachments(): string[] {
    return this.complaint()?.attachments ?? [];
  }

  // ── Labels ───────────────────────────────────────────────────

  getStatusLabel   = (s: ComplaintStatus) => (STATUS_LABELS   as Record<string, string>)[s] || s;
  getStatusColor   = (s: ComplaintStatus) => (STATUS_COLORS   as Record<string, string>)[s] || '#999';
  getPriorityLabel = (p: any)             => (PRIORITY_LABELS as Record<string, string>)[p] || p;
  getPriorityColor = (p: any)             => (PRIORITY_COLORS as Record<string, string>)[p] || '#999';
  getCategoryLabel = (c: any)             => (CATEGORY_LABELS as Record<string, string>)[c] || c;

  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('en-US', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  formatShortDate(date: Date): string {
    return new Date(date).toLocaleDateString('en-US', {
      day: '2-digit', month: 'short',
      hour: '2-digit', minute: '2-digit'
    });
  }
}