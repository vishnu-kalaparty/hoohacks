"""Check-in routes with embedding pipeline."""
from fastapi import APIRouter, HTTPException, BackgroundTasks, Depends
from app.core.database import get_db, SnowflakeDB
from app.core.auth import get_current_user
from fastapi.security import HTTPBearer

security = HTTPBearer()
from app.models.schemas import CheckinSubmit
from app.services.embedding_pipeline import process_checkin_pipeline

router = APIRouter(
    prefix="/checkins",
    tags=["checkins"],
    dependencies=[Depends(security)]
)


@router.get("/scale-questions/{scale_type}")
async def get_questions(
    scale_type: str,
    db: SnowflakeDB = Depends(get_db)
):
    """Get scale questions (PHQ-9 or GAD-7)."""
    rows = await db.query("""
        SELECT QUESTION_ID, QUESTION_TEXT, IS_VITALS_CORRELATED, CLINICAL_CONSTRUCT
        FROM CADENCE.PUBLIC.SCALE_QUESTIONS
        WHERE SCALE_TYPE = %s
        ORDER BY QUESTION_ID
    """, (scale_type,))
    
    return {"scale_type": scale_type, "questions": rows}


@router.post("/")
async def submit_checkin(
    data: CheckinSubmit,
    background_tasks: BackgroundTasks,
    db: SnowflakeDB = Depends(get_db)
):
    """
    Submit check-in.
    Embedding pipeline runs in background after submission.
    """
    try:
        # 1. Insert session
        await db.execute("""
            INSERT INTO CADENCE.PUBLIC.CHECKIN_SESSIONS 
            (PATIENT_ID, CHECKIN_DATE, SCALE_TYPE, SCALE_SCORE, 
             HRV_VALUE, BREATHING_RATE, PULSE_RATE, 
             DISTRESS_RATING, SITUATION_TEXT, COPING_TEXT)
            VALUES (%s, CURRENT_DATE, %s, %s, %s, %s, %s, %s, %s, %s)
        """, (
            data.patient_id, data.scale_type, data.scale_score,
            data.hrv, data.breathing_rate, data.pulse_rate,
            data.distress, data.situation, data.coping
        ))
        
        # Get session_id
        result = await db.fetch_one("""
            SELECT SESSION_ID FROM CADENCE.PUBLIC.CHECKIN_SESSIONS 
            WHERE PATIENT_ID = %s AND CHECKIN_DATE = CURRENT_DATE
            ORDER BY COMPLETED_AT DESC LIMIT 1
        """, (data.patient_id,))
        
        if not result:
            raise HTTPException(status_code=500, detail="Failed to get session_id")
            
        session_id = result["SESSION_ID"]
        
        # 2. Insert question vitals
        for q in data.questions:
            q_info = await db.fetch_one("""
                SELECT QUESTION_TEXT, IS_VITALS_CORRELATED 
                FROM CADENCE.PUBLIC.SCALE_QUESTIONS 
                WHERE SCALE_TYPE = %s AND QUESTION_ID = %s
            """, (data.scale_type, q.question_id))
            
            if q_info:
                await db.execute("""
                    INSERT INTO CADENCE.PUBLIC.QUESTION_VITALS
                    (SESSION_ID, PATIENT_ID, QUESTION_ID, QUESTION_TEXT, SCALE_TYPE,
                     RESPONSE_VALUE, HRV_AT_QUESTION, IS_VITALS_CORRELATED, CAPTURED_AT)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, CURRENT_TIMESTAMP)
                """, (
                    session_id, data.patient_id, q.question_id, 
                    q_info["QUESTION_TEXT"], data.scale_type,
                    q.response, q.hrv_at_question, q_info["IS_VITALS_CORRELATED"]
                ))
        
        # 3. Mark schedule complete
        await db.execute("""
            UPDATE CADENCE.PUBLIC.CHECKIN_SCHEDULE 
            SET COMPLETED = TRUE, COMPLETED_AT = CURRENT_TIMESTAMP
            WHERE PATIENT_ID = %s AND SCHEDULED_DATE = CURRENT_DATE
        """, (data.patient_id,))
        
        # 4. Trigger embedding pipeline (BACKGROUND TASK)
        background_tasks.add_task(
            process_checkin_pipeline, 
            session_id=session_id, 
            patient_id=data.patient_id
        )
        
        return {
            "success": True, 
            "session_id": session_id,
            "message": "Check-in submitted. Embedding pipeline running in background.",
            "pipeline_status": "processing"
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Submit failed: {str(e)}")


@router.get("/{session_id}/status")
async def get_checkin_status(
    session_id: int,
    db: SnowflakeDB = Depends(get_db)
):
    """Get check-in processing status (embedding & cluster)."""
    result = await db.fetch_one("""
        SELECT 
            SESSION_ID,
            SITUATION_TEXT IS NOT NULL AS HAS_TEXT,
            SITUATION_VECTOR IS NOT NULL AS HAS_EMBEDDING,
            SITUATION_CLUSTER IS NOT NULL AS HAS_CLUSTER,
            SITUATION_CLUSTER
        FROM CADENCE.PUBLIC.CHECKIN_SESSIONS
        WHERE SESSION_ID = %s
    """, (session_id,))
    
    if not result:
        raise HTTPException(status_code=404, detail="Session not found")
    
    return {
        "session_id": session_id,
        "has_situation_text": result["HAS_TEXT"],
        "has_embedding": result["HAS_EMBEDDING"],
        "has_cluster": result["HAS_CLUSTER"],
        "cluster_label": result["SITUATION_CLUSTER"],
        "pipeline_complete": result["HAS_EMBEDDING"] and result["HAS_CLUSTER"]
    }


@router.post("/{session_id}/run-pipeline")
async def manual_run_pipeline(
    session_id: int,
    db: SnowflakeDB = Depends(get_db),
    user: dict = Depends(get_current_user)
):
    """
    Manually trigger embedding pipeline (for retry or admin).
    """
    # Get patient_id
    result = await db.fetch_one("""
        SELECT PATIENT_ID FROM CADENCE.PUBLIC.CHECKIN_SESSIONS WHERE SESSION_ID = %s
    """, (session_id,))
    
    if not result:
        raise HTTPException(status_code=404, detail="Session not found")
    
    patient_id = result["PATIENT_ID"]
    
    # Run pipeline
    cluster_label = await process_checkin_pipeline(session_id, patient_id)
    
    if cluster_label:
        return {
            "success": True,
            "session_id": session_id,
            "cluster_label": cluster_label
        }
    else:
        raise HTTPException(status_code=500, detail="Pipeline failed")
