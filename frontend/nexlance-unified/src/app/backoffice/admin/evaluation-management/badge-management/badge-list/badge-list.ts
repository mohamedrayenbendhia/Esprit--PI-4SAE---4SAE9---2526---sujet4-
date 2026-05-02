import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

// ✅ Correction du chemin (4 niveaux pour atteindre core/)
import { AdminService, Badge } from '../../../../../core/services/admin.service';

@Component({
  selector: 'app-badge-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './badge-list.html',
  styleUrls: ['./badge-list.css']
})
export class BadgeListComponent implements OnInit {
  badges: Badge[] = [];
  isLoading = true;
  errorMessage = '';

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadBadges();
  }

  loadBadges(): void {
    this.isLoading = true;
    this.adminService.getAllBadges().subscribe({
      next: (data: Badge[]) => {  // ✅ Type explicite
        this.badges = data;
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => {  // ✅ Type HttpErrorResponse
        this.errorMessage = 'Erreur chargement badges';
        this.isLoading = false;
        console.error('❌ Erreur:', error.message);
      }
    });
  }

  deleteBadge(id: number): void {
    if (confirm('Supprimer ce badge ?')) {
      this.adminService.deleteBadge(id).subscribe({
        next: () => {
          this.badges = this.badges.filter(b => b.id !== id);
        },
        error: (error: HttpErrorResponse) => {  // ✅ Type HttpErrorResponse
          alert('Erreur lors de la suppression');
          console.error('❌ Erreur:', error.message);
        }
      });
    }
  }
}