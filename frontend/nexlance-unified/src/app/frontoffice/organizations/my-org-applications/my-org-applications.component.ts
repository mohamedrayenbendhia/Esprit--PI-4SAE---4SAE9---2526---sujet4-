import { Component, OnInit, signal, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { OrganizationService } from '@core/services/organization.service';
import { OrgApplication, ApplicationStatus } from '@core/models/organization.model';

@Component({
  selector: 'app-my-org-applications',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule, MatCardModule],
  templateUrl: './my-org-applications.component.html',
  styleUrls: ['./my-org-applications.component.scss']
})
export class MyOrgApplicationsComponent implements OnInit {
  private orgService = inject(OrganizationService);
  private router     = inject(Router);
  private destroyRef = inject(DestroyRef);

  apps        = signal<OrgApplication[]>([]);
  isLoading   = signal(true);
  withdrawing = signal<string | null>(null);
  ApplicationStatus = ApplicationStatus;

  ngOnInit() {
    this.orgService.getMyOrgApplications()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: list => { this.apps.set(Array.isArray(list) ? list : []); this.isLoading.set(false); },
        error: () => this.isLoading.set(false)
      });
  }

  viewOrg(orgId: string) { this.router.navigate(['/frontoffice/organizations', orgId]); }

  withdraw(app: OrgApplication) {
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

  getStatusLabel(s: ApplicationStatus): string {
    return { PENDING: 'Pending', ACCEPTED: 'Accepted', REJECTED: 'Refused', WITHDRAWN: 'Withdrawn' }[s] || s;
  }

  getStatusColor(s: ApplicationStatus): string {
    return { PENDING: '#f59e0b', ACCEPTED: '#10b981', REJECTED: '#ef4444', WITHDRAWN: '#9ca3af' }[s] || '#9ca3af';
  }
}
