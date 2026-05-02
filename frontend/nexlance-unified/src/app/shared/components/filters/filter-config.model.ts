/**
 * Typed filter configuration for the reusable FilterBar component.
 * Designed to be list-agnostic (jobs, complaints, organizations, etc.).
 */

export type FilterValue = string | number | boolean | null | undefined;

export interface FilterOption {
  /** Canonical value stored in the filter state */
  value: FilterValue;
  /** Human-readable label */
  label: string;
  /** Optional material icon or emoji */
  icon?: string;
  /** Optional count shown as a soft badge next to the label */
  count?: number;
}

/** Quick-select category pill row (horizontal). Single value. */
export interface PillGroupConfig {
  /** Key used to look up & write the value in the filter state object */
  key: string;
  /** Optional label shown above the pills row */
  label?: string;
  /** Pills */
  options: FilterOption[];
  /** Whether the "All" pill is rendered on the left */
  allowClear?: boolean;
  /** Label for the clear-all pill (defaults to "All") */
  clearLabel?: string;
}

/** Dropdown single-select (e.g. experience level, sort by) */
export interface DropdownConfig {
  key: string;
  label: string;
  icon?: string;
  options: FilterOption[];
  /** Placeholder shown when no value selected */
  placeholder?: string;
}

/** Numeric min/max range (e.g. budget) */
export interface RangeConfig {
  key: string;                 // logical prefix (we store `${key}Min` & `${key}Max`)
  label: string;
  icon?: string;
  minKey: string;              // e.g. 'minBudget'
  maxKey: string;              // e.g. 'maxBudget'
  unit?: string;               // e.g. 'DT'
  step?: number;
  presets?: { label: string; min?: number; max?: number }[];
}

/** Boolean toggle (e.g. remote only) */
export interface ToggleConfig {
  key: string;
  label: string;
  icon?: string;
}

export interface FilterBarConfig {
  /** Placeholder for the main search input */
  searchPlaceholder?: string;
  /** Key used to store the search term (defaults to 'search') */
  searchKey?: string;
  /** Category-like quick pills row (optional, displayed first) */
  pills?: PillGroupConfig;
  /** Dropdown filters shown on the secondary row */
  dropdowns?: DropdownConfig[];
  /** Numeric range filters (rendered as dropdowns with two inputs) */
  ranges?: RangeConfig[];
  /** Boolean toggles (rendered as switch chips) */
  toggles?: ToggleConfig[];
  /** Sort dropdown (special — pinned right) */
  sort?: DropdownConfig;
}
