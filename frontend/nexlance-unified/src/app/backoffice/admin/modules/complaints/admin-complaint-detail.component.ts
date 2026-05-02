import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { forkJoin } from 'rxjs';
import { ComplaintService } from '@core/services/complaint.service';
import { ComplaintPdfService } from '@core/services/complaint-pdf.service';
import { UserService } from '@core/services/user.service';
import {
  Complaint, SupportMessage, ComplaintStatus, ComplaintPriority,
  MessageType, ResolutionType, ConversationType,
  STATUS_LABELS, STATUS_COLORS, PRIORITY_LABELS, PRIORITY_COLORS, CATEGORY_LABELS
} from '@core/models/complaint.model';

// ─── IMPORT CLEF ─────────────────────────────────────────────────────────────
// ComplaintConversationComponent affiche automatiquement DEUX onglets pour
// l'admin : fil Plaignant ↔ Support + fil Partie mise en cause ↔ Support.
// Il expose aussi le bouton "Impliquer la partie" si celle-ci n'est pas
// encore impliquée.
//
// On conserve FormsModule ici car l'admin a d'autres formulaires (resolve,
// assign, status…). On supprime uniquement : ViewChild messagesEnd,
// signal messages, newMessage, isSending, loadMessages(), sendMessage(),
// scrollBottom() et isNoteInterne() — tout cela est géré par le composant.
// ─────────────────────────────────────────────────────────────────────────────
import { ComplaintConversationComponent } from '../../../../shared/components/conversation/complaint-conversation.component';
import { ComplaintTimelineComponent } from '../../../../shared/components/complaint-timeline/complaint-timeline.component';
import { SlaTrackerComponent } from '../../../../shared/components/sla-tracker/sla-tracker.component';

@Component({
  selector: 'app-admin-complaint-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatIconModule,
    MatSnackBarModule,
    ComplaintConversationComponent,
    ComplaintTimelineComponent,
    SlaTrackerComponent,
  ],
  templateUrl: './admin-complaint-detail.component.html',
  styleUrls: ['./admin-complaint-detail.component.scss']
})
export class AdminComplaintDetailComponent implements OnInit {

  complaint    = signal<Complaint | null>(null);
  agents       = signal<any[]>([]);
  messages     = signal<SupportMessage[]>([]);
  reporterInfo  = signal<any>(null);
  reportedInfo  = signal<any>(null);
  assignedInfo  = signal<any>(null);
  isLoading    = signal(true);
  isDeleting   = signal(false);
  isExporting  = signal(false);
  isActing     = signal(false);

  showStatusPanel   = signal(false);
  showPriorityPanel = signal(false);
  showAssignModal   = signal(false);
  showDeleteConfirm = signal(false);
  showCloseConfirm  = signal(false);

  statuses   = Object.values(ComplaintStatus);
  priorities = Object.values(ComplaintPriority);

  // CLOSED → bouton Clôturer dédié | OPEN → irréversible
  // RESOLVED → bouton Résoudre (agent) | ESCALATED → via réassignation
  readonly changeableStatuses = Object.values(ComplaintStatus).filter(s =>
    s !== ComplaintStatus.CLOSED    &&
    s !== ComplaintStatus.OPEN      &&
    s !== ComplaintStatus.RESOLVED  &&
    s !== ComplaintStatus.ESCALATED
  );

  ComplaintStatus = ComplaintStatus;

  constructor(
    private route:               ActivatedRoute,
    private router:              Router,
    private complaintService:    ComplaintService,
    private pdfService:          ComplaintPdfService,
    private userService:         UserService,
    private snackBar:            MatSnackBar,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.loadComplaint(id);
    this.loadAgents();
  }

  loadComplaint(id: string): void {
    this.complaintService.getComplaintById(id).subscribe({
      next: c  => {
        this.complaint.set(c);
        this.isLoading.set(false);
        this.loadMessages(id);
        this.loadUserInfos(c);
      },
      error: () => {
        this.snackBar.open('Complaint not found', 'Close', { duration: 3000 });
        this.goBack();
      }
    });
  }

  private loadMessages(complaintId: string): void {
    forkJoin({
      complainant: this.complaintService.getMessages(complaintId, ConversationType.COMPLAINANT),
      reported:    this.complaintService.getMessages(complaintId, ConversationType.REPORTED)
    }).subscribe({
      next: ({ complainant, reported }) => {
        this.messages.set([...complainant, ...reported]);
      },
      error: () => this.snackBar.open('Failed to load messages', 'OK', { duration: 3000 })
    });
  }

  private isValidId(id: string | undefined | null): id is string {
    return !!id && id !== 'undefined' && id !== 'null';
  }

  private loadUserInfos(c: Complaint): void {
    if (this.isValidId(c.reporterId)) {
      this.userService.getUserById(c.reporterId).subscribe({
        next: u => this.reporterInfo.set(u), error: () => {}
      });
    }
    if (this.isValidId(c.reportedUserId)) {
      this.userService.getUserById(c.reportedUserId).subscribe({
        next: u => this.reportedInfo.set(u), error: () => {}
      });
    }
    if (this.isValidId(c.assignedToId)) {
      this.userService.getUserById(c.assignedToId).subscribe({
        next: u => this.assignedInfo.set(u), error: () => {}
      });
    }
  }

  userName(info: any): string {
    if (!info) return '—';
    return `${info.firstName ?? ''} ${info.lastName ?? ''}`.trim() || info.email || '—';
  }

  loadAgents(): void {
    this.complaintService.getAgents().subscribe({
      next: list => this.agents.set(list),
      error: () => this.snackBar.open('Failed to load agents list', 'OK', { duration: 3000 })
    });
  }

  // ── Export PDF ────────────────────────────────────────────────

  exportPdf(): void {
    const c = this.complaint();
    if (!c) return;
    this.isExporting.set(true);
    try {
      const reporter = this.reporterInfo();
      const reported = this.reportedInfo();
      this.pdfService.generate({
        complaint:     c,
        messages:      this.messages(),
        agents:        this.agents(),
        reporterName:  reporter ? `${reporter.firstName} ${reporter.lastName}` : undefined,
        reporterEmail: reporter?.email,
        reportedName:  reported ? `${reported.firstName} ${reported.lastName}` : undefined,
        reportedEmail: reported?.email
      });
      this.snackBar.open('✅ PDF exported successfully!', 'Close', {
        duration: 3000, panelClass: ['snack-success']
      });
    } catch (err) {
      console.error('Error exporting PDF', err);
      this.snackBar.open('Error generating PDF', 'Close', { duration: 4000 });
    } finally {
      this.isExporting.set(false);
    }
  }

  // ── Actions admin ─────────────────────────────────────────────

  changeStatus(status: ComplaintStatus): void {
    if (this.isActing()) return;
    this.isActing.set(true);
    this.complaintService.updateStatus(this.complaint()!.id, status).subscribe({
      next: c => {
        this.complaint.set(c);
        this.showStatusPanel.set(false);
        this.snackBar.open('Status updated', 'Close', { duration: 2000, panelClass: ['snack-success'] });
        this.isActing.set(false);
      },
      error: () => {
        this.snackBar.open('Error', 'Close', { duration: 3000 });
        this.isActing.set(false);
      }
    });
  }

  changePriority(priority: ComplaintPriority): void {
    if (this.isActing()) return;
    this.isActing.set(true);
    this.complaintService.updatePriority(this.complaint()!.id, priority).subscribe({
      next: c => {
        this.complaint.set(c);
        this.showPriorityPanel.set(false);
        this.snackBar.open('Priority updated', 'Close', { duration: 2000, panelClass: ['snack-success'] });
        this.isActing.set(false);
      },
      error: () => {
        this.snackBar.open('Error', 'Close', { duration: 3000 });
        this.isActing.set(false);
      }
    });
  }

  closeComplaint(): void {
    if (this.isActing()) return;
    this.isActing.set(true);
    this.complaintService.closeComplaint(this.complaint()!.id).subscribe({
      next: c => {
        this.complaint.set(c);
        this.showCloseConfirm.set(false);
        this.snackBar.open('Complaint closed', 'Close', { duration: 3000, panelClass: ['snack-success'] });
        this.isActing.set(false);
      },
      error: () => {
        this.snackBar.open('Error closing complaint', 'Close', { duration: 3000 });
        this.isActing.set(false);
      }
    });
  }

  deleteComplaint(): void {
    this.isDeleting.set(true);
    this.complaintService.deleteComplaint(this.complaint()!.id).subscribe({
      next: () => {
        this.snackBar.open('Complaint deleted', 'Close', { duration: 3000 });
        this.goBack();
      },
      error: () => {
        this.snackBar.open('Error deleting complaint', 'Close', { duration: 3000 });
        this.isDeleting.set(false);
      }
    });
  }

  assignTo(agentId: string): void {
    if (this.isActing()) return;
    this.isActing.set(true);
    this.complaintService.assignComplaint(this.complaint()!.id, agentId).subscribe({
      next: c => {
        this.complaint.set(c);
        this.loadUserInfos(c);
        this.showAssignModal.set(false);
        this.snackBar.open('Complaint reassigned', 'Close', { duration: 2000, panelClass: ['snack-success'] });
        this.isActing.set(false);
      },
      error: () => {
        this.snackBar.open('Error reassigning complaint', 'Close', { duration: 3000 });
        this.isActing.set(false);
      }
    });
  }

  // ── Helpers ───────────────────────────────────────────────────

  goBack(): void { this.router.navigate(['/backoffice/admin/complaints']); }
  canClose(): boolean { return this.complaint()?.status === ComplaintStatus.RESOLVED; }

  getAgentName(id: string): string {
    const a = this.agents().find(ag => ag.id === id);
    return a ? `${a.firstName} ${a.lastName}` : id.substring(0, 8) + '…';
  }

  getStatusLabel   = (s: any) => (STATUS_LABELS   as Record<string, string>)[s] || s;
  getStatusColor   = (s: any) => (STATUS_COLORS   as Record<string, string>)[s] || '#999';
  getPriorityLabel = (p: any) => (PRIORITY_LABELS as Record<string, string>)[p] || p;
  getPriorityColor = (p: any) => (PRIORITY_COLORS as Record<string, string>)[p] || '#999';
  getCategoryLabel = (c: any) => (CATEGORY_LABELS as Record<string, string>)[c] || c;

  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('en-GB', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }
}