import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  SlaRule, SlaTracking, CreateSlaRuleRequest,
  MediationSession, OpenMediationRequest, SubmitEvidenceRequest, MediationDecisionRequest,
  UserRiskProfile, UserSanction, ApplySanctionRequest,
  ResponseTemplate, CreateTemplateRequest,
  NpsSurvey, NpsStats, NpsResponseRequest,
  ReopenRequest, ComplaintEvent
} from '../models/complaint-advanced.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ComplaintAdvancedService {
  private http = inject(HttpClient);

  private readonly GW = environment.complaintsApiUrl;

  // ── SLA ────────────────────────────────────────────────────────────────────

  getSlaRules(): Observable<SlaRule[]> {
    return this.http.get<SlaRule[]>(`${this.GW}/sla/rules`);
  }

  createSlaRule(req: CreateSlaRuleRequest): Observable<SlaRule> {
    return this.http.post<SlaRule>(`${this.GW}/sla/rules`, req);
  }

  updateSlaRule(ruleId: string, req: CreateSlaRuleRequest): Observable<SlaRule> {
    return this.http.put<SlaRule>(`${this.GW}/sla/rules/${ruleId}`, req);
  }

  deleteSlaRule(ruleId: string): Observable<void> {
    return this.http.delete<void>(`${this.GW}/sla/rules/${ruleId}`);
  }

  getSlaTracking(complaintId: string): Observable<SlaTracking> {
    return this.http.get<SlaTracking>(`${this.GW}/sla/tracking/${complaintId}`);
  }

  // ── Mediation ──────────────────────────────────────────────────────────────

  openMediation(complaintId: string, req: OpenMediationRequest): Observable<MediationSession> {
    return this.http.post<MediationSession>(`${this.GW}/mediation/${complaintId}/open`, req);
  }

  getMediation(complaintId: string): Observable<MediationSession> {
    return this.http.get<MediationSession>(`${this.GW}/mediation/${complaintId}`);
  }

  submitEvidence(sessionId: string, req: SubmitEvidenceRequest): Observable<MediationSession> {
    return this.http.post<MediationSession>(`${this.GW}/mediation/sessions/${sessionId}/evidence`, req);
  }

  decideMediation(sessionId: string, req: MediationDecisionRequest): Observable<MediationSession> {
    return this.http.post<MediationSession>(`${this.GW}/mediation/sessions/${sessionId}/decide`, req);
  }

  // ── Risk & Sanctions ───────────────────────────────────────────────────────

  getUserRiskProfile(userId: string): Observable<UserRiskProfile> {
    return this.http.get<UserRiskProfile>(`${this.GW}/risk/users/${userId}`);
  }

  recomputeRisk(userId: string): Observable<UserRiskProfile> {
    return this.http.post<UserRiskProfile>(`${this.GW}/risk/users/${userId}/compute`, {});
  }

  getHighRiskUsers(): Observable<UserRiskProfile[]> {
    return this.http.get<UserRiskProfile[]>(`${this.GW}/risk/high-risk`);
  }

  getUserSanctions(userId: string): Observable<UserSanction[]> {
    return this.http.get<UserSanction[]>(`${this.GW}/risk/users/${userId}/sanctions`);
  }

  applySanction(userId: string, req: ApplySanctionRequest): Observable<UserSanction> {
    return this.http.post<UserSanction>(`${this.GW}/risk/users/${userId}/sanctions`, req);
  }

  liftSanction(sanctionId: string): Observable<UserSanction> {
    return this.http.delete<UserSanction>(`${this.GW}/risk/sanctions/${sanctionId}`);
  }

  // ── Response Templates ─────────────────────────────────────────────────────

  getAllTemplates(): Observable<ResponseTemplate[]> {
    return this.http.get<ResponseTemplate[]>(`${this.GW}/response-templates`);
  }

  getTemplatesByCategory(category: string): Observable<ResponseTemplate[]> {
    return this.http.get<ResponseTemplate[]>(`${this.GW}/response-templates/category/${category}`);
  }

  createTemplate(req: CreateTemplateRequest): Observable<ResponseTemplate> {
    return this.http.post<ResponseTemplate>(`${this.GW}/response-templates`, req);
  }

  updateTemplate(id: string, req: CreateTemplateRequest): Observable<ResponseTemplate> {
    return this.http.put<ResponseTemplate>(`${this.GW}/response-templates/${id}`, req);
  }

  deleteTemplate(id: string): Observable<void> {
    return this.http.delete<void>(`${this.GW}/response-templates/${id}`);
  }

  // ── NPS ────────────────────────────────────────────────────────────────────

  respondNps(complaintId: string, req: NpsResponseRequest): Observable<NpsSurvey> {
    return this.http.post<NpsSurvey>(`${this.GW}/nps/${complaintId}/respond`, req);
  }

  getNpsStats(): Observable<NpsStats> {
    return this.http.get<NpsStats>(`${this.GW}/nps/stats`);
  }

  getNpsSurvey(complaintId: string): Observable<NpsSurvey> {
    return this.http.get<NpsSurvey>(`${this.GW}/nps/${complaintId}`);
  }

  // ── Reopen ─────────────────────────────────────────────────────────────────

  reopenComplaint(complaintId: string, req: ReopenRequest): Observable<any> {
    return this.http.post(`${this.GW}/complaints/${complaintId}/reopen`, req);
  }

  // ── Audit Trail ─────────────────────────────────────────────────────────────

  /** Récupère la timeline complète des événements d'une réclamation. */
  getComplaintEvents(complaintId: string): Observable<ComplaintEvent[]> {
    return this.http.get<ComplaintEvent[]>(`${this.GW}/complaints/${complaintId}/events`);
  }
}
