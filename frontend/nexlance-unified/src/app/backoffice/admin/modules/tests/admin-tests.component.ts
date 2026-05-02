import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { CertificationService } from '../../../../core/services/certification.service';
import { CompetenceService } from '../../../../core/services/competence.service';
import { Test, TestRequest, QuestionRequest } from '../../../../shared/models/certification.model';
import { Skill } from '../../../../shared/models/competence.model';

@Component({
  selector: 'app-admin-tests',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTableModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatSnackBarModule, MatExpansionModule,
    MatTooltipModule, MatChipsModule
  ],
  templateUrl: './admin-tests.component.html',
  styleUrls: ['./admin-tests.component.scss']
})
export class AdminTestsComponent implements OnInit {
  private certService = inject(CertificationService);
  private competenceService = inject(CompetenceService);
  private snackBar = inject(MatSnackBar);
  private fb = inject(FormBuilder);

  tests = signal<Test[]>([]);
  skills = signal<Skill[]>([]);
  loading = signal(false);
  showTestForm = signal(false);
  showQuestionForm = signal(false);
  editingTest = signal<Test | null>(null);
  selectedTest = signal<Test | null>(null);
  editingQuestionId = signal<number | null>(null);
  aiGenerating = signal(false);
  aiCount = signal(5);

  displayedColumns = ['title', 'skill', 'passingScore', 'questions', 'actions'];

  testForm: FormGroup = this.fb.group({
  title: ['', [Validators.required, Validators.minLength(3)]],
  skillId: [null, Validators.required],
  passingScore: [70, [Validators.required, Validators.min(1), Validators.max(100)]]
});

questionForm: FormGroup = this.fb.group({
  questionText: ['', [Validators.required, Validators.minLength(5)]],
  correctAnswer: ['', Validators.required]
});

  ngOnInit(): void {
    this.loadTests();
    this.loadSkills();
  }

  loadTests(): void {
    this.loading.set(true);
    this.certService.getAllTests().subscribe({
      next: (tests) => { this.tests.set(tests); this.loading.set(false); },
      error: () => { this.snackBar.open('Error loading tests', 'Close', { duration: 3000 }); this.loading.set(false); }
    });
  }

  loadSkills(): void {
    this.competenceService.getAllSkills().subscribe({
      next: (skills) => this.skills.set(skills)
    });
  }

  openCreateTestForm(): void {
    this.editingTest.set(null);
    this.testForm.reset({ passingScore: 70 });
    this.showTestForm.set(true);
  }

  openEditTestForm(test: Test): void {
    this.editingTest.set(test);
    this.testForm.patchValue(test);
    this.showTestForm.set(true);
  }

  closeTestForm(): void {
    this.showTestForm.set(false);
    this.editingTest.set(null);
    this.testForm.reset();
  }

  saveTest(): void {
    if (this.testForm.invalid) return;
    const request: TestRequest = this.testForm.value;
    const editing = this.editingTest();

    if (editing) {
      this.certService.updateTest(editing.id, request).subscribe({
        next: () => { this.snackBar.open('Test updated!', 'Close', { duration: 3000 }); this.closeTestForm(); this.loadTests(); },
        error: () => this.snackBar.open('Error updating test', 'Close', { duration: 3000 })
      });
    } else {
      this.certService.createTest(request).subscribe({
        next: () => { this.snackBar.open('Test created!', 'Close', { duration: 3000 }); this.closeTestForm(); this.loadTests(); },
        error: () => this.snackBar.open('Error creating test', 'Close', { duration: 3000 })
      });
    }
  }

  deleteTest(test: Test): void {
    if (!confirm(`Delete test "${test.title}"?`)) return;
    this.certService.deleteTest(test.id).subscribe({
      next: () => { this.snackBar.open('Test deleted!', 'Close', { duration: 3000 }); this.loadTests(); },
      error: () => this.snackBar.open('Error deleting test', 'Close', { duration: 3000 })
    });
  }

  selectTest(test: Test): void {
    this.selectedTest.set(test);
    this.showQuestionForm.set(false);
    this.questionForm.reset();
  }

  openAddQuestion(): void {
    this.editingQuestionId.set(null);
    this.questionForm.reset();
    this.showQuestionForm.set(true);
  }

  openEditQuestion(q: any): void {
  this.editingQuestionId.set(q.id);
  this.questionForm.patchValue({
    questionText: q.questionText,
    correctAnswer: q.correctAnswer
  });
  this.showQuestionForm.set(true);
}

  closeQuestionForm(): void {
    this.showQuestionForm.set(false);
    this.questionForm.reset();
    this.editingQuestionId.set(null);
  }

  saveQuestion(): void {
    if (this.questionForm.invalid) return;
    const v = this.questionForm.value;
    const options = [v.option0, v.option1, v.option2, v.option3].filter(o => o?.trim());
    const request: QuestionRequest = {
      questionText: v.questionText,
      correctAnswer: v.correctAnswer
    };

    const test = this.selectedTest();
    if (!test) return;

    const editId = this.editingQuestionId();
    if (editId) {
      this.certService.updateQuestion(editId, request).subscribe({
        next: () => { this.snackBar.open('Question updated!', 'Close', { duration: 3000 }); this.closeQuestionForm(); this.refreshSelectedTest(); },
        error: () => this.snackBar.open('Error updating question', 'Close', { duration: 3000 })
      });
    } else {
      this.certService.addQuestion(test.id, request).subscribe({
        next: () => { this.snackBar.open('Question added!', 'Close', { duration: 3000 }); this.closeQuestionForm(); this.refreshSelectedTest(); },
        error: () => this.snackBar.open('Error adding question', 'Close', { duration: 3000 })
      });
    }
  }

  deleteQuestion(questionId: number): void {
    if (!confirm('Delete this question?')) return;
    this.certService.deleteQuestion(questionId).subscribe({
      next: () => { this.snackBar.open('Question deleted!', 'Close', { duration: 3000 }); this.refreshSelectedTest(); },
      error: () => this.snackBar.open('Error deleting question', 'Close', { duration: 3000 })
    });
  }

  refreshSelectedTest(): void {
    const test = this.selectedTest();
    if (!test) return;
    this.certService.getTestById(test.id).subscribe({
      next: (updated) => {
        this.selectedTest.set(updated);
        this.tests.update(tests => tests.map(t => t.id === updated.id ? updated : t));
      }
    });
  }

  getSkillName(skillId: number): string {
    return this.skills().find(s => s.id === skillId)?.name || `Skill #${skillId}`;
  }

  generateWithAI(): void {
    const test = this.selectedTest();
    if (!test) return;
    const skillName = this.getSkillName(test.skillId);
    this.aiGenerating.set(true);
    this.certService.generateQuestionsWithAI(test.id, this.aiCount(), skillName).subscribe({
      next: () => {
        this.snackBar.open(`${this.aiCount()} questions generated!`, 'Close', { duration: 3000 });
        this.aiGenerating.set(false);
        this.refreshSelectedTest();
      },
      error: (err) => {
        const msg = err?.error?.error || 'AI generation failed';
        this.snackBar.open(msg, 'Close', { duration: 5000 });
        this.aiGenerating.set(false);
      }
    });
  }

  getScoreColor(score: number): string {
    if (score >= 80) return '#e53935';
    if (score >= 60) return '#f5a623';
    return '#43e97b';
  }
}