package com.example.cadence.api

import com.google.gson.annotations.SerializedName

data class PatientListResponse(
    val patients: List<ApiPatient>
)

data class ApiPatient(
    @SerializedName("PATIENT_ID") val patientId: Int,
    @SerializedName("NAME") val name: String,
    @SerializedName("EMAIL") val email: String?,
    @SerializedName("ASSIGNED_SCALE") val assignedScale: String?,
    @SerializedName("CHECKIN_COUNT") val checkinCount: Int?,
    @SerializedName("LATEST_SCORE") val latestScore: Int?,
    @SerializedName("LATEST_CHECKIN") val latestCheckin: String?
)

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
