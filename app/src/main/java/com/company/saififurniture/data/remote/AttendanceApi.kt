package com.saififurnitures.app.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

data class AttendanceResponse(
    val success: Boolean,
    val message: String,
    val data: AttendanceData?
)

data class AttendanceListResponse(
    val success: Boolean,
    val count: Int,
    val data: List<AttendanceData>
)

data class AttendanceData(
    val _id: String,
    val userId: String,
    val employeeId: String,
    val type: String,
    val sessionId: String,
    val selfieUrl: String?,
    val selfiePath: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val timestamp: String,
    val date: String,
    val time: String,
    val isSynced: Boolean,
    val createdAt: String
)

interface AttendanceApi {

    @Multipart
    @POST("attendance/checkin")
    suspend fun checkIn(
        @Part selfie: MultipartBody.Part,
        @Part("latitude")  latitude:  RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("timestamp") timestamp: RequestBody
    ): Response<AttendanceResponse>

    @Multipart
    @POST("attendance/checkout")
    suspend fun checkOut(
        @Part selfie: MultipartBody.Part,
        @Part("latitude")  latitude:  RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("timestamp") timestamp: RequestBody,
        @Part("sessionId") sessionId: RequestBody
    ): Response<AttendanceResponse>

    @GET("attendance/today")
    suspend fun getTodayAttendance(): Response<AttendanceListResponse>

    @GET("attendance/active-session")
    suspend fun getActiveSession(): Response<AttendanceResponse>

    @GET("attendance/history")
    suspend fun getAttendanceHistory(
        @Query("startDate") startDate: String? = null,
        @Query("endDate")   endDate:   String? = null
    ): Response<AttendanceListResponse>
}