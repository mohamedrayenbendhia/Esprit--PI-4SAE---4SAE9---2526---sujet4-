// src/app/core/services/admin.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Badge {
  id: number;
  name: string;
  description: string;
  minScore: number;
  minProjects: number;
  icon: string;
  createdAt: Date;
}

export interface Evaluation {
  id?: number;
  projectId: string;
  evaluatorId: string;
  evaluatedId: string;
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

export enum ReportStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED'
}

export enum EvaluationStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  REPORTED = 'REPORTED'
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private baseUrl: string;

  constructor(private http: HttpClient) {
    this.baseUrl = `${environment.evaluationApiUrl}/admin/evaluations`;
    console.log('✅ AdminService initialisé avec URL:', this.baseUrl);
  }

  // ========== GESTION DES BADGES ==========
  
  /**
   * Récupérer tous les badges
   */
  getAllBadges(): Observable<Badge[]> {
    return this.http.get<Badge[]>(`${this.baseUrl}/badges`);
  }

  /**
   * Récupérer un badge par son ID
   * @param id ID du badge
   */
  getBadgeById(id: number): Observable<Badge> {
    return this.http.get<Badge>(`${this.baseUrl}/badges/${id}`);
  }

  /**
   * Créer un nouveau badge
   * @param badge Données du badge
   */
  createBadge(badge: Omit<Badge, 'id' | 'createdAt'>): Observable<Badge> {
    return this.http.post<Badge>(`${this.baseUrl}/badges`, badge);
  }

  /**
   * Mettre à jour un badge existant
   * @param id ID du badge
   * @param badge Nouvelles données
   */
  updateBadge(id: number, badge: Partial<Badge>): Observable<Badge> {
    return this.http.put<Badge>(`${this.baseUrl}/badges/${id}`, badge);
  }

  /**
   * Supprimer un badge
   * @param id ID du badge
   */
  deleteBadge(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/badges/${id}`);
  }

  // ========== GESTION DES ÉVALUATIONS ==========
  
  /**
   * Récupérer toutes les évaluations
   */
  getAllEvaluations(): Observable<Evaluation[]> {
    return this.http.get<Evaluation[]>(`${this.baseUrl}/all`);
  }

  /**
   * Récupérer les évaluations signalées
   */
  getReportedEvaluations(): Observable<Evaluation[]> {
    return this.http.get<Evaluation[]>(`${this.baseUrl}/reported`);
  }

  /**
   * Récupérer les évaluations avec leurs réponses
   */
  getEvaluationsWithResponses(): Observable<Evaluation[]> {
    return this.http.get<Evaluation[]>(`${this.baseUrl}/with-responses`);
  }

  /**
   * Récupérer les évaluations avec réponses filtrées par statut
   * @param status Statut des évaluations
   */
  getEvaluationsWithResponsesByStatus(status?: EvaluationStatus | string): Observable<Evaluation[]> {
    let url = `${this.baseUrl}/with-responses`;
    if (status) {
      url += `?status=${status}`;
    }
    return this.http.get<Evaluation[]>(url);
  }

  /**
   * Récupérer une évaluation spécifique
   * @param evaluationId ID de l'évaluation
   */
  getEvaluationById(evaluationId: number): Observable<Evaluation> {
    return this.http.get<Evaluation>(`${this.baseUrl}/evaluation/${evaluationId}`);
  }

  /**
   * Modérer une évaluation
   * @param evaluationId ID de l'évaluation
   * @param decision Décision de modération (APPROVED ou REJECTED)
   */
  moderateEvaluation(evaluationId: number, decision: ReportStatus): Observable<Evaluation> {
    const params = new HttpParams().set('decision', decision);
    return this.http.put<Evaluation>(
      `${this.baseUrl}/evaluation/${evaluationId}/moderate`, 
      {}, 
      { params }
    );
  }

  /**
   * Supprimer n'importe quelle évaluation (admin uniquement)
   * @param evaluationId ID de l'évaluation à supprimer
   */
  deleteAnyEvaluation(evaluationId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/evaluation/${evaluationId}`);
  }

  /**
   * Récupérer les évaluations reçues par un utilisateur
   * @param userId ID de l'utilisateur
   */
  getUserReceivedEvaluations(userId: string): Observable<Evaluation[]> {
    return this.http.get<Evaluation[]>(`${this.baseUrl}/user/${userId}/received`);
  }

  /**
   * Récupérer les évaluations données par un utilisateur
   * @param userId ID de l'utilisateur
   */
  getUserGivenEvaluations(userId: string): Observable<Evaluation[]> {
    return this.http.get<Evaluation[]>(`${this.baseUrl}/user/${userId}/given`);
  }

  // ========== MÉTHODES STATISTIQUES ==========
  
  /**
   * Récupérer les statistiques globales des évaluations
   */
  getEvaluationStats(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/stats`);
  }

  /**
   * Récupérer les évaluations par période
   * @param startDate Date de début
   * @param endDate Date de fin
   */
  getEvaluationsByDateRange(startDate: Date, endDate: Date): Observable<Evaluation[]> {
    const params = new HttpParams()
      .set('startDate', startDate.toISOString())
      .set('endDate', endDate.toISOString());
    return this.http.get<Evaluation[]>(`${this.baseUrl}/date-range`, { params });
  }

  /**
   * Récupérer les évaluations en attente de modération
   */
  getPendingModerations(): Observable<Evaluation[]> {
    return this.http.get<Evaluation[]>(`${this.baseUrl}/pending-moderations`);
  }

  /**
   * Approuver une évaluation signalée
   * @param evaluationId ID de l'évaluation
   * @param adminComment Commentaire de l'admin
   */
  approveReportedEvaluation(evaluationId: number, adminComment?: string): Observable<Evaluation> {
    return this.http.put<Evaluation>(`${this.baseUrl}/evaluation/${evaluationId}/approve`, { adminComment });
  }

  /**
   * Rejeter une évaluation signalée (la supprimer)
   * @param evaluationId ID de l'évaluation
   * @param adminComment Commentaire de l'admin
   */
  rejectReportedEvaluation(evaluationId: number, adminComment?: string): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/evaluation/${evaluationId}/reject`, { adminComment });
  }

  // ========== MÉTHODES DE RECHERCHE ==========
  
  /**
   * Rechercher des évaluations
   * @param searchTerm Terme de recherche
   * @param status Statut optionnel
   */
  searchEvaluations(searchTerm: string, status?: string): Observable<Evaluation[]> {
    let params = new HttpParams().set('search', searchTerm);
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<Evaluation[]>(`${this.baseUrl}/search`, { params });
  }

  /**
   * Récupérer les évaluations par projet
   * @param projectId ID du projet
   */
  getEvaluationsByProject(projectId: string): Observable<Evaluation[]> {
    return this.http.get<Evaluation[]>(`${this.baseUrl}/project/${projectId}`);
  }
}