import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

/**
 * Dual-mode pill used by the FilterBar:
 *  • mode="toggle"    → category quick-filter pill (selectable, not removable)
 *  • mode="removable" → active-filter chip (always "selected", with × to clear)
 *
 * Styled to match claude-design.md tokens (cyan primary, gray neutrals, 9999px radius).
 */
@Component({
  selector: 'app-filter-chip',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    <button
      type="button"
      class="chip"
      [class.chip--selected]="selected"
      [class.chip--removable]="mode === 'removable'"
      [attr.aria-pressed]="mode === 'toggle' ? selected : null"
      (click)="onClick($event)">
      <mat-icon *ngIf="icon" class="chip__icon">{{ icon }}</mat-icon>
      <span class="chip__label">{{ label }}</span>
      <span *ngIf="count !== undefined && count !== null" class="chip__count">{{ count }}</span>
      <mat-icon *ngIf="mode === 'removable'" class="chip__close">close</mat-icon>
    </button>
  `,
  styles: [`
    :host { display: inline-flex; }

    .chip {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 36px;
      padding: 0 14px;
      border-radius: 9999px;
      border: 1px solid #E5E7EB;
      background: #FFFFFF;
      color: #374151;
      font-family: inherit;
      font-size: 13px;
      font-weight: 500;
      line-height: 1;
      cursor: pointer;
      transition: background .15s ease, border-color .15s ease, color .15s ease, box-shadow .15s ease;
      white-space: nowrap;
      user-select: none;
    }
    .chip:hover {
      background: #F9FAFB;
      border-color: #D1D5DB;
      color: #111827;
    }
    .chip:focus-visible {
      outline: none;
      box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.18);
      border-color: #0EA5E9;
    }

    .chip--selected {
      background: #0EA5E9;
      border-color: #0EA5E9;
      color: #FFFFFF;
      box-shadow: 0 1px 2px rgba(14, 165, 233, 0.25);
    }
    .chip--selected:hover {
      background: #0284C7;
      border-color: #0284C7;
      color: #FFFFFF;
    }

    .chip--removable {
      background: #E0F2FE;
      border-color: #BAE6FD;
      color: #0369A1;
      padding-right: 8px;
    }
    .chip--removable:hover {
      background: #BAE6FD;
      border-color: #7DD3FC;
      color: #075985;
    }

    .chip__icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
      line-height: 16px;
    }

    .chip__count {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: 20px;
      height: 18px;
      padding: 0 6px;
      border-radius: 9999px;
      background: rgba(0,0,0,0.06);
      font-size: 11px;
      font-weight: 600;
    }
    .chip--selected .chip__count {
      background: rgba(255,255,255,0.22);
      color: #FFFFFF;
    }

    .chip__close {
      font-size: 16px;
      width: 16px;
      height: 16px;
      line-height: 16px;
      opacity: 0.75;
    }
    .chip:hover .chip__close { opacity: 1; }
  `]
})
export class FilterChipComponent {
  @Input() label = '';
  @Input() icon?: string;
  @Input() count?: number | null;
  @Input() selected = false;
  @Input() mode: 'toggle' | 'removable' = 'toggle';

  @Output() toggled = new EventEmitter<void>();
  @Output() removed = new EventEmitter<void>();

  onClick(e: MouseEvent) {
    e.stopPropagation();
    if (this.mode === 'removable') this.removed.emit();
    else this.toggled.emit();
  }
}
