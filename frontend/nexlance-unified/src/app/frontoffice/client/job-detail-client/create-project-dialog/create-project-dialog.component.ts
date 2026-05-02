import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';

export interface CreateProjectDialogData {
  jobOffer: any;
  application: any;
}

export interface MilestoneInput {
  title: string;
  description: string;
  dueDate: string;
  requiresDocuments: boolean;
  deliverables: string;
  acceptanceCriteria: string;
}

export interface CreateProjectDialogResult {
  title: string;
  startDate: string;
  endDate: string;
  requirements: string;
  deliverables: string;
  milestones: MilestoneInput[];
}

@Component({
  selector: 'app-create-project-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatButtonModule
  ],
  templateUrl: './create-project-dialog.component.html',
  styleUrls: ['./create-project-dialog.component.scss']
})
export class CreateProjectDialogComponent {
  projectTitle: string = '';
  startDate: string = '';
  endDate: string = '';
  requirements: string = '';
  deliverables: string = '';

  milestones: MilestoneInput[] = [
    {
      title: '',
      description: '',
      dueDate: '',
      requiresDocuments: false,
      deliverables: '',
      acceptanceCriteria: ''
    }
  ];

  constructor(
    public dialogRef: MatDialogRef<CreateProjectDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CreateProjectDialogData
  ) {
    // Pre-fill from job offer
    if (data.jobOffer) {
      this.projectTitle = data.jobOffer.title;
      this.requirements = data.jobOffer.description || '';

      const now = new Date();
      this.startDate = now.toISOString().split('T')[0];

      if (data.jobOffer.estimatedDuration) {
        const end = new Date(now);
        end.setDate(end.getDate() + data.jobOffer.estimatedDuration);
        this.endDate = end.toISOString().split('T')[0];
      }
    }
  }

  addMilestone(): void {
    this.milestones.push({
      title: '',
      description: '',
      dueDate: '',
      requiresDocuments: false,
      deliverables: '',
      acceptanceCriteria: ''
    });
  }

  removeMilestone(index: number): void {
    if (this.milestones.length > 1) {
      this.milestones.splice(index, 1);
    }
  }

  isFormValid(): boolean {
    if (!this.projectTitle.trim() || !this.startDate || !this.endDate) {
      return false;
    }
    return this.milestones.every(m => m.title.trim() && m.dueDate);
  }

  onCancel(): void {
    this.dialogRef.close(null);
  }

  onSubmit(): void {
    if (!this.isFormValid()) return;

    const result: CreateProjectDialogResult = {
      title: this.projectTitle,
      startDate: this.startDate,
      endDate: this.endDate,
      requirements: this.requirements,
      deliverables: this.deliverables,
      milestones: this.milestones
    };
    this.dialogRef.close(result);
  }
}
