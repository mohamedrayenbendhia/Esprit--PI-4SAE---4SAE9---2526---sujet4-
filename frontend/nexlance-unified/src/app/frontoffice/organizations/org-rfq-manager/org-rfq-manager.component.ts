import { Component, Input, OnInit, signal, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { OrganizationService } from '@core/services/organization.service';
import { OrgRfq, RfqStatus, RfqResponseRequest } from '@core/models/organization.model';

@Component({
  selector: 'app-org-rfq-manager',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSnackBarModule],
  templateUrl: './org-rfq-manager.component.html',
  styleUrls: ['./org-rfq-manager.component.scss']
})
export class OrgRfqManagerComponent implements OnInit {
  @Input() orgId!: string;

  rfqs            = signal<OrgRfq[]>([]);
  isLoading       = signal(true);
  total           = signal(0);
  page            = signal(0);
  respondingId    = signal<string | null>(null);
  closingId       = signal<string | null>(null);
  responseMessage = '';
  private orgService = inject(OrganizationService);
  private snack      = inject(MatSnackBar);
  private destroyRef = inject(DestroyRef);

  RfqStatus = RfqStatus;

  ngOnInit() { this.load(); }

  load() {
    this.isLoading.set(true);
    this.orgService.getOrgRfqs(this.orgId, this.page())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: p => { this.rfqs.set(p?.content ?? []); this.total.set(p?.totalElements ?? 0); this.isLoading.set(false); },
        error: () => this.isLoading.set(false)
      });
  }

  openRespond(rfqId: string) { this.respondingId.set(rfqId); this.responseMessage = ''; }
  cancelRespond() { this.respondingId.set(null); }

  submitRespond(rfq: OrgRfq) {
    if (!this.responseMessage.trim()) return;
    const req: RfqResponseRequest = { responseMessage: this.responseMessage };
    this.orgService.respondToRfq(this.orgId, rfq.id, req)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => {
          this.rfqs.update(list => list.map(r => r.id === rfq.id ? updated : r));
          this.snack.open('Response sent.', 'OK', { duration: 2500 });
          this.respondingId.set(null);
        },
        error: err => this.snack.open(err?.error?.message ?? 'An error occurred.', 'OK', { duration: 3000 })
      });
  }

  close(rfq: OrgRfq) {
    if (!confirm('Permanently close this quote request?')) return;
    this.closingId.set(rfq.id);
    this.orgService.closeRfq(this.orgId, rfq.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.rfqs.update(list =>
            list.map(r => r.id === rfq.id ? { ...r, status: RfqStatus.CLOSED } : r)
          );
          this.snack.open('Request closed.', 'OK', { duration: 2500 });
          this.closingId.set(null);
        },
        error: err => {
          this.snack.open(err?.error?.message ?? 'An error occurred.', 'OK', { duration: 3000 });
          this.closingId.set(null);
        }
      });
  }

  getStatusLabel(s: RfqStatus): string {
    return { PENDING: 'Pending', RESPONDED: 'Responded', CLOSED: 'Closed' }[s] || s;
  }

  getStatusColor(s: RfqStatus): string {
    return { PENDING: '#f59e0b', RESPONDED: '#10b981', CLOSED: '#9ca3af' }[s] || '#9ca3af';
  }
}
