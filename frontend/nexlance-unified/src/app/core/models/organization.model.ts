// ── Enums ─────────────────────────────────────────────────────────────────────

// Uniquement des entités côté offre (groupes de freelances)
export enum OrganizationType {
  AGENCY         = 'AGENCY',        // Agence de freelances
  STARTUP        = 'STARTUP',       // Startup fondée par des freelances
  ASSOCIATION    = 'ASSOCIATION',   // Collectif associatif
  FREELANCE_COOP = 'FREELANCE_COOP' // Coopérative de freelances
}

export enum OrganizationStatus {
  PENDING_VERIFICATION = 'PENDING_VERIFICATION',
  AWAITING_INFO        = 'AWAITING_INFO',
  ACTIVE               = 'ACTIVE',
  SUSPENDED            = 'SUSPENDED',
  DISSOLVED            = 'DISSOLVED',
  REJECTED             = 'REJECTED'
}

export enum OrganizationSize {
  SOLO       = 'SOLO',
  SMALL      = 'SMALL',
  MEDIUM     = 'MEDIUM',
  LARGE      = 'LARGE',
  ENTERPRISE = 'ENTERPRISE'    // ← ajouté (existait backend)
}

export enum OrganizationVisibility {
  PUBLIC       = 'PUBLIC',
  PRIVATE      = 'PRIVATE',
  MEMBERS_ONLY = 'MEMBERS_ONLY'  // ← ajouté (existait backend)
}

export enum MemberRole {
  OWNER   = 'OWNER',
  MANAGER = 'MANAGER',
  MEMBER  = 'MEMBER'
}

export enum MemberStatus {
  ACTIVE    = 'ACTIVE',
  INACTIVE  = 'INACTIVE',   // ← corrigé (était REMOVED — inexistant backend)
  SUSPENDED = 'SUSPENDED'   // ← corrigé (était LEFT — inexistant backend)
}

export enum InvitationStatus {
  PENDING   = 'PENDING',
  ACCEPTED  = 'ACCEPTED',
  DECLINED  = 'DECLINED',
  EXPIRED   = 'EXPIRED',
  CANCELLED = 'CANCELLED'
}

export enum ApplicationStatus {
  PENDING   = 'PENDING',
  ACCEPTED  = 'ACCEPTED',
  REJECTED  = 'REJECTED',
  WITHDRAWN = 'WITHDRAWN'
}

export enum RfqStatus {
  PENDING   = 'PENDING',
  RESPONDED = 'RESPONDED',
  CLOSED    = 'CLOSED'
}

export enum TrustBadge {
  VERIFIED       = 'VERIFIED',
  TOP_RATED      = 'TOP_RATED',
  EXPERIENCED    = 'EXPERIENCED',
  FAST_RESPONDER = 'FAST_RESPONDER',
  PREMIUM        = 'PREMIUM'
}

export enum CollabOfferStatus {
  OPEN      = 'OPEN',
  CLOSED    = 'CLOSED',
  CANCELLED = 'CANCELLED'
}

export enum CollabApplicationStatus {
  PENDING   = 'PENDING',
  ACCEPTED  = 'ACCEPTED',
  REJECTED  = 'REJECTED',
  WITHDRAWN = 'WITHDRAWN'
}

// ── Page wrapper (Spring Data) ────────────────────────────────────────────────

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// ── Response interfaces ───────────────────────────────────────────────────────

export interface Organization {
  id: string;
  name: string;
  description: string | null;
  logoUrl: string | null;
  website: string | null;
  type: OrganizationType;
  specialties: string[];
  location: string | null;
  siret: string | null;
  size: OrganizationSize;
  status: OrganizationStatus;
  visibility: OrganizationVisibility;
  ownerId: string;
  averageRating: number;
  completedProjectsCount: number;
  reviewCount: number;
  trustLevel: number;
  adminNote: string | null;
  createdAt: string;
  updatedAt: string;
  dissolvedAt: string | null;
}

export interface OrganizationSummary {
  id: string;
  name: string;
  logoUrl: string | null;
  type: OrganizationType;
  status: OrganizationStatus;
  location: string | null;
  averageRating: number;
  memberCount: number;
  // Optional enriched fields returned by some endpoints
  size?: OrganizationSize;
  specialties?: string[];
  completedProjectsCount?: number;
  reviewCount?: number;
  /** Present only on GET /api/organizations/my — role of the authenticated user in this org */
  myRole?: MemberRole;
}

export interface OrgMember {
  id: string;
  organizationId: string;
  userId: string;
  role: MemberRole;
  status: MemberStatus;
  displayOnProfile: boolean;
  joinedAt: string;
  leftAt: string | null;
}

export interface OrgInvitation {
  id: string;
  organizationId: string;
  organizationName?: string | null; // present in token-resolve responses
  inviterId: string;           // ← corrigé (était invitedByUserId)
  inviteeId: string | null;    // ← corrigé (était invitedUserId)
  inviteeEmail: string | null; // ← corrigé (était invitedEmail)
  role: MemberRole;            // ← corrigé (était proposedRole)
  status: InvitationStatus;
  message: string | null;
  expiresAt: string;
  createdAt: string;
  respondedAt: string | null;
}

export interface OrgReview {
  id: string;
  organizationId: string;
  reviewerId: string;          // ← corrigé (était clientId)
  projectId: string | null;
  rating: number;              // ← corrigé : note unique 1-5 (remplace 4 sous-notes)
  comment: string | null;
  reply: string | null;        // ← corrigé (était ownerReply)
  replyAt: string | null;
  reported: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface OrgAuditLog {
  id: string;
  organizationId: string;
  performedByUserId: string;
  action: string;
  details: string | null;
  createdAt: string;           // ← corrigé (était performedAt — inexistant backend)
}

/** Correspond exactement au OrganizationDashboardStats du backend */
export interface OrgDashboardStats {
  totalOrganizations: number;        // ← corrigé (était total)
  activeOrganizations: number;       // ← corrigé (était dans countByStatus)
  pendingVerification: number;
  suspended: number;
  dissolved: number;
}

export interface OrgPortfolioItem {
  id: string;
  organizationId: string;
  title: string;
  description: string | null;
  imageUrl: string | null;
  projectUrl: string | null;
  clientName: string | null;
  tags: string[];
  completedAt: string | null;
  createdAt: string;
}

export interface OrgApplication {
  id: string;
  organizationId: string;
  applicantId: string;
  message: string;
  cvUrl: string | null;
  status: ApplicationStatus;
  rejectionReason: string | null;
  createdAt: string;
  respondedAt: string | null;
}

export interface OrgRfq {
  id: string;
  organizationId: string;
  requesterId: string;
  title: string;
  description: string;
  budgetMin: number | null;
  budgetMax: number | null;
  deadline: string | null;
  skillsNeeded: string[];
  status: RfqStatus;
  responseMessage: string | null;
  respondedById: string | null;
  createdAt: string;
  respondedAt: string | null;
}

export interface OrgBadgeInfo {
  organizationId: string;
  badges: TrustBadge[];
}

// ── Analytics ─────────────────────────────────────────────────────────────────

export interface OrgMemberStats {
  total: number; active: number; inactive: number;
  owners: number; managers: number; members: number;
}
export interface OrgApplicationStats {
  total: number; pending: number; accepted: number;
  rejected: number; withdrawn: number; acceptanceRate: number | null;
}
export interface OrgReviewStats {
  total: number; average: number; withReply: number; reported: number;
  distribution: Record<string, number>; // "1"–"5" → count
}
export interface OrgCollabStats {
  totalOffers: number; openOffers: number; closedOffers: number; cancelledOffers: number;
  totalApplications: number; acceptedApplications: number; pendingApplications: number;
  applicationAcceptanceRate: number | null;
}
export interface OrgRfqStats {
  total: number; pending: number; responded: number; closed: number;
  responseRate: number | null;
}
export interface OrgInvitationStats {
  total: number; pending: number; accepted: number; declined: number;
  expired: number; cancelled: number; acceptanceRate: number | null;
}
export interface OrgAnalytics {
  organizationId: string;
  organizationName: string;
  averageRating: number;
  trustLevel: number;
  createdAt: string;
  daysActive: number;
  members: OrgMemberStats;
  applications: OrgApplicationStats;
  reviews: OrgReviewStats;
  collab: OrgCollabStats;
  rfq: OrgRfqStats;
  invitations: OrgInvitationStats;
}

// ── Matching scoré ────────────────────────────────────────────────────────────

export interface ScoredMatchingRequest {
  freelancerSkills?: string[];
  freelancerLocation?: string;
  preferredType?: OrganizationType;
  preferredSize?: OrganizationSize;
  minScore?: number;
  limit?: number;
}

export interface OrgScoreBreakdown {
  skillScore: number;    matchedSkillCount: number; requiredSkillCount: number;
  trustScore: number;    trustLevel: number;
  locationScore: number; locationMatch: 'EXACT' | 'REGION' | 'NONE' | 'NOT_SPECIFIED';
  typeScore: number;     typeMatch: 'EXACT' | 'NONE' | 'NOT_SPECIFIED';
  ratingScore: number;   averageRating: number;
}

export interface CompatibilityResult {
  organization: OrganizationSummary;
  compatibilityScore: number;
  breakdown: OrgScoreBreakdown;
}

export interface CollabOfferMatchingRequest {
  freelancerSkills?: string[];
  freelancerLocation?: string;
  minBudget?: number;
  minScore?: number;
  limit?: number;
}

export interface CollabOfferScoreBreakdown {
  skillScore: number;    matchedSkillCount: number; offerSkillCount: number;
  freshnessScore: number; offerAgeDays: number;
  trustScore: number;
  budgetScore: number;   budgetMatch: 'MATCH' | 'NO_MATCH' | 'NOT_SPECIFIED';
  locationScore: number; locationMatch: 'EXACT' | 'REGION' | 'NONE' | 'NOT_SPECIFIED';
}

export interface CollabOfferMatchResult {
  offer: CollabOffer;
  organizationName: string;
  organizationTrustLevel: number;
  compatibilityScore: number;
  breakdown: CollabOfferScoreBreakdown;
}

// ── TrustScore ────────────────────────────────────────────────────────────────

export interface TrustScoreBreakdown {
  ratingScore: number;      averageRating: number;       reviewCount: number;
  rfqScore: number;         rfqResponseRate: number;     rfqTotal: number;
  replyScore: number;       reviewReplyRate: number;     reviewsWithReply: number;
  maturityScore: number;    daysActive: number;
  invitationScore: number;  invitationAcceptanceRate: number; invitationDecided: number;
  projectScore: number;     completedProjectsCount: number;
}

export interface TrustScore {
  organizationId: string;
  organizationName: string;
  globalScore: number;
  trustLevel: number;
  badges: TrustBadge[];
  breakdown: TrustScoreBreakdown;
}

export interface CollabOffer {
  id: string;
  organizationId: string;
  createdBy: string;
  title: string;
  description: string;
  requiredSkills: string[];
  durationLabel: string | null;
  budgetEstimate: number | null;
  maxApplicants: number | null;
  deadlineDate: string | null;
  status: CollabOfferStatus;
  applicationCount: number;
  acceptedCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CollabApplication {
  id: string;
  offerId: string;
  organizationId: string;
  applicantId: string;
  message: string;
  portfolioUrl: string | null;
  status: CollabApplicationStatus;
  rejectionReason: string | null;
  createdAt: string;
  respondedAt: string | null;
  offerTitle: string;
}

// ── Géolocalisation ───────────────────────────────────────────────────────────

export interface GeoLocationResponse {
  organizationId: string;
  organizationName: string;
  /** Adresse textuelle stockée sur l'organisation */
  address: string | null;
  /** Latitude WGS84 — null si non géocodé */
  latitude: number | null;
  /** Longitude WGS84 — null si non géocodé */
  longitude: number | null;
  /** true si lat/lon sont disponibles */
  geocoded: boolean;
}

// ── Profile Completion Score ──────────────────────────────────────────────────

export interface ProfileCompletionBreakdown {
  hasDescription: boolean;
  hasLogo: boolean;
  hasWebsite: boolean;
  hasSpecialties: boolean;
  hasSiret: boolean;
  hasLocation: boolean;
  hasPortfolio: boolean;
  hasTeamMember: boolean;
  missingItems: string[];
}

export interface ProfileCompletionScore {
  organizationId: string;
  organizationName: string;
  /** Score global 0–100 */
  score: number;
  /** true si score >= 40 (visible en recherche publique) */
  visibleInSearch: boolean;
  breakdown: ProfileCompletionBreakdown;
}

// ── Request DTOs ──────────────────────────────────────────────────────────────

export interface CreateOrganizationRequest {
  name: string;
  description?: string;
  logoUrl?: string;
  website?: string;
  type: OrganizationType;
  specialties?: string[];
  location?: string;
  siret?: string;
  size?: OrganizationSize;
  visibility?: OrganizationVisibility;
}

export interface UpdateOrganizationRequest {
  name?: string;
  description?: string;
  logoUrl?: string;
  website?: string;
  specialties?: string[];
  location?: string;
  size?: OrganizationSize;
}

export interface TransferOwnershipRequest {
  newOwnerId: string;
}

/** Correspond exactement à InviteMemberRequest du backend */
export interface InviteMemberRequest {
  inviteeId?: string;    // ← corrigé (était invitedUserId)
  inviteeEmail?: string; // ← corrigé (était invitedEmail)
  role?: MemberRole;     // ← corrigé (était proposedRole)
  message?: string;
}

/** Note unique 1-5 — correspond à CreateReviewRequest du backend */
export interface CreateReviewRequest {
  rating: number;        // ← corrigé : note unique (remplace les 4 sous-notes)
  comment?: string;
  projectId?: string;
}

export interface AdminVerifyRequest {
  decision: 'APPROVE' | 'REJECT' | 'AWAITING_INFO';
  adminNote?: string;   // backend accepte adminNote ou note
}

export interface AdminSuspendRequest {
  reason: string;
}

export interface CreatePortfolioItemRequest {
  title: string;
  description?: string;
  imageUrl?: string;
  projectUrl?: string;
  clientName?: string;
  tags?: string[];
  completedAt?: string;
}

export interface CreateApplicationRequest {
  message: string;
  cvUrl?: string;
}

export interface RespondApplicationRequest {
  status: 'ACCEPTED' | 'REJECTED';
  rejectionReason?: string;
}

export interface CreateRfqRequest {
  title: string;
  description: string;
  budgetMin?: number;
  budgetMax?: number;
  deadline?: string;
  skillsNeeded?: string[];
}

export interface RfqResponseRequest {
  responseMessage: string;
}

/** Correspond exactement à MatchingRequest du backend */
export interface MatchingRequest {
  requiredSkills?: string[];     // ← corrigé (était requiredSpecialties)
  preferredType?: OrganizationType;
  preferredSize?: OrganizationSize; // ← ajouté (existait backend)
  location?: string;                // ← ajouté (existait backend)
}

export interface CreateCollabOfferRequest {
  title: string;
  description: string;
  requiredSkills?: string[];
  durationLabel?: string;
  budgetEstimate?: number;
  maxApplicants?: number;
  deadlineDate?: string;
}

export interface ApplyCollabOfferRequest {
  message: string;
  portfolioUrl?: string;
}

export interface RespondCollabApplicationRequest {
  status: CollabApplicationStatus;
  rejectionReason?: string;
}
