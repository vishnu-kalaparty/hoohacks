package com.example.cadence.api

import com.google.gson.annotations.SerializedName

// ─── Auth ────────────────────────────────────────────────────────────────────

data class AuthConfigResponse(
    @SerializedName("domain") val domain: String,
    @SerializedName("client_id") val clientId: String,
    @SerializedName("audience") val audience: String
)

data class UserProfile(
    @SerializedName("auth0_user_id") val auth0UserId: String,
    @SerializedName("email") val email: String,
    @SerializedName("name") val name: String,
    @SerializedName("role") val role: String,
    @SerializedName("patient_id") val patientId: Int?,
    @SerializedName("therapist_id") val therapistId: Int?,
    @SerializedName("assigned_scale") val assignedScale: String?
)

data class SyncUserRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("therapist_id") val therapistId: Int? = null,
    @SerializedName("assigned_scale") val assignedScale: String? = "PHQ-9"
)

data class LoginResponse(
    @SerializedName("authenticated") val authenticated: Boolean,
    @SerializedName("needs_registration") val needsRegistration: Boolean,
    @SerializedName("user") val user: UserProfile?,
    @SerializedName("auth0_user_id") val auth0UserId: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("message") val message: String?
)

// ─── Patients ────────────────────────────────────────────────────────────────

data class PatientsResponse(
    @SerializedName("patients") val patients: List<Patient>
)

data class Patient(
    @SerializedName("PATIENT_ID") val patientId: Int,
    @SerializedName("NAME") val name: String,
    @SerializedName("EMAIL") val email: String,
    @SerializedName("ASSIGNED_SCALE") val assignedScale: String,
    @SerializedName("CHECKIN_COUNT") val checkinCount: Int,
    @SerializedName("LATEST_SCORE") val latestScore: Int?,
    @SerializedName("LATEST_CHECKIN") val latestCheckin: String?
)

data class PatientDetail(
    @SerializedName("PATIENT_ID") val patientId: Int,
    @SerializedName("NAME") val name: String,
    @SerializedName("EMAIL") val email: String,
    @SerializedName("ASSIGNED_SCALE") val assignedScale: String,
    @SerializedName("THERAPIST_ID") val therapistId: Int,
    @SerializedName("APPOINTMENT_DAY") val appointmentDay: String?,
    @SerializedName("TIMEZONE") val timezone: String?
)

data class PatientQuestionsResponse(
    @SerializedName("patient_id") val patientId: Int,
    @SerializedName("patient_name") val patientName: String,
    @SerializedName("scale_type") val scaleType: String,
    @SerializedName("questions") val questions: List<ScaleQuestion>
)

data class PatientHistoryResponse(
    @SerializedName("patient_id") val patientId: Int,
    @SerializedName("checkins") val checkins: List<CheckinHistoryItem>
)

data class CheckinHistoryItem(
    @SerializedName("SESSION_ID") val sessionId: Int,
    @SerializedName("CHECKIN_DATE") val checkinDate: String,
    @SerializedName("SCALE_TYPE") val scaleType: String?,
    @SerializedName("SCALE_SCORE") val scaleScore: Int?,
    @SerializedName("HRV_VALUE") val hrvValue: Double?,
    @SerializedName("BREATHING_RATE") val breathingRate: Double?,
    @SerializedName("PULSE_RATE") val pulseRate: Double?,
    @SerializedName("DISTRESS_RATING") val distressRating: Int?,
    @SerializedName("SITUATION_TEXT") val situationText: String?,
    @SerializedName("COPING_TEXT") val copingText: String?,
    @SerializedName("COMPLETED_AT") val completedAt: String?
)

data class NextCheckinResponse(
    @SerializedName("next_checkin") val nextCheckin: ScheduleEntry?,
    @SerializedName("is_checkin_day") val isCheckinDay: Boolean
)

data class ScheduleEntry(
    @SerializedName("SCHEDULE_ID") val scheduleId: Int,
    @SerializedName("SCHEDULED_DATE") val scheduledDate: String
)

// ─── Checkins ────────────────────────────────────────────────────────────────

data class ScaleQuestionsResponse(
    @SerializedName("scale_type") val scaleType: String,
    @SerializedName("questions") val questions: List<ScaleQuestion>
)

data class ScaleQuestion(
    @SerializedName("QUESTION_ID") val questionId: String,
    @SerializedName("QUESTION_TEXT") val questionText: String,
    @SerializedName("IS_VITALS_CORRELATED") val isVitalsCorrelated: Boolean,
    @SerializedName("CLINICAL_CONSTRUCT") val clinicalConstruct: String?
)

data class CheckinSubmitRequest(
    @SerializedName("patient_id") val patientId: Int,
    @SerializedName("scale_type") val scaleType: String,
    @SerializedName("scale_score") val scaleScore: Int,
    @SerializedName("hrv") val hrv: Double,
    @SerializedName("breathing_rate") val breathingRate: Double,
    @SerializedName("pulse_rate") val pulseRate: Double,
    @SerializedName("distress") val distress: Int,
    @SerializedName("situation") val situation: String,
    @SerializedName("coping") val coping: String,
    @SerializedName("questions") val questions: List<QuestionSubmit>
)

data class QuestionSubmit(
    @SerializedName("question_id") val questionId: String,
    @SerializedName("response") val response: Int,
    @SerializedName("hrv_at_question") val hrvAtQuestion: Double? = null
)

data class CheckinSubmitResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("session_id") val sessionId: Int,
    @SerializedName("message") val message: String,
    @SerializedName("pipeline_status") val pipelineStatus: String
)

data class CheckinStatusResponse(
    @SerializedName("session_id") val sessionId: Int,
    @SerializedName("has_situation_text") val hasSituationText: Boolean,
    @SerializedName("has_embedding") val hasEmbedding: Boolean,
    @SerializedName("pipeline_complete") val pipelineComplete: Boolean
)

data class RunPipelineResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("session_id") val sessionId: Int
)

// ─── Dashboard ───────────────────────────────────────────────────────────────

data class PatientBriefResponse(
    @SerializedName("patient_id") val patientId: Int,
    @SerializedName("latest") val latest: Map<String, Any?>?,
    @SerializedName("weekly_trend") val weeklyTrend: List<WeeklyTrendItem>,
    @SerializedName("question_vitals") val questionVitals: List<QuestionVital>,
    @SerializedName("flags") val flags: List<String>
)

data class WeeklyTrendItem(
    @SerializedName("CHECKIN_DATE") val checkinDate: String,
    @SerializedName("SCALE_SCORE") val scaleScore: Int?,
    @SerializedName("HRV_VALUE") val hrvValue: Double?
)

data class SessionTrendsResponse(
    @SerializedName("patient_id") val patientId: Int,
    @SerializedName("trends") val trends: List<SessionTrend>
)

data class SessionTrend(
    @SerializedName("SESSION_ID") val sessionId: Int,
    @SerializedName("PATIENT_ID") val patientId: Int,
    @SerializedName("CHECKIN_DATE") val checkinDate: String,
    @SerializedName("HRV_VALUE") val hrvValue: Double?,
    @SerializedName("BREATHING_RATE") val breathingRate: Double?,
    @SerializedName("SCALE_SCORE") val scaleScore: Int?,
    @SerializedName("HRV_TREND") val hrvTrend: Double?,
    @SerializedName("BREATHING_TREND") val breathingTrend: Double?,
    @SerializedName("SCORE_TREND") val scoreTrend: Int?
)

data class QuestionTrendsResponse(
    @SerializedName("patient_id") val patientId: Int,
    @SerializedName("question_trends") val questionTrends: List<QuestionHrvTrend>
)

data class QuestionHrvTrend(
    @SerializedName("PATIENT_ID") val patientId: Int,
    @SerializedName("QUESTION_ID") val questionId: String,
    @SerializedName("CAPTURED_AT") val capturedAt: String,
    @SerializedName("HRV_AT_QUESTION") val hrvAtQuestion: Double?,
    @SerializedName("QUESTION_HRV_TREND") val questionHrvTrend: Double?
)

data class SparklineResponse(
    @SerializedName("patient_id") val patientId: Int,
    @SerializedName("question_id") val questionId: String,
    @SerializedName("readings") val readings: List<SparklineReading>
)

data class SparklineReading(
    @SerializedName("CAPTURED_AT") val capturedAt: String,
    @SerializedName("HRV_AT_QUESTION") val hrvAtQuestion: Double?,
    @SerializedName("RESPONSE_VALUE") val responseValue: Int?
)

data class SimilarSessionsResponse(
    @SerializedName("patient_id") val patientId: Int,
    @SerializedName("anchor_session_id") val anchorSessionId: Int,
    @SerializedName("similar_sessions") val similarSessions: List<SimilarSession>,
    @SerializedName("count") val count: Int
)

data class SimilarSession(
    @SerializedName("SESSION_ID") val sessionId: Int,
    @SerializedName("CHECKIN_DATE") val checkinDate: String,
    @SerializedName("SCALE_SCORE") val scaleScore: Int,
    @SerializedName("HRV_VALUE") val hrvValue: Double?,
    @SerializedName("BREATHING_RATE") val breathingRate: Double?,
    @SerializedName("DISTRESS_RATING") val distressRating: Int?,
    @SerializedName("SITUATION_TEXT") val situationText: String?,
    @SerializedName("COPING_TEXT") val copingText: String?,
    @SerializedName("SCALE_TYPE") val scaleType: String?,
    @SerializedName("SIMILARITY") val similarity: Double
)

data class SessionDetailResponse(
    @SerializedName("session") val session: SessionDetail,
    @SerializedName("questions") val questions: List<QuestionVital>
)

data class SessionDetail(
    @SerializedName("SESSION_ID") val sessionId: Int,
    @SerializedName("PATIENT_ID") val patientId: Int,
    @SerializedName("CHECKIN_DATE") val checkinDate: String,
    @SerializedName("SCALE_TYPE") val scaleType: String,
    @SerializedName("SCALE_SCORE") val scaleScore: Int,
    @SerializedName("HRV_VALUE") val hrvValue: Double?,
    @SerializedName("BREATHING_RATE") val breathingRate: Double?,
    @SerializedName("PULSE_RATE") val pulseRate: Double?,
    @SerializedName("DISTRESS_RATING") val distressRating: Int?,
    @SerializedName("SITUATION_TEXT") val situationText: String?,
    @SerializedName("COPING_TEXT") val copingText: String?
)

data class QuestionVital(
    @SerializedName("QUESTION_ID") val questionId: String,
    @SerializedName("QUESTION_TEXT") val questionText: String,
    @SerializedName("RESPONSE_VALUE") val responseValue: Int,
    @SerializedName("HRV_AT_QUESTION") val hrvAtQuestion: Double?,
    @SerializedName("IS_VITALS_CORRELATED") val isVitalsCorrelated: Boolean?,
    @SerializedName("SCALE_TYPE") val scaleType: String?
)

// ─── Therapist Schedule ──────────────────────────────────────────────────────

data class ScheduleInsertRequest(
    @SerializedName("patient_id") val patientId: Int,
    @SerializedName("scheduled_dates") val scheduledDates: List<String>
)

data class ScheduleInsertResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("patient_id") val patientId: Int,
    @SerializedName("therapist_id") val therapistId: Int,
    @SerializedName("inserted") val inserted: Int,
    @SerializedName("skipped_existing") val skippedExisting: Int
)
