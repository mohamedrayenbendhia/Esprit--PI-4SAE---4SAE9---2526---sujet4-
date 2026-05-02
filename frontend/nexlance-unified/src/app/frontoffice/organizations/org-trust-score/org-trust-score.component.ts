import { Component, Input, OnInit, inject, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule, DecimalPipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { OrganizationService } from '../../../core/services/organization.service';
import { TrustScore } from '../../../core/models/organization.model';

@Component({
  selector: 'app-org-trust-score',
  standalone: true,
  imports: [CommonModule, DecimalPipe, MatIconModule, MatProgressSpinnerModule, MatTooltipModule],
  templateUrl: './org-trust-score.component.html',
  styleUrls: ['./org-trust-score.component.scss']
})
export class OrgTrustScoreComponent implements OnInit {

  @Input({ required: true }) orgId!: string;

  private orgService = inject(OrganizationService);
  private destroyRef = inject(DestroyRef);

  score     = signal<TrustScore | null>(null);
  isLoading = signal(true);
  error     = signal<string | null>(null);
  expanded  = signal(false);

  ngOnInit() {
    this.orgService.getTrustScore(this.orgId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ts => { this.score.set(ts); this.isLoading.set(false); },
        error: ()  => { this.error.set('Score unavailable'); this.isLoading.set(false); }
      });
  }

  toggleExpanded() { this.expanded.set(!this.expanded()); }

  /** Largeur de la barre de progression pour un signal donné (max = son poids). */
  barWidth(contribution: number, maxWeight: number): number {
    if (maxWeight === 0) return 0;
    return Math.min((contribution / maxWeight) * 100, 100);
  }

  levelLabel(level: number): string {
    return ['', 'Beginner', 'Developing', 'Reliable', 'Established', 'Expert'][level] ?? '';
  }

  levelClass(level: number): string {
    return ['', 'level-1', 'level-2', 'level-3', 'level-4', 'level-5'][level] ?? '';
  }

  scoreColor(score: number): string {
    if (score >= 80) return '#4caf50';
    if (score >= 60) return '#8bc34a';
    if (score >= 40) return '#ff9800';
    if (score >= 20) return '#ff5722';
    return '#9e9e9e';
  }
}
