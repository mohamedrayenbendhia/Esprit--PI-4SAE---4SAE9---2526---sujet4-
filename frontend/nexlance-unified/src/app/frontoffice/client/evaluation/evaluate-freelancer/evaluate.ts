import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { EvaluationService, Evaluation } from '@core/services/evaluation.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-evaluate-freelancer',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './evaluate.html',
  styleUrls: ['./evaluate.css']
})
export class EvaluateFreelancerComponent implements OnInit {
  freelancerId: string = '';
  projectId: string = '';
  clientId: string = '';
  
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
    console.log('Form ready - Enter freelancer email');
    
    // Get connected client ID
    try {
      const userStr = localStorage.getItem('current_user');
      if (userStr) {
        const user = JSON.parse(userStr);
        if (user && user.id) {
          this.clientId = user.id;
          console.log(' Connected client:', this.clientId);
        }
      }
    } catch (e) {
      console.error(' Error getting client:', e);
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
    // Basic validation
    if (!this.freelancerId || this.freelancerId.trim() === '') {
      this.errorMessage = ' Please enter freelancer email';
      return;
    }

    if (!this.freelancerId.includes('@')) {
      this.errorMessage = ' Please enter a valid email';
      return;
    }

    if (!this.isValid()) {
      this.errorMessage = ' Please rate each criterion (1 to 5)';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    
    console.log(' Sending evaluation for:', this.freelancerId);
    
    this.evaluationService.evaluateFreelancer(
      this.freelancerId.trim(),
      this.evaluation as Evaluation,
      this.clientId,
      this.projectId || null
    ).subscribe({
      next: (response: Evaluation) => {
        this.isLoading = false;
        this.successMessage = ' Evaluation sent successfully!';
        console.log(' Response:', response);
        setTimeout(() => {
          this.router.navigate(['/frontoffice/client/evaluations/given']);
        }, 2000);
      },
      error: (error: HttpErrorResponse) => {
        this.isLoading = false;
        console.error(' Error:', error);
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
    this.router.navigate(['/frontoffice/client/dashboard']);
  }
}