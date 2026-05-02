-- ============================================================
--  TEST DATA — notification-service
--  Base de données : smart_freelance_notifications
--  Exécuter via : mysql -u root smart_freelance_notifications < test-data-notifications.sql
--
--  Couvre les user stories : US-N01 à US-N12
--  22 notifications : lues/non lues, 4 types distincts, 9 destinataires
-- ============================================================

USE smart_freelance_notifications;

-- ─────────────────────────────────────────────────────────────
--  UUIDs de référence
-- ─────────────────────────────────────────────────────────────
-- alice  = '17355b7e-9642-4861-8f87-b7a5d9cfa973'  CLIENT
-- bob    = '6d1d18e1-813a-4dc0-a89f-6ace6f2a8a61'  FREELANCE
-- marc   = 'adcd3be5-c66b-4708-a863-d9fc07c4a9f5'  SUPPORT_AGENT
-- admin  = '9a3af34e-605d-4514-bd9b-4c2a2fb70413'  ADMIN
-- clara  = 'fa100001-0000-4001-8001-000000000001'  FREELANCE
-- david  = 'fa100002-0000-4001-8001-000000000002'  CLIENT
-- sophie = 'fa100003-0000-4001-8001-000000000003'  SUPPORT_AGENT
-- thomas = 'fa100004-0000-4001-8001-000000000004'  FREELANCE
-- lea    = 'fa100005-0000-4001-8001-000000000005'  FREELANCE
-- ─────────────────────────────────────────────────────────────
-- Types utilisés :
--   COMPLAINT_SUBMITTED, COMPLAINT_ASSIGNED, COMPLAINT_STATUS_CHANGED,
--   COMPLAINT_RESOLVED, COMPLAINT_ESCALATED, COMPLAINT_CLOSED,
--   COMPLAINT_RECEIVED, COMPLAINT_REOPENED, COMPLAINT_CRITICAL,
--   ORG_INVITATION_RECEIVED, ORG_INVITATION_ACCEPTED, ORG_INVITATION_DECLINED,
--   ORG_APPLICATION_SUBMITTED, ORG_APPLICATION_REJECTED,
--   ORG_MEMBER_JOINED, MEDIATION_OPENED, USER_SANCTIONED,
--   NPS_SURVEY_INVITATION, SYSTEM_ANNOUNCEMENT
-- ─────────────────────────────────────────────────────────────

INSERT IGNORE INTO notifications
    (id, recipient_id, type, title, message,
     reference_id, reference_type,
     is_read, created_at, read_at)
VALUES

-- ══════════════════════════════════════════════════════════════
--  MODULE RÉCLAMATIONS
-- ══════════════════════════════════════════════════════════════

-- ── US-N01 : Alice reçoit confirmation de soumission (COMP-001) ───────────────
('no000001-0000-4000-8000-000000000001',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973',  -- recipient: Alice
 'COMPLAINT_SUBMITTED',
 'Your complaint has been submitted',
 'Your complaint NX-2025-0001 regarding "Freelancer did not deliver the agreed work and is refusing to refund" has been received. An agent will be assigned shortly.',
 'cc000001-0000-4000-8000-000000000001', 'COMPLAINT',
 0, '2025-01-25 14:32:01', NULL),

-- ── US-N02 : Bob est notifié qu'une réclamation le vise (COMP-001) ─────────────
('no000002-0000-4000-8000-000000000002',
 '6d1d18e1-813a-4dc0-a89f-6ace6f2a8a61',  -- recipient: Bob
 'COMPLAINT_RECEIVED',
 'A complaint has been filed against you',
 'A complaint (NX-2025-0001) of type Payment Issue has been filed against your account. Our support team will review the case and contact you shortly.',
 'cc000001-0000-4000-8000-000000000001', 'COMPLAINT',
 0, '2025-01-25 14:32:05', NULL),

-- ── US-N03 : Marc reçoit une affectation (COMP-002) ───────────────────────────
('no000003-0000-4000-8000-000000000003',
 'adcd3be5-c66b-4708-a863-d9fc07c4a9f5',  -- recipient: Marc
 'COMPLAINT_ASSIGNED',
 'A complaint has been assigned to you',
 'Complaint NX-2025-0002 (Quality Dispute — HIGH priority) has been assigned to you. Reporter: Alice Dupont. Please review and respond within your SLA window.',
 'cc000002-0000-4000-8000-000000000002', 'COMPLAINT',
 1, '2025-02-03 11:31:00', '2025-02-03 11:35:00'),

-- ── US-N04 : Alice est informée que son dossier est en cours (COMP-002) ─────────
('no000004-0000-4000-8000-000000000004',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973',  -- recipient: Alice
 'COMPLAINT_STATUS_CHANGED',
 'Your complaint is now in progress',
 'Good news — complaint NX-2025-0002 has been assigned to a support agent and is now In Progress. Agent Marc Leblanc will be in touch shortly.',
 'cc000002-0000-4000-8000-000000000002', 'COMPLAINT',
 1, '2025-02-03 11:40:02', '2025-02-03 12:00:00'),

-- ── US-N05 : Clara est notifiée d'une réclamation la visant (COMP-004) ─────────
('no000005-0000-4000-8000-000000000005',
 'fa100001-0000-4001-8001-000000000001',  -- recipient: Clara (reported in COMP-004)
 'COMPLAINT_RECEIVED',
 'A complaint has been filed against you',
 'A complaint (NX-2025-0004) of type Harassment has been filed against your account. A senior agent has been assigned. You will be contacted separately.',
 'cc000004-0000-4000-8000-000000000004', 'COMPLAINT',
 1, '2025-02-14 08:55:05', '2025-02-14 10:00:00'),

-- ── US-N06 : Sophie reçoit l'escalade (COMP-004) ──────────────────────────────
('no000006-0000-4000-8000-000000000006',
 'fa100003-0000-4001-8001-000000000003',  -- recipient: Sophie
 'COMPLAINT_ESCALATED',
 'A complaint has been escalated to you',
 'Complaint NX-2025-0004 (Harassment — HIGH priority) has been escalated and reassigned to you. This case involves physical threats. Please review the evidence immediately.',
 'cc000004-0000-4000-8000-000000000004', 'COMPLAINT',
 0, '2025-02-14 08:52:00', NULL),

-- ── US-N07 : Alice (COMP-005) — réclamation résolue avec remboursement ─────────
('no000007-0000-4000-8000-000000000007',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973',  -- recipient: Alice
 'COMPLAINT_RESOLVED',
 'Your complaint has been resolved — refund initiated',
 'Complaint NX-2025-0005 has been resolved. Our investigation confirmed a scam. A full refund of 600€ has been initiated to your account (3-5 business days). A 50€ platform credit has also been applied.',
 'cc000005-0000-4000-8000-000000000005', 'COMPLAINT',
 1, '2025-01-28 16:30:05', '2025-01-28 17:00:00'),

-- ── US-N08 : Alice reçoit une enquête NPS après résolution (COMP-005) ──────────
('no000008-0000-4000-8000-000000000008',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973',  -- recipient: Alice
 'NPS_SURVEY_INVITATION',
 'How was your support experience?',
 'Your complaint NX-2025-0005 has been resolved. Would you take 2 minutes to rate your experience? Your feedback helps us improve our support team.',
 'np000001-0000-4000-8000-000000000001', 'NPS_SURVEY',
 1, '2025-01-28 17:00:00', '2025-01-29 10:00:00'),

-- ── US-N09 : David — réclamation clôturée (COMP-006) ──────────────────────────
('no000009-0000-4000-8000-000000000009',
 'fa100002-0000-4001-8001-000000000002',  -- recipient: David
 'COMPLAINT_CLOSED',
 'Your complaint has been closed',
 'Complaint NX-2025-0006 has been officially closed following successful mediation. The agreed refund of 350€ has been processed. This case is now archived.',
 'cc000006-0000-4000-8000-000000000006', 'COMPLAINT',
 1, '2025-02-19 09:00:05', '2025-02-19 11:00:00'),

-- ── US-N09 : David — invitation NPS (COMP-006) ────────────────────────────────
('no000010-0000-4000-8000-000000000010',
 'fa100002-0000-4001-8001-000000000002',  -- recipient: David
 'NPS_SURVEY_INVITATION',
 'How satisfied are you with our resolution?',
 'Complaint NX-2025-0006 is now closed. Please share your feedback on our mediation process — it takes only 1 minute.',
 'np000002-0000-4000-8000-000000000002', 'NPS_SURVEY',
 0, '2025-02-19 09:30:00', NULL),

-- ── US-N10 : Alice — réclamation CRITIQUE escaladée (COMP-007) ───────────────
('no000011-0000-4000-8000-000000000011',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973',  -- recipient: Alice
 'COMPLAINT_ESCALATED',
 'Your complaint has been escalated to CRITICAL',
 'Complaint NX-2025-0007 has been upgraded to CRITICAL priority. A formal mediation session has been opened. Our legal team will contact you within 1 business day. Bob Martin''s account has been suspended.',
 'cc000007-0000-4000-8000-000000000007', 'COMPLAINT',
 0, '2025-02-21 09:00:05', NULL),

-- ── US-N11 : Admin notifié d'une réclamation CRITIQUE (COMP-007) ──────────────
('no000012-0000-4000-8000-000000000012',
 '9a3af34e-605d-4514-bd9b-4c2a2fb70413',  -- recipient: Admin
 'COMPLAINT_CRITICAL',
 '[CRITICAL] New complaint requires immediate attention',
 'Complaint NX-2025-0007 has been flagged CRITICAL: phishing + extortion reported. Bob Martin''s account has been suspended automatically. Mediation session opened. Action required.',
 'cc000007-0000-4000-8000-000000000007', 'COMPLAINT',
 1, '2025-02-20 07:22:00', '2025-02-20 07:25:00'),

-- ── US-N12 : Alice — médiation formelle ouverte (COMP-007) ───────────────────
('no000013-0000-4000-8000-000000000013',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973',  -- recipient: Alice
 'MEDIATION_OPENED',
 'A formal mediation session has been opened for your case',
 'A mediation session has been opened for complaint NX-2025-0007. You have until February 28, 2025 to submit your evidence. Please upload all supporting documents through the complaint portal.',
 'ms000001-0000-4000-8000-000000000001', 'MEDIATION_SESSION',
 0, '2025-02-21 09:01:00', NULL),

-- ── US-N13 : Bob — sanction (suspension de compte) ────────────────────────────
('no000014-0000-4000-8000-000000000014',
 '6d1d18e1-813a-4dc0-a89f-6ace6f2a8a61',  -- recipient: Bob
 'USER_SANCTIONED',
 'Your account has been suspended',
 'Your NexLance account has been suspended pending investigation of complaint NX-2025-0007. You may not take on new projects or receive payments during this period. If you believe this is an error, contact support@nexlance.com.',
 'sa000001-0000-4000-8000-000000000001', 'USER_SANCTION',
 0, '2025-02-21 10:00:05', NULL),

-- ── US-N14 : Marc — file d'attente (COMP-001 non assigné) ─────────────────────
('no000015-0000-4000-8000-000000000015',
 'adcd3be5-c66b-4708-a863-d9fc07c4a9f5',  -- recipient: Marc
 'COMPLAINT_STATUS_CHANGED',
 'New complaint in the queue',
 'A new complaint NX-2025-0001 (Payment Issue — MEDIUM) is available for assignment in your queue. Reporter: Alice Dupont.',
 'cc000001-0000-4000-8000-000000000001', 'COMPLAINT',
 1, '2025-01-25 14:33:00', '2025-01-25 15:00:00'),

-- ── US-N14 : Léa — réclamation rouverte (COMP-010) ────────────────────────────
('no000016-0000-4000-8000-000000000016',
 'fa100001-0000-4001-8001-000000000001',  -- recipient: Clara (reported in COMP-010)
 'COMPLAINT_REOPENED',
 'A resolved complaint has been reopened',
 'Complaint NX-2025-0010 against your account has been reopened by the complainant. A support agent will review the case and contact you within 24 hours.',
 'cc000010-0000-4000-8000-000000000010', 'COMPLAINT',
 0, '2025-02-18 10:00:05', NULL),

-- ══════════════════════════════════════════════════════════════
--  MODULE ORGANISATIONS
-- ══════════════════════════════════════════════════════════════

-- ── US-N05 : Thomas reçoit une invitation à rejoindre CodeCraft Agency ─────────
('no000017-0000-4000-8000-000000000017',
 'fa100004-0000-4001-8001-000000000004',  -- recipient: Thomas
 'ORG_INVITATION_RECEIVED',
 'You have been invited to join CodeCraft Agency',
 'Clara Petit has invited you to join CodeCraft Agency as a Member. Click to view the invitation details and respond before March 20, 2025.',
 'oi000001-0000-4000-8000-000000000001', 'ORG_INVITATION',
 0, '2025-02-20 10:00:05', NULL),

-- ── US-N05 : Léa reçoit une invitation (refusée) à CodeCraft Agency ───────────
('no000018-0000-4000-8000-000000000018',
 'fa100005-0000-4001-8001-000000000005',  -- recipient: Léa
 'ORG_INVITATION_RECEIVED',
 'You have been invited to join CodeCraft Agency',
 'Clara Petit has invited you to join CodeCraft Agency as a Member. Click to view and respond.',
 'oi000002-0000-4000-8000-000000000002', 'ORG_INVITATION',
 1, '2025-01-25 09:00:05', '2025-01-26 10:00:00'),

-- ── US-N06 : Clara est notifiée que Léa a refusé l'invitation ─────────────────
('no000019-0000-4000-8000-000000000019',
 'fa100001-0000-4001-8001-000000000001',  -- recipient: Clara (org owner)
 'ORG_INVITATION_DECLINED',
 'An invitation was declined',
 'Léa Rousseau has declined your invitation to join CodeCraft Agency. You may send a new invitation after 30 days.',
 'oi000002-0000-4000-8000-000000000002', 'ORG_INVITATION',
 1, '2025-02-01 14:00:05', '2025-02-01 15:00:00'),

-- ── US-N07 : Thomas est notifié que Léa a candidaté à Open Devs Collective ─────
('no000020-0000-4000-8000-000000000020',
 'fa100004-0000-4001-8001-000000000004',  -- recipient: Thomas (org owner)
 'ORG_APPLICATION_SUBMITTED',
 'New membership application received',
 'Léa Rousseau has submitted an application to join Open Devs Collective. Please review the application and respond in the organisation portal.',
 'oa000001-0000-4000-8000-000000000001', 'ORG_APPLICATION',
 0, '2025-02-05 10:00:05', NULL),

-- ── US-N08 : Bob est notifié de sa candidature acceptée à CodeCraft ───────────
('no000021-0000-4000-8000-000000000021',
 '6d1d18e1-813a-4dc0-a89f-6ace6f2a8a61',  -- recipient: Bob
 'ORG_APPLICATION_SUBMITTED',
 'Your collaboration application has been accepted',
 'Congratulations! CodeCraft Agency has accepted your application for the DevOps mission. Please check your messages for onboarding instructions.',
 'ca000002-0000-4000-8000-000000000002', 'COLLAB_APPLICATION',
 1, '2025-01-08 15:00:05', '2025-01-08 16:00:00'),

-- ══════════════════════════════════════════════════════════════
--  NOTIFICATIONS SYSTÈME
-- ══════════════════════════════════════════════════════════════

-- ── US-N12 : Annonce système — maintenance planifiée (tous destinataires) ──────
-- Ici on envoie à 3 utilisateurs pour simuler une annonce broadcast
('no000022-0000-4000-8000-000000000022',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973',  -- recipient: Alice
 'SYSTEM_ANNOUNCEMENT',
 'Scheduled maintenance — Saturday March 1, 2025',
 'NexLance will undergo scheduled maintenance on Saturday March 1, 2025 from 02:00 to 06:00 CET. The platform will be unavailable during this window. Please plan accordingly.',
 NULL, NULL,
 1, '2025-02-24 09:00:00', '2025-02-24 09:05:00');
