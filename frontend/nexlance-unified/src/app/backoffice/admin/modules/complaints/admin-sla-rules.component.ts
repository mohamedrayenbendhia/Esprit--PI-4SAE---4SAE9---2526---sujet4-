import { Component, signal, computed, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ComplaintAdvancedService } from '../../../../core/services/complaint-advanced.service';
import { SlaRule, CreateSlaRuleRequest } from '../../../../core/models/complaint-advanced.model';

@Component({
  selector: 'app-admin-sla-rules',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatCheckboxModule, MatSlideToggleModule, MatProgressSpinnerModule],
  templateUrl: './admin-sla-rules.component.html',
  styleUrls: ['./admin-sla-rules.component.scss']
})
export class AdminSlaRulesComponent implements OnInit {
  private advService = inject(ComplaintAdvancedService);

  rules     = signal<SlaRule[]>([]);
  isLoading = signal(true);
  editingId = signal<string | null>(null);
  saving    = signal(false);
  showForm  = signal(false);

  readonly ALL_PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
  readonly MAX_RULES = 4;

  /** Vrai si on peut encore créer une règle (moins de 4 priorités configurées) */
  canCreate = computed(() => this.rules().length < this.MAX_RULES);

  /** Priorités pas encore configurées (pour le select du formulaire de création) */
  availablePriorities = computed(() => {
    const used = new Set(this.rules().map(r => r.priority));
    return this.ALL_PRIORITIES.filter(p => !used.has(p));
  });

  form: CreateSlaRuleRequest = {
    priority: 'LOW',
    maxFirstResponseHours: 8,
    maxResolutionHours: 48,
    warningThresholdHours: 4,
    autoEscalate: false
  };

  ngOnInit() { this.load(); }

  load() {
    this.isLoading.set(true);
    this.advService.getSlaRules().subscribe({
      next:  r  => { this.rules.set(r); this.isLoading.set(false); },
      error: () => this.isLoading.set(false)
    });
  }

  openCreate() {
    if (!this.canCreate()) return;
    this.editingId.set(null);
    const firstAvailable = this.availablePriorities()[0] ?? 'LOW';
    this.form = {
      priority: firstAvailable,
      maxFirstResponseHours: 8,
      maxResolutionHours: 48,
      warningThresholdHours: 4,
      autoEscalate: false
    };
    this.showForm.set(true);
    setTimeout(() => document.querySelector('.form-section')?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 50);
  }

  openEdit(rule: SlaRule) {
    this.editingId.set(rule.id);
    this.form = {
      priority:              rule.priority,
      maxFirstResponseHours: rule.maxFirstResponseHours,
      maxResolutionHours:    rule.maxResolutionHours,
      warningThresholdHours: rule.warningThresholdHours,
      autoEscalate:          rule.autoEscalate
    };
    this.showForm.set(true);
    setTimeout(() => document.querySelector('.form-section')?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 50);
  }

  save() {
    this.saving.set(true);
    const id = this.editingId();
    const obs = id
      ? this.advService.updateSlaRule(id, this.form)
      : this.advService.createSlaRule(this.form);

    obs.subscribe({
      next: saved => {
        if (id) {
          this.rules.update(list => list.map(r => r.id === id ? saved : r));
        } else {
          this.rules.update(list => [...list, saved]);
        }
        this.showForm.set(false);
        this.saving.set(false);
      },
      error: () => this.saving.set(false)
    });
  }

  delete(rule: SlaRule) {
    if (!confirm(`Supprimer la règle SLA pour la priorité ${rule.priority} ?`)) return;
    this.advService.deleteSlaRule(rule.id).subscribe({
      next: () => this.rules.update(list => list.filter(r => r.id !== rule.id))
    });
  }

  cancel() { this.showForm.set(false); }

  priorityMeta(p: string): { color: string; bg: string; icon: string; label: string } {
    const map: Record<string, { color: string; bg: string; icon: string; label: string }> = {
      LOW:      { color: '#16a34a', bg: '#f0fdf4', icon: 'south',        label: 'Low' },
      MEDIUM:   { color: '#d97706', bg: '#fffbeb', icon: 'remove',       label: 'Medium' },
      HIGH:     { color: '#dc2626', bg: '#fef2f2', icon: 'north',        label: 'High' },
      CRITICAL: { color: '#7c3aed', bg: '#f5f3ff', icon: 'priority_high', label: 'Critical' },
    };
    return map[p] ?? { color: '#64748b', bg: '#f8fafc', icon: 'help', label: p };
  }

  /** Priorités déjà configurées, dans l'ordre défini */
  orderedRules = computed(() =>
    this.ALL_PRIORITIES
      .map(p => this.rules().find(r => r.priority === p))
      .filter((r): r is SlaRule => !!r)
  );
}
