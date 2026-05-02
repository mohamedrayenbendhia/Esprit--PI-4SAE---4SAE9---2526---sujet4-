import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { BadgeService, Badge } from '@core/services/badge.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-badges-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './badges-list.html',
  styleUrls: ['./badges-list.css']
})
export class BadgesListComponent implements OnInit {
  badges: Badge[] = [];
  isLoading = true;
  errorMessage = '';

  constructor(private badgeService: BadgeService) {}

  ngOnInit(): void {
    this.loadBadges();
  }

  loadBadges(): void {
    this.isLoading = true;
    this.badgeService.getAllBadges().subscribe({
      next: (data: Badge[]) => {
        this.badges = data;
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        console.error('Erreur chargement badges:', error);
        this.errorMessage = 'Impossible de charger les badges';
        this.isLoading = false;
      }
    });
  }

  getBadgeIcon(badgeName: string): string {
    const icons: {[key: string]: string} = {
      'Expert': '🏆',
      'Confirmé': '🥈',
      'Débutant': '🌱',
      'TOP 1%': '👑',
      'Rapide': '⚡',
      'Communication': '💬',
      'Qualité': '✨',
      'Or': '🥇',
      'Argent': '🥈',
      'Bronze': '🥉'
    };
    return icons[badgeName] || '🎖️';
  }
}