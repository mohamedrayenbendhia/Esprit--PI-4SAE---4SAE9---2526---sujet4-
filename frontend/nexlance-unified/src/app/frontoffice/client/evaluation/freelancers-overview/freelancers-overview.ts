import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { EvaluationService } from '@core/services/evaluation.service';

@Component({
  selector: 'app-freelancers-overview',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './freelancers-overview.html',
  styleUrls: ['./freelancers-overview.css']
})
export class FreelancersOverviewComponent implements OnInit {
  freelancers: any = {};
  isLoading = true;
  errorMessage = '';

  constructor(private evaluationService: EvaluationService) {}

  ngOnInit(): void {
    this.loadOverview();
  }

  loadOverview(): void {
    this.isLoading = true;
    this.evaluationService.getAllFreelancersOverview().subscribe({
      next: (data) => {
        this.freelancers = data;
        this.isLoading = false;
      },
      error: (error) => {
        this.errorMessage = 'Error loading freelancers';
        this.isLoading = false;
        console.error('Error:', error);
      }
    });
  }

  getStars(rating: number): number[] {
    return Array(5).fill(0).map((_, i) => i + 1);
  }

  getRatingPercent(rating: number): number {
    return (rating / 5) * 100;
  }
}