import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CompetenceService } from '../../../../core/services/competence.service';
import { UserService } from '../../../../core/services/user.service';
import { Skill, UserSkill } from '../../../../shared/models/competence.model';

@Component({
  selector: 'app-client-skills',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatSnackBarModule, MatTooltipModule
  ],
  templateUrl: './client-skills.component.html',
  styleUrls: ['./client-skills.component.scss']
})
export class ClientSkillsComponent implements OnInit {
  private competenceService = inject(CompetenceService);
  private userService = inject(UserService);
  private snackBar = inject(MatSnackBar);

  skills = signal<Skill[]>([]);
  freelancers = signal<UserSkill[]>([]);
  selectedSkill = signal<Skill | null>(null);
  userNames = signal<Record<string, string>>({});
  loading = signal(false);
  loadingFreelancers = signal(false);
  endorsements = signal<Record<string, number>>({});
  hasEndorsedMap = signal<Record<string, boolean>>({});

  ngOnInit(): void {
    this.loadSkills();
  }

  loadSkills(): void {
    this.loading.set(true);
    this.competenceService.getAllSkillsClient().subscribe({
      next: (data) => { this.skills.set(data); this.loading.set(false); },
      error: () => { this.snackBar.open('Error loading skills', 'Close', { duration: 3000 }); this.loading.set(false); }
    });
  }

  selectSkill(skill: Skill): void {
    this.selectedSkill.set(skill);
    this.loadingFreelancers.set(true);
    this.competenceService.getFreelancersBySkill(skill.id).subscribe({
      next: (data) => {
        this.freelancers.set(data);
        this.loadingFreelancers.set(false);

        // Load user names with delay to avoid ERR_INSUFFICIENT_RESOURCES
        data.forEach((f, index) => {
          setTimeout(() => {
            this.userService.getUserById(f.userId).subscribe({
              next: (user) => {
                this.userNames.update(names => ({ ...names, [f.userId]: `${user.firstName} ${user.lastName}` }));
              },
              error: () => {
                this.userNames.update(names => ({ ...names, [f.userId]: f.userId.substring(0, 8) + '...' }));
              }
            });
          }, index * 100);
        });

        // Load endorsements with delay
        data.forEach((f, index) => {
          setTimeout(() => {
            this.competenceService.getEndorsementCount(f.userId, skill.id).subscribe({
              next: (res) => {
                this.endorsements.update(e => ({ ...e, [`${f.userId}_${skill.id}`]: res.endorsements }));
              }
            });
            this.competenceService.hasEndorsed(f.userId, skill.id).subscribe({
              next: (res) => {
                this.hasEndorsedMap.update(h => ({ ...h, [`${f.userId}_${skill.id}`]: res.hasEndorsed }));
              }
            });
          }, index * 150);
        });
      },
      error: () => { this.snackBar.open('Error loading freelancers', 'Close', { duration: 3000 }); this.loadingFreelancers.set(false); }
    });
  }

  getName(userId: string): string {
    return this.userNames()[userId] || userId.substring(0, 8) + '...';
  }

  back(): void {
    this.selectedSkill.set(null);
    this.freelancers.set([]);
    this.userNames.set({});
    this.endorsements.set({});
    this.hasEndorsedMap.set({});
  }

  getLevelColor(level: string): string {
    const colors: Record<string, string> = {
      BEGINNER: '#4facfe',
      INTERMEDIATE: '#f5a623',
      EXPERT: '#43e97b'
    };
    return colors[level] || '#667eea';
  }

  getEndorsementCount(userId: string, skillId: number): number {
    return this.endorsements()[`${userId}_${skillId}`] || 0;
  }

  isEndorsed(userId: string, skillId: number): boolean {
    return this.hasEndorsedMap()[`${userId}_${skillId}`] || false;
  }

  toggleEndorse(freelancer: UserSkill): void {
    const skillId = this.selectedSkill()!.id;
    const key = `${freelancer.userId}_${skillId}`;

    if (this.isEndorsed(freelancer.userId, skillId)) {
      this.competenceService.removeEndorsement(freelancer.userId, skillId).subscribe({
        next: (res) => {
          this.endorsements.update(e => ({ ...e, [key]: res.endorsements }));
          this.hasEndorsedMap.update(h => ({ ...h, [key]: false }));
          this.snackBar.open('Endorsement removed', 'Close', { duration: 2000 });
        }
      });
    } else {
      this.competenceService.endorseSkill(freelancer.userId, skillId).subscribe({
        next: (res) => {
          this.endorsements.update(e => ({ ...e, [key]: res.endorsements }));
          this.hasEndorsedMap.update(h => ({ ...h, [key]: true }));
          this.snackBar.open('Skill endorsed! ✅', 'Close', { duration: 2000 });
        },
        error: (err) => {
          this.snackBar.open(err?.error?.error || 'Error endorsing', 'Close', { duration: 3000 });
        }
      });
    }
  }
}