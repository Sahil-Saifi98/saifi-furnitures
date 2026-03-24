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
import kotlinx.coroutines.Job
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

    private val repository     = AttendanceRepository(
        AppDatabase.getDatabase(application).attendanceDao(), application
    )
    private val sessionManager = SessionManager(application)

    private val _attendanceState = MutableStateFlow(AttendanceState.READY_TO_CHECK_IN)
    val attendanceState: StateFlow<AttendanceState> = _attendanceState

    private val _status = MutableStateFlow("Good day! Tap Check In when you arrive.")
    val status: StateFlow<String> = _status

    // Today's two records — at most one check-in and one check-out
    private val _checkIn  = MutableStateFlow<AttendanceEntity?>(null)
    val checkIn: StateFlow<AttendanceEntity?> = _checkIn

    private val _checkOut = MutableStateFlow<AttendanceEntity?>(null)
    val checkOut: StateFlow<AttendanceEntity?> = _checkOut

    // Keep todayRecords for backward compat with AttendanceScreen list
    private val _todayRecords = MutableStateFlow<List<AttendanceEntity>>(emptyList())
    val todayRecords: StateFlow<List<AttendanceEntity>> = _todayRecords

    private val _openSession = MutableStateFlow<AttendanceEntity?>(null)
    val openSession: StateFlow<AttendanceEntity?> = _openSession

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount

    // Locked at button tap — safe from async interference
    private var pendingIsCheckIn: Boolean = true
    private var pendingSessionId: String  = ""

    // Set after selfie captured
    private var capturedSelfiePath: String? = null
    private var capturedLatitude:   Double? = null
    private var capturedLongitude:  Double? = null

    // Guards
    private var isCurrentlyProcessing = false
    private var loadTodayJob: Job? = null

    init {
        viewModelScope.launch { loadTodayData() }
        syncOnStartup()
    }

    // ── Called at BUTTON TAP (before camera opens) ─────────────────

    fun prepareCheckIn() {
        pendingIsCheckIn = true
        // Generate sessionId now — one fixed ID for today's check-in
        pendingSessionId = repository.newSessionId()
        Log.d("AttendanceVM", "prepareCheckIn sessionId=$pendingSessionId")
    }

    fun prepareCheckOut() {
        pendingIsCheckIn = false
        // Lock in the open session's ID right now
        pendingSessionId = _openSession.value?.sessionId ?: ""
        Log.d("AttendanceVM", "prepareCheckOut sessionId=$pendingSessionId openSession=${_openSession.value?.sessionId}")
    }

    // ── Camera & location callbacks ────────────────────────────────

    fun onSelfieCaptured(file: File) {
        capturedSelfiePath = file.absolutePath
        _status.value      = "Selfie captured, fetching location…"
    }

    fun onCheckInSelfieCaptured(file: File)  { onSelfieCaptured(file) }
    fun onCheckOutSelfieCaptured(file: File) { onSelfieCaptured(file) }

    fun onLocationReceived(lat: Double, lng: Double) {
        capturedLatitude  = lat
        capturedLongitude = lng
        processAttendance()
    }

    fun onLocationError(error: String) {
        _status.value = "Location error: $error"
        restoreState()
        clearPending()
    }

    fun onCameraError(error: String) {
        _status.value = "Camera error: $error"
        restoreState()
        clearPending()
    }

    private fun restoreState() {
        _attendanceState.value = if (_openSession.value != null)
            AttendanceState.CHECKED_IN else AttendanceState.READY_TO_CHECK_IN
    }

    private fun clearPending() {
        capturedSelfiePath    = null
        capturedLatitude      = null
        capturedLongitude     = null
        isCurrentlyProcessing = false
    }

    // ── Core processing ────────────────────────────────────────────

    private fun processAttendance() {
        if (isCurrentlyProcessing) {
            Log.w("AttendanceVM", "Already processing — ignoring")
            return
        }

        val path = capturedSelfiePath ?: run { Log.w("AttendanceVM", "No selfie path"); return }
        val lat  = capturedLatitude   ?: run { Log.w("AttendanceVM", "No latitude");    return }
        val lng  = capturedLongitude  ?: run { Log.w("AttendanceVM", "No longitude");   return }

        val isCheckIn = pendingIsCheckIn
        val sessionId = if (isCheckIn) {
            pendingSessionId
        } else {
            // For checkout, if somehow pendingSessionId is empty, try openSession
            pendingSessionId.ifBlank {
                Log.w("AttendanceVM", "pendingSessionId blank for checkout — using openSession")
                _openSession.value?.sessionId ?: repository.newSessionId()
            }
        }

        val type = if (isCheckIn) "check_in" else "check_out"
        Log.d("AttendanceVM", "processAttendance type=$type sessionId=$sessionId")

        // Clear immediately to prevent reuse
        capturedSelfiePath    = null
        capturedLatitude      = null
        capturedLongitude     = null
        isCurrentlyProcessing = true
        _attendanceState.value = AttendanceState.PROCESSING

        viewModelScope.launch {
            val timestamp  = System.currentTimeMillis()
            val userId     = sessionManager.getUserId()     ?: ""
            val employeeId = sessionManager.getEmployeeId() ?: ""

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
            Log.d("AttendanceVM", "Saved locally id=$localId type=$type sessionId=$sessionId")

            try {
                _status.value = "Uploading…"
                val selfieFile = File(path)
                if (!selfieFile.exists()) {
                    _status.value = "⚠️ Selfie missing — saved locally, tap Sync to retry"
                    return@launch
                }

                val selfiePart = MultipartBody.Part.createFormData(
                    "selfie", "selfie_${timestamp}.jpg",
                    selfieFile.asRequestBody("image/jpeg".toMediaType())
                )
                val latBody     = lat.toString().toRequestBody("text/plain".toMediaType())
                val lngBody     = lng.toString().toRequestBody("text/plain".toMediaType())
                val tsBody      = timestamp.toString().toRequestBody("text/plain".toMediaType())

                val response = if (isCheckIn) {
                    RetrofitClient.attendanceApi.checkIn(selfiePart, latBody, lngBody, tsBody)
                } else {
                    val sessionBody = sessionId.toRequestBody("text/plain".toMediaType())
                    RetrofitClient.attendanceApi.checkOut(selfiePart, latBody, lngBody, tsBody, sessionBody)
                }

                if (response.isSuccessful && response.body()?.success == true) {
                    val addr = response.body()!!.data?.address ?: entity.address
                    val url  = response.body()!!.data?.selfieUrl ?: ""
                    repository.update(entity.copy(id = localId, address = addr, selfieUrl = url, isSynced = true))
                    _status.value = if (isCheckIn) "✅ Checked in successfully!" else "✅ Checked out successfully!"
                } else {
                    _status.value = "⚠️ Saved locally — tap Sync to retry (${response.code()})"
                }
            } catch (e: Exception) {
                _status.value = "⚠️ Network error — saved locally, tap Sync to retry"
                Log.e("AttendanceVM", "Upload error: ${e.message}")
            } finally {
                isCurrentlyProcessing = false
                refreshAll()
            }
        }
    }

    // ── Sync ───────────────────────────────────────────────────────

    fun syncNow() {
        viewModelScope.launch {
            _status.value = "Syncing pending records…"
            try {
                val result = repository.syncAllPending()
                _status.value = when {
                    result.total == 0              -> "✅ Everything is up to date"
                    result.success == result.total -> "✅ Synced all ${result.success} record(s)"
                    result.success > 0             -> "⚠️ Synced ${result.success}/${result.total} — ${result.failed} failed"
                    else                           -> "⚠️ Sync failed — check connection"
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
            try { repository.syncAllPending(); refreshAll() } catch (_: Exception) { }
        }
    }

    // ── Refresh & Load ─────────────────────────────────────────────

    private fun refreshAll() {
        loadTodayJob?.cancel()
        loadTodayJob = viewModelScope.launch { loadTodayData() }
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            _pendingCount.value = repository.getUnsyncedCount(userId)
        }
    }

    private suspend fun loadTodayData() {
        if (isCurrentlyProcessing) return

        val records  = repository.getTodayAttendance()
        val checkIn  = records.firstOrNull { it.type == "check_in" }
        val checkOut = records.firstOrNull { it.type == "check_out" }
        val open     = if (checkIn != null && checkOut == null) checkIn else null

        _todayRecords.value    = records
        _checkIn.value         = checkIn
        _checkOut.value        = checkOut
        _openSession.value     = open
        _attendanceState.value = if (open != null) AttendanceState.CHECKED_IN
        else AttendanceState.READY_TO_CHECK_IN

        // If no local check-in, check API for open session (handles reinstall)
        if (checkIn == null) {
            try {
                val activeResp = RetrofitClient.attendanceApi.getActiveSession()
                val apiOpen    = activeResp.body()?.data ?: return

                val alreadyExists = repository.getTodayAttendance()
                    .any { it.sessionId == apiOpen.sessionId && it.type == "check_in" }
                if (alreadyExists) {
                    _attendanceState.value = AttendanceState.CHECKED_IN
                    return
                }

                val userId     = sessionManager.getUserId()     ?: return
                val employeeId = sessionManager.getEmployeeId() ?: return
                val ts = try {
                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                        .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                        .parse(apiOpen.timestamp)?.time ?: System.currentTimeMillis()
                } catch (_: Exception) { System.currentTimeMillis() }

                repository.insert(AttendanceEntity(
                    userId = userId, employeeId = employeeId, type = "check_in",
                    sessionId = apiOpen.sessionId, selfiePath = "",
                    selfieUrl = apiOpen.selfieUrl ?: "", latitude = apiOpen.latitude,
                    longitude = apiOpen.longitude, address = apiOpen.address,
                    timestamp = ts, isSynced = true
                ))
                // Reload after insert
                val refreshed = repository.getTodayAttendance()
                val restoredCheckIn = refreshed.firstOrNull { it.type == "check_in" }
                _todayRecords.value = refreshed
                _checkIn.value      = restoredCheckIn
                _openSession.value  = restoredCheckIn
                _attendanceState.value = AttendanceState.CHECKED_IN
            } catch (_: Exception) { }
        }
    }
}