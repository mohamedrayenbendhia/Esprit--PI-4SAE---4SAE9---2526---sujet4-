import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { ProjectService } from '../../../../../core/services/project.service';
import { CloudinaryService, CloudinaryFile } from '../../../../../core/services/cloudinary.service';
import { PosthogService } from '../../../../../core/services/posthog.service';
import { ProjectMilestone } from '../../../../../core/models/project.model';

@Component({
  selector: 'app-freelancer-milestone-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatChipsModule,
    MatCheckboxModule
  ],
  templateUrl: './freelancer-milestone-detail.component.html',
  styleUrls: ['./freelancer-milestone-detail.component.scss']
})
export class FreelancerMilestoneDetailComponent implements OnInit {
  milestone: ProjectMilestone | null = null;
  projectId: string | null = null;
  submissionForm: FormGroup;
  loading = true;
  submitting = false;

  // Cloudinary deliverable uploads
  cloudinaryFiles: CloudinaryFile[] = [];
  isUploading = false;

  constructor(
    private route: ActivatedRoute,
    private projectService: ProjectService,
    private cloudinaryService: CloudinaryService,
    private posthogService: PosthogService,
    private fb: FormBuilder
  ) {
    this.submissionForm = this.fb.group({
      comment: [''],
      attachments: [''],
      confirmCriteria: [false, Validators.requiredTrue]
    });
  }

  ngOnInit(): void {
    this.projectId = this.route.snapshot.paramMap.get('id');
    const milestoneId = this.route.snapshot.paramMap.get('milestoneId');
    
    if (milestoneId) {
      this.loadMilestone(milestoneId);
    }
  }

  loadMilestone(id: string): void {
    this.loading = true;
    this.projectService.getMilestoneById(id).subscribe({
      next: (milestone) => {
        this.milestone = milestone;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading milestone:', error);
        this.loading = false;
      }
    });
  }

  onFileSelected(event: any): void {
    const files = event.target.files;
    if (files && files.length > 0) {
      const fileArray = Array.from(files) as File[];
      // Upload to Cloudinary for versioning
      this.isUploading = true;
      const folder = `nexlance/deliverables/${this.projectId}/${this.milestone?.id}`;
      this.cloudinaryService.uploadFiles(fileArray, folder).subscribe({
        next: (results) => {
          this.cloudinaryFiles.push(...results);
          this.submissionForm.patchValue({
            attachments: JSON.stringify(this.cloudinaryFiles.map(f => f.url))
          });
          this.isUploading = false;
          results.forEach(f => {
            this.posthogService.trackFileUpload('deliverable', {
              fileName: f.fileName, size: f.size, version: f.version
            });
          });
        },
        error: () => {
          // Fallback to local file names
          this.submissionForm.patchValue({
            attachments: JSON.stringify(fileArray.map(f => f.name))
          });
          this.isUploading = false;
        }
      });
    }
  }

  removeCloudinaryFile(index: number): void {
    this.cloudinaryFiles.splice(index, 1);
    this.submissionForm.patchValue({
      attachments: JSON.stringify(this.cloudinaryFiles.map(f => f.url))
    });
  }

  submitMilestone(): void {
    if (!this.milestone?.id || !this.submissionForm.valid) {
      alert('Please fill in all required fields');
      return;
    }

    this.submitting = true;
    const submissionData = {
      attachments: this.cloudinaryFiles.length > 0
        ? JSON.stringify(this.cloudinaryFiles.map(f => f.url))
        : (this.submissionForm.value.attachments || '[]'),
      comment: this.submissionForm.value.comment
    };

    this.posthogService.trackMilestone('submitted', {
      milestoneId: this.milestone.id,
      projectId: this.projectId,
      attachmentsCount: this.cloudinaryFiles.length
    });

    this.projectService.submitMilestone(this.milestone.id, submissionData).subscribe({
      next: () => {
        alert('Milestone submitted successfully!');
        window.history.back();
      },
      error: (error) => {
        console.error('Error submitting milestone:', error);
        alert('Error submitting milestone');
        this.submitting = false;
      }
    });
  }

  canSubmit(): boolean {
    return this.milestone?.status === 'IN_PROGRESS' || this.milestone?.status === 'REJECTED' || this.milestone?.status === 'PENDING';
  }

  getFileIcon(fileName: string): string {
    const ext = fileName.split('.').pop()?.toLowerCase() || '';
    if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'svg', 'bmp'].includes(ext)) return 'image';
    if (ext === 'pdf') return 'picture_as_pdf';
    if (['doc', 'docx'].includes(ext)) return 'article';
    return 'insert_drive_file';
  }
}
