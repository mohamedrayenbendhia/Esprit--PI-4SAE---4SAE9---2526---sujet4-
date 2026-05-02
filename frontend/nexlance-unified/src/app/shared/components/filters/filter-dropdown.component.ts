import {
  Component, ElementRef, EventEmitter, HostListener,
  Input, Output, TemplateRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { FilterOption } from './filter-config.model';

/**
 * Generic click-outside dropdown used for:
 *  • single-select filters (experience level, sort)
 *  • custom content (budget range min/max) via <ng-template #content>
 *
 * Closed by default, opens on click. Closes on outside-click or ESC.
 * Displays a "dot" indicator on the trigger when a value is active.
 */
@Component({
  selector: 'app-filter-dropdown',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  template: `
    <div class="dd" [class.dd--open]="open">
      <button
        type="button"
        class="dd__trigger"
        [class.dd__trigger--active]="active"
        [attr.aria-expanded]="open"
        (click)="toggle()">
        <mat-icon *ngIf="icon" class="dd__trigger-icon">{{ icon }}</mat-icon>
        <span class="dd__trigger-label">
          {{ displayValue || label }}
        </span>
        <span *ngIf="active" class="dd__dot"></span>
        <mat-icon class="dd__caret">{{ open ? 'expand_less' : 'expand_more' }}</mat-icon>
      </button>

      <div class="dd__menu" *ngIf="open" role="listbox">
        <!-- Option-list mode -->
        <ng-container *ngIf="options?.length && !customTemplate">
          <button
            *ngIf="allowClear"
            type="button"
            class="dd__opt"
            [class.dd__opt--selected]="value == null || value === ''"
            (click)="select(null)">
            <span>{{ clearLabel }}</span>
            <mat-icon *ngIf="value == null || value === ''">check</mat-icon>
          </button>
          <button
            *ngFor="let opt of options"
            type="button"
            class="dd__opt"
            [class.dd__opt--selected]="opt.value === value"
            (click)="select(opt.value)">
            <mat-icon *ngIf="opt.icon" class="dd__opt-icon">{{ opt.icon }}</mat-icon>
            <span>{{ opt.label }}</span>
            <span *ngIf="opt.count !== undefined" class="dd__opt-count">{{ opt.count }}</span>
            <mat-icon *ngIf="opt.value === value" class="dd__opt-check">check</mat-icon>
          </button>
        </ng-container>

        <!-- Custom template mode (range/date/etc.) -->
        <div class="dd__custom" *ngIf="customTemplate">
          <ng-container *ngTemplateOutlet="customTemplate"></ng-container>
        </div>
      </div>
    </div>
  `,
  styles: [`
    :host { display: inline-flex; position: relative; }
    .dd { position: relative; }

    .dd__trigger {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      height: 40px;
      padding: 0 14px;
      border-radius: 10px;
      border: 1px solid #E5E7EB;
      background: #FFFFFF;
      color: #374151;
      font-family: inherit;
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
      transition: border-color .15s ease, box-shadow .15s ease, background .15s ease;
      white-space: nowrap;
    }
    .dd__trigger:hover {
      border-color: #D1D5DB;
      background: #F9FAFB;
    }
    .dd__trigger:focus-visible {
      outline: none;
      border-color: #0EA5E9;
      box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.18);
    }
    .dd__trigger--active {
      border-color: #0EA5E9;
      background: #F0F9FF;
      color: #0369A1;
    }

    .dd__trigger-icon { font-size: 18px; width: 18px; height: 18px; }
    .dd__caret        { font-size: 18px; width: 18px; height: 18px; opacity: .7; }
    .dd__dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #0EA5E9;
      box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.18);
    }

    .dd__menu {
      position: absolute;
      top: calc(100% + 6px);
      left: 0;
      min-width: 220px;
      max-width: 340px;
      background: #FFFFFF;
      border: 1px solid #E5E7EB;
      border-radius: 12px;
      box-shadow: 0 10px 24px -6px rgba(15, 23, 42, 0.14), 0 4px 8px -2px rgba(15, 23, 42, 0.08);
      padding: 6px;
      z-index: 50;
      animation: dd-in .14s ease-out;
    }
    @keyframes dd-in {
      from { opacity: 0; transform: translateY(-4px); }
      to   { opacity: 1; transform: translateY(0); }
    }

    .dd__opt {
      display: flex;
      align-items: center;
      gap: 10px;
      width: 100%;
      padding: 9px 10px;
      border: none;
      background: transparent;
      border-radius: 8px;
      color: #374151;
      font-family: inherit;
      font-size: 13px;
      text-align: left;
      cursor: pointer;
      transition: background .12s ease, color .12s ease;
    }
    .dd__opt:hover {
      background: #F3F4F6;
      color: #111827;
    }
    .dd__opt--selected {
      background: #E0F2FE;
      color: #0369A1;
      font-weight: 600;
    }
    .dd__opt--selected:hover {
      background: #BAE6FD;
    }
    .dd__opt-icon { font-size: 16px; width: 16px; height: 16px; }
    .dd__opt-check {
      font-size: 16px; width: 16px; height: 16px;
      margin-left: auto;
      color: #0EA5E9;
    }
    .dd__opt-count {
      margin-left: auto;
      font-size: 11px;
      color: #6B7280;
      background: #F3F4F6;
      padding: 2px 6px;
      border-radius: 9999px;
    }

    .dd__custom { padding: 10px 6px; }
  `]
})
export class FilterDropdownComponent {
  @Input() label = '';
  @Input() icon?: string;
  @Input() options: FilterOption[] = [];
  @Input() value: any = null;
  @Input() allowClear = true;
  @Input() clearLabel = 'All';
  /** If provided, option list is hidden and this template is rendered instead */
  @Input() customTemplate?: TemplateRef<any>;

  @Output() valueChange = new EventEmitter<any>();

  open = false;

  constructor(private host: ElementRef<HTMLElement>) {}

  get active(): boolean {
    return this.value !== null && this.value !== undefined && this.value !== '';
  }

  get displayValue(): string | null {
    if (!this.active) return null;
    const opt = this.options.find(o => o.value === this.value);
    return opt ? opt.label : null;
  }

  toggle() { this.open = !this.open; }
  close() { this.open = false; }

  select(v: any) {
    this.valueChange.emit(v);
    this.close();
  }

  @HostListener('document:click', ['$event'])
  onDocClick(e: MouseEvent) {
    if (!this.open) return;
    if (!this.host.nativeElement.contains(e.target as Node)) this.close();
  }

  @HostListener('document:keydown.escape')
  onEsc() { this.close(); }
}
