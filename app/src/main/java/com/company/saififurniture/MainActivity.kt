package com.saififurnitures.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.saififurnitures.app.data.remote.RetrofitClient
import com.saififurnitures.app.data.session.SessionManager
import com.saififurnitures.app.navigation.AppNavGraph
import com.saififurnitures.app.ui.theme.SaifiFurnituresTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restore auth token on launch
        val sessionManager = SessionManager(this)
        RetrofitClient.setAuthToken(sessionManager.getToken())

        setContent {
            SaifiFurnituresTheme {
                AppNavGraph()
            }
        }
    }
}