import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CompetenceService } from '../../../../core/services/competence.service';
import { Skill, SkillRequest } from '../../../../shared/models/competence.model';

@Component({
  selector: 'app-admin-skills',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTableModule, MatDialogModule, MatFormFieldModule,
    MatInputModule, MatSnackBarModule, MatChipsModule, MatTooltipModule
  ],
  templateUrl: './admin-skills.component.html',
  styleUrls: ['./admin-skills.component.scss']
})
export class AdminSkillsComponent implements OnInit {
  private competenceService = inject(CompetenceService);
  private snackBar = inject(MatSnackBar);
  private fb = inject(FormBuilder);

  skills = signal<Skill[]>([]);
  skillStats = signal<Record<string, any>[]>([]);
  globalStats = signal<Record<string, any>>({});
  loading = signal(false);
  showForm = signal(false);
  editingSkill = signal<Skill | null>(null);

  displayedColumns = ['name', 'actions'];

  skillForm: FormGroup = this.fb.group({
  name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]]
});

  ngOnInit(): void {
    this.loadSkills();
    this.loadStats();
  }

  loadStats(): void {
    this.competenceService.getSkillStats().subscribe({
      next: (stats) => this.skillStats.set(stats),
      error: () => {}
    });
    this.competenceService.getGlobalStats().subscribe({
      next: (stats) => this.globalStats.set(stats),
      error: () => {}
    });
  }

  loadSkills(): void {
    this.loading.set(true);
    this.competenceService.getAllSkills().subscribe({
      next: (skills) => {
        this.skills.set(skills);
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Error loading skills', 'Close', { duration: 3000 });
        this.loading.set(false);
      }
    });
  }

  openCreateForm(): void {
    this.editingSkill.set(null);
    this.skillForm.reset();
    this.showForm.set(true);
  }

  openEditForm(skill: Skill): void {
    this.editingSkill.set(skill);
    this.skillForm.patchValue(skill);
    this.showForm.set(true);
  }

  closeForm(): void {
    this.showForm.set(false);
    this.editingSkill.set(null);
    this.skillForm.reset();
  }

  saveSkill(): void {
    if (this.skillForm.invalid) return;
    const request: SkillRequest = this.skillForm.value;
    const editing = this.editingSkill();

    if (editing) {
      this.competenceService.updateSkill(editing.id, request).subscribe({
        next: () => {
          this.snackBar.open('Skill updated!', 'Close', { duration: 3000 });
          this.closeForm();
          this.loadSkills();
        },
        error: () => this.snackBar.open('Error updating skill', 'Close', { duration: 3000 })
      });
    } else {
      this.competenceService.createSkill(request).subscribe({
        next: () => {
          this.snackBar.open('Skill created!', 'Close', { duration: 3000 });
          this.closeForm();
          this.loadSkills();
        },
        error: () => this.snackBar.open('Error creating skill', 'Close', { duration: 3000 })
      });
    }
  }

  deleteSkill(skill: Skill): void {
    if (!confirm(`Delete skill "${skill.name}"?`)) return;
    this.competenceService.deleteSkill(skill.id).subscribe({
      next: () => {
        this.snackBar.open('Skill deleted!', 'Close', { duration: 3000 });
        this.loadSkills();
      },
      error: () => this.snackBar.open('Error deleting skill', 'Close', { duration: 3000 })
    });
  }

  getUniqueCategories(): number {
    return new Set(this.skills().map(s => s.category)).size;
  }

  getCategoryColor(category: string): string {
    const colors: Record<string, string> = {
      'Frontend': '#667eea',
      'Backend': '#43e97b',
      'Mobile': '#f093fb',
      'DevOps': '#f5a623',
      'Design': '#4facfe',
      'Data': '#ff6b6b'
    };
    return colors[category] || '#667eea';
  }
}