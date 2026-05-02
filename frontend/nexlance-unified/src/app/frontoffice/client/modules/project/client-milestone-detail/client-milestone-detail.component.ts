import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ProjectService } from '../../../../../core/services/project.service';
import { ProjectMilestone } from '../../../../../core/models/project.model';

@Component({
  selector: 'app-client-milestone-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule
  ],
  templateUrl: './client-milestone-detail.component.html',
  styleUrls: ['./client-milestone-detail.component.scss']
})
export class ClientMilestoneDetailComponent implements OnInit {
  milestone: ProjectMilestone | null = null;
  projectId: string | null = null;
  rejectionReason: string = '';
  loading = true;

  constructor(
    private route: ActivatedRoute,
    private projectService: ProjectService
  ) {}

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

  approveMilestone(): void {
    if (!this.milestone?.id) return;
    
    if (confirm('Are you sure you want to approve this milestone?')) {
      this.projectService.approveMilestone(this.milestone.id).subscribe({
        next: () => {
          alert('Milestone approved successfully');
          window.history.back();
        },
        error: (error) => {
          console.error('Error approving milestone:', error);
          alert('Error approving milestone');
        }
      });
    }
  }

  requestRevisions(): void {
    if (!this.milestone?.id || !this.rejectionReason.trim()) {
      alert('Please provide a reason for requesting revisions');
      return;
    }
    
    this.projectService.rejectMilestone(this.milestone.id, this.rejectionReason).subscribe({
      next: () => {
        alert('Revisions requested');
        window.history.back();
      },
      error: (error) => {
        console.error('Error requesting revisions:', error);
        alert('Error requesting revisions');
      }
    });
  }

  getAttachments(): string[] {
    if (!this.milestone?.attachments) return [];
    try {
      const parsed = JSON.parse(this.milestone.attachments);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }

  getFileNameFromUrl(url: string): string {
    try {
      const parts = url.split('/');
      return decodeURIComponent(parts[parts.length - 1]);
    } catch {
      return 'File';
    }
  }

  getFileIconFromUrl(url: string): string {
    const name = this.getFileNameFromUrl(url).toLowerCase();
    if (/\.(png|jpg|jpeg|gif|webp|svg|bmp)$/.test(name)) return 'image';
    if (/\.pdf$/.test(name)) return 'picture_as_pdf';
    if (/\.(doc|docx)$/.test(name)) return 'article';
    return 'insert_drive_file';
  }
}
