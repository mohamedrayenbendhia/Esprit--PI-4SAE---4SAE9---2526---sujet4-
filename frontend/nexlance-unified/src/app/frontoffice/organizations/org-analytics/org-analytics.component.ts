import { Component, Input, OnInit, signal, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { OrganizationService } from '../../../core/services/organization.service';
import { OrgAnalytics } from '../../../core/models/organization.model';

@Component({
  selector: 'app-org-analytics',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './org-analytics.component.html',
  styleUrls: ['./org-analytics.component.scss']
})
export class OrgAnalyticsComponent implements OnInit {
  @Input() orgId!: string;

  private orgService = inject(OrganizationService);
  private destroyRef = inject(DestroyRef);

  analytics = signal<OrgAnalytics | null>(null);
  isLoading = signal(true);
  error     = signal<string | null>(null);

  ngOnInit() {
    this.orgService.getOrgAnalytics(this.orgId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next:  data => { this.analytics.set(data); this.isLoading.set(false); },
        error: ()   => { this.error.set('Failed to load analytics.'); this.isLoading.set(false); }
      });
  }

  /** Largeur de barre pour la distribution des avis (max = 100%) */
  barWidth(count: number, total: number): number {
    return total > 0 ? Math.round((count / total) * 100) : 0;
  }

  /** Format d'un taux : "67.5%" ou "—" si null */
  rate(v: number | null): string {
    return v !== null ? v + ' %' : '—';
  }

  /** Étoiles unicode */
  stars(n: number): string {
    const c = Math.round(Math.max(0, Math.min(5, n)));
    return '★'.repeat(c) + '☆'.repeat(5 - c);
  }

  /** Classe CSS du trust level */
  trustClass(level: number): string {
    if (level >= 5) return 'trust-5';
    if (level >= 4) return 'trust-4';
    if (level >= 3) return 'trust-3';
    return 'trust-low';
  }
}
