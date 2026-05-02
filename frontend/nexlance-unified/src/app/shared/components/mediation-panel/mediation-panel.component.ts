import { Component, Input, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { ComplaintAdvancedService } from '../../../core/services/complaint-advanced.service';
import { AuthService } from '../../../core/services/auth.service';
import { MediationSession, SubmitEvidenceRequest, MediationDecisionRequest } from '../../../core/models/complaint-advanced.model';

@Component({
  selector: 'app-mediation-panel',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatProgressSpinnerModule,
    MatDividerModule, MatChipsModule
  ],
  templateUrl: './mediation-panel.component.html'
})
export class MediationPanelComponent implements OnInit {
  @Input() complaintId!: string;
  @Input() isAdmin = false;

  private advService  = inject(ComplaintAdvancedService);
  private authService = inject(AuthService);

  session   = signal<MediationSession | null>(null);
  isLoading = signal(true);
  error     = signal<string | null>(null);

  // Evidence form
  evidenceDesc  = signal('');
  evidenceFiles = signal('');
  submitting    = signal(false);

  // Decision form (admin)
  decisionOutcome  = signal('');
  decisionType     = signal('NO_ACTION');
  deciding         = signal(false);

  readonly resolutionTypes = ['NO_ACTION','REFUND','WARNING','ACCOUNT_SUSPENSION','MEDIATION'];

  get userId(): string { return this.authService.getCurrentUser()?.id ?? ''; }

  ngOnInit() {
    this.advService.getMediation(this.complaintId).subscribe({
      next:  s  => { this.session.set(s);  this.isLoading.set(false); },
      error: () => this.isLoading.set(false)
    });
  }

  canSubmitEvidence(): boolean {
    const s = this.session();
    return !!s && (s.status === 'OPEN' || s.status === 'EVIDENCE_PHASE');
  }

  submitEvidence() {
    const s = this.session();
    if (!s || !this.evidenceDesc().trim()) return;
    this.submitting.set(true);
    const req: SubmitEvidenceRequest = {
      description: this.evidenceDesc(),
      fileUrls: this.evidenceFiles() ? this.evidenceFiles().split('\n').map(u => u.trim()).filter(Boolean) : []
    };
    this.advService.submitEvidence(s.id, req).subscribe({
      next: updated => {
        this.session.set(updated);
        this.evidenceDesc.set('');
        this.evidenceFiles.set('');
        this.submitting.set(false);
      },
      error: () => this.submitting.set(false)
    });
  }

  decide() {
    const s = this.session();
    if (!s || !this.decisionOutcome().trim()) return;
    this.deciding.set(true);
    const req: MediationDecisionRequest = {
      outcome: this.decisionOutcome(),
      resolutionType: this.decisionType()
    };
    this.advService.decideMediation(s.id, req).subscribe({
      next: updated => { this.session.set(updated); this.deciding.set(false); },
      error: () => this.deciding.set(false)
    });
  }

  statusLabel(s: string): string {
    return { OPEN: 'Open', EVIDENCE_PHASE: 'Evidence Submission', DELIBERATION: 'Deliberation', CLOSED: 'Closed' }[s] ?? s;
  }

  statusColor(s: string): string {
    return { OPEN: '#1976d2', EVIDENCE_PHASE: '#f57f17', DELIBERATION: '#6a1b9a', CLOSED: '#2e7d32' }[s] ?? '#999';
  }
}
