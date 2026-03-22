package com.example.cadence.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    var baseUrl = "https://rayford-unoperatable-unqualifyingly.ngrok-free.dev"
        private set

    private var authToken: String? = null

    fun setBaseUrl(url: String) {
        baseUrl = url
        rebuildApi()
    }

    fun setAuthToken(token: String?) {
        authToken = token
        rebuildApi()
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private fun buildClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val builder = chain.request().newBuilder()
                authToken?.let { builder.addHeader("Authorization", "Bearer $it") }
                chain.proceed(builder.build())
            }
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private fun buildRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(buildClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    var api: CadenceApi = buildRetrofit().create(CadenceApi::class.java)
        private set

    private fun rebuildApi() {
        api = buildRetrofit().create(CadenceApi::class.java)
    }
}
