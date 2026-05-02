import {
  Component, Input, OnChanges, SimpleChanges,
  signal, computed, inject, DestroyRef
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { OrganizationService } from '../../../core/services/organization.service';
import { ProfileCompletionScore } from '../../../core/models/organization.model';

/**
 * Widget de complétude du profil d'organisation.
 *
 * Affiche :
 *  - Barre de progression circulaire (SVG) avec le score 0–100
 *  - Badge "Visible en recherche" ou "Profil incomplet"
 *  - Liste des critères cochés / manquants avec leurs points
 *
 * Usage :
 *   <app-org-completion-score [orgId]="orgId" />
 */
@Component({
  selector: 'app-org-completion-score',
  standalone: true,
  imports: [
    CommonModule, MatIconModule, MatProgressBarModule,
    MatChipsModule, MatTooltipModule, MatButtonModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './org-completion-score.component.html',
  styleUrls: ['./org-completion-score.component.scss']
})
export class OrgCompletionScoreComponent implements OnChanges {
  @Input({ required: true }) orgId!: string;

  private orgService = inject(OrganizationService);
  private destroyRef = inject(DestroyRef);

  data      = signal<ProfileCompletionScore | null>(null);
  isLoading = signal(false);
  error     = signal<string | null>(null);
  expanded  = signal(false);

  // ── SVG gauge ────────────────────────────────────────────────────────────────
  readonly RADIUS       = 36;
  readonly CIRCUMFERENCE = 2 * Math.PI * this.RADIUS;

  dashOffset = computed(() => {
    const score = this.data()?.score ?? 0;
    return this.CIRCUMFERENCE * (1 - score / 100);
  });

  gaugeColor = computed(() => this.scoreColor(this.data()?.score ?? 0));

  // ── Criteria rows ─────────────────────────────────────────────────────────────
  readonly criteria = computed(() => {
    const d = this.data();
    if (!d) return [];
    const b = d.breakdown;
    return [
      { label: 'Description',          pts: 15, done: b.hasDescription },
      { label: 'Logo',                 pts: 10, done: b.hasLogo },
      { label: 'Website',             pts: 10, done: b.hasWebsite },
      { label: '3+ specialties',       pts: 15, done: b.hasSpecialties },
      { label: 'SIRET',                pts: 10, done: b.hasSiret },
      { label: 'Location',         pts: 10, done: b.hasLocation },
      { label: 'Portfolio (≥ 1)',      pts: 15, done: b.hasPortfolio },
      { label: 'Active member',         pts: 15, done: b.hasTeamMember },
    ];
  });

  completedCount = computed(() => this.criteria().filter(c => c.done).length);

  ngOnChanges(changes: SimpleChanges) {
    if (changes['orgId'] && this.orgId) {
      this.load();
    }
  }

  load() {
    this.isLoading.set(true);
    this.error.set(null);
    this.orgService.getCompletionScore(this.orgId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next:  d  => { this.data.set(d);  this.isLoading.set(false); },
        error: _  => { this.error.set('Failed to load the completion score.'); this.isLoading.set(false); }
      });
  }

  toggle() { this.expanded.update(v => !v); }

  scoreColor(score: number): string {
    if (score >= 80) return '#4caf50';
    if (score >= 60) return '#8bc34a';
    if (score >= 40) return '#ff9800';
    return '#f44336';
  }
}
