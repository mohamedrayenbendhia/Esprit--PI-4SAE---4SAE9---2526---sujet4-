import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AdminService, Badge } from '../../../../../core/services/admin.service';

@Component({
  selector: 'app-badge-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './badge-form.html',
  styleUrls: ['./badge-form.css']
})
export class BadgeFormComponent implements OnInit {
  badge: Badge = {
    id: 0,
    name: '',
    description: '',
    minScore: 0,
    minProjects: 0,
    icon: '🏆',
    createdAt: new Date()
  };
  
  isEditMode = false;
  isLoading = false;
  errorMessage = '';

  constructor(
    private adminService: AdminService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
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

  saveBadge(): void {
    this.isLoading = true;
    
    const request = this.isEditMode
      ? this.adminService.updateBadge(this.badge.id, this.badge)
      : this.adminService.createBadge(this.badge);

    request.subscribe({
      next: () => {
        this.router.navigate(['/backoffice/admin/badges']);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = 'Erreur sauvegarde';
        this.isLoading = false;
        console.error(error);
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/backoffice/admin/badges']);
  }
}