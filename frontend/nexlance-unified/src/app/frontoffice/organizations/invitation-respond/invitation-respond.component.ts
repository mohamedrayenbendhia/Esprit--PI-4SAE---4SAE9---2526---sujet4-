import { Component, signal, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { OrganizationService } from '../../../core/services/organization.service';

@Component({
  selector: 'app-invitation-respond',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './invitation-respond.component.html',
  styleUrls: ['./invitation-respond.component.scss']
})
export class InvitationRespondComponent implements OnInit {
  private orgService = inject(OrganizationService);
  private route      = inject(ActivatedRoute);
  private router     = inject(Router);

  token      = signal('');
  isLoading  = signal(false);
  result     = signal<'accepted' | 'declined' | 'error' | null>(null);
  orgName    = signal<string | null>(null);
  errorMsg   = signal<string | null>(null);

  ngOnInit() {
    const t = this.route.snapshot.queryParamMap.get('token') ?? '';
    this.token.set(t);
  }

  respond(accept: boolean) {
    if (!this.token()) { this.errorMsg.set('Missing token.'); return; }
    this.isLoading.set(true);
    this.orgService.respondByToken(this.token(), accept).subscribe({
      next: inv => {
        this.orgName.set(inv.organizationName ?? inv.organizationId);
        this.result.set(accept ? 'accepted' : 'declined');
        this.isLoading.set(false);
      },
      error: err => {
        this.errorMsg.set(err?.error?.message ?? 'This link is invalid or has expired.');
        this.result.set('error');
        this.isLoading.set(false);
      }
    });
  }

  goHome() { this.router.navigate(['/']); }
  goToOrg(inv: any) { if (inv?.organizationId) this.router.navigate(['/organizations', inv.organizationId]); }
}
