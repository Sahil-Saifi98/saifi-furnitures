package com.saififurnitures.app.ui.admin

import android.app.Application
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saififurnitures.app.data.remote.AdminUser
import com.saififurnitures.app.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ExportStatus(val message: String, val isSuccess: Boolean)

class AdminExportViewModel(application: Application) : AndroidViewModel(application) {

    private val _isLoading     = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _exportStatus  = MutableStateFlow<ExportStatus?>(null)
    val exportStatus: StateFlow<ExportStatus?> = _exportStatus

    private val _startDate     = MutableStateFlow<String?>(null)
    val startDate: StateFlow<String?> = _startDate

    private val _endDate       = MutableStateFlow<String?>(null)
    val endDate: StateFlow<String?> = _endDate

    // User list for dropdown
    private val _users         = MutableStateFlow<List<AdminUser>>(emptyList())
    val users: StateFlow<List<AdminUser>> = _users

    private val _selectedUser  = MutableStateFlow<AdminUser?>(null)
    val selectedUser: StateFlow<AdminUser?> = _selectedUser

    init { loadUsers() }

    fun setStartDate(date: String)       { _startDate.value    = date }
    fun setEndDate(date: String)         { _endDate.value      = date }
    fun selectUser(user: AdminUser?)     { _selectedUser.value = user }

    private fun loadUsers() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.adminApi.getAllUsers()
                if (response.isSuccessful && response.body()?.success == true) {
                    _users.value = response.body()!!.data
                }
            } catch (_: Exception) { }
        }
    }

    // ── Export ALL employees ──────────────────────────────────────

    fun exportAllCSV() {
        viewModelScope.launch {
            _isLoading.value    = true
            _exportStatus.value = null
            try {
                val response = RetrofitClient.adminApi.exportAttendanceCSV(
                    startDate = _startDate.value,
                    endDate   = _endDate.value
                )
                if (response.isSuccessful && response.body() != null) {
                    saveFile(response.body()!!.bytes(), "attendance_all_${timestamp()}.csv", "text/csv")
                    _exportStatus.value = ExportStatus("✅ CSV downloaded to Downloads folder", true)
                } else {
                    _exportStatus.value = ExportStatus("Export failed: ${response.code()}", false)
                }
            } catch (e: Exception) {
                _exportStatus.value = ExportStatus("Error: ${e.message}", false)
                Log.e("ExportVM", "All CSV error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun exportAllPDF() {
        viewModelScope.launch {
            _isLoading.value    = true
            _exportStatus.value = null
            try {
                val response = RetrofitClient.adminApi.exportAttendancePDF(
                    startDate = _startDate.value,
                    endDate   = _endDate.value
                )
                if (response.isSuccessful && response.body() != null) {
                    saveFile(response.body()!!.bytes(), "attendance_all_${timestamp()}.pdf", "application/pdf")
                    _exportStatus.value = ExportStatus("✅ PDF downloaded to Downloads folder", true)
                } else {
                    _exportStatus.value = ExportStatus("Export failed: ${response.code()}", false)
                }
            } catch (e: Exception) {
                _exportStatus.value = ExportStatus("Error: ${e.message}", false)
                Log.e("ExportVM", "All PDF error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Export SPECIFIC user ──────────────────────────────────────

    fun exportUserCSV() {
        val user = _selectedUser.value ?: run {
            _exportStatus.value = ExportStatus("Please select a carpenter first", false)
            return
        }
        viewModelScope.launch {
            _isLoading.value    = true
            _exportStatus.value = null
            try {
                val response = RetrofitClient.adminApi.exportAttendanceCSV(
                    startDate  = _startDate.value,
                    endDate    = _endDate.value,
                    employeeId = user.employeeId
                )
                if (response.isSuccessful && response.body() != null) {
                    val fileName = "attendance_${user.name.replace(" ", "_")}_${timestamp()}.csv"
                    saveFile(response.body()!!.bytes(), fileName, "text/csv")
                    _exportStatus.value = ExportStatus("✅ ${user.name}'s CSV downloaded", true)
                } else {
                    _exportStatus.value = ExportStatus("Export failed: ${response.code()}", false)
                }
            } catch (e: Exception) {
                _exportStatus.value = ExportStatus("Error: ${e.message}", false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun exportUserPDF() {
        val user = _selectedUser.value ?: run {
            _exportStatus.value = ExportStatus("Please select a carpenter first", false)
            return
        }
        viewModelScope.launch {
            _isLoading.value    = true
            _exportStatus.value = null
            try {
                val response = RetrofitClient.adminApi.exportAttendancePDF(
                    startDate  = _startDate.value,
                    endDate    = _endDate.value,
                    employeeId = user.employeeId
                )
                if (response.isSuccessful && response.body() != null) {
                    val fileName = "attendance_${user.name.replace(" ", "_")}_${timestamp()}.pdf"
                    saveFile(response.body()!!.bytes(), fileName, "application/pdf")
                    _exportStatus.value = ExportStatus("✅ ${user.name}'s PDF downloaded", true)
                } else {
                    _exportStatus.value = ExportStatus("Export failed: ${response.code()}", false)
                }
            } catch (e: Exception) {
                _exportStatus.value = ExportStatus("Error: ${e.message}", false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── File save helper ──────────────────────────────────────────

    private fun saveFile(bytes: ByteArray, fileName: String, mimeType: String) {
        val context = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE,    mimeType)
                put(MediaStore.Downloads.IS_PENDING,   1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                resolver.openOutputStream(it)?.use { os -> os.write(bytes) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(it, values, null, null)
            }
        } else {
            val dir  = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(dir, fileName)
            file.writeBytes(bytes)
        }
    }

    private fun timestamp() =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
}