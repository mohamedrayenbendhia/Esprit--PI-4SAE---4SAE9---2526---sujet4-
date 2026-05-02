from pydantic import BaseModel
from typing import List, Optional

class FreelancerProfile(BaseModel):
    id: str  # Changé int -> str pour UUID
    skills: List[str] = []
    experience_years: int = 0
    hourly_rate: float = 0.0
    rating: float = 0.0
    completed_projects: int = 0

class JobOfferProfile(BaseModel):
    id: str  # Changé int -> str
    required_skills: List[str] = []
    budget: float = 0.0
    experience_required: int = 0
    category: Optional[str] = None

class RecommendationResult(BaseModel):
    freelancer_id: str # Changé int -> str
    job_offer_id: str  # Changé int -> str
    score: float
    match_details: dict = {}

class RecommendationResponse(BaseModel):
    job_offer_id: str
    recommendations: List[RecommendationResult]
    total: int

class HealthResponse(BaseModel):
    status: str
    service: str
    port: int
    eureka: str
    database: str

class SyncRequest(BaseModel):
    freelancer_id: Optional[str] = None
    job_offer_id: Optional[str] = None