import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import logging
from typing import List, Tuple

from database import get_connection

logger = logging.getLogger(__name__)

# Vectorizer TF-IDF global pour les compétences
_vectorizer = TfidfVectorizer(lowercase=True, token_pattern=r"(?u)\b\w[\w\+\#\-\.]+\b")


def compute_skill_similarity(freelancer_skills: List[str], job_skills: List[str]) -> float:
    """Calcule la similarité cosinus entre les compétences du freelancer et du job."""
    if not freelancer_skills or not job_skills:
        return 0.0

    freelancer_text = " ".join(freelancer_skills)
    job_text = " ".join(job_skills)

    try:
        tfidf_matrix = _vectorizer.fit_transform([freelancer_text, job_text])
        similarity = cosine_similarity(tfidf_matrix[0:1], tfidf_matrix[1:2])[0][0]
        return float(similarity)
    except Exception:
        # Fallback: Jaccard similarity
        f_set = set(s.lower() for s in freelancer_skills)
        j_set = set(s.lower() for s in job_skills)
        if not f_set or not j_set:
            return 0.0
        intersection = f_set & j_set
        union = f_set | j_set
        return len(intersection) / len(union)


def compute_experience_score(freelancer_exp: int, required_exp: int) -> float:
    """Score basé sur l'expérience (1.0 si le freelancer a assez d'expérience)."""
    if required_exp <= 0:
        return 1.0
    if freelancer_exp >= required_exp:
        return 1.0
    return freelancer_exp / required_exp


def compute_budget_match(hourly_rate: float, budget: float) -> float:
    """Score de correspondance budget/tarif."""
    if budget <= 0 or hourly_rate <= 0:
        return 0.5  # neutre si pas d'info
    ratio = hourly_rate / budget
    if ratio <= 1.0:
        return 1.0  # dans le budget
    elif ratio <= 1.5:
        return 1.0 - (ratio - 1.0)  # dégradation linéaire
    return 0.0


def compute_global_score(
    skill_sim: float,
    exp_score: float,
    budget_score: float,
    rating: float,
    completed: int,
) -> float:
    """
    Score global pondéré:
    - 40% compétences
    - 20% expérience
    - 15% budget
    - 15% rating
    - 10% projets complétés
    """
    rating_norm = rating / 5.0 if rating > 0 else 0.0
    completed_norm = min(completed / 50.0, 1.0)

    score = (
        0.40 * skill_sim
        + 0.20 * exp_score
        + 0.15 * budget_score
        + 0.15 * rating_norm
        + 0.10 * completed_norm
    )
    return round(score, 4)


def get_recommendations_for_job(job_offer_id: int, limit: int = 10) -> List[Tuple[int, float, dict]]:
    """Retourne les meilleurs freelancers pour une offre d'emploi."""
    conn = get_connection()
    if not conn:
        return []

    try:
        cursor = conn.cursor(dictionary=True)

        # Récupérer l'offre d'emploi
        cursor.execute("SELECT * FROM job_offer_profiles WHERE id = %s", (job_offer_id,))
        job = cursor.fetchone()
        if not job:
            return []

        job_skills = job["required_skills"].split(",") if job["required_skills"] else []
        job_budget = float(job["budget"] or 0)
        job_exp = int(job["experience_required"] or 0)

        # Récupérer tous les freelancers
        cursor.execute("SELECT * FROM freelancer_profiles")
        freelancers = cursor.fetchall()
        cursor.close()

        results = []
        for f in freelancers:
            f_skills = f["skills"].split(",") if f["skills"] else []
            skill_sim = compute_skill_similarity(f_skills, job_skills)
            exp_score = compute_experience_score(int(f["experience_years"] or 0), job_exp)
            budget_score = compute_budget_match(float(f["hourly_rate"] or 0), job_budget)
            rating = float(f["rating"] or 0)
            completed = int(f["completed_projects"] or 0)

            global_score = compute_global_score(skill_sim, exp_score, budget_score, rating, completed)

            details = {
                "skill_similarity": round(skill_sim, 4),
                "experience_score": round(exp_score, 4),
                "budget_match": round(budget_score, 4),
                "rating_score": round(rating / 5.0, 4) if rating > 0 else 0.0,
                "projects_score": round(min(completed / 50.0, 1.0), 4),
            }
            results.append((f["id"], global_score, details))

        # Trier par score décroissant
        results.sort(key=lambda x: x[1], reverse=True)
        return results[:limit]

    except Exception as e:
        logger.error("Erreur calcul recommandations pour job %d: %s", job_offer_id, e)
        return []
    finally:
        conn.close()


def get_recommendations_for_freelancer(freelancer_id: int, limit: int = 10) -> List[Tuple[int, float, dict]]:
    """Retourne les meilleures offres pour un freelancer."""
    conn = get_connection()
    if not conn:
        return []

    try:
        cursor = conn.cursor(dictionary=True)

        # Récupérer le freelancer
        cursor.execute("SELECT * FROM freelancer_profiles WHERE id = %s", (freelancer_id,))
        freelancer = cursor.fetchone()
        if not freelancer:
            return []

        f_skills = freelancer["skills"].split(",") if freelancer["skills"] else []
        f_exp = int(freelancer["experience_years"] or 0)
        f_rate = float(freelancer["hourly_rate"] or 0)
        f_rating = float(freelancer["rating"] or 0)
        f_completed = int(freelancer["completed_projects"] or 0)

        # Récupérer toutes les offres
        cursor.execute("SELECT * FROM job_offer_profiles")
        jobs = cursor.fetchall()
        cursor.close()

        results = []
        for job in jobs:
            job_skills = job["required_skills"].split(",") if job["required_skills"] else []
            skill_sim = compute_skill_similarity(f_skills, job_skills)
            exp_score = compute_experience_score(f_exp, int(job["experience_required"] or 0))
            budget_score = compute_budget_match(f_rate, float(job["budget"] or 0))

            global_score = compute_global_score(skill_sim, exp_score, budget_score, f_rating, f_completed)

            details = {
                "skill_similarity": round(skill_sim, 4),
                "experience_score": round(exp_score, 4),
                "budget_match": round(budget_score, 4),
            }
            results.append((job["id"], global_score, details))

        results.sort(key=lambda x: x[1], reverse=True)
        return results[:limit]

    except Exception as e:
        logger.error("Erreur calcul recommandations pour freelancer %d: %s", freelancer_id, e)
        return []
    finally:
        conn.close()


def save_recommendations(recommendations: List[Tuple[int, int, float]]):
    """Persiste les recommandations en base."""
    conn = get_connection()
    if not conn:
        return

    try:
        cursor = conn.cursor()
        cursor.executemany(
            "INSERT INTO recommendations (freelancer_id, job_offer_id, score) VALUES (%s, %s, %s)",
            recommendations,
        )
        cursor.close()
        logger.info("%d recommandations sauvegardées", len(recommendations))
    except Exception as e:
        logger.error("Erreur sauvegarde recommandations: %s", e)
    finally:
        conn.close()
