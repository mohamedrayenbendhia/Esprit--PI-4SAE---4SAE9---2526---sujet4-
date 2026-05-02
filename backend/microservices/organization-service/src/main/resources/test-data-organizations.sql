-- ============================================================
--  TEST DATA — organization-service
--  Base de données : smart_freelance_organizations
--  Exécuter via : mysql -u root smart_freelance_organizations < test-data-organizations.sql
--
--  Couvre les user stories : US-ORG-01 à US-ORG-15
--  5 organisations × statuts variés + membres, invitations, candidatures,
--  portfolios, avis, RFQ, collab offers/applications, audit logs
-- ============================================================

USE smart_freelance_organizations;

-- ─────────────────────────────────────────────────────────────
--  UUIDs de référence (identiques à ceux du user-service)
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

-- ============================================================
--  1. ORGANISATIONS
-- ============================================================
INSERT IGNORE INTO organizations (
    id, name, description, logo_url, website,
    type, specialties, location, siret, size,
    status, visibility, owner_id,
    average_rating, completed_projects_count, review_count, trust_level,
    badges, admin_note,
    created_at, updated_at, dissolved_at,
    latitude, longitude
)
VALUES

-- ── US-ORG-01 : organisation ACTIVE avec membres multiples et portfolio ───────
('org00001-0000-4000-8000-000000000001',
 'TechVision Studio',
 'A dynamic startup of freelance developers and designers specialising in web and mobile product development. We deliver end-to-end digital solutions from UX research to production deployment.',
 'https://cdn.nexlance.com/logos/techvision.png',
 'https://techvision-studio.fr',
 'STARTUP',
 '["React","TypeScript","Spring Boot","UI/UX Design","Mobile Development"]',
 'Paris, France', '12345678901234', 'SMALL',
 'ACTIVE', 'PUBLIC',
 '6d1d18e1-813a-4dc0-a89f-6ace6f2a8a61',  -- owner: Bob
 4.0, 8, 2, 3,
 '["VERIFIED","EXPERIENCED"]',
 NULL,
 '2024-10-01 09:00:00', '2025-02-15 10:00:00', NULL,
 48.8566, 2.3522),

-- ── US-ORG-02 : AGENCY ACTIVE, visibilité MEMBERS_ONLY, multi-membres ─────────
('org00002-0000-4000-8000-000000000002',
 'CodeCraft Agency',
 'Full-service digital agency specialising in custom software development for SMEs. Members are senior freelancers with 5+ years of industry experience.',
 NULL,
 'https://codecraft.agency',
 'AGENCY',
 '["Python","Django","Vue.js","DevOps","Cloud AWS"]',
 'Lyon, France', '98765432109876', 'MEDIUM',
 'ACTIVE', 'MEMBERS_ONLY',
 'fa100001-0000-4001-8001-000000000001',   -- owner: Clara
 3.8, 14, 0, 2,
 '["VERIFIED"]',
 NULL,
 '2024-11-15 14:00:00', '2025-01-20 09:00:00', NULL,
 45.7640, 4.8357),

-- ── US-ORG-03 : ASSOCIATION PENDING_VERIFICATION — en attente validation admin ─
('org00003-0000-4000-8000-000000000003',
 'Open Devs Collective',
 'An open-source developers association promoting collaboration on community projects, knowledge sharing, and mentorship for junior freelancers.',
 NULL, NULL,
 'ASSOCIATION',
 '["Open Source","PHP","Laravel","Community Management"]',
 'Toulouse, France', NULL, 'SMALL',
 'PENDING_VERIFICATION', 'PUBLIC',
 'fa100004-0000-4001-8001-000000000004',   -- owner: Thomas
 0.0, 0, 0, 1,
 '[]',
 'Pending verification: SIRET document requested on 2025-02-01. Awaiting response.',
 '2025-01-28 11:00:00', '2025-02-01 09:00:00', NULL,
 43.6047, 1.4442),

-- ── US-ORG-04 : FREELANCE_COOP SUSPENDED — violation des règles plateforme ─────
('org00004-0000-4000-8000-000000000004',
 'AlphaFreelance Coop',
 'A cooperative of freelance consultants focusing on enterprise digital transformation.',
 NULL, NULL,
 'FREELANCE_COOP',
 '["Consulting","Agile","SAP","Digital Transformation"]',
 'Bordeaux, France', NULL, 'SMALL',
 'SUSPENDED', 'PRIVATE',
 'fa100005-0000-4001-8001-000000000005',   -- owner: Léa
 2.1, 3, 0, 1,
 '[]',
 'SUSPENDED 2025-02-10: Multiple complaints of misleading client contracts. Under investigation by compliance team.',
 '2024-08-20 10:00:00', '2025-02-10 16:00:00', NULL,
 44.8378, -0.5792),

-- ── US-ORG-08 : DISSOLVED — données historiques ────────────────────────────────
('org00005-0000-4000-8000-000000000005',
 'Legacy Code Studio',
 'Former studio specialising in legacy system modernisation. Dissolved after the founding team dispersed.',
 NULL, NULL,
 'STARTUP',
 '["Java","Legacy Migration","COBOL","Mainframe"]',
 'Nantes, France', NULL, 'SOLO',
 'DISSOLVED', 'PUBLIC',
 '6d1d18e1-813a-4dc0-a89f-6ace6f2a8a61',  -- owner: Bob
 3.5, 5, 0, 2,
 '["EXPERIENCED"]',
 'Dissolved by owner request on 2025-01-15.',
 '2023-06-01 08:00:00', '2025-01-15 12:00:00', '2025-01-15 12:00:00',
 47.2184, -1.5536);

-- ============================================================
--  2. MEMBRES DES ORGANISATIONS (organization_members)
--     US-ORG-05 : plusieurs membres avec rôles distincts
-- ============================================================
INSERT IGNORE INTO organization_members
    (id, organization_id, user_id, role, status, display_on_profile, joined_at, left_at)
VALUES
-- ── TechVision Studio (org1) : Bob (OWNER) + Thomas (MANAGER) + Léa (MEMBER) ──
('om000001-0000-4000-8000-000000000001',
 'org00001-0000-4000-8000-000000000001',
 '6d1d18e1-813a-4dc0-a89f-6ace6f2a8a61',  -- Bob
 'OWNER', 'ACTIVE', 1, '2024-10-01 09:00:00', NULL),

('om000002-0000-4000-8000-000000000002',
 'org00001-0000-4000-8000-000000000001',
 'fa100004-0000-4001-8001-000000000004',  -- Thomas
 'MANAGER', 'ACTIVE', 1, '2024-10-15 10:00:00', NULL),

('om000003-0000-4000-8000-000000000003',
 'org00001-0000-4000-8000-000000000001',
 'fa100005-0000-4001-8001-000000000005',  -- Léa
 'MEMBER', 'ACTIVE', 1, '2024-11-01 11:00:00', NULL),

-- ── CodeCraft Agency (org2) : Clara (OWNER) + Bob (MANAGER) ──────────────────
-- US-ORG-06 : Bob est membre de deux organisations différentes (cas limite)
('om000004-0000-4000-8000-000000000004',
 'org00002-0000-4000-8000-000000000002',
 'fa100001-0000-4001-8001-000000000001',  -- Clara
 'OWNER', 'ACTIVE', 1, '2024-11-15 14:00:00', NULL),

('om000005-0000-4000-8000-000000000005',
 'org00002-0000-4000-8000-000000000002',
 '6d1d18e1-813a-4dc0-a89f-6ace6f2a8a61',  -- Bob (also in org1)
 'MANAGER', 'ACTIVE', 0, '2024-12-01 09:00:00', NULL),

-- ── Open Devs Collective (org3) : Thomas seul ─────────────────────────────────
('om000006-0000-4000-8000-000000000006',
 'org00003-0000-4000-8000-000000000003',
 'fa100004-0000-4001-8001-000000000004',  -- Thomas
 'OWNER', 'ACTIVE', 1, '2025-01-28 11:00:00', NULL),

-- ── AlphaFreelance Coop (org4) : Léa (OWNER, INACTIVE) + Bob (parti) ─────────
-- US-ORG-04 : membres d'une organisation suspendue
('om000007-0000-4000-8000-000000000007',
 'org00004-0000-4000-8000-000000000004',
 'fa100005-0000-4001-8001-000000000005',  -- Léa
 'OWNER', 'INACTIVE', 1, '2024-08-20 10:00:00', NULL),

('om000008-0000-4000-8000-000000000008',
 'org00004-0000-4000-8000-000000000004',
 '6d1d18e1-813a-4dc0-a89f-6ace6f2a8a61',  -- Bob (parti)
 'MEMBER', 'INACTIVE', 0, '2024-09-01 09:00:00', '2025-01-10 15:00:00'),

-- ── Legacy Code Studio (org5) : Bob (OWNER) + Thomas (MEMBER, parti) ──────────
('om000009-0000-4000-8000-000000000009',
 'org00005-0000-4000-8000-000000000005',
 '6d1d18e1-813a-4dc0-a89f-6ace6f2a8a61',  -- Bob
 'OWNER', 'INACTIVE', 0, '2023-06-01 08:00:00', '2025-01-15 12:00:00'),

('om000010-0000-4000-8000-000000000010',
 'org00005-0000-4000-8000-000000000005',
 'fa100004-0000-4001-8001-000000000004',  -- Thomas
 'MEMBER', 'INACTIVE', 0, '2024-01-10 09:00:00', '2025-01-15 12:00:00');

-- ============================================================
--  3. INVITATIONS (org_invitations)
--     US-ORG-10 : invitations dans différents états
-- ============================================================
INSERT IGNORE INTO org_invitations
    (id, organization_id, inviter_id, invitee_id, invitee_email,
     role, status, message, token, expires_at, created_at, responded_at)
VALUES
-- ── CodeCraft Agency invite Thomas (PENDING) — cas nominal ───────────────────
-- US-ORG-10 : invitation reçue mais pas encore traitée
('oi000001-0000-4000-8000-000000000001',
 'org00002-0000-4000-8000-000000000002',
 'fa100001-0000-4001-8001-000000000001',  -- inviter: Clara
 'fa100004-0000-4001-8001-000000000004',  -- invitee: Thomas
 'thomas.laurent@test.com',
 'MEMBER', 'PENDING',
 'Hi Thomas! We would love to have you join CodeCraft Agency. Your Laravel expertise would be a great fit for our current projects.',
 'tok-oi000001-secure-random-abc123', '2025-03-20 23:59:00',
 '2025-02-20 10:00:00', NULL),

-- ── CodeCraft Agency invite Léa (DECLINED) — invitation refusée ───────────────
-- US-ORG-11 : invitée refuse l'invitation
('oi000002-0000-4000-8000-000000000002',
 'org00002-0000-4000-8000-000000000002',
 'fa100001-0000-4001-8001-000000000001',  -- inviter: Clara
 'fa100005-0000-4001-8001-000000000005',  -- invitee: Léa
 'lea.rousseau@test.com',
 'MEMBER', 'DECLINED',
 'Hi Léa, would you like to join CodeCraft as a freelance member?',
 'tok-oi000002-secure-random-def456', '2025-02-10 23:59:00',
 '2025-01-25 09:00:00', '2025-02-01 14:00:00'),

-- ── TechVision invite par email externe (PENDING) — invitée non inscrite ───────
-- US-ORG-12 : invitation envoyée à une adresse email non encore enregistrée
('oi000003-0000-4000-8000-000000000003',
 'org00001-0000-4000-8000-000000000001',
 '6d1d18e1-813a-4dc0-a89f-6ace6f2a8a61',  -- inviter: Bob
 NULL,                                      -- invitee unknown yet
 'julien.dupuis@externaldomain.com',
 'MEMBER', 'PENDING',
 'Hi Julien, I would like to invite you to join TechVision Studio as a member.',
 'tok-oi000003-secure-random-ghi789', '2025-03-15 23:59:00',
 '2025-02-15 11:00:00', NULL),

-- ── TechVision invite — EXPIRED ───────────────────────────────────────────────
-- US-ORG-12 : invitation expirée (deadline passée sans réponse)
('oi000004-0000-4000-8000-000000000004',
 'org00001-0000-4000-8000-000000000001',
 '6d1d18e1-813a-4dc0-a89f-6ace6f2a8a61',  -- inviter: Bob
 'fa100002-0000-4001-8001-000000000002',   -- invitee: David (CLIENT, invitation refusée car pas freelance)
 'david.chen@test.com',
 'MEMBER', 'EXPIRED',
 'David, would you like to join TechVision Studio as an associate?',
 'tok-oi000004-secure-random-jkl012', '2025-01-31 23:59:00',
 '2025-01-20 08:00:00', NULL);

-- ============================================================
--  4. CANDIDATURES À L'ORGANISATION (org_applications)
--     US-ORG-07 : freelance candidate pour rejoindre une organisation
-- ============================================================
INSERT IGNORE INTO org_applications
    (id, organization_id, applicant_id, message, cv_url,
     status, rejection_reason, created_at, responded_at)
VALUES
-- ── Léa candidate à Open Devs Collective (PENDING) ───────────────────────────
-- US-ORG-07 : candidature en attente de réponse du propriétaire
('oa000001-0000-4000-8000-000000000001',
 'org00003-0000-4000-8000-000000000003',
 'fa100005-0000-4001-8001-000000000005',  -- applicant: Léa
 'I am passionate about open-source development and have contributed to 12 public GitHub repositories. I believe I can add value to your collective with my expertise in PHP and community management.',
 'https://cdn.nexlance.com/cv/lea_rousseau_cv.pdf',
 'PENDING', NULL, '2025-02-05 10:00:00', NULL),

-- ── Bob candidate à TechVision puis accepté (ACCEPTED — déjà membre via invitation)
-- US-ORG-13 : candidature acceptée (cas limite — Bob est déjà OWNER, ne s'applique pas
--             ici, donc on utilise un autre scénario : Léa candidate à org4 puis rejetée)
('oa000002-0000-4000-8000-000000000002',
 'org00004-0000-4000-8000-000000000004',
 'fa100005-0000-4001-8001-000000000005',  -- applicant: Léa (owns org4 — test data)
 NULL,
 NULL,
 'REJECTED',
 'Application rejected: organisation currently suspended. No new members accepted during suspension period.',
 '2024-08-19 14:00:00', '2024-08-20 10:00:00');

-- ============================================================
--  5. PORTFOLIO (org_portfolio_items)
--     US-ORG-09 : vitrine des réalisations d'une organisation
-- ============================================================
INSERT IGNORE INTO org_portfolio_items
    (id, organization_id, title, description, image_url, project_url,
     client_name, tags, completed_at, created_at)
VALUES
-- TechVision Studio — 2 réalisations
('pf000001-0000-4000-8000-000000000001',
 'org00001-0000-4000-8000-000000000001',
 'E-Commerce Platform — FashionHub',
 'Full-stack e-commerce solution built with React + Spring Boot + PostgreSQL. Features include real-time inventory management, Stripe payment integration, and a PWA mobile experience. Handled 10k concurrent users on launch day.',
 'https://cdn.nexlance.com/portfolio/techvision/fashionhub.jpg',
 'https://fashionhub.fr',
 'FashionHub SAS',
 '["React","Spring Boot","PostgreSQL","Stripe","PWA"]',
 '2024-09-30', '2024-10-15 09:00:00'),

('pf000002-0000-4000-8000-000000000002',
 'org00001-0000-4000-8000-000000000001',
 'HR Management SaaS — PeopleFirst',
 'Custom SaaS platform for mid-size companies with employee onboarding, payroll integration, and analytics dashboard. 3-month delivery for a 6-month estimated project.',
 NULL,
 'https://peoplefirst-app.com',
 'PeopleFirst Technologies',
 '["TypeScript","NestJS","React","Docker","CI/CD"]',
 '2024-12-15', '2025-01-10 10:00:00'),

-- CodeCraft Agency — 1 réalisation
('pf000003-0000-4000-8000-000000000003',
 'org00002-0000-4000-8000-000000000002',
 'Supply Chain Dashboard — LogiTrack',
 'Real-time logistics tracking dashboard with predictive analytics, built with Python/Django REST API and Vue.js frontend. Deployed on AWS with auto-scaling.',
 'https://cdn.nexlance.com/portfolio/codecraft/logitrack.jpg',
 NULL,
 'LogiTrack Industries',
 '["Python","Django","Vue.js","AWS","Redis"]',
 '2025-01-20', '2025-01-25 14:00:00');

-- ============================================================
--  6. AVIS CLIENTS (organization_reviews)
--     US-ORG-14 : notation + réponse + avis signalé
-- ============================================================
INSERT IGNORE INTO organization_reviews
    (id, organization_id, reviewer_id, project_id,
     rating, comment, reply, reply_at,
     reported, created_at, updated_at)
VALUES
-- Alice note TechVision Studio 5/5 ✓ avis positif
-- US-ORG-14 : CLIENT laisse un avis positif avec réponse de l'organisation
('rv000001-0000-4000-8000-000000000001',
 'org00001-0000-4000-8000-000000000001',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973',  -- reviewer: Alice
 NULL,
 5,
 'TechVision Studio delivered our platform 2 weeks ahead of schedule. The code quality is excellent, the team communicates proactively, and they genuinely cared about our product vision. Highly recommended for complex projects.',
 'Thank you Alice! It was a pleasure working with FashionHub. We look forward to the next project together.',
 '2025-02-16 10:00:00',
 0, '2025-02-15 09:00:00', '2025-02-16 10:00:00'),

-- David note TechVision Studio 2/5 — avis négatif signalé (reported)
-- US-ORG-14 cas limite : avis signalé comme abusif
('rv000002-0000-4000-8000-000000000002',
 'org00001-0000-4000-8000-000000000001',
 'fa100002-0000-4001-8001-000000000002',  -- reviewer: David
 NULL,
 2,
 'Terrible experience. They ghosted me mid-project. Would never work with them again. Scammers.',
 NULL, NULL,
 1,  -- reported as abusive (David has a pending complaint — bias suspected)
 '2025-02-20 08:00:00', '2025-02-20 08:00:00'),

-- Alice note CodeCraft Agency 4/5
('rv000003-0000-4000-8000-000000000003',
 'org00002-0000-4000-8000-000000000002',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973',  -- reviewer: Alice
 NULL,
 4,
 'CodeCraft delivered a solid backend API. Slight delays on sprint 3 but the end result was production-ready. Good communication throughout.',
 'Thanks Alice! We''re glad the project landed well. The sprint 3 delay was due to a scope change on our end — we appreciate your understanding.',
 '2025-01-22 09:00:00',
 0, '2025-01-21 15:00:00', '2025-01-22 09:00:00');

-- ============================================================
--  7. APPELS D'OFFRES — RFQ (org_rfq)
--     US-ORG-15 : CLIENT envoie un RFQ, organisation répond
-- ============================================================
INSERT IGNORE INTO org_rfq
    (id, organization_id, requester_id, title, description,
     budget_min, budget_max, deadline, skills_needed,
     status, response_message, responded_by_id,
     created_at, responded_at)
VALUES
-- Alice envoie un RFQ à TechVision Studio (PENDING — pas encore répondu)
('rq000001-0000-4000-8000-000000000001',
 'org00001-0000-4000-8000-000000000001',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973',  -- requester: Alice
 'Looking for a team to build a B2B SaaS platform',
 'We need a complete B2B SaaS platform: multi-tenant architecture, subscription billing, admin dashboard, REST API, and React frontend. The platform will serve 50+ enterprise clients. Estimated 6-month project.',
 15000.00, 25000.00, '2025-05-31',
 '["React","Spring Boot","Multi-tenancy","Stripe","Kubernetes"]',
 'PENDING', NULL, NULL,
 '2025-02-18 10:00:00', NULL),

-- David envoie un RFQ à CodeCraft Agency (RESPONDED)
-- US-ORG-15 : RFQ avec réponse de l'organisation
('rq000002-0000-4000-8000-000000000002',
 'org00002-0000-4000-8000-000000000002',
 'fa100002-0000-4001-8001-000000000002',  -- requester: David
 'Data analytics pipeline for e-commerce reporting',
 'We need a real-time data pipeline ingesting Shopify events, transforming them via Python, and displaying insights on a Vue.js dashboard. Approximately 3 months of work.',
 8000.00, 12000.00, '2025-04-15',
 '["Python","Apache Kafka","Vue.js","PostgreSQL","AWS"]',
 'RESPONDED',
 'We can deliver this project in 10 weeks. Our Django + Vue.js stack is a perfect fit. We propose starting with a 2-week discovery sprint. Our quote is 9500€ all-inclusive.',
 'fa100001-0000-4001-8001-000000000001',  -- responded by: Clara
 '2025-01-15 14:00:00', '2025-01-18 11:00:00'),

-- Alice envoie un RFQ à Open Devs (CLOSED — org pas encore vérifiée)
-- US-ORG-15 cas limite : RFQ fermé car organisation non vérifiable
('rq000003-0000-4000-8000-000000000003',
 'org00003-0000-4000-8000-000000000003',
 '17355b7e-9642-4861-8f87-b7a5d9cfa973',  -- requester: Alice
 'Open-source community portal — Laravel + PHP',
 'Small community portal for an association. Laravel preferred. Budget modest, open to mentorship exchange.',
 1500.00, 3000.00, '2025-03-30',
 '["PHP","Laravel","MySQL","Community Portal"]',
 'CLOSED', NULL, NULL,
 '2025-02-10 09:00:00', NULL);

-- ============================================================
--  8. OFFRES DE COLLABORATION (collab_offers)
--     US-ORG-13 : organisation publie une offre de collaboration ponctuelle
-- ============================================================
INSERT IGNORE INTO collab_offers
    (id, organization_id, created_by, title, description,
     required_skills, duration_label, budget_estimate, max_applicants,
     deadline_date, status, created_at, updated_at)
VALUES
-- TechVision Studio publie une offre (OPEN) — cherche un freelance externe
('co000001-0000-4000-8000-000000000001',
 'org00001-0000-4000-8000-000000000001',
 'fa100004-0000-4001-8001-000000000004',  -- created_by: Thomas (MANAGER)
 'React Developer — 3-month Mission for HealthTech Client',
 'TechVision Studio is looking for an experienced React developer to join our team on a client mission for a HealthTech startup. You will be responsible for building a patient portal with real-time notifications and integration with external medical APIs.',
 '["React","TypeScript","REST API","Jest","Accessibility"]',
 '3 months (renewable)', 6000.00, 3,
 '2025-03-15', 'OPEN',
 '2025-02-15 09:00:00', '2025-02-15 09:00:00'),

-- CodeCraft Agency publie une offre (CLOSED — quota atteint)
('co000002-0000-4000-8000-000000000002',
 'org00002-0000-4000-8000-000000000002',
 'fa100001-0000-4001-8001-000000000001',  -- created_by: Clara (OWNER)
 'DevOps Engineer — AWS Infrastructure Deployment',
 'We need a senior DevOps engineer to help us migrate a client infrastructure to AWS (ECS + RDS + CloudFront). 6-week mission, remote.',
 '["AWS","Docker","Terraform","CI/CD","Linux"]',
 '6 weeks', 4800.00, 1,
 '2025-01-31', 'CLOSED',
 '2025-01-05 10:00:00', '2025-01-20 15:00:00');

-- ============================================================
--  9. CANDIDATURES COLLAB (collab_applications)
--     US-ORG-13 : freelances candidatent à une offre de collaboration
-- ============================================================
INSERT IGNORE INTO collab_applications
    (id, offer_id, organization_id, applicant_id,
     message, portfolio_url, status, rejection_reason,
     created_at, responded_at)
VALUES
-- Léa candidate à l'offre React de TechVision Studio (PENDING)
('ca000001-0000-4000-8000-000000000001',
 'co000001-0000-4000-8000-000000000001',
 'org00001-0000-4000-8000-000000000001',
 'fa100005-0000-4001-8001-000000000005',  -- applicant: Léa
 'I have 4 years of React experience including healthcare projects with strict accessibility requirements (WCAG 2.1 AA). I am available from March 1st and would love to discuss the mission further.',
 'https://lea-rousseau.dev/portfolio',
 'PENDING', NULL,
 '2025-02-17 10:00:00', NULL),

-- Bob candidate à l'offre DevOps CodeCraft (ACCEPTED)
-- US-ORG-13 : candidature acceptée
('ca000002-0000-4000-8000-000000000002',
 'co000002-0000-4000-8000-000000000002',
 'org00002-0000-4000-8000-000000000002',
 '6d1d18e1-813a-4dc0-a89f-6ace6f2a8a61',  -- applicant: Bob
 'I have 6 years of AWS experience and have migrated 3 large-scale infrastructures to ECS in the last 2 years. Available immediately.',
 'https://bob-devops.fr',
 'ACCEPTED', NULL,
 '2025-01-07 09:00:00', '2025-01-08 15:00:00');

-- ============================================================
--  10. JOURNAUX D'AUDIT (org_audit_logs)
--      US-ORG-15 : traçabilité des actions administratives
-- ============================================================
INSERT IGNORE INTO org_audit_logs
    (id, organization_id, performed_by_user_id, action, details, created_at)
VALUES
-- Admin modifie le statut de TechVision Studio → ACTIVE
('al000001-0000-4000-8000-000000000001',
 'org00001-0000-4000-8000-000000000001',
 '9a3af34e-605d-4514-bd9b-4c2a2fb70413',  -- admin
 'STATUS_CHANGED',
 '{"from":"PENDING_VERIFICATION","to":"ACTIVE","reason":"SIRET verified, all documents validated"}',
 '2024-10-05 14:00:00'),

-- Bob met à jour la description de TechVision Studio
('al000002-0000-4000-8000-000000000002',
 'org00001-0000-4000-8000-000000000001',
 '6d1d18e1-813a-4dc0-a89f-6ace6f2a8a61',  -- bob (owner)
 'PROFILE_UPDATED',
 '{"fields":["description","specialties","website"]}',
 '2025-01-10 11:00:00'),

-- Admin suspend AlphaFreelance Coop
-- US-ORG-04 : action admin de suspension
('al000003-0000-4000-8000-000000000003',
 'org00004-0000-4000-8000-000000000004',
 '9a3af34e-605d-4514-bd9b-4c2a2fb70413',  -- admin
 'STATUS_CHANGED',
 '{"from":"ACTIVE","to":"SUSPENDED","reason":"Multiple misleading contract complaints. Investigation opened."}',
 '2025-02-10 16:00:00'),

-- Thomas ajoute une spécialité à Open Devs Collective
('al000004-0000-4000-8000-000000000004',
 'org00003-0000-4000-8000-000000000003',
 'fa100004-0000-4001-8001-000000000004',  -- thomas (owner)
 'PROFILE_UPDATED',
 '{"fields":["specialties"],"added":"Symfony"}',
 '2025-02-02 09:00:00');
