import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ProjectService } from '../../../../../core/services/project.service';
import { AuthService } from '../../../../../core/services/auth.service';
import { Project, ProjectStatus } from '../../../../../core/models/project.model';

@Component({
  selector: 'app-client-projects',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatIconModule,
    TranslateModule
  ],
  templateUrl: './client-projects.component.html',
  styleUrls: ['./client-projects.component.scss']
})
export class ClientProjectsComponent implements OnInit {
  projects: Project[] = [];
  loading = true;
  loadError = false;
  ProjectStatus = ProjectStatus;
  Math = Math;

  constructor(
    private projectService: ProjectService,
    private authService: AuthService,
    private translate: TranslateService
  ) {}

  ngOnInit(): void {
    this.loadProjects();
  }

  loadProjects(): void {
    const currentUser = this.authService.getCurrentUser();
    if (currentUser?.id) {
      this.loading = true;
      this.loadError = false;
      this.projectService.getProjectsByClientId(currentUser.id).subscribe({
        next: (projects) => {
          this.projects = projects;
          this.loadError = false;
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading projects:', error);
          this.projects = [];
          this.loadError = true;
          this.loading = false;
        }
      });
    } else {
      this.projects = [];
      this.loadError = true;
      this.loading = false;
    }
  }

  getStatusColor(status: ProjectStatus): string {
    return this.projectService.getStatusColor(status);
  }

  getStatusLabel(status: ProjectStatus): string {
    return this.translate.instant(`clientProjects.status.${status}`);
  }

  getDaysRemaining(endDate: Date | string): number {
    const end = new Date(endDate);
    const today = new Date();
    const diffTime = end.getTime() - today.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays;
  }

  getPendingMilestonesCount(project: Project): number {
    return project.milestones?.filter(m => m.status === 'SUBMITTED').length || 0;
  }
}
