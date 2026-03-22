"""Patient routes."""
from fastapi import APIRouter, HTTPException, Depends
from app.core.database import get_db, SnowflakeDB
from app.core.auth import get_current_user

router = APIRouter(prefix="/patients", tags=["patients"])

@router.get("/get-questions/{patient_id}")
async def get_patient_questions(
    patient_id: int,
    db: SnowflakeDB = Depends(get_db),
    user: dict = Depends(get_current_user)
):
    """Get all questions for patient based on their assigned scale type.
    
    First gets the patient's ASSIGNED_SCALE (PHQ-9 or GAD-7),
    then returns all questions for that scale type.
    """
    # Get patient's assigned scale type
    patient_result = await db.query("""
        SELECT PATIENT_ID, NAME, ASSIGNED_SCALE
        FROM CADENCE.PUBLIC.PATIENTS WHERE PATIENT_ID = %s
    """, (patient_id,))
    
    if not patient_result:
        raise HTTPException(status_code=404, detail="Patient not found")
    
    patient = patient_result[0]
    scale_type = patient["ASSIGNED_SCALE"]
    
    # Get all questions for the patient's scale type
    questions = await db.query("""
        SELECT QUESTION_ID, QUESTION_TEXT, IS_VITALS_CORRELATED, CLINICAL_CONSTRUCT
        FROM CADENCE.PUBLIC.SCALE_QUESTIONS
        WHERE SCALE_TYPE = %s
        ORDER BY QUESTION_ID
    """, (scale_type,))
    
    return {
        "patient_id": patient_id,
        "patient_name": patient["NAME"],
        "scale_type": scale_type,
        "questions": questions
    }

@router.get("/{patient_id}")
async def get_patient(
    patient_id: int,
    db: SnowflakeDB = Depends(get_db),
    user: dict = Depends(get_current_user)
):
    """Get patient info."""
    result = await db.query("""
        SELECT PATIENT_ID, NAME, EMAIL, ASSIGNED_SCALE, THERAPIST_ID, APPOINTMENT_DAY, TIMEZONE
        FROM CADENCE.PUBLIC.PATIENTS WHERE PATIENT_ID = %s
    """, (patient_id,))
    
    if not result:
        raise HTTPException(status_code=404, detail="Patient not found")
    
    return result[0]


@router.get("/{patient_id}/history")
async def get_history(
    patient_id: int,
    limit: int = 10,
    db: SnowflakeDB = Depends(get_db)
):
    """Get check-in history."""
    rows = await db.query("""
        SELECT SESSION_ID, CHECKIN_DATE, SCALE_TYPE, SCALE_SCORE,
               HRV_VALUE, BREATHING_RATE, PULSE_RATE, DISTRESS_RATING,
               SITUATION_TEXT, COPING_TEXT, COMPLETED_AT
        FROM CADENCE.PUBLIC.CHECKIN_SESSIONS
        WHERE PATIENT_ID = %s
        ORDER BY CHECKIN_DATE DESC
        LIMIT %s
    """, (patient_id, limit))
    
    return {"patient_id": patient_id, "checkins": rows}


@router.get("/{patient_id}/next-checkin")
async def get_next_checkin(
    patient_id: int,
    db: SnowflakeDB = Depends(get_db),
    user: dict = Depends(get_current_user)
):
    """Get next scheduled checkin."""
    row = await db.query("""
        SELECT SCHEDULE_ID, SCHEDULED_DATE
        FROM CADENCE.PUBLIC.CHECKIN_SCHEDULE
        WHERE PATIENT_ID = %s AND COMPLETED = FALSE
        ORDER BY SCHEDULED_DATE ASC
        LIMIT 1
    """, (patient_id,))
    
    is_today = False
    if row:
        from datetime import date
        is_today = row[0]["SCHEDULED_DATE"] == date.today()
    
    return {
        "next_checkin": row[0] if row else None,
        "is_checkin_day": is_today
    }
