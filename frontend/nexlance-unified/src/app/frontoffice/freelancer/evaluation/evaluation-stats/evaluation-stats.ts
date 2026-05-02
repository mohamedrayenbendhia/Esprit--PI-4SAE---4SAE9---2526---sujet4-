import { Component, OnInit, OnDestroy, ChangeDetectorRef, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { EvaluationService } from '@core/services/evaluation.service';
import { Chart, registerables } from 'chart.js';
import { environment } from '../../../../../environments/environment';
Chart.register(...registerables);

@Component({
  selector: 'app-evaluation-stats',
  templateUrl: './evaluation-stats.html',
  styleUrls: ['./evaluation-stats.css'],
  standalone: true,
  imports: [CommonModule, RouterModule]
})
export class EvaluationStatsComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('radarCanvas') radarCanvasRef!: ElementRef;
  @ViewChild('doughnutCanvas') doughnutCanvasRef!: ElementRef;
  @ViewChild('lineCanvas') lineCanvasRef!: ElementRef;

  Math = Math;

  freelancerEmail: string = '';
  evaluations: any[] = [];
  _trend: any[] = [];
  
  stats: any = { 
    totalEvaluations: 0, 
    averageRating: 0,
    distribution: { '1': 0, '2': 0, '3': 0, '4': 0, '5': 0 },
    categoryAverages: { quality: 0, deadline: 0, communication: 0, professionalism: 0 }
  };
  
  get responseRate(): number {
    if (!this.evaluations || this.evaluations.length === 0) return 0;
    const responded = this.evaluations.filter(e => e.responseText).length;
    return Math.round((responded / this.evaluations.length) * 100);
  }

  get fiveStars(): number { return this.stats.distribution?.['5'] || 0; }
  get fourStars(): number { return this.stats.distribution?.['4'] || 0; }
  get threeStars(): number { return this.stats.distribution?.['3'] || 0; }
  get twoStars(): number { return this.stats.distribution?.['2'] || 0; }
  get oneStar(): number { return this.stats.distribution?.['1'] || 0; }

  get averageQuality(): number { return this.stats.categoryAverages?.quality || 0; }
  get averageDeadline(): number { return this.stats.categoryAverages?.deadline || 0; }
  get averageCommunication(): number { return this.stats.categoryAverages?.communication || 0; }
  get averageProfessionalism(): number { return this.stats.categoryAverages?.professionalism || 0; }

  isLoading = true;
  isLive = false;
  lastUpdated = '';
  isNewEval = false;
  
  private eventSource: EventSource | null = null;
  private charts: Chart[] = [];

  constructor(private evaluationService: EvaluationService, private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    const userStr = localStorage.getItem('current_user');
    if (userStr) {
      this.freelancerEmail = JSON.parse(userStr).email;
      this.loadInitialData();
      this.connectSSE();
    }
  }

  ngAfterViewInit() {
    setTimeout(() => this.drawCharts(), 500);
    this.cdr.detectChanges();
  }

  loadInitialData() {
    this.evaluationService.getFreelancerEvaluations(this.freelancerEmail).subscribe({
      next: (data) => {
        this.evaluations = data;
        this.calculateLocalStats();
        this.isLoading = false;
        this.cdr.detectChanges();
        setTimeout(() => this.drawCharts(), 300);
      },
      error: (error) => {
        console.error('Error loading data:', error);
        this.isLoading = false;
      }
    });
  }

  calculateLocalStats() {
    if (!this.evaluations || this.evaluations.length === 0) return;

    const sum = this.evaluations.reduce((acc, e) => acc + (e.ratingGlobal || 0), 0);
    this.stats.averageRating = Math.round((sum / this.evaluations.length) * 10) / 10;
    
    this.stats.distribution = { '1': 0, '2': 0, '3': 0, '4': 0, '5': 0 };
    this.evaluations.forEach(e => {
      const rating = Math.round(e.ratingGlobal || 0);
      if (rating >= 1 && rating <= 5) {
        this.stats.distribution[rating.toString()]++;
      }
    });

    let qualitySum = 0, deadlineSum = 0, commSum = 0, profSum = 0;
    let qualityCount = 0, deadlineCount = 0, commCount = 0, profCount = 0;

    this.evaluations.forEach(e => {
      if (e.qualityScore) { qualitySum += e.qualityScore; qualityCount++; }
      if (e.deadlineScore) { deadlineSum += e.deadlineScore; deadlineCount++; }
      if (e.communicationScore) { commSum += e.communicationScore; commCount++; }
      if (e.professionalismScore) { profSum += e.professionalismScore; profCount++; }
    });

    this.stats.categoryAverages = {
      quality: qualityCount > 0 ? Math.round((qualitySum / qualityCount) * 10) / 10 : 0,
      deadline: deadlineCount > 0 ? Math.round((deadlineSum / deadlineCount) * 10) / 10 : 0,
      communication: commCount > 0 ? Math.round((commSum / commCount) * 10) / 10 : 0,
      professionalism: profCount > 0 ? Math.round((profSum / profCount) * 10) / 10 : 0
    };
  }

  connectSSE() {
    const url = `${environment.evaluationApiUrl}/freelancer/evaluations/stats/${encodeURIComponent(this.freelancerEmail)}/stream`;
    this.eventSource = new EventSource(url);

    this.eventSource.addEventListener('stats', (event: any) => {
      const data = JSON.parse(event.data);
      
      this.stats = {
        totalEvaluations: data.totalEvaluations || 0,
        averageRating: data.averageRating || 0,
        distribution: data.distribution || { '1': 0, '2': 0, '3': 0, '4': 0, '5': 0 },
        categoryAverages: data.categoryAverages || { quality: 0, deadline: 0, communication: 0, professionalism: 0 }
      };
      
      this.evaluations = data.recentEvaluations || [];
      this.isLive = true;
      this.isLoading = false;
      this.lastUpdated = new Date().toLocaleTimeString('fr-FR');
      this.isNewEval = true;
      setTimeout(() => this.isNewEval = false, 1000);
      
      this.cdr.detectChanges();
      setTimeout(() => this.drawCharts(), 100);
    });

    this.eventSource.onerror = (error) => {
      console.error('SSE Error:', error);
      this.isLive = false;
      this.cdr.detectChanges();
    };
  }

  drawCharts() {
    this.charts.forEach(c => c.destroy());
    this.charts = [];

    if (!this.radarCanvasRef?.nativeElement || !this.doughnutCanvasRef?.nativeElement) return;

    try {
      // RADAR CHART
      this.charts.push(new Chart(this.radarCanvasRef.nativeElement, {
        type: 'radar',
        data: {
          labels: ['Quality', 'Deadline', 'Communication', 'Professionalism'],
          datasets: [{
            label: 'Average Score',
            data: [
              this.averageQuality, 
              this.averageDeadline, 
              this.averageCommunication, 
              this.averageProfessionalism
            ],
            backgroundColor: 'rgba(99, 102, 241, 0.2)',
            borderColor: '#6366f1',
            pointBackgroundColor: '#6366f1',
            pointBorderColor: '#fff',
            pointHoverBackgroundColor: '#fff',
            pointHoverBorderColor: '#6366f1',
            borderWidth: 2
          }]
        },
        options: {
          scales: { 
            r: { 
              min: 0, 
              max: 5, 
              ticks: { display: false, stepSize: 1 },
              grid: { color: '#e2e8f0' },
              pointLabels: { font: { size: 11, weight: 600 } }
            } 
          },
          plugins: { legend: { display: false } }
        }
      }));

      // DOUGHNUT CHART
      this.charts.push(new Chart(this.doughnutCanvasRef.nativeElement, {
        type: 'doughnut',
        data: {
          labels: ['5★', '4★', '3★', '2★', '1★'],
          datasets: [{
            data: [this.fiveStars, this.fourStars, this.threeStars, this.twoStars, this.oneStar],
            backgroundColor: ['#22c55e', '#84cc16', '#eab308', '#f97316', '#ef4444'],
            borderWidth: 0,
            hoverOffset: 4
          }]
        },
        options: { 
          cutout: '70%', 
          plugins: { legend: { display: false } }
        }
      }));

      // LINE CHART
      if (this.lineCanvasRef?.nativeElement && this.evaluations.length >= 2) {
        const sorted = [...this.evaluations].sort((a, b) => 
          new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
        );
        
        this.charts.push(new Chart(this.lineCanvasRef.nativeElement, {
          type: 'line',
          data: {
            labels: sorted.map(e => new Date(e.createdAt).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' })),
            datasets: [{
              label: 'Rating Evolution',
              data: sorted.map(e => e.ratingGlobal || 0),
              borderColor: '#6366f1',
              backgroundColor: 'rgba(99, 102, 241, 0.1)',
              tension: 0.3,
              fill: true,
              pointBackgroundColor: '#6366f1',
              pointBorderColor: '#fff',
              pointRadius: 4,
              pointHoverRadius: 6
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
              y: { min: 0, max: 5, grid: { color: '#e2e8f0' } },
              x: { grid: { display: false } }
            },
            plugins: { legend: { display: false } }
          }
        }));
      }
    } catch (error) {
      console.error('Error creating charts:', error);
    }
  }

  ngOnDestroy() {
    if (this.eventSource) this.eventSource.close();
    this.charts.forEach(c => c.destroy());
  }

  safeDiv(value: number, total: number): number {
    return total > 0 ? (value / total) * 100 : 0;
  }
  
  getRatingPercent(r: number): number {
    return (r / 5) * 100;
  }
}