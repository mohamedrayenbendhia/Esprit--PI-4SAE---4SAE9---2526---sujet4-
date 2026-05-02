import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { CertificationService } from '../../../../core/services/certification.service';
import { Certification } from '../../../../shared/models/certification.model';
import { UserService } from '../../../../core/services/user.service';

interface FreelancerCert {
  userId: string;
  certifications: Certification[];
}

@Component({
  selector: 'app-client-certifications',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatSnackBarModule, MatTableModule
  ],
  templateUrl: './client-certifications.component.html',
  styleUrls: ['./client-certifications.component.scss']
})
export class ClientCertificationsComponent implements OnInit {
  private certService = inject(CertificationService);
  private userService = inject(UserService);
  private snackBar = inject(MatSnackBar);

  allCertifications = signal<Certification[]>([]);
  freelancerGroups = signal<FreelancerCert[]>([]);
  selectedFreelancer = signal<FreelancerCert | null>(null);
  userNames = signal<Record<string, string>>({});
  loading = signal(false);

  displayedColumns = ['testTitle', 'score', 'date'];

  ngOnInit(): void {
    this.loadAllCertifications();
  }

  loadAllCertifications(): void {
    this.loading.set(true);
    this.certService.getAllCertifications().subscribe({
      next: (data) => {
        this.allCertifications.set(data);
        this.groupByFreelancer(data);
        this.loading.set(false);
      },
      error: () => { this.snackBar.open('Error loading certifications', 'Close', { duration: 3000 }); this.loading.set(false); }
    });
  }

  groupByFreelancer(certs: Certification[]): void {
    const groups: Record<string, Certification[]> = {};
    certs.forEach(c => {
      if (!groups[c.userId]) groups[c.userId] = [];
      groups[c.userId].push(c);
    });
    const grouped = Object.entries(groups).map(([userId, certifications]) => ({ userId, certifications }));
    this.freelancerGroups.set(grouped);

    // Load user names with delay to avoid ERR_INSUFFICIENT_RESOURCES
    grouped.forEach((f, index) => {
      setTimeout(() => {
        this.userService.getUserById(f.userId).subscribe({
          next: (user) => {
            this.userNames.update(names => ({
              ...names,
              [f.userId]: `${user.firstName} ${user.lastName}`
            }));
          },
          error: () => {
            this.userNames.update(names => ({
              ...names,
              [f.userId]: f.userId.substring(0, 8) + '...'
            }));
          }
        });
      }, index * 100);
    });
  }

  getName(userId: string): string {
    return this.userNames()[userId] || userId.substring(0, 8) + '...';
  }

  selectFreelancer(f: FreelancerCert): void {
    this.selectedFreelancer.set(f);
  }

  back(): void {
    this.selectedFreelancer.set(null);
  }

  getScoreColor(score: number): string {
    if (score >= 90) return '#2ecc71';
    if (score >= 70) return '#f5a623';
    return '#e53935';
  }

  getAvgScore(certs: Certification[]): number {
    return Math.round(certs.reduce((s, c) => s + c.score, 0) / certs.length);
  }
}