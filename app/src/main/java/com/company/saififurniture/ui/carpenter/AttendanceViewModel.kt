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

enum class AttendanceState { READY_TO_CHECK_IN, CHECKED_IN, COMPLETED, PROCESSING }

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

    // These are set at button tap time — before camera opens
    // This ensures async loadTodayData() cannot affect them later
    private var pendingType:      String? = null   // "check_in" or "check_out"
    private var pendingSessionId: String? = null   // locked in at button tap

    // Captured during camera/location flow
    private var capturedSelfiePath: String? = null
    private var capturedLatitude:   Double? = null
    private var capturedLongitude:  Double? = null

    // Prevents double processing
    private var isCurrentlyProcessing = false

    // Cancels in-progress loadTodayData to avoid race conditions
    private var loadTodayJob: Job? = null

    init {
        val dao = AppDatabase.getDatabase(application).attendanceDao()
        repository = AttendanceRepository(dao, application)

        viewModelScope.launch {
            loadTodayData()
        }

        syncOnStartup()
    }

    // ── Called at BUTTON TAP — before camera opens ─────────────────
    // This locks in sessionId while _openSession is guaranteed correct

    fun prepareCheckIn() {
        pendingType      = "check_in"
        pendingSessionId = repository.newSessionId()
        Log.d("AttendanceVM", "prepareCheckIn: new sessionId=$pendingSessionId")
    }

    fun prepareCheckOut() {
        pendingType      = "check_out"
        pendingSessionId = _openSession.value?.sessionId
        Log.d("AttendanceVM", "prepareCheckOut: sessionId=$pendingSessionId openSession=${_openSession.value?.sessionId}")
    }

    // ── Called after selfie captured ───────────────────────────────

    fun onSelfieCaptured(file: File) {
        capturedSelfiePath = file.absolutePath
        _status.value      = "Selfie captured, fetching location…"
    }

    // Keep old methods for compatibility
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
        val records = _todayRecords.value
        _attendanceState.value = when {
            records.any { it.type == "check_in" } &&
                    records.any { it.type == "check_out" } -> AttendanceState.COMPLETED
            _openSession.value != null             -> AttendanceState.CHECKED_IN
            else                                   -> AttendanceState.READY_TO_CHECK_IN
        }
    }

    private fun clearPending() {
        pendingType       = null
        pendingSessionId  = null
        capturedSelfiePath = null
        capturedLatitude   = null
        capturedLongitude  = null
        isCurrentlyProcessing = false
    }

    // ── Core attendance processing ─────────────────────────────────

    private fun processAttendance() {
        if (isCurrentlyProcessing) {
            Log.w("AttendanceVM", "Already processing — ignoring duplicate call")
            return
        }

        val path      = capturedSelfiePath ?: return
        val lat       = capturedLatitude   ?: return
        val lng       = capturedLongitude  ?: return
        val type      = pendingType        ?: return
        val sessionId = pendingSessionId
            ?: if (type == "check_out") {
                // Last resort fallback — should never happen if prepareCheckOut was called
                Log.w("AttendanceVM", "pendingSessionId null for checkout! Using openSession")
                _openSession.value?.sessionId ?: repository.newSessionId()
            } else {
                repository.newSessionId()
            }

        Log.d("AttendanceVM", "processAttendance: type=$type sessionId=$sessionId")

        // Clear captured values immediately to prevent any reuse
        clearPending()

        isCurrentlyProcessing = true
        _attendanceState.value = AttendanceState.PROCESSING

        viewModelScope.launch {
            val timestamp  = System.currentTimeMillis()
            val userId     = sessionManager.getUserId()     ?: ""
            val employeeId = sessionManager.getEmployeeId() ?: ""

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
            try {
                repository.syncAllPending()
                refreshAll()
            } catch (_: Exception) { }
        }
    }

    // ── Refresh ────────────────────────────────────────────────────

    private fun refreshAll() {
        // Cancel any in-progress load to avoid stale data races
        loadTodayJob?.cancel()
        loadTodayJob = viewModelScope.launch { loadTodayData() }
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            _pendingCount.value = repository.getUnsyncedCount(userId)
        }
    }

    private suspend fun loadTodayData() {
        // Skip if a check-in/out is being processed — don't mess with state mid-flow
        if (isCurrentlyProcessing) return

        val localRecords = repository.getTodayAttendance()
        val localOpen    = repository.getOpenSession()
        val hasCheckIn   = localRecords.any { it.type == "check_in" }
        val hasCheckOut  = localRecords.any { it.type == "check_out" }

        _todayRecords.value    = localRecords
        _openSession.value     = localOpen
        _attendanceState.value = when {
            hasCheckIn && hasCheckOut -> AttendanceState.COMPLETED
            localOpen != null         -> AttendanceState.CHECKED_IN
            else                      -> AttendanceState.READY_TO_CHECK_IN
        }

        // If today is fully done locally — no need to check API
        if (hasCheckIn && hasCheckOut) return
        // If open session exists locally — no need to check API
        if (localOpen != null) return

        try {
            val activeResp = RetrofitClient.attendanceApi.getActiveSession()
            val apiOpen    = activeResp.body()?.data ?: return

            // API has open session but local doesn't — restore it (reinstall case)
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

            repository.insert(
                AttendanceEntity(
                    userId     = userId,
                    employeeId = employeeId,
                    type       = "check_in",
                    sessionId  = apiOpen.sessionId,
                    selfiePath = "",
                    selfieUrl  = apiOpen.selfieUrl ?: "",
                    latitude   = apiOpen.latitude,
                    longitude  = apiOpen.longitude,
                    address    = apiOpen.address,
                    timestamp  = ts,
                    isSynced   = true
                )
            )

            val refreshed    = repository.getTodayAttendance()
            val openSess     = repository.getOpenSession()
            val restoredDone = refreshed.any { it.type == "check_in" } &&
                    refreshed.any { it.type == "check_out" }
            _todayRecords.value    = refreshed
            _openSession.value     = openSess
            _attendanceState.value = if (restoredDone) AttendanceState.COMPLETED
            else AttendanceState.CHECKED_IN

        } catch (_: Exception) {
            // Network unavailable — local data already shown
        }
    }
}