"""Embedding pipeline using Snowflake Cortex."""
import logging
from app.core.database import SnowflakeDB

logger = logging.getLogger(__name__)


async def process_checkin_pipeline(session_id: int, patient_id: int) -> bool:
    """
    Generate embedding for a check-in's situation text using snowflake-arctic-embed-m-v1.5.
    Returns True on success, False on failure.
    """
    db = SnowflakeDB()
    
    try:
        logger.info(f"[Pipeline] Generating embedding for session {session_id}")
        
        await db.execute("""
            UPDATE CADENCE.PUBLIC.CHECKIN_SESSIONS
            SET SITUATION_VECTOR = SNOWFLAKE.CORTEX.EMBED_TEXT_768(
                'snowflake-arctic-embed-m-v1.5', 
                SITUATION_TEXT
            )
            WHERE SESSION_ID = %s 
              AND SITUATION_TEXT IS NOT NULL
              AND SITUATION_VECTOR IS NULL
        """, (session_id,))
        
        logger.info(f"[Pipeline] Embedding generated for session {session_id}")
        return True
        
    except Exception as e:
        logger.error(f"[Pipeline] Failed for session {session_id}: {e}")
        return False


async def get_session_trends(patient_id: int) -> list:
    """Compute session-level trends using LAG() window function."""
    db = SnowflakeDB()
    
    rows = await db.query("""
        SELECT 
            SESSION_ID,
            PATIENT_ID, 
            CHECKIN_DATE,
            HRV_VALUE,
            BREATHING_RATE,
            SCALE_SCORE,
            HRV_VALUE - LAG(HRV_VALUE) OVER (
                PARTITION BY PATIENT_ID ORDER BY CHECKIN_DATE
            ) AS HRV_TREND,
            BREATHING_RATE - LAG(BREATHING_RATE) OVER (
                PARTITION BY PATIENT_ID ORDER BY CHECKIN_DATE
            ) AS BREATHING_TREND,
            SCALE_SCORE - LAG(SCALE_SCORE) OVER (
                PARTITION BY PATIENT_ID ORDER BY CHECKIN_DATE
            ) AS SCORE_TREND
        FROM CADENCE.PUBLIC.CHECKIN_SESSIONS
        WHERE PATIENT_ID = %s
        ORDER BY CHECKIN_DATE
    """, (patient_id,))
    
    return rows


async def get_question_hrv_trends(patient_id: int) -> list:
    """Compute per-question HRV trends using LAG() window function."""
    db = SnowflakeDB()
    
    rows = await db.query("""
        SELECT 
            qv.PATIENT_ID,
            qv.QUESTION_ID,
            qv.CAPTURED_AT,
            qv.HRV_AT_QUESTION,
            qv.HRV_AT_QUESTION - LAG(qv.HRV_AT_QUESTION) OVER (
                PARTITION BY qv.PATIENT_ID, qv.QUESTION_ID 
                ORDER BY qv.CAPTURED_AT
            ) AS QUESTION_HRV_TREND
        FROM CADENCE.PUBLIC.QUESTION_VITALS qv
        WHERE qv.PATIENT_ID = %s 
          AND qv.IS_VITALS_CORRELATED = TRUE
        ORDER BY qv.QUESTION_ID, qv.CAPTURED_AT
    """, (patient_id,))
    
    return rows


async def get_similar_sessions(patient_id: int, session_id: int, limit: int = 20) -> list:
    """
    Find historically similar sessions using Snowflake Cortex vector cosine similarity.
    Compares the given session's SITUATION_VECTOR against all other sessions for the patient.
    Returns sessions ordered by similarity descending, with scores and dates for graphing.
    """
    db = SnowflakeDB()

    rows = await db.query("""
        SELECT 
            cs2.SESSION_ID,
            cs2.CHECKIN_DATE,
            cs2.SCALE_SCORE,
            cs2.HRV_VALUE,
            cs2.BREATHING_RATE,
            cs2.DISTRESS_RATING,
            cs2.SITUATION_TEXT,
            cs2.COPING_TEXT,
            cs2.SCALE_TYPE,
            VECTOR_COSINE_SIMILARITY(cs1.SITUATION_VECTOR, cs2.SITUATION_VECTOR) AS SIMILARITY
        FROM CADENCE.PUBLIC.CHECKIN_SESSIONS cs1
        JOIN CADENCE.PUBLIC.CHECKIN_SESSIONS cs2
            ON cs2.PATIENT_ID = cs1.PATIENT_ID
            AND cs2.SESSION_ID != cs1.SESSION_ID
            AND cs2.SITUATION_VECTOR IS NOT NULL
        WHERE cs1.SESSION_ID = %s
            AND cs1.PATIENT_ID = %s
            AND cs1.SITUATION_VECTOR IS NOT NULL
        ORDER BY SIMILARITY DESC
        LIMIT %s
    """, (session_id, patient_id, limit))

    return rows


async def get_latest_session(patient_id: int) -> dict:
    """Get the most recent session for a patient."""
    db = SnowflakeDB()

    row = await db.query("""
        SELECT 
            SESSION_ID, CHECKIN_DATE, SCALE_TYPE, SCALE_SCORE,
            HRV_VALUE, BREATHING_RATE, DISTRESS_RATING,
            SITUATION_TEXT, COPING_TEXT,
            SITUATION_VECTOR IS NOT NULL AS HAS_EMBEDDING
        FROM CADENCE.PUBLIC.CHECKIN_SESSIONS
        WHERE PATIENT_ID = %s
        ORDER BY CHECKIN_DATE DESC
        LIMIT 1
    """, (patient_id,))

    return row[0] if row else None
