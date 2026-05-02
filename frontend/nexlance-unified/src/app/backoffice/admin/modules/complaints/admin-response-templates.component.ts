import { Component, signal, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { ComplaintAdvancedService } from '../../../../core/services/complaint-advanced.service';
import { ResponseTemplate, CreateTemplateRequest } from '../../../../core/models/complaint-advanced.model';

@Component({
  selector: 'app-admin-response-templates',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './admin-response-templates.component.html',
  styleUrls: ['./admin-response-templates.component.scss']
})
export class AdminResponseTemplatesComponent implements OnInit {
  private advService = inject(ComplaintAdvancedService);

  templates  = signal<ResponseTemplate[]>([]);
  isLoading  = signal(true);
  editingId  = signal<string | null>(null);
  showForm   = signal(false);
  saving     = signal(false);
  filter     = signal('');

  readonly categories = ['PAYMENT_ISSUE','QUALITY_DISPUTE','COMMUNICATION_PROBLEM',
                         'HARASSMENT','SCAM','TECHNICAL_ISSUE','OTHER'];

  form: CreateTemplateRequest = { title: '', content: '', category: '' };

  filtered = () => {
    const q = this.filter().toLowerCase();
    return this.templates().filter(t =>
      !q || t.title.toLowerCase().includes(q) || t.content.toLowerCase().includes(q)
    );
  };

  ngOnInit() { this.load(); }

  load() {
    this.isLoading.set(true);
    this.advService.getAllTemplates().subscribe({
      next:  list => { this.templates.set(list); this.isLoading.set(false); },
      error: ()   => this.isLoading.set(false)
    });
  }

  openCreate() {
    this.editingId.set(null);
    this.form = { title: '', content: '', category: '' };
    this.showForm.set(true);
  }

  openEdit(t: ResponseTemplate) {
    this.editingId.set(t.id);
    this.form = { title: t.title, content: t.content, category: t.category ?? '' };
    this.showForm.set(true);
  }

  save() {
    if (!this.form.title.trim() || !this.form.content.trim()) return;
    this.saving.set(true);
    const id = this.editingId();
    const obs = id
      ? this.advService.updateTemplate(id, this.form)
      : this.advService.createTemplate(this.form);

    obs.subscribe({
      next: saved => {
        if (id) this.templates.update(list => list.map(t => t.id === id ? saved : t));
        else    this.templates.update(list => [saved, ...list]);
        this.showForm.set(false);
        this.saving.set(false);
      },
      error: () => this.saving.set(false)
    });
  }

  delete(id: string) {
    if (!confirm('Delete this template?')) return;
    this.advService.deleteTemplate(id).subscribe({
      next: () => this.templates.update(list => list.filter(t => t.id !== id))
    });
  }

  cancel() { this.showForm.set(false); }
}
