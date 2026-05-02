import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DeliverableVersion {
  id: string;
  milestoneId: string;
  versionNumber: number;
  fileName: string;
  filePath: string;
  fileSize: number;
  contentType: string;
  description: string;
  changeNotes: string;
  uploadedBy: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'SUPERSEDED';
  reviewComment: string;
  reviewedBy: string;
  reviewedAt: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class DeliverableVersionService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.projectsApiUrl}/deliverables`;

  uploadVersion(milestoneId: string, file: File, uploadedBy: string,
                description?: string, changeNotes?: string): Observable<DeliverableVersion> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('uploadedBy', uploadedBy);
    if (description) formData.append('description', description);
    if (changeNotes) formData.append('changeNotes', changeNotes);

    return this.http.post<DeliverableVersion>(
      `${this.baseUrl}/milestone/${milestoneId}/upload`, formData
    );
  }

  getVersionHistory(milestoneId: string): Observable<DeliverableVersion[]> {
    return this.http.get<DeliverableVersion[]>(`${this.baseUrl}/milestone/${milestoneId}`);
  }

  getLatestVersion(milestoneId: string): Observable<DeliverableVersion> {
    return this.http.get<DeliverableVersion>(`${this.baseUrl}/milestone/${milestoneId}/latest`);
  }

  getVersionCount(milestoneId: string): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/milestone/${milestoneId}/count`);
  }

  reviewVersion(versionId: string, reviewedBy: string, status: 'APPROVED' | 'REJECTED',
                reviewComment?: string): Observable<DeliverableVersion> {
    return this.http.put<DeliverableVersion>(`${this.baseUrl}/${versionId}/review`, {
      reviewedBy,
      status,
      reviewComment: reviewComment || ''
    });
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }
}
