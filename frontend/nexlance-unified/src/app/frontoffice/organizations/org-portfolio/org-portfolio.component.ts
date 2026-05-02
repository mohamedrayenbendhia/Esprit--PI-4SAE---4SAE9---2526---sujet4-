import { Component, Input, OnInit, signal, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule } from '@angular/material/dialog';
import { OrganizationService } from '@core/services/organization.service';
import { OrgPortfolioItem, CreatePortfolioItemRequest } from '@core/models/organization.model';

@Component({
  selector: 'app-org-portfolio',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatIconModule, MatButtonModule,
    MatFormFieldModule, MatInputModule, MatCardModule,
    MatSnackBarModule, MatDialogModule
  ],
  templateUrl: './org-portfolio.component.html',
  styleUrls: ['./org-portfolio.component.scss']
})
export class OrgPortfolioComponent implements OnInit {
  @Input() orgId!: string;
  @Input() canEdit = false;

  items     = signal<OrgPortfolioItem[]>([]);
  isLoading = signal(true);
  showForm  = signal(false);
  saving    = signal(false);

  editingId = signal<string | null>(null);
  form: CreatePortfolioItemRequest = { title: '' };
  private orgService = inject(OrganizationService);
  private snack      = inject(MatSnackBar);
  private destroyRef = inject(DestroyRef);

  tagInput = '';

  ngOnInit() { this.load(); }

  load() {
    this.orgService.getPortfolio(this.orgId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: list => { this.items.set(Array.isArray(list) ? list : []); this.isLoading.set(false); },
        error: () => this.isLoading.set(false)
      });
  }

  openCreate() {
    this.editingId.set(null);
    this.form = { title: '', tags: [] };
    this.tagInput = '';
    this.showForm.set(true);
  }

  openEdit(item: OrgPortfolioItem) {
    this.editingId.set(item.id);
    this.form = {
      title: item.title,
      description: item.description ?? '',
      imageUrl: item.imageUrl ?? '',
      projectUrl: item.projectUrl ?? '',
      clientName: item.clientName ?? '',
      tags: [...item.tags],
      completedAt: item.completedAt ?? ''
    };
    this.tagInput = '';
    this.showForm.set(true);
  }

  addTag() {
    const t = this.tagInput.trim();
    if (t && !this.form.tags?.includes(t)) {
      this.form.tags = [...(this.form.tags ?? []), t];
    }
    this.tagInput = '';
  }

  removeTag(tag: string) {
    this.form.tags = this.form.tags?.filter(t => t !== tag);
  }

  save() {
    if (!this.form.title?.trim()) return;
    this.saving.set(true);
    const obs$ = this.editingId()
      ? this.orgService.updatePortfolioItem(this.orgId, this.editingId()!, this.form)
      : this.orgService.createPortfolioItem(this.orgId, this.form);

    obs$.pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: item => {
            if (this.editingId()) {
              this.items.update(list => list.map(i => i.id === item.id ? item : i));
            } else {
              this.items.update(list => [item, ...list]);
            }
            this.snack.open('Saved.', 'OK', { duration: 2000 });
            this.showForm.set(false);
            this.saving.set(false);
          },
          error: err => {
            this.snack.open(err?.error?.message ?? 'An error occurred.', 'OK', { duration: 3000 });
            this.saving.set(false);
          }
        });
  }

  cancel() { this.showForm.set(false); }

  delete(item: OrgPortfolioItem) {
    if (!confirm(`Supprimer "${item.title}" du portfolio ?`)) return;
    this.orgService.deletePortfolioItem(this.orgId, item.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.items.update(list => list.filter(i => i.id !== item.id));
          this.snack.open('Deleted.', 'OK', { duration: 2000 });
        }
      });
  }
}
