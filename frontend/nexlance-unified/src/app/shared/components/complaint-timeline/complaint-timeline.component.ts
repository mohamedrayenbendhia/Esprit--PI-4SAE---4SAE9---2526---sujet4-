import { Component, Input, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { ComplaintAdvancedService } from '@core/services/complaint-advanced.service';
import {
  ComplaintEvent,
  STATUS_TIMELINE_LABELS,
  PRIORITY_TIMELINE_LABELS,
  ACTOR_ROLE_LABELS
} from '@core/models/complaint-advanced.model';

@Component({
  selector: 'app-complaint-timeline',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    <div class="timeline-container">
      <div class="timeline-header">
        <mat-icon>history</mat-icon>
        <h3>History</h3>
        <span class="event-count" *ngIf="events().length > 0">{{ events().length }} event{{ events().length > 1 ? 's' : '' }}</span>
      </div>

      <!-- Loading -->
      <div *ngIf="isLoading()" class="timeline-loading">
        <div class="tl-skeleton" *ngFor="let i of [1,2,3]"></div>
      </div>

      <!-- Empty -->
      <div *ngIf="!isLoading() && events().length === 0" class="timeline-empty">
        <mat-icon>timeline</mat-icon>
        <p>No events recorded</p>
      </div>

      <!-- Timeline -->
      <div *ngIf="!isLoading() && events().length > 0" class="timeline">
        <div class="tl-item" *ngFor="let ev of events(); let last = last"
             [class.last]="last">
          <!-- Ligne verticale + point -->
          <div class="tl-axis">
            <div class="tl-dot" [ngClass]="dotClass(ev.eventType)">
              <mat-icon>{{ ev.icon }}</mat-icon>
            </div>
            <div class="tl-line" *ngIf="!last"></div>
          </div>

          <!-- Contenu -->
          <div class="tl-content">
            <div class="tl-top">
              <span class="tl-label">{{ ev.eventLabel }}</span>
              <span class="tl-time">{{ formatDate(ev.occurredAt) }}</span>
            </div>

            <!-- Transition valeur -->
            <div class="tl-transition" *ngIf="ev.oldValue || ev.newValue">
              <span class="tl-val old" *ngIf="ev.oldValue">
                {{ translateValue(ev.oldValue) }}
              </span>
              <mat-icon *ngIf="ev.oldValue && ev.newValue" class="tl-arrow">arrow_forward</mat-icon>
              <span class="tl-val new" *ngIf="ev.newValue">
                {{ translateValue(ev.newValue) }}
              </span>
            </div>

            <!-- Commentaire -->
            <div class="tl-comment" *ngIf="ev.comment">
              <mat-icon>format_quote</mat-icon>
              {{ ev.comment }}
            </div>

            <!-- Acteur -->
            <div class="tl-actor" *ngIf="ev.actorRole">
              <mat-icon>person_outline</mat-icon>
              {{ actorLabel(ev.actorRole) }}
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .timeline-container {
      background: #fff;
      border-radius: 12px;
      border: 1px solid #e8eaf0;
      padding: 20px;
    }

    .timeline-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 20px;
      padding-bottom: 14px;
      border-bottom: 1px solid #f0f2f5;

      mat-icon { color: #6366f1; font-size: 20px; width: 20px; height: 20px; }

      h3 {
        margin: 0;
        font-size: 15px;
        font-weight: 600;
        color: #1a1f36;
        flex: 1;
      }

      .event-count {
        font-size: 12px;
        color: #6b7280;
        background: #f3f4f6;
        padding: 2px 8px;
        border-radius: 20px;
      }
    }

    .timeline-loading {
      display: flex;
      flex-direction: column;
      gap: 16px;

      .tl-skeleton {
        height: 56px;
        background: linear-gradient(90deg, #f3f4f6 25%, #e5e7eb 50%, #f3f4f6 75%);
        background-size: 200% 100%;
        border-radius: 8px;
        animation: shimmer 1.4s infinite;
      }

      @keyframes shimmer {
        0%   { background-position: 200% 0; }
        100% { background-position: -200% 0; }
      }
    }

    .timeline-empty {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      padding: 24px 0;
      color: #9ca3af;

      mat-icon { font-size: 32px; width: 32px; height: 32px; opacity: .5; }
      p { margin: 0; font-size: 13px; }
    }

    .timeline { display: flex; flex-direction: column; }

    .tl-item {
      display: flex;
      gap: 14px;
      padding-bottom: 20px;

      &.last { padding-bottom: 0; }
    }

    .tl-axis {
      display: flex;
      flex-direction: column;
      align-items: center;
      flex-shrink: 0;
    }

    .tl-dot {
      width: 34px;
      height: 34px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      border: 2px solid transparent;

      mat-icon {
        font-size: 16px;
        width: 16px;
        height: 16px;
      }

      &.dot-create   { background: #ecfdf5; border-color: #34d399; color: #059669; }
      &.dot-status   { background: #eff6ff; border-color: #60a5fa; color: #2563eb; }
      &.dot-resolve  { background: #f0fdf4; border-color: #4ade80; color: #16a34a; }
      &.dot-close    { background: #f3f4f6; border-color: #9ca3af; color: #6b7280; }
      &.dot-assign   { background: #faf5ff; border-color: #c084fc; color: #9333ea; }
      &.dot-escalate { background: #fff7ed; border-color: #fb923c; color: #ea580c; }
      &.dot-priority { background: #fffbeb; border-color: #fbbf24; color: #d97706; }
      &.dot-reopen   { background: #fefce8; border-color: #facc15; color: #ca8a04; }
      &.dot-mediation{ background: #fff1f2; border-color: #fb7185; color: #e11d48; }
      &.dot-default  { background: #f8fafc; border-color: #cbd5e1; color: #64748b; }
    }

    .tl-line {
      width: 2px;
      flex: 1;
      background: linear-gradient(to bottom, #e5e7eb, #f3f4f6);
      margin-top: 4px;
      min-height: 16px;
    }

    .tl-content {
      flex: 1;
      padding-top: 6px;
      min-width: 0;
    }

    .tl-top {
      display: flex;
      align-items: baseline;
      gap: 8px;
      flex-wrap: wrap;
      margin-bottom: 4px;

      .tl-label {
        font-size: 13px;
        font-weight: 600;
        color: #1a1f36;
      }

      .tl-time {
        font-size: 11px;
        color: #9ca3af;
        margin-left: auto;
        white-space: nowrap;
      }
    }

    .tl-transition {
      display: flex;
      align-items: center;
      gap: 6px;
      margin-bottom: 4px;

      .tl-val {
        font-size: 11px;
        padding: 2px 8px;
        border-radius: 20px;
        font-weight: 500;

        &.old {
          background: #fee2e2;
          color: #b91c1c;
          text-decoration: line-through;
          opacity: .8;
        }
        &.new {
          background: #dcfce7;
          color: #15803d;
        }
      }

      .tl-arrow {
        font-size: 14px;
        width: 14px;
        height: 14px;
        color: #9ca3af;
      }
    }

    .tl-comment {
      display: flex;
      align-items: flex-start;
      gap: 4px;
      font-size: 12px;
      color: #6b7280;
      font-style: italic;
      margin-bottom: 4px;

      mat-icon { font-size: 13px; width: 13px; height: 13px; flex-shrink: 0; margin-top: 1px; }
    }

    .tl-actor {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 11px;
      color: #9ca3af;

      mat-icon { font-size: 13px; width: 13px; height: 13px; }
    }
  `]
})
export class ComplaintTimelineComponent implements OnInit {
  @Input() complaintId!: string;

  events    = signal<ComplaintEvent[]>([]);
  isLoading = signal(true);

  constructor(private advService: ComplaintAdvancedService) {}

  ngOnInit(): void {
    this.advService.getComplaintEvents(this.complaintId).subscribe({
      next: data => { this.events.set(data); this.isLoading.set(false); },
      error: ()   => { this.isLoading.set(false); }
    });
  }

  dotClass(eventType: string): string {
    const map: Record<string, string> = {
      COMPLAINT_CREATED:   'dot-create',
      STATUS_CHANGED:      'dot-status',
      COMPLAINT_RESOLVED:  'dot-resolve',
      COMPLAINT_CLOSED:    'dot-close',
      COMPLAINT_REOPENED:  'dot-reopen',
      COMPLAINT_ASSIGNED:  'dot-assign',
      COMPLAINT_ESCALATED: 'dot-escalate',
      PRIORITY_CHANGED:    'dot-priority',
      MEDIATION_OPENED:    'dot-mediation',
      MEDIATION_DECIDED:   'dot-mediation',
      REPORTED_INVOLVED:   'dot-assign',
    };
    return map[eventType] ?? 'dot-default';
  }

  translateValue(val: string | null): string {
    if (!val) return '';
    return STATUS_TIMELINE_LABELS[val]
        ?? PRIORITY_TIMELINE_LABELS[val]
        ?? val;
  }

  actorLabel(role: string | null): string {
    if (!role) return '';
    return ACTOR_ROLE_LABELS[role] ?? role;
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('en-US', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }
}
