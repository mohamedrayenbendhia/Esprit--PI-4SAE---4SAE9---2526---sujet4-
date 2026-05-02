// ============================================================
// ENUMS
// ============================================================

export enum ComplaintStatus {
  OPEN         = 'OPEN',
  IN_PROGRESS  = 'IN_PROGRESS',
  PENDING_USER = 'PENDING_USER',
  RESOLVED     = 'RESOLVED',
  CLOSED       = 'CLOSED',
  ESCALATED    = 'ESCALATED'
}

export enum ComplaintPriority {
  LOW      = 'LOW',
  MEDIUM   = 'MEDIUM',
  HIGH     = 'HIGH',
  CRITICAL = 'CRITICAL'
}

export enum ComplaintCategory {
  PAYMENT_ISSUE         = 'PAYMENT_ISSUE',
  QUALITY_DISPUTE       = 'QUALITY_DISPUTE',
  COMMUNICATION_PROBLEM = 'COMMUNICATION_PROBLEM',
  HARASSMENT            = 'HARASSMENT',
  SCAM                  = 'SCAM',
  TECHNICAL_ISSUE       = 'TECHNICAL_ISSUE',
  OTHER                 = 'OTHER'
}

export enum ResolutionType {
  REFUND             = 'REFUND',
  WARNING            = 'WARNING',
  ACCOUNT_SUSPENSION = 'ACCOUNT_SUSPENSION',
  NO_ACTION          = 'NO_ACTION',
  MEDIATION          = 'MEDIATION'
}

export enum MessageType {
  TEXT         = 'TEXT',
  NOTE_INTERNE = 'NOTE_INTERNE',
  RESOLUTION   = 'RESOLUTION',
  AUTO_RESPONSE = 'AUTO_RESPONSE'
}

export enum SenderType {
  USER    = 'USER',
  SUPPORT = 'SUPPORT',
  SYSTEM  = 'SYSTEM'
}

/**
 * Type de conversation dans une réclamation.
 * COMPLAINANT = fil plaignant ↔ support
 * REPORTED    = fil partie mise en cause ↔ support
 */
export enum ConversationType {
  COMPLAINANT = 'COMPLAINANT',
  REPORTED    = 'REPORTED'
}

// ============================================================
// INTERFACES PRINCIPALES
// ============================================================

export interface Complaint {
  id: string;
  ticketNumber: string;
  reporterId: string;
  reportedUserId?: string;
  projectId?: string;
  assignedToId?: string;
  category: ComplaintCategory;
  priority: ComplaintPriority;
  status: ComplaintStatus;
  subject: string;
  description: string;
  attachments?: string[];
  resolution?: string;
  resolutionType?: ResolutionType;
  satisfactionRating?: number;
  createdAt: Date;
  updatedAt: Date;
  resolvedAt?: Date;
  closedAt?: Date;
  firstResponseAt?: Date;

  // ── ML Escalation Prediction fields ──────────────────────────
  messageCount?        : number;   // nombre de messages échangés
  disputeAmount?       : number;   // montant du litige (€)
  userTenureMonths?    : number;   // ancienneté du client (mois)
  totalProjects?       : number;   // nb total de projets
  previousComplaints?  : number;   // réclamations antérieures
  averageRating?       : number;   // note moyenne reçue
  accountType?         : string;   // 'client' | 'freelancer'
  responseTimeHours?   : number;   // temps de 1re réponse (h)
  nbRelances?          : number;   // nb de relances envoyées
  contractValue?       : number;   // valeur du contrat (€)
}

export interface SupportMessage {
  id: string;
  complaintId: string;
  senderId: string;
  senderType: SenderType;
  messageType: MessageType;
  conversationType: ConversationType;
  content: string;
  attachments?: string[];
  isRead: boolean;
  readAt?: Date;
  createdAt: Date;
}

// ============================================================
// DTOs — REQUÊTES
// ============================================================

export interface CreateComplaintRequest {
  category: ComplaintCategory;
  priority?: ComplaintPriority;
  subject: string;
  description: string;
  reportedUserEmail?: string;
  projectId?: string;
  attachments?: string[];
}

export interface CreateMessageRequest {
  complaintId: string;
  content: string;
  messageType?: MessageType;
  conversationType?: ConversationType;
  attachments?: string[];
}

export interface ResolveComplaintRequest {
  resolution: string;
  resolutionType: ResolutionType;
}

export interface InvolveReportedRequest {
  invitationMessage: string;
}

// ============================================================
// LABELS — AFFICHAGE
// ============================================================

export const STATUS_LABELS: Record<ComplaintStatus, string> = {
  [ComplaintStatus.OPEN]:         'Open',
  [ComplaintStatus.IN_PROGRESS]:  'In Progress',
  [ComplaintStatus.PENDING_USER]: 'Pending',
  [ComplaintStatus.RESOLVED]:     'Resolved',
  [ComplaintStatus.CLOSED]:       'Closed',
  [ComplaintStatus.ESCALATED]:    'Escalated'
};

export const STATUS_COLORS: Record<ComplaintStatus, string> = {
  [ComplaintStatus.OPEN]:         '#1976d2',
  [ComplaintStatus.IN_PROGRESS]:  '#f57c00',
  [ComplaintStatus.PENDING_USER]: '#7b1fa2',
  [ComplaintStatus.RESOLVED]:     '#388e3c',
  [ComplaintStatus.CLOSED]:       '#616161',
  [ComplaintStatus.ESCALATED]:    '#c62828'
};

export const PRIORITY_LABELS: Record<ComplaintPriority, string> = {
  [ComplaintPriority.LOW]:      'Low',
  [ComplaintPriority.MEDIUM]:   'Medium',
  [ComplaintPriority.HIGH]:     'High',
  [ComplaintPriority.CRITICAL]: 'Critical'
};

export const PRIORITY_COLORS: Record<ComplaintPriority, string> = {
  [ComplaintPriority.LOW]:      '#43a047',
  [ComplaintPriority.MEDIUM]:   '#fb8c00',
  [ComplaintPriority.HIGH]:     '#e53935',
  [ComplaintPriority.CRITICAL]: '#b71c1c'
};

export const CATEGORY_LABELS: Record<ComplaintCategory, string> = {
  [ComplaintCategory.PAYMENT_ISSUE]:         'Payment issue',
  [ComplaintCategory.QUALITY_DISPUTE]:       'Quality dispute',
  [ComplaintCategory.COMMUNICATION_PROBLEM]: 'Communication problem',
  [ComplaintCategory.HARASSMENT]:            'Harassment',
  [ComplaintCategory.SCAM]:                  'Scam',
  [ComplaintCategory.TECHNICAL_ISSUE]:       'Technical issue',
  [ComplaintCategory.OTHER]:                 'Other'
};

export const CONVERSATION_LABELS: Record<ConversationType, string> = {
  [ConversationType.COMPLAINANT]: 'My conversation',
  [ConversationType.REPORTED]:    'Reported party'
};