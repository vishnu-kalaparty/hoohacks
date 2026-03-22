"""Therapist-managed check-in schedule (insert into CHECKIN_SCHEDULE)."""
from fastapi import APIRouter, Depends, HTTPException
from fastapi.security import HTTPBearer
from app.core.database import get_db, SnowflakeDB
from app.models.schemas import TherapistScheduleInsert

security = HTTPBearer()

router = APIRouter(
    prefix="/therapists",
    tags=["therapist-schedule"],
    dependencies=[Depends(security)]
)


@router.post("/{therapist_id}/schedule")
async def insert_schedule(
    therapist_id: int,
    body: TherapistScheduleInsert,
    db: SnowflakeDB = Depends(get_db),
):
    """
    Insert check-in schedule rows for a patient. Patient must belong to this therapist.
    Skips dates that already exist for the patient (idempotent).
    """
    if not body.scheduled_dates:
        raise HTTPException(status_code=400, detail="scheduled_dates must not be empty")

    ok = await db.fetch_one(
        """
        SELECT 1 AS ok
        FROM CADENCE.PUBLIC.PATIENTS
        WHERE PATIENT_ID = %s AND THERAPIST_ID = %s
        """,
        (body.patient_id, therapist_id),
    )
    if not ok:
        raise HTTPException(
            status_code=404,
            detail="Patient not found or not assigned to this therapist",
        )

    dates = list(dict.fromkeys(body.scheduled_dates))  # de-dupe, preserve order
    placeholders = ",".join(["%s"] * len(dates))
    count_sql = f"""
        SELECT COUNT(*) AS cnt
        FROM CADENCE.PUBLIC.CHECKIN_SCHEDULE
        WHERE PATIENT_ID = %s AND SCHEDULED_DATE IN ({placeholders})
    """
    count_params = (body.patient_id, *dates)

    before_row = await db.fetch_one(count_sql, count_params)
    before = before_row["CNT"] if before_row else 0

    for d in dates:
        await db.execute(
            """
            INSERT INTO CADENCE.PUBLIC.CHECKIN_SCHEDULE (PATIENT_ID, SCHEDULED_DATE, COMPLETED)
            SELECT %s, %s, FALSE
            WHERE NOT EXISTS (
                SELECT 1
                FROM CADENCE.PUBLIC.CHECKIN_SCHEDULE cs
                WHERE cs.PATIENT_ID = %s AND cs.SCHEDULED_DATE = %s
            )
            """,
            (body.patient_id, d, body.patient_id, d),
        )

    after_row = await db.fetch_one(count_sql, count_params)
    after = after_row["CNT"] if after_row else 0
    inserted = after - before
    skipped = len(dates) - inserted
    return {
        "success": True,
        "patient_id": body.patient_id,
        "therapist_id": therapist_id,
        "inserted": inserted,
        "skipped_existing": skipped,
    }
