import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ContractService } from '../../../../core/services/contract.service';
import { AuthService } from '../../../../core/services/auth.service';
import { Contract, ContractStatus, ContractType, FreelancerEarnings } from '../../../../shared/models/contract.model';
import { ContractStatusBadgeComponent } from '../../../../shared/components/contract-status-badge/contract';

@Component({
  selector: 'app-contract-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ContractStatusBadgeComponent],
  templateUrl: './contract.html',
  styleUrls: ['./contract.css']
})
export class ContractListComponent implements OnInit {
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
  statuses = Object.values(ContractStatus);

  stats = {
    totalContracts: 0,
    pendingSignature: 0,
    activeContracts: 0,
    completedContracts: 0
  };

  earnings: FreelancerEarnings = {
    totalEarnings: 0,
    pendingEarnings: 0,
    completedContracts: 0
  };

  ngOnInit(): void {
    this.loadContracts();
    this.loadStats();
    this.loadEarnings();
  }

  get freelancerId(): string {
    return this.authService.getCurrentUser()?.id || '';
  }

  loadContracts(): void {
    this.loading = true;
    this.contractService.getMyContractsAsFreelancer(this.freelancerId).subscribe({
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
    this.contractService.getFreelancerStatistics(this.freelancerId).subscribe({
      next: (data) => this.stats = data as any
    });
  }

  loadEarnings(): void {
    this.contractService.getFreelancerEarnings(this.freelancerId).subscribe({
      next: (data) => this.earnings = data
    });
  }

  applyFilters(): void {
    this.contractService.searchContractsAsFreelancer(
      this.freelancerId,
      this.selectedStatus as ContractStatus || undefined,
      this.searchKeyword || undefined
    ).subscribe({
      next: (data) => this.filteredContracts = data
    });
  }

  resetFilters(): void {
    this.selectedStatus = '';
    this.searchKeyword = '';
    this.filteredContracts = this.contracts;
  }

  goToDetail(id: number): void {
    this.router.navigate(['/frontoffice/freelancer/contracts', id]);
  }

  getTypeLabel(type: ContractType): string {
    const labels = {
      [ContractType.STANDARD]: 'Standard',
      [ContractType.PROFESSIONAL]: 'Professional',
      [ContractType.PREMIUM]: 'Premium'
    };
    return labels[type] || type;
  }

  hasPendingAction(contract: Contract): boolean {
    return contract.status === ContractStatus.PENDING_FREELANCER_SIGNATURE;
  }
}