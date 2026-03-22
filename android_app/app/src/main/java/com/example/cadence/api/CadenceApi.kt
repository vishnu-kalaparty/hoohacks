package com.example.cadence.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CadenceApi {

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

    @GET("sessions/{session_id}/detail")
    fun getSessionDetail(
        @Path("session_id") sessionId: Int
    ): Call<SessionDetailResponse>
}
