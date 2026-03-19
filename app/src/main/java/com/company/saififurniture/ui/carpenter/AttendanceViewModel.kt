package com.saififurnitures.app.ui.attendance

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saififurnitures.app.data.local.AppDatabase
import com.saififurnitures.app.data.local.AttendanceEntity
import com.saififurnitures.app.data.remote.RetrofitClient
import com.saififurnitures.app.data.repository.AttendanceRepository
import com.saififurnitures.app.data.session.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

enum class AttendanceState { READY_TO_CHECK_IN, CHECKED_IN, PROCESSING }

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AttendanceRepository
    private val sessionManager = SessionManager(application)

    private val _attendanceState = MutableStateFlow(AttendanceState.READY_TO_CHECK_IN)
    val attendanceState: StateFlow<AttendanceState> = _attendanceState

    private val _status = MutableStateFlow("Good day! Tap Check In when you arrive at a site.")
    val status: StateFlow<String> = _status

    private val _todayRecords = MutableStateFlow<List<AttendanceEntity>>(emptyList())
    val todayRecords: StateFlow<List<AttendanceEntity>> = _todayRecords

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount

    private val _openSession = MutableStateFlow<AttendanceEntity?>(null)
    val openSession: StateFlow<AttendanceEntity?> = _openSession

    private var capturedSelfiePath: String? = null
    private var capturedLatitude:   Double? = null
    private var capturedLongitude:  Double? = null
    private var isCapturingCheckIn  = true

    init {
        val dao = AppDatabase.getDatabase(application).attendanceDao()
        repository = AttendanceRepository(dao, application)
        loadTodayData()
        syncOnStartup()
    }

    fun onCheckInSelfieCaptured(file: File) {
        isCapturingCheckIn  = true
        capturedSelfiePath  = file.absolutePath
        _status.value       = "Selfie captured, fetching location…"
    }

    fun onCheckOutSelfieCaptured(file: File) {
        isCapturingCheckIn  = false
        capturedSelfiePath  = file.absolutePath
        _status.value       = "Selfie captured, fetching location…"
    }

    fun onLocationReceived(lat: Double, lng: Double) {
        capturedLatitude  = lat
        capturedLongitude = lng
        processAttendance()
    }

    fun onLocationError(error: String) {
        _status.value        = "Location error: $error"
        _attendanceState.value = if (_openSession.value != null)
            AttendanceState.CHECKED_IN
        else
            AttendanceState.READY_TO_CHECK_IN
    }

    fun onCameraError(error: String) {
        _status.value          = "Camera error: $error"
        _attendanceState.value = if (_openSession.value != null)
            AttendanceState.CHECKED_IN
        else
            AttendanceState.READY_TO_CHECK_IN
    }

    private fun processAttendance() {
        val path = capturedSelfiePath ?: return
        val lat  = capturedLatitude   ?: return
        val lng  = capturedLongitude  ?: return

        capturedSelfiePath = null
        capturedLatitude   = null
        capturedLongitude  = null

        _attendanceState.value = AttendanceState.PROCESSING

        viewModelScope.launch {
            val timestamp  = System.currentTimeMillis()
            val userId     = sessionManager.getUserId()     ?: ""
            val employeeId = sessionManager.getEmployeeId() ?: ""
            val type       = if (isCapturingCheckIn) "check_in" else "check_out"
            val sessionId  = if (isCapturingCheckIn)
                repository.newSessionId()
            else
                _openSession.value?.sessionId ?: repository.newSessionId()

            // Save locally first — never lose attendance
            val entity = AttendanceEntity(
                userId     = userId,
                employeeId = employeeId,
                type       = type,
                sessionId  = sessionId,
                selfiePath = path,
                latitude   = lat,
                longitude  = lng,
                address    = "Location: $lat, $lng",
                timestamp  = timestamp,
                isSynced   = false
            )
            val localId = repository.insert(entity)
            Log.d("AttendanceVM", "Saved locally id=$localId type=$type")

            // Attempt upload
            try {
                _status.value = "Uploading…"
                val selfieFile = File(path)
                if (!selfieFile.exists()) {
                    _status.value = "⚠️ Selfie missing — saved locally, tap Sync to retry"
                    refreshAll()
                    return@launch
                }

                val selfiePart = MultipartBody.Part.createFormData(
                    "selfie",
                    "selfie_${timestamp}.jpg",
                    selfieFile.asRequestBody("image/jpeg".toMediaType())
                )
                val latBody = lat.toString().toRequestBody("text/plain".toMediaType())
                val lngBody = lng.toString().toRequestBody("text/plain".toMediaType())
                val tsBody  = timestamp.toString().toRequestBody("text/plain".toMediaType())

                val response = if (type == "check_in") {
                    RetrofitClient.attendanceApi.checkIn(selfiePart, latBody, lngBody, tsBody)
                } else {
                    val sessionBody = sessionId.toRequestBody("text/plain".toMediaType())
                    RetrofitClient.attendanceApi.checkOut(selfiePart, latBody, lngBody, tsBody, sessionBody)
                }

                if (response.isSuccessful && response.body()?.success == true) {
                    val addr = response.body()!!.data?.address ?: entity.address
                    val url  = response.body()!!.data?.selfieUrl ?: ""
                    repository.update(
                        entity.copy(id = localId, address = addr, selfieUrl = url, isSynced = true)
                    )
                    _status.value = if (type == "check_in")
                        "✅ Checked in successfully!"
                    else
                        "✅ Checked out successfully!"
                } else {
                    _status.value = "⚠️ Saved locally — tap Sync to retry (${response.code()})"
                }
            } catch (e: Exception) {
                _status.value = "⚠️ Network error — saved locally, tap Sync to retry"
                Log.e("AttendanceVM", "Upload error: ${e.message}")
            } finally {
                refreshAll()
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _status.value = "Syncing pending records…"
            try {
                val result = repository.syncAllPending()
                _status.value = when {
                    result.total == 0          -> "✅ Everything is up to date"
                    result.success == result.total -> "✅ Synced all ${result.success} record(s)"
                    result.success > 0         -> "⚠️ Synced ${result.success}/${result.total} — ${result.failed} failed"
                    else                       -> "⚠️ Sync failed — check connection"
                }
            } catch (e: Exception) {
                _status.value = "Sync error: ${e.message}"
            } finally {
                refreshAll()
            }
        }
    }

    private fun syncOnStartup() {
        viewModelScope.launch {
            delay(700)
            val userId = sessionManager.getUserId() ?: return@launch
            if (repository.getUnsyncedCount(userId) == 0) return@launch
            try {
                repository.syncAllPending()
                refreshAll()
            } catch (_: Exception) { }
        }
    }

    private fun refreshAll() {
        viewModelScope.launch { loadTodayData() }
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            _pendingCount.value = repository.getUnsyncedCount(userId)
        }
    }

    private fun loadTodayData() {
        viewModelScope.launch {
            val records = repository.getTodayAttendance()
            _todayRecords.value    = records
            val open               = repository.getOpenSession()
            _openSession.value     = open
            _attendanceState.value = if (open != null)
                AttendanceState.CHECKED_IN
            else
                AttendanceState.READY_TO_CHECK_IN
        }
    }
}