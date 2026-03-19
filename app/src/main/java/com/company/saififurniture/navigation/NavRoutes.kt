package com.saififurnitures.app.navigation

sealed class NavRoutes(val route: String) {
    object Login           : NavRoutes("login")
    object Attendance      : NavRoutes("attendance")
    object AdminDashboard  : NavRoutes("admin_dashboard")
    object AdminAttendance : NavRoutes("admin_attendance")
    object AdminExport     : NavRoutes("admin_export")
    object Profile : NavRoutes("profile")
    object History : NavRoutes("history")
    object AdminUsers : NavRoutes("admin_users")
    object AdminCarpenterDetail : NavRoutes("admin_carpenter/{employeeId}/{carpenterName}") {
        fun createRoute(employeeId: String, name: String) =
            "admin_carpenter/$employeeId/${name.replace(" ", "_")}"
    }
}