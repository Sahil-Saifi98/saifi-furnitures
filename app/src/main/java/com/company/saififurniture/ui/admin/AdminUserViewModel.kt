package com.saififurnitures.app.ui.admin

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saififurnitures.app.data.remote.AdminUser
import com.saififurnitures.app.data.remote.RegisterCarpenterRequest
import com.saififurnitures.app.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class UserActionStatus(val message: String, val isSuccess: Boolean)

class AdminUsersViewModel(application: Application) : AndroidViewModel(application) {

    private val _users        = MutableStateFlow<List<AdminUser>>(emptyList())
    val users: StateFlow<List<AdminUser>> = _users

    private val _isLoading    = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _actionStatus = MutableStateFlow<UserActionStatus?>(null)
    val actionStatus: StateFlow<UserActionStatus?> = _actionStatus

    // Add form fields
    private val _newName       = MutableStateFlow("")
    val newName: StateFlow<String> = _newName

    private val _newEmployeeId = MutableStateFlow("")
    val newEmployeeId: StateFlow<String> = _newEmployeeId

    private val _newEmail      = MutableStateFlow("")
    val newEmail: StateFlow<String> = _newEmail

    private val _newPassword   = MutableStateFlow("")
    val newPassword: StateFlow<String> = _newPassword

    private val _isAdding      = MutableStateFlow(false)
    val isAdding: StateFlow<Boolean> = _isAdding

    init { loadUsers() }

    fun onNameChange(v: String)       { _newName.value       = v }
    fun onEmployeeIdChange(v: String) { _newEmployeeId.value = v }
    fun onEmailChange(v: String)      { _newEmail.value      = v }
    fun onPasswordChange(v: String)   { _newPassword.value   = v }
    fun clearActionStatus()           { _actionStatus.value  = null }

    fun loadUsers() {
        viewModelScope.launch {
            _isLoading.value    = true
            _errorMessage.value = null
            try {
                val response = RetrofitClient.adminApi.getAllUsers()
                if (response.isSuccessful && response.body()?.success == true) {
                    _users.value = response.body()!!.data
                        .filter { it.role == "carpenter" }
                        .sortedBy { it.name }
                } else {
                    _errorMessage.value = "Failed to load carpenters"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
                Log.e("AdminUsersVM", "loadUsers: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addCarpenter() {
        val name  = _newName.value.trim()
        val empId = _newEmployeeId.value.trim()
        val email = _newEmail.value.trim()
        val pass  = _newPassword.value

        if (name.isBlank() || empId.isBlank() || pass.isBlank()) {
            _actionStatus.value = UserActionStatus("Name, ID and Password are required", false)
            return
        }

        viewModelScope.launch {
            _isAdding.value = true
            try {
                val response = RetrofitClient.adminApi.addCarpenter(
                    RegisterCarpenterRequest(
                        employeeId = empId,
                        name       = name,
                        email      = email,
                        password   = pass,
                        role       = "carpenter"
                    )
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    _actionStatus.value = UserActionStatus("✅ ${name} added successfully", true)
                    clearForm()
                    loadUsers()
                } else {
                    _actionStatus.value = UserActionStatus(
                        response.body()?.message ?: "Failed to add carpenter", false
                    )
                }
            } catch (e: Exception) {
                _actionStatus.value = UserActionStatus("Error: ${e.message}", false)
                Log.e("AdminUsersVM", "addCarpenter: ${e.message}")
            } finally {
                _isAdding.value = false
            }
        }
    }

    fun deleteCarpenter(user: AdminUser) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.adminApi.deleteCarpenter(user._id)
                if (response.isSuccessful && response.body()?.success == true) {
                    _actionStatus.value = UserActionStatus("✅ ${user.name} removed", true)
                    loadUsers()
                } else {
                    _actionStatus.value = UserActionStatus(
                        response.body()?.message ?: "Failed to remove", false
                    )
                }
            } catch (e: Exception) {
                _actionStatus.value = UserActionStatus("Error: ${e.message}", false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun clearForm() {
        _newName.value       = ""
        _newEmployeeId.value = ""
        _newEmail.value      = ""
        _newPassword.value   = ""
    }
}