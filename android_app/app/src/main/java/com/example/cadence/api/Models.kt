package com.example.cadence.api

import com.google.gson.annotations.SerializedName

// ── Auth ──────────────────────────────────────────────────────────────

data class AuthConfigResponse(
    val domain: String,
    val client_id: String,
    val audience: String
)

data class LoginResponse(
    val authenticated: Boolean?,
    val needs_registration: Boolean?,
    val user: LoginUser?,
    val auth0_user_id: String?,
    val email: String?,
    val name: String?,
    val message: String?
)

data class LoginUser(
    val auth0_user_id: String?,
    val email: String?,
    val name: String?,
    val role: String?,
    val patient_id: Int?,
    val therapist_id: Int?,
    val assigned_scale: String?
)

data class UserProfileResponse(
    val auth0_user_id: String,
    val email: String,
    val name: String,
    val role: String,
    val patient_id: Int?,
    val therapist_id: Int?,
    val assigned_scale: String?
)

data class SyncUserRequest(
    val name: String?,
    val role: String?,
    val therapist_id: Int?,
    val assigned_scale: String?
)

// ── Patients ──────────────────────────────────────────────────────────

data class PatientListResponse(
    val patients: List<ApiPatient>
)

data class ApiPatient(
    @SerializedName(value = "PATIENT_ID", alternate = ["patient_id"]) val patientId: Int,
    @SerializedName(value = "NAME", alternate = ["name"]) val name: String,
    @SerializedName(value = "EMAIL", alternate = ["email"]) val email: String?,
    @SerializedName(value = "ASSIGNED_SCALE", alternate = ["assigned_scale"]) val assignedScale: String?,
    @SerializedName(value = "CHECKIN_COUNT", alternate = ["checkin_count"]) val checkinCount: Int?,
    /** Snowflake / JSON may send integer or float — Gson is picky on Int vs Double. */
    @SerializedName(value = "LATEST_SCORE", alternate = ["latest_score"]) val latestScore: Double?,
    @SerializedName(value = "LATEST_CHECKIN", alternate = ["latest_checkin"]) val latestCheckin: String?
)

data class ScaleQuestionsResponse(
    val scale_type: String,
    val questions: List<ScaleQuestion>
)

data class ScaleQuestion(
    @SerializedName("QUESTION_ID") val questionId: String,
    @SerializedName("QUESTION_TEXT") val questionText: String,
    @SerializedName("IS_VITALS_CORRELATED") val isVitalsCorrelated: Boolean?,
    @SerializedName("CLINICAL_CONSTRUCT") val clinicalConstruct: String?
)

data class PatientQuestionsResponse(
    val patient_id: Int,
    val patient_name: String,
    val scale_type: String,
    val questions: List<ScaleQuestion>
)

data class PatientHistoryResponse(
    val patient_id: Int,
    val checkins: List<HistoryCheckin>
)

data class HistoryCheckin(
    @SerializedName("SESSION_ID") val sessionId: Int,
    @SerializedName("CHECKIN_DATE") val checkinDate: String?,
    @SerializedName("SCALE_TYPE") val scaleType: String?,
    @SerializedName("SCALE_SCORE") val scaleScore: Int?,
    @SerializedName("HRV_VALUE") val hrvValue: Double?,
    @SerializedName("BREATHING_RATE") val breathingRate: Double?,
    @SerializedName("PULSE_RATE") val pulseRate: Double?,
    @SerializedName("DISTRESS_RATING") val distressRating: Int?,
    @SerializedName("SITUATION_TEXT") val situationText: String?,
    @SerializedName("COPING_TEXT") val copingText: String?
)

data class NextCheckinResponse(
    val next_checkin: NextCheckin?,
    val is_checkin_day: Boolean
)

data class NextCheckin(
    @SerializedName("SCHEDULE_ID") val scheduleId: Int,
    @SerializedName("SCHEDULED_DATE") val scheduledDate: String
)

// ── Checkins ──────────────────────────────────────────────────────────

data class CheckinSubmitRequest(
    val patient_id: Int,
    val scale_type: String,
    val scale_score: Int,
    val hrv: Double,
    val breathing_rate: Double,
    val pulse_rate: Double,
    val distress: Int,
    val situation: String,
    val coping: String,
    val questions: List<QuestionResponseItem>
)

data class QuestionResponseItem(
    val question_id: String,
    val response: Int,
    val hrv_at_question: Double?
)

data class CheckinSubmitResponse(
    val success: Boolean,
    val session_id: Int?,
    val message: String?,
    val pipeline_status: String?
)

data class CheckinStatusResponse(
    val session_id: Int,
    val has_situation_text: Boolean,
    val has_embedding: Boolean,
    val pipeline_complete: Boolean
)

// ── Dashboard ─────────────────────────────────────────────────────────

data class SimilarSessionsResponse(
    @SerializedName("patient_id") val patientId: Int,
    @SerializedName("anchor_session_id") val anchorSessionId: Int,
    @SerializedName("similar_sessions") val similarSessions: List<SimilarSession>,
    val count: Int
)

data class SimilarSession(
    @SerializedName("SESSION_ID") val sessionId: Int,
    @SerializedName("CHECKIN_DATE") val checkinDate: String,
    @SerializedName("SCALE_SCORE") val scaleScore: Int?,
    @SerializedName("HRV_VALUE") val hrvValue: Double?,
    @SerializedName("BREATHING_RATE") val breathingRate: Double?,
    @SerializedName("DISTRESS_RATING") val distressRating: Int?,
    @SerializedName("SITUATION_TEXT") val situationText: String?,
    @SerializedName("COPING_TEXT") val copingText: String?,
    @SerializedName("SCALE_TYPE") val scaleType: String?,
    @SerializedName("SIMILARITY") val similarity: Double?
)

data class SessionDetailResponse(
    val session: SessionInfo,
    val questions: List<QuestionVital>
)

data class SessionInfo(
    @SerializedName("SESSION_ID") val sessionId: Int,
    @SerializedName("PATIENT_ID") val patientId: Int,
    @SerializedName("CHECKIN_DATE") val checkinDate: String,
    @SerializedName("SCALE_TYPE") val scaleType: String?,
    @SerializedName("SCALE_SCORE") val scaleScore: Int?,
    @SerializedName("HRV_VALUE") val hrvValue: Double?,
    @SerializedName("BREATHING_RATE") val breathingRate: Double?,
    @SerializedName("PULSE_RATE") val pulseRate: Double?,
    @SerializedName("DISTRESS_RATING") val distressRating: Int?,
    @SerializedName("SITUATION_TEXT") val situationText: String?,
    @SerializedName("COPING_TEXT") val copingText: String?
)

data class QuestionVital(
    @SerializedName("QUESTION_ID") val questionId: String,
    @SerializedName("QUESTION_TEXT") val questionText: String?,
    @SerializedName("RESPONSE_VALUE") val responseValue: Int?,
    @SerializedName("HRV_AT_QUESTION") val hrvAtQuestion: Double?,
    @SerializedName("IS_VITALS_CORRELATED") val isVitalsCorrelated: Boolean?,
    @SerializedName("SCALE_TYPE") val scaleType: String?
)

data class SessionTrendsResponse(
    val patient_id: Int,
    val trends: List<Map<String, Any?>>
)
