package com.saififurnitures.app.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // ← Replace with your SaifiFurnitures backend URL
    private const val BASE_URL = "https://YOUR_SAIFI_BACKEND.onrender.com/api/"

    @Volatile private var authToken: String? = null

    fun setAuthToken(token: String?) { authToken = token }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val token   = tokenProvider()
            val request = chain.request()
            return if (token != null) {
                chain.proceed(
                    request.newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                )
            } else {
                chain.proceed(request)
            }
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(AuthInterceptor { authToken })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val downloadClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        })
        .addInterceptor(AuthInterceptor { authToken })
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .callTimeout(15, TimeUnit.MINUTES)
        .retryOnConnectionFailure(true)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val downloadRetrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(downloadClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApi:       AuthApi       = retrofit.create(AuthApi::class.java)
    val attendanceApi: AttendanceApi = retrofit.create(AttendanceApi::class.java)
    val adminApi:      AdminApi      = downloadRetrofit.create(AdminApi::class.java)
}