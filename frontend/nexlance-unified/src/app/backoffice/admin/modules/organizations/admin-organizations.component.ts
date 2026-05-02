import { Component, signal, OnInit, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSelectModule } from '@angular/material/select';
import { OrganizationService } from '../../../../core/services/organization.service';
import {
  Organization, OrgDashboardStats, OrgAuditLog,
  AdminVerifyRequest, OrganizationStatus
} from '../../../../core/models/organization.model';

@Component({
  selector: 'app-admin-organizations',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatTabsModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatProgressSpinnerModule,
    MatDividerModule, MatChipsModule, MatTableModule, MatTooltipModule,
    MatSelectModule
  ],
  templateUrl: './admin-organizations.component.html',
  styleUrls: ['./admin-organizations.component.scss']
})
export class AdminOrganizationsComponent implements OnInit {
  private orgService = inject(OrganizationService);
  private router     = inject(Router);
  private destroyRef = inject(DestroyRef);

  // ── Onglet En attente ─────────────────────────────────────────────────────
  pending        = signal<Organization[]>([]);
  loadingPending = signal(true);
  pendingError   = signal<string | null>(null);

  verifyNotes   = signal<Record<string, string>>({});
  verifyLoading = signal<string | null>(null);

  suspendOrgId  = signal<string | null>(null);
  suspendReason = signal('');
  suspendLoading = signal(false);

  // ── Onglet Statistiques ───────────────────────────────────────────────────
  stats        = signal<OrgDashboardStats | null>(null);
  loadingStats = signal(false);
  statsError   = signal<string | null>(null);

  // ── Onglet Gestion (réactivation + badges) ───────────────────────────────
  manageStatus    = signal<OrganizationStatus>(OrganizationStatus.SUSPENDED);
  manageOrgs      = signal<Organization[]>([]);
  loadingManage   = signal(false);
  reactivatingId  = signal<string | null>(null);
  badgeOrgId      = signal<string | null>(null);
  badgeValue      = signal<string>('VERIFIED');
  badgeLoading    = signal(false);
  readonly statusOptions: OrganizationStatus[] = [
    OrganizationStatus.ACTIVE,
    OrganizationStatus.PENDING_VERIFICATION,
    OrganizationStatus.AWAITING_INFO,
    OrganizationStatus.SUSPENDED,
    OrganizationStatus.DISSOLVED,
    OrganizationStatus.REJECTED
  ];
  readonly badgeOptions = ['VERIFIED', 'TOP_RATED', 'EXPERIENCED', 'FAST_RESPONDER', 'PREMIUM'];

  // ── Onglet Audit ──────────────────────────────────────────────────────────
  auditOrgId   = signal('');
  auditLogs    = signal<OrgAuditLog[]>([]);
  loadingAudit = signal(false);
  /** FIXÉ : colonne 'createdAt' (était 'performedAt' — champ inexistant backend) */
  auditCols    = ['createdAt', 'action', 'performedByUserId', 'details'];

  ngOnInit() {
    this.loadPending();
  }

  // ── Pending ───────────────────────────────────────────────────────────────

  loadPending() {
    this.loadingPending.set(true);
    this.pendingError.set(null);
    this.orgService.getPendingVerification()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next:  list => { this.pending.set(Array.isArray(list) ? list : []); this.loadingPending.set(false); },
        error: ()   => {
          this.pendingError.set('Unable to load organizations pending verification.');
          this.loadingPending.set(false);
        }
      });
  }

  verify(orgId: string, decision: 'APPROVE' | 'REJECT' | 'AWAITING_INFO') {
    const note = this.verifyNotes()[orgId]?.trim() ?? '';
    if ((decision === 'REJECT' || decision === 'AWAITING_INFO') && !note) {
      alert('A note is required for this decision.');
      return;
    }
    /** FIXÉ : utilise adminNote (backend accepte adminNote ou note, on standardise sur adminNote) */
    const req: AdminVerifyRequest = { decision, adminNote: note || undefined };
    this.verifyLoading.set(orgId);
    this.orgService.verifyOrg(orgId, req)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.pending.update(list => list.filter(o => o.id !== orgId));
          this.verifyLoading.set(null);
        },
        error: () => this.verifyLoading.set(null)
      });
  }

  openSuspend(orgId: string) { this.suspendOrgId.set(orgId); this.suspendReason.set(''); }
  closeSuspend()             { this.suspendOrgId.set(null); }

  confirmSuspend() {
    const id = this.suspendOrgId();
    if (!id || !this.suspendReason().trim()) {
      alert('A suspension reason is required.');
      return;
    }
    this.suspendLoading.set(true);
    this.orgService.suspendOrg(id, { reason: this.suspendReason() })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.pending.update(list => list.filter(o => o.id !== id));
          this.suspendOrgId.set(null);
          this.suspendLoading.set(false);
        },
        error: () => this.suspendLoading.set(false)
      });
  }

  forceDissolve(orgId: string) {
    if (!confirm('Permanently dissolve this organization? This action is irreversible.')) return;
    this.orgService.forceDissolveOrg(orgId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.pending.update(list => list.filter(o => o.id !== orgId))
      });
  }

  // ── Stats ─────────────────────────────────────────────────────────────────

  loadStats() {
    if (this.stats()) return; // cache simple — déjà chargé
    this.loadingStats.set(true);
    this.statsError.set(null);
    this.orgService.getDashboardStats()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next:  s => { this.stats.set(s); this.loadingStats.set(false); },
        error: () => {
          this.statsError.set('Unable to load statistics (server error).');
          this.loadingStats.set(false);
        }
      });
  }

  retryStats() {
    this.stats.set(null);
    this.loadStats();
  }

  // ── Gestion (réactivation / badges) ──────────────────────────────────────

  loadManage() {
    this.loadingManage.set(true);
    this.orgService.getAllOrganizations(this.manageStatus())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: page => {
          this.manageOrgs.set(page?.content ?? []);
          this.loadingManage.set(false);
        },
        error: () => this.loadingManage.set(false)
      });
  }

  reactivate(orgId: string) {
    if (!confirm('Reactivate this organization?')) return;
    this.reactivatingId.set(orgId);
    this.orgService.reactivateOrg(orgId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => {
          // Retire de la liste courante si filtre ≠ ACTIVE
          if (this.manageStatus() !== OrganizationStatus.ACTIVE) {
            this.manageOrgs.update(list => list.filter(o => o.id !== orgId));
          } else {
            this.manageOrgs.update(list => list.map(o => o.id === orgId ? updated : o));
          }
          this.reactivatingId.set(null);
        },
        error: () => this.reactivatingId.set(null)
      });
  }

  openBadgeDialog(orgId: string) { this.badgeOrgId.set(orgId); this.badgeValue.set('VERIFIED'); }
  closeBadgeDialog()             { this.badgeOrgId.set(null); }

  assignBadge() {
    const id = this.badgeOrgId();
    if (!id) return;
    this.badgeLoading.set(true);
    this.orgService.assignBadge(id, this.badgeValue())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => { this.badgeLoading.set(false); this.closeBadgeDialog(); },
        error: () => this.badgeLoading.set(false)
      });
  }

  removeBadge() {
    const id = this.badgeOrgId();
    if (!id) return;
    this.badgeLoading.set(true);
    this.orgService.removeBadge(id, this.badgeValue())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => { this.badgeLoading.set(false); this.closeBadgeDialog(); },
        error: () => this.badgeLoading.set(false)
      });
  }

  // ── Audit ─────────────────────────────────────────────────────────────────

  loadAudit() {
    if (!this.auditOrgId().trim()) return;
    this.loadingAudit.set(true);
    this.orgService.getAuditLog(this.auditOrgId())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next:  page => { this.auditLogs.set(page?.content ?? []); this.loadingAudit.set(false); },
        error: ()   => this.loadingAudit.set(false)
      });
  }

  setNote(orgId: string, note: string) {
    this.verifyNotes.update(m => ({ ...m, [orgId]: note }));
  }

  viewPublicProfile(id: string) {
    this.router.navigate(['/frontoffice/organizations', id]);
  }

  statusClass(status: OrganizationStatus): string {
    const map: Record<string, string> = {
      ACTIVE: 'active', PENDING_VERIFICATION: 'pending', AWAITING_INFO: 'pending',
      SUSPENDED: 'suspended', DISSOLVED: 'dissolved', REJECTED: 'rejected'
    };
    return map[status] ?? 'other';
  }
}
