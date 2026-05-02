import { Component, Input, Output, EventEmitter, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatCardModule } from '@angular/material/card';
import { ComplaintAdvancedService } from '../../../core/services/complaint-advanced.service';
import { ResponseTemplate } from '../../../core/models/complaint-advanced.model';

@Component({
  selector: 'app-template-picker',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule,
            MatFormFieldModule, MatSelectModule, MatCardModule],
  template: `
    <div class="picker-wrap">
      <mat-form-field appearance="outline" style="width:100%">
        <mat-label>
          <mat-icon>library_books</mat-icon> Use a response template
        </mat-label>
        <mat-select [ngModel]="selectedId()" (ngModelChange)="onSelect($event)">
          <mat-option value="">-- None --</mat-option>
          @for (t of templates(); track t.id) {
            <mat-option [value]="t.id">
              {{ t.title }}
              @if (t.category) { <span class="cat-hint"> ({{ t.category }})</span> }
            </mat-option>
          }
        </mat-select>
      </mat-form-field>

      @if (preview()) {
        <div class="preview-box">
          <p>{{ preview() }}</p>
          <button mat-raised-button color="primary" (click)="use()">
            <mat-icon>content_copy</mat-icon> Use this text
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    .picker-wrap { margin-bottom: 8px; }
    .cat-hint { font-size: 0.75rem; color: #888; }
    .preview-box { background: #f5f5f5; border-left: 3px solid #1976d2; padding: 12px; border-radius: 4px; margin-top: 4px; }
    .preview-box p { margin: 0 0 10px; white-space: pre-wrap; font-size: 0.9rem; }
  `]
})
export class TemplatePickerComponent implements OnInit {
  @Input()  category?: string;
  @Output() templateSelected = new EventEmitter<string>();

  private advService = inject(ComplaintAdvancedService);

  templates  = signal<ResponseTemplate[]>([]);
  selectedId = signal('');
  preview    = signal('');

  ngOnInit() {
    const obs = this.category
      ? this.advService.getTemplatesByCategory(this.category)
      : this.advService.getAllTemplates();
    obs.subscribe({ next: list => this.templates.set(list) });
  }

  onSelect(id: string) {
    this.selectedId.set(id);
    const t = this.templates().find(x => x.id === id);
    this.preview.set(t?.content ?? '');
  }

  use() {
    if (this.preview()) this.templateSelected.emit(this.preview());
  }
}
