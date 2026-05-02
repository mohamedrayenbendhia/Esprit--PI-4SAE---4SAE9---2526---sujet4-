import { Component, OnInit, inject, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ContractService } from '../../../../core/services/contract.service';
import { AuthService } from '../../../../core/services/auth.service';
import {
  Contract,
  ContractStatus,
  Milestone,
  MilestoneStatus,
  PaymentSchedule,
  PaymentStatus,
  CustomClause
} from '../../../../shared/models/contract.model';
import { ContractStatusBadgeComponent } from '../../../../shared/components/contract-status-badge/contract';
import { MilestoneTrackerComponent } from '../../../../shared/components/milestone-tracker/contract';
import { SignaturePadComponent } from '../../../../shared/components/signature-pad/signature-pad.component';

@Component({
  selector: 'app-contract-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, ContractStatusBadgeComponent, SignaturePadComponent],
  templateUrl: './contract.html',
  styleUrls: ['./contract.css']
})
export class ContractDetailComponent implements OnInit {
  @ViewChild('signPad') signPad!: SignaturePadComponent;

  private contractService = inject(ContractService);
  private authService = inject(AuthService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  contract: Contract | null = null;
  milestones: Milestone[] = [];
  payments: PaymentSchedule[] = [];
  clauses: CustomClause[] = [];

  loading = false;
  error = '';
  activeTab = 'overview';

  ContractStatus = ContractStatus;
  MilestoneStatus = MilestoneStatus;
  PaymentStatus = PaymentStatus;

  showSignModal = false;
  showDeclineModal = false;
  showModificationModal = false;
  declineReason = '';
  modificationReason = '';

  // Conversion
  currencies = ['EUR', 'USD', 'GBP', 'TND', 'MAD', 'CAD', 'CHF', 'JPY'];
  selectedCurrency = 'USD';
  conversionResult: { convertedAmount: number; rate: number; to: string; date: string } | null = null;
  converting = false;

  // Signature
  signMode: 'draw' | 'code' = 'draw';
  signatureDataUrl: string | null = null;
  signatureCode = '';
  codePreviewUrl: string | null = null;
  savedSignatureCode: string | null = null;
  showSavedCode = false;
  signing = false;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loadContract(id);
  }

  get freelancerId(): string {
    return this.authService.getCurrentUser()?.id || '';
  }

  loadContract(id: number): void {
    this.loading = true;
    this.contractService.getContractByIdAsFreelancer(id).subscribe({
      next: (data) => {
        this.contract = data;
        this.milestones = data.milestones || [];
        this.payments = data.paymentSchedules || [];
        this.clauses = data.customClauses || [];
        this.loading = false;
        this.loadSignatureCode(id);
      },
      error: () => {
        this.error = 'Contract not found';
        this.loading = false;
      }
    });
  }

  loadSignatureCode(id: number): void {
    this.contractService.getFreelancerSignatureCode(id).subscribe({
      next: (res) => {
        if (res.code) this.savedSignatureCode = res.code;
      },
      error: () => {}
    });
  }

  openSignModal(): void {
    this.signMode = this.savedSignatureCode ? 'code' : 'draw';
    this.signatureDataUrl = null;
    this.signatureCode = '';
    this.showSignModal = true;
  }

  onSignatureDrawn(dataUrl: string | null): void {
    this.signatureDataUrl = dataUrl;
  }

  onCodeInput(): void {
    if (this.signatureCode.trim()) {
      this.codePreviewUrl = this.generateSignatureImage(this.signatureCode);
    } else {
      this.codePreviewUrl = null;
    }
  }

  generateSignatureImage(code: string): string {
    const canvas = document.createElement('canvas');
    canvas.width = 420;
    canvas.height = 140;
    const ctx = canvas.getContext('2d')!;
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    ctx.font = 'italic bold 36px "Segoe Script", "Brush Script MT", "Comic Sans MS", cursive';
    ctx.fillStyle = '#1a1a2e';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(code.toUpperCase(), canvas.width / 2, canvas.height / 2 - 5);

    const textWidth = ctx.measureText(code.toUpperCase()).width;
    const startX = (canvas.width - textWidth) / 2 - 15;
    const endX = (canvas.width + textWidth) / 2 + 15;
    const y = canvas.height / 2 + 28;
    ctx.beginPath();
    ctx.moveTo(startX, y);
    ctx.quadraticCurveTo(canvas.width / 2, y + 8, endX, y - 2);
    ctx.strokeStyle = '#1a1a2e';
    ctx.lineWidth = 2;
    ctx.stroke();

    return canvas.toDataURL('image/png');
  }

  get canSign(): boolean {
    if (this.signMode === 'code') return !!this.signatureCode;
    return !!this.signatureDataUrl;
  }

  // ===== MÉTHODE PDF =====
  downloadPdf(): void {
    if (!this.contract?.id) return;
    
    this.contractService.generatePdf(this.contract.id).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `contract-${this.contract?.contractNumber || this.contract?.id}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Error generating PDF:', err);
        alert('Failed to generate PDF. Please try again.');
      }
    });
  }

  signContract(): void {
    if (!this.contract?.id || this.signing) return;
    this.signing = true;

    let imageToSend: string;
    if (this.signMode === 'code' && this.signatureCode) {
      imageToSend = this.generateSignatureImage(this.signatureCode);
    } else if (this.signatureDataUrl) {
      imageToSend = this.signatureDataUrl;
    } else {
      this.signing = false;
      return;
    }

    this.contractService.signByFreelancer(this.contract.id, imageToSend).subscribe({
      next: (updated) => {
        this.contract = updated;
        this.showSignModal = false;
        this.signing = false;
        this.loadSignatureCode(this.contract!.id!);
        this.showSavedCode = true;
      },
      error: () => {
        this.signing = false;
        alert('Failed to sign contract.');
      }
    });
  }

  declineContract(): void {
    if (!this.contract?.id || !this.declineReason) return;
    this.contractService.declineContract(this.contract.id, this.declineReason).subscribe({
      next: (updated) => {
        this.contract = updated;
        this.showDeclineModal = false;
        this.declineReason = '';
      }
    });
  }

  requestModification(): void {
    if (!this.contract?.id || !this.modificationReason) return;
    this.contractService.requestModification(this.contract.id, this.modificationReason).subscribe({
      next: (updated) => {
        this.contract = updated;
        this.showModificationModal = false;
        this.modificationReason = '';
      }
    });
  }

  startMilestone(milestoneId: number): void {
    this.contractService.startMilestone(milestoneId).subscribe({
      next: (updated) => {
        const index = this.milestones.findIndex(m => m.id === updated.id);
        if (index !== -1) this.milestones[index] = updated;
      }
    });
  }

  submitForValidation(milestoneId: number): void {
    if (!confirm('Submit this milestone for validation?')) return;
    this.contractService.submitMilestoneForValidation(milestoneId).subscribe({
      next: (updated) => {
        const index = this.milestones.findIndex(m => m.id === updated.id);
        if (index !== -1) this.milestones[index] = updated;
      }
    });
  }

  getMilestoneStatusLabel(status: MilestoneStatus): string {
    const labels: Record<MilestoneStatus, string> = {
      [MilestoneStatus.PENDING]: 'Pending',
      [MilestoneStatus.IN_PROGRESS]: 'In Progress',
      [MilestoneStatus.AWAITING_VALIDATION]: 'Awaiting Validation',
      [MilestoneStatus.VALIDATED]: 'Validated',
      [MilestoneStatus.REJECTED]: 'Rejected'
    };
    return labels[status] || status;
  }

  getPaymentStatusLabel(status: PaymentStatus): string {
    const labels: Record<PaymentStatus, string> = {
      [PaymentStatus.PENDING]: 'Pending',
      [PaymentStatus.PAID]: 'Paid',
      [PaymentStatus.OVERDUE]: 'Overdue'
    };
    return labels[status] || status;
  }

  canStartMilestone(m: Milestone): boolean {
    return m.status === MilestoneStatus.PENDING;
  }

  canSubmitMilestone(m: Milestone): boolean {
    return m.status === MilestoneStatus.IN_PROGRESS;
  }

  isPendingSignature(): boolean {
    return this.contract?.status === ContractStatus.PENDING_FREELANCER_SIGNATURE;
  }

  convertAmount(): void {
    if (!this.contract?.id) return;
    this.converting = true;
    this.conversionResult = null;

    this.contractService.convertAmount(this.contract.id, this.selectedCurrency, true)
      .subscribe({
        next: (res) => {
          this.conversionResult = res;
          this.converting = false;
        },
        error: () => {
          this.converting = false;
          alert('Conversion failed. Please try again.');
        }
      });
  }

  goBack(): void {
    this.router.navigate(['/frontoffice/freelancer/contracts']);
  }
}