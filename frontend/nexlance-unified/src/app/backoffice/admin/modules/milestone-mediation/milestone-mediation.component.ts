import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDividerModule } from '@angular/material/divider';
import { TranslateModule } from '@ngx-translate/core';
import { ProjectService } from '../../../../core/services/project.service';
import { ProjectMilestone } from '../../../../core/models/project.model';

@Component({
  selector: 'app-milestone-mediation',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    TranslateModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatDividerModule
  ],
  templateUrl: './milestone-mediation.component.html',
  styleUrls: ['./milestone-mediation.component.scss']
})
export class MilestoneMediationComponent implements OnInit {
  milestone: ProjectMilestone | null = null;
  projectId: string | null = null;
  adminNotes: string = '';
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

  forceApprove(): void {
    if (!this.milestone?.id) return;
    
    this.projectService.adminApproveMilestone(this.milestone.id, this.adminNotes).subscribe({
      next: () => {
        alert('Milestone approved successfully');
        window.history.back();
      },
      error: (error: any) => {
        console.error('Error approving milestone:', error);
        alert('Error approving the milestone');
      }
    });
  }

  requestRevisions(): void {
    if (!this.milestone?.id) return;
    
    this.projectService.adminRequestRevisions(this.milestone.id, this.adminNotes).subscribe({
      next: () => {
        alert('Revisions requested');
        window.history.back();
      },
      error: (error: any) => {
        console.error('Error requesting revisions:', error);
        alert('Error requesting revisions');
      }
    });
  }

  cancelMilestone(): void {
    if (!this.milestone?.id) return;
    
    if (confirm('Are you sure you want to cancel this milestone?')) {
      this.projectService.deleteMilestone(this.milestone.id).subscribe({
        next: () => {
          alert('Milestone cancelled');
          window.history.back();
        },
        error: (error) => {
          console.error('Error canceling milestone:', error);
          alert('Error cancelling the milestone');
        }
      });
    }
  }
}
