import { Component, Input, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ComplaintAdvancedService } from '../../../core/services/complaint-advanced.service';
import { SlaTracking } from '../../../core/models/complaint-advanced.model';

@Component({
  selector: 'app-sla-tracker',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatTooltipModule],
  template: `
    @if (tracking()) {
      <div class="sla-widget" [class.breached]="isBreached()" [class.warning]="isWarning()">
        <mat-icon class="sla-icon">schedule</mat-icon>
        <div class="sla-info">
          <span class="sla-label">Resolution SLA</span>
          <span class="sla-deadline">
            @if (isBreached()) {
              <mat-icon class="breach-icon">warning</mat-icon> Exceeded
            } @else {
              {{ remaining() }}
            }
          </span>
        </div>
        @if (tracking()!.firstResponseBreached) {
          <span class="badge-breach" matTooltip="First response overdue">1st resp. !</span>
        }
      </div>
    }
  `,
  styles: [`
    .sla-widget {
      display: inline-flex; align-items: center; gap: 8px;
      padding: 6px 12px; border-radius: 20px;
      background: #e8f5e9; border: 1px solid #a5d6a7;
      font-size: 0.82rem; color: #2e7d32;
    }
    .sla-widget.warning { background: #fff8e1; border-color: #ffe082; color: #f57f17; }
    .sla-widget.breached { background: #ffebee; border-color: #ef9a9a; color: #c62828; }
    .sla-icon { font-size: 16px; height: 16px; width: 16px; }
    .breach-icon { font-size: 14px; height: 14px; width: 14px; vertical-align: middle; }
    .sla-label { font-weight: 500; }
    .sla-deadline { font-weight: 700; }
    .badge-breach {
      background: #c62828; color: white;
      padding: 1px 6px; border-radius: 8px; font-size: 0.7rem; font-weight: 700;
    }
  `]
})
export class SlaTrackerComponent implements OnInit {
  @Input() complaintId!: string;

  private advService = inject(ComplaintAdvancedService);

  tracking  = signal<SlaTracking | null>(null);
  remaining = signal('...');

  ngOnInit() {
    this.advService.getSlaTracking(this.complaintId).subscribe({
      next: t => {
        this.tracking.set(t);
        this.computeRemaining(t);
      }
    });
  }

  isBreached(): boolean {
    const t = this.tracking();
    return !!t?.resolutionBreached;
  }

  isWarning(): boolean {
    const t = this.tracking();
    if (!t || t.resolutionBreached) return false;
    const ms = new Date(t.resolutionDeadline).getTime() - Date.now();
    return ms > 0 && ms < 4 * 3600 * 1000; // moins de 4h
  }

  private computeRemaining(t: SlaTracking) {
    const ms = new Date(t.resolutionDeadline).getTime() - Date.now();
    if (ms <= 0) { this.remaining.set('Exceeded'); return; }
    const h = Math.floor(ms / 3600000);
    const m = Math.floor((ms % 3600000) / 60000);
    this.remaining.set(h > 0 ? `${h}h ${m}min` : `${m} min`);
  }
}
