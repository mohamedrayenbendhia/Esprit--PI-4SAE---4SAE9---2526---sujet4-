import { Component, signal, OnInit, inject, DestroyRef, computed, Signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipInputEvent, MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { COMMA, ENTER } from '@angular/cdk/keycodes';
import { OrganizationService } from '../../../core/services/organization.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import {
  Organization, OrgMember, OrgInvitation,
  UpdateOrganizationRequest, InviteMemberRequest,
  MemberRole, OrganizationVisibility, OrganizationSize
} from '../../../core/models/organization.model';
import { OrgPortfolioComponent } from '../org-portfolio/org-portfolio.component';
import { OrgApplicationsManagerComponent } from '../org-applications-manager/org-applications-manager.component';
import { OrgRfqManagerComponent } from '../org-rfq-manager/org-rfq-manager.component';
import { OrgAnalyticsComponent } from '../org-analytics/org-analytics.component';
import { OrgCompletionScoreComponent } from '../org-completion-score/org-completion-score.component';

@Component({
  selector: 'app-organization-settings',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatTabsModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatButtonModule, MatIconModule, MatChipsModule,
    MatProgressSpinnerModule, MatDividerModule, MatTooltipModule,
    OrgPortfolioComponent, OrgApplicationsManagerComponent, OrgRfqManagerComponent,
    OrgAnalyticsComponent, OrgCompletionScoreComponent
  ],
  templateUrl: './organization-settings.component.html',
  styleUrls: ['./organization-settings.component.scss']
})
export class OrganizationSettingsComponent implements OnInit {
  private orgService          = inject(OrganizationService);
  private route               = inject(ActivatedRoute);
  private router              = inject(Router);
  private authService         = inject(AuthService);
  private notificationService = inject(NotificationService);
  private destroyRef          = inject(DestroyRef);

  readonly separators   = [ENTER, COMMA];
  readonly sizes        = Object.values(OrganizationSize);
  readonly roles        = [MemberRole.MEMBER, MemberRole.MANAGER];
  readonly visibilities = Object.values(OrganizationVisibility);
  readonly MemberRole   = MemberRole;

  org         = signal<Organization | null>(null);
  members     = signal<OrgMember[]>([]);
  invitations = signal<OrgInvitation[]>([]);
  isLoading   = signal(true);
  loadError   = signal<string | null>(null);

  /**
   * FIXÉ : isOwner est maintenant dérivé de org.ownerId === currentUserId,
   * sans dépendre du chargement de la liste des membres (qui peut être lent/vide).
   */
  isOwner = computed(() => {
    const o = this.org();
    return !!o && o.ownerId === this.currentUserId;
  });

  /**
   * isManager : soit propriétaire, soit le membre trouvé dans la liste avec rôle MANAGER.
   */
  isManager = computed(() => {
    if (this.isOwner()) return true;
    return this.members().some(
      m => m.userId === this.currentUserId && m.role === MemberRole.MANAGER
    );
  });

  /**
   * Membres éligibles au transfert de propriété :
   * membres ACTIFS de l'org, excluant le propriétaire actuel.
   */
  transferableMembers = computed(() =>
    this.members().filter(
      m => m.userId !== this.currentUserId && m.status === 'ACTIVE' as any
    )
  );

  editForm: UpdateOrganizationRequest = {};
  editSpecialties = signal<string[]>([]);
  saveLoading  = signal(false);
  saveSuccess  = signal(false);
  saveError    = signal<string | null>(null);

  // Formulaire d'invitation — noms alignés sur le backend
  inviteForm: InviteMemberRequest = { inviteeEmail: '', role: MemberRole.MEMBER };
  inviteLoading = signal(false);
  inviteError   = signal<string | null>(null);
  inviteSuccess = signal(false);

  dissolveConfirmName = signal('');
  dissolveLoading     = signal(false);
  dissolveError       = signal<string | null>(null);

  leaveLoading = signal(false);
  leaveError   = signal<string | null>(null);

  transferNewOwnerId = signal('');
  transferLoading    = signal(false);
  transferError      = signal<string | null>(null);
  transferSuccess    = signal(false);

  /** Convertit n'importe quelle valeur en tableau — garde-fou universel pour @for */
  safeArray<T>(val: T[] | null | undefined | any): T[] {
    return Array.isArray(val) ? val : [];
  }

  get orgId(): string         { return this.route.snapshot.paramMap.get('id')!; }
  get currentUserId(): string { return this.authService.getCurrentUser()?.id ?? ''; }

  ngOnInit() {
    // Chargement de l'organisation (source de vérité pour isOwner)
    this.orgService.getById(this.orgId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: o => {
          this.org.set(o);
          this.editForm = {
            name:        o.name,
            description: o.description ?? '',
            logoUrl:     o.logoUrl     ?? '',
            website:     o.website     ?? '',
            location:    o.location    ?? '',
            size:        o.size
          };
          this.editSpecialties.set([...(o.specialties ?? [])]);
          this.isLoading.set(false);
        },
        error: () => {
          this.loadError.set('Failed to load organization.');
          this.isLoading.set(false);
        }
      });

    // Membres (service extrait déjà .content)
    this.orgService.getMembers(this.orgId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: list => this.members.set(Array.isArray(list) ? list : []) });

    // Invitations en attente (service extrait déjà .content)
    this.orgService.getPendingInvitationsForOrg(this.orgId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: list => this.invitations.set(Array.isArray(list) ? list : []) });
  }

  addSpecialty(event: MatChipInputEvent) {
    const v = (event.value || '').trim();
    if (v) this.editSpecialties.update(s => [...s, v]);
    event.chipInput!.clear();
  }

  removeSpecialty(s: string) {
    this.editSpecialties.update(list => list.filter(x => x !== s));
  }

  saveInfo() {
    this.saveLoading.set(true);
    this.saveError.set(null);
    this.saveSuccess.set(false);

    const payload: UpdateOrganizationRequest = {
      ...this.editForm,
      specialties: this.editSpecialties()
    };

    this.orgService.update(this.orgId, payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => {
          this.org.set(updated);
          this.saveSuccess.set(true);
          this.saveLoading.set(false);
          setTimeout(() => this.saveSuccess.set(false), 3000);
        },
        error: err => {
          this.saveError.set(err?.error?.message ?? 'Failed to save changes.');
          this.saveLoading.set(false);
        }
      });
  }

  setVisibility(v: OrganizationVisibility) {
    this.orgService.setVisibility(this.orgId, v)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: updated => this.org.set(updated) });
  }

  promote(memberId: string) {
    this.orgService.updateMemberRole(this.orgId, memberId, MemberRole.MANAGER)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => this.members.update(list => list.map(m => m.id === memberId ? updated : m))
      });
  }

  demote(memberId: string) {
    this.orgService.updateMemberRole(this.orgId, memberId, MemberRole.MEMBER)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => this.members.update(list => list.map(m => m.id === memberId ? updated : m))
      });
  }

  remove(memberId: string) {
    if (!confirm('Remove this member from the organization?')) return;
    this.orgService.removeMember(this.orgId, memberId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.members.update(list => list.filter(m => m.id !== memberId))
      });
  }

  sendInvite() {
    // Validation : email ou ID requis
    if (!this.inviteForm.inviteeEmail?.trim() && !this.inviteForm.inviteeId?.trim()) {
      this.inviteError.set('Email or user ID required.');
      return;
    }
    this.inviteLoading.set(true);
    this.inviteError.set(null);

    this.orgService.invite(this.orgId, this.inviteForm)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: inv => {
          this.invitations.update(list => [inv, ...list]);
          this.inviteSuccess.set(true);
          this.inviteForm = { inviteeEmail: '', role: MemberRole.MEMBER };
          this.inviteLoading.set(false);
          setTimeout(() => this.inviteSuccess.set(false), 3000);
          // Refresh the notification bell count — the backend sends a notification to the invitee
          this.notificationService.loadUnreadCount(this.currentUserId);
        },
        error: err => {
          this.inviteError.set(err?.error?.message ?? 'Failed to send invitation.');
          this.inviteLoading.set(false);
        }
      });
  }

  cancelInvitation(invId: string) {
    this.orgService.cancelInvitation(this.orgId, invId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.invitations.update(list => list.filter(i => i.id !== invId))
      });
  }

  dissolve() {
    if (this.dissolveConfirmName() !== this.org()?.name) {
      this.dissolveError.set('The entered name does not match.');
      return;
    }
    this.dissolveLoading.set(true);
    this.orgService.dissolve(this.orgId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.router.navigate(['/frontoffice/my-organizations']),
        error: err => {
          this.dissolveError.set(err?.error?.message ?? 'Failed to dissolve organization.');
          this.dissolveLoading.set(false);
        }
      });
  }

  leaveOrg() {
    if (!confirm('Leave this organization?')) return;
    this.leaveLoading.set(true);
    this.leaveError.set(null);
    this.orgService.leaveOrganization(this.orgId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.router.navigate(['/frontoffice/my-organizations']),
        error: err => {
          this.leaveError.set(err?.error?.message ?? 'Failed to leave organization.');
          this.leaveLoading.set(false);
        }
      });
  }

  transferOwnership() {
    const newOwnerId = this.transferNewOwnerId();
    if (!newOwnerId) {
      this.transferError.set('Please select a member from the list.');
      return;
    }
    const memberName = this.transferableMembers().find(m => m.userId === newOwnerId)?.userId ?? newOwnerId;
    if (!confirm(`Permanently transfer ownership to ${memberName}? You will become a regular member.`)) return;
    this.transferLoading.set(true);
    this.transferError.set(null);
    this.orgService.transferOwnership(this.orgId, newOwnerId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.transferSuccess.set(true);
          this.transferLoading.set(false);
          setTimeout(() => this.router.navigate(['/frontoffice/my-organizations']), 1500);
        },
        error: err => {
          this.transferError.set(err?.error?.message ?? 'Failed to transfer ownership.');
          this.transferLoading.set(false);
        }
      });
  }

  goToProfile() {
    this.router.navigate(['/frontoffice/organizations', this.orgId]);
  }

  roleLabel(role: MemberRole): string {
    const labels: Record<MemberRole, string> = {
      [MemberRole.OWNER]:   'Owner',
      [MemberRole.MANAGER]: 'Manager',
      [MemberRole.MEMBER]:  'Member'
    };
    return labels[role] ?? role;
  }

  visibilityLabel(v: OrganizationVisibility): string {
    const labels: Record<OrganizationVisibility, string> = {
      [OrganizationVisibility.PUBLIC]:       'Public',
      [OrganizationVisibility.PRIVATE]:      'Private',
      [OrganizationVisibility.MEMBERS_ONLY]: 'Members only'
    };
    return labels[v] ?? v;
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: 'Active', PENDING_VERIFICATION: 'Under Review',
      AWAITING_INFO: 'Info Required', SUSPENDED: 'Suspended',
      DISSOLVED: 'Dissolved', REJECTED: 'Rejected'
    };
    return map[status] ?? status;
  }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: 'active', PENDING_VERIFICATION: 'pending',
      AWAITING_INFO: 'pending', SUSPENDED: 'suspended',
      DISSOLVED: 'dissolved', REJECTED: 'rejected'
    };
    return map[status] ?? 'other';
  }
}
