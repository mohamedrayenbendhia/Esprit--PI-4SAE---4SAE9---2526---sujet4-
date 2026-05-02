import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CompetenceService } from '../../../../core/services/competence.service';
import { Skill, UserSkill, UserSkillRequest, SkillLevel } from '../../../../shared/models/competence.model';

@Component({
  selector: 'app-freelancer-skills',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSnackBarModule, MatTooltipModule
  ],
  templateUrl: './freelancer-skills.component.html',
  styleUrls: ['./freelancer-skills.component.scss']
})
export class FreelancerSkillsComponent implements OnInit {
  private competenceService = inject(CompetenceService);
  private snackBar = inject(MatSnackBar);
  private fb = inject(FormBuilder);

  mySkills = signal<UserSkill[]>([]);
  availableSkills = signal<Skill[]>([]);
  loading = signal(false);
  showForm = signal(false);

  skillLevels = Object.values(SkillLevel);

  skillForm: FormGroup = this.fb.group({
  skillId: [null, Validators.required],
  level: ['', Validators.required],
  yearsOfExperience: [0, [Validators.required, Validators.min(0), Validators.max(50)]]
});

  ngOnInit(): void {
    this.loadMySkills();
    this.loadAvailableSkills();
  }

  loadMySkills(): void {
    this.loading.set(true);
    this.competenceService.getMySkills().subscribe({
      next: (skills) => { this.mySkills.set(skills); this.loading.set(false); },
      error: () => { this.snackBar.open('Error loading skills', 'Close', { duration: 3000 }); this.loading.set(false); }
    });
  }

  loadAvailableSkills(): void {
    this.competenceService.getAvailableSkills().subscribe({
      next: (skills) => this.availableSkills.set(skills)
    });
  }

  openForm(): void {
    this.skillForm.reset({ yearsOfExperience: 1 });
    this.showForm.set(true);
  }

  closeForm(): void {
    this.showForm.set(false);
    this.skillForm.reset();
  }

  addSkill(): void {
    if (this.skillForm.invalid) return;
    const request: UserSkillRequest = this.skillForm.value;
    this.competenceService.addSkill(request).subscribe({
      next: () => {
        this.snackBar.open('Skill added!', 'Close', { duration: 3000 });
        this.closeForm();
        this.loadMySkills();
      },
      error: () => this.snackBar.open('Error adding skill', 'Close', { duration: 3000 })
    });
  }

  getLevelColor(level: SkillLevel): string {
    const colors: Record<string, string> = {
      BEGINNER: '#4facfe',
      INTERMEDIATE: '#f5a623',
      EXPERT: '#43e97b'
    };
    return colors[level] || '#667eea';
  }

  getLevelIcon(level: SkillLevel): string {
    const icons: Record<string, string> = {
      BEGINNER: 'school',
      INTERMEDIATE: 'trending_up',
      EXPERT: 'star'
    };
    return icons[level] || 'star';
  }

  getSkillName(skillId: number): string {
    return this.availableSkills().find(s => s.id === skillId)?.name || '';
  }

getExpertCount(): number {
  return this.mySkills().filter(s => s.level === SkillLevel.EXPERT).length;
}

  getAvgYears(): number {
    const skills = this.mySkills();
    if (!skills.length) return 0;
    return Math.round(skills.reduce((sum, s) => sum + s.yearsOfExperience, 0) / skills.length);
  }
}