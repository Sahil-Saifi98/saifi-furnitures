package com.saififurnitures.app.data.session

import android.content.Context
import android.content.SharedPreferences
import com.saififurnitures.app.data.remote.RetrofitClient

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    init {
        val token = prefs.getString(KEY_TOKEN, null)
        if (token != null) RetrofitClient.setAuthToken(token)
    }

    companion object {
        private const val PREF_NAME   = "SaifiSession"
        private const val KEY_LOGGED  = "isLoggedIn"
        private const val KEY_TOKEN   = "token"
        private const val KEY_USER_ID = "userId"
        private const val KEY_EMP_ID  = "employeeId"
        private const val KEY_NAME    = "name"
        private const val KEY_EMAIL   = "email"
        private const val KEY_ROLE    = "role"
    }

    fun saveLoginSession(
        token: String,
        userId: String,
        employeeId: String,
        name: String,
        email: String,
        role: String
    ) {
        prefs.edit().apply {
            putBoolean(KEY_LOGGED,  true)
            putString(KEY_TOKEN,    token)
            putString(KEY_USER_ID,  userId)
            putString(KEY_EMP_ID,   employeeId)
            putString(KEY_NAME,     name)
            putString(KEY_EMAIL,    email)
            putString(KEY_ROLE,     role)
            apply()
        }
        RetrofitClient.setAuthToken(token)
    }

    fun isLoggedIn()    = prefs.getBoolean(KEY_LOGGED,  false)
    fun getToken()      = prefs.getString(KEY_TOKEN,    null)
    fun getUserId()     = prefs.getString(KEY_USER_ID,  null)
    fun getEmployeeId() = prefs.getString(KEY_EMP_ID,   null)
    fun getName()       = prefs.getString(KEY_NAME,     null)
    fun getEmail()      = prefs.getString(KEY_EMAIL,    null)
    fun getRole()       = prefs.getString(KEY_ROLE,     null)

    fun logout() {
        prefs.edit().clear().apply()
        RetrofitClient.setAuthToken(null)
    }

    fun getCurrentUser(): UserSession? {
        if (!isLoggedIn()) return null
        return UserSession(
            userId     = getUserId()     ?: return null,
            employeeId = getEmployeeId() ?: return null,
            name       = getName()       ?: return null,
            email      = getEmail()      ?: return null,
            role       = getRole()       ?: "carpenter"
        )
    }
}

data class UserSession(
    val userId: String,
    val employeeId: String,
    val name: String,
    val email: String,
    val role: String
)