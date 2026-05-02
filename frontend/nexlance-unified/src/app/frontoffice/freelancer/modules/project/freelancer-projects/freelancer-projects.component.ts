import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { ProjectService } from '../../../../../core/services/project.service';
import { AuthService } from '../../../../../core/services/auth.service';
import { Project } from '../../../../../core/models/project.model';

@Component({
  selector: 'app-freelancer-projects',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatIconModule
  ],
  templateUrl: './freelancer-projects.component.html',
  styleUrls: ['./freelancer-projects.component.scss']
})
export class FreelancerProjectsComponent implements OnInit {
  projects: Project[] = [];
  loading = true;

  constructor(
    private projectService: ProjectService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadProjects();
  }

  loadProjects(): void {
    const currentUser = this.authService.getCurrentUser();
    const freelanceId = currentUser?.id;
    if (!freelanceId) return;
    
    this.loading = true;
    this.projectService.getProjectsByFreelanceId(freelanceId).subscribe({
      next: (projects) => {
        this.projects = projects;
        this.loading = false;
      },
      error: (error: any) => {
        console.error('Error loading projects:', error);
        this.loading = false;
      }
    });
  }

  getStatusColor(status: string): string {
    const colors: any = {
      'ACTIVE': 'primary',
      'ON_HOLD': 'warn',
      'COMPLETED': 'accent',
      'CANCELLED': ''
    };
    return colors[status] || '';
  }
}
