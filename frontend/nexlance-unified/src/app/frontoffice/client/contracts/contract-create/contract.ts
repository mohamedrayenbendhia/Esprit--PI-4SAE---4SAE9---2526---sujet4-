import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ContractService } from '@core/services/contract.service';
import { AuthService } from '@core/services/auth.service';
import { UserService } from '@core/services/user.service';
import { UserType } from '@shared/models/user.model';
import {
  Contract,
  ContractType,
  Currency,
  PaymentMethod,
  ApplicableLaw,
  Milestone,
  PaymentSchedule
} from '@shared/models/contract.model';

@Component({
  selector: 'app-contract-create',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './contract.html',
  styleUrls: ['./contract.css']
})
export class ContractCreateComponent implements OnInit {
  private contractService = inject(ContractService);
  private authService = inject(AuthService);
  private userService = inject(UserService);
  private router = inject(Router);

  loading = false;
  error = '';
  currentStep = 1;
  totalSteps = 4;

  // Freelancer fields entered by client
  freelancerEmail = '';
  freelancerCin = '';
  freelancerLookupError = '';
  freelancerResolved = false;
  freelancerLookupLoading = false;

  ContractType = ContractType;
  Currency = Currency;
  PaymentMethod = PaymentMethod;
  ApplicableLaw = ApplicableLaw;

  contract: Contract = {
    contractType: ContractType.STANDARD,
    clientId: '',
    freelancerId: '',
    missionTitle: '',
    totalAmount: 0,
    currency: Currency.EUR,
    paymentMethod: PaymentMethod.BANK_TRANSFER,
    applicableLaw: ApplicableLaw.FRENCH,
    confidentialityYears: 2,
    ipTransferToClient: true,
    portfolioAllowed: false,
    
    // Client fields
    clientName: '',
    clientEmail: '',
    clientPhone: '',
    clientCompany: '',
    clientAddress: '',
    clientCountry: '',
    clientSiret: '',
    
    // Freelancer fields
    freelancerName: '',
    freelancerEmail: '',
    freelancerCin: '',
    freelancerPhone: '',
    freelancerAddress: '',
    freelancerCountry: '',
    freelancerSiret: '',
    freelancerSpecialty: ''
  };

  newMilestone: Milestone = { title: '', dueDate: '', description: '', linkedPaymentAmount: undefined };
  newPayment: PaymentSchedule = { label: '', amount: 0, dueDate: '', percentage: undefined };
  milestones: Milestone[] = [];
  payments: PaymentSchedule[] = [];

  ngOnInit(): void {
    const currentUser = this.authService.getCurrentUser();
    this.contract.clientId = currentUser?.id || '';
    
    // Pré-remplir avec les infos du client si disponibles
    if (currentUser) {
      // Nom complet
      if (currentUser.firstName || currentUser.lastName) {
        this.contract.clientName = `${currentUser.firstName || ''} ${currentUser.lastName || ''}`.trim();
      }
      this.contract.clientEmail = currentUser.email || '';
    }
  }

  // ===== VALIDATIONS =====
  get cinInvalid(): boolean {
    return !!this.freelancerCin && !/^[0-9]{8}$/.test(this.freelancerCin);
  }

  get cinValid(): boolean {
    return /^[0-9]{8}$/.test(this.freelancerCin);
  }

  get emailValid(): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.freelancerEmail);
  }

  get clientInfoValid(): boolean {
    return !!(this.contract.clientName && this.contract.clientEmail);
  }

  // ===== FREELANCER RESOLUTION =====
  resolveFreelancer(): void {
    if (!this.emailValid || !this.cinValid) return;

    this.freelancerLookupLoading = true;
    this.freelancerLookupError = '';
    this.freelancerResolved = false;

    this.userService.getAllUsers({
      searchTerm: this.freelancerEmail,
      type: UserType.FREELANCE
    }).subscribe({
      next: (response) => {
        const freelancer = response.content.find(
          (u: any) => u.email?.toLowerCase() === this.freelancerEmail.toLowerCase()
        );

        if (!freelancer) {
          this.freelancerLookupError = 'No freelancer found with this email.';
          this.freelancerLookupLoading = false;
          return;
        }

        // Auto-fill freelancer information
        this.contract.freelancerId = freelancer.id;
        this.contract.freelancerEmail = freelancer.email;
        
        // Nom complet du freelancer
        if (freelancer.firstName || freelancer.lastName) {
          this.contract.freelancerName = `${freelancer.firstName || ''} ${freelancer.lastName || ''}`.trim();
        }
        
        this.contract.freelancerCin = this.freelancerCin;
        
        // Champs optionnels (peuvent être remplis plus tard)
        this.contract.freelancerPhone = '';
        this.contract.freelancerAddress = '';
        this.contract.freelancerCountry = '';
        this.contract.freelancerSiret = '';
        this.contract.freelancerSpecialty = '';

        this.freelancerResolved = true;
        this.freelancerLookupLoading = false;
      },
      error: (err) => {
        console.error('Error searching freelancer:', err);
        this.freelancerLookupError = 'Error searching for freelancer.';
        this.freelancerLookupLoading = false;
      }
    });
  }

  // ===== NAVIGATION =====
  get stepTitle(): string {
    const titles = ['General Information', 'Financial Details', 'Milestones & Payments', 'Legal Clauses'];
    return titles[this.currentStep - 1];
  }

  nextStep(): void {
    if (this.currentStep === 1) {
      if (!this.clientInfoValid) {
        this.error = 'Please fill your name and email.';
        return;
      }
      if (!this.freelancerResolved) {
        this.error = 'Please resolve the freelancer by email.';
        return;
      }
    }
    this.error = '';
    if (this.currentStep < this.totalSteps) this.currentStep++;
  }

  prevStep(): void {
    if (this.currentStep > 1) this.currentStep--;
  }

  addMilestone(): void {
    if (!this.newMilestone.title || !this.newMilestone.dueDate) return;
    this.milestones.push({ ...this.newMilestone });
    this.newMilestone = { title: '', dueDate: '', description: '', linkedPaymentAmount: undefined };
  }

  removeMilestone(index: number): void {
    this.milestones.splice(index, 1);
  }

  addPayment(): void {
    if (!this.newPayment.label || !this.newPayment.amount || !this.newPayment.dueDate) return;
    this.payments.push({ ...this.newPayment });
    this.newPayment = { label: '', amount: 0, dueDate: '', percentage: undefined };
  }

  removePayment(index: number): void {
    this.payments.splice(index, 1);
  }

  submit(): void {
    // Validation finale
    if (!this.contract.clientName || !this.contract.clientEmail) {
      this.error = 'Please fill your information.';
      return;
    }

    if (!this.contract.freelancerId) {
      this.error = 'Freelancer not resolved. Please check email and CIN.';
      return;
    }

    if (!this.contract.missionTitle) {
      this.error = 'Please enter a mission title.';
      return;
    }

    if (this.contract.totalAmount <= 0) {
      this.error = 'Please enter a valid total amount.';
      return;
    }

    this.loading = true;
    this.error = '';

    this.contractService.createContract(this.contract).subscribe({
      next: (created) => {
        const contractId = created.id!;
        const requests = [
          ...this.milestones.map(m => this.contractService.addMilestone(contractId, m).toPromise()),
          ...this.payments.map(p => this.contractService.addPayment(contractId, p).toPromise())
        ];
        Promise.all(requests).then(() => {
          this.loading = false;
          this.router.navigate(['/frontoffice/client/contracts', contractId]);
        }).catch(err => {
          console.error('Error adding milestones/payments:', err);
          this.error = 'Contract created but error adding details.';
          this.loading = false;
        });
      },
      error: (err) => {
        console.error('Backend error:', err);
        this.error = err.error?.message || 'Error creating contract';
        this.loading = false;
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/frontoffice/client/contracts']);
  }
}