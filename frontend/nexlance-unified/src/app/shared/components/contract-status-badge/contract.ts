import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ContractStatus } from '@shared/models/contract.model';

@Component({
  selector: 'app-contract-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `<span class="badge" [ngClass]="badgeClass">{{ label }}</span>`,
  styles: [`
    .badge {
      display: inline-block;
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .badge-draft { background: #e0e0e0; color: #616161; }
    .badge-signed { background: #fff3e0; color: #e65100; }
    .badge-pending { background: #fff8e1; color: #f57f17; }
    .badge-fully-signed { background: #e8f5e9; color: #2e7d32; }
    .badge-active { background: #e3f2fd; color: #1565c0; }
    .badge-completed { background: #e8f5e9; color: #1b5e20; }
    .badge-cancelled { background: #fce4ec; color: #c62828; }
  `]
})
export class ContractStatusBadgeComponent {
  @Input() status!: ContractStatus;

  get badgeClass(): string {
    switch (this.status) {
      case ContractStatus.DRAFT: return 'badge-draft';
      case ContractStatus.SIGNED_BY_CLIENT: return 'badge-signed';
      case ContractStatus.PENDING_FREELANCER_SIGNATURE: return 'badge-pending';
      case ContractStatus.FULLY_SIGNED: return 'badge-fully-signed';
      case ContractStatus.ACTIVE: return 'badge-active';
      case ContractStatus.COMPLETED: return 'badge-completed';
      case ContractStatus.CANCELLED: return 'badge-cancelled';
      default: return 'badge-draft';
    }
  }

  get label(): string {
    return (this.status || 'DRAFT').replace(/_/g, ' ');
  }
}
