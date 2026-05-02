import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';
import { AuthService } from '@core/services/auth.service';
import { ComplaintService } from '@core/services/complaint.service';
import { ComplaintPdfService } from '@core/services/complaint-pdf.service';
import { UserService } from '@core/services/user.service';
import {
  Complaint, SupportMessage, ComplaintStatus, ComplaintPriority,
  ResolutionType, MessageType, ConversationType,
  STATUS_LABELS, STATUS_COLORS, PRIORITY_LABELS, PRIORITY_COLORS, CATEGORY_LABELS
} from '@core/models/complaint.model';

// ─── IMPORT CLEF ─────────────────────────────────────────────────────────────
// ComplaintConversationComponent affiche automatiquement DEUX onglets pour
// l'agent : fil Plaignant ↔ Support + fil Partie mise en cause ↔ Support.
// L'agent peut aussi accéder au bouton "Impliquer la partie" depuis l'onglet
// "Partie mise en cause" si elle n'est pas encore impliquée.
//
// On conserve FormsModule car l'agent a encore ses formulaires (résolution,
// changement de statut/priorité, réassignation).
// On supprime : ViewChild messagesEnd, signal messages, newMessage,
// noteContent, isSending, loadMessages(), sendMessage(), sendNote(),
// scrollToBottom() et isNoteInterne() — tout géré par le composant.
// ─────────────────────────────────────────────────────────────────────────────
import { ComplaintConversationComponent } from '../../shared/components/conversation/complaint-conversation.component';
import { ComplaintTimelineComponent } from '../../shared/components/complaint-timeline/complaint-timeline.component';
import { SlaTrackerComponent } from '../../shared/components/sla-tracker/sla-tracker.component';

@Component({
  selector: 'app-agent-complaint-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatIconModule,
    MatSnackBarModule,
    TranslateModule,
    ComplaintConversationComponent,
    ComplaintTimelineComponent,
    SlaTrackerComponent,
  ],
  templateUrl: './agent-complaint-detail.component.html',
  styleUrls: ['./agent-complaint-detail.component.scss']
})
export class AgentComplaintDetailComponent implements OnInit {

  complaint        = signal<Complaint | null>(null);
  agents           = signal<any[]>([]);
  messages         = signal<SupportMessage[]>([]);
  reporterInfo     = signal<any>(null);
  reportedInfo     = signal<any>(null);
  assignedInfo     = signal<any>(null);
  isLoading        = signal(true);
  isExporting      = signal(false);
  isActing         = signal(false);
  showResolveModal = signal(false);
  showAssignModal  = signal(false);

  selectedAgentId = '';

  resolution: { text: string; type: ResolutionType } = {
    text: '', type: ResolutionType.NO_ACTION
  };

  currentUserId = '';

  ComplaintStatus   = ComplaintStatus;
  ComplaintPriority = ComplaintPriority;
  ResolutionType    = ResolutionType;

  priorityOptions = Object.values(ComplaintPriority).map(v => ({ value: v, label: PRIORITY_LABELS[v] }));
  resolutionTypes = [
    { value: ResolutionType.REFUND,             label: 'Refund' },
    { value: ResolutionType.WARNING,            label: 'Warning' },
    { value: ResolutionType.ACCOUNT_SUSPENSION, label: 'Account Suspension' },
    { value: ResolutionType.NO_ACTION,          label: 'No Action' },
    { value: ResolutionType.MEDIATION,          label: 'Mediation' }
  ];

  constructor(
    private route:            ActivatedRoute,
    private router:           Router,
    private complaintService: ComplaintService,
    private authService:      AuthService,
    private pdfService:       ComplaintPdfService,
    private userService:      UserService,
    private snackBar:         MatSnackBar
  ) {}

  ngOnInit(): void {
    this.currentUserId = this.authService.getCurrentUser()?.id || '';
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
      next: agents => this.agents.set(agents.filter((a: any) => a.id !== this.currentUserId)),
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

  // ── Actions agent ─────────────────────────────────────────────

  changeStatus(status: ComplaintStatus): void {
    if (!this.complaint()) return;
    if (this.isActing()) return;
    this.isActing.set(true);
    this.complaintService.updateStatus(this.complaint()!.id, status).subscribe({
      next: updated => {
        this.complaint.set(updated);
        this.snackBar.open('Status updated', 'Close', { duration: 2000 });
        this.isActing.set(false);
      },
      error: () => {
        this.snackBar.open('Error', 'Close', { duration: 3000 });
        this.isActing.set(false);
      }
    });
  }

  changePriority(priority: ComplaintPriority): void {
    if (!this.complaint()) return;
    if (this.isActing()) return;
    this.isActing.set(true);
    this.complaintService.updatePriority(this.complaint()!.id, priority).subscribe({
      next: updated => {
        this.complaint.set(updated);
        this.snackBar.open('Priority updated', 'Close', { duration: 2000 });
        this.isActing.set(false);
      },
      error: () => {
        this.snackBar.open('Error', 'Close', { duration: 3000 });
        this.isActing.set(false);
      }
    });
  }

  confirmResolve(): void {
    if (!this.resolution.text.trim() || !this.complaint()) return;
    if (this.isActing()) return;
    this.isActing.set(true);
    this.complaintService.resolveComplaint(this.complaint()!.id, {
      resolution:     this.resolution.text,
      resolutionType: this.resolution.type
    }).subscribe({
      next: updated => {
        this.complaint.set(updated);
        this.showResolveModal.set(false);
        this.snackBar.open('Complaint resolved!', 'Close', {
          duration: 3000, panelClass: ['snack-success']
        });
        this.isActing.set(false);
      },
      error: () => {
        this.snackBar.open('Error resolving complaint', 'Close', { duration: 3000 });
        this.isActing.set(false);
      }
    });
  }

  confirmAssign(): void {
    if (!this.selectedAgentId || !this.complaint()) return;
    if (this.isActing()) return;
    this.isActing.set(true);
    this.complaintService.assignComplaint(this.complaint()!.id, this.selectedAgentId).subscribe({
      next: updated => {
        this.complaint.set(updated);
        this.loadUserInfos(updated);
        this.showAssignModal.set(false);
        this.snackBar.open('Complaint reassigned/escalated', 'Close', {
          duration: 3000, panelClass: ['snack-success']
        });
        this.isActing.set(false);
      },
      error: err => {
        const error      = err.error?.error      || 'Error';
        const detail     = err.error?.detail     || '';
        const suggestion = err.error?.suggestion || '';
        const fullMsg = [error, detail, suggestion].filter(Boolean).join(' ');
        this.snackBar.open(fullMsg, 'Close', { duration: 7000 });
        this.isActing.set(false);
      }
    });
  }

  // ── Guards affichage ─────────────────────────────────────────

  goBack    = () => this.router.navigate(['/backoffice/agent/queue']);
  canResolve = () => {
    const s = this.complaint()?.status;
    return s === ComplaintStatus.IN_PROGRESS || s === ComplaintStatus.PENDING_USER;
  };
  canReassign = () => this.complaint()?.priority === ComplaintPriority.CRITICAL;

  // Statuts modifiables par l'agent :
  // CLOSED → bouton Clôturer (admin only) | OPEN → irréversible
  // RESOLVED → bouton Résoudre dédié      | ESCALATED → bouton Réassigner
  readonly changeableStatuses = Object.values(ComplaintStatus)
    .filter(s =>
      s !== ComplaintStatus.CLOSED    &&
      s !== ComplaintStatus.OPEN      &&
      s !== ComplaintStatus.RESOLVED  &&
      s !== ComplaintStatus.ESCALATED
    )
    .map(v => ({ value: v, label: STATUS_LABELS[v] }));

  // ── Labels ───────────────────────────────────────────────────

  getStatusLabel   = (s: ComplaintStatus) => (STATUS_LABELS   as Record<string, string>)[s] || s;
  getStatusColor   = (s: ComplaintStatus) => (STATUS_COLORS   as Record<string, string>)[s] || '#999';
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