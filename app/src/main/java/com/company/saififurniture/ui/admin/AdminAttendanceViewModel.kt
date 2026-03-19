package com.saififurnitures.app.ui.admin

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saififurnitures.app.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class AdminSessionRow(
    val id: String,
    val employeeName: String,
    val employeeId: String,
    val date: String,
    val checkInTime: String,
    val checkOutTime: String?,
    val checkInAddress: String,
    val checkOutAddress: String?,
    val checkInSelfieUrl: String,
    val checkOutSelfieUrl: String?,
    val checkInLat: Double,
    val checkInLng: Double,
    val isSynced: Boolean
)

class AdminAttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val _sessionList  = MutableStateFlow<List<AdminSessionRow>>(emptyList())
    val sessionList: StateFlow<List<AdminSessionRow>> = _sessionList

    private val _allData      = MutableStateFlow<List<AdminSessionRow>>(emptyList())

    private val _isLoading    = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _selectedDate = MutableStateFlow(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    )
    val selectedDate: StateFlow<String> = _selectedDate

    private val _searchQuery  = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    init { loadAttendance() }

    fun loadAttendance() {
        viewModelScope.launch {
            _isLoading.value    = true
            _errorMessage.value = null
            try {
                val startDate = if (_searchQuery.value.isEmpty()) _selectedDate.value else null
                val endDate   = if (_searchQuery.value.isEmpty()) _selectedDate.value else null

                val response = RetrofitClient.adminApi.getAllAttendance(startDate, endDate)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success) {
                        val rows = body.data.map { item ->
                            AdminSessionRow(
                                id               = item._id,
                                employeeName     = item.userId.name,
                                employeeId       = item.userId.employeeId,
                                date             = item.date,
                                checkInTime      = convertUtcToIst(item.checkInTime),
                                checkOutTime     = item.checkOutTime?.let { convertUtcToIst(it) },
                                checkInAddress   = item.checkInAddress,
                                checkOutAddress  = item.checkOutAddress,
                                checkInSelfieUrl = item.checkInSelfieUrl ?: "",
                                checkOutSelfieUrl= item.checkOutSelfieUrl,
                                checkInLat       = item.checkInLatitude,
                                checkInLng       = item.checkInLongitude,
                                isSynced         = item.isSynced
                            )
                        }.sortedByDescending { it.date + it.checkInTime }

                        _allData.value = rows
                        applyFilters()
                        Log.d("AdminVM", "Loaded ${rows.size} rows")
                    } else {
                        _errorMessage.value = body.message ?: "Failed to load"
                    }
                } else {
                    _errorMessage.value = "Server error: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
                Log.e("AdminVM", "Exception: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onDateSelected(date: String) {
        _selectedDate.value = date
        if (_searchQuery.value.isEmpty()) loadAttendance()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.length >= 2 || query.isEmpty()) loadAttendance()
        else applyFilters()
    }

    private fun applyFilters() {
        val q = _searchQuery.value.lowercase()
        _sessionList.value = if (q.isEmpty()) _allData.value
        else _allData.value.filter {
            it.employeeName.lowercase().contains(q) ||
                    it.employeeId.lowercase().contains(q)   ||
                    it.checkInAddress.lowercase().contains(q)
        }
    }

    private fun convertUtcToIst(utc: String): String {
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
            val date = fmt.parse(utc)
            SimpleDateFormat("hh:mm a", Locale.getDefault())
                .apply { timeZone = TimeZone.getTimeZone("Asia/Kolkata") }
                .format(date!!)
        } catch (_: Exception) { utc }
    }
}