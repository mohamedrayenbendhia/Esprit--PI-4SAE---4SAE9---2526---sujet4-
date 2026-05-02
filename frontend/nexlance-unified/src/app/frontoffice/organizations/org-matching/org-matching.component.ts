import { Component, signal, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDividerModule } from '@angular/material/divider';
import { OrganizationService } from '../../../core/services/organization.service';
import {
  CompatibilityResult, CollabOfferMatchResult,
  OrganizationType, OrganizationSize
} from '../../../core/models/organization.model';

@Component({
  selector: 'app-org-matching',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatIconModule, MatButtonModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatChipsModule, MatProgressSpinnerModule,
    MatTooltipModule, MatTabsModule, MatDividerModule
  ],
  templateUrl: './org-matching.component.html',
  styleUrls: ['./org-matching.component.scss']
})
export class OrgMatchingComponent {

  private orgService = inject(OrganizationService);
  private router     = inject(Router);
  private destroyRef = inject(DestroyRef);

  readonly orgTypes = Object.values(OrganizationType);
  readonly orgSizes = Object.values(OrganizationSize);

  // ── Formulaire ─────────────────────────────────────────────────────────────
  skillInput    = '';
  skills        = signal<string[]>([]);
  location      = '';
  minBudget     : number | null = null;
  preferredType : OrganizationType | null = null;
  preferredSize : OrganizationSize | null = null;

  // ── Résultats ──────────────────────────────────────────────────────────────
  orgResults    = signal<CompatibilityResult[]>([]);
  offerResults  = signal<CollabOfferMatchResult[]>([]);
  isLoading     = signal(false);
  error         = signal<string | null>(null);
  hasSearched   = signal(false);

  // Détail dépliable
  expandedOrgId   = signal<string | null>(null);
  expandedOfferId = signal<string | null>(null);

  // ── Chips compétences ──────────────────────────────────────────────────────
  addSkill() {
    const s = this.skillInput.trim();
    if (s && !this.skills().includes(s)) this.skills.update(l => [...l, s]);
    this.skillInput = '';
  }
  addOnEnter(e: KeyboardEvent) { if (e.key === 'Enter') { e.preventDefault(); this.addSkill(); } }
  removeSkill(s: string) { this.skills.update(l => l.filter(x => x !== s)); }

  // ── Recherche ──────────────────────────────────────────────────────────────
  search() {
    this.isLoading.set(true);
    this.error.set(null);
    this.hasSearched.set(true);

    forkJoin({
      orgs: this.orgService.matchOrganizationsScored({
        freelancerSkills: this.skills(),
        freelancerLocation: this.location || undefined,
        preferredType: this.preferredType ?? undefined,
        preferredSize: this.preferredSize ?? undefined,
        limit: 20
      }).pipe(catchError(() => of([]))),

      offers: this.orgService.matchCollabOffers({
        freelancerSkills: this.skills(),
        freelancerLocation: this.location || undefined,
        minBudget: this.minBudget ?? undefined,
        limit: 20
      }).pipe(catchError(() => of([])))
    })
    .pipe(takeUntilDestroyed(this.destroyRef))
    .subscribe({
      next: ({ orgs, offers }) => {
        this.orgResults.set(orgs);
        this.offerResults.set(offers);
        this.isLoading.set(false);
      },
      error: () => {
        this.error.set('An error occurred during matching.');
        this.isLoading.set(false);
      }
    });
  }

  reset() {
    this.skills.set([]); this.location = ''; this.minBudget = null;
    this.preferredType = null; this.preferredSize = null;
    this.orgResults.set([]); this.offerResults.set([]);
    this.hasSearched.set(false);
  }

  // ── Navigation ─────────────────────────────────────────────────────────────
  viewOrg(id: string) { this.router.navigate(['/frontoffice/organizations', id]); }

  // ── UI helpers ─────────────────────────────────────────────────────────────
  toggleOrg(id: string) { this.expandedOrgId.set(this.expandedOrgId() === id ? null : id); }
  toggleOffer(id: string) { this.expandedOfferId.set(this.expandedOfferId() === id ? null : id); }

  scoreColor(s: number): string {
    if (s >= 75) return '#4caf50';
    if (s >= 50) return '#8bc34a';
    if (s >= 30) return '#ff9800';
    return '#f44336';
  }
  scoreLabel(s: number): string {
    if (s >= 75) return 'Excellent';
    if (s >= 50) return 'Good';
    if (s >= 30) return 'Average';
    return 'Low';
  }
  locLabel(m: string): string {
    return ({ EXACT: 'Same city', REGION: 'Same region', NONE: 'Remote', NOT_SPECIFIED: '—' } as any)[m] ?? m;
  }
  typeLabel(t: string): string {
    return ({ AGENCY: 'Agency', STARTUP: 'Startup', ASSOCIATION: 'Association', FREELANCE_COOP: 'Cooperative' } as any)[t] ?? t;
  }
  sizeLabel(s: string): string {
    return ({ SOLO: 'Solo', SMALL: '2–10', MEDIUM: '11–50', LARGE: '50+', ENTERPRISE: '200+' } as any)[s] ?? s;
  }
  barPct(score: number, max: number): number { return max > 0 ? Math.min((score / max) * 100, 100) : 0; }
  stars(r: number): string { return '★'.repeat(Math.round(r)) + '☆'.repeat(5 - Math.round(r)); }
}
