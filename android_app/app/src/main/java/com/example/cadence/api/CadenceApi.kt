package com.example.cadence.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CadenceApi {

    // ─── Auth ────────────────────────────────────────────────────────────────

    @GET("auth/config")
    suspend fun getAuthConfig(): AuthConfigResponse

    @GET("auth/me")
    suspend fun getCurrentUser(): UserProfile

    @POST("auth/sync-user")
    suspend fun syncUser(@Body body: SyncUserRequest): UserProfile

    @POST("auth/login")
    suspend fun login(): LoginResponse

    // ─── Patients ────────────────────────────────────────────────────────────

    @GET("patients/get-questions/{patient_id}")
    suspend fun getPatientQuestions(
        @Path("patient_id") patientId: Int
    ): PatientQuestionsResponse

    @GET("patients/{patient_id}")
    suspend fun getPatient(
        @Path("patient_id") patientId: Int
    ): PatientDetail

    @GET("patients/{patient_id}/history")
    suspend fun getPatientHistory(
        @Path("patient_id") patientId: Int,
        @Query("limit") limit: Int = 10
    ): PatientHistoryResponse

    @GET("patients/{patient_id}/next-checkin")
    suspend fun getNextCheckin(
        @Path("patient_id") patientId: Int
    ): NextCheckinResponse

    // ─── Checkins ────────────────────────────────────────────────────────────

    @GET("checkins/scale-questions/{scale_type}")
    suspend fun getScaleQuestions(
        @Path("scale_type") scaleType: String
    ): ScaleQuestionsResponse

    @POST("checkins/")
    suspend fun submitCheckin(@Body body: CheckinSubmitRequest): CheckinSubmitResponse

    @GET("checkins/{session_id}/status")
    suspend fun getCheckinStatus(
        @Path("session_id") sessionId: Int
    ): CheckinStatusResponse

    @POST("checkins/{session_id}/run-pipeline")
    suspend fun runPipeline(
        @Path("session_id") sessionId: Int
    ): RunPipelineResponse

    // ─── Dashboard ───────────────────────────────────────────────────────────

    @GET("dashboard/therapists/{therapist_id}/patients")
    suspend fun getPatients(
        @Path("therapist_id") therapistId: Int
    ): PatientsResponse

    @GET("dashboard/patients/{patient_id}/brief")
    suspend fun getPatientBrief(
        @Path("patient_id") patientId: Int
    ): PatientBriefResponse

    @GET("dashboard/patients/{patient_id}/trends")
    suspend fun getSessionTrends(
        @Path("patient_id") patientId: Int
    ): SessionTrendsResponse

    @GET("dashboard/patients/{patient_id}/question-trends")
    suspend fun getQuestionTrends(
        @Path("patient_id") patientId: Int
    ): QuestionTrendsResponse

    @GET("dashboard/patients/{patient_id}/sparkline/{question_id}")
    suspend fun getSparkline(
        @Path("patient_id") patientId: Int,
        @Path("question_id") questionId: String
    ): SparklineResponse

    @GET("dashboard/patients/{patient_id}/similar-sessions")
    suspend fun getSimilarSessions(
        @Path("patient_id") patientId: Int,
        @Query("session_id") sessionId: Int? = null,
        @Query("limit") limit: Int = 20
    ): SimilarSessionsResponse

    @GET("dashboard/sessions/{session_id}/detail")
    suspend fun getSessionDetail(
        @Path("session_id") sessionId: Int
    ): SessionDetailResponse

    // ─── Therapist Schedule ──────────────────────────────────────────────────

    @POST("therapists/{therapist_id}/schedule")
    suspend fun insertSchedule(
        @Path("therapist_id") therapistId: Int,
        @Body body: ScheduleInsertRequest
    ): ScheduleInsertResponse
}
