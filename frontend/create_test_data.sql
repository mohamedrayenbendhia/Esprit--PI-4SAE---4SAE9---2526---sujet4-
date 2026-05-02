-- ============================================
-- Script de Données de Test pour NexLance
-- Module: Certification, Competence, Portfolio
-- ============================================

-- ============================================
-- DATABASE: nexlance_competence
-- ============================================

USE nexlance_competence;

-- Ajouter des skills (si pas déjà créés)
INSERT INTO skills (name, description, created_at, updated_at)
VALUES 
    ('Java', 'Java programming language', NOW(), NOW()),
    ('JavaScript', 'JavaScript programming language', NOW(), NOW()),
    ('Python', 'Python programming language', NOW(), NOW()),
    ('Spring', 'Spring Framework for Java', NOW(), NOW()),
    ('Symfony', 'Symfony Framework for PHP', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=name;

-- Vérifier les IDs créés
SELECT * FROM skills;

-- ============================================
-- DATABASE: nexlance_certification
-- ============================================

USE nexlance_certification;

-- Créer des tests pour chaque skill
-- IMPORTANT: Remplacer les skill_id par les vrais IDs de votre base

-- Test Java (supposons skill_id = 1)
INSERT INTO tests (title, skill_id, passing_score, created_at, updated_at)
VALUES ('Java Test', 1, 70, NOW(), NOW());

SET @java_test_id = LAST_INSERT_ID();

INSERT INTO questions (test_id, question_text, correct_answer, created_at, updated_at)
VALUES 
    (@java_test_id, 'À quoi sert this ?', 'this', NOW(), NOW()),
    (@java_test_id, 'Qu''est-ce qu''un constructeur ?', 'constructeur', NOW(), NOW()),
    (@java_test_id, 'Comment faire une constante ?', 'final', NOW(), NOW()),
    (@java_test_id, 'Que fait System.out.println() ?', 'afficher', NOW(), NOW()),
    (@java_test_id, 'C''est quoi une exception ?', 'exception', NOW(), NOW());

-- Test JavaScript (supposons skill_id = 2)
INSERT INTO tests (title, skill_id, passing_score, created_at, updated_at)
VALUES ('JavaScript Test', 2, 70, NOW(), NOW());

SET @js_test_id = LAST_INSERT_ID();

INSERT INTO questions (test_id, question_text, correct_answer, created_at, updated_at)
VALUES 
    (@js_test_id, 'Quel mot-clé pour déclarer une variable ?', 'let', NOW(), NOW()),
    (@js_test_id, 'Qu''est-ce que const ?', 'constante', NOW(), NOW()),
    (@js_test_id, 'Comment créer une fonction ?', 'function', NOW(), NOW()),
    (@js_test_id, 'Qu''est-ce qu''une promise ?', 'asynchrone', NOW(), NOW()),
    (@js_test_id, 'À quoi sert async/await ?', 'asynchrone', NOW(), NOW());

-- Test Python (supposons skill_id = 3)
INSERT INTO tests (title, skill_id, passing_score, created_at, updated_at)
VALUES ('Python Test', 3, 70, NOW(), NOW());

SET @python_test_id = LAST_INSERT_ID();

INSERT INTO questions (test_id, question_text, correct_answer, created_at, updated_at)
VALUES 
    (@python_test_id, 'Comment déclarer une variable ?', 'variable', NOW(), NOW()),
    (@python_test_id, 'Qu''est-ce qu''une liste ?', 'liste', NOW(), NOW()),
    (@python_test_id, 'À quoi sert def ?', 'fonction', NOW(), NOW()),
    (@python_test_id, 'Comment importer un module ?', 'import', NOW(), NOW()),
    (@python_test_id, 'Qu''est-ce qu''un dictionnaire ?', 'dictionnaire', NOW(), NOW());

-- Test Spring (supposons skill_id = 4)
INSERT INTO tests (title, skill_id, passing_score, created_at, updated_at)
VALUES ('Spring Test', 4, 70, NOW(), NOW());

SET @spring_test_id = LAST_INSERT_ID();

INSERT INTO questions (test_id, question_text, correct_answer, created_at, updated_at)
VALUES 
    (@spring_test_id, 'Qu''est-ce que Spring Boot ?', 'framework', NOW(), NOW()),
    (@spring_test_id, 'À quoi sert @Autowired ?', 'injection', NOW(), NOW()),
    (@spring_test_id, 'Qu''est-ce qu''un bean ?', 'bean', NOW(), NOW()),
    (@spring_test_id, 'À quoi sert @RestController ?', 'controller', NOW(), NOW()),
    (@spring_test_id, 'Qu''est-ce que JPA ?', 'persistence', NOW(), NOW());

-- Test Symfony (supposons skill_id = 5)
INSERT INTO tests (title, skill_id, passing_score, created_at, updated_at)
VALUES ('Symfony Test', 5, 70, NOW(), NOW());

SET @symfony_test_id = LAST_INSERT_ID();

INSERT INTO questions (test_id, question_text, correct_answer, created_at, updated_at)
VALUES 
    (@symfony_test_id, 'Qu''est-ce que Symfony ?', 'framework', NOW(), NOW()),
    (@symfony_test_id, 'À quoi sert Doctrine ?', 'orm', NOW(), NOW()),
    (@symfony_test_id, 'Qu''est-ce qu''un controller ?', 'controller', NOW(), NOW()),
    (@symfony_test_id, 'À quoi sert Twig ?', 'template', NOW(), NOW()),
    (@symfony_test_id, 'Qu''est-ce qu''un bundle ?', 'bundle', NOW(), NOW());

-- Vérifier les tests créés
SELECT t.id, t.title, t.skill_id, t.passing_score, COUNT(q.id) as num_questions
FROM tests t
LEFT JOIN questions q ON q.test_id = t.id
GROUP BY t.id;

-- ============================================
-- Vérifier la structure
-- ============================================

SELECT 'Tests' as table_name, COUNT(*) as count FROM tests
UNION ALL
SELECT 'Questions', COUNT(*) FROM questions;

-- ============================================
-- NOTES IMPORTANTES
-- ============================================

/*
INSTRUCTIONS POUR L'UTILISATION:

1. Exécuter d'abord la section nexlance_competence pour créer les skills
2. Noter les IDs des skills créés
3. Remplacer les skill_id dans les INSERT INTO tests
4. Exécuter la section nexlance_certification

5. Pour tester:
   - Se connecter en tant que freelancer
   - Ajouter des skills dans "My Skills"
   - Aller dans "My Certifications"
   - Cliquer sur "Take a Test"
   - Choisir un skill
   - Répondre aux questions avec les mots clés (la casse n'importe pas)
   - Le score devrait être calculé correctement maintenant

6. Réponses acceptées (exemples):
   - "this" = "THIS" = "This" = "  this  " (tous valides)
   - "constructeur" = "CONSTRUCTEUR" = "Constructeur" (tous valides)
   - etc.

7. Score calculé:
   - Si 5/5 correctes → 100%
   - Si 4/5 correctes → 80%
   - Si 3/5 correctes → 60% (échec, cooldown 2 min)
   - etc.
*/

-- ============================================
-- DATABASE: nexlance_portfolio (optionnel)
-- ============================================

-- Note: Les portfolios et projets sont créés via l'interface utilisateur
-- Car ils nécessitent userId du JWT
