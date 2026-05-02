import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException, Query, Body
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse # Import ajouté
from typing import List, Optional

from config import get_config
from database import init_db, get_connection
from eureka_registration import register_with_eureka, deregister_from_eureka
from models import HealthResponse

# --- Configuration du Logging ---
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# --- Configuration ---
cfg = get_config()
APP_NAME = "recommendation-ai"
PORT = int(cfg.get("server.port", 9000))
EUREKA_URL = cfg.get("eureka.client.service-url.defaultZone", "http://localhost:8761/eureka/")

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup : Initialisation DB et Enregistrement Eureka
    init_db(cfg)
    await register_with_eureka(APP_NAME, PORT, EUREKA_URL)
    logger.info(f"🚀 {APP_NAME} démarré sur le port {PORT}")
    yield
    # Shutdown : Désinscription Eureka
    await deregister_from_eureka()
    logger.info(f"🛑 {APP_NAME} arrêté")

app = FastAPI(title="NexLance AI", lifespan=lifespan)

# Configuration CORS (La Gateway s'occupe de la déduplication)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- ENDPOINTS ---

@app.get("/api/recommendations/health", response_model=HealthResponse)
def health_check():
    """Vérifie l'état de santé du service et de la DB."""
    conn = get_connection()
    db_status = "UP" if conn else "DOWN"
    if conn: conn.close()
    return HealthResponse(
        status="UP",
        service=APP_NAME,
        port=PORT,
        eureka="REGISTERED",
        database=db_status
    )

@app.get("/api/recommendations/for-job/{job_id}")
async def get_recommendations_for_job(job_id: str, limit: int = Query(10)):
    """Récupère les freelancers recommandés pour un job spécifique."""
    conn = get_connection()
    if not conn:
        raise HTTPException(status_code=500, detail="Database connection failed")

    try:
        cursor = conn.cursor(dictionary=True)
        query = """
                SELECT f.*, COALESCE(r.score, 0) as matching_score
                FROM freelancer_profiles f
                         LEFT JOIN recommendations r ON f.id = r.freelancer_id AND r.job_offer_id = %s
                ORDER BY matching_score DESC, f.rating DESC
                    LIMIT %s
                """
        cursor.execute(query, (job_id, limit))
        results = cursor.fetchall()
        return results
    except Exception as e:
        logger.error(f"Erreur lors de la récupération : {e}")
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        conn.close()

# --- ENDPOINTS DE SYNCHRONISATION (Corrigés avec JSONResponse) ---

@app.post("/api/recommendations/sync/freelancer")
async def sync_freelancer(data: dict = Body(...)):
    """Reçoit les données d'un freelancer depuis le service USER-SERVICE."""
    conn = get_connection()
    try:
        cursor = conn.cursor()
        query = """
                INSERT INTO freelancer_profiles (id, skills, experience_years, hourly_rate, rating)
                VALUES (%s, %s, %s, %s, %s)
                    ON DUPLICATE KEY UPDATE
                                         skills=%s, experience_years=%s, hourly_rate=%s, rating=%s
                """
        values = (
            data['id'], data['skills'], data.get('experienceYears', 0),
            data.get('hourlyRate', 0), data.get('rating', 0),
            data['skills'], data.get('experienceYears', 0),
            data.get('hourlyRate', 0), data.get('rating', 0)
        )
        cursor.execute(query, values)
        # On renvoie un JSON explicite pour éviter l'erreur "Unexpected Token"
        return JSONResponse(
            status_code=200,
            content={"status": "success", "message": "Freelancer synced"}
        )
    except Exception as e:
        logger.error(f"Erreur synchro freelancer: {e}")
        return JSONResponse(
            status_code=500,
            content={"status": "error", "message": str(e)}
        )
    finally:
        conn.close()

@app.post("/api/recommendations/sync/job")
async def sync_job(data: dict = Body(...)):
    """Reçoit les données d'une offre depuis le service JOBOFFER-SERVICE."""
    conn = get_connection()
    try:
        cursor = conn.cursor()
        query = """
                INSERT INTO job_offer_profiles (id, required_skills, budget, experience_required, category)
                VALUES (%s, %s, %s, %s, %s)
                    ON DUPLICATE KEY UPDATE
                                         required_skills=%s, budget=%s, experience_required=%s, category=%s
                """
        values = (
            data['id'], data['requiredSkills'], data.get('budget', 0),
            data.get('experienceRequired', 0), data.get('category', ''),
            data['requiredSkills'], data.get('budget', 0),
            data.get('experienceRequired', 0), data.get('category', '')
        )
        cursor.execute(query, values)
        return JSONResponse(
            status_code=200,
            content={"status": "success", "message": "Job synced"}
        )
    except Exception as e:
        logger.error(f"Erreur synchro job: {e}")
        return JSONResponse(
            status_code=500,
            content={"status": "error", "message": str(e)}
        )
    finally:
        conn.close()

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=PORT)