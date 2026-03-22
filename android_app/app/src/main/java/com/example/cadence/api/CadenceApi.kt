package com.example.cadence.api

import retrofit2.Call
import retrofit2.http.*

interface CadenceApi {

    // ── Auth ──────────────────────────────────────────────────────────

    @GET("auth/config")
    fun getAuthConfig(): Call<AuthConfigResponse>

    @POST("auth/login")
    fun login(): Call<LoginResponse>

    @GET("auth/me")
    fun getCurrentUser(): Call<UserProfileResponse>

    @POST("auth/sync-user")
    fun syncUser(@Body request: SyncUserRequest): Call<UserProfileResponse>

    // ── Patients ──────────────────────────────────────────────────────

    @GET("patients/")
    fun listPatients(
        @Query("therapist_id") therapistId: Int? = null
    ): Call<PatientListResponse>

    @GET("patients/get-questions/{patient_id}")
    fun getPatientQuestions(
        @Path("patient_id") patientId: Int
    ): Call<PatientQuestionsResponse>

    @GET("patients/{patient_id}")
    fun getPatient(
        @Path("patient_id") patientId: Int
    ): Call<Map<String, Any>>

    @GET("patients/{patient_id}/history")
    fun getPatientHistory(
        @Path("patient_id") patientId: Int,
        @Query("limit") limit: Int? = null
    ): Call<PatientHistoryResponse>

    @GET("patients/{patient_id}/next-checkin")
    fun getNextCheckin(
        @Path("patient_id") patientId: Int
    ): Call<NextCheckinResponse>

    // ── Checkins ──────────────────────────────────────────────────────

    @GET("checkins/scale-questions/{scale_type}")
    fun getScaleQuestions(
        @Path("scale_type") scaleType: String
    ): Call<ScaleQuestionsResponse>

    @POST("checkins/")
    fun submitCheckin(
        @Body data: CheckinSubmitRequest
    ): Call<CheckinSubmitResponse>

    @GET("checkins/{session_id}/status")
    fun getCheckinStatus(
        @Path("session_id") sessionId: Int
    ): Call<CheckinStatusResponse>

    @POST("checkins/{session_id}/run-pipeline")
    fun runPipeline(
        @Path("session_id") sessionId: Int
    ): Call<Map<String, Any>>

    // ── Dashboard ─────────────────────────────────────────────────────

    @GET("dashboard/therapists/{therapist_id}/patients")
    fun getPatients(
        @Path("therapist_id") therapistId: Int
    ): Call<PatientListResponse>

    @GET("dashboard/patients/{patient_id}/similar-sessions")
    fun getSimilarSessions(
        @Path("patient_id") patientId: Int,
        @Query("session_id") sessionId: Int? = null,
        @Query("limit") limit: Int? = null
    ): Call<SimilarSessionsResponse>

    @GET("dashboard/sessions/{session_id}/detail")
    fun getSessionDetail(
        @Path("session_id") sessionId: Int
    ): Call<SessionDetailResponse>

    @GET("dashboard/patients/{patient_id}/trends")
    fun getSessionTrends(
        @Path("patient_id") patientId: Int
    ): Call<SessionTrendsResponse>
}
