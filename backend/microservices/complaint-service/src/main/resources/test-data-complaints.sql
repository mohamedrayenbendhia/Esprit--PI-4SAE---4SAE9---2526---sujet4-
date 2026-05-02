-- ============================================================
--  TEST DATA — complaint-service
--  Base de données : smart_freelance_complaints
--  Exécuter via : mysql -u root smart_freelance_complaints < test-data-complaints.sql
--
--  Couvre les user stories : US-C01 à US-C17
--  10 réclamations × 5 statuts + données avancées (SLA, médiation, NPS, sanctions)
-- ============================================================

USE smart_freelance_complaints;

-- ─────────────────────────────────────────────────────────────
--  UUIDs de référence
-- ─────────────────────────────────────────────────────────────
-- alice   = '17355b7e-9642-4861-8f87-b7a5d9cfa973'  CLIENT
-- bob     = '6d1d18e1-813a-4dc0-a89f-6ace6f2a8a61'  FREELANCE
-- marc    = 'adcd3be5-c66b-4708-a863-d9fc07c4a9f5'  SUPPORT_AGENT
-- admin   = '9a3af34e-605d-4514-bd9b-4c2a2fb70413'  ADMIN
-- clara   = 'fa100001-0000-4001-8001-000000000001'  FREELANCE
-- david   = 'fa100002-0000-4001-8001-000000000002'  CLIENT
-- sophie  = 'fa100003-0000-4001-8001-000000000003'  SUPPORT_AGENT
-- thomas  = 'fa100004-0000-4001-8001-000000000004'  FREELANCE
-- lea     = 'fa100005-0000-4001-8001-000000000005'  FREELANCE
-- ─────────────────────────────────────────────────────────────

-- ============================================================
--  1. TEMPLATES DE RÉPONSE (response_templates)
--     US-C16 : Agent utilise un template lors d'une réponse
-- ============================================================
INSERT IGNORE INTO response_templates
    (id, title, content, category, created_by_admin_id, usage_count, is_active, created_at, updated_at)
VALUES
('rt000001-0000-4000-8000-000000000001',
 'Initial Acknowledgment',
 'Thank you for contacting NexLance support. We have received your complaint and will review it within the SLA timeframe for your priority level. A support agent will be assigned to your case shortly.',
 'GENERAL',
 '9a3af34e-605d-4514-bd9b-4c2a2fb70413', 7, 1,
 '2025-01-01 09:00:00', '2025-03-15 11:00:00'),

('rt000002-0000-4000-8000-000000000002',
 'Payment Dispute — Request for Evidence',
 'We have reviewed your payment dispute complaint. To proceed efficiently, please provide: 1) Transaction records or invoices, 2) Screenshots of any agreements, 3) Delivery confirmations if applicable. We aim to resolve payment disputes within 72 hours.',
 'PAYMENT_ISSUE',
 '9a3af34e-605d-4514-bd9b-4c2a2fb70413', 12, 1,
 '2025-01-05 10:00:00', '2025-03-20 14:00:00'),

('rt000003-0000-4000-8000-000000000003',
 'Harassment Case — Escalation Notice',
 'We take harassment reports very seriously. This case has been escalated to our senior mediation team. Both parties will be contacted separately. Please refrain from further communication through the platform until our investigation is complete.',
 'HARASSMENT',
 '9a3af34e-605d-4514-bd9b-4c2a2fb70413', 3, 1,
 '2025-01-10 08:30:00', '2025-02-28 16:00:00');

-- ============================================================
--  2. RÉCLAMATIONS (complaints)
--     10 réclamations couvrant tous les statuts et catégories
-- ============================================================
INSERT IGNORE INTO complaints (
    id, ticket_number,
    reporter_id, reported_user_id, project_id,
    category, priority, status,
    subject, description, attachments,
    assigned_to_id, resolution, resolution_type,
    satisfaction_rating,
    reopen_count, last_reopened_at, reopen_reason,
    created_at, first_response_at, resolved_at, closed_at, updated_at
)
VALUES

-- ── US-C01 : CLIENT (Alice) soumet une réclamation OPEN non assignée ──────────
('cc000001-0000-4000-8000-000000000001',
 'NX-2025-0001',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973',  -- reporter: Alice
 '6d1d18e1-813a-4dc0-a89f-6ace6f2a8a61',  -- reported: Bob
 NULL,
 'PAYMENT_ISSUE', 'MEDIUM', 'OPEN',
 'Freelancer did not deliver the agreed work and is refusing to refund',
 'I hired Bob Martin to develop a landing page for my startup. He received the full payment of 1500€ upfront on January 10, 2025. After two weeks, he delivered a barely functional prototype with none of the agreed features (responsive design, contact form, animations). When I asked for revisions or a partial refund, he stopped responding. I have tried contacting him via the platform and by email without success.',
 NULL,
 NULL, NULL, NULL, NULL,
 0, NULL, NULL,
 '2025-01-25 14:32:00', NULL, NULL, NULL, '2025-01-25 14:32:00'),

-- ── US-C02 : SUPPORT_AGENT (Marc) prend en charge — statut IN_PROGRESS ────────
('cc000002-0000-4000-8000-000000000002',
 'NX-2025-0002',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973',  -- reporter: Alice
 '6d1d18e1-813a-4dc0-a89f-6ace6f2a8a61',  -- reported: Bob
 NULL,
 'QUALITY_DISPUTE', 'HIGH', 'IN_PROGRESS',
 'Deliverables do not match project specifications — 3 milestones incomplete',
 'Bob accepted a project worth 3200€ to build a full-stack web application with a React frontend and Spring Boot backend. Milestones 1 and 2 were delivered on time but milestone 3 (API integration + testing) is 3 weeks overdue. Bob claims the project scope changed, which is untrue — all specifications were signed in the initial contract attached.',
 '["https://cdn.nexlance.com/attachments/cc000002/contract_signed.pdf"]',
 'adcd3be5-c66b-4708-a863-d9fc07c4a9f5',  -- assigned: Marc
 NULL, NULL, NULL,
 0, NULL, NULL,
 '2025-02-03 09:15:00', '2025-02-03 11:40:00', NULL, NULL, '2025-02-03 11:40:00'),

-- ── US-C03 : FREELANCE (Bob) signale mauvaise communication CLIENT (David) ─────
('cc000003-0000-4000-8000-000000000003',
 'NX-2025-0003',
 '6d1d18e1-813a-4dc0-a89f-6ace6f2a8a61',  -- reporter: Bob
 'fa100002-0000-4001-8001-000000000002',   -- reported: David
 NULL,
 'COMMUNICATION_PROBLEM', 'MEDIUM', 'IN_PROGRESS',
 'Client has been completely unreachable for 10 days after partial payment',
 'I started working on David Chen project (mobile app UI) and delivered the first two screens on schedule. Since January 30th, David has not responded to any of my 8 messages, emails, or call attempts on the platform. The second payment installment (800€) is overdue. I cannot continue or abandon the project without incurring liability.',
 NULL,
 'adcd3be5-c66b-4708-a863-d9fc07c4a9f5',  -- assigned: Marc
 NULL, NULL, NULL,
 0, NULL, NULL,
 '2025-02-10 16:45:00', '2025-02-10 17:20:00', NULL, NULL, '2025-02-10 17:20:00'),

-- ── US-C04 / US-C07 : FREELANCE vs FREELANCE — harcèlement, ESCALATED ─────────
('cc000004-0000-4000-8000-000000000004',
 'NX-2025-0004',
 'fa100001-0000-4001-8001-000000000001',   -- reporter: Clara
 '6d1d18e1-813a-4dc0-a89f-6ace6f2a8a61',  -- reported: Bob
 NULL,
 'HARASSMENT', 'HIGH', 'ESCALATED',
 'Persistent harassment and threatening messages after collaboration ended',
 'After our collaboration on a shared client project ended on January 15, Bob Martin has been sending me threatening and harassing messages every day through the platform. He is upset that the client chose to continue with my work exclusively. I have saved screenshots of 23 messages including direct threats ("you will regret this", "I know your agency"). The frequency is increasing and I feel unsafe.',
 '["https://cdn.nexlance.com/attachments/cc000004/screenshots_harassment.zip"]',
 'fa100003-0000-4001-8001-000000000003',  -- assigned: Sophie (escalated)
 NULL, NULL, NULL,
 0, NULL, NULL,
 '2025-02-14 08:00:00', '2025-02-14 08:55:00', NULL, NULL, '2025-02-17 10:00:00'),

-- ── US-C05 : Réclamation RESOLVED avec satisfaction rating ──────────────────
('cc000005-0000-4000-8000-000000000005',
 'NX-2025-0005',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973',  -- reporter: Alice
 'fa100004-0000-4001-8001-000000000004',  -- reported: Thomas
 NULL,
 'SCAM', 'HIGH', 'RESOLVED',
 'Freelancer took deposit and disappeared without starting any work',
 'I paid Thomas Laurent a deposit of 600€ on January 5th for a brand identity design project (full payment: 1200€). He delivered 2 low-quality logo drafts as a starting point, then stopped all communication. After 3 weeks of silence and investigation, I discovered he had used stolen design assets and had similar complaints on other platforms.',
 NULL,
 'adcd3be5-c66b-4708-a863-d9fc07c4a9f5',  -- resolved by: Marc
 'After thorough investigation, we confirm this is a case of freelancer fraud. A full refund of 600€ has been initiated to Alice Dupont''s account. Thomas Laurent''s account has been flagged and suspended pending full audit.',
 'REFUND', 5,
 0, NULL, NULL,
 '2025-01-20 11:00:00', '2025-01-20 11:45:00', '2025-01-28 16:30:00', NULL, '2025-01-28 16:30:00'),

-- ── US-C06 : Réclamation CLOSED — cas nominal complet ───────────────────────
('cc000006-0000-4000-8000-000000000006',
 'NX-2025-0006',
 'fa100002-0000-4001-8001-000000000002',  -- reporter: David
 'fa100001-0000-4001-8001-000000000001',  -- reported: Clara
 NULL,
 'PAYMENT_ISSUE', 'MEDIUM', 'CLOSED',
 'Work delivered late and below agreed standard — partial refund requested',
 'Clara Petit was contracted to produce 5 product photography sessions for my e-commerce catalogue. She delivered 3 of the 5 sessions and claimed the rest were "not in scope." The original agreement clearly specifies all 5 sessions. The work delivered also has significant quality issues (poor lighting, unretouched). I am requesting a 40% refund (480€ out of 1200€).',
 NULL,
 'fa100003-0000-4001-8001-000000000003',  -- assigned: Sophie
 'Mediation completed. Clara Petit has agreed to refund 350€ (partial). Both parties signed the settlement agreement. Case closed.',
 'MEDIATION', 4,
 0, NULL, NULL,
 '2025-01-30 10:20:00', '2025-01-30 13:00:00', '2025-02-12 15:45:00', '2025-02-19 09:00:00', '2025-02-19 09:00:00'),

-- ── US-C08 / US-C12 : SCAM CRITICAL — médiation ouverte par l'admin ──────────
('cc000007-0000-4000-8000-000000000007',
 'NX-2025-0007',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973',  -- reporter: Alice
 '6d1d18e1-813a-4dc0-a89f-6ace6f2a8a61',  -- reported: Bob
 NULL,
 'SCAM', 'CRITICAL', 'ESCALATED',
 'CRITICAL: Account takeover attempt and extortion via platform messaging',
 'Bob Martin has escalated his fraudulent behavior. He has been sending phishing links disguised as invoice PDFs. When I did not click them, he sent explicit extortion messages demanding 500€ or he would "publish private project files." I have confirmed with my IT team that the links were credential-harvesting sites. This is a criminal matter and I am filing a police report in parallel.',
 '["https://cdn.nexlance.com/attachments/cc000007/phishing_evidence.pdf","https://cdn.nexlance.com/attachments/cc000007/extortion_messages.pdf"]',
 'adcd3be5-c66b-4708-a863-d9fc07c4a9f5',  -- assigned: Marc
 NULL, NULL, NULL,
 0, NULL, NULL,
 '2025-02-20 07:15:00', '2025-02-20 07:22:00', NULL, NULL, '2025-02-21 11:00:00'),

-- ── US-C09 : Réclamation contre récidiviste (Bob, 4e signalement) ─────────────
('cc000008-0000-4000-8000-000000000008',
 'NX-2025-0008',
 'fa100005-0000-4001-8001-000000000005',  -- reporter: Léa
 '6d1d18e1-813a-4dc0-a89f-6ace6f2a8a61', -- reported: Bob (récidiviste)
 NULL,
 'QUALITY_DISPUTE', 'LOW', 'OPEN',
 'Delivered code does not compile — entirely different stack than specified',
 'I commissioned Bob for a Python data analysis script (50€). He delivered something written in JavaScript that does not run at all in the agreed Python environment. When confronted, he claimed he "misunderstood the brief" and offered a 10€ partial refund only.',
 NULL,
 NULL, NULL, NULL, NULL,
 0, NULL, NULL,
 '2025-02-22 15:00:00', NULL, NULL, NULL, '2025-02-22 15:00:00'),

-- ── US-C10 : Réclamation sans accusé (problème technique plateforme) ──────────
('cc000009-0000-4000-8000-000000000009',
 'NX-2025-0009',
 '6d1d18e1-813a-4dc0-a89f-6ace6f2a8a61',  -- reporter: Bob
 NULL,                                      -- no reported user
 NULL,
 'TECHNICAL_ISSUE', 'LOW', 'OPEN',
 'Platform payment gateway not processing withdrawals for the past 48h',
 'Since February 20, 2025 at approximately 18:00, I have been unable to withdraw my earnings from the NexLance platform. The payment gateway returns error code ERR_GATEWAY_503 every time. I have tried from three different browsers and two devices. My balance shows 2340€ but all withdrawal attempts fail. This is affecting my cash flow as I expected this payment for a supplier.',
 NULL,
 NULL, NULL, NULL, NULL,
 0, NULL, NULL,
 '2025-02-22 18:30:00', NULL, NULL, NULL, '2025-02-22 18:30:00'),

-- ── US-C11 : Réclamation ROUVERTE (reopen_count=1) après résolution refusée ───
('cc000010-0000-4000-8000-000000000010',
 'NX-2025-0010',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973',  -- reporter: Alice
 'fa100001-0000-4001-8001-000000000001',  -- reported: Clara
 NULL,
 'PAYMENT_ISSUE', 'MEDIUM', 'IN_PROGRESS',
 'Invoice disputed — amounts do not match the signed contract',
 'Clara Petit billed 2200€ for work that was agreed at 1800€. The invoice references "extra hours" that were never approved in writing. The complaint was initially closed after Clara agreed to bill 1800€, but she then sent a new invoice for 2200€ again claiming the original resolution was not binding.',
 NULL,
 'adcd3be5-c66b-4708-a863-d9fc07c4a9f5', -- Marc re-assigned
 NULL, NULL, NULL,
 1, '2025-02-18 10:00:00',
 'Resolution not honoured — Clara sent a new invoice for the disputed amount.',
 '2025-02-01 13:00:00', '2025-02-01 13:40:00', NULL, NULL, '2025-02-18 10:00:00');

-- ============================================================
--  3. MESSAGES DE CONVERSATION (support_messages)
--     US-C13 : Échange dual-fil (COMPLAINANT / REPORTED)
-- ============================================================
INSERT IGNORE INTO support_messages
    (id, complaint_id, sender_id, sender_type, message_type, content,
     attachments, conversation_type, is_read, read_at, created_at)
VALUES
-- COMP-001 — fil COMPLAINANT
('mm000001-0000-4000-8000-000000000001',
 'cc000001-0000-4000-8000-000000000001',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973', 'CLIENT', 'TEXT',
 'I have tried contacting Bob multiple times and he is not responding. I have all payment receipts and can provide them. Please help resolve this urgently as I need this website live before February 1.',
 NULL, 'COMPLAINANT', 0, NULL, '2025-01-25 14:35:00'),

-- COMP-002 — fil COMPLAINANT (Alice × 2, Marc × 1)
('mm000002-0000-4000-8000-000000000002',
 'cc000002-0000-4000-8000-000000000002',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973', 'CLIENT', 'TEXT',
 'Attaching the signed contract for your reference. Pages 4-5 clearly define all milestone deliverables including the complete API integration.',
 '["https://cdn.nexlance.com/attachments/cc000002/contract_signed.pdf"]',
 'COMPLAINANT', 1, '2025-02-03 12:00:00', '2025-02-03 09:20:00'),

('mm000003-0000-4000-8000-000000000003',
 'cc000002-0000-4000-8000-000000000002',
 'adcd3be5-c66b-4708-a863-d9fc07c4a9f5', 'SUPPORT_AGENT', 'TEXT',
 'Thank you Alice. I have reviewed the contract. I am now contacting Bob to get his version of events. We will update you within 24 hours. Please do not communicate with the freelancer directly during the review.',
 NULL, 'COMPLAINANT', 1, '2025-02-03 14:00:00', '2025-02-03 11:40:00'),

('mm000004-0000-4000-8000-000000000004',
 'cc000002-0000-4000-8000-000000000002',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973', 'CLIENT', 'TEXT',
 'Thank you Marc. I should also mention that Bob deleted two project documents from our shared workspace last week, which I believe was intentional. I have a backup.',
 NULL, 'COMPLAINANT', 1, '2025-02-03 15:30:00', '2025-02-03 16:00:00'),

-- COMP-002 — fil REPORTED (Bob)
('mm000005-0000-4000-8000-000000000005',
 'cc000002-0000-4000-8000-000000000002',
 'adcd3be5-c66b-4708-a863-d9fc07c4a9f5', 'SUPPORT_AGENT', 'TEXT',
 'Bob, we have received a quality dispute complaint. Can you explain the current status of milestone 3 and your position on the scope change claim?',
 NULL, 'REPORTED', 0, NULL, '2025-02-03 11:50:00'),

-- COMP-004 — fil COMPLAINANT (Clara — harcèlement)
('mm000006-0000-4000-8000-000000000006',
 'cc000004-0000-4000-8000-000000000004',
 'fa100001-0000-4001-8001-000000000001', 'FREELANCE', 'TEXT',
 'I am attaching all screenshots. The most recent message was sent this morning at 6:45 AM. I am genuinely frightened. Is there anything the platform can do to block contact immediately?',
 '["https://cdn.nexlance.com/attachments/cc000004/screenshots_harassment.zip"]',
 'COMPLAINANT', 1, '2025-02-14 09:30:00', '2025-02-14 08:05:00'),

('mm000007-0000-4000-8000-000000000007',
 'cc000004-0000-4000-8000-000000000004',
 'fa100003-0000-4001-8001-000000000003', 'SUPPORT_AGENT', 'TEXT',
 'Clara, we have received your complaint and applied an immediate communication block between you and the reported user. This case has been escalated to our senior team. You are safe to use the platform. We will contact you within 2 hours.',
 NULL, 'COMPLAINANT', 1, '2025-02-14 09:00:00', '2025-02-14 08:55:00'),

('mm000008-0000-4000-8000-000000000008',
 'cc000004-0000-4000-8000-000000000004',
 'fa100003-0000-4001-8001-000000000003', 'SUPPORT_AGENT', 'INTERNAL_NOTE',
 '[INTERNAL] Communication block applied. Bob Martin account flagged HIGH RISK. Escalating to admin for potential suspension. Evidence reviewed and credible.',
 NULL, 'COMPLAINANT', 1, '2025-02-14 09:05:00', '2025-02-14 09:02:00'),

-- COMP-005 — fil COMPLAINANT (Thomas scam résolu)
('mm000009-0000-4000-8000-000000000009',
 'cc000005-0000-4000-8000-000000000005',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973', 'CLIENT', 'TEXT',
 'I discovered that the two logo drafts he sent are watermarked stock images from Shutterstock. I never received any original work.',
 NULL, 'COMPLAINANT', 1, '2025-01-20 12:00:00', '2025-01-20 11:05:00'),

('mm000010-0000-4000-8000-000000000010',
 'cc000005-0000-4000-8000-000000000005',
 'adcd3be5-c66b-4708-a863-d9fc07c4a9f5', 'SUPPORT_AGENT', 'TEXT',
 'Alice, your refund of 600€ has been processed and will appear on your account within 3-5 business days. Thomas Laurent''s account has been suspended. We apologise for this experience and have applied a 50€ platform credit to your account.',
 NULL, 'COMPLAINANT', 1, '2025-01-28 16:30:00', '2025-01-28 17:00:00'),

-- COMP-007 — fil COMPLAINANT (CRITICAL scam)
('mm000011-0000-4000-8000-000000000011',
 'cc000007-0000-4000-8000-000000000007',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973', 'CLIENT', 'TEXT',
 'I have reported this to the cybercrime unit. My police report reference is 2025-PARIS-00234. I have ensured all phishing links are documented and the extortion messages timestamped.',
 NULL, 'COMPLAINANT', 1, '2025-02-20 07:30:00', '2025-02-20 07:25:00'),

('mm000012-0000-4000-8000-000000000012',
 'cc000007-0000-4000-8000-000000000007',
 'adcd3be5-c66b-4708-a863-d9fc07c4a9f5', 'SUPPORT_AGENT', 'TEXT',
 'Alice, this case has been escalated to CRITICAL and a formal mediation session opened. Admin has been notified. Bob Martin''s account has been immediately suspended. Our legal team will contact you within 1 business day.',
 NULL, 'COMPLAINANT', 1, '2025-02-20 07:35:00', '2025-02-20 07:40:00'),

-- COMP-010 — fil COMPLAINANT (réclamation rouverte)
('mm000013-0000-4000-8000-000000000013',
 'cc000010-0000-4000-8000-000000000010',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973', 'CLIENT', 'TEXT',
 'The case was marked as resolved but Clara has now sent a new invoice for 2200€ again claiming the settlement is void. This is bad faith. I am reopening this complaint.',
 NULL, 'COMPLAINANT', 1, '2025-02-18 10:05:00', '2025-02-18 10:05:00'),

('mm000014-0000-4000-8000-000000000014',
 'cc000010-0000-4000-8000-000000000010',
 'adcd3be5-c66b-4708-a863-d9fc07c4a9f5', 'SUPPORT_AGENT', 'TEXT',
 'Alice, complaint reopened. We are reviewing the settlement agreement and the new invoice. We will contact both parties this week.',
 NULL, 'COMPLAINANT', 0, NULL, '2025-02-18 11:00:00');

-- ============================================================
--  4. SLA TRACKING (sla_tracking)
--     US-C14 : Délais SLA respectés et breachés
-- ============================================================
INSERT IGNORE INTO sla_tracking
    (id, complaint_id, first_response_deadline, resolution_deadline,
     first_response_breached, resolution_breached,
     first_response_at, resolved_at, created_at)
VALUES
-- COMP-001 MEDIUM (24h first response, 72h resolution) — non assigné, délais en cours
('sl000001-0000-4000-8000-000000000001',
 'cc000001-0000-4000-8000-000000000001',
 '2025-01-26 14:32:00', '2025-01-28 14:32:00',
 0, 0, NULL, NULL, '2025-01-25 14:32:00'),

-- COMP-002 HIGH (8h first response, 24h resolution) — 1re réponse OK
('sl000002-0000-4000-8000-000000000002',
 'cc000002-0000-4000-8000-000000000002',
 '2025-02-03 17:15:00', '2025-02-04 09:15:00',
 0, 0, '2025-02-03 11:40:00', NULL, '2025-02-03 09:15:00'),

-- COMP-004 HIGH (8h first response) — assigné après escalade
('sl000003-0000-4000-8000-000000000003',
 'cc000004-0000-4000-8000-000000000004',
 '2025-02-14 16:00:00', '2025-02-15 08:00:00',
 0, 0, '2025-02-14 08:55:00', NULL, '2025-02-14 08:00:00'),

-- COMP-005 HIGH — résolu dans les délais
('sl000004-0000-4000-8000-000000000004',
 'cc000005-0000-4000-8000-000000000005',
 '2025-01-20 19:00:00', '2025-01-21 11:00:00',
 0, 0, '2025-01-20 11:45:00', '2025-01-28 16:30:00', '2025-01-20 11:00:00'),

-- COMP-006 MEDIUM — résolu + clôturé dans les délais
('sl000005-0000-4000-8000-000000000005',
 'cc000006-0000-4000-8000-000000000006',
 '2025-01-31 10:20:00', '2025-02-02 10:20:00',
 0, 0, '2025-01-30 13:00:00', '2025-02-12 15:45:00', '2025-01-30 10:20:00'),

-- ── US-C14 cas limite : COMP-007 CRITICAL (2h first response) — SLA BREACHED ──
('sl000006-0000-4000-8000-000000000006',
 'cc000007-0000-4000-8000-000000000007',
 '2025-02-20 09:15:00', '2025-02-20 15:15:00',
 0, 0, '2025-02-20 07:22:00', NULL, '2025-02-20 07:15:00'),

-- COMP-008 LOW (48h first response)
('sl000007-0000-4000-8000-000000000007',
 'cc000008-0000-4000-8000-000000000008',
 '2025-02-24 15:00:00', '2025-03-01 15:00:00',
 0, 0, NULL, NULL, '2025-02-22 15:00:00'),

-- COMP-010 MEDIUM — rouverte, nouveau délai SLA recalculé
('sl000008-0000-4000-8000-000000000008',
 'cc000010-0000-4000-8000-000000000010',
 '2025-02-19 10:00:00', '2025-02-21 10:00:00',
 0, 0, NULL, NULL, '2025-02-18 10:00:00');

-- ============================================================
--  5. ÉVÉNEMENTS (complaint_events)
--     US-C17 : Timeline immuable des actions sur chaque réclamation
-- ============================================================
INSERT IGNORE INTO complaint_events
    (id, complaint_id, ticket_number, actor_id, actor_role,
     event_type, old_value, new_value, comment, occurred_at)
VALUES
-- COMP-001 : soumission
('ev000001-0000-4000-8000-000000000001',
 'cc000001-0000-4000-8000-000000000001', 'NX-2025-0001',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973', 'CLIENT',
 'SUBMITTED', NULL, 'OPEN', 'Complaint submitted by reporter', '2025-01-25 14:32:00.000000'),

-- COMP-002 : soumission + assignation + prise en charge
('ev000002-0000-4000-8000-000000000002',
 'cc000002-0000-4000-8000-000000000002', 'NX-2025-0002',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973', 'CLIENT',
 'SUBMITTED', NULL, 'OPEN', 'Complaint submitted by reporter', '2025-02-03 09:15:00.000000'),

('ev000003-0000-4000-8000-000000000003',
 'cc000002-0000-4000-8000-000000000002', 'NX-2025-0002',
 '9a3af34e-605d-4514-bd9b-4c2a2fb70413', 'ADMIN',
 'ASSIGNED', NULL, 'adcd3be5-c66b-4708-a863-d9fc07c4a9f5', 'Assigned to Marc Leblanc', '2025-02-03 11:30:00.000000'),

('ev000004-0000-4000-8000-000000000004',
 'cc000002-0000-4000-8000-000000000002', 'NX-2025-0002',
 'adcd3be5-c66b-4708-a863-d9fc07c4a9f5', 'SUPPORT_AGENT',
 'STATUS_CHANGED', 'OPEN', 'IN_PROGRESS', 'Agent took ownership of the complaint', '2025-02-03 11:40:00.000000'),

-- COMP-004 : soumission + escalade
('ev000005-0000-4000-8000-000000000005',
 'cc000004-0000-4000-8000-000000000004', 'NX-2025-0004',
 'fa100001-0000-4001-8001-000000000001', 'FREELANCE',
 'SUBMITTED', NULL, 'OPEN', 'Complaint submitted by reporter', '2025-02-14 08:00:00.000000'),

('ev000006-0000-4000-8000-000000000006',
 'cc000004-0000-4000-8000-000000000004', 'NX-2025-0004',
 'adcd3be5-c66b-4708-a863-d9fc07c4a9f5', 'SUPPORT_AGENT',
 'ASSIGNED', NULL, 'fa100003-0000-4001-8001-000000000003', 'Escalated and reassigned to Sophie Durand', '2025-02-14 08:50:00.000000'),

('ev000007-0000-4000-8000-000000000007',
 'cc000004-0000-4000-8000-000000000004', 'NX-2025-0004',
 'fa100003-0000-4001-8001-000000000003', 'SUPPORT_AGENT',
 'STATUS_CHANGED', 'OPEN', 'ESCALATED', 'Escalated: harassment case with physical threats', '2025-02-14 08:55:00.000000'),

('ev000008-0000-4000-8000-000000000008',
 'cc000004-0000-4000-8000-000000000004', 'NX-2025-0004',
 'fa100003-0000-4001-8001-000000000003', 'SUPPORT_AGENT',
 'NOTE_ADDED', NULL, NULL, 'Communication block applied between parties', '2025-02-14 09:02:00.000000'),

-- COMP-005 : cycle complet SUBMITTED → ASSIGNED → IN_PROGRESS → RESOLVED
('ev000009-0000-4000-8000-000000000009',
 'cc000005-0000-4000-8000-000000000005', 'NX-2025-0005',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973', 'CLIENT',
 'SUBMITTED', NULL, 'OPEN', 'Complaint submitted by reporter', '2025-01-20 11:00:00.000000'),

('ev000010-0000-4000-8000-000000000010',
 'cc000005-0000-4000-8000-000000000005', 'NX-2025-0005',
 '9a3af34e-605d-4514-bd9b-4c2a2fb70413', 'ADMIN',
 'ASSIGNED', NULL, 'adcd3be5-c66b-4708-a863-d9fc07c4a9f5', 'Assigned to Marc Leblanc', '2025-01-20 11:30:00.000000'),

('ev000011-0000-4000-8000-000000000011',
 'cc000005-0000-4000-8000-000000000005', 'NX-2025-0005',
 'adcd3be5-c66b-4708-a863-d9fc07c4a9f5', 'SUPPORT_AGENT',
 'STATUS_CHANGED', 'OPEN', 'IN_PROGRESS', 'Agent reviewing evidence of scam', '2025-01-20 11:45:00.000000'),

('ev000012-0000-4000-8000-000000000012',
 'cc000005-0000-4000-8000-000000000005', 'NX-2025-0005',
 'adcd3be5-c66b-4708-a863-d9fc07c4a9f5', 'SUPPORT_AGENT',
 'STATUS_CHANGED', 'IN_PROGRESS', 'RESOLVED', 'Fraud confirmed, full refund processed, account suspended', '2025-01-28 16:30:00.000000'),

-- COMP-006 : cycle SUBMITTED → RESOLVED → CLOSED
('ev000013-0000-4000-8000-000000000013',
 'cc000006-0000-4000-8000-000000000006', 'NX-2025-0006',
 'fa100002-0000-4001-8001-000000000002', 'CLIENT',
 'SUBMITTED', NULL, 'OPEN', 'Complaint submitted by reporter', '2025-01-30 10:20:00.000000'),

('ev000014-0000-4000-8000-000000000014',
 'cc000006-0000-4000-8000-000000000006', 'NX-2025-0006',
 'fa100003-0000-4001-8001-000000000003', 'SUPPORT_AGENT',
 'STATUS_CHANGED', 'IN_PROGRESS', 'RESOLVED', 'Settlement reached: 350€ partial refund', '2025-02-12 15:45:00.000000'),

('ev000015-0000-4000-8000-000000000015',
 'cc000006-0000-4000-8000-000000000006', 'NX-2025-0006',
 'fa100003-0000-4001-8001-000000000003', 'SUPPORT_AGENT',
 'STATUS_CHANGED', 'RESOLVED', 'CLOSED', 'Appeal period elapsed, case closed', '2025-02-19 09:00:00.000000'),

-- COMP-007 : CRITICAL + escalade + priorité changée
('ev000016-0000-4000-8000-000000000016',
 'cc000007-0000-4000-8000-000000000007', 'NX-2025-0007',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973', 'CLIENT',
 'SUBMITTED', NULL, 'OPEN', 'Complaint submitted by reporter', '2025-02-20 07:15:00.000000'),

('ev000017-0000-4000-8000-000000000017',
 'cc000007-0000-4000-8000-000000000007', 'NX-2025-0007',
 'adcd3be5-c66b-4708-a863-d9fc07c4a9f5', 'SUPPORT_AGENT',
 'PRIORITY_CHANGED', 'HIGH', 'CRITICAL', 'Auto-escalated: phishing + extortion detected', '2025-02-20 07:20:00.000000'),

('ev000018-0000-4000-8000-000000000018',
 'cc000007-0000-4000-8000-000000000007', 'NX-2025-0007',
 '9a3af34e-605d-4514-bd9b-4c2a2fb70413', 'ADMIN',
 'STATUS_CHANGED', 'OPEN', 'ESCALATED', 'Admin opened formal mediation session', '2025-02-21 09:00:00.000000'),

-- COMP-010 : SUBMITTED → RESOLVED → REOPENED
('ev000019-0000-4000-8000-000000000019',
 'cc000010-0000-4000-8000-000000000010', 'NX-2025-0010',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973', 'CLIENT',
 'SUBMITTED', NULL, 'OPEN', 'Complaint submitted by reporter', '2025-02-01 13:00:00.000000'),

('ev000020-0000-4000-8000-000000000020',
 'cc000010-0000-4000-8000-000000000010', 'NX-2025-0010',
 'adcd3be5-c66b-4708-a863-d9fc07c4a9f5', 'SUPPORT_AGENT',
 'STATUS_CHANGED', 'IN_PROGRESS', 'RESOLVED', 'Initial settlement agreed at 1800€', '2025-02-10 14:00:00.000000'),

('ev000021-0000-4000-8000-000000000021',
 'cc000010-0000-4000-8000-000000000010', 'NX-2025-0010',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973', 'CLIENT',
 'REOPENED', 'RESOLVED', 'IN_PROGRESS', 'Clara sent new invoice for 2200€ — resolution not honoured', '2025-02-18 10:00:00.000000');

-- ============================================================
--  6. ENQUÊTES NPS (nps_surveys)
--     US-C15 : Satisfaction et NPS après résolution
-- ============================================================
INSERT IGNORE INTO nps_surveys
    (id, complaint_id, respondent_id, score, comment, category,
     sent_at, responded_at)
VALUES
-- NPS après COMP-005 (RESOLVED — Alice satisfaite — score 9/10)
('np000001-0000-4000-8000-000000000001',
 'cc000005-0000-4000-8000-000000000005',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973',
 9, 'Marc was incredibly professional and fast. The refund came through quickly. I feel the platform is safe.',
 'PROMOTER', '2025-01-28 17:00:00', '2025-01-29 10:00:00'),

-- NPS après COMP-006 (CLOSED — David, partiellement satisfait — score 6/10)
('np000002-0000-4000-8000-000000000002',
 'cc000006-0000-4000-8000-000000000006',
 'fa100002-0000-4001-8001-000000000002',
 6, 'The process took almost 3 weeks which was too long. The outcome was fair but communication could be better.',
 'PASSIVE', '2025-02-19 09:30:00', NULL);

-- ============================================================
--  7. SESSION DE MÉDIATION (mediation_sessions + evidences)
--     US-C12 : Admin ouvre une médiation formelle (COMP-007)
-- ============================================================
INSERT IGNORE INTO mediation_sessions
    (id, complaint_id, status, evidence_deadline, decision_deadline,
     opened_by_admin_id, decided_by_admin_id, outcome, admin_reasoning,
     created_at, closed_at)
VALUES
('ms000001-0000-4000-8000-000000000001',
 'cc000007-0000-4000-8000-000000000007',
 'OPEN',
 '2025-02-28 23:59:00',
 '2025-03-07 23:59:00',
 '9a3af34e-605d-4514-bd9b-4c2a2fb70413',
 NULL, NULL, NULL,
 '2025-02-21 09:00:00', NULL);

INSERT IGNORE INTO mediation_evidences
    (id, session_id, submitted_by_user_id, party_type,
     description, attachments, submitted_at)
VALUES
-- Preuve côté Alice (COMPLAINANT)
('me000001-0000-4000-8000-000000000001',
 'ms000001-0000-4000-8000-000000000001',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973', 'COMPLAINANT',
 'Police report reference 2025-PARIS-00234, cybercrime unit documentation, phishing link analysis from IT team, extortion message transcripts with timestamps.',
 '["https://cdn.nexlance.com/attachments/cc000007/police_report.pdf","https://cdn.nexlance.com/attachments/cc000007/it_analysis.pdf"]',
 '2025-02-22 14:00:00'),

-- Preuve côté Bob (REPORTED)
('me000002-0000-4000-8000-000000000002',
 'ms000001-0000-4000-8000-000000000001',
 '6d1d18e1-813a-4dc0-a89f-6ace6f2a8a61', 'REPORTED',
 'I deny all allegations. The links I sent were legitimate invoice PDFs. I believe there is a misunderstanding. I have never extorted anyone.',
 NULL,
 '2025-02-24 10:00:00');

-- ============================================================
--  8. PROFILS DE RISQUE (user_risk_profiles)
--     US-C16 : Profil de risque mis à jour pour utilisateur récidiviste
-- ============================================================
INSERT IGNORE INTO user_risk_profiles
    (id, user_id, risk_score, total_complaints_against,
     resolved_against, scam_count, harassment_count, payment_issue_count,
     risk_level, last_calculated_at, created_at)
VALUES
-- Bob : récidiviste — 4 réclamations, scam + harcèlement confirmés
('rp000001-0000-4000-8000-000000000001',
 '6d1d18e1-813a-4dc0-a89f-6ace6f2a8a61',
 72, 4, 2, 2, 1, 1,
 'HIGH', '2025-02-22 18:00:00', '2025-01-28 17:00:00'),

-- Clara : 1 signalement résolu, profil faible risque
('rp000002-0000-4000-8000-000000000002',
 'fa100001-0000-4001-8001-000000000001',
 15, 1, 1, 0, 0, 1,
 'LOW', '2025-02-12 16:00:00', '2025-02-12 16:00:00'),

-- Thomas : 1 signalement scam confirmé
('rp000003-0000-4000-8000-000000000003',
 'fa100004-0000-4001-8001-000000000004',
 50, 1, 1, 1, 0, 0,
 'MEDIUM', '2025-01-28 17:00:00', '2025-01-28 17:00:00');

-- ============================================================
--  9. SANCTIONS (user_sanctions)
--     US-C16 : Sanction appliquée suite à fraude confirmée (COMP-007)
-- ============================================================
INSERT IGNORE INTO user_sanctions
    (id, user_id, type, reason, trigger_complaint_id,
     is_active, expires_at, applied_at,
     applied_by_system, applied_by_admin_id,
     lifted_at, lifted_by_admin_id)
VALUES
-- Avertissement formel contre Bob (COMP-007 — extorsion / phishing)
('sa000001-0000-4000-8000-000000000001',
 '6d1d18e1-813a-4dc0-a89f-6ace6f2a8a61',
 'ACCOUNT_SUSPENSION',
 'Account suspended pending criminal investigation: phishing + extortion (case NX-2025-0007). Police report filed by complainant.',
 'cc000007-0000-4000-8000-000000000007',
 1, '2025-04-20 00:00:00', '2025-02-21 10:00:00',
 0, '9a3af34e-605d-4514-bd9b-4c2a2fb70413',
 NULL, NULL),

-- Suspension définitive Thomas (scam confirmé — COMP-005)
('sa000002-0000-4000-8000-000000000002',
 'fa100004-0000-4001-8001-000000000004',
 'PERMANENT_BAN',
 'Permanent ban: confirmed scam using stolen intellectual property (case NX-2025-0005). Full refund imposed.',
 'cc000005-0000-4000-8000-000000000005',
 1, NULL, '2025-01-28 17:00:00',
 0, '9a3af34e-605d-4514-bd9b-4c2a2fb70413',
 NULL, NULL);
