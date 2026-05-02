import {
  Component, EventEmitter, Input, Output, OnInit, OnDestroy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';

import { FilterChipComponent } from './filter-chip.component';
import { FilterDropdownComponent } from './filter-dropdown.component';
import {
  FilterBarConfig, FilterOption, RangeConfig, ToggleConfig, DropdownConfig
} from './filter-config.model';

/**
 * Reusable, config-driven filter bar inspired by the Dribbble "Job Feed" shot.
 *
 * Layout (top → bottom):
 *  ┌─────────────────────────────────────────────────────────────────────┐
 *  │ 🔍 search bar ………………………………………    [sort dropdown]               │
 *  ├─────────────────────────────────────────────────────────────────────┤
 *  │ [All] [Dev] [Design] [Marketing]…     ← quick category pills        │
 *  ├─────────────────────────────────────────────────────────────────────┤
 *  │ [Budget ▾] [Experience ▾] [Remote ⇄]  clear-all →                   │
 *  ├─────────────────────────────────────────────────────────────────────┤
 *  │ active filter chips × × ×                                           │
 *  └─────────────────────────────────────────────────────────────────────┘
 *
 * Usage:
 *   <app-filter-bar [config]="cfg" [filters]="state" (filtersChange)="apply($event)"></app-filter-bar>
 */
@Component({
  selector: 'app-filter-bar',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatIconModule,
    FilterChipComponent, FilterDropdownComponent
  ],
  templateUrl: './filter-bar.component.html',
  styleUrls: ['./filter-bar.component.scss']
})
export class FilterBarComponent implements OnInit, OnDestroy {
  @Input({ required: true }) config!: FilterBarConfig;
  @Input() filters: Record<string, any> = {};
  /** Debounce for search input (ms). 0 disables debouncing. */
  @Input() searchDebounce = 300;
  /** Total result count shown inline (optional) */
  @Input() resultCount?: number;
  /** Label suffix for result count (e.g. "results found") */
  @Input() resultLabel = 'results';

  @Output() filtersChange = new EventEmitter<Record<string, any>>();
  @Output() searchChange = new EventEmitter<string>();

  private search$ = new Subject<string>();
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.search$.pipe(
      debounceTime(this.searchDebounce),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(v => {
      this.patch(this.searchKey, v || undefined);
      this.searchChange.emit(v);
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ─── Helpers ──────────────────────────────────────────────

  get searchKey(): string { return this.config.searchKey || 'search'; }
  get searchValue(): string { return this.filters[this.searchKey] || ''; }

  onSearchInput(ev: Event) {
    const v = (ev.target as HTMLInputElement).value;
    this.search$.next(v);
  }

  clearSearch() {
    this.patch(this.searchKey, undefined);
    this.searchChange.emit('');
  }

  patch(key: string, value: any) {
    const next = { ...this.filters };
    if (value === undefined || value === null || value === '' || value === false) {
      delete next[key];
    } else {
      next[key] = value;
    }
    this.filters = next;
    this.filtersChange.emit(next);
  }

  patchRange(cfg: RangeConfig, min: any, max: any) {
    const next = { ...this.filters };
    const m = min === '' || min == null ? undefined : Number(min);
    const M = max === '' || max == null ? undefined : Number(max);
    if (m === undefined) delete next[cfg.minKey]; else next[cfg.minKey] = m;
    if (M === undefined) delete next[cfg.maxKey]; else next[cfg.maxKey] = M;
    this.filters = next;
    this.filtersChange.emit(next);
  }

  applyPreset(cfg: RangeConfig, preset: { min?: number; max?: number }) {
    this.patchRange(cfg, preset.min, preset.max);
  }

  toggleBool(cfg: ToggleConfig) {
    this.patch(cfg.key, !this.filters[cfg.key]);
  }

  isPillSelected(opt: FilterOption): boolean {
    const cur = this.filters[this.config.pills!.key];
    return cur === opt.value;
  }

  selectPill(opt: FilterOption) {
    const key = this.config.pills!.key;
    const cur = this.filters[key];
    // click again on a selected pill to clear
    this.patch(key, cur === opt.value ? undefined : opt.value);
  }

  clearPill() { this.patch(this.config.pills!.key, undefined); }

  // ─── Active filters (removable chips) ─────────────────────

  get activeChips(): { key: string; label: string; icon?: string; onRemove: () => void }[] {
    const chips: { key: string; label: string; icon?: string; onRemove: () => void }[] = [];

    // Pills (single value)
    if (this.config.pills) {
      const v = this.filters[this.config.pills.key];
      if (v !== undefined && v !== null && v !== '') {
        const opt = this.config.pills.options.find(o => o.value === v);
        if (opt) {
          chips.push({
            key: this.config.pills.key,
            label: opt.label,
            icon: opt.icon,
            onRemove: () => this.patch(this.config.pills!.key, undefined)
          });
        }
      }
    }

    // Dropdowns
    for (const dd of this.config.dropdowns || []) {
      const v = this.filters[dd.key];
      if (v !== undefined && v !== null && v !== '') {
        const opt = dd.options.find(o => o.value === v);
        chips.push({
          key: dd.key,
          label: `${dd.label}: ${opt?.label ?? v}`,
          icon: dd.icon,
          onRemove: () => this.patch(dd.key, undefined)
        });
      }
    }

    // Ranges
    for (const r of this.config.ranges || []) {
      const min = this.filters[r.minKey];
      const max = this.filters[r.maxKey];
      if (min !== undefined || max !== undefined) {
        const unit = r.unit ? ` ${r.unit}` : '';
        const label = `${r.label}: ${min ?? '–'}${unit} – ${max ?? '∞'}${unit}`;
        chips.push({
          key: r.minKey,
          label,
          icon: r.icon,
          onRemove: () => this.patchRange(r, undefined, undefined)
        });
      }
    }

    // Toggles
    for (const t of this.config.toggles || []) {
      if (this.filters[t.key]) {
        chips.push({
          key: t.key,
          label: t.label,
          icon: t.icon,
          onRemove: () => this.patch(t.key, undefined)
        });
      }
    }

    // Sort
    if (this.config.sort) {
      const v = this.filters[this.config.sort.key];
      if (v !== undefined && v !== null && v !== '') {
        const opt = this.config.sort.options.find(o => o.value === v);
        chips.push({
          key: this.config.sort.key,
          label: `Sort: ${opt?.label ?? v}`,
          icon: 'sort',
          onRemove: () => this.patch(this.config.sort!.key, undefined)
        });
      }
    }

    return chips;
  }

  get hasActiveFilters(): boolean {
    return this.activeChips.length > 0 || !!this.searchValue;
  }

  clearAll() {
    const preserved: Record<string, any> = {};
    // keep page-size/pagination fields the caller may carry in state
    for (const k of Object.keys(this.filters)) {
      if (k === 'page' || k === 'size' || k === 'pageSize') preserved[k] = this.filters[k];
    }
    this.filters = preserved;
    this.filtersChange.emit(preserved);
    this.searchChange.emit('');
  }
}
