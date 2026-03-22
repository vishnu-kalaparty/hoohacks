"""Therapist dashboard routes with trends."""
import asyncio
from fastapi import APIRouter, Depends, HTTPException
from fastapi.security import HTTPBearer
from app.core.database import get_db, SnowflakeDB
from app.core.auth import get_current_user
from app.services.embedding_pipeline import get_session_trends, get_question_hrv_trends

security = HTTPBearer()

router = APIRouter(
    prefix="/dashboard",
    tags=["dashboard"],
    dependencies=[Depends(security)]
)


@router.get("/therapists/{therapist_id}/patients")
async def get_patients(
    therapist_id: int,
    db: SnowflakeDB = Depends(get_db)
):
    """Get therapist's patients with check-in count."""
    rows = await db.query("""
        SELECT 
            p.PATIENT_ID, p.NAME, p.EMAIL, p.ASSIGNED_SCALE,
            (SELECT COUNT(*) FROM CHECKIN_SESSIONS cs 
             WHERE cs.PATIENT_ID = p.PATIENT_ID) as CHECKIN_COUNT
        FROM CADENCE.PUBLIC.PATIENTS p
        WHERE p.THERAPIST_ID = %s
    """, (therapist_id,))
    
    return {"patients": rows}


@router.get("/patients/{patient_id}/brief")
async def get_brief(
    patient_id: int,
    db: SnowflakeDB = Depends(get_db),
    user: dict = Depends(get_current_user)
):
    """Get complete patient brief for therapist dashboard."""
    tasks = [
        db.query("""
            SELECT * FROM CADENCE.PUBLIC.CHECKIN_SESSIONS
            WHERE PATIENT_ID = %s
            ORDER BY CHECKIN_DATE DESC
            LIMIT 1
        """, (patient_id,)),
        
        db.query("""
            SELECT CHECKIN_DATE, SCALE_SCORE, HRV_VALUE
            FROM CADENCE.PUBLIC.CHECKIN_SESSIONS
            WHERE PATIENT_ID = %s
            AND CHECKIN_DATE >= DATEADD(day, -7, CURRENT_DATE)
            ORDER BY CHECKIN_DATE
        """, (patient_id,)),
        
        db.query("""
            SELECT QUESTION_ID, QUESTION_TEXT, RESPONSE_VALUE, HRV_AT_QUESTION
            FROM CADENCE.PUBLIC.QUESTION_VITALS
            WHERE PATIENT_ID = %s AND IS_VITALS_CORRELATED = TRUE
            ORDER BY CAPTURED_AT DESC
            LIMIT 20
        """, (patient_id,)),
        
        db.query("""
            SELECT 1 as HAS_FLAG FROM CADENCE.PUBLIC.QUESTION_VITALS qv
            JOIN CADENCE.PUBLIC.CHECKIN_SESSIONS cs ON qv.SESSION_ID = cs.SESSION_ID
            WHERE qv.PATIENT_ID = %s
            AND qv.SCALE_TYPE = 'PHQ-9' AND qv.QUESTION_ID = 'Q9'
            AND qv.RESPONSE_VALUE > 1
            AND cs.CHECKIN_DATE >= DATEADD(day, -7, CURRENT_DATE)
            LIMIT 1
        """, (patient_id,)),
    ]
    
    results = await asyncio.gather(*tasks)
    latest, weekly, vitals, flag = results
    
    return {
        "patient_id": patient_id,
        "latest": latest[0] if latest else None,
        "weekly_trend": weekly,
        "question_vitals": vitals,
        "flags": ["PHQ-9 Q9 elevated - discuss in session"] if flag else [],
    }


@router.get("/patients/{patient_id}/trends")
async def get_trends(
    patient_id: int
):
    """Get session trends with LAG() calculation."""
    trends = await get_session_trends(patient_id)
    return {
        "patient_id": patient_id,
        "trends": trends
    }


@router.get("/patients/{patient_id}/question-trends")
async def get_question_trends(
    patient_id: int,
    user: dict = Depends(get_current_user)
):
    """Get per-question HRV trends."""
    trends = await get_question_hrv_trends(patient_id)
    return {
        "patient_id": patient_id,
        "question_trends": trends
    }


@router.get("/patients/{patient_id}/sparkline/{question_id}")
async def get_sparkline(
    patient_id: int,
    question_id: str,
    db: SnowflakeDB = Depends(get_db),
    user: dict = Depends(get_current_user)
):
    """Get HRV sparkline for specific question over time."""
    rows = await db.query("""
        SELECT CAPTURED_AT, HRV_AT_QUESTION, RESPONSE_VALUE
        FROM CADENCE.PUBLIC.QUESTION_VITALS
        WHERE PATIENT_ID = %s AND QUESTION_ID = %s
        ORDER BY CAPTURED_AT ASC
    """, (patient_id, question_id))
    
    return {
        "patient_id": patient_id,
        "question_id": question_id,
        "readings": rows
    }
