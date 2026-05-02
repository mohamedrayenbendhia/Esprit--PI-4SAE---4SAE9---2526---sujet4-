import { Component, Input, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ComplaintAdvancedService } from '../../../core/services/complaint-advanced.service';
import { NpsSurvey } from '../../../core/models/complaint-advanced.model';

@Component({
  selector: 'app-nps-widget',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule,
            MatFormFieldModule, MatInputModule, MatProgressSpinnerModule],
  templateUrl: './nps-widget.component.html'
})
export class NpsWidgetComponent implements OnInit {
  @Input() complaintId!: string;

  private advService = inject(ComplaintAdvancedService);

  survey       = signal<NpsSurvey | null>(null);
  isLoading    = signal(true);
  selectedScore = signal<number | null>(null);
  comment      = signal('');
  submitting   = signal(false);
  submitted    = signal(false);

  readonly scores = [0,1,2,3,4,5,6,7,8,9,10];

  ngOnInit() {
    this.advService.getNpsSurvey(this.complaintId).subscribe({
      next:  s  => { this.survey.set(s); this.isLoading.set(false); if (s.respondedAt) this.submitted.set(true); },
      error: () => this.isLoading.set(false)
    });
  }

  submit() {
    const score = this.selectedScore();
    if (score === null) return;
    this.submitting.set(true);
    this.advService.respondNps(this.complaintId, { score, comment: this.comment() || undefined }).subscribe({
      next:  () => { this.submitted.set(true); this.submitting.set(false); },
      error: () => this.submitting.set(false)
    });
  }

  scoreLabel(s: number): string {
    if (s <= 6) return '😞';
    if (s <= 8) return '😐';
    return '😊';
  }

  scoreClass(s: number): string {
    if (s <= 6) return 'detractor';
    if (s <= 8) return 'passive';
    return 'promoter';
  }
}
