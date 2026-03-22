package com.example.cadence.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // TODO: Replace with your actual backend URL before deploying
    //  - Emulator → host machine: "http://10.0.2.2:8000/api/v1/"
    //  - Physical device on same Wi-Fi: "http://<LAN_IP>:8000/api/v1/"
    //  - Production: "https://your-api.example.com/api/v1/"
    private var baseUrl = "http://10.0.2.2:8000/api/v1/"

    private var authToken: String? = null

    fun setBaseUrl(url: String) {
        baseUrl = url
    }

    fun setAuthToken(token: String) {
        authToken = token
    }

    private val authInterceptor = Interceptor { chain ->
        val builder = chain.request().newBuilder()
        authToken?.let { builder.addHeader("Authorization", "Bearer $it") }
        chain.proceed(builder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: CadenceApi by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CadenceApi::class.java)
    }
}
