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
import { SignaturePadComponent } from '../../../../shared/components/signature-pad/signature-pad.component';

@Component({
  selector: 'app-client-contract-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, ContractStatusBadgeComponent, SignaturePadComponent],
  templateUrl: './contract.html',
  styleUrls: ['./contract.css']
})
export class ClientContractDetailComponent implements OnInit {
  @ViewChild('signPad') signPad!: SignaturePadComponent;

  private contractService = inject(ContractService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  contract: Contract | null = null;
  milestones: Milestone[] = [];
  payments: PaymentSchedule[] = [];
  loading = false;
  error = '';

  ContractStatus = ContractStatus;

  // Conversion
  currencies = ['EUR', 'USD', 'GBP', 'TND', 'MAD', 'CAD', 'CHF', 'JPY'];
  selectedCurrency = 'USD';
  conversionResult: { convertedAmount: number; rate: number; to: string; date: string } | null = null;
  converting = false;

  // Signature
  showSignModal = false;
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

  loadContract(id: number): void {
    this.loading = true;
    this.contractService.getContractById(id).subscribe({
      next: (data) => {
        this.contract = data;
        this.milestones = data.milestones || [];
        this.payments = data.paymentSchedules || [];
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
    this.contractService.getClientSignatureCode(id).subscribe({
      next: (res) => {
        if (res.code) {
          this.savedSignatureCode = res.code;
        }
      },
      error: () => {} // ignore - code not available yet
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

    // Background
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // Signature text
    ctx.font = 'italic bold 36px "Segoe Script", "Brush Script MT", "Comic Sans MS", cursive';
    ctx.fillStyle = '#1a1a2e';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(code.toUpperCase(), canvas.width / 2, canvas.height / 2 - 5);

    // Underline flourish
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

    this.contractService.signByClient(this.contract.id, imageToSend).subscribe({
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

  get canSign(): boolean {
    if (this.signMode === 'code') return !!this.signatureCode;
    return !!this.signatureDataUrl;
  }

  sendToFreelancer(): void {
    if (!this.contract?.id) return;
    this.contractService.sendToFreelancer(this.contract.id).subscribe({
      next: (updated) => this.contract = updated,
      error: () => alert('Failed to send contract.')
    });
  }

  activateContract(): void {
    if (!this.contract?.id) return;
    this.contractService.activateContract(this.contract.id).subscribe({
      next: (updated) => this.contract = updated,
      error: () => alert('Failed to activate contract.')
    });
  }

  completeContract(): void {
    if (!this.contract?.id || !confirm('Mark this contract as completed?')) return;
    this.contractService.completeContract(this.contract.id).subscribe({
      next: (updated) => this.contract = updated,
      error: () => alert('Failed to complete contract.')
    });
  }

  cancelContract(): void {
    if (!this.contract?.id || !confirm('Are you sure you want to cancel this contract?')) return;
    this.contractService.cancelContract(this.contract.id).subscribe({
      next: (updated) => this.contract = updated,
      error: () => alert('Failed to cancel contract.')
    });
  }

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
      error: () => alert('Failed to generate PDF.')
    });
  }

  convertAmount(): void {
    if (!this.contract?.id) return;
    this.converting = true;
    this.conversionResult = null;

    this.contractService.convertAmount(this.contract.id, this.selectedCurrency, false)
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
    this.router.navigate(['/frontoffice/client/contracts']);
  }
}