"""Therapist dashboard routes with trends."""
import asyncio
from fastapi import APIRouter, Depends, HTTPException, Query
from fastapi.security import HTTPBearer
from app.core.database import get_db, SnowflakeDB
from app.core.auth import get_current_user
from app.services.embedding_pipeline import (
    get_session_trends,
    get_question_hrv_trends,
    get_similar_sessions,
    get_latest_session,
)

security = HTTPBearer()

router = APIRouter(
    prefix="/dashboard",
    tags=["dashboard"],
    # dependencies=[Depends(security)]
)


@router.get("/therapists/{therapist_id}/patients")
async def get_patients(
    therapist_id: int,
    db: SnowflakeDB = Depends(get_db)
):
    """Get therapist's patients with check-in count and latest score."""
    rows = await db.query("""
        SELECT 
            p.PATIENT_ID, p.NAME, p.EMAIL, p.ASSIGNED_SCALE,
            (SELECT COUNT(*) FROM CADENCE.PUBLIC.CHECKIN_SESSIONS cs
             WHERE cs.PATIENT_ID = p.PATIENT_ID) AS CHECKIN_COUNT,
            (SELECT cs.SCALE_SCORE FROM CADENCE.PUBLIC.CHECKIN_SESSIONS cs
             WHERE cs.PATIENT_ID = p.PATIENT_ID
             ORDER BY cs.CHECKIN_DATE DESC
             LIMIT 1) AS LATEST_SCORE,
            (SELECT cs.CHECKIN_DATE FROM CADENCE.PUBLIC.CHECKIN_SESSIONS cs
             WHERE cs.PATIENT_ID = p.PATIENT_ID
             ORDER BY cs.CHECKIN_DATE DESC
             LIMIT 1) AS LATEST_CHECKIN
        FROM CADENCE.PUBLIC.PATIENTS p
        WHERE p.THERAPIST_ID = %s
    """, (therapist_id,))
    
    return {"patients": rows}


@router.get("/patients/{patient_id}/brief")
async def get_brief(
    patient_id: int,
    db: SnowflakeDB = Depends(get_db),
    # user: dict = Depends(get_current_user)
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
    # user: dict = Depends(get_current_user)
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
    # user: dict = Depends(get_current_user)
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


@router.get("/patients/{patient_id}/similar-sessions")
async def get_patient_similar_sessions(
    patient_id: int,
    session_id: int = Query(None, description="Session to compare against. Uses latest if omitted."),
    limit: int = Query(20, ge=1, le=50),
    # user: dict = Depends(get_current_user)
):
    """
    Find historically similar sessions based on situation text embeddings.
    Returns similar sessions sorted by cosine similarity, with dates and scores
    suitable for time-series graphing on the therapist dashboard.
    """
    anchor_session_id = session_id
    if anchor_session_id is None:
        latest = await get_latest_session(patient_id)
        if not latest:
            raise HTTPException(status_code=404, detail="No sessions found for patient")
        anchor_session_id = latest["SESSION_ID"]
        if not latest.get("HAS_EMBEDDING"):
            raise HTTPException(
                status_code=422,
                detail="Latest session has no embedding yet. Pipeline may still be processing."
            )

    similar = await get_similar_sessions(patient_id, anchor_session_id, limit)

    return {
        "patient_id": patient_id,
        "anchor_session_id": anchor_session_id,
        "similar_sessions": similar,
        "count": len(similar),
    }


@router.get("/sessions/{session_id}/detail")
async def get_session_detail(
    session_id: int,
    db: SnowflakeDB = Depends(get_db),
    # user: dict = Depends(get_current_user)
):
    """Get full detail for a single session (day view)."""
    session = await db.fetch_one("""
        SELECT 
            SESSION_ID, PATIENT_ID, CHECKIN_DATE, SCALE_TYPE, SCALE_SCORE,
            HRV_VALUE, BREATHING_RATE, PULSE_RATE,
            DISTRESS_RATING, SITUATION_TEXT, COPING_TEXT
        FROM CADENCE.PUBLIC.CHECKIN_SESSIONS
        WHERE SESSION_ID = %s
    """, (session_id,))

    if not session:
        raise HTTPException(status_code=404, detail="Session not found")

    questions = await db.query("""
        SELECT QUESTION_ID, QUESTION_TEXT, RESPONSE_VALUE, HRV_AT_QUESTION,
               IS_VITALS_CORRELATED, SCALE_TYPE
        FROM CADENCE.PUBLIC.QUESTION_VITALS
        WHERE SESSION_ID = %s
        ORDER BY QUESTION_ID
    """, (session_id,))

    return {
        "session": session,
        "questions": questions,
    }
