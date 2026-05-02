import { Component, OnInit, signal, computed, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule } from '@ngx-translate/core';
import { ComplaintService } from '@core/services/complaint.service';
import {
  Complaint, ComplaintStatus, ComplaintPriority, ComplaintCategory,
  STATUS_LABELS, STATUS_COLORS, PRIORITY_LABELS, PRIORITY_COLORS, CATEGORY_LABELS
} from '@core/models/complaint.model';

export type DateFilter = 'all' | 'today' | 'week' | 'month';
export type SortField  = 'date_desc' | 'date_asc' | 'priority_desc';

@Component({
  selector: 'app-my-complaints',
  standalone: true,
  imports: [
    CommonModule, RouterModule, FormsModule,
    MatIconModule, MatSnackBarModule, TranslateModule
  ],
  templateUrl: './my-complaints.component.html',
  styleUrls: ['./my-complaints.component.scss']
})
export class MyComplaintsComponent implements OnInit {

  @Input() userType: 'client' | 'freelancer' = 'client';

  complaints         = signal<Complaint[]>([]);
  involvedComplaints = signal<Complaint[]>([]);
  activeTab             = signal<'mine' | 'involved'>('mine');
  isLoading             = signal(true);
  isLoadingInvolved     = signal(false);

  // ── Filtres ──────────────────────────────────────────────────
  filterStatus   = signal<string>('all');
  filterPriority = signal<string>('all');
  filterDate     = signal<DateFilter>('all');
  sortBy         = signal<SortField>('date_desc');
  searchQuery    = signal<string>('');

  // ── Dropdown open state ──────────────────────────────────────
  sortDropOpen     = false;
  priorityDropOpen = false;
  dateDropOpen     = false;

  // ── Données statiques filtres ────────────────────────────────
  statusFilters = [
    { value: 'all',                          label: 'All statuses' },
    { value: ComplaintStatus.OPEN,           label: STATUS_LABELS[ComplaintStatus.OPEN] },
    { value: ComplaintStatus.IN_PROGRESS,    label: STATUS_LABELS[ComplaintStatus.IN_PROGRESS] },
    { value: ComplaintStatus.PENDING_USER,   label: STATUS_LABELS[ComplaintStatus.PENDING_USER] },
    { value: ComplaintStatus.RESOLVED,       label: STATUS_LABELS[ComplaintStatus.RESOLVED] },
    { value: ComplaintStatus.CLOSED,         label: STATUS_LABELS[ComplaintStatus.CLOSED] }
  ];

  priorityFilters = [
    { value: 'all',                        label: 'All priorities' },
    { value: ComplaintPriority.CRITICAL,   label: PRIORITY_LABELS[ComplaintPriority.CRITICAL] },
    { value: ComplaintPriority.HIGH,       label: PRIORITY_LABELS[ComplaintPriority.HIGH] },
    { value: ComplaintPriority.MEDIUM,     label: PRIORITY_LABELS[ComplaintPriority.MEDIUM] },
    { value: ComplaintPriority.LOW,        label: PRIORITY_LABELS[ComplaintPriority.LOW] }
  ];

  dateFilters: { value: DateFilter; label: string }[] = [
    { value: 'all',   label: 'All dates' },
    { value: 'today', label: 'Today' },
    { value: 'week',  label: 'Last 7 days' },
    { value: 'month', label: 'Last 30 days' }
  ];

  sortOptions: { value: SortField; label: string }[] = [
    { value: 'date_desc',     label: 'Most recent' },
    { value: 'date_asc',      label: 'Oldest first' },
    { value: 'priority_desc', label: 'Priority (high to low)' }
  ];

  // ── Computed : liste filtrée + triée ─────────────────────────
  filtered = computed(() => {
    let list = [...this.complaints()];

    // 1. Recherche textuelle
    const q = this.searchQuery().trim().toLowerCase();
    if (q) {
      list = list.filter(c =>
        c.subject?.toLowerCase().includes(q) ||
        c.description?.toLowerCase().includes(q) ||
        c.ticketNumber?.toLowerCase().includes(q)
      );
    }

    // 2. Filtre statut
    const status = this.filterStatus();
    if (status !== 'all') list = list.filter(c => c.status === status);

    // 3. Filtre priorité
    const priority = this.filterPriority();
    if (priority !== 'all') list = list.filter(c => c.priority === priority);

    // 4. Filtre date
    const dateF = this.filterDate();
    if (dateF !== 'all') {
      const now = new Date();
      const days = dateF === 'today' ? 0 : dateF === 'week' ? 7 : 30;
      const cutoff = new Date(now);
      if (days === 0) cutoff.setHours(0, 0, 0, 0);
      else { cutoff.setDate(now.getDate() - days); cutoff.setHours(0, 0, 0, 0); }
      list = list.filter(c => new Date(c.createdAt) >= cutoff);
    }

    // 5. Tri
    const sort = this.sortBy();
    const priorityOrder: Record<string, number> = { CRITICAL: 4, HIGH: 3, MEDIUM: 2, LOW: 1 };
    list.sort((a, b) => {
      if (sort === 'date_desc')     return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
      if (sort === 'date_asc')      return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
      if (sort === 'priority_desc') return (priorityOrder[b.priority] ?? 0) - (priorityOrder[a.priority] ?? 0);
      return 0;
    });

    return list;
  });

  countByStatus = computed(() => {
    const counts: Record<string, number> = {
      all: this.complaints().length,
      OPEN: 0, IN_PROGRESS: 0, PENDING_USER: 0, RESOLVED: 0, CLOSED: 0, ESCALATED: 0
    };
    this.complaints().forEach(c => { counts[c.status] = (counts[c.status] ?? 0) + 1; });
    return counts;
  });

  hasActiveFilters = computed(() =>
    this.filterStatus() !== 'all' ||
    this.filterPriority() !== 'all' ||
    this.filterDate() !== 'all' ||
    !!this.searchQuery()
  );

  constructor(
    private complaintService: ComplaintService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    // Detect context from URL — @Input() userType is never bound by the router
    this.userType = this.router.url.includes('/frontoffice/freelancer') ? 'freelancer' : 'client';
    this.load();
    this.loadInvolved();
  }

  load(): void {
    this.isLoading.set(true);
    this.complaintService.getMyComplaints().subscribe({
      next: data => { this.complaints.set(data); this.isLoading.set(false); },
      error: () => {
        this.snackBar.open('Error loading complaints', 'Close', { duration: 3000 });
        this.isLoading.set(false);
      }
    });
  }

  loadInvolved(): void {
    if (this.isLoadingInvolved()) return;
    this.isLoadingInvolved.set(true);
    this.complaintService.getInvolvedComplaints().subscribe({
      next: data => { this.involvedComplaints.set(data); this.isLoadingInvolved.set(false); },
      error: () => {
        this.snackBar.open('Failed to load involved complaints', 'OK', { duration: 3000 });
        this.isLoadingInvolved.set(false);
      }
    });
  }

  resetFilters(): void {
    this.filterStatus.set('all');
    this.filterPriority.set('all');
    this.filterDate.set('all');
    this.sortBy.set('date_desc');
    this.searchQuery.set('');
    this.sortDropOpen = false;
    this.priorityDropOpen = false;
    this.dateDropOpen = false;
  }

  viewDetail(id: string): void {
    const base = this.userType === 'freelancer'
      ? '/frontoffice/freelancer/my-complaints'
      : '/frontoffice/client/my-complaints';
    this.router.navigate([base, id]);
  }

  createNew(): void {
    const base = this.userType === 'freelancer'
      ? '/frontoffice/freelancer/my-complaints/new'
      : '/frontoffice/client/my-complaints/new';
    this.router.navigate([base]);
  }

  // ── Helpers affichage ─────────────────────────────────────────

  getStatusLabel(status: string): string {
    return (STATUS_LABELS as Record<string, string>)[status] || status;
  }

  getStatusColor(status: string): string {
    return (STATUS_COLORS as Record<string, string>)[status] || '#999';
  }

  getPriorityLabel(priority: any): string {
    return (PRIORITY_LABELS as Record<string, string>)[priority] || priority;
  }

  getPriorityColor(priority: any): string {
    return (PRIORITY_COLORS as Record<string, string>)[priority] || '#999';
  }

  getCategoryLabel(category: ComplaintCategory): string {
    return CATEGORY_LABELS[category] || category;
  }

  getCategoryIcon(category: ComplaintCategory): string {
    const icons: Record<string, string> = {
      PAYMENT_ISSUE:          'payments',
      QUALITY_DISPUTE:        'star_half',
      COMMUNICATION_PROBLEM:  'chat_bubble_outline',
      HARASSMENT:             'report',
      SCAM:                   'warning',
      TECHNICAL_ISSUE:        'build',
      OTHER:                  'help_outline'
    };
    return icons[category] || 'help_outline';
  }

  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('en-US', {
      day: '2-digit', month: 'short', year: 'numeric'
    });
  }

  canRate(complaint: Complaint): boolean {
    return complaint.status === ComplaintStatus.CLOSED && !complaint.satisfactionRating;
  }

  get activeSortLabel(): string {
    return this.sortOptions.find(o => o.value === this.sortBy())?.label ?? 'Sort by';
  }

  get activePriorityLabel(): string {
    return this.priorityFilters.find(o => o.value === this.filterPriority())?.label ?? 'Priority';
  }

  get activeDateLabel(): string {
    return this.dateFilters.find(o => o.value === this.filterDate())?.label ?? 'Period';
  }
}
