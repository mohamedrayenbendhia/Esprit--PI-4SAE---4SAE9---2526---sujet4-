import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  Organization, OrganizationSummary, OrgMember, OrgInvitation,
  OrgReview, OrgAuditLog, OrgDashboardStats, Page,
  CreateOrganizationRequest, UpdateOrganizationRequest,
  InviteMemberRequest, CreateReviewRequest,
  AdminVerifyRequest, AdminSuspendRequest,
  OrganizationSize, OrganizationType, OrganizationVisibility, MemberRole,
  OrgPortfolioItem, OrgApplication, OrgRfq, OrgBadgeInfo,
  CreatePortfolioItemRequest, CreateApplicationRequest,
  RespondApplicationRequest, CreateRfqRequest, RfqResponseRequest,
  MatchingRequest,
  CollabOffer, CollabApplication,
  CreateCollabOfferRequest, ApplyCollabOfferRequest, RespondCollabApplicationRequest,
  OrgAnalytics, TrustScore,
  ScoredMatchingRequest, CompatibilityResult,
  CollabOfferMatchingRequest, CollabOfferMatchResult,
  ProfileCompletionScore,
  GeoLocationResponse
} from '../models/organization.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class OrganizationService {
  private http = inject(HttpClient);

  // Tout passe par le Gateway (8765) → organization-service (9095)
  private readonly BASE  = `${environment.organizationsApiUrl}/organizations`;
  private readonly ADMIN = `${environment.organizationsApiUrl}/admin/organizations`;
  private readonly INV   = `${environment.organizationsApiUrl}/org-invitations`;
  private readonly GDPR  = `${environment.organizationsApiUrl}/gdpr`;

  // ── Public ────────────────────────────────────────────────────────────────

  /**
   * Recherche publique paginée.
   * FIXÉ : séparation du paramètre de pagination `pageSize` du filtre `size` (OrganizationSize).
   *        Le backend attend `keyword` (pas `query`) pour la recherche textuelle.
   */
  search(
    keyword?: string,
    type?: OrganizationType,
    size?: OrganizationSize,
    page = 0,
    pageSize = 12
  ): Observable<Page<OrganizationSummary>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', pageSize);
    if (keyword) params = params.set('keyword', keyword);   // ← corrigé : était 'query'
    if (type)    params = params.set('type', type);
    if (size)    params = params.set('orgSize', size);
    return this.http.get<Page<OrganizationSummary>>(`${this.BASE}/search`, { params });
  }

  getById(id: string): Observable<Organization> {
    return this.http.get<Organization>(`${this.BASE}/${id}`);
  }

  getReviews(orgId: string, page = 0, pageSize = 10): Observable<Page<OrgReview>> {
    const params = new HttpParams().set('page', page).set('size', pageSize);
    return this.http.get<Page<OrgReview>>(`${this.BASE}/${orgId}/reviews`, { params });
  }

  canReview(orgId: string): Observable<boolean> {
    return this.http
      .get<{ canReview: boolean }>(`${this.BASE}/${orgId}/reviews/eligibility`)
      .pipe(map(r => !!r.canReview));
  }

  // ── Organisation (authentifié) ────────────────────────────────────────────

  create(req: CreateOrganizationRequest): Observable<Organization> {
    return this.http.post<Organization>(this.BASE, req);
  }

  isNameAvailable(name: string): Observable<boolean> {
    const params = new HttpParams().set('name', name);
    return this.http
      .get<{ available: boolean }>(`${this.BASE}/name-available`, { params })
      .pipe(map(r => !!r.available));
  }

  update(id: string, req: UpdateOrganizationRequest): Observable<Organization> {
    return this.http.put<Organization>(`${this.BASE}/${id}`, req);
  }

  setVisibility(id: string, visibility: OrganizationVisibility): Observable<Organization> {
    return this.http.patch<Organization>(`${this.BASE}/${id}/visibility`, null, {
      params: new HttpParams().set('visibility', visibility)
    });
  }

  /** Dissolution douce (conserve les données, status → DISSOLVED) */
  dissolve(id: string): Observable<void> {
    return this.http.post<void>(`${this.BASE}/${id}/dissolve`, null);
  }

  /** Retourne les organisations dont l'utilisateur est propriétaire */
  getMyOrganizations(): Observable<OrganizationSummary[]> {
    return this.http.get<OrganizationSummary[] | Page<OrganizationSummary>>(`${this.BASE}/my`).pipe(
      map(r => Array.isArray(r) ? r : (r?.content ?? []))
    );
  }

  transferOwnership(orgId: string, newOwnerId: string): Observable<void> {
    return this.http.post<void>(`${this.BASE}/${orgId}/transfer-ownership`, { newOwnerId });
  }

  // ── Membres ───────────────────────────────────────────────────────────────

  /**
   * FIXÉ : le backend retourne Page<MemberResponse>, on extrait .content.
   * Signature inchangée côté composant (toujours Observable<OrgMember[]>).
   */
  getMembers(orgId: string, page = 0, pageSize = 50): Observable<OrgMember[]> {
    const params = new HttpParams().set('page', page).set('size', pageSize);
    return this.http.get<Page<OrgMember> | OrgMember[]>(`${this.BASE}/${orgId}/members`, { params }).pipe(
      map(p => Array.isArray(p) ? p : (p?.content ?? []))
    );
  }

  /**
   * Mise à jour du rôle d'un membre (MEMBER → MANAGER ou inverse).
   * Remplace l'ancien promoteToManager() trop restrictif.
   */
  updateMemberRole(orgId: string, memberId: string, role: MemberRole): Observable<OrgMember> {
    return this.http.patch<OrgMember>(
      `${this.BASE}/${orgId}/members/${memberId}/role`, null,
      { params: new HttpParams().set('role', role) }
    );
  }

  removeMember(orgId: string, memberId: string): Observable<void> {
    return this.http.delete<void>(`${this.BASE}/${orgId}/members/${memberId}`);
  }

  leaveOrganization(orgId: string): Observable<void> {
    return this.http.post<void>(`${this.BASE}/${orgId}/members/leave`, null);
  }

  toggleProfileDisplay(orgId: string, display: boolean): Observable<OrgMember> {
    return this.http.patch<OrgMember>(`${this.BASE}/${orgId}/members/profile-display`, null, {
      params: new HttpParams().set('display', display)
    });
  }

  // ── Invitations ───────────────────────────────────────────────────────────

  /**
   * InviteMemberRequest contient maintenant les bons noms de champs
   * (inviteeId, inviteeEmail, role) alignés sur le backend.
   */
  invite(orgId: string, req: InviteMemberRequest): Observable<OrgInvitation> {
    return this.http.post<OrgInvitation>(`${this.BASE}/${orgId}/invitations`, req);
  }

  cancelInvitation(orgId: string, invitationId: string): Observable<void> {
    return this.http.delete<void>(`${this.BASE}/${orgId}/invitations/${invitationId}`);
  }

  /**
   * FIXÉ : le backend retourne Page<InvitationResponse>, on extrait .content.
   */
  getPendingInvitationsForOrg(orgId: string, page = 0): Observable<OrgInvitation[]> {
    const params = new HttpParams().set('page', page).set('size', 50);
    return this.http.get<Page<OrgInvitation> | OrgInvitation[]>(`${this.BASE}/${orgId}/invitations`, { params }).pipe(
      map(p => Array.isArray(p) ? p : (p?.content ?? []))
    );
  }

  /** Invitations reçues par l'utilisateur courant (liste in-app) */
  getMyPendingInvitations(): Observable<OrgInvitation[]> {
    return this.http.get<OrgInvitation[] | Page<OrgInvitation>>(`${this.INV}/my`).pipe(
      map(r => Array.isArray(r) ? r : (r?.content ?? []))
    );
  }

  /**
   * Réponse à une invitation IN-APP via son ID et son orgId.
   * FIXÉ : remplace respondByToken() pour les invitations listées en app
   * (les tokens sont réservés aux liens email — le backend ne les inclut pas dans les réponses API).
   */
  respondToInvitation(orgId: string, invitationId: string, accepted: boolean): Observable<OrgInvitation> {
    return this.http.post<OrgInvitation>(
      `${this.BASE}/${orgId}/invitations/${invitationId}/respond`, null,
      { params: new HttpParams().set('accepted', accepted) }
    );
  }

  /**
   * Réponse par token (lien email uniquement — /invitations/token/{token}/respond).
   * Conservé pour la page InvitationRespondComponent.
   */
  respondByToken(token: string, accept: boolean): Observable<OrgInvitation> {
    return this.http.post<OrgInvitation>(`${this.INV}/token/${token}/respond`, null, {
      params: new HttpParams().set('accepted', accept)
    });
  }

  // ── Avis ──────────────────────────────────────────────────────────────────

  /** CreateReviewRequest contient maintenant rating (unique 1-5) aligné sur le backend */
  submitReview(orgId: string, req: CreateReviewRequest): Observable<OrgReview> {
    return this.http.post<OrgReview>(`${this.BASE}/${orgId}/reviews`, req);
  }

  replyToReview(orgId: string, reviewId: string, reply: string): Observable<OrgReview> {
    return this.http.post<OrgReview>(`${this.BASE}/${orgId}/reviews/${reviewId}/reply`, { reply });
  }

  reportReview(orgId: string, reviewId: string): Observable<OrgReview> {
    return this.http.post<OrgReview>(`${this.BASE}/${orgId}/reviews/${reviewId}/report`, null);
  }

  deleteReview(orgId: string, reviewId: string): Observable<void> {
    return this.http.delete<void>(`${this.BASE}/${orgId}/reviews/${reviewId}`);
  }

  // ── Admin ─────────────────────────────────────────────────────────────────

  /** Backend retourne Page<OrganizationResponse> → on extrait .content */
  getPendingVerification(): Observable<Organization[]> {
    return this.http.get<Page<Organization> | Organization[]>(`${this.ADMIN}/pending`).pipe(
      map(p => Array.isArray(p) ? p : (p?.content ?? []))
    );
  }

  getAllOrganizations(status?: string, page = 0): Observable<Page<Organization>> {
    let params = new HttpParams().set('page', page).set('size', 20);
    if (status) params = params.set('status', status);
    return this.http.get<Page<Organization>>(this.ADMIN, { params });
  }

  verifyOrg(id: string, req: AdminVerifyRequest): Observable<Organization> {
    return this.http.post<Organization>(`${this.ADMIN}/${id}/verify`, req);
  }

  suspendOrg(id: string, req: AdminSuspendRequest): Observable<Organization> {
    return this.http.post<Organization>(`${this.ADMIN}/${id}/suspend`, req);
  }

  reactivateOrg(id: string): Observable<Organization> {
    return this.http.post<Organization>(`${this.ADMIN}/${id}/reactivate`, null);
  }

  hardDeleteOrg(id: string): Observable<void> {
    return this.http.delete<void>(`${this.ADMIN}/${id}`);
  }

  forceDissolveOrg(id: string): Observable<void> {
    return this.http.delete<void>(`${this.ADMIN}/${id}/force-dissolve`);
  }

  getAuditLog(orgId: string, page = 0): Observable<Page<OrgAuditLog>> {
    const params = new HttpParams().set('page', page).set('size', 20);
    return this.http.get<Page<OrgAuditLog>>(`${this.ADMIN}/${orgId}/audit`, { params });
  }

  /** OrgDashboardStats aligné sur le backend (totalOrganizations, activeOrganizations, etc.) */
  getDashboardStats(): Observable<OrgDashboardStats> {
    return this.http.get<OrgDashboardStats>(`${this.ADMIN}/stats`);
  }

  // ── Portfolio ─────────────────────────────────────────────────────────────

  getPortfolio(orgId: string): Observable<OrgPortfolioItem[]> {
    return this.http.get<OrgPortfolioItem[] | Page<OrgPortfolioItem>>(`${this.BASE}/${orgId}/portfolio`).pipe(
      map(r => Array.isArray(r) ? r : (r?.content ?? []))
    );
  }

  createPortfolioItem(orgId: string, req: CreatePortfolioItemRequest): Observable<OrgPortfolioItem> {
    return this.http.post<OrgPortfolioItem>(`${this.BASE}/${orgId}/portfolio`, req);
  }

  updatePortfolioItem(orgId: string, itemId: string, req: CreatePortfolioItemRequest): Observable<OrgPortfolioItem> {
    return this.http.put<OrgPortfolioItem>(`${this.BASE}/${orgId}/portfolio/${itemId}`, req);
  }

  deletePortfolioItem(orgId: string, itemId: string): Observable<void> {
    return this.http.delete<void>(`${this.BASE}/${orgId}/portfolio/${itemId}`);
  }

  // ── Candidatures spontanées ───────────────────────────────────────────────

  applyToOrg(orgId: string, req: CreateApplicationRequest): Observable<OrgApplication> {
    return this.http.post<OrgApplication>(`${this.BASE}/${orgId}/applications`, req);
  }

  getOrgApplications(orgId: string, page = 0): Observable<Page<OrgApplication>> {
    const params = new HttpParams().set('page', page).set('size', 20);
    return this.http.get<Page<OrgApplication>>(`${this.BASE}/${orgId}/applications`, { params });
  }

  getMyOrgApplications(): Observable<OrgApplication[]> {
    return this.http.get<OrgApplication[] | Page<OrgApplication>>(`${this.BASE}/applications/mine`).pipe(
      map(r => Array.isArray(r) ? r : (r?.content ?? []))
    );
  }

  respondToApplication(orgId: string, appId: string, req: RespondApplicationRequest): Observable<OrgApplication> {
    return this.http.post<OrgApplication>(`${this.BASE}/${orgId}/applications/${appId}/respond`, req);
  }

  withdrawApplication(orgId: string, appId: string): Observable<void> {
    return this.http.delete<void>(`${this.BASE}/${orgId}/applications/${appId}/withdraw`);
  }

  // ── Demandes de devis (RFQ) ───────────────────────────────────────────────

  createRfq(orgId: string, req: CreateRfqRequest): Observable<OrgRfq> {
    return this.http.post<OrgRfq>(`${this.BASE}/${orgId}/rfq`, req);
  }

  getOrgRfqs(orgId: string, page = 0): Observable<Page<OrgRfq>> {
    const params = new HttpParams().set('page', page).set('size', 20);
    return this.http.get<Page<OrgRfq>>(`${this.BASE}/${orgId}/rfq`, { params });
  }

  getMyRfqs(): Observable<OrgRfq[]> {
    return this.http.get<OrgRfq[] | Page<OrgRfq>>(`${this.BASE}/rfq/mine`).pipe(
      map(r => Array.isArray(r) ? r : (r?.content ?? []))
    );
  }

  respondToRfq(orgId: string, rfqId: string, req: RfqResponseRequest): Observable<OrgRfq> {
    return this.http.post<OrgRfq>(`${this.BASE}/${orgId}/rfq/${rfqId}/respond`, req);
  }

  closeRfq(orgId: string, rfqId: string): Observable<void> {
    return this.http.post<void>(`${this.BASE}/${orgId}/rfq/${rfqId}/close`, null);
  }

  // ── Badges ────────────────────────────────────────────────────────────────

  getBadges(orgId: string): Observable<OrgBadgeInfo> {
    return this.http.get<OrgBadgeInfo>(`${this.BASE}/${orgId}/badges`);
  }

  // NOTE : recomputeBadges() supprimé — endpoint inexistant côté backend.
  // Les badges sont gérés par l'admin via POST/DELETE /badges/{badge}.

  assignBadge(orgId: string, badge: string): Observable<OrgBadgeInfo> {
    return this.http.post<OrgBadgeInfo>(`${this.ADMIN}/${orgId}/badges/${badge}`, null);
  }

  removeBadge(orgId: string, badge: string): Observable<OrgBadgeInfo> {
    return this.http.delete<OrgBadgeInfo>(`${this.ADMIN}/${orgId}/badges/${badge}`);
  }

  // ── Matching ──────────────────────────────────────────────────────────────

  /** Matching simple par filtres (rétrocompatibilité) */
  matchOrganizations(req: MatchingRequest): Observable<OrganizationSummary[]> {
    return this.http.post<OrganizationSummary[]>(`${this.BASE}/matching`, req);
  }

  /** Matching scoré : retourne les orgs triées par compatibilité avec le profil du freelance */
  matchOrganizationsScored(req: ScoredMatchingRequest): Observable<CompatibilityResult[]> {
    return this.http.post<CompatibilityResult[]>(`${this.BASE}/matching/scored`, req);
  }

  /** Matching offres de collaboration : retourne les offres OPEN triées par compatibilité */
  matchCollabOffers(req: CollabOfferMatchingRequest): Observable<CollabOfferMatchResult[]> {
    return this.http.post<CollabOfferMatchResult[]>(`${this.BASE}/matching/collab-offers`, req);
  }

  // ── Analytics ─────────────────────────────────────────────────────────────

  getOrgAnalytics(orgId: string): Observable<OrgAnalytics> {
    return this.http.get<OrgAnalytics>(`${this.BASE}/${orgId}/analytics`);
  }

  // ── TrustScore ────────────────────────────────────────────────────────────

  getTrustScore(orgId: string): Observable<TrustScore> {
    return this.http.get<TrustScore>(`${this.BASE}/${orgId}/trust-score`);
  }

  forceRecomputeTrustScore(orgId: string): Observable<TrustScore> {
    return this.http.post<TrustScore>(`${this.BASE}/${orgId}/trust-score/recompute`, {});
  }

  // ── Profile Completion Score ──────────────────────────────────────────────

  getCompletionScore(orgId: string): Observable<ProfileCompletionScore> {
    return this.http.get<ProfileCompletionScore>(`${this.BASE}/${orgId}/completion-score`);
  }

  // ── Géolocalisation ───────────────────────────────────────────────────────

  /**
   * Retourne les coordonnées (lat/lon) déjà stockées pour l'organisation.
   * Appel léger — aucun appel Nominatim déclenché.
   */
  getLocation(orgId: string): Observable<GeoLocationResponse> {
    return this.http.get<GeoLocationResponse>(`${this.BASE}/${orgId}/location`);
  }

  /**
   * Déclenche le géocodage à la demande (appel Nominatim côté backend),
   * persiste les coordonnées et retourne le résultat.
   * Réservé aux utilisateurs authentifiés (owner/manager).
   */
  geocodeOrganization(orgId: string): Observable<GeoLocationResponse> {
    return this.http.get<GeoLocationResponse>(`${this.BASE}/${orgId}/geocode`);
  }

  // ── Offres de collaboration ponctuelle ────────────────────────────────────

  private readonly COLLAB = `${environment.organizationsApiUrl}`;

  createCollabOffer(orgId: string, req: CreateCollabOfferRequest): Observable<CollabOffer> {
    return this.http.post<CollabOffer>(`${this.BASE}/${orgId}/collab-offers`, req);
  }

  getCollabOffers(orgId: string, page = 0, pageSize = 10): Observable<Page<CollabOffer>> {
    const params = new HttpParams().set('page', page).set('size', pageSize);
    return this.http.get<Page<CollabOffer>>(`${this.BASE}/${orgId}/collab-offers`, { params });
  }

  getCollabOffer(orgId: string, offerId: string): Observable<CollabOffer> {
    return this.http.get<CollabOffer>(`${this.BASE}/${orgId}/collab-offers/${offerId}`);
  }

  closeCollabOffer(orgId: string, offerId: string): Observable<CollabOffer> {
    return this.http.post<CollabOffer>(`${this.BASE}/${orgId}/collab-offers/${offerId}/close`, null);
  }

  cancelCollabOffer(orgId: string, offerId: string): Observable<CollabOffer> {
    return this.http.post<CollabOffer>(`${this.BASE}/${orgId}/collab-offers/${offerId}/cancel`, null);
  }

  applyToCollabOffer(offerId: string, req: ApplyCollabOfferRequest): Observable<CollabApplication> {
    return this.http.post<CollabApplication>(`${this.COLLAB}/collab-offers/${offerId}/apply`, req);
  }

  getCollabApplicationsForOffer(offerId: string, page = 0): Observable<Page<CollabApplication>> {
    const params = new HttpParams().set('page', page).set('size', 20);
    return this.http.get<Page<CollabApplication>>(`${this.COLLAB}/collab-offers/${offerId}/applications`, { params });
  }

  respondToCollabApplication(applicationId: string, req: RespondCollabApplicationRequest): Observable<CollabApplication> {
    return this.http.post<CollabApplication>(`${this.COLLAB}/collab-offers/applications/${applicationId}/respond`, req);
  }

  withdrawCollabApplication(applicationId: string): Observable<void> {
    return this.http.delete<void>(`${this.COLLAB}/collab-offers/applications/${applicationId}/withdraw`);
  }

  getMyCollabApplications(): Observable<CollabApplication[]> {
    return this.http.get<CollabApplication[]>(`${this.COLLAB}/collab-offers/my-applications`);
  }

  // ── RGPD ──────────────────────────────────────────────────────────────────

  exportGdprData(): Observable<Blob> {
    return this.http.get(`${this.GDPR}/export`, { responseType: 'blob' });
  }

  deleteGdprData(): Observable<void> {
    return this.http.delete<void>(`${this.GDPR}/delete`);
  }
}
