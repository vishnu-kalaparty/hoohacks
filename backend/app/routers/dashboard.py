"""Therapist dashboard routes with trends and clustering."""
import asyncio
from fastapi import APIRouter, Depends, HTTPException
from app.core.database import get_db, SnowflakeDB
from app.services.embedding_pipeline import (
    get_session_trends, 
    get_question_hrv_trends,
    get_cluster_comparison
)

router = APIRouter(prefix="/dashboard", tags=["dashboard"])


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
    db: SnowflakeDB = Depends(get_db)
):
    """Get complete patient brief for therapist dashboard."""
    # Run queries concurrently
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
        
        db.query("""
            SELECT DISTINCT SITUATION_CLUSTER
            FROM CADENCE.PUBLIC.CHECKIN_SESSIONS
            WHERE PATIENT_ID = %s AND SITUATION_CLUSTER IS NOT NULL
        """, (patient_id,))
    ]
    
    results = await asyncio.gather(*tasks)
    latest, weekly, vitals, flag, clusters = results
    
    # Get cluster comparisons
    cluster_comparisons = []
    for cluster_row in clusters:
        cluster_label = cluster_row["SITUATION_CLUSTER"]
        comparison = await get_cluster_comparison(patient_id, cluster_label)
        if comparison:
            cluster_comparisons.append(comparison)
    
    return {
        "patient_id": patient_id,
        "latest": latest[0] if latest else None,
        "weekly_trend": weekly,
        "question_vitals": vitals,
        "flags": ["PHQ-9 Q9 elevated - discuss in session"] if flag else [],
        "clusters": [c["cluster_label"] for c in clusters],
        "cluster_comparisons": cluster_comparisons
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
    patient_id: int
):
    """Get per-question HRV trends."""
    trends = await get_question_hrv_trends(patient_id)
    return {
        "patient_id": patient_id,
        "question_trends": trends
    }


@router.get("/patients/{patient_id}/cluster/{cluster_label}/comparison")
async def get_cluster_compare(
    patient_id: int,
    cluster_label: str
):
    """Get earliest vs latest comparison for a cluster."""
    comparison = await get_cluster_comparison(patient_id, cluster_label)
    
    if not comparison:
        raise HTTPException(status_code=404, detail="Cluster not found or no data")
    
    return comparison


@router.get("/patients/{patient_id}/sparkline/{question_id}")
async def get_sparkline(
    patient_id: int,
    question_id: str,
    db: SnowflakeDB = Depends(get_db)
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


@router.post("/patients/{patient_id}/confirm-cluster")
async def confirm_cluster(
    patient_id: int,
    therapist_id: int,
    cluster_label: str,
    direction: str,  # better, worse, different
    db: SnowflakeDB = Depends(get_db)
):
    """Therapist confirms cluster comparison direction."""
    await db.execute("""
        INSERT INTO CADENCE.PUBLIC.THERAPIST_CONFIRMATIONS
        (THERAPIST_ID, PATIENT_ID, CLUSTER_LABEL, COMPARISON_DIRECTION)
        VALUES (%s, %s, %s, %s)
    """, (therapist_id, patient_id, cluster_label, direction))
    
    return {"success": True, "message": f"Confirmed {direction} for {cluster_label}"}
