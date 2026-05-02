import { Component, computed, signal, OnInit, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatBadgeModule } from '@angular/material/badge';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { OrganizationService } from '../../../core/services/organization.service';
import { AuthService } from '../../../core/services/auth.service';
import {
  OrganizationSummary, OrgInvitation, OrgApplication,
  OrganizationStatus, OrganizationType, OrganizationSize,
  MemberRole, ApplicationStatus, InvitationStatus,
  CollabApplication, CollabApplicationStatus
} from '../../../core/models/organization.model';
import { UserRole } from '../../../shared/models/user.model';

@Component({
  selector: 'app-my-org-hub',
  standalone: true,
  imports: [
    CommonModule, RouterModule,
    MatTabsModule, MatIconModule, MatButtonModule, MatCardModule,
    MatProgressSpinnerModule, MatBadgeModule, MatChipsModule,
    MatTooltipModule, MatDividerModule
  ],
  templateUrl: './my-org-hub.component.html',
  styleUrls: ['./my-org-hub.component.scss']
})
export class MyOrgHubComponent implements OnInit {
  private orgService = inject(OrganizationService);
  private authService = inject(AuthService);
  private router     = inject(Router);
  private destroyRef = inject(DestroyRef);

  // ── Données ────────────────────────────────────────────────────────────────
  orgs          = signal<OrganizationSummary[]>([]);
  invitations   = signal<OrgInvitation[]>([]);
  apps          = signal<OrgApplication[]>([]);
  collabApps    = signal<CollabApplication[]>([]);
  isLoading     = signal(true);
  error         = signal<string | null>(null);

  // ── Compteurs pour les badges sur les onglets ──────────────────────────────
  pendingInvCount       = computed(() => this.invitations().filter(i => i.status === InvitationStatus.PENDING).length);
  pendingAppCount       = computed(() => this.apps().filter(a => a.status === ApplicationStatus.PENDING).length);
  pendingCollabAppCount = computed(() => this.collabApps().filter(a => a.status === CollabApplicationStatus.PENDING).length);
  activeOrgCount        = computed(() => this.orgs().filter(o => o.status === OrganizationStatus.ACTIVE).length);
  pendingOrgCount       = computed(() => this.orgs().filter(o =>
    o.status === OrganizationStatus.PENDING_VERIFICATION || o.status === OrganizationStatus.AWAITING_INFO
  ).length);

  readonly CollabApplicationStatus = CollabApplicationStatus;

  // État local de réponse / retrait
  responding  = signal<string | null>(null);
  withdrawing = signal<string | null>(null);
  actionError = signal<string | null>(null);

  readonly ApplicationStatus  = ApplicationStatus;
  readonly InvitationStatus   = InvitationStatus;
  readonly OrganizationStatus = OrganizationStatus;

  // ── Init — chargement parallèle ────────────────────────────────────────────
  ngOnInit() {
    const isFreelancer = this.authService.getCurrentUser()?.role === UserRole.FREELANCER;

    forkJoin({
      orgs:        this.orgService.getMyOrganizations().pipe(catchError(() => of([]))),
      invitations: this.orgService.getMyPendingInvitations().pipe(catchError(() => of([]))),
      apps:        this.orgService.getMyOrgApplications().pipe(catchError(() => of([]))),
      collabApps:  isFreelancer
        ? this.orgService.getMyCollabApplications().pipe(catchError(() => of([])))
        : of([])
    }).pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ orgs, invitations, apps, collabApps }) => {
          this.orgs.set(Array.isArray(orgs) ? orgs : []);
          this.invitations.set(Array.isArray(invitations) ? invitations : []);
          this.apps.set(Array.isArray(apps) ? apps : []);
          this.collabApps.set(Array.isArray(collabApps) ? collabApps : []);
          this.isLoading.set(false);
        },
        error: () => {
          this.error.set('Failed to load your data. Please refresh.');
          this.isLoading.set(false);
        }
      });
  }

  // ── Actions collab ─────────────────────────────────────────────────────────
  withdrawingCollabAppId = signal<string | null>(null);

  withdrawCollabApp(app: CollabApplication) {
    if (!confirm('Withdraw this application?')) return;
    this.withdrawingCollabAppId.set(app.id);
    this.orgService.withdrawCollabApplication(app.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.collabApps.update(list =>
            list.map(a => a.id === app.id ? { ...a, status: CollabApplicationStatus.WITHDRAWN } : a)
          );
          this.withdrawingCollabAppId.set(null);
        },
        error: () => this.withdrawingCollabAppId.set(null)
      });
  }

  collabAppStatusLabel(s: CollabApplicationStatus): string {
    return ({ PENDING: 'Pending', ACCEPTED: 'Accepted', REJECTED: 'Refused', WITHDRAWN: 'Withdrawn' } as any)[s] ?? s;
  }

  collabAppStatusClass(s: CollabApplicationStatus): string {
    return ({ PENDING: 'pending', ACCEPTED: 'accepted', REJECTED: 'rejected', WITHDRAWN: 'withdrawn' } as any)[s] ?? 'other';
  }

  // ── Actions ────────────────────────────────────────────────────────────────

  respondToInvitation(inv: OrgInvitation, accept: boolean) {
    this.responding.set(inv.id);
    this.actionError.set(null);
    this.orgService.respondToInvitation(inv.organizationId, inv.id, accept)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.invitations.update(list => list.filter(i => i.id !== inv.id));
          this.responding.set(null);
          if (accept) {
            this.router.navigate(['/frontoffice/organizations', inv.organizationId]);
          }
        },
        error: err => {
          this.actionError.set(err?.error?.message ?? 'Failed to respond to invitation.');
          this.responding.set(null);
        }
      });
  }

  withdrawApplication(app: OrgApplication) {
    if (!confirm('Withdraw this application?')) return;
    this.withdrawing.set(app.id);
    this.orgService.withdrawApplication(app.organizationId, app.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.apps.update(list =>
            list.map(a => a.id === app.id ? { ...a, status: ApplicationStatus.WITHDRAWN } : a)
          );
          this.withdrawing.set(null);
        },
        error: () => this.withdrawing.set(null)
      });
  }

  // ── Navigation ─────────────────────────────────────────────────────────────
  createOrg()          { this.router.navigate(['/frontoffice/my-organizations/create']); }
  viewOrg(id: string)  { this.router.navigate(['/frontoffice/organizations', id]); }
  settingsOrg(id: string) { this.router.navigate(['/frontoffice/my-organizations', id, 'settings']); }
  browseOrgs()         { this.router.navigate(['/frontoffice/organizations']); }

  // ── Labels ─────────────────────────────────────────────────────────────────
  statusLabel(s: OrganizationStatus): string {
    return ({ ACTIVE: 'Active', PENDING_VERIFICATION: 'Under Review', AWAITING_INFO: 'Info Required',
              SUSPENDED: 'Suspended', DISSOLVED: 'Dissolved', REJECTED: 'Rejected' } as any)[s] ?? s;
  }

  statusClass(s: OrganizationStatus): string {
    return ({ ACTIVE: 'active', PENDING_VERIFICATION: 'pending', AWAITING_INFO: 'pending',
              SUSPENDED: 'suspended', DISSOLVED: 'dissolved', REJECTED: 'rejected' } as any)[s] ?? 'other';
  }

  typeLabel(t: OrganizationType): string {
    return ({ AGENCY: 'Agency', STARTUP: 'Startup',
              ASSOCIATION: 'Association', FREELANCE_COOP: 'Cooperative' } as any)[t] ?? t;
  }

  sizeLabel(s: OrganizationSize | undefined): string {
    if (!s) return '';
    return ({ SOLO: 'Solo', SMALL: '2–10', MEDIUM: '11–50', LARGE: '50+', ENTERPRISE: '200+' } as any)[s] ?? s;
  }

  roleLabel(r: MemberRole): string {
    return ({ OWNER: 'Owner', MANAGER: 'Manager', MEMBER: 'Member' } as any)[r] ?? r;
  }

  appStatusLabel(s: ApplicationStatus): string {
    return ({ PENDING: 'Pending', ACCEPTED: 'Accepted', REJECTED: 'Refused', WITHDRAWN: 'Withdrawn' } as any)[s] ?? s;
  }

  appStatusClass(s: ApplicationStatus): string {
    return ({ PENDING: 'pending', ACCEPTED: 'accepted', REJECTED: 'rejected', WITHDRAWN: 'withdrawn' } as any)[s] ?? 'other';
  }
}
