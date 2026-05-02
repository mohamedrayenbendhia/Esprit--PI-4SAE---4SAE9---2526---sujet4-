import { Component, signal, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { ComplaintAdvancedService } from '../../../../core/services/complaint-advanced.service';
import {
  UserRiskProfile, UserSanction, SanctionType
} from '../../../../core/models/complaint-advanced.model';

@Component({
  selector: 'app-admin-risk-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './admin-risk-dashboard.component.html',
  styleUrls: ['./admin-risk-dashboard.component.scss']
})
export class AdminRiskDashboardComponent implements OnInit {
  private advService = inject(ComplaintAdvancedService);

  activeTab = signal<'risks' | 'manage'>('risks');

  // High-risk list
  highRisk    = signal<UserRiskProfile[]>([]);
  isLoading   = signal(true);

  // User lookup
  lookupId      = signal('');
  lookedUp      = signal<UserRiskProfile | null>(null);
  sanctions     = signal<UserSanction[]>([]);
  lookupLoading = signal(false);

  // Apply sanction form
  sanctionType   = signal<SanctionType>('WARNING');
  sanctionReason = signal('');
  applying       = signal(false);
  applySuccess   = signal(false);
  applyError     = signal<string | null>(null);

  readonly sanctionTypes: SanctionType[] = ['WARNING', 'TEMPORARY_SUSPENSION', 'PERMANENT_SUSPENSION'];

  ngOnInit() {
    this.advService.getHighRiskUsers().subscribe({
      next:  list => { this.highRisk.set(list); this.isLoading.set(false); },
      error: ()   => this.isLoading.set(false)
    });
  }

  lookup() {
    if (!this.lookupId().trim()) return;
    this.lookupLoading.set(true);
    this.lookedUp.set(null);
    this.sanctions.set([]);

    this.advService.getUserRiskProfile(this.lookupId()).subscribe({
      next: p => {
        this.lookedUp.set(p);
        this.lookupLoading.set(false);
        this.loadSanctions(p.userId);
      },
      error: () => this.lookupLoading.set(false)
    });
  }

  loadSanctions(userId: string) {
    this.advService.getUserSanctions(userId).subscribe({
      next: list => this.sanctions.set(list)
    });
  }

  selectUser(profile: UserRiskProfile) {
    this.lookupId.set(profile.userId);
    this.lookedUp.set(profile);
    this.loadSanctions(profile.userId);
  }

  recompute(userId: string) {
    this.advService.recomputeRisk(userId).subscribe({
      next: updated => {
        this.highRisk.update(list => list.map(r => r.userId === userId ? updated : r));
        if (this.lookedUp()?.userId === userId) this.lookedUp.set(updated);
      }
    });
  }

  applyManualSanction() {
    const u = this.lookedUp();
    if (!u || !this.sanctionReason().trim()) {
      this.applyError.set('A reason is required.'); return;
    }
    this.applying.set(true);
    this.applyError.set(null);
    this.advService.applySanction(u.userId, {
      type: this.sanctionType(),
      reason: this.sanctionReason()
    }).subscribe({
      next: s => {
        this.sanctions.update(list => [s, ...list]);
        this.applySuccess.set(true);
        this.sanctionReason.set('');
        this.applying.set(false);
        setTimeout(() => this.applySuccess.set(false), 3000);
      },
      error: err => {
        this.applyError.set(err?.error?.message ?? 'An error occurred.');
        this.applying.set(false);
      }
    });
  }

  lift(sanctionId: string) {
    if (!confirm('Lift this sanction?')) return;
    this.advService.liftSanction(sanctionId).subscribe({
      next: updated => this.sanctions.update(list => list.map(s => s.id === sanctionId ? updated : s))
    });
  }

  // ── Colors ────────────────────────────────────────────────────────────────
  riskColor(level: string): string {
    return ({ LOW: '#16a34a', MEDIUM: '#d97706', HIGH: '#dc2626', CRITICAL: '#7c3aed' } as Record<string, string>)[level] ?? '#64748b';
  }

  riskBg(level: string): string {
    return ({ LOW: '#f0fdf4', MEDIUM: '#fffbeb', HIGH: '#fef2f2', CRITICAL: '#f5f3ff' } as Record<string, string>)[level] ?? '#f8fafc';
  }

  sanctionColor(type: string): string {
    return ({ WARNING: '#d97706', TEMPORARY_SUSPENSION: '#dc2626', PERMANENT_SUSPENSION: '#7c3aed' } as Record<string, string>)[type] ?? '#64748b';
  }

  sanctionBg(type: string): string {
    return ({ WARNING: '#fffbeb', TEMPORARY_SUSPENSION: '#fef2f2', PERMANENT_SUSPENSION: '#f5f3ff' } as Record<string, string>)[type] ?? '#f8fafc';
  }
}
