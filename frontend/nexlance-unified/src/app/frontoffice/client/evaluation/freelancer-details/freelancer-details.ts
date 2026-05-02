import { Component, OnInit, AfterViewInit, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { EvaluationService } from '@core/services/evaluation.service';
import { Chart, registerables } from 'chart.js';
Chart.register(...registerables);

@Component({
  selector: 'app-freelancer-details',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './freelancer-details.html',
  styleUrls: ['./freelancer-details.css']
})
export class FreelancerDetailsComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('radarChart') radarChartRef!: ElementRef;
  @ViewChild('doughnutChart') doughnutChartRef!: ElementRef;
  @ViewChild('lineChart') lineChartRef!: ElementRef;

  freelancerEmail: string = '';
  details: any = null;
  isLoading = true;
  errorMessage = '';
  showAllEvaluations = false;
  
  private charts: Chart[] = [];

  constructor(
    private route: ActivatedRoute,
    private evaluationService: EvaluationService
  ) {}

  ngOnInit(): void {
    this.freelancerEmail = this.route.snapshot.paramMap.get('email') || '';
    if (this.freelancerEmail) {
      this.loadDetails();
    }
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.createCharts(), 500);
  }

  get displayedEvaluations(): any[] {
    if (!this.details?.evaluations) return [];
    return this.showAllEvaluations 
      ? this.details.evaluations 
      : this.details.evaluations.slice(0, 5);
  }

  toggleEvaluations(): void {
    this.showAllEvaluations = !this.showAllEvaluations;
  }

  loadDetails(): void {
    this.isLoading = true;
    this.evaluationService.getFreelancerDetails(this.freelancerEmail).subscribe({
      next: (data) => {
        this.details = data;
        this.isLoading = false;
        setTimeout(() => this.createCharts(), 300);
      },
      error: (error) => {
        this.errorMessage = 'Error loading details';
        this.isLoading = false;
        console.error('Error:', error);
      }
    });
  }

  createCharts(): void {
    this.charts.forEach(c => c.destroy());
    this.charts = [];

    if (!this.details || !this.details.evaluations?.length) return;

    try {
      // RADAR CHART
      if (this.radarChartRef?.nativeElement) {
        const evaluations = this.details.evaluations;
        const avgQuality = this.calculateAverage(evaluations, 'qualityScore');
        const avgDeadline = this.calculateAverage(evaluations, 'deadlineScore');
        const avgComm = this.calculateAverage(evaluations, 'communicationScore');
        const avgProf = this.calculateAverage(evaluations, 'professionalismScore');

        this.charts.push(new Chart(this.radarChartRef.nativeElement, {
          type: 'radar',
          data: {
            labels: ['Quality', 'Deadline', 'Communication', 'Professionalism'],
            datasets: [{
              label: 'Average Score',
              data: [avgQuality, avgDeadline, avgComm, avgProf],
              backgroundColor: 'rgba(99, 102, 241, 0.2)',
              borderColor: '#6366f1',
              pointBackgroundColor: '#6366f1',
              pointBorderColor: '#fff',
              borderWidth: 2
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
              r: {
                min: 0,
                max: 5,
                ticks: { display: true, stepSize: 1 },
                pointLabels: { font: { size: 11 } }
              }
            },
            plugins: { legend: { display: false } }
          }
        }));
      }

      // DOUGHNUT CHART
      if (this.doughnutChartRef?.nativeElement) {
        const dist = this.details.distribution || {};
        
        this.charts.push(new Chart(this.doughnutChartRef.nativeElement, {
          type: 'doughnut',
          data: {
            labels: ['5★', '4★', '3★', '2★', '1★'],
            datasets: [{
              data: [
                dist['5stars'] || 0,
                dist['4stars'] || 0,
                dist['3stars'] || 0,
                dist['2stars'] || 0,
                dist['1star'] || 0
              ],
              backgroundColor: ['#22c55e', '#84cc16', '#eab308', '#f97316', '#ef4444'],
              borderWidth: 0
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '65%',
            plugins: { legend: { position: 'bottom' } }
          }
        }));
      }

      // LINE CHART
      if (this.lineChartRef?.nativeElement && this.details.evaluations.length >= 2) {
        const sorted = [...this.details.evaluations]
          .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
          .slice(-8);

        this.charts.push(new Chart(this.lineChartRef.nativeElement, {
          type: 'line',
          data: {
            labels: sorted.map(e => new Date(e.createdAt).toLocaleDateString('en-US', { day: '2-digit', month: 'short' })),
            datasets: [{
              label: 'Rating',
              data: sorted.map(e => e.ratingGlobal),
              borderColor: '#6366f1',
              backgroundColor: 'rgba(99, 102, 241, 0.1)',
              tension: 0.3,
              fill: true,
              pointBackgroundColor: '#6366f1'
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
              y: { min: 0, max: 5, grid: { color: '#e2e8f0' } }
            },
            plugins: { legend: { display: false } }
          }
        }));
      }
    } catch (error) {
      console.error('Error creating charts:', error);
    }
  }

  calculateAverage(evaluations: any[], field: string): number {
    if (!evaluations || evaluations.length === 0) return 0;
    const sum = evaluations.reduce((acc, e) => acc + (e[field] || 0), 0);
    return Math.round((sum / evaluations.length) * 10) / 10;
  }

  getStars(rating: number): number[] {
    return Array(5).fill(0).map((_, i) => i + 1);
  }

  getRatingPercent(rating: number): number {
    return (rating / 5) * 100;
  }

  goBack(): void {
    window.history.back();
  }

  ngOnDestroy(): void {
    this.charts.forEach(c => c.destroy());
  }
}