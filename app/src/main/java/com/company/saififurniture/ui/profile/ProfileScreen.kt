package com.saififurnitures.app.ui.profile

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.saififurnitures.app.navigation.NavRoutes
import com.saififurnitures.app.ui.theme.*

@Composable
fun ProfileScreen(
    navController: NavHostController,
    viewModel: ProfileViewModel = viewModel()
) {
    val profile           by viewModel.profile.collectAsState()
    val stats             by viewModel.stats.collectAsState()
    var showLogoutDialog  by remember { mutableStateOf(false) }
    val scrollState       = rememberScrollState()

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text("Sign Out", fontWeight = FontWeight.Bold, color = TextDark)
            },
            text = {
                Text("Are you sure you want to sign out?", color = TextMid)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.signOut()
                        navController.navigate(NavRoutes.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ) {
                    Text("Sign Out", color = AccentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = TextMid)
                }
            },
            containerColor = BgCard
        )
    }

    Scaffold(
        containerColor = BgMain,
        bottomBar = {
            NavigationBar(
                containerColor = BgCard,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = false,
                    onClick  = {
                        navController.navigate(NavRoutes.Attendance.route) {
                            popUpTo(NavRoutes.Attendance.route) { inclusive = true }
                        }
                    },
                    icon  = { Icon(Icons.Default.AccessTime, "Attendance") },
                    label = { Text("Attendance") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor   = WoodMid,
                        selectedTextColor   = WoodMid,
                        indicatorColor      = WoodCream
                    )
                )
                NavigationBarItem(
                    selected = true,
                    onClick  = { },
                    icon  = { Icon(Icons.Default.Person, "Profile") },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor   = WoodMid,
                        selectedTextColor   = WoodMid,
                        indicatorColor      = WoodCream
                    )
                )
            }
        }
    ) { padding ->
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
                            end    = Offset(1000f, 400f)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(AccentGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .background(WoodDark, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text  = profile.initials.ifEmpty { "C" },
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = AccentGold
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text  = profile.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(Modifier.height(4.dp))

                    Surface(
                        color = AccentGold.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text     = "Carpenter",
                            style    = MaterialTheme.typography.labelMedium,
                            color    = AccentGold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Today's Stats ─────────────────────────────────────
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
                    "Today's Summary",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = TextDark
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    label    = "Check-ins",
                    value    = stats.checkIns.toString(),
                    icon     = Icons.Default.Login,
                    color    = AccentGreen,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label    = "Check-outs",
                    value    = stats.checkOuts.toString(),
                    icon     = Icons.Default.Logout,
                    color    = AccentRed,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label    = "Sites Done",
                    value    = stats.sitesCompleted.toString(),
                    icon     = Icons.Default.Handyman,
                    color    = AccentGold,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Info Card ─────────────────────────────────────────
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
                    "Account Details",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = TextDark
                )
            }

            Spacer(Modifier.height(12.dp))

            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = BgCard),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    InfoRow(
                        icon  = Icons.Default.Badge,
                        label = "Carpenter ID",
                        value = profile.employeeId
                    )
                    HorizontalDivider(color = DividerWood, thickness = 1.dp)
                    InfoRow(
                        icon  = Icons.Default.Email,
                        label = "Email",
                        value = profile.email.ifEmpty { "—" }
                    )
                    HorizontalDivider(color = DividerWood, thickness = 1.dp)
                    InfoRow(
                        icon  = Icons.Default.Carpenter,
                        label = "Role",
                        value = "Carpenter"
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Sign Out Button ───────────────────────────────────
            Button(
                onClick  = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(54.dp),
                shape  = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
            ) {
                Icon(Icons.Default.ExitToApp, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Sign Out",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text      = "Saifi Furnitures v1.0",
                modifier  = Modifier.fillMaxWidth(),
                style     = MaterialTheme.typography.bodySmall,
                color     = TextLight,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Stat card ─────────────────────────────────────────────────────

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = color
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = TextLight
            )
        }
    }
}

// ── Info row ──────────────────────────────────────────────────────

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(WoodCream, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = WoodMid, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextLight)
            Text(
                value,
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color      = TextDark
            )
        }
    }
}