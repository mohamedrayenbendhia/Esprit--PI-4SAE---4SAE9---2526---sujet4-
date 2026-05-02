import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { OrganizationService } from '../../../core/services/organization.service';
import { CreateCollabOfferRequest } from '../../../core/models/organization.model';

@Component({
  selector: 'app-create-collab-offer',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatIconModule, MatButtonModule,
    MatFormFieldModule, MatInputModule, MatChipsModule
  ],
  templateUrl: './create-collab-offer.component.html',
  styleUrls: ['./create-collab-offer.component.scss']
})
export class CreateCollabOfferComponent {
  private orgService  = inject(OrganizationService);
  private route       = inject(ActivatedRoute);
  private router      = inject(Router);

  get orgId(): string { return this.route.snapshot.paramMap.get('orgId')!; }

  // ── Formulaire ─────────────────────────────────────────────────────────────
  form: CreateCollabOfferRequest = {
    title: '',
    description: '',
    requiredSkills: [],
    durationLabel: '',
    budgetEstimate: undefined,
    maxApplicants: undefined,
    deadlineDate: undefined
  };

  skillInput  = '';
  isSubmitting = signal(false);
  error        = signal<string | null>(null);
  success      = signal(false);

  // ── Checklist de complétion ────────────────────────────────────────────────
  get completionPct(): number {
    const checks = [
      (this.form.title?.length ?? 0) >= 5,
      (this.form.description?.length ?? 0) >= 20,
      (this.form.requiredSkills?.length ?? 0) > 0,
    ];
    return Math.round((checks.filter(Boolean).length / checks.length) * 100);
  }

  // ── Compétences ───────────────────────────────────────────────────────────
  addSkill() {
    const s = this.skillInput.trim();
    if (s && !(this.form.requiredSkills ?? []).includes(s)) {
      this.form.requiredSkills = [...(this.form.requiredSkills ?? []), s];
    }
    this.skillInput = '';
  }

  removeSkill(skill: string) {
    this.form.requiredSkills = (this.form.requiredSkills ?? []).filter(s => s !== skill);
  }

  // ── Soumission ─────────────────────────────────────────────────────────────
  submit() {
    if (!this.form.title?.trim() || !this.form.description?.trim()) {
      this.error.set('Title and description are required.');
      return;
    }
    this.isSubmitting.set(true);
    this.error.set(null);

    const payload: CreateCollabOfferRequest = {
      ...this.form,
      budgetEstimate: this.form.budgetEstimate || undefined,
      maxApplicants: this.form.maxApplicants || undefined,
      durationLabel: this.form.durationLabel?.trim() || undefined,
      deadlineDate: this.form.deadlineDate || undefined
    };

    this.orgService.createCollabOffer(this.orgId, payload).subscribe({
      next: () => {
        this.success.set(true);
        this.isSubmitting.set(false);
        setTimeout(() => this.router.navigate(['/frontoffice/organizations', this.orgId]), 1500);
      },
      error: err => {
        this.error.set(err?.error?.message ?? 'Error creating the offer.');
        this.isSubmitting.set(false);
      }
    });
  }

  cancel() {
    this.router.navigate(['/frontoffice/organizations', this.orgId]);
  }
}
