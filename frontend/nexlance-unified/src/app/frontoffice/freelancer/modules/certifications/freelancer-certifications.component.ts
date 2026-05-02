import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
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
import { MatStepperModule } from '@angular/material/stepper';
import { MatRadioModule } from '@angular/material/radio';
import { CertificationService } from '../../../../core/services/certification.service';
import { CompetenceService } from '../../../../core/services/competence.service';
import { Certification, TestPublic, UserTestResult } from '../../../../shared/models/certification.model';
import { UserSkill } from '../../../../shared/models/competence.model';
import jsPDF from 'jspdf';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-freelancer-certifications',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSnackBarModule, MatTooltipModule,
    MatStepperModule, MatRadioModule
  ],
  templateUrl: './freelancer-certifications.component.html',
  styleUrls: ['./freelancer-certifications.component.scss']
})
export class FreelancerCertificationsComponent implements OnInit, OnDestroy {
  private certService = inject(CertificationService);
  private competenceService = inject(CompetenceService);
  private snackBar = inject(MatSnackBar);
  private authService = inject(AuthService);

  certifications = signal<Certification[]>([]);
  mySkills = signal<UserSkill[]>([]);
  loading = signal(false);

  // Test taking
  showTestPanel = signal(false);
  selectedSkill = signal<UserSkill | null>(null);
  currentTest = signal<TestPublic | null>(null);
  answers = signal<string[]>([]);
  testResult = signal<UserTestResult | null>(null);
  loadingTest = signal(false);
  submitted = signal(false);

  // Cooldown — Map par skillId
  cooldownSkillIds = signal<Set<number>>(new Set());
  cooldownIntervals = new Map<number, any>();
  remainingTimes = signal<Record<number, string>>({});

  ngOnInit(): void {
  this.loadCertificationsFirst();
}
loadCertificationsFirst(): void {
  this.loading.set(true);
  this.certService.getMyCertifications().subscribe({
    next: (data) => {
      this.certifications.set(data);
      this.loading.set(false);
      this.loadMySkills(); // ← charge skills APRÈS certifications
    },
    error: () => {
      this.snackBar.open('Error loading certifications', 'Close', { duration: 3000 });
      this.loading.set(false);
      this.loadMySkills();
    }
  });
}

loadCertifications(): void {
  this.certService.getMyCertifications().subscribe({
    next: (data) => this.certifications.set(data)
  });
}

  loadMySkills(): void {
    this.competenceService.getMySkills().subscribe({
      next: (skills) => {
        this.mySkills.set(skills);
        skills.forEach(skill => {
          this.certService.checkCooldown(skill.skill.id).subscribe({
            next: (res) => {
              if (res.onCooldown && res.lastAttemptAt) {
                this.cooldownSkillIds.update(ids => new Set([...ids, skill.skill.id]));
                this.startCountdown(skill.skill.id, res.lastAttemptAt);
              }
            },
            error: () => {}
          });
        });
      }
    });
  }

  openTestPanel(): void {
    this.showTestPanel.set(true);
    this.selectedSkill.set(null);
    this.currentTest.set(null);
    this.answers.set([]);
    this.testResult.set(null);
    this.submitted.set(false);
  }

  closeTestPanel(): void {
    this.showTestPanel.set(false);
  }

  onSkillClick(skill: UserSkill): void {
    if (this.isCertified(skill) || this.isCooldown(skill)) return;
    this.selectSkill(skill);
  }

  selectSkill(skill: UserSkill): void {
    // Vérifier si déjà certifié en comparant avec userSkillId (qui est le skill.id)
    const alreadyCertified = this.certifications().some(
      c => c.userSkillId === skill.skill.id
    );
    if (alreadyCertified) {
      this.snackBar.open('You already have a certification for this skill!', 'Close', { duration: 3000 });
      return;
    }

    if (this.isCooldown(skill)) {
      this.snackBar.open(`You must wait ${this.getRemainingTime(skill.skill.id)} before retrying!`, 'Close', { duration: 4000 });
      return;
    }

    this.certService.checkCooldown(skill.skill.id).subscribe({
      next: (res) => {
        if (res.onCooldown && res.lastAttemptAt) {
          this.cooldownSkillIds.update(ids => new Set([...ids, skill.skill.id]));
          this.startCountdown(skill.skill.id, res.lastAttemptAt);
          this.snackBar.open('You must wait 24h before retrying!', 'Close', { duration: 4000 });
          return;
        }
        this.loadTest(skill);
      },
      error: () => this.loadTest(skill)
    });
  }

  private loadTest(skill: UserSkill): void {
    this.selectedSkill.set(skill);
    this.loadingTest.set(true);
    this.certService.getTestBySkill(skill.skill.id).subscribe({
      next: (test) => {
        this.currentTest.set(test);
        this.answers.set(new Array(test.questions.length).fill(''));
        this.loadingTest.set(false);
      },
      error: () => {
        this.snackBar.open('No test available for this skill', 'Close', { duration: 3000 });
        this.loadingTest.set(false);
      }
    });
  }

  setAnswer(index: number, value: string): void {
    const updated = [...this.answers()];
    updated[index] = value;
    this.answers.set(updated);
  }

  canSubmit(): boolean {
    return this.answers().length > 0 && this.answers().every(a => a.trim() !== '');
  }

  submitTest(): void {
    const skill = this.selectedSkill();
    const test = this.currentTest();
    if (!skill || !test) return;

    this.certService.submitTest({
      userId: 'placeholder',
      userSkillId: skill.skill.id,
      answers: this.answers()
    }).subscribe({
      next: (result) => {
        this.testResult.set(result);
        this.submitted.set(true);
        this.loadCertifications();
        if (!result.isPassed) {
          this.cooldownSkillIds.update(ids => new Set([...ids, skill.skill.id]));
          this.startCountdown(skill.skill.id, new Date().toISOString());
        }
        this.snackBar.open('Test submitted!', 'Close', { duration: 3000 });
      },
      error: (err) => {
        const msg = err?.error?.message || '';
        if (msg.startsWith('COOLDOWN:')) {
          this.cooldownSkillIds.update(ids => new Set([...ids, skill.skill.id]));
          this.certService.checkCooldown(skill.skill.id).subscribe({
            next: (res) => {
              if (res.lastAttemptAt) {
                this.startCountdown(skill.skill.id, res.lastAttemptAt);
              }
            }
          });
          this.snackBar.open('You must wait 24h before retrying!', 'Close', { duration: 4000 });
        } else {
          this.snackBar.open('Error submitting test', 'Close', { duration: 3000 });
        }
      }
    });
  }

  isCooldown(skill: UserSkill): boolean {
    return this.cooldownSkillIds().has(skill.skill.id);
  }

  isCertified(skill: UserSkill): boolean {
    // userSkillId dans la certification correspond directement à skill.id
    return this.certifications().some(c => c.userSkillId === skill.skill.id);
  }

  getRemainingTime(skillId: number): string {
    return this.remainingTimes()[skillId] || '';
  }

  startCountdown(skillId: number, lastAttemptAt: string): void {
    if (this.cooldownIntervals.has(skillId)) {
      clearInterval(this.cooldownIntervals.get(skillId));
    }

    const interval = setInterval(() => {
      const last = new Date(lastAttemptAt).getTime();
      const now = new Date().getTime();
      const diff = 2 * 60 * 1000 - (now - last);

      if (diff <= 0) {
        clearInterval(interval);
        this.cooldownIntervals.delete(skillId);
        this.cooldownSkillIds.update(ids => {
          const newIds = new Set(ids);
          newIds.delete(skillId);
          return newIds;
        });
        this.remainingTimes.update(times => {
          const t = { ...times };
          delete t[skillId];
          return t;
        });
        return;
      }

      const hours = Math.floor(diff / (1000 * 60 * 60));
      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((diff % (1000 * 60)) / 1000);
      this.remainingTimes.update(times => ({
        ...times,
        [skillId]: `${hours}h ${minutes}m ${seconds}s`
      }));
    }, 1000);

    this.cooldownIntervals.set(skillId, interval);
  }

  getScoreColor(score: number): string {
    if (score >= 90) return '#2ecc71';
    if (score >= 70) return '#f5a623';
    return '#e53935';
  }

  passed(score: number, passingScore: number): boolean {
    return score >= passingScore;
  }

  viewCertificate(cert: Certification): void {
    const doc = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
    const w = doc.internal.pageSize.getWidth();
    const h = doc.internal.pageSize.getHeight();

    doc.setFillColor(245, 247, 255);
    doc.rect(0, 0, w, h, 'F');
    doc.setDrawColor(102, 126, 234);
    doc.setLineWidth(3);
    doc.rect(8, 8, w - 16, h - 16);
    doc.setDrawColor(118, 75, 162);
    doc.setLineWidth(1);
    doc.rect(12, 12, w - 24, h - 24);
    doc.setFillColor(102, 126, 234);
    doc.rect(8, 8, w - 16, 35, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(22);
    doc.setFont('helvetica', 'bold');
    doc.text('NEXLANCE', w / 2, 22, { align: 'center' });
    doc.setFontSize(11);
    doc.setFont('helvetica', 'normal');
    doc.text('Professional Certification Platform', w / 2, 32, { align: 'center' });
    doc.setTextColor(102, 126, 234);
    doc.setFontSize(28);
    doc.setFont('helvetica', 'bold');
    doc.text('CERTIFICATE OF ACHIEVEMENT', w / 2, 65, { align: 'center' });
    doc.setDrawColor(102, 126, 234);
    doc.setLineWidth(0.5);
    doc.line(40, 72, w - 40, 72);
    doc.setTextColor(100, 100, 100);
    doc.setFontSize(13);
    doc.setFont('helvetica', 'normal');
    doc.text('This is to certify that', w / 2, 85, { align: 'center' });
    doc.setTextColor(30, 30, 60);
    doc.setFontSize(26);
    doc.setFont('helvetica', 'bold');
    const user = this.authService.getCurrentUser();
    const fullName = user ? `${user.firstName} ${user.lastName}` : cert.userId.substring(0, 8);
    doc.text(fullName, w / 2, 100, { align: 'center' });
    doc.setTextColor(100, 100, 100);
    doc.setFontSize(13);
    doc.setFont('helvetica', 'normal');
    doc.text('has successfully completed and passed the certification test for', w / 2, 115, { align: 'center' });
    doc.setTextColor(118, 75, 162);
    doc.setFontSize(22);
    doc.setFont('helvetica', 'bold');
    doc.text(cert.testTitle, w / 2, 130, { align: 'center' });
    doc.setFillColor(102, 126, 234);
    doc.roundedRect(w / 2 - 30, 138, 60, 20, 5, 5, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(14);
    doc.setFont('helvetica', 'bold');
    doc.text(`Score: ${cert.score}%`, w / 2, 151, { align: 'center' });
    doc.setTextColor(100, 100, 100);
    doc.setFontSize(11);
    doc.setFont('helvetica', 'normal');
    const date = new Date(cert.date).toLocaleDateString('en-US', {
      year: 'numeric', month: 'long', day: 'numeric'
    });
    doc.text(`Issued on: ${date}`, w / 2, 170, { align: 'center' });
    doc.setFontSize(9);
    doc.setTextColor(150, 150, 150);
    doc.text(`Certificate ID: NEXLANCE-${cert.id}-${cert.userId.substring(0, 6).toUpperCase()}`, w / 2, 180, { align: 'center' });
    doc.setFillColor(102, 126, 234);
    doc.rect(8, h - 22, w - 16, 14, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(9);
    doc.text('www.nexlance.com  |  Powered by NexLance Platform', w / 2, h - 13, { align: 'center' });
    doc.save(`NexLance-Certificate-${cert.testTitle}-${cert.id}.pdf`);
  }

  ngOnDestroy(): void {
    this.cooldownIntervals.forEach(interval => clearInterval(interval));
  }
}