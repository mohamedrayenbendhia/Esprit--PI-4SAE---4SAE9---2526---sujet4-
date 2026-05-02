import { Component, computed, signal, OnInit, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { OrganizationService } from '../../../core/services/organization.service';
import {
  OrganizationSummary, OrganizationStatus,
  OrganizationType, OrganizationSize
} from '../../../core/models/organization.model';

@Component({
  selector: 'app-my-organizations',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './my-organizations.component.html',
  styleUrls: ['./my-organizations.component.scss']
})
export class MyOrganizationsComponent implements OnInit {
  private orgService = inject(OrganizationService);
  private router     = inject(Router);
  private destroyRef = inject(DestroyRef);

  orgs      = signal<OrganizationSummary[]>([]);
  isLoading = signal(true);
  error     = signal<string | null>(null);

  activeCount  = computed(() => this.orgs().filter(o => o.status === OrganizationStatus.ACTIVE).length);
  pendingCount = computed(() => this.orgs().filter(o =>
    o.status === OrganizationStatus.PENDING_VERIFICATION ||
    o.status === OrganizationStatus.AWAITING_INFO
  ).length);

  ngOnInit() {
    this.orgService.getMyOrganizations()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next:  list => { this.orgs.set(Array.isArray(list) ? list : []); this.isLoading.set(false); },
        error: ()   => { this.error.set('Failed to load your organizations.'); this.isLoading.set(false); }
      });
  }

  create()             { this.router.navigate(['/frontoffice/my-organizations/create']); }
  view(id: string)     { this.router.navigate(['/frontoffice/organizations', id]); }
  settings(id: string) { this.router.navigate(['/frontoffice/my-organizations', id, 'settings']); }

  statusClass(status: OrganizationStatus): string {
    const map: Record<string, string> = {
      ACTIVE: 'active', PENDING_VERIFICATION: 'pending', AWAITING_INFO: 'pending',
      SUSPENDED: 'suspended', DISSOLVED: 'dissolved', REJECTED: 'rejected'
    };
    return map[status] ?? 'other';
  }

  statusLabel(status: OrganizationStatus): string {
    const map: Record<string, string> = {
      ACTIVE: 'Active', PENDING_VERIFICATION: 'Under Review',
      AWAITING_INFO: 'Info Required', SUSPENDED: 'Suspended',
      DISSOLVED: 'Dissolved', REJECTED: 'Rejected'
    };
    return map[status] ?? status;
  }

  typeLabel(type: OrganizationType): string {
    const map: Record<string, string> = {
      AGENCY: 'Agency', STARTUP: 'Startup',
      ASSOCIATION: 'Association', FREELANCE_COOP: 'Cooperative'
    };
    return map[type] ?? type;
  }

  sizeLabel(size: OrganizationSize | undefined): string {
    if (!size) return '';
    const map: Record<string, string> = {
      SOLO: 'Solo', SMALL: '2–10', MEDIUM: '11–50', LARGE: '50+'
    };
    return map[size] ?? size;
  }
}
