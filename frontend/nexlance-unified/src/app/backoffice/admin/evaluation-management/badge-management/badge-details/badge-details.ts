import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AdminService, Badge } from '../../../../../core/services/admin.service';

@Component({
  selector: 'app-badge-details',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './badge-details.html',
  styleUrls: ['./badge-details.css']
})
export class BadgeDetailsComponent implements OnInit {
  badge: Badge | null = null;
  isLoading = true;
  errorMessage = '';

  constructor(
    private adminService: AdminService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadBadge(+id);
    }
  }

  loadBadge(id: number): void {
    this.isLoading = true;
    this.adminService.getBadgeById(id).subscribe({
      next: (data) => {
        this.badge = data;
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = 'Erreur chargement du badge';
        this.isLoading = false;
        console.error(error);
      }
    });
  }

  editBadge(): void {
    if (this.badge) {
      this.router.navigate(['/backoffice/admin/badges/edit', this.badge.id]);
    }
  }

  deleteBadge(): void {
    if (!this.badge) return;
    
    if (confirm('Supprimer ce badge ?')) {
      this.adminService.deleteBadge(this.badge.id).subscribe({
        next: () => {
          this.router.navigate(['/backoffice/admin/badges']);
        },
        error: (error: HttpErrorResponse) => {
          alert('Erreur lors de la suppression');
          console.error(error);
        }
      });
    }
  }

  goBack(): void {
    this.router.navigate(['/backoffice/admin/badges']);
  }
}