import { Component, signal, computed, OnInit, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { OrganizationService } from '../../../core/services/organization.service';
import {
  OrganizationSummary, OrganizationType, OrganizationSize, OrganizationStatus
} from '../../../core/models/organization.model';

@Component({
  selector: 'app-organization-search',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatChipsModule
  ],
  templateUrl: './organization-search.component.html',
  styleUrls: ['./organization-search.component.scss']
})
export class OrganizationSearchComponent implements OnInit {
  private orgService = inject(OrganizationService);
  private router     = inject(Router);
  private destroyRef = inject(DestroyRef);

  // ── State ─────────────────────────────────────────────────────────────────
  results       = signal<OrganizationSummary[]>([]);
  isLoading     = signal(false);
  totalPages    = signal(0);
  currentPage   = signal(0);

  query         = signal('');
  selectedType  = signal<OrganizationType | ''>('');
  selectedSize  = signal<OrganizationSize | ''>('');

  // ── Options ───────────────────────────────────────────────────────────────
  readonly types = Object.values(OrganizationType);
  readonly sizes = Object.values(OrganizationSize);

  ngOnInit() { this.load(); }

  load(page = 0) {
    this.isLoading.set(true);
    this.orgService.search(
      this.query() || undefined,
      this.selectedType() as OrganizationType || undefined,
      this.selectedSize() as OrganizationSize || undefined,
      page
    ).pipe(takeUntilDestroyed(this.destroyRef))
     .subscribe({
      next: res => {
        this.results.set(res?.content ?? []);
        this.totalPages.set(res?.totalPages ?? 0);
        this.currentPage.set(res?.number ?? 0);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  search() { this.load(0); }
  reset()  { this.query.set(''); this.selectedType.set(''); this.selectedSize.set(''); this.load(0); }
  prev()   { if (this.currentPage() > 0) this.load(this.currentPage() - 1); }
  next()   { if (this.currentPage() < this.totalPages() - 1) this.load(this.currentPage() + 1); }

  goToProfile(id: string) { this.router.navigate(['/frontoffice/organizations', id]); }

  stars(rating: number): string { return '★'.repeat(Math.round(rating)) + '☆'.repeat(5 - Math.round(rating)); }
}
