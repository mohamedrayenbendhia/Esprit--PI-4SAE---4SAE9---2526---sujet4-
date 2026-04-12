-- Script d'initialisation de la base de données NexLance
-- Base de données pour le système de gestion des freelances

-- Créer la base de données si elle n'existe pas
CREATE DATABASE IF NOT EXISTS nexlance_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE nexlance_db;

-- Les tables seront créées automatiquement par Hibernate (spring.jpa.hibernate.ddl-auto=update)
-- Ce script est uniquement pour insérer des données de test

-- Supprimer les données de test existantes (optionnel)
-- DELETE FROM users WHERE email IN ('admin@nexlance.com', 'client@nexlance.com', 'freelancer@nexlance.com');

-- Insérer des utilisateurs de test pour chaque rôle
-- Mot de passe pour tous : "password123" (hashé avec BCrypt)
-- Note: Vous devez générer un nouveau hash BCrypt pour la production

-- Admin de test
INSERT INTO users (id, email, password, type, first_name, last_name, phone_number, status, subscription_type, email_verified, created_at, updated_at)
VALUES 
(UUID(), 'admin@nexlance.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1IEbhEhHhvLdUmE7CGl5bkXKN6Gz8Vi', 'ADMIN', 'Admin', 'NexLance', '+33612345678', 'ACTIVE', 'ENTERPRISE', true, NOW(), NOW())
ON DUPLICATE KEY UPDATE email = email;

-- Client de test
INSERT INTO users (id, email, password, type, first_name, last_name, phone_number, status, subscription_type, email_verified, created_at, updated_at)
VALUES 
(UUID(), 'client@nexlance.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1IEbhEhHhvLdUmE7CGl5bkXKN6Gz8Vi', 'CLIENT', 'Jean', 'Dupont', '+33623456789', 'ACTIVE', 'PREMIUM', true, NOW(), NOW())
ON DUPLICATE KEY UPDATE email = email;

-- Freelancer de test
INSERT INTO users (id, email, password, type, first_name, last_name, phone_number, status, subscription_type, email_verified, created_at, updated_at)
VALUES 
(UUID(), 'freelancer@nexlance.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1IEbhEhHhvLdUmE7CGl5bkXKN6Gz8Vi', 'FREELANCE', 'Marie', 'Martin', '+33634567890', 'ACTIVE', 'FREE', true, NOW(), NOW())
ON DUPLICATE KEY UPDATE email = email;

-- Plus d'utilisateurs de test
-- Client 2
INSERT INTO users (id, email, password, type, first_name, last_name, phone_number, status, subscription_type, email_verified, created_at, updated_at)
VALUES 
(UUID(), 'client2@nexlance.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1IEbhEhHhvLdUmE7CGl5bkXKN6Gz8Vi', 'CLIENT', 'Pierre', 'Bernard', '+33645678901', 'ACTIVE', 'FREE', true, NOW(), NOW())
ON DUPLICATE KEY UPDATE email = email;

-- Freelancer 2
INSERT INTO users (id, email, password, type, first_name, last_name, phone_number, status, subscription_type, email_verified, created_at, updated_at)
VALUES 
(UUID(), 'freelancer2@nexlance.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1IEbhEhHhvLdUmE7CGl5bkXKN6Gz8Vi', 'FREELANCE', 'Sophie', 'Dubois', '+33656789012', 'PENDING_VERIFICATION', 'FREE', false, NOW(), NOW())
ON DUPLICATE KEY UPDATE email = email;

-- Freelancer 3 - SANS profil freelance (pour tester l'affichage des users FREELANCE sans profil complet)
INSERT INTO users (id, email, password, type, first_name, last_name, phone_number, status, subscription_type, email_verified, created_at, updated_at)
VALUES 
(UUID(), 'khalilyy@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1IEbhEhHhvLdUmE7CGl5bkXKN6Gz8Vi', 'FREELANCE', 'Khalil', 'Younes', '+33698765432', 'ACTIVE', 'FREE', true, NOW(), NOW())
ON DUPLICATE KEY UPDATE email = email;

-- Utilisateur suspendu (pour tester)
INSERT INTO users (id, email, password, type, first_name, last_name, phone_number, status, subscription_type, email_verified, created_at, updated_at)
VALUES 
(UUID(), 'suspended@nexlance.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1IEbhEhHhvLdUmE7CGl5bkXKN6Gz8Vi', 'CLIENT', 'Test', 'Suspendu', '+33667890123', 'SUSPENDED', 'FREE', true, NOW(), NOW())
ON DUPLICATE KEY UPDATE email = email;

-- Afficher les utilisateurs créés
SELECT id, email, type, first_name, last_name, status, subscription_type, email_verified, created_at
FROM users
WHERE email LIKE '%@nexlance.com'
ORDER BY type, created_at;

-- Insérer des profils freelance de test
-- Note: Les IDs des utilisateurs sont générés dynamiquement, donc on utilise des sous-requêtes

-- Profil pour Marie Martin (freelancer@nexlance.com) - Développeuse Full Stack
INSERT INTO freelance_profiles (id, user_id, title, bio, hourly_rate, availability, experience_years, portfolio_url, linked_in_url, github_url, location, languages, timezone, completion_rate, response_time, skills, certifications, created_at, updated_at)
SELECT 
    UUID(),
    u.id,
    'Développeuse Full Stack Senior',
    'Développeuse passionnée avec 8 ans d''expérience dans le développement web. Spécialisée en Angular, React et Spring Boot. J''ai travaillé sur plus de 50 projets pour des clients internationaux. J''aime créer des applications performantes et élégantes qui résolvent de vrais problèmes business.',
    75.00,
    'AVAILABLE',
    8,
    'https://mariem-portfolio.dev',
    'https://linkedin.com/in/marie-martin',
    'https://github.com/mariemartin',
    'Paris, France',
    '["Français", "Anglais", "Espagnol"]',
    'Europe/Paris',
    98.50,
    2,
    '["Angular", "React", "TypeScript", "Spring Boot", "Java", "Node.js", "MySQL", "MongoDB", "Git", "Docker"]',
    '["Oracle Certified Professional Java SE", "AWS Certified Developer", "Scrum Master Certified"]',
    NOW(),
    NOW()
FROM users u
WHERE u.email = 'freelancer@nexlance.com'
ON DUPLICATE KEY UPDATE user_id = user_id;

-- Profil pour Sophie Dubois (freelancer2@nexlance.com) - Designer UI/UX
INSERT INTO freelance_profiles (id, user_id, title, bio, hourly_rate, availability, experience_years, portfolio_url, linked_in_url, github_url, location, languages, timezone, completion_rate, response_time, skills, certifications, created_at, updated_at)
SELECT 
    UUID(),
    u.id,
    'Designer UI/UX & Product Designer',
    'Designer créative avec 5 ans d''expérience en design d''interfaces et d''expérience utilisateur. Je transforme vos idées en designs modernes et intuitifs. Spécialisée en design mobile-first, systèmes de design et prototypage interactif. J''ai collaboré avec des startups et des grands comptes pour créer des expériences exceptionnelles.',
    60.00,
    'BUSY',
    5,
    'https://sophiedubois-design.com',
    'https://linkedin.com/in/sophie-dubois',
    'https://github.com/sophiedubois',
    'Lyon, France',
    '["Français", "Anglais"]',
    'Europe/Paris',
    95.00,
    3,
    '["Figma", "Adobe XD", "Sketch", "Photoshop", "Illustrator", "Prototyping", "User Research", "Wireframing", "Design Systems"]',
    '["Google UX Design Certificate", "Nielsen Norman Group UX Certification"]',
    NOW(),
    NOW()
FROM users u
WHERE u.email = 'freelancer2@nexlance.com'
ON DUPLICATE KEY UPDATE user_id = user_id;

-- Créer un utilisateur freelance supplémentaire et son profil
INSERT INTO users (id, email, password, type, first_name, last_name, phone_number, status, subscription_type, email_verified, created_at, updated_at)
VALUES 
(UUID(), 'freelancer3@nexlance.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1IEbhEhHhvLdUmE7CGl5bkXKN6Gz8Vi', 'FREELANCE', 'Thomas', 'Petit', '+33678901234', 'ACTIVE', 'PREMIUM', true, NOW(), NOW())
ON DUPLICATE KEY UPDATE email = email;

-- Profil pour Thomas Petit - DevOps Engineer
INSERT INTO freelance_profiles (id, user_id, title, bio, hourly_rate, availability, experience_years, portfolio_url, linked_in_url, github_url, location, languages, timezone, completion_rate, response_time, skills, certifications, created_at, updated_at)
SELECT 
    UUID(),
    u.id,
    'DevOps Engineer & Cloud Architect',
    'Ingénieur DevOps expérimenté avec 10 ans dans l''automatisation et le cloud. Expert en CI/CD, containerisation et orchestration. J''aide les entreprises à moderniser leur infrastructure et à accélérer leurs déploiements. Passionné par l''automatisation et les bonnes pratiques DevOps.',
    90.00,
    'AVAILABLE',
    10,
    'https://thomaspetit-devops.io',
    'https://linkedin.com/in/thomas-petit',
    'https://github.com/thomaspetit',
    'Marseille, France',
    '["Français", "Anglais", "Allemand"]',
    'Europe/Paris',
    99.00,
    1,
    '["Docker", "Kubernetes", "AWS", "Azure", "Terraform", "Jenkins", "GitLab CI", "Ansible", "Python", "Linux", "Monitoring"]',
    '["AWS Solutions Architect Professional", "Kubernetes Certified Administrator", "HashiCorp Terraform Associate"]',
    NOW(),
    NOW()
FROM users u
WHERE u.email = 'freelancer3@nexlance.com'
ON DUPLICATE KEY UPDATE user_id = user_id;

-- Créer un quatrième freelancer - Développeur Mobile
INSERT INTO users (id, email, password, type, first_name, last_name, phone_number, status, subscription_type, email_verified, created_at, updated_at)
VALUES 
(UUID(), 'freelancer4@nexlance.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1IEbhEhHhvLdUmE7CGl5bkXKN6Gz8Vi', 'FREELANCE', 'Emma', 'Lefebvre', '+33689012345', 'ACTIVE', 'PREMIUM', true, NOW(), NOW())
ON DUPLICATE KEY UPDATE email = email;

-- Profil pour Emma Lefebvre - Développeuse Mobile
INSERT INTO freelance_profiles (id, user_id, title, bio, hourly_rate, availability, experience_years, portfolio_url, linked_in_url, github_url, location, languages, timezone, completion_rate, response_time, skills, certifications, created_at, updated_at)
SELECT 
    UUID(),
    u.id,
    'Développeuse Mobile iOS & Android',
    'Développeuse mobile passionnée avec 6 ans d''expérience. Spécialisée en développement natif et cross-platform avec React Native et Flutter. J''ai publié plus de 30 applications sur l''App Store et Google Play avec des millions de téléchargements. Je crée des applications performantes et élégantes qui ravissent les utilisateurs.',
    70.00,
    'AVAILABLE',
    6,
    'https://emmalefebvre-apps.com',
    'https://linkedin.com/in/emma-lefebvre',
    'https://github.com/emmalefebvre',
    'Toulouse, France',
    '["Français", "Anglais", "Italien"]',
    'Europe/Paris',
    97.00,
    2,
    '["React Native", "Flutter", "Swift", "Kotlin", "iOS", "Android", "Firebase", "REST APIs", "Mobile UI", "Performance Optimization"]',
    '["Google Associate Android Developer", "Meta React Native Specialist"]',
    NOW(),
    NOW()
FROM users u
WHERE u.email = 'freelancer4@nexlance.com'
ON DUPLICATE KEY UPDATE user_id = user_id;

-- Créer un cinquième freelancer - Data Scientist
INSERT INTO users (id, email, password, type, first_name, last_name, phone_number, status, subscription_type, email_verified, created_at, updated_at)
VALUES 
(UUID(), 'freelancer5@nexlance.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1IEbhEhHhvLdUmE7CGl5bkXKN6Gz8Vi', 'FREELANCE', 'Lucas', 'Moreau', '+33690123456', 'ACTIVE', 'FREE', true, NOW(), NOW())
ON DUPLICATE KEY UPDATE email = email;

-- Profil pour Lucas Moreau - Data Scientist
INSERT INTO freelance_profiles (id, user_id, title, bio, hourly_rate, availability, experience_years, portfolio_url, linked_in_url, github_url, location, languages, timezone, completion_rate, response_time, skills, certifications, created_at, updated_at)
SELECT 
    UUID(),
    u.id,
    'Data Scientist & Machine Learning Engineer',
    'Data Scientist avec 7 ans d''expérience en analyse de données et machine learning. Expert en Python, TensorFlow et PyTorch. J''aide les entreprises à exploiter leurs données pour prendre de meilleures décisions. Spécialisé en NLP, computer vision et systèmes de recommandation. Passionné par l''IA et ses applications concrètes.',
    85.00,
    'UNAVAILABLE',
    7,
    'https://lucasmoreau-ml.ai',
    'https://linkedin.com/in/lucas-moreau',
    'https://github.com/lucasmoreau',
    'Bordeaux, France',
    '["Français", "Anglais"]',
    'Europe/Paris',
    96.50,
    4,
    '["Python", "TensorFlow", "PyTorch", "Scikit-learn", "Pandas", "NumPy", "Machine Learning", "Deep Learning", "NLP", "Computer Vision", "SQL"]',
    '["Google Cloud Professional Data Engineer", "AWS Machine Learning Specialty", "TensorFlow Developer Certificate"]',
    NOW(),
    NOW()
FROM users u
WHERE u.email = 'freelancer5@nexlance.com'
ON DUPLICATE KEY UPDATE user_id = user_id;

-- Créer un sixième freelancer - Développeur Backend Java
INSERT INTO users (id, email, password, type, first_name, last_name, phone_number, status, subscription_type, email_verified, created_at, updated_at)
VALUES 
(UUID(), 'freelancer6@nexlance.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1IEbhEhHhvLdUmE7CGl5bkXKN6Gz8Vi', 'FREELANCE', 'Julien', 'Rousseau', '+33701234567', 'ACTIVE', 'PREMIUM', true, NOW(), NOW())
ON DUPLICATE KEY UPDATE email = email;

-- Profil pour Julien Rousseau - Développeur Backend Java Senior
INSERT INTO freelance_profiles (id, user_id, title, bio, hourly_rate, availability, experience_years, portfolio_url, linked_in_url, github_url, location, languages, timezone, completion_rate, response_time, skills, certifications, created_at, updated_at)
SELECT 
    UUID(),
    u.id,
    'Développeur Backend Java Senior',
    'Expert en architecture backend avec 9 ans d''expérience. Spécialisé en microservices, Spring Boot et bases de données distribuées. J''ai conçu et développé des systèmes distribués pour des entreprises du Fortune 500. Passionné par les architectures scalables, la performance et les bonnes pratiques. J''accompagne les équipes dans la modernisation de leur infrastructure backend.',
    85.00,
    'AVAILABLE',
    9,
    'https://backend-expert.dev',
    'https://linkedin.com/in/julien-rousseau',
    'https://github.com/jrousseau',
    'Nice, France',
    '["Français", "Anglais"]',
    'Europe/Paris',
    97.50,
    2,
    '["Java", "Spring Boot", "Microservices", "PostgreSQL", "Redis", "Kafka", "Docker", "Kubernetes", "API REST", "GraphQL"]',
    '["Oracle Certified Professional Java SE 11", "Spring Professional Certification", "AWS Certified Solutions Architect"]',
    NOW(),
    NOW()
FROM users u
WHERE u.email = 'freelancer6@nexlance.com'
ON DUPLICATE KEY UPDATE user_id = user_id;

-- Créer un septième freelancer - Expert Cybersécurité
INSERT INTO users (id, email, password, type, first_name, last_name, phone_number, status, subscription_type, email_verified, created_at, updated_at)
VALUES 
(UUID(), 'freelancer7@nexlance.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1IEbhEhHhvLdUmE7CGl5bkXKN6Gz8Vi', 'FREELANCE', 'Alexandre', 'Laurent', '+33712345678', 'ACTIVE', 'PREMIUM', true, NOW(), NOW())
ON DUPLICATE KEY UPDATE email = email;

-- Profil pour Alexandre Laurent - Expert Cybersécurité
INSERT INTO freelance_profiles (id, user_id, title, bio, hourly_rate, availability, experience_years, portfolio_url, linked_in_url, github_url, location, languages, timezone, completion_rate, response_time, skills, certifications, created_at, updated_at)
SELECT 
    UUID(),
    u.id,
    'Expert Cybersécurité & Pentesting',
    'Consultant en cybersécurité avec 7 ans d''expérience en tests d''intrusion et audits de sécurité. Expert en sécurité applicative, infrastructure et cloud. J''ai sécurisé plus de 100 applications web et mobiles. Certifié OSCP et CEH. Je réalise des audits de sécurité complets, des tests d''intrusion et forme les équipes aux bonnes pratiques de sécurité.',
    95.00,
    'BUSY',
    7,
    'https://cybersec-expert.io',
    'https://linkedin.com/in/alexandre-laurent',
    'https://github.com/alaurent',
    'Lille, France',
    '["Français", "Anglais", "Espagnol"]',
    'Europe/Paris',
    98.00,
    3,
    '["Pentesting", "OWASP", "Security Audit", "Burp Suite", "Metasploit", "Nmap", "Wireshark", "Cryptographie", "ISO 27001", "GDPR"]',
    '["OSCP - Offensive Security Certified Professional", "CEH - Certified Ethical Hacker", "CISSP"]',
    NOW(),
    NOW()
FROM users u
WHERE u.email = 'freelancer7@nexlance.com'
ON DUPLICATE KEY UPDATE user_id = user_id;

-- Créer un huitième freelancer - Architecte Cloud
INSERT INTO users (id, email, password, type, first_name, last_name, phone_number, status, subscription_type, email_verified, created_at, updated_at)
VALUES 
(UUID(), 'freelancer8@nexlance.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1IEbhEhHhvLdUmE7CGl5bkXKN6Gz8Vi', 'FREELANCE', 'Camille', 'Durand', '+33723456789', 'ACTIVE', 'FREE', true, NOW(), NOW())
ON DUPLICATE KEY UPDATE email = email;

-- Profil pour Camille Durand - Architecte Cloud
INSERT INTO freelance_profiles (id, user_id, title, bio, hourly_rate, availability, experience_years, portfolio_url, linked_in_url, github_url, location, languages, timezone, completion_rate, response_time, skills, certifications, created_at, updated_at)
SELECT 
    UUID(),
    u.id,
    'Architecte Cloud AWS & Azure',
    'Architecte cloud avec 8 ans d''expérience dans la conception et la migration de solutions cloud. Expert AWS et Azure. J''ai accompagné plus de 30 entreprises dans leur transformation cloud. Spécialisée en architecture distribuée, haute disponibilité et optimisation des coûts. Je conçois des infrastructures cloud robustes et scalables.',
    80.00,
    'AVAILABLE',
    8,
    'https://cloud-architect.pro',
    'https://linkedin.com/in/camille-durand',
    'https://github.com/cdurand',
    'Nantes, France',
    '["Français", "Anglais"]',
    'Europe/Paris',
    96.00,
    2,
    '["AWS", "Azure", "Cloud Architecture", "Terraform", "CloudFormation", "Serverless", "Lambda", "S3", "EC2", "RDS", "Cost Optimization"]',
    '["AWS Solutions Architect Professional", "Azure Solutions Architect Expert", "Terraform Associate"]',
    NOW(),
    NOW()
FROM users u
WHERE u.email = 'freelancer8@nexlance.com'
ON DUPLICATE KEY UPDATE user_id = user_id;

-- Créer un neuvième freelancer - Consultant SEO
INSERT INTO users (id, email, password, type, first_name, last_name, phone_number, status, subscription_type, email_verified, created_at, updated_at)
VALUES 
(UUID(), 'freelancer9@nexlance.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1IEbhEhHhvLdUmE7CGl5bkXKN6Gz8Vi', 'FREELANCE', 'Léa', 'Martinez', '+33734567890', 'ACTIVE', 'PREMIUM', true, NOW(), NOW())
ON DUPLICATE KEY UPDATE email = email;

-- Profil pour Léa Martinez - Consultante SEO
INSERT INTO freelance_profiles (id, user_id, title, bio, hourly_rate, availability, experience_years, portfolio_url, linked_in_url, github_url, location, languages, timezone, completion_rate, response_time, skills, certifications, created_at, updated_at)
SELECT 
    UUID(),
    u.id,
    'Consultante SEO & Marketing Digital',
    'Experte SEO avec 6 ans d''expérience en référencement naturel et stratégie digitale. J''ai multiplié le trafic organique de plus de 80 sites web. Spécialisée en SEO technique, contenu optimisé et stratégies de croissance. Je réalise des audits SEO complets, optimise le référencement et développe des stratégies de contenu performantes.',
    65.00,
    'AVAILABLE',
    6,
    'https://seo-expert.digital',
    'https://linkedin.com/in/lea-martinez',
    'https://github.com/lmartinez',
    'Montpellier, France',
    '["Français", "Anglais", "Espagnol"]',
    'Europe/Paris',
    99.00,
    1,
    '["SEO", "Google Analytics", "SEMrush", "Ahrefs", "Content Marketing", "Link Building", "Technical SEO", "Google Search Console", "Keyword Research"]',
    '["Google Analytics Certified", "Google Ads Certified", "HubSpot Content Marketing Certified"]',
    NOW(),
    NOW()
FROM users u
WHERE u.email = 'freelancer9@nexlance.com'
ON DUPLICATE KEY UPDATE user_id = user_id;

-- Créer un dixième freelancer - Chef de Projet Agile
INSERT INTO users (id, email, password, type, first_name, last_name, phone_number, status, subscription_type, email_verified, created_at, updated_at)
VALUES 
(UUID(), 'freelancer10@nexlance.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1IEbhEhHhvLdUmE7CGl5bkXKN6Gz8Vi', 'FREELANCE', 'Antoine', 'Bernard', '+33745678901', 'ACTIVE', 'FREE', true, NOW(), NOW())
ON DUPLICATE KEY UPDATE email = email;

-- Profil pour Antoine Bernard - Chef de Projet Agile
INSERT INTO freelance_profiles (id, user_id, title, bio, hourly_rate, availability, experience_years, portfolio_url, linked_in_url, github_url, location, languages, timezone, completion_rate, response_time, skills, certifications, created_at, updated_at)
SELECT 
    UUID(),
    u.id,
    'Chef de Projet Agile & Scrum Master',
    'Chef de projet certifié avec 10 ans d''expérience en gestion de projets digitaux. Expert Scrum et Kanban. J''ai piloté plus de 60 projets pour des startups et des grands comptes. Spécialisé en transformation agile, coaching d''équipe et livraison continue. J''accompagne les équipes vers l''excellence opérationnelle et la livraison de valeur.',
    75.00,
    'UNAVAILABLE',
    10,
    'https://agile-pm.pro',
    'https://linkedin.com/in/antoine-bernard',
    'https://github.com/abernard',
    'Strasbourg, France',
    '["Français", "Anglais", "Allemand"]',
    'Europe/Paris',
    98.50,
    2,
    '["Scrum", "Agile", "Kanban", "JIRA", "Confluence", "Project Management", "Team Coaching", "Roadmap Planning", "Sprint Planning", "Stakeholder Management"]',
    '["Certified Scrum Master", "SAFe Agilist", "PMP - Project Management Professional", "Professional Scrum Product Owner"]',
    NOW(),
    NOW()
FROM users u
WHERE u.email = 'freelancer10@nexlance.com'
ON DUPLICATE KEY UPDATE user_id = user_id;

-- Afficher les profils freelance créés
SELECT 
    fp.id,
    u.first_name,
    u.last_name,
    u.email,
    fp.title,
    fp.hourly_rate,
    fp.availability,
    fp.experience_years,
    fp.location,
    fp.skills
FROM freelance_profiles fp
JOIN users u ON fp.user_id = u.id
ORDER BY fp.created_at;
