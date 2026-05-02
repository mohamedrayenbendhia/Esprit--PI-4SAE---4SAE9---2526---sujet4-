import { Component, Input, OnInit, OnChanges, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { TranslateModule } from '@ngx-translate/core';
import { ReputationService, ReputationScore } from '../../../core/services/reputation.service';

@Component({
  selector: 'app-reputation-badge',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressBarModule,
    MatChipsModule,
    TranslateModule
  ],
  template: `
    <div class="reputation-widget" *ngIf="reputation()">
      <mat-card class="reputation-card">
        <mat-card-content>
          <!-- Tier Badge -->
          <div class="tier-section">
            <div class="tier-badge" [style.background]="getTierGradient()">
              <mat-icon [style.color]="reputationService.getTierColor(reputation()!.tier)">
                {{ reputationService.getTierIcon(reputation()!.tier) }}
              </mat-icon>
            </div>
            <div class="tier-info">
              <span class="tier-name">{{ reputation()!.tier }}</span>
              <span class="tier-score">{{ reputation()!.overallScore }}/100</span>
            </div>
          </div>

          <!-- Score Bar -->
          <div class="score-bar-section">
            <mat-progress-bar
              mode="determinate"
              [value]="reputation()!.overallScore"
              [color]="getProgressColor()">
            </mat-progress-bar>
          </div>

          <!-- Stats Grid -->
          <div class="stats-mini-grid">
            <div class="mini-stat" matTooltip="Applications accepted / total">
              <mat-icon>assignment_turned_in</mat-icon>
              <span>{{ reputation()!.acceptedApplications }}/{{ reputation()!.totalApplications }}</span>
              <small>Applications</small>
            </div>
            <div class="mini-stat" matTooltip="Recommendations accepted / total">
              <mat-icon>thumb_up</mat-icon>
              <span>{{ reputation()!.acceptedRecommendations }}/{{ reputation()!.totalRecommendations }}</span>
              <small>Recommendations</small>
            </div>
            <div class="mini-stat" matTooltip="Response rate">
              <mat-icon>speed</mat-icon>
              <span>{{ reputation()!.responseRate | number:'1.0-0' }}%</span>
              <small>Response</small>
            </div>
          </div>

          <!-- Badges -->
          <div class="badges-section" *ngIf="reputation()!.badges && reputation()!.badges.length > 0">
            <div class="badges-label">Badges</div>
            <div class="badges-list">
              <span
                *ngFor="let badge of reputation()!.badges"
                class="badge-chip"
                [matTooltip]="reputationService.getBadgeLabel(badge)">
                <mat-icon class="badge-icon">{{ getBadgeIcon(badge) }}</mat-icon>
                {{ reputationService.getBadgeLabel(badge) }}
              </span>
            </div>
          </div>
        </mat-card-content>
      </mat-card>
    </div>

    <!-- Compact mode -->
    <div class="reputation-compact" *ngIf="compact && reputation()">
      <span class="compact-tier" [style.color]="reputationService.getTierColor(reputation()!.tier)">
        <mat-icon class="compact-icon">{{ reputationService.getTierIcon(reputation()!.tier) }}</mat-icon>
        {{ reputation()!.tier }}
      </span>
      <span class="compact-score">({{ reputation()!.overallScore }}/100)</span>
    </div>
  `,
  styles: [`
    .reputation-widget {
      .reputation-card {
        border-radius: 12px;
        overflow: hidden;
        box-shadow: 0 3px 15px rgba(0,0,0,0.06);
      }
    }

    .tier-section {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;

      .tier-badge {
        width: 56px;
        height: 56px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        background: linear-gradient(135deg, #1a1a2e, #16213e);

        mat-icon {
          font-size: 32px;
          width: 32px;
          height: 32px;
        }
      }

      .tier-info {
        display: flex;
        flex-direction: column;

        .tier-name {
          font-size: 1.25rem;
          font-weight: 700;
          color: #2d3748;
          text-transform: capitalize;
        }

        .tier-score {
          font-size: 0.875rem;
          color: #718096;
          font-weight: 500;
        }
      }
    }

    .score-bar-section {
      margin-bottom: 16px;
      mat-progress-bar { border-radius: 8px; height: 8px; }
    }

    .stats-mini-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 12px;
      margin-bottom: 16px;

      .mini-stat {
        display: flex;
        flex-direction: column;
        align-items: center;
        text-align: center;
        padding: 8px;
        border-radius: 8px;
        background: #f7fafc;

        mat-icon {
          font-size: 20px;
          width: 20px;
          height: 20px;
          color: #0EA5E9;
          margin-bottom: 4px;
        }

        span {
          font-weight: 700;
          font-size: 0.875rem;
          color: #2d3748;
        }

        small {
          font-size: 0.688rem;
          color: #94a3b8;
          margin-top: 2px;
        }
      }
    }

    .badges-section {
      .badges-label {
        font-size: 0.75rem;
        text-transform: uppercase;
        font-weight: 600;
        color: #94a3b8;
        letter-spacing: 0.5px;
        margin-bottom: 8px;
      }

      .badges-list {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;

        .badge-chip {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          padding: 4px 10px;
          border-radius: 16px;
          font-size: 0.75rem;
          font-weight: 600;
          background: linear-gradient(135deg, #EFF6FF, #DBEAFE);
          color: #1E40AF;

          .badge-icon {
            font-size: 14px;
            width: 14px;
            height: 14px;
          }
        }
      }
    }

    .reputation-compact {
      display: inline-flex;
      align-items: center;
      gap: 6px;

      .compact-tier {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        font-weight: 700;
        font-size: 0.875rem;

        .compact-icon {
          font-size: 18px;
          width: 18px;
          height: 18px;
        }
      }

      .compact-score {
        font-size: 0.813rem;
        color: #718096;
      }
    }
  `]
})
export class ReputationBadgeComponent implements OnInit, OnChanges {
  @Input() freelancerId!: string;
  @Input() compact = false;

  reputationService = inject(ReputationService);
  reputation = signal<ReputationScore | null>(null);

  ngOnInit(): void {
    this.loadReputation();
  }

  ngOnChanges(): void {
    if (this.freelancerId) {
      this.loadReputation();
    }
  }

  loadReputation(): void {
    if (!this.freelancerId) return;
    this.reputationService.getReputation(this.freelancerId).subscribe({
      next: (rep) => this.reputation.set(rep),
      error: () => this.reputation.set(null)
    });
  }

  getTierGradient(): string {
    switch (this.reputation()?.tier) {
      case 'PLATINUM': return 'linear-gradient(135deg, #1a1a2e, #4a4a6e)';
      case 'GOLD': return 'linear-gradient(135deg, #92400E, #D97706)';
      case 'SILVER': return 'linear-gradient(135deg, #475569, #94A3B8)';
      default: return 'linear-gradient(135deg, #78350F, #B45309)';
    }
  }

  getProgressColor(): 'primary' | 'accent' | 'warn' {
    const score = this.reputation()?.overallScore ?? 0;
    if (score >= 60) return 'primary';
    if (score >= 40) return 'accent';
    return 'warn';
  }

  getBadgeIcon(badge: string): string {
    const icons: { [key: string]: string } = {
      'TOP_PERFORMER': 'emoji_events',
      'EXPERIENCED': 'school',
      'VERIFIED_FREELANCER': 'verified',
      'HIGHLY_RECOMMENDED': 'recommend',
      'TRUSTED_EXPERT': 'workspace_premium',
      'FAST_RESPONDER': 'bolt',
      'ACTIVE_CONTRIBUTOR': 'local_fire_department'
    };
    return icons[badge] || 'star';
  }
}
