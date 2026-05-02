import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { OrganizationService } from '../../../core/services/organization.service';
import {
  CreateOrganizationRequest, OrganizationType, OrganizationSize
} from '../../../core/models/organization.model';

@Component({
  selector: 'app-create-organization',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './create-organization.component.html',
  styleUrls: ['./create-organization.component.scss']
})
export class CreateOrganizationComponent {
  private orgService = inject(OrganizationService);
  private router     = inject(Router);

  // ── Type tiles ────────────────────────────────────────────
  readonly orgTypes = [
    { value: OrganizationType.AGENCY,        label: 'Agency',       icon: 'business' },
    { value: OrganizationType.STARTUP,       label: 'Startup',      icon: 'rocket_launch' },
    { value: OrganizationType.ASSOCIATION,   label: 'Association',  icon: 'volunteer_activism' },
    { value: OrganizationType.FREELANCE_COOP, label: 'Cooperative', icon: 'groups' },
  ];

  // ── Size buttons ──────────────────────────────────────────
  readonly orgSizes = [
    { value: OrganizationSize.SOLO,   label: 'Solo',      icon: 'person' },
    { value: OrganizationSize.SMALL,  label: '2–10',      icon: 'group' },
    { value: OrganizationSize.MEDIUM, label: '11–50',     icon: 'groups' },
    { value: OrganizationSize.LARGE,  label: '50+',       icon: 'corporate_fare' },
  ];

  // ── Form model ────────────────────────────────────────────
  form: CreateOrganizationRequest = {
    name: '', description: '', logoUrl: '', website: '',
    type: OrganizationType.AGENCY, specialties: [], location: '', siret: '',
    size: OrganizationSize.SMALL
  };

  isSubmitting = signal(false);
  error        = signal<string | null>(null);
  success      = signal(false);

  // ── Computed helpers ──────────────────────────────────────
  get descPct(): number {
    return Math.min(((this.form.description?.length || 0) / 2000) * 100, 100);
  }

  get checklistPct(): number {
    const checks = [
      !!this.form.type,
      (this.form.name?.length || 0) >= 2,
      (this.form.description?.length || 0) >= 10,
      (this.form.specialties?.length || 0) > 0,
    ];
    return Math.round((checks.filter(Boolean).length / checks.length) * 100);
  }

  // ── Specialty helpers ─────────────────────────────────────
  addSpecialtyRaw(input: HTMLInputElement): void {
    const value = input.value.replace(/,$/, '').trim();
    if (value && !(this.form.specialties ?? []).includes(value)) {
      this.form.specialties = [...(this.form.specialties ?? []), value];
    }
    input.value = '';
  }

  removeSpecialty(s: string): void {
    this.form.specialties = (this.form.specialties ?? []).filter(x => x !== s);
  }

  // ── Submit ────────────────────────────────────────────────
  submit(): void {
    const payload: CreateOrganizationRequest = {
      ...this.form,
      name: this.form.name?.trim() ?? '',
      description: this.emptyToUndefined(this.form.description),
      logoUrl: this.emptyToUndefined(this.form.logoUrl),
      website: this.emptyToUndefined(this.form.website),
      location: this.emptyToUndefined(this.form.location),
      siret: this.emptyToUndefined(this.form.siret),
      specialties: (this.form.specialties ?? []).map(s => s.trim()).filter(Boolean)
    };

    if (!payload.name || !payload.type) {
      this.error.set('Name and type are required.');
      return;
    }
    if (payload.name.length > 100) {
      this.error.set('Organization name must be 100 characters or less.');
      return;
    }
    this.isSubmitting.set(true);
    this.error.set(null);
    this.orgService.isNameAvailable(payload.name).subscribe({
      next: available => {
        if (!available) {
          this.error.set('An organization with this name already exists. Please choose another name.');
          this.isSubmitting.set(false);
          return;
        }
        this.orgService.create(payload).subscribe({
          next: org => {
            this.success.set(true);
            this.isSubmitting.set(false);
            setTimeout(() => this.router.navigate(['/frontoffice/my-organizations', org.id, 'settings']), 1500);
          },
          error: err => {
            this.error.set(err?.error?.message ?? 'Failed to create organization.');
            this.isSubmitting.set(false);
          }
        });
      },
      error: err => {
        this.error.set(err?.error?.message ?? 'Unable to verify organization name.');
        this.isSubmitting.set(false);
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/frontoffice/my-organizations']);
  }

  private emptyToUndefined(value?: string): string | undefined {
    const trimmed = value?.trim();
    return trimmed ? trimmed : undefined;
  }
}
