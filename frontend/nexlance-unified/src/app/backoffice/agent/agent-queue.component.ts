import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';
import { AuthService } from '@core/services/auth.service';
import { ComplaintService } from '@core/services/complaint.service';
import {
  Complaint, ComplaintStatus,
  STATUS_LABELS, STATUS_COLORS, PRIORITY_LABELS, PRIORITY_COLORS, CATEGORY_LABELS
} from '@core/models/complaint.model';

@Component({
  selector: 'app-agent-queue',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, MatIconModule, MatSnackBarModule, TranslateModule],
  templateUrl: './agent-queue.component.html',
  styleUrls: ['./agent-queue.component.scss']
})
export class AgentQueueComponent implements OnInit {
  queue           = signal<Complaint[]>([]);
  overdueList     = signal<Complaint[]>([]);
  isLoading       = signal(true);
  isLoadingOverdue = signal(false);
  takingId        = signal<string | null>(null);
  selectedTab     = signal<'unassigned' | 'mine' | 'overdue'>('unassigned');
  currentUserId   = '';

  // ── Jump-to-ticket ────────────────────────────────────────
  ticketInput     = '';
  isSearchingTicket = signal(false);

  readonly workflowHint = `Complaints are normally assigned by the administrator. You may take one on if it has not yet been assigned.`;

  unassigned = computed(() => this.queue().filter(c => !c.assignedToId));
  mine       = computed(() => this.queue().filter(c => c.assignedToId === this.currentUserId));
  displayed  = computed(() => {
    if (this.selectedTab() === 'unassigned') return this.unassigned();
    if (this.selectedTab() === 'overdue')    return this.overdueList();
    return this.mine();
  });

  constructor(
    private complaintService: ComplaintService,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.currentUserId = this.authService.getCurrentUser()?.id || '';
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    // Charge en parallèle : réclamations disponibles (non assignées) + celles déjà assignées à moi
    forkJoin({
      available: this.complaintService.getAgentAvailableQueue(),
      mine:      this.complaintService.getMyAssignedComplaints()
    }).subscribe({
      next: ({ available, mine }) => {
        // Merge en dédupliquant par id (au cas où une réclamation apparaîtrait dans les deux)
        const map = new Map<string, Complaint>();
        [...available, ...mine].forEach(c => map.set(c.id, c));
        this.queue.set(Array.from(map.values()));
        this.isLoading.set(false);
      },
      error: () => {
        this.snackBar.open('Loading error', 'Close', { duration: 3000 });
        this.isLoading.set(false);
      }
    });
  }

  loadOverdue(): void {
    this.isLoadingOverdue.set(true);
    this.complaintService.getOverdueComplaints(7).subscribe({
      next: data => { this.overdueList.set(data); this.isLoadingOverdue.set(false); },
      error: () => { this.snackBar.open('Error loading overdue complaints', 'Close', { duration: 3000 }); this.isLoadingOverdue.set(false); }
    });
  }

  jumpToTicket(): void {
    const ticket = this.ticketInput.trim().toUpperCase();
    if (!ticket) return;
    this.isSearchingTicket.set(true);
    this.complaintService.getComplaintByTicketNumber(ticket).subscribe({
      next: complaint => {
        this.isSearchingTicket.set(false);
        this.router.navigate(['/backoffice/agent/complaints', complaint.id]);
      },
      error: () => {
        this.isSearchingTicket.set(false);
        this.snackBar.open(`Ticket "${ticket}" not found`, 'Close', { duration: 4000 });
      }
    });
  }

  take(complaint: Complaint, event: Event): void {
    event.stopPropagation();

    if (!confirm(`Take on complaint "${complaint.subject}"?\nComplaints are normally assigned by the administrator.`)) {
      return;
    }

    this.takingId.set(complaint.id);
    this.complaintService.takeComplaint(complaint.id).subscribe({
      next: () => {
        this.snackBar.open('Complaint taken on board!', 'Close', { duration: 3000, panelClass: ['snack-success'] });
        this.load();
        this.takingId.set(null);
        this.selectedTab.set('mine');
      },
      error: err => {
        const msg = err.error?.error || err.error?.detail || 'Error';
        this.snackBar.open(msg, 'Close', { duration: 5000 });
        this.takingId.set(null);
      }
    });
  }

  viewDetail(id: string): void {
    this.router.navigate(['/backoffice/agent/complaints', id]);
  }

  getStatusLabel   = (s: ComplaintStatus) => (STATUS_LABELS   as Record<string, string>)[s] || s;
  getStatusColor   = (s: ComplaintStatus) => (STATUS_COLORS   as Record<string, string>)[s] || '#999';
  getPriorityLabel = (p: any) => (PRIORITY_LABELS as Record<string, string>)[p] || p;
  getPriorityColor = (p: any) => (PRIORITY_COLORS as Record<string, string>)[p] || '#999';
  getCategoryLabel = (c: any) => (CATEGORY_LABELS as Record<string, string>)[c] || c;

  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
  }
}