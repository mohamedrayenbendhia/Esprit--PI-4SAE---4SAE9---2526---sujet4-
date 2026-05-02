import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Project,
  ProjectMilestone,
  ProjectStatus,
  MilestoneStatus,
  CreateProjectRequest,
  MilestoneSubmission,
  MilestoneReview
} from '../models/project.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  private apiUrl = environment.projectsApiUrl;
  private projectsUrl = `${this.apiUrl}/projects`;
  private milestonesUrl = `${this.apiUrl}/milestones`;

  constructor(private http: HttpClient) {}

  // ===== PROJECT ENDPOINTS =====

  /**
   * Create a new project
   */
  createProject(project: CreateProjectRequest): Observable<Project> {
    return this.http.post<Project>(this.projectsUrl, this.normalizeProjectPayload(project));
  }

  /**
   * Get all projects
   */
  getAllProjects(): Observable<Project[]> {
    return this.http.get<Project[]>(this.projectsUrl);
  }

  /**
   * Get project by ID
   */
  getProjectById(id: string): Observable<Project> {
    return this.http.get<Project>(`${this.projectsUrl}/${id}`);
  }

  /**
   * Get projects by client ID
   */
  getProjectsByClientId(clientId: string): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.projectsUrl}/client/${clientId}`);
  }

  /**
   * Get projects by freelance ID
   */
  getProjectsByFreelanceId(freelanceId: string): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.projectsUrl}/freelance/${freelanceId}`);
  }

  /**
   * Get projects by job offer ID
   */
  getProjectsByJobOfferId(jobOfferId: string): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.projectsUrl}/job-offer/${jobOfferId}`);
  }

  /**
   * Get projects by status
   */
  getProjectsByStatus(status: ProjectStatus): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.projectsUrl}/status/${status}`);
  }

  /**
   * Update a project
   */
  updateProject(id: string, project: Partial<Project>): Observable<Project> {
    return this.http.put<Project>(`${this.projectsUrl}/${id}`, this.normalizeProjectPayload(project));
  }

  /**
   * Update project status
   */
  updateProjectStatus(id: string, status: ProjectStatus): Observable<Project> {
    const params = new HttpParams().set('status', status);
    return this.http.patch<Project>(`${this.projectsUrl}/${id}/status`, null, { params });
  }

  /**
   * Calculate and update project progress
   */
  updateProjectProgress(id: string): Observable<Project> {
    return this.http.post<Project>(`${this.projectsUrl}/${id}/calculate-progress`, {});
  }

  /**
   * Delete a project
   */
  deleteProject(id: string): Observable<void> {
    return this.http.delete<void>(`${this.projectsUrl}/${id}`);
  }

  // ===== PROJECT MILESTONE ENDPOINTS =====

  /**
   * Create a new milestone
   */
  createMilestone(projectId: string, milestone: Omit<ProjectMilestone, 'id' | 'projectId'>): Observable<ProjectMilestone> {
    const milestoneData = { ...this.normalizeMilestonePayload(milestone), projectId };
    return this.http.post<ProjectMilestone>(this.milestonesUrl, milestoneData);
  }

  /**
   * Get all milestones
   */
  getAllMilestones(): Observable<ProjectMilestone[]> {
    return this.http.get<ProjectMilestone[]>(this.milestonesUrl);
  }

  /**
   * Get milestone by ID
   */
  getMilestoneById(id: string): Observable<ProjectMilestone> {
    return this.http.get<ProjectMilestone>(`${this.milestonesUrl}/${id}`);
  }

  /**
   * Get milestones by project ID
   */
  getMilestonesByProjectId(projectId: string): Observable<ProjectMilestone[]> {
    return this.http.get<ProjectMilestone[]>(`${this.milestonesUrl}/project/${projectId}`);
  }

  /**
   * Get milestones by project ID and status
   */
  getMilestonesByProjectIdAndStatus(projectId: string, status: MilestoneStatus): Observable<ProjectMilestone[]> {
    return this.http.get<ProjectMilestone[]>(`${this.milestonesUrl}/project/${projectId}/status/${status}`);
  }

  /**
   * Get milestones by status
   */
  getMilestonesByStatus(status: MilestoneStatus): Observable<ProjectMilestone[]> {
    return this.http.get<ProjectMilestone[]>(`${this.milestonesUrl}/status/${status}`);
  }

  /**
   * Get overdue milestones
   */
  getOverdueMilestones(): Observable<ProjectMilestone[]> {
    return this.http.get<ProjectMilestone[]>(`${this.milestonesUrl}/overdue`);
  }

  /**
   * Get milestones due soon
   */
  getDueSoonMilestones(days: number = 7): Observable<ProjectMilestone[]> {
    const params = new HttpParams().set('days', days.toString());
    return this.http.get<ProjectMilestone[]>(`${this.milestonesUrl}/due-soon`, { params });
  }

  /**
   * Update a milestone
   */
  updateMilestone(id: string, milestone: Partial<ProjectMilestone>): Observable<ProjectMilestone> {
    return this.http.put<ProjectMilestone>(`${this.milestonesUrl}/${id}`, this.normalizeMilestonePayload(milestone));
  }

  /**
   * Update milestone status
   */
  updateMilestoneStatus(id: string, status: MilestoneStatus): Observable<ProjectMilestone> {
    const params = new HttpParams().set('status', status);
    return this.http.patch<ProjectMilestone>(`${this.milestonesUrl}/${id}/status`, null, { params });
  }

  /**
   * Submit milestone (Freelance)
   */
  submitMilestone(id: string, submission: MilestoneSubmission): Observable<ProjectMilestone> {
    return this.http.post<ProjectMilestone>(`${this.milestonesUrl}/${id}/submit`, submission);
  }

  /**
   * Approve milestone (Client)
   */
  approveMilestone(id: string): Observable<ProjectMilestone> {
    return this.http.post<ProjectMilestone>(`${this.milestonesUrl}/${id}/approve`, {});
  }

  /**
   * Reject milestone (Client)
   */
  rejectMilestone(id: string, reason: string): Observable<ProjectMilestone> {
    return this.http.post<ProjectMilestone>(`${this.milestonesUrl}/${id}/reject`, { rejectionReason: reason });
  }

  /**
   * Delete a milestone
   */
  deleteMilestone(id: string): Observable<void> {
    return this.http.delete<void>(`${this.milestonesUrl}/${id}`);
  }

  // ===== HELPER METHODS =====

  /**
   * Calculate days until deadline
   */
  getDaysUntilDeadline(dueDate: Date | string): number {
    const due = new Date(dueDate);
    const today = new Date();
    const diffTime = due.getTime() - today.getTime();
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  }

  /**
   * Check if milestone is overdue
   */
  isMilestoneOverdue(milestone: ProjectMilestone): boolean {
    return this.getDaysUntilDeadline(milestone.dueDate) < 0 && 
           milestone.status !== MilestoneStatus.APPROVED;
  }

  /**
   * Get status color class
   */
  getStatusColor(status: MilestoneStatus | ProjectStatus): string {
    const colorMap: Record<string, string> = {
      // Milestone statuses
      'PENDING': 'gray',
      'IN_PROGRESS': 'blue',
      'SUBMITTED': 'orange',
      'APPROVED': 'green',
      'REJECTED': 'red',
      // Project statuses
      'ACTIVE': 'green',
      'ON_HOLD': 'orange',
      'COMPLETED': 'blue',
      'CANCELLED': 'red'
    };
    return colorMap[status] || 'gray';
  }

  /**
   * Get status icon
   */
  getStatusIcon(status: MilestoneStatus): string {
    const iconMap: Record<MilestoneStatus, string> = {
      [MilestoneStatus.PENDING]: 'schedule',
      [MilestoneStatus.IN_PROGRESS]: 'work',
      [MilestoneStatus.SUBMITTED]: 'send',
      [MilestoneStatus.APPROVED]: 'check_circle',
      [MilestoneStatus.REJECTED]: 'cancel'
    };
    return iconMap[status];
  }

  /**
   * Parse JSON string safely
   */
  parseJsonField<T>(jsonString: string | undefined): T[] {
    if (!jsonString) return [];
    try {
      return JSON.parse(jsonString);
    } catch {
      return [];
    }
  }

  /**
   * Stringify JSON field
   */
  stringifyJsonField<T>(data: T[]): string {
    return JSON.stringify(data);
  }

  private normalizeProjectPayload(project: Partial<CreateProjectRequest> | Partial<Project>): Record<string, unknown> {
    const payload: Record<string, unknown> = { ...(project as Record<string, unknown>) };
    payload['startDate'] = this.toLocalDateTime(project.startDate);
    payload['endDate'] = this.toLocalDateTime(project.endDate);

    const milestones = (project as { milestones?: Partial<ProjectMilestone>[] }).milestones;
    if (milestones) {
      payload['milestones'] = milestones.map(milestone => this.normalizeMilestonePayload(milestone));
    }

    return payload;
  }

  private normalizeMilestonePayload(milestone: Partial<ProjectMilestone>): Record<string, unknown> {
    const payload: Record<string, unknown> = { ...(milestone as Record<string, unknown>) };
    payload['dueDate'] = this.toLocalDateTime(milestone.dueDate);
    return payload;
  }

  private toLocalDateTime(value: Date | string | undefined): Date | string | undefined {
    if (!value) return value;

    if (value instanceof Date) {
      return this.formatDateTime(value);
    }

    if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
      return `${value}T00:00:00`;
    }

    return this.formatDateTime(new Date(value));
  }

  private formatDateTime(date: Date): string {
    if (Number.isNaN(date.getTime())) return '';

    const pad = (part: number) => part.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
  }

  // ===== ADMIN METHODS =====

  /**
   * Admin approve milestone (force approval)
   */
  adminApproveMilestone(id: string, adminNotes: string): Observable<ProjectMilestone> {
    return this.http.post<ProjectMilestone>(`${this.milestonesUrl}/${id}/admin-approve`, { adminNotes });
  }

  /**
   * Admin request revisions
   */
  adminRequestRevisions(id: string, adminNotes: string): Observable<ProjectMilestone> {
    return this.http.post<ProjectMilestone>(`${this.milestonesUrl}/${id}/admin-revisions`, { adminNotes });
  }

  /**
   * Get milestone statistics
   */
  getMilestoneStats(): Observable<any> {
    return this.http.get<any>(`${this.milestonesUrl}/stats`);
  }
}
