import { Component, Input, OnInit, signal, computed, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { OrganizationService } from '../../../core/services/organization.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserRole } from '../../../shared/models/user.model';
import {
  CollabOffer, CollabApplication, CollabOfferStatus, CollabApplicationStatus,
  ApplyCollabOfferRequest, Page
} from '../../../core/models/organization.model';

/**
 * Composant affiché dans l'onglet "Collaborations" du profil d'une organisation.
 * - Vue freelance externe : liste des offres ouvertes + bouton postuler
 * - Vue owner/manager : toutes les offres + boutons clôturer/annuler
 */
@Component({
  selector: 'app-org-collab-offers',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule, MatChipsModule,
    MatProgressSpinnerModule, MatSnackBarModule, MatTooltipModule
  ],
  templateUrl: './org-collab-offers.component.html',
  styleUrls: ['./org-collab-offers.component.scss']
})
export class OrgCollabOffersComponent implements OnInit {
  @Input() orgId!: string;
  @Input() isOwnerOrManager = false;

  private orgService = inject(OrganizationService);
  private authService = inject(AuthService);
  private router      = inject(Router);
  private snack       = inject(MatSnackBar);
  private destroyRef  = inject(DestroyRef);

  // ── State ──────────────────────────────────────────────────────────────────
  offers    = signal<CollabOffer[]>([]);
  isLoading = signal(true);
  error     = signal<string | null>(null);

  // Candidature en cours
  applyingOfferId = signal<string | null>(null);
  applyForm       = { message: '', portfolioUrl: '' };
  applying        = signal(false);
  appliedOfferIds = signal<Set<string>>(new Set());

  // Vue manager : offre sélectionnée pour voir ses candidatures
  selectedOfferId   = signal<string | null>(null);
  applications      = signal<CollabApplication[]>([]);
  loadingApps       = signal(false);
  respondingAppId   = signal<string | null>(null);

  readonly CollabOfferStatus      = CollabOfferStatus;
  readonly CollabApplicationStatus = CollabApplicationStatus;

  get currentUserId(): string { return this.authService.getCurrentUser()?.id ?? ''; }
  get isFreelance(): boolean  { return this.authService.getCurrentUser()?.role === UserRole.FREELANCER; }

  ngOnInit() {
    this.loadOffers();
    if (this.isFreelance) {
      this.loadMyApplications();
    }
  }

  loadOffers() {
    this.isLoading.set(true);
    this.orgService.getCollabOffers(this.orgId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: page => {
          this.offers.set(page.content ?? []);
          this.isLoading.set(false);
        },
        error: () => {
          this.error.set('Failed to load offers.');
          this.isLoading.set(false);
        }
      });
  }

  loadMyApplications() {
    this.orgService.getMyCollabApplications()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: apps => {
          const ids = new Set(
            apps
              .filter(a => a.organizationId === this.orgId &&
                           a.status !== CollabApplicationStatus.WITHDRAWN)
              .map(a => a.offerId)
          );
          this.appliedOfferIds.set(ids);
        }
      });
  }

  // ── Postuler ───────────────────────────────────────────────────────────────

  startApply(offerId: string) {
    this.applyingOfferId.set(offerId);
    this.applyForm = { message: '', portfolioUrl: '' };
  }

  cancelApply() {
    this.applyingOfferId.set(null);
  }

  submitApply(offerId: string) {
    if (!this.applyForm.message.trim()) return;
    this.applying.set(true);
    const req: ApplyCollabOfferRequest = {
      message: this.applyForm.message,
      portfolioUrl: this.applyForm.portfolioUrl || undefined
    };
    this.orgService.applyToCollabOffer(offerId, req)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.appliedOfferIds.update(s => new Set([...s, offerId]));
          this.applyingOfferId.set(null);
          this.applying.set(false);
          this.snack.open('Application submitted!', 'OK', { duration: 3000 });
        },
        error: err => {
          this.snack.open(err?.error?.message ?? 'Error submitting the application.', 'OK', { duration: 4000 });
          this.applying.set(false);
        }
      });
  }

  // ── Gestion des offres (manager) ───────────────────────────────────────────

  closeOffer(offerId: string) {
    if (!confirm('Close this offer? It will no longer accept applications.')) return;
    this.orgService.closeCollabOffer(this.orgId, offerId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => {
          this.offers.update(list => list.map(o => o.id === offerId ? updated : o));
          this.snack.open('Offer closed.', 'OK', { duration: 2500 });
        },
        error: err => this.snack.open(err?.error?.message ?? 'An error occurred.', 'OK', { duration: 3000 })
      });
  }

  cancelOffer(offerId: string) {
    if (!confirm('Cancel this offer?')) return;
    this.orgService.cancelCollabOffer(this.orgId, offerId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => {
          this.offers.update(list => list.map(o => o.id === offerId ? updated : o));
          this.snack.open('Offer cancelled.', 'OK', { duration: 2500 });
        },
        error: err => this.snack.open(err?.error?.message ?? 'An error occurred.', 'OK', { duration: 3000 })
      });
  }

  // ── Candidatures (vue manager) ─────────────────────────────────────────────

  toggleApplications(offerId: string) {
    if (this.selectedOfferId() === offerId) {
      this.selectedOfferId.set(null);
      return;
    }
    this.selectedOfferId.set(offerId);
    this.loadingApps.set(true);
    this.orgService.getCollabApplicationsForOffer(offerId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: page => {
          this.applications.set(page.content ?? []);
          this.loadingApps.set(false);
        },
        error: () => this.loadingApps.set(false)
      });
  }

  respondToApp(appId: string, accept: boolean) {
    this.respondingAppId.set(appId);
    const req = { status: accept ? CollabApplicationStatus.ACCEPTED : CollabApplicationStatus.REJECTED };
    this.orgService.respondToCollabApplication(appId, req)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => {
          this.applications.update(list => list.map(a => a.id === appId ? updated : a));
          this.respondingAppId.set(null);
          this.snack.open(accept ? 'Application accepted!' : 'Application declined.', 'OK', { duration: 2500 });
          this.loadOffers(); // refresh counts
        },
        error: err => {
          this.snack.open(err?.error?.message ?? 'An error occurred.', 'OK', { duration: 3000 });
          this.respondingAppId.set(null);
        }
      });
  }

  // ── Créer une offre (navigation) ───────────────────────────────────────────

  createOffer() {
    this.router.navigate(['/frontoffice/organizations', this.orgId, 'collab-offers', 'new']);
  }

  // ── Labels/helpers ─────────────────────────────────────────────────────────

  statusLabel(s: CollabOfferStatus): string {
    return { OPEN: 'Open', CLOSED: 'Closed', CANCELLED: 'Cancelled' }[s] ?? s;
  }

  statusClass(s: CollabOfferStatus): string {
    return { OPEN: 'open', CLOSED: 'closed', CANCELLED: 'cancelled' }[s] ?? '';
  }

  appStatusLabel(s: CollabApplicationStatus): string {
    return { PENDING: 'Pending', ACCEPTED: 'Accepted', REJECTED: 'Refused', WITHDRAWN: 'Withdrawn' }[s] ?? s;
  }

  appStatusClass(s: CollabApplicationStatus): string {
    return { PENDING: 'pending', ACCEPTED: 'accepted', REJECTED: 'rejected', WITHDRAWN: 'withdrawn' }[s] ?? '';
  }

  hasApplied(offerId: string): boolean {
    return this.appliedOfferIds().has(offerId);
  }

  isExpired(deadlineDate: string | null): boolean {
    if (!deadlineDate) return false;
    return new Date(deadlineDate) < new Date();
  }
}
