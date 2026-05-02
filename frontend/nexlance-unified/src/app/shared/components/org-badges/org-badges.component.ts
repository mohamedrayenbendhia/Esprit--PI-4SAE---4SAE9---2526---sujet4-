import { Component, Input, OnInit, signal, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { OrganizationService } from '@core/services/organization.service';
import { OrgBadgeInfo, TrustBadge } from '@core/models/organization.model';

@Component({
  selector: 'app-org-badges',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatTooltipModule],
  template: `
    @if (info() && info()!.badges.length > 0) {
      <div class="badges-row">
        @for (b of info()!.badges; track b) {
          <span class="badge-chip" [matTooltip]="getLabel(b)">
            <mat-icon>{{ getIcon(b) }}</mat-icon>
            <span class="badge-label">{{ getLabel(b) }}</span>
          </span>
        }
      </div>
    }
  `,
  styles: [`
    .badges-row { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; margin-top: 4px; }
    .badge-chip {
      display: inline-flex; align-items: center; gap: 4px;
      padding: 3px 10px; background: #f3f4f6; border-radius: 14px;
      font-size: 0.75rem; font-weight: 600; cursor: default; color: #374151;
    }
    .badge-chip mat-icon { font-size: 15px; height: 15px; width: 15px; }
    .badge-label { display: none; }
    @media (min-width: 640px) { .badge-label { display: inline; } }
  `]
})
export class OrgBadgesComponent implements OnInit {
  @Input() orgId!: string;

  private orgService = inject(OrganizationService);
  private destroyRef = inject(DestroyRef);

  info = signal<OrgBadgeInfo | null>(null);

  ngOnInit() {
    /**
     * trustLevel supprimé : BadgeResponse backend retourne seulement organizationId + badges[].
     * trustLevel est une propriété de Organization, pas de BadgeResponse.
     */
    this.orgService.getBadges(this.orgId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: b => this.info.set(b),
        error: () => {} // fail silently — badges are non-critical
      });
  }

  getLabel(b: TrustBadge): string {
    const labels: Record<TrustBadge, string> = {
      [TrustBadge.VERIFIED]:       'Verified',
      [TrustBadge.TOP_RATED]:      'Top Rated',
      [TrustBadge.EXPERIENCED]:    'Experienced',
      [TrustBadge.FAST_RESPONDER]: 'Fast Responder',
      [TrustBadge.PREMIUM]:        'Premium'
    };
    return labels[b] ?? b;
  }

  getIcon(b: TrustBadge): string {
    const icons: Record<TrustBadge, string> = {
      [TrustBadge.VERIFIED]:       'verified_user',
      [TrustBadge.TOP_RATED]:      'star',
      [TrustBadge.EXPERIENCED]:    'workspace_premium',
      [TrustBadge.FAST_RESPONDER]: 'speed',
      [TrustBadge.PREMIUM]:        'diamond'
    };
    return icons[b] ?? 'badge';
  }
}
