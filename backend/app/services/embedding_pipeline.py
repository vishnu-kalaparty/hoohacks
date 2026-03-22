"""Embedding and clustering pipeline using Snowflake Cortex."""
import logging
from typing import Optional
from app.core.database import SnowflakeDB

logger = logging.getLogger(__name__)


async def process_checkin_pipeline(session_id: int, patient_id: int) -> Optional[str]:
    """
    Complete pipeline:
    1. Generate embedding using snowflake-arctic-embed-m-v1.5
    2. Find best matching cluster (cosine similarity > 0.82)
    3. Assign cluster label
    
    Returns: assigned cluster label or None
    """
    db = SnowflakeDB()
    
    try:
        # Step 1: Generate embedding
        logger.info(f"[Pipeline] Step 1: Generating embedding for session {session_id}")
        
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
        
        # Step 2: Find best matching cluster
        logger.info(f"[Pipeline] Step 2: Finding similar cluster for session {session_id}")
        
        similar = await db.fetch_one("""
            SELECT 
                cs2.SITUATION_CLUSTER,
                VECTOR_COSINE_SIMILARITY(cs1.SITUATION_VECTOR, cs2.SITUATION_VECTOR) AS SIM
            FROM CADENCE.PUBLIC.CADENCE.PUBLIC.CHECKIN_SESSIONS cs1
            JOIN CADENCE.PUBLIC.CHECKIN_SESSIONS cs2 
              ON cs1.PATIENT_ID = cs2.PATIENT_ID 
              AND cs2.SESSION_ID != cs1.SESSION_ID
            WHERE cs1.SESSION_ID = %s
              AND cs2.SITUATION_VECTOR IS NOT NULL
              AND cs2.SITUATION_CLUSTER IS NOT NULL
            ORDER BY SIM DESC
            LIMIT 1
        """, (session_id,))
        
        # Step 3: Assign cluster
        if similar and similar.get("SIM", 0) > 0.82:
            cluster_label = similar["SITUATION_CLUSTER"]
            logger.info(f"[Pipeline] Matched existing cluster: {cluster_label} (sim: {similar['SIM']:.3f})")
        else:
            # Create new cluster
            from datetime import date
            cluster_label = f"cluster_{patient_id}_{date.today().isoformat()}"
            sim_str = f"{similar['SIM']:.3f}" if similar else "N/A"
            logger.info(f"[Pipeline] Creating new cluster: {cluster_label} (best match: {sim_str})")
        
        # Update session with cluster
        await db.execute("""
            UPDATE CADENCE.PUBLIC.CHECKIN_SESSIONS
            SET SITUATION_CLUSTER = %s
            WHERE SESSION_ID = %s
        """, (cluster_label, session_id))
        
        logger.info(f"[Pipeline] Complete: session {session_id} -> cluster {cluster_label}")
        return cluster_label
        
    except Exception as e:
        logger.error(f"[Pipeline] Failed for session {session_id}: {e}")
        return None


async def get_session_trends(patient_id: int) -> list:
    """
    Compute session-level trends using LAG() window function.
    Returns: list of trends per session
    """
    db = SnowflakeDB()
    
    rows = await db.query("""
        SELECT 
            SESSION_ID,
            PATIENT_ID, 
            SITUATION_CLUSTER, 
            CHECKIN_DATE,
            HRV_VALUE,
            BREATHING_RATE,
            SCALE_SCORE,
            HRV_VALUE - LAG(HRV_VALUE) OVER (
                PARTITION BY PATIENT_ID, SITUATION_CLUSTER 
                ORDER BY CHECKIN_DATE
            ) AS HRV_TREND,
            BREATHING_RATE - LAG(BREATHING_RATE) OVER (
                PARTITION BY PATIENT_ID, SITUATION_CLUSTER 
                ORDER BY CHECKIN_DATE
            ) AS BREATHING_TREND,
            SCALE_SCORE - LAG(SCALE_SCORE) OVER (
                PARTITION BY PATIENT_ID, SITUATION_CLUSTER 
                ORDER BY CHECKIN_DATE
            ) AS SCORE_TREND
        FROM CADENCE.PUBLIC.CHECKIN_SESSIONS
        WHERE PATIENT_ID = %s
        ORDER BY SITUATION_CLUSTER, CHECKIN_DATE
    """, (patient_id,))
    
    return rows


async def get_question_hrv_trends(patient_id: int) -> list:
    """
    Compute per-question HRV trends using LAG() window function.
    Returns: list of question-level trends
    """
    db = SnowflakeDB()
    
    rows = await db.query("""
        SELECT 
            qv.PATIENT_ID,
            cs.SITUATION_CLUSTER,
            qv.QUESTION_ID,
            qv.CAPTURED_AT,
            qv.HRV_AT_QUESTION,
            qv.HRV_AT_QUESTION - LAG(qv.HRV_AT_QUESTION) OVER (
                PARTITION BY qv.PATIENT_ID, qv.QUESTION_ID 
                ORDER BY qv.CAPTURED_AT
            ) AS QUESTION_HRV_TREND
        FROM CADENCE.PUBLIC.QUESTION_VITALS qv
        JOIN CADENCE.PUBLIC.CHECKIN_SESSIONS cs 
          ON qv.SESSION_ID = cs.SESSION_ID
        WHERE qv.PATIENT_ID = %s 
          AND qv.IS_VITALS_CORRELATED = TRUE
        ORDER BY qv.QUESTION_ID, qv.CAPTURED_AT
    """, (patient_id,))
    
    return rows


async def get_cluster_comparison(patient_id: int, cluster_label: str) -> dict:
    """
    Compare earliest vs latest event in a cluster.
    """
    db = SnowflakeDB()
    
    result = await db.fetch_one("""
        SELECT 
            MIN(CHECKIN_DATE) as EARLIEST_DATE,
            MAX(CHECKIN_DATE) as LATEST_DATE,
            MIN_BY(HRV_VALUE, CHECKIN_DATE) as EARLIEST_HRV,
            MAX_BY(HRV_VALUE, CHECKIN_DATE) as LATEST_HRV,
            MIN_BY(SCALE_SCORE, CHECKIN_DATE) as EARLIEST_SCORE,
            MAX_BY(SCALE_SCORE, CHECKIN_DATE) as LATEST_SCORE
        FROM CADENCE.PUBLIC.CHECKIN_SESSIONS
        WHERE PATIENT_ID = %s AND SITUATION_CLUSTER = %s
    """, (patient_id, cluster_label))
    
    if not result or not result.get("EARLIEST_DATE"):
        return None
    
    # Calculate percentage changes
    earliest_hrv = result.get("EARLIEST_HRV", 0)
    latest_hrv = result.get("LATEST_HRV", 0)
    earliest_score = result.get("EARLIEST_SCORE", 0)
    latest_score = result.get("LATEST_SCORE", 0)
    
    hrv_change_pct = ((latest_hrv - earliest_hrv) / earliest_hrv * 100) if earliest_hrv else 0
    score_change_pct = ((latest_score - earliest_score) / earliest_score * 100) if earliest_score else 0
    
    return {
        "cluster_label": cluster_label,
        "earliest_date": result["EARLIEST_DATE"],
        "latest_date": result["LATEST_DATE"],
        "hrv_change_pct": round(hrv_change_pct, 1),
        "score_change_pct": round(score_change_pct, 1),
        "days_between": (result["LATEST_DATE"] - result["EARLIEST_DATE"]).days if result["LATEST_DATE"] else 0
    }
