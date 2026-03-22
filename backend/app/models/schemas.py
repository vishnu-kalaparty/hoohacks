"""Pydantic schemas matching CADENCE database."""
from pydantic import BaseModel
from typing import List, Optional
from datetime import date


class QuestionResponse(BaseModel):
    question_id: str  # VARCHAR: Q1, Q2, etc.
    response: int  # 0-3
    hrv_at_question: Optional[float] = None


class CheckinSubmit(BaseModel):
    patient_id: int
    scale_type: str  # PHQ-9 or GAD-7
    scale_score: int
    hrv: float
    breathing_rate: float
    pulse_rate: float
    distress: int  # 1-10
    situation: str
    coping: str
    questions: List[QuestionResponse]


class Patient(BaseModel):
    patient_id: int
    name: str
    email: str
    assigned_scale: str
    therapist_id: int
    appointment_day: Optional[str] = None
    timezone: str = "UTC"


class ScaleQuestion(BaseModel):
    scale_type: str
    question_id: str  # VARCHAR: Q1, Q2, etc.
    question_text: str
    is_vitals_correlated: bool
    clinical_construct: Optional[str] = None


class TherapistScheduleInsert(BaseModel):
    """Therapist assigns future check-in due dates (CHECKIN_SCHEDULE rows)."""
    patient_id: int
    scheduled_dates: List[date]
