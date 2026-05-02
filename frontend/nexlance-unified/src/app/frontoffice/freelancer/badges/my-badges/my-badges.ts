import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { BadgeService, UserBadge } from '@core/services/badge.service';

@Component({
  selector: 'app-my-badges',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './my-badges.html',
  styleUrls: ['./my-badges.css']
})
export class MyBadgesComponent implements OnInit {
  userBadges: UserBadge[] = [];
  freelancerEmail: string = '';
  isLoading: boolean = true;
  errorMessage: string = '';

  constructor(private badgeService: BadgeService) {}

  ngOnInit(): void {
  const userStr = localStorage.getItem('current_user');
if (userStr) {
  try {
    const user = JSON.parse(userStr);
    this.freelancerEmail = user.email.toLowerCase(); // ✅ Normalisation
    console.log('📧 Freelancer email:', this.freelancerEmail);
  } catch (e) {
    console.error('Error parsing user:', e);
  }
}


    if (!this.freelancerEmail) {
      this.errorMessage = 'Email not found';
      this.isLoading = false;
      return;
    }

    this.loadMyBadges();
  }

  loadMyBadges(): void {
    this.isLoading = true;
    console.log('📡 Loading badges for email:', this.freelancerEmail);

    this.badgeService.getFreelancerBadges(this.freelancerEmail).subscribe({
      next: (data: UserBadge[]) => {
        console.log('✅ Badges received:', data);
        console.log('📦 Raw data:', JSON.stringify(data));
        this.userBadges = data;
        this.isLoading = false;
      },
      error: (error: any) => {
        this.isLoading = false;
        this.errorMessage = 'Unable to load your badges';
        console.error('❌ Error:', error);
      }
    });
  }

  // ✅ Utiliser l'icône depuis le backend
  getBadgeIcon(badge: any): string {
    return badge.icon || '🎖️';
  }

  // ✅ Couleur par défaut ou basée sur le nom
  getBadgeColor(badgeName: string): string {
    // Garder quelques couleurs spéciales pour les badges connus
    const colors: { [key: string]: string } = {
      'Expert': 'linear-gradient(135deg, #fbbf24, #f59e0b)',
      'TOP 1%': 'linear-gradient(135deg, #fbbf24, #f59e0b)',
      'Confirmed': 'linear-gradient(135deg, #94a3b8, #64748b)',
      'Beginner': 'linear-gradient(135deg, #cd7f32, #b45309)',
      'Fast': 'linear-gradient(135deg, #3b82f6, #1e40af)',
      'Communication': 'linear-gradient(135deg, #10b981, #047857)',
      'Quality': 'linear-gradient(135deg, #8b5cf6, #5b21b6)'
    };
    
    // Si le badge est dans la liste, utiliser sa couleur, sinon une couleur par défaut
    return colors[badgeName] || 'linear-gradient(135deg, #667eea, #764ba2)';
  }

  // ✅ SUPPRIMER complètement getBadgeDescription - on utilise badge.description directement
}