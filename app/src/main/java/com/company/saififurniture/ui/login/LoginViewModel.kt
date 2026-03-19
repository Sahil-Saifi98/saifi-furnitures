package com.saififurnitures.app.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saififurnitures.app.data.remote.LoginRequest
import com.saififurnitures.app.data.remote.RetrofitClient
import com.saififurnitures.app.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
            try {
                val response = RetrofitClient.authApi.login(
                    LoginRequest(
                        employeeId = _employeeId.value.trim(),
                        password   = _password.value
                    )
                )
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success && body.user != null && body.token != null) {
                        sessionManager.saveLoginSession(
                            token      = body.token,
                            userId     = body.user.id,
                            employeeId = body.user.employeeId,
                            name       = body.user.name,
                            email      = body.user.email,
                            role       = body.user.role
                        )
                        _isAdmin.value      = body.user.role == "admin"
                        _loginSuccess.value = true
                    } else {
                        _errorMessage.value = body.message ?: "Login failed"
                    }
                } else {
                    _errorMessage.value = "Invalid ID or Password"
                }
            } catch (e: Exception) {
                _errorMessage.value = when {
                    e.message?.contains("Unable to resolve host") == true ->
                        "No internet connection. Please try again."
                    e.message?.contains("timeout") == true ->
                        "Connection timed out. Please try again."
                    else ->
                        "Error: ${e.message ?: "Something went wrong"}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}