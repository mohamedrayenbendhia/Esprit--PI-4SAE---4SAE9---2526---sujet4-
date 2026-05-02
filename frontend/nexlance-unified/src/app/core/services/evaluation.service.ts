// core/services/evaluation.service.ts

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Evaluation {
  id?: number;
  projectId: string;
  evaluatorId: string;
  evaluatedId: string;
  evaluatedEmail?: string;
  ratingGlobal?: number;
  qualityScore: number;
  deadlineScore: number;
  communicationScore: number;
  professionalismScore: number;
  comment?: string;
  responseText?: string;
  responseDate?: Date;
  helpfulCount?: number;
  notHelpfulCount?: number;
  status?: string;
  reportReason?: string;
  reportStatus?: string;
  lockedAfter48h?: boolean;
  createdAt?: Date;
  updatedAt?: Date;
}

@Injectable({
  providedIn: 'root'
})
export class EvaluationService {
  private http = inject(HttpClient);
  private freelancerUrl = `${environment.evaluationApiUrl}/freelancer/evaluations`;
  private clientUrl = `${environment.evaluationApiUrl}/client/evaluations`;

  constructor() {}

  // =====================================================================
  // MÉTHODES FREELANCER (Évaluations REÇUES des clients)
  // =====================================================================

  /** Compte les évaluations reçues par email */
  countUserEvaluations(email: string): Observable<number> {
    return this.http.get<number>(`${this.freelancerUrl}/count/${encodeURIComponent(email)}`);
  }

  /** Calcule la moyenne des notes par email */
  calculateUserAverageRating(email: string): Observable<number> {
    return this.http.get<number>(`${this.freelancerUrl}/average-rating/${encodeURIComponent(email)}`);
  }

  /** Liste des évaluations reçues par email */
  getFreelancerEvaluations(email: string): Observable<Evaluation[]> {
    const params = new HttpParams().set('email', email);
    return this.http.get<Evaluation[]>(`${this.freelancerUrl}/my-evaluations`, { params });
  }

  /** Récupère une évaluation spécifique par son ID + email (sécurité) */
  getFreelancerEvaluationById(evaluationId: number, freelancerEmail: string): Observable<Evaluation> {
    const params = new HttpParams().set('freelancerEmail', freelancerEmail);
    return this.http.get<Evaluation>(`${this.freelancerUrl}/${evaluationId}`, { params });
  }

  /** Répondre à un avis (Action du Freelancer) */
  respondToEvaluation(id: number, responseText: string, freelancerEmail: string): Observable<Evaluation> {
    const params = new HttpParams().set('freelancerEmail', freelancerEmail);
    return this.http.post<Evaluation>(
      `${this.freelancerUrl}/evaluation/${id}/respond`, 
      { response: responseText }, 
      { params }
    );
  }

  // =====================================================================
  // ✅ NOUVELLES MÉTHODES FREELANCER (Évaluations DONNÉES aux clients)
  // =====================================================================

  /** Évaluer un client (pour le freelancer) */
  evaluateClient(clientEmail: string, evalData: Evaluation, freelancerId: string, projectId: string | null): Observable<Evaluation> {
    let params = new HttpParams().set('freelancerId', freelancerId);
    if (projectId) {
      params = params.set('projectId', projectId);
    }
    
    return this.http.post<Evaluation>(
      `${this.freelancerUrl}/evaluate-client/${encodeURIComponent(clientEmail)}`, 
      evalData, 
      { params }
    );
  }

  /** Récupérer les évaluations données par un freelancer (à des clients) */
  getMyGivenEvaluationsAsFreelancer(freelancerId: string): Observable<Evaluation[]> {
    return this.http.get<Evaluation[]>(`${this.freelancerUrl}/freelancer/${freelancerId}/given`);
  }

  // =====================================================================
  // MÉTHODES CLIENT (Évaluations DONNÉES aux freelancers)
  // =====================================================================

  /** Créer une évaluation pour un freelancer (en utilisant son email) */
  evaluateFreelancer(freelancerEmail: string, evalData: Evaluation, clientId: string, projId: string | null): Observable<Evaluation> {
    let params = new HttpParams().set('clientId', clientId);
    if (projId) params = params.set('projectId', projId);
    
    return this.http.post<Evaluation>(`${this.clientUrl}/freelancer/${encodeURIComponent(freelancerEmail)}`, evalData, { params });
  }

  /** Récupérer les évaluations données par un client */
  getMyGivenEvaluations(clientId: string): Observable<Evaluation[]> {
    return this.http.get<Evaluation[]>(`${this.clientUrl}/client/${clientId}/given`);
  }

  /** Récupérer une évaluation par son ID (pour modification par le client) */
  getEvaluationById(id: number): Observable<Evaluation> {
    return this.http.get<Evaluation>(`${this.clientUrl}/${id}`);
  }

  /** Voter pour une évaluation (utile/pas utile) */
  voteForEvaluation(id: number, clientId: string, helpful: boolean): Observable<any> {
    const params = new HttpParams().set('clientId', clientId).set('helpful', helpful.toString());
    return this.http.post(`${this.clientUrl}/evaluation/${id}/vote`, null, { params });
  }

  /** Signaler une évaluation */
  reportEvaluation(id: number, reason: string): Observable<Evaluation> {
    const params = new HttpParams().set('reason', reason);
    return this.http.post<Evaluation>(`${this.clientUrl}/evaluation/${id}/report`, null, { params });
  }

  /** Supprimer une évaluation (client seulement) */
  deleteMyEvaluation(id: number, clientId: string): Observable<void> {
    const params = new HttpParams().set('clientId', clientId);
    return this.http.delete<void>(`${this.clientUrl}/${id}`, { params });
  }

  /** Vérifier si un client a déjà voté */
  hasUserVoted(evaluationId: number, clientId: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.clientUrl}/evaluation/${evaluationId}/has-voted/${clientId}`);
  }

  // =====================================================================
  // MÉTHODES GLOBALES
  // =====================================================================

  getAllFreelancersOverview(): Observable<any> {
    return this.http.get(`${this.clientUrl}/freelancers/all/overview`);
  }

  getFreelancerDetails(email: string): Observable<any> {
    return this.http.get(`${this.clientUrl}/freelancer/${encodeURIComponent(email)}/details`);
  }
}
