import { Component, OnInit, signal, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { OrganizationService } from '@core/services/organization.service';
import { OrgRfq, RfqStatus } from '@core/models/organization.model';

@Component({
  selector: 'app-my-org-rfqs',
  standalone: true,
  imports: [CommonModule, RouterModule, MatIconModule, MatButtonModule],
  templateUrl: './my-org-rfqs.component.html',
  styleUrls: ['./my-org-rfqs.component.scss']
})
export class MyOrgRfqsComponent implements OnInit {
  private orgService = inject(OrganizationService);
  private router     = inject(Router);
  private destroyRef = inject(DestroyRef);

  rfqs      = signal<OrgRfq[]>([]);
  isLoading = signal(true);
  RfqStatus = RfqStatus;

  ngOnInit() {
    this.orgService.getMyRfqs()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: list => { this.rfqs.set(Array.isArray(list) ? list : []); this.isLoading.set(false); },
        error: () => this.isLoading.set(false)
      });
  }

  viewOrg(orgId: string) { this.router.navigate(['/frontoffice/organizations', orgId]); }

  getStatusLabel(s: RfqStatus): string {
    return { PENDING: 'Pending', RESPONDED: 'Response received', CLOSED: 'Closed' }[s] || s;
  }

  getStatusColor(s: RfqStatus): string {
    return { PENDING: '#f59e0b', RESPONDED: '#10b981', CLOSED: '#9ca3af' }[s] || '#9ca3af';
  }
}
