"""
Auris Backend - MVP (Async + Dependency Injection)
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.routers import patients, checkins, dashboard, therapist_schedule

app = FastAPI(title="Auris MVP", version="0.1.0")

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

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
    except Exception as e:
        return {"status": "unhealthy", "error": str(e)}


# Run: uvicorn main:app --reload
