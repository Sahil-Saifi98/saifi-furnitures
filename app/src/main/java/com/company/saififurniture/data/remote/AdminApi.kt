package com.saififurnitures.app.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

// ── Attendance models ──────────────────────────────────────────────

data class AdminAttendanceResponse(
    val success: Boolean,
    val message: String? = null,
    val count: Int,
    val data: List<AdminAttendanceItem>
)

data class AdminUserInfo(
    val _id: String,
    val name: String,
    val employeeId: String,
    val email: String
)

data class AdminAttendanceItem(
    val _id: String,
    val userId: AdminUserInfo,
    val employeeId: String,
    val sessionId: String,
    val checkInTime: String?,   // null if session only has check-out
    val checkOutTime: String?,
    val checkInSelfieUrl: String?,
    val checkOutSelfieUrl: String?,
    val checkInLatitude: Double,
    val checkInLongitude: Double,
    val checkInAddress: String,
    val checkOutAddress: String?,
    val date: String,
    val isSynced: Boolean
)

// ── User models ────────────────────────────────────────────────────

data class AdminUsersResponse(
    val success: Boolean,
    val count: Int,
    val data: List<AdminUser>
)

data class AdminUser(
    val _id: String,
    val employeeId: String,
    val name: String,
    val email: String,
    val role: String,
    val isActive: Boolean
)

data class AdminUserResponse(
    val success: Boolean,
    val message: String,
    val user: AdminUser? = null
)

data class RegisterCarpenterRequest(
    val employeeId: String,
    val name: String,
    val email: String,
    val password: String,
    val role: String = "carpenter"
)

// ── API interface ──────────────────────────────────────────────────

interface AdminApi {

    // Attendance
    @GET("admin/attendance")
    suspend fun getAllAttendance(
        @Query("startDate")  startDate:  String? = null,
        @Query("endDate")    endDate:    String? = null,
        @Query("employeeId") employeeId: String? = null
    ): Response<AdminAttendanceResponse>

    // Get attendance for a specific user
    @GET("admin/attendance")
    suspend fun getUserAttendance(
        @Query("employeeId") employeeId: String,
        @Query("startDate")  startDate:  String? = null,
        @Query("endDate")    endDate:    String? = null
    ): Response<AdminAttendanceResponse>

    // Users
    @GET("admin/users")
    suspend fun getAllUsers(): Response<AdminUsersResponse>

    @POST("admin/users")
    suspend fun addCarpenter(
        @Body request: RegisterCarpenterRequest
    ): Response<AdminUserResponse>

    @DELETE("admin/users/{id}")
    suspend fun deleteCarpenter(
        @Path("id") userId: String
    ): Response<AdminUserResponse>

    // Export
    @Streaming
    @GET("admin/export/attendance/csv")
    suspend fun exportAttendanceCSV(
        @Query("startDate")  startDate:  String? = null,
        @Query("endDate")    endDate:    String? = null,
        @Query("employeeId") employeeId: String? = null
    ): Response<ResponseBody>

    @Streaming
    @GET("admin/export/attendance/pdf")
    suspend fun exportAttendancePDF(
        @Query("startDate")  startDate:  String? = null,
        @Query("endDate")    endDate:    String? = null,
        @Query("employeeId") employeeId: String? = null
    ): Response<ResponseBody>
}