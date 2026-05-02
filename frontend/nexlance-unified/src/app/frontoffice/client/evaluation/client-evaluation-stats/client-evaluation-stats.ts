import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EvaluationService, Evaluation } from '@core/services/evaluation.service';

@Component({
  selector: 'app-client-evaluation-stats',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './client-evaluation-stats.html',
  styleUrls: ['./client-evaluation-stats.css']
})
export class ClientEvaluationStatsComponent implements OnInit {
  evaluations: Evaluation[] = [];
  clientEmail: string = '';
  isLoading = true;

  // ✅ Correction du type avec index signature
  stats = {
    total: 0,
    average: 0,
    distribution: { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 } as { [key: number]: number },
    byCategory: {
      quality: 0,
      deadline: 0,
      communication: 0,
      professionalism: 0
    }
  };

  constructor(private evaluationService: EvaluationService) {}

  ngOnInit(): void {
    const userStr = localStorage.getItem('current_user');
    if (userStr) {
      const user = JSON.parse(userStr);
      this.clientEmail = user.email;
      this.loadStats();
    }
  }

  loadStats(): void {
    this.evaluationService.getFreelancerEvaluations(this.clientEmail).subscribe({
      next: (data) => {
        this.evaluations = data;
        this.calculateStats();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erreur chargement stats:', err);
        this.isLoading = false;
      }
    });
  }

  calculateStats(): void {
    if (this.evaluations.length === 0) return;

    // Total
    this.stats.total = this.evaluations.length;

    // Moyenne globale
    const sum = this.evaluations.reduce((acc, e) => acc + (e.ratingGlobal || 0), 0);
    this.stats.average = Math.round((sum / this.evaluations.length) * 10) / 10;

    // Distribution - ✅ correction avec type number
    this.evaluations.forEach(e => {
      const rating = Math.round(e.ratingGlobal || 0);
      if (rating >= 1 && rating <= 5) {
        this.stats.distribution[rating]++; // ✅ Plus d'erreur
      }
    });

    // Moyennes par catégorie
    let qualitySum = 0, deadlineSum = 0, commSum = 0, profSum = 0;
    let qualityCount = 0, deadlineCount = 0, commCount = 0, profCount = 0;

    this.evaluations.forEach(e => {
      if (e.qualityScore) { qualitySum += e.qualityScore; qualityCount++; }
      if (e.deadlineScore) { deadlineSum += e.deadlineScore; deadlineCount++; }
      if (e.communicationScore) { commSum += e.communicationScore; commCount++; }
      if (e.professionalismScore) { profSum += e.professionalismScore; profCount++; }
    });

    this.stats.byCategory = {
      quality: qualityCount > 0 ? Math.round((qualitySum / qualityCount) * 10) / 10 : 0,
      deadline: deadlineCount > 0 ? Math.round((deadlineSum / deadlineCount) * 10) / 10 : 0,
      communication: commCount > 0 ? Math.round((commSum / commCount) * 10) / 10 : 0,
      professionalism: profCount > 0 ? Math.round((profSum / profCount) * 10) / 10 : 0
    };
  }
}