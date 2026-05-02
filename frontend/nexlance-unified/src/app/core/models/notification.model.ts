/**
 * Modèle de notification étendu pour couvrir les réclamations.
 *
 * Types ajoutés :
 *   COMPLAINT_CREATED  → accusé de réception
 *   COMPLAINT_STATUS   → changement de statut
 *   COMPLAINT_RESOLVED → résolution
 *   COMPLAINT_CLOSED   → clôture
 *   COMPLAINT_MESSAGE  → nouveau message dans une conversation
 *   COMPLAINT_INVOLVED → la partie mise en cause est impliquée
 *   COMPLAINT_ASSIGNED → réclamation assignée à l'agent
 *   COMPLAINT_ESCALATED→ escalade
 */
export type NotificationType =
  // ── Notifications existantes ──────────────────────────────
  | 'JOB_OFFER'
  | 'APPLICATION'
  | 'RECOMMENDATION'
  | 'INVITATION'
  | 'PROJECT'
  | 'MILESTONE'
  | 'DEADLINE'
  | 'OVERDUE'
  // ── Nouvelles : module réclamations ───────────────────────
  | 'COMPLAINT_CREATED'
  | 'COMPLAINT_STATUS'
  | 'COMPLAINT_RESOLVED'
  | 'COMPLAINT_CLOSED'
  | 'COMPLAINT_MESSAGE'
  | 'COMPLAINT_INVOLVED'
  | 'COMPLAINT_ASSIGNED'
  | 'COMPLAINT_ESCALATED';

export interface AppNotification {
  id?: string;
  recipientId: string;
  type: NotificationType;
  title: string;
  message: string;
  referenceId?: string;    // complaintId ou autre ID métier
  referenceType?: string;  // 'COMPLAINT' | 'JOB_OFFER' | …
  read: boolean;
  createdAt?: string;
  readAt?: string;
}

// ── Helpers ─────────────────────────────────────────────────

export const COMPLAINT_NOTIFICATION_TYPES: NotificationType[] = [
  'COMPLAINT_CREATED',
  'COMPLAINT_STATUS',
  'COMPLAINT_RESOLVED',
  'COMPLAINT_CLOSED',
  'COMPLAINT_MESSAGE',
  'COMPLAINT_INVOLVED',
  'COMPLAINT_ASSIGNED',
  'COMPLAINT_ESCALATED'
];

export function isComplaintNotification(type: NotificationType): boolean {
  return COMPLAINT_NOTIFICATION_TYPES.includes(type);
}