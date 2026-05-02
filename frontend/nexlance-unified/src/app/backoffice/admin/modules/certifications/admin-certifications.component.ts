import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { CertificationService } from '../../../../core/services/certification.service';
import { UserService } from '../../../../core/services/user.service';
import { Certification } from '../../../../shared/models/certification.model';

@Component({
  selector: 'app-admin-certifications',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule, MatSnackBarModule, MatTableModule],
  templateUrl: './admin-certifications.component.html',
  styleUrls: ['./admin-certifications.component.scss']
})
export class AdminCertificationsComponent implements OnInit {
  private certService = inject(CertificationService);
  private userService = inject(UserService);
  private snackBar = inject(MatSnackBar);

  certifications = signal<Certification[]>([]);
  userNames = signal<Record<string, string>>({});
  loading = signal(false);
  displayedColumns = ['freelancer', 'testTitle', 'score', 'date'];

  ngOnInit(): void { this.loadCertifications(); }

  loadCertifications(): void {
    this.loading.set(true);
    this.certService.getAllCertifications().subscribe({
      next: (data) => {
        this.certifications.set(data);
        this.loading.set(false);
        const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
        data.forEach(c => {
          if (!this.userNames()[c.userId]) {
            if (!uuidPattern.test(c.userId)) {
              this.userNames.update(names => ({ ...names, [c.userId]: c.userId.substring(0, 8) + '...' }));
              return;
            }
            this.userService.getUserById(c.userId).subscribe({
              next: (user) => {
                this.userNames.update(names => ({
                  ...names,
                  [c.userId]: `${user.firstName} ${user.lastName}`
                }));
              },
              error: () => {
                this.userNames.update(names => ({
                  ...names,
                  [c.userId]: c.userId.substring(0, 8) + '...'
                }));
              }
            });
          }
        });
      },
      error: () => { this.snackBar.open('Error', 'Close', { duration: 3000 }); this.loading.set(false); }
    });
  }

  getName(userId: string): string {
    return this.userNames()[userId] || userId.substring(0, 8) + '...';
  }

  getScoreColor(score: number): string {
    if (score >= 90) return '#2ecc71';
    if (score >= 70) return '#f5a623';
    return '#e53935';
  }
}