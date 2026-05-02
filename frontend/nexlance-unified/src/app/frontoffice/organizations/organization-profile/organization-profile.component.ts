import { Component, signal, OnInit, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { OrganizationService } from '../../../core/services/organization.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserRole } from '../../../shared/models/user.model';
import {
  Organization, OrgMember, OrgReview, CreateReviewRequest, MemberRole
} from '../../../core/models/organization.model';
import { OrgBadgesComponent } from '../../../shared/components/org-badges/org-badges.component';
import { OrgPortfolioComponent } from '../org-portfolio/org-portfolio.component';
import { ApplyToOrgComponent } from '../apply-to-org/apply-to-org.component';
import { SubmitRfqComponent } from '../submit-rfq/submit-rfq.component';
import { OrgCollabOffersComponent } from '../org-collab-offers/org-collab-offers.component';
import { OrgTrustScoreComponent } from '../org-trust-score/org-trust-score.component';
import { OrgMapComponent } from '../org-map/org-map.component';

@Component({
  selector: 'app-organization-profile',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatButtonModule, MatIconModule, MatTabsModule,
    MatChipsModule, MatProgressSpinnerModule, MatFormFieldModule,
    MatInputModule, MatDividerModule, MatTooltipModule,
    OrgBadgesComponent, OrgPortfolioComponent,
    ApplyToOrgComponent, SubmitRfqComponent,
    OrgCollabOffersComponent, OrgTrustScoreComponent,
    OrgMapComponent
  ],
  templateUrl: './organization-profile.component.html',
  styleUrls: ['./organization-profile.component.scss']
})
export class OrganizationProfileComponent implements OnInit {
  private orgService   = inject(OrganizationService);
  private route        = inject(ActivatedRoute);
  private router       = inject(Router);
  private authService  = inject(AuthService);
  private destroyRef   = inject(DestroyRef);

  org       = signal<Organization | null>(null);
  members   = signal<OrgMember[]>([]);
  reviews   = signal<OrgReview[]>([]);
  isLoading = signal(true);
  loadError = signal<string | null>(null);
  isMyOrg   = signal(false);
  canReview = signal(false);

  // Formulaire d'avis — note unique 1-5 (aligné backend)
  showReviewForm   = signal(false);
  reviewSubmitting = signal(false);
  reviewSuccess    = signal(false);
  reviewError      = signal<string | null>(null);
  newReview: CreateReviewRequest = { rating: 5, comment: '' };

  // Réponse propriétaire
  replyingReviewId = signal<string | null>(null);
  replyText        = signal('');
  replySubmitting  = signal(false);

  get orgId(): string        { return this.route.snapshot.paramMap.get('id')!; }
  get currentUserId(): string { return this.authService.getCurrentUser()?.id ?? ''; }
  get isLoggedIn(): boolean  { return !!this.authService.getCurrentUser(); }
  get isClient(): boolean    { return this.authService.getCurrentUser()?.role === UserRole.CLIENT; }
  get isFreelance(): boolean { return this.authService.getCurrentUser()?.role === UserRole.FREELANCER; }

  readonly MemberRole = MemberRole;

  ngOnInit() {
    // Chargement principal de l'organisation
    this.orgService.getById(this.orgId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: o => {
          this.org.set(o);
          this.isMyOrg.set(o.ownerId === this.currentUserId);
          this.isLoading.set(false);
        },
        error: () => {
          this.loadError.set('Organization not found or access denied.');
          this.isLoading.set(false);
        }
      });

    // Membres (getMembers extrait déjà .content côté service)
    this.orgService.getMembers(this.orgId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: m => this.members.set(Array.isArray(m) ? m : []),
        error: () => this.members.set([])
      });

    this.loadReviews();

    if (this.isLoggedIn) {
      this.orgService.canReview(this.orgId)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({ next: canReview => this.canReview.set(canReview), error: () => this.canReview.set(false) });
    }
  }

  goToSettings() {
    this.router.navigate(['/frontoffice/my-organizations', this.orgId, 'settings']);
  }

  submitReview() {
    if (!this.isLoggedIn) {
      this.router.navigate(['/auth/login']);
      return;
    }
    if (!this.newReview.rating) return;
    this.reviewSubmitting.set(true);
    this.reviewError.set(null);
    this.orgService.submitReview(this.orgId, this.newReview)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: r => {
          if (r?.id) {
            this.reviews.update(list => [r, ...list.filter(item => item.id !== r.id)]);
          }
          this.loadReviews();
          this.reviewSuccess.set(true);
          this.canReview.set(true);
          this.showReviewForm.set(false);
          this.reviewSubmitting.set(false);
          this.newReview = { rating: 5, comment: '' };
        },
        error: err => {
          this.reviewError.set(err?.error?.message ?? 'Failed to publish review.');
          this.reviewSubmitting.set(false);
        }
      });
  }

  private loadReviews() {
    this.orgService.getReviews(this.orgId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: r => this.reviews.set(Array.isArray(r?.content) ? r.content : []),
        error: () => this.reviews.set([])
      });
  }

  startReply(reviewId: string) {
    this.replyingReviewId.set(reviewId);
    this.replyText.set('');
  }

  cancelReply() {
    this.replyingReviewId.set(null);
  }

  submitReply(reviewId: string) {
    if (!this.replyText().trim()) return;
    this.replySubmitting.set(true);
    this.orgService.replyToReview(this.orgId, reviewId, this.replyText())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => {
          this.reviews.update(list => list.map(r => r.id === reviewId ? updated : r));
          this.replyingReviewId.set(null);
          this.replySubmitting.set(false);
        },
        error: () => this.replySubmitting.set(false)
      });
  }

  canDeleteReview(review: OrgReview): boolean {
    return this.isMyOrg() || review.reviewerId === this.currentUserId;
  }

  deleteReview(reviewId: string) {
    if (!confirm('Permanently delete this review?')) return;
    this.orgService.deleteReview(this.orgId, reviewId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.reviews.update(list => list.filter(r => r.id !== reviewId))
      });
  }

  reportReview(reviewId: string) {
    this.orgService.reportReview(this.orgId, reviewId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => this.reviews.update(list => list.map(r => r.id === reviewId ? updated : r))
      });
  }

  roleLabel(role: MemberRole): string {
    const labels: Record<MemberRole, string> = {
      [MemberRole.OWNER]:   'Owner',
      [MemberRole.MANAGER]: 'Manager',
      [MemberRole.MEMBER]:  'Member'
    };
    return labels[role] ?? role;
  }

  typeLabel(type: string): string {
    const labels: Record<string, string> = {
      AGENCY: 'Agency', STARTUP: 'Startup',
      ASSOCIATION: 'Association', FREELANCE_COOP: 'Freelance Cooperative'
    };
    return labels[type] ?? type;
  }

  sizeLabel(size: string): string {
    const labels: Record<string, string> = {
      SOLO: 'Solo', SMALL: 'Small (2-10)', MEDIUM: 'Medium (11-50)',
      LARGE: 'Large (51-200)', ENTERPRISE: 'Very Large (200+)'
    };
    return labels[size] ?? size;
  }

  /** Génère une chaîne d'étoiles pour l'affichage (ex: ★★★☆☆) */
  stars(n: number): string {
    const clamped = Math.round(Math.max(0, Math.min(5, n)));
    return '★'.repeat(clamped) + '☆'.repeat(5 - clamped);
  }
}
