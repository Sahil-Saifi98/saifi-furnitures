package com.saififurnitures.app.data.repository

import android.content.Context
import android.util.Log
import com.saififurnitures.app.data.local.AttendanceDao
import com.saififurnitures.app.data.local.AttendanceEntity
import com.saififurnitures.app.data.remote.RetrofitClient
import com.saififurnitures.app.data.session.SessionManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID

class AttendanceRepository(
    private val dao: AttendanceDao,
    private val context: Context
) {
    private val sessionManager = SessionManager(context)

    suspend fun insert(entity: AttendanceEntity): Long = dao.insert(entity)
    suspend fun update(entity: AttendanceEntity)        = dao.update(entity)

    suspend fun getTodayAttendance(): List<AttendanceEntity> {
        val userId = sessionManager.getUserId() ?: return emptyList()
        return dao.getTodayAttendance(userId)
    }

    suspend fun getOpenSession(): AttendanceEntity? {
        val userId = sessionManager.getUserId() ?: return null
        return dao.getOpenSession(userId)
    }

    suspend fun getUnsyncedCount(userId: String) = dao.getUnsyncedCount(userId)

    fun newSessionId(): String = UUID.randomUUID().toString()

    suspend fun syncAllPending(): SyncResult {
        val userId = sessionManager.getUserId()
            ?: return SyncResult(0, 0, 0, errorMessage = "Not logged in")

        val pending = dao.getUnsyncedAttendance(userId)
        if (pending.isEmpty()) return SyncResult(0, 0, 0)

        var success = 0
        var failed  = 0
        var missing = 0

        for (record in pending) {
            try {
                val file = File(record.selfiePath)
                if (!file.exists()) {
                    dao.markSyncedWithAddress(
                        record.id,
                        record.address.ifBlank { "Location: ${record.latitude}, ${record.longitude}" }
                    )
                    missing++
                    continue
                }

                val selfiePart = MultipartBody.Part.createFormData(
                    "selfie",
                    "selfie_${record.timestamp}.jpg",
                    file.asRequestBody("image/jpeg".toMediaType())
                )
                val latBody  = record.latitude.toString().toRequestBody("text/plain".toMediaType())
                val lngBody  = record.longitude.toString().toRequestBody("text/plain".toMediaType())
                val tsBody   = record.timestamp.toString().toRequestBody("text/plain".toMediaType())

                val response = if (record.type == "check_in") {
                    RetrofitClient.attendanceApi.checkIn(selfiePart, latBody, lngBody, tsBody)
                } else {
                    val sessionBody = record.sessionId.toRequestBody("text/plain".toMediaType())
                    RetrofitClient.attendanceApi.checkOut(selfiePart, latBody, lngBody, tsBody, sessionBody)
                }

                if (response.isSuccessful && response.body()?.success == true) {
                    // Both 200 (already exists) and 201 (newly created) are success
                    val addr = response.body()!!.data?.address ?: ""
                    val url  = response.body()!!.data?.selfieUrl ?: ""
                    dao.markSyncedWithDetails(
                        record.id,
                        addr.ifBlank { record.address },
                        url
                    )
                    success++
                } else if (response.code() == 409) {
                    // Conflict = already exists on server — mark as synced
                    dao.markAsSynced(record.id)
                    success++
                    Log.i("Repository", "Record already on server id=${record.id} — marked synced")
                } else {
                    if (response.code() != 401) failed++
                    Log.w("Repository", "Sync fail id=${record.id} code=${response.code()}")
                }
            } catch (e: Exception) {
                failed++
                Log.e("Repository", "Sync exception id=${record.id}: ${e.message}")
            }
        }

        return SyncResult(
            total        = pending.size,
            success      = success,
            failed       = failed,
            missingFiles = missing
        )
    }
}

data class SyncResult(
    val total: Int,
    val success: Int,
    val failed: Int,
    val missingFiles: Int = 0,
    val errorMessage: String? = null
)