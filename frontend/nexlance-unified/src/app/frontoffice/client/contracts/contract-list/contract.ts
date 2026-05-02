import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ContractService } from '@core/services/contract.service';
import { AuthService } from '@core/services/auth.service';
import { Contract, ContractStatus, ContractType } from '@shared/models/contract.model';
import { ContractStatusBadgeComponent } from '@shared/components/contract-status-badge/contract';

@Component({
  selector: 'app-contract-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ContractStatusBadgeComponent],
  templateUrl: './contract.html',
  styleUrls: ['./contract.css']
})
export class ContractListComponent implements OnInit {
  hasPendingAction(_t70: Contract) {
    throw new Error('Method not implemented.');
  }

  private contractService = inject(ContractService);
  private authService = inject(AuthService);
  private router = inject(Router);

  contracts: Contract[] = [];
  filteredContracts: Contract[] = [];
  loading = false;
  error = '';

  selectedStatus: ContractStatus | '' = '';
  searchKeyword = '';

  ContractStatus = ContractStatus;
  ContractType = ContractType;
  statuses = Object.values(ContractStatus);

  stats = {
    totalContracts: 0,
    draftContracts: 0,
    activeContracts: 0,
    completedContracts: 0,
    pendingSignature: 0
  };
  earnings: any;

  ngOnInit(): void {
    this.loadContracts();
    this.loadStats();
  }

  get clientId(): string {
    return this.authService.getCurrentUser()?.id || '';
  }

  loadContracts(): void {
    this.loading = true;
    this.contractService.getMyContractsAsClient(this.clientId).subscribe({
      next: (data) => {
        this.contracts = data;
        this.filteredContracts = data;
        this.loading = false;
      },
      error: () => {
        this.error = 'Error loading contracts';
        this.loading = false;
      }
    });
  }

  loadStats(): void {
    this.contractService.getClientStatistics(this.clientId).subscribe({
      next: (data) => this.stats = data as any,
      error: () => {
        this.stats = {
          totalContracts: 0,
          draftContracts: 0,
          activeContracts: 0,
          completedContracts: 0,
          pendingSignature: 0
        };
      }
    });
  }

  applyFilters(): void {
    this.contractService.searchContractsAsClient(
      this.clientId,
      this.selectedStatus as ContractStatus || undefined,
      this.searchKeyword || undefined
    ).subscribe({
      next: (data) => this.filteredContracts = data,
      error: () => {
        this.filteredContracts = [];
        this.error = 'Error searching contracts';
      }
    });
  }

  resetFilters(): void {
    this.selectedStatus = '';
    this.searchKeyword = '';
    this.filteredContracts = this.contracts;
  }

  goToCreate(): void {
    this.router.navigate(['/frontoffice/client/contracts/create']);
  }

  goToDetail(id: number): void {
    this.router.navigate(['/frontoffice/client/contracts', id]);
  }

  deleteContract(id: number, event: Event): void {
    event.stopPropagation();
    if (confirm('Delete this contract?')) {
      this.contractService.deleteContract(id).subscribe({
        next: () => this.loadContracts(),
        error: () => this.error = 'Error deleting contract'
      });
    }
  }

  getTypeLabel(type: ContractType): string {
    const labels: Record<ContractType, string> = {
      [ContractType.STANDARD]: 'Standard',
      [ContractType.PROFESSIONAL]: 'Professional',
      [ContractType.PREMIUM]: 'Premium'
    };
    return labels[type] || String(type);
  }
}