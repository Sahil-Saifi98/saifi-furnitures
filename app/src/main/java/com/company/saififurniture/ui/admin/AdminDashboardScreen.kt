package com.saififurnitures.app.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.saififurnitures.app.data.session.SessionManager
import com.saififurnitures.app.navigation.NavRoutes
import com.saififurnitures.app.ui.theme.*

@Composable
fun AdminDashboardScreen(navController: NavHostController) {
    val context        = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    var showLogout     by remember { mutableStateOf(false) }
    val scrollState    = rememberScrollState()

    Scaffold(containerColor = BgMain) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // ── Header ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(WoodDark, WoodMid),
                            start  = Offset(0f, 0f),
                            end    = Offset(1000f, 500f)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 28.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.Top
                    ) {
                        Column {
                            Text(
                                "Admin Panel",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WoodWarm.copy(alpha = 0.8f)
                            )
                            Text(
                                "Rajuddin Furnitures",
                                style      = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White
                            )
                        }

                        // Logout button
                        Box {
                            Row(
                                modifier = Modifier
                                    .clickable { showLogout = true }
                                    .background(
                                        Color.White.copy(alpha = 0.12f),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AdminPanelSettings, null,
                                    tint     = AccentGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Admin",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }

                            DropdownMenu(
                                expanded         = showLogout,
                                onDismissRequest = { showLogout = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Logout, null,
                                                tint     = AccentRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text("Logout", color = AccentRed)
                                        }
                                    },
                                    onClick = {
                                        showLogout = false
                                        sessionManager.logout()
                                        navController.navigate(NavRoutes.Login.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🪵", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Manage your carpenter workforce",
                            style = MaterialTheme.typography.bodySmall,
                            color = WoodWarm.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Section title ─────────────────────────────────────
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(20.dp)
                        .background(AccentGold, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Quick Actions",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = TextDark
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── Row 1: Attendance + Carpenters ────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminActionCard(
                    title    = "Attendance",
                    subtitle = "View all logs",
                    icon     = Icons.Default.PeopleAlt,
                    color    = WoodMid,
                    modifier = Modifier.weight(1f),
                    onClick  = { navController.navigate(NavRoutes.AdminAttendance.route) }
                )
                AdminActionCard(
                    title    = "Carpenters",
                    subtitle = "Manage team",
                    icon     = Icons.Default.Group,
                    color    = AccentGreen,
                    modifier = Modifier.weight(1f),
                    onClick  = { navController.navigate(NavRoutes.AdminUsers.route) }
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Row 2: Export (centered, half width) ──────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminActionCard(
                    title    = "Export",
                    subtitle = "Download records",
                    icon     = Icons.Default.Download,
                    color    = AccentGoldDark,
                    modifier = Modifier.weight(1f),
                    onClick  = { navController.navigate(NavRoutes.AdminExport.route) }
                )
                // Empty spacer card to keep alignment
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // ── Info card ─────────────────────────────────────────
            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = WoodCream),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(WoodMid.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Carpenter, null,
                            tint     = WoodMid,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "Rajuddin Furnitures",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color      = TextDark
                        )
                        Text(
                            "Carpenter Attendance System • v1.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLight
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AdminActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier  = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, null,
                    tint     = color,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                title,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = TextDark
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextLight
            )
        }
    }
}