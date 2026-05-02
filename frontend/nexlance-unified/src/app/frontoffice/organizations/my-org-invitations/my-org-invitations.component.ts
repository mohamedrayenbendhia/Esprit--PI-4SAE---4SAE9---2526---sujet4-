import { Component, signal, OnInit, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { OrganizationService } from '../../../core/services/organization.service';
import { OrgInvitation, MemberRole } from '../../../core/models/organization.model';

@Component({
  selector: 'app-my-org-invitations',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatDividerModule, MatTooltipModule
  ],
  templateUrl: './my-org-invitations.component.html',
  styleUrls: ['./my-org-invitations.component.scss']
})
export class MyOrgInvitationsComponent implements OnInit {
  private orgService = inject(OrganizationService);
  private router     = inject(Router);
  private destroyRef = inject(DestroyRef);

  invitations = signal<OrgInvitation[]>([]);
  isLoading   = signal(true);
  loadError   = signal<string | null>(null);
  responding  = signal<string | null>(null);
  respondError = signal<string | null>(null);

  ngOnInit() {
    this.orgService.getMyPendingInvitations()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next:  list => { this.invitations.set(Array.isArray(list) ? list : []); this.isLoading.set(false); },
        error: ()   => {
          this.loadError.set('Failed to load invitations.');
          this.isLoading.set(false);
        }
      });
  }

  respond(inv: OrgInvitation, accept: boolean) {
    this.responding.set(inv.id);
    this.respondError.set(null);

    /**
     * FIXÉ : utilise respondToInvitation() (POST /organizations/{orgId}/invitations/{id}/respond)
     * au lieu de respondByToken() — le token n'est pas inclus dans les réponses API,
     * il est réservé aux liens email.
     */
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
          this.respondError.set(err?.error?.message ?? 'Failed to respond to invitation.');
          this.responding.set(null);
        }
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
}
