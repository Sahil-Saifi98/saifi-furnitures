package com.saififurnitures.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.saififurnitures.app.data.session.SessionManager
import com.saififurnitures.app.ui.admin.AdminAttendanceScreen
import com.saififurnitures.app.ui.admin.AdminDashboardScreen
import com.saififurnitures.app.ui.admin.AdminExportScreen
import com.saififurnitures.app.ui.attendance.AttendanceScreen
import com.saififurnitures.app.ui.login.LoginScreen
import com.saififurnitures.app.ui.profile.ProfileScreen
import com.saififurnitures.app.ui.attendance.HistoryScreen
import com.saififurnitures.app.ui.admin.AdminUsersScreen
import com.saififurnitures.app.ui.admin.AdminCarpenterDetailScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val context       = LocalContext.current
    val session       = SessionManager(context)

    val startDestination = when {
        !session.isLoggedIn()          -> NavRoutes.Login.route
        session.getRole() == "admin"   -> NavRoutes.AdminDashboard.route
        else                           -> NavRoutes.Attendance.route
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(NavRoutes.Login.route) {
            LoginScreen(
                onLoginSuccess = { isAdmin ->
                    val dest = if (isAdmin) NavRoutes.AdminDashboard.route
                    else        NavRoutes.Attendance.route
                    navController.navigate(dest) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.Attendance.route) {
            AttendanceScreen(navController = navController)
        }

        composable(NavRoutes.AdminDashboard.route) {
            AdminDashboardScreen(navController = navController)
        }

        composable(NavRoutes.AdminAttendance.route) {
            AdminAttendanceScreen(navController = navController)
        }

        composable(NavRoutes.AdminExport.route) {
            AdminExportScreen(navController = navController)
        }
        composable(NavRoutes.Profile.route) {
            ProfileScreen(navController = navController)
        }

                composable(NavRoutes.History.route) {
                    HistoryScreen(navController = navController)
                }


                composable(NavRoutes.AdminUsers.route) {
                    AdminUsersScreen(navController = navController)
                }
        composable(NavRoutes.AdminCarpenterDetail.route) {
            AdminCarpenterDetailScreen(navController = navController)
        }
    }
}