package com.saififurnitures.app.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saififurnitures.app.data.session.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ── Hardcoded test users (remove when backend is ready) ───────────
private data class TestUser(
    val employeeId: String,
    val password: String,
    val name: String,
    val email: String,
    val role: String
)

private val TEST_USERS = listOf(
    TestUser("ADMIN-001",  "admin123",  "Admin Saifi",    "admin@saifi.com",     "admin"),
    TestUser("SAIFI-001",  "saifi001",  "Raza Carpenter", "raza@saifi.com",      "carpenter"),
    TestUser("SAIFI-002",  "saifi002",  "Ali Carpenter",  "ali@saifi.com",       "carpenter"),
    TestUser("SAIFI-003",  "saifi003",  "Usman Carpenter","usman@saifi.com",     "carpenter")
)
// ─────────────────────────────────────────────────────────────────

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)

    private val _employeeId   = MutableStateFlow("")
    val employeeId: StateFlow<String> = _employeeId

    private val _password     = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _isLoading    = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess

    private val _isAdmin      = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin

    fun onEmployeeIdChange(v: String) { _employeeId.value = v; _errorMessage.value = null }
    fun onPasswordChange(v: String)   { _password.value   = v; _errorMessage.value = null }

    fun login() {
        viewModelScope.launch {
            _isLoading.value    = true
            _errorMessage.value = null

            // Simulate small network delay
            delay(600)

            val id   = _employeeId.value.trim()
            val pass = _password.value

            val match = TEST_USERS.find {
                it.employeeId.equals(id, ignoreCase = true) && it.password == pass
            }

            if (match != null) {
                sessionManager.saveLoginSession(
                    token      = "test_token_${match.employeeId}",
                    userId     = match.employeeId,
                    employeeId = match.employeeId,
                    name       = match.name,
                    email      = match.email,
                    role       = match.role
                )
                _isAdmin.value      = match.role == "admin"
                _loginSuccess.value = true
            } else {
                _errorMessage.value = "Invalid ID or Password"
            }

            _isLoading.value = false
        }
    }
}