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
    q_ids = list(dict.fromkeys(q.question_id for q in data.questions))
    question_map = {}
    if q_ids:
        ph = ",".join(["%s"] * len(q_ids))
        rows = await db.query(
            f"""
            SELECT QUESTION_ID, QUESTION_TEXT, IS_VITALS_CORRELATED
            FROM CADENCE.PUBLIC.SCALE_QUESTIONS
            WHERE SCALE_TYPE = %s AND QUESTION_ID IN ({ph})
            """,
            (data.scale_type, *q_ids),
        )
        question_map = {r["QUESTION_ID"]: r for r in rows}

    insert_session = """
        INSERT INTO CADENCE.PUBLIC.CHECKIN_SESSIONS
        (PATIENT_ID, CHECKIN_DATE, SCALE_TYPE, SCALE_SCORE,
         HRV_VALUE, BREATHING_RATE, PULSE_RATE,
         DISTRESS_RATING, SITUATION_TEXT, COPING_TEXT)
        VALUES (%s, CURRENT_DATE, %s, %s, %s, %s, %s, %s, %s, %s)
    """
    select_session = """
        SELECT SESSION_ID FROM CADENCE.PUBLIC.CHECKIN_SESSIONS
        WHERE PATIENT_ID = %s AND CHECKIN_DATE = CURRENT_DATE
        ORDER BY COMPLETED_AT DESC LIMIT 1
    """
    insert_vital = """
        INSERT INTO CADENCE.PUBLIC.QUESTION_VITALS
        (SESSION_ID, PATIENT_ID, QUESTION_ID, QUESTION_TEXT, SCALE_TYPE,
         RESPONSE_VALUE, HRV_AT_QUESTION, IS_VITALS_CORRELATED, CAPTURED_AT)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, CURRENT_TIMESTAMP)
    """
    update_schedule = """
        UPDATE CADENCE.PUBLIC.CHECKIN_SCHEDULE
        SET COMPLETED = TRUE, COMPLETED_AT = CURRENT_TIMESTAMP
        WHERE PATIENT_ID = %s AND SCHEDULED_DATE = CURRENT_DATE
    """
    session_params = (
        data.patient_id,
        data.scale_type,
        data.scale_score,
        data.hrv,
        data.breathing_rate,
        data.pulse_rate,
        data.distress,
        data.situation,
        data.coping,
    )

    def write(conn):
        cur = conn.cursor()
        try:
            cur.execute(insert_session, session_params)
            cur.execute(select_session, (data.patient_id,))
            row = cur.fetchone()
            if not row:
                raise HTTPException(status_code=500, detail="Failed to get session_id")
            cols = [d[0] for d in cur.description]
            session_id = dict(zip(cols, row))["SESSION_ID"]

            vitals_batch = []
            for q in data.questions:
                info = question_map.get(q.question_id)
                if not info:
                    continue
                vitals_batch.append(
                    (
                        session_id,
                        data.patient_id,
                        q.question_id,
                        info["QUESTION_TEXT"],
                        data.scale_type,
                        q.response,
                        q.hrv_at_question,
                        info["IS_VITALS_CORRELATED"],
                    )
                )
            if vitals_batch:
                cur.executemany(insert_vital, vitals_batch)
            cur.execute(update_schedule, (data.patient_id,))
            return session_id
        finally:
            cur.close()

    try:
        session_id = await db.run_transaction(write)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Submit failed: {str(e)}")

    background_tasks.add_task(
        process_checkin_pipeline,
        session_id=session_id,
        patient_id=data.patient_id,
    )

    return {
        "success": True,
        "session_id": session_id,
        "message": "Check-in submitted. Embedding pipeline running in background.",
        "pipeline_status": "processing",
    }


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
