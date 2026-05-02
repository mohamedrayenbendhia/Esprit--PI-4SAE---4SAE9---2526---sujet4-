import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Complaint,
  SupportMessage,
  CreateComplaintRequest,
  CreateMessageRequest,
  ResolveComplaintRequest,
  InvolveReportedRequest,
  ComplaintStatus,
  ComplaintPriority,
  ConversationType
} from '../models/complaint.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ComplaintService {
  private http = inject(HttpClient);

  private readonly BASE_URL = `${environment.complaintsApiUrl}/complaints`;
  private readonly MSG_URL  = `${environment.complaintsApiUrl}/support-messages`;

  // ============================================================
  // RÉCLAMATIONS — CLIENT / FREELANCE
  // ============================================================

  /** Vérifie si un email correspond à un utilisateur existant */
  checkUserByEmail(email: string): Observable<{id: string; email: string; firstName: string; lastName: string; type: string}> {
    return this.http.get<{id: string; email: string; firstName: string; lastName: string; type: string}>(
      `${environment.userApiUrl}/users/email/${encodeURIComponent(email)}`
    );
  }

  createComplaint(request: CreateComplaintRequest): Observable<Complaint> {
    return this.http.post<Complaint>(this.BASE_URL, request);
  }

  getMyComplaints(): Observable<Complaint[]> {
    return this.http.get<Complaint[]>(`${this.BASE_URL}/my-complaints`);
  }

  /** Réclamations où l'utilisateur connecté est la partie mise en cause */
  getInvolvedComplaints(): Observable<Complaint[]> {
    return this.http.get<Complaint[]>(`${this.BASE_URL}/involved`);
  }

  getComplaintById(id: string): Observable<Complaint> {
    return this.http.get<Complaint>(`${this.BASE_URL}/${id}`);
  }

  rateComplaint(id: string, rating: number): Observable<Complaint> {
    return this.http.put<Complaint>(`${this.BASE_URL}/${id}/rate`, null, {
      params: new HttpParams().set('rating', rating.toString())
    });
  }

  // ============================================================
  // RÉCLAMATIONS — SUPPORT_AGENT
  // ============================================================

  /** File des réclamations disponibles à prendre (non assignées) */
  getAgentAvailableQueue(): Observable<Complaint[]> {
    return this.http.get<Complaint[]>(`${this.BASE_URL}/agent/available`);
  }

  getMyAssignedComplaints(): Observable<Complaint[]> {
    return this.http.get<Complaint[]>(`${this.BASE_URL}/agent/my-assigned`);
  }

  takeComplaint(id: string): Observable<Complaint> {
    return this.http.put<Complaint>(`${this.BASE_URL}/${id}/take`, null);
  }

  assignComplaint(id: string, agentId: string): Observable<Complaint> {
    return this.http.put<Complaint>(`${this.BASE_URL}/${id}/assign`, null, {
      params: new HttpParams().set('agentId', agentId)
    });
  }

  updateStatus(id: string, status: ComplaintStatus): Observable<Complaint> {
    return this.http.put<Complaint>(`${this.BASE_URL}/${id}/status`, null, {
      params: new HttpParams().set('status', status)
    });
  }

  updatePriority(id: string, priority: ComplaintPriority): Observable<Complaint> {
    return this.http.put<Complaint>(`${this.BASE_URL}/${id}/priority`, null, {
      params: new HttpParams().set('priority', priority)
    });
  }

  resolveComplaint(id: string, request: ResolveComplaintRequest): Observable<Complaint> {
    return this.http.put<Complaint>(`${this.BASE_URL}/${id}/resolve`, request);
  }

  // ============================================================
  // RÉCLAMATIONS — ADMIN
  // ============================================================

  getAllComplaints(): Observable<Complaint[]> {
    return this.http.get<Complaint[]>(this.BASE_URL);
  }

  /** File d'attente de l'admin : réclamations OPEN non assignées + ESCALATED */
  getAdminQueue(): Observable<Complaint[]> {
    return this.http.get<Complaint[]>(`${this.BASE_URL}/admin/queue`);
  }

  uploadComplaintAttachments(files: File[]): Observable<{ urls: string[] }> {
    const formData = new FormData();
    files.forEach(file => formData.append('files', file));
    return this.http.post<{ urls: string[] }>(`${this.BASE_URL}/upload`, formData);
  }

  closeComplaint(id: string): Observable<Complaint> {
    return this.http.put<Complaint>(`${this.BASE_URL}/${id}/close`, null);
  }

  deleteComplaint(id: string): Observable<void> {
    return this.http.delete<void>(`${this.BASE_URL}/${id}`);
  }

  getStatsByStatus(): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(`${this.BASE_URL}/statistics/by-status`);
  }

  getStatsByPriority(): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(`${this.BASE_URL}/statistics/by-priority`);
  }

  getComplaintByTicketNumber(ticketNumber: string): Observable<Complaint> {
    return this.http.get<Complaint>(`${this.BASE_URL}/ticket/${ticketNumber}`);
  }

  getOverdueComplaints(daysThreshold: number = 7): Observable<Complaint[]> {
    return this.http.get<Complaint[]>(`${this.BASE_URL}/overdue`, {
      params: new HttpParams().set('daysThreshold', daysThreshold.toString())
    });
  }

  getComplaintsByAssignedAgent(agentId: string): Observable<Complaint[]> {
    return this.http.get<Complaint[]>(`${this.BASE_URL}/assigned/${agentId}`);
  }

  getAgents(): Observable<any[]> {
    return this.http.get<any[]>(`${environment.userApiUrl}/users/agents`);
  }

  // ============================================================
  // MESSAGES — filtrés par conversationType
  // ============================================================

  /**
   * Envoie un message dans un fil de conversation spécifique.
   * conversationType : COMPLAINANT (défaut) | REPORTED
   */
  sendMessage(request: CreateMessageRequest): Observable<SupportMessage> {
    return this.http.post<SupportMessage>(this.MSG_URL, request);
  }

  /**
   * Récupère les messages d'une réclamation.
   * @param conversationType optionnel — si fourni, filtre sur ce fil (support/admin uniquement)
   */
  getMessages(
    complaintId: string,
    conversationType?: ConversationType
  ): Observable<SupportMessage[]> {
    let params = new HttpParams();
    if (conversationType) {
      params = params.set('conversationType', conversationType);
    }
    return this.http.get<SupportMessage[]>(
      `${this.MSG_URL}/complaint/${complaintId}`,
      { params }
    );
  }

  markMessageRead(messageId: string): Observable<SupportMessage> {
    return this.http.put<SupportMessage>(`${this.MSG_URL}/${messageId}/mark-read`, null);
  }

  markAllRead(complaintId: string): Observable<SupportMessage[]> {
    return this.http.put<SupportMessage[]>(
      `${this.MSG_URL}/complaint/${complaintId}/mark-all-read`, null
    );
  }

  getUnreadCount(complaintId: string): Observable<{ unreadCount: number }> {
    return this.http.get<{ unreadCount: number }>(
      `${this.MSG_URL}/complaint/${complaintId}/unread-count`
    );
  }

  // ============================================================
  // IMPLIQUER LA PARTIE MISE EN CAUSE
  // ============================================================

  /**
   * Action explicite du support/admin : implique la partie mise en cause.
   * Crée le fil REPORTED et envoie une notification + email à la partie.
   */
  involveReportedUser(
    complaintId: string,
    request: InvolveReportedRequest
  ): Observable<{ message: string; reportedUserId: string }> {
    return this.http.post<{ message: string; reportedUserId: string }>(
      `${this.MSG_URL}/complaint/${complaintId}/involve-reported`,
      request
    );
  }

  // ============================================================
  // ACTIVITÉ (anti-spam email)
  // ============================================================

  /** Ping de présence — appelé toutes les 2 min par ActivityPingService */
  pingActivity(): Observable<{ status: string }> {
    return this.http.put<{ status: string }>(
      `${this.BASE_URL}/activity/ping`, null
    );
  }
}