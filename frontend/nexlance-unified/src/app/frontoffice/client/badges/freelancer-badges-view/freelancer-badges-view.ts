import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { BadgeService, UserBadge } from '@core/services/badge.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-freelancer-badges-view',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './freelancer-badges-view.html',
  styleUrls: ['./freelancer-badges-view.css']
})
export class FreelancerBadgesViewComponent implements OnInit {
  userBadges: UserBadge[] = [];
  freelancerId: string = '';                    // ✅ Changé de number à string
  isLoading = true;
  errorMessage = '';

  constructor(
    private badgeService: BadgeService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('freelancerId');
    if (idParam) {
      this.freelancerId = idParam;               // ✅ Garder comme string, pas Number()
      this.loadFreelancerBadges();
    }
  }

  loadFreelancerBadges(): void {
    this.isLoading = true;
    this.badgeService.getFreelancerBadges(this.freelancerId).subscribe({
      next: (data: UserBadge[]) => {
        this.userBadges = data;
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        console.error('Erreur chargement badges freelancer:', error);
        this.errorMessage = 'Impossible de charger les badges';
        this.isLoading = false;
      }
    });
  }
}