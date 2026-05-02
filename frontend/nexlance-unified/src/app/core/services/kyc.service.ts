import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { KYCVerification, SubmitKYCRequest, ReviewKYCRequest, KYCStatus, DocumentType } from '../models/kyc-verification.model';
import { environment } from '../../../environments/environment';

export interface KYCStats {
  totalPending: number;
  totalApproved: number;
  totalRejected: number;
  averageReviewTime: number;
}

@Injectable({
  providedIn: 'root'
})
export class KycService {
  private http = inject(HttpClient);
  private readonly API_URL = `${environment.apiUrl}/kyc`;

  // User endpoints
  getMyVerifications(): Observable<KYCVerification[]> {
    return this.http.get<KYCVerification[]>(`${this.API_URL}/me`);
  }

  getMyKYCStatus(): Observable<{ verified: boolean; status: KYCStatus; documents: KYCVerification[] }> {
    return this.http.get<{ verified: boolean; status: KYCStatus; documents: KYCVerification[] }>(`${this.API_URL}/me/status`);
  }

  submitDocument(request: SubmitKYCRequest): Observable<KYCVerification> {
    const formData = new FormData();
    formData.append('documentType', request.documentType);
    formData.append('document', request.documentFile);
    if (request.expiryDate) {
      formData.append('expiryDate', request.expiryDate.toISOString());
    }
    
    return this.http.post<KYCVerification>(`${this.API_URL}/submit`, formData);
  }

  getVerificationById(id: string): Observable<KYCVerification> {
    return this.http.get<KYCVerification>(`${this.API_URL}/${id}`);
  }

  deleteDocument(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  // Admin endpoints
  getPendingVerifications(): Observable<KYCVerification[]> {
    return this.http.get<KYCVerification[]>(`${this.API_URL}/admin/pending`);
  }

  getAllVerifications(status?: KYCStatus): Observable<KYCVerification[]> {
    const url = status 
      ? `${this.API_URL}/admin/all?status=${status}`
      : `${this.API_URL}/admin/all`;
    return this.http.get<KYCVerification[]>(url);
  }

  getUserVerifications(userId: string): Observable<KYCVerification[]> {
    return this.http.get<KYCVerification[]>(`${this.API_URL}/admin/user/${userId}`);
  }

  reviewDocument(id: string, review: ReviewKYCRequest): Observable<KYCVerification> {
    return this.http.post<KYCVerification>(`${this.API_URL}/admin/${id}/review`, review);
  }

  approveDocument(id: string, expiryDate?: Date, notes?: string): Observable<KYCVerification> {
    const request: any = {
      status: 'APPROVED'
    };
    if (expiryDate) {
      request.expiryDate = expiryDate;
    }
    if (notes) {
      request.notes = notes;
    }
    return this.reviewDocument(id, request);
  }

  rejectDocument(id: string, rejectionReason: string, notes?: string): Observable<KYCVerification> {
    const request: any = {
      status: 'REJECTED',
      rejectionReason
    };
    if (notes) {
      request.notes = notes;
    }
    return this.reviewDocument(id, request);
  }

  getKYCStats(): Observable<KYCStats> {
    return this.http.get<KYCStats>(`${this.API_URL}/admin/stats`);
  }

  getDocumentUrl(id: string): string {
    return `${this.API_URL}/${id}/document`;
  }
}
