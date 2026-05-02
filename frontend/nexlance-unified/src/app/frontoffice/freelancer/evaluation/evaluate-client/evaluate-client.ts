import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { EvaluationService, Evaluation } from '@core/services/evaluation.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-evaluate-client',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './evaluate-client.html',
  styleUrls: ['./evaluate-client.css']
})
export class EvaluateClientComponent implements OnInit {
  clientEmail: string = '';
  projectId: string = '';
  freelancerId: string = '';
  
  evaluation: Partial<Evaluation> = {
    qualityScore: 0,
    deadlineScore: 0,
    communicationScore: 0,
    professionalismScore: 0,
    comment: ''
  };

  errorMessage: string = '';
  successMessage: string = '';
  isLoading: boolean = false;

  constructor(
    private router: Router,
    private evaluationService: EvaluationService
  ) {}

  ngOnInit(): void {
    console.log('📝 Formulaire prêt - Évaluer un client');
    
    // Récupérer l'ID du freelancer connecté
    try {
      const userStr = localStorage.getItem('current_user');
      if (userStr) {
        const user = JSON.parse(userStr);
        if (user && user.id) {
          this.freelancerId = user.id;
          console.log('✅ Freelancer connecté:', this.freelancerId);
        }
      }
    } catch (e) {
      console.error('❌ Erreur récupération freelancer:', e);
    }
  }

  setScore(type: string, score: number): void {
    switch(type) {
      case 'quality':
        this.evaluation.qualityScore = score;
        break;
      case 'deadline':
        this.evaluation.deadlineScore = score;
        break;
      case 'communication':
        this.evaluation.communicationScore = score;
        break;
      case 'professionalism':
        this.evaluation.professionalismScore = score;
        break;
    }
  }

  onSubmit(): void {
    this.submitEvaluation();
  }

  submitEvaluation(): void {
    // Validation de base
    if (!this.clientEmail || this.clientEmail.trim() === '') {
      this.errorMessage = 'Please enter client email';
      return;
    }

    if (!this.clientEmail.includes('@')) {
      this.errorMessage = 'Please enter a valid email';
      return;
    }

    if (!this.isValid()) {
      this.errorMessage = 'Please rate each criterion (1 to 5)';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    
    console.log('📡 Envoi évaluation pour client:', this.clientEmail);
    
    this.evaluationService.evaluateClient(
      this.clientEmail.trim(),
      this.evaluation as Evaluation,
      this.freelancerId,
      this.projectId || null
    ).subscribe({
      next: (response: Evaluation) => {
        this.isLoading = false;
        this.successMessage = '✅ Evaluation sent successfully!';
        console.log('✅ Réponse:', response);
        setTimeout(() => {
          // ✅ CORRECTION : chemin correct vers la liste des évaluations données
          this.router.navigate(['/frontoffice/freelancer/evaluations/given']);
        }, 2000);
      },
      error: (error: HttpErrorResponse) => {
        this.isLoading = false;
        console.error('❌ Erreur:', error);
        this.errorMessage = error.error?.message || 'Error sending evaluation';
      }
    });
  }

  isValid(): boolean {
    return (this.evaluation.qualityScore ?? 0) > 0 &&
           (this.evaluation.deadlineScore ?? 0) > 0 &&
           (this.evaluation.communicationScore ?? 0) > 0 &&
           (this.evaluation.professionalismScore ?? 0) > 0;
  }

  cancel(): void {
    this.router.navigate(['/frontoffice/freelancer/dashboard']);
  }
}