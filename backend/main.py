"""
Auris Backend - MVP (Async + Dependency Injection)
"""

import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import ALLOWED_ORIGINS
from app.middleware.audit import AuditLogMiddleware
from app.routers import patients, checkins, dashboard, therapist_schedule

logger = logging.getLogger(__name__)

app = FastAPI(title="Auris MVP", version="0.1.0")

# CORS — pinned to explicit origins; falls back to localhost dev defaults.
_origins = ALLOWED_ORIGINS or [
    "http://localhost:3000",
    "http://localhost:8501",
]
app.add_middleware(
    CORSMiddleware,
    allow_origins=_origins,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type"],
)

# HIPAA audit trail — logs every PHI-touching request to Snowflake.
app.add_middleware(AuditLogMiddleware)

# Include routers
app.include_router(patients.router)
app.include_router(checkins.router)
app.include_router(dashboard.router)
app.include_router(therapist_schedule.router)


@app.get("/")
async def root():
    return {"message": "Auris MVP Backend (Async + DI)", "status": "ok"}


@app.get("/health")
async def health():
    from app.core.database import get_db
    try:
        db = get_db()
        await db.query("SELECT 1")
        return {"status": "healthy", "db": "connected"}
    except Exception:
        return {"status": "unhealthy"}


# Run: uvicorn main:app --reload
