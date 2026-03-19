package com.saififurnitures.app.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saififurnitures.app.data.local.AppDatabase
import com.saififurnitures.app.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CarpenterProfile(
    val name: String = "",
    val employeeId: String = "",
    val email: String = "",
    val role: String = "",
    val initials: String = ""
)

data class CarpenterStats(
    val checkIns: Int = 0,
    val checkOuts: Int = 0,
    val sitesCompleted: Int = 0
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val dao = AppDatabase.getDatabase(application).attendanceDao()

    private val _profile = MutableStateFlow(CarpenterProfile())
    val profile: StateFlow<CarpenterProfile> = _profile

    private val _stats = MutableStateFlow(CarpenterStats())
    val stats: StateFlow<CarpenterStats> = _stats

    init {
        loadProfile()
        loadStats()
    }

    private fun loadProfile() {
        val session = sessionManager.getCurrentUser() ?: return
        val initials = session.name
            .split(" ")
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .take(2)
            .joinToString("")

        _profile.value = CarpenterProfile(
            name       = session.name,
            employeeId = session.employeeId,
            email      = session.email,
            role       = session.role,
            initials   = initials
        )
    }

    private fun loadStats() {
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            try {
                val today      = dao.getTodayAttendance(userId)
                val checkIns   = today.count { it.type == "check_in" }
                val checkOuts  = today.count { it.type == "check_out" }
                _stats.value   = CarpenterStats(
                    checkIns       = checkIns,
                    checkOuts      = checkOuts,
                    sitesCompleted = checkOuts
                )
            } catch (_: Exception) { }
        }
    }

    fun signOut() {
        sessionManager.logout()
    }
}