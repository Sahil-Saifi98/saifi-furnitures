package com.saififurnitures.app.ui.attendance

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.saififurnitures.app.data.local.AttendanceEntity
import com.saififurnitures.app.data.session.SessionManager
import com.saififurnitures.app.navigation.NavRoutes
import com.saififurnitures.app.ui.theme.*
import com.saififurnitures.app.utils.LocationUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AttendanceScreen(
    navController: NavHostController,
    viewModel: AttendanceViewModel = viewModel()
) {
    val context         = LocalContext.current
    val sessionManager  = remember { SessionManager(context) }
    val attendanceState by viewModel.attendanceState.collectAsState()
    val status          by viewModel.status.collectAsState()
    val todayRecords    by viewModel.todayRecords.collectAsState()
    val pendingCount    by viewModel.pendingCount.collectAsState()
    val openSession     by viewModel.openSession.collectAsState()

    var cameraAction by remember { mutableStateOf("check_in") }
    var showCamera   by remember { mutableStateOf(false) }

    val carpenterName = sessionManager.getName() ?: "Carpenter"
    val today = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            LocationUtils.getCurrentLocation(context,
                onLocationReceived = { lat, lng -> viewModel.onLocationReceived(lat, lng) },
                onError            = { viewModel.onLocationError(it) }
            )
        } else {
            viewModel.onLocationError("Location permission denied")
        }
    }

    fun requestLocation() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            LocationUtils.getCurrentLocation(context,
                onLocationReceived = { lat, lng -> viewModel.onLocationReceived(lat, lng) },
                onError            = { viewModel.onLocationError(it) }
            )
        } else {
            locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Show camera full screen
    if (showCamera) {
        val isCheckIn = cameraAction == "check_in"
        CameraPreviewScreen(
            actionLabel = if (isCheckIn) "Capture & Check In" else "Capture & Check Out",
            actionColor = if (isCheckIn) AccentGreen else AccentRed,
            onImageCaptured = { file ->
                if (isCheckIn) viewModel.onCheckInSelfieCaptured(file)
                else           viewModel.onCheckOutSelfieCaptured(file)
                showCamera = false
                requestLocation()
            },
            onError = { err ->
                viewModel.onCameraError(err)
                showCamera = false
            }
        )
        return
    }

    Scaffold(
        containerColor = BgMain,
        bottomBar = {
            NavigationBar(
                containerColor = BgCard,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick  = { },
                    icon     = { Icon(Icons.Default.AccessTime, null) },
                    label    = { Text("Attendance") },
                    colors   = NavigationBarItemDefaults.colors(
                        selectedIconColor = WoodMid,
                        selectedTextColor = WoodMid,
                        indicatorColor    = WoodCream
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick  = { navController.navigate(NavRoutes.History.route) },
                    icon     = { Icon(Icons.Default.History, null) },
                    label    = { Text("History") },
                    colors   = NavigationBarItemDefaults.colors(
                        selectedIconColor = WoodMid,
                        selectedTextColor = WoodMid,
                        indicatorColor    = WoodCream
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick  = { navController.navigate(NavRoutes.Profile.route) },
                    icon     = { Icon(Icons.Default.Person, null) },
                    label    = { Text("Profile") },
                    colors   = NavigationBarItemDefaults.colors(
                        selectedIconColor = WoodMid,
                        selectedTextColor = WoodMid,
                        indicatorColor    = WoodCream
                    )
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // Header
            item {
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
                        .padding(horizontal = 20.dp, vertical = 28.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    "Welcome back,",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = WoodWarm.copy(alpha = 0.8f)
                                )
                                Text(
                                    carpenterName,
                                    style      = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color.White
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(AccentGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text       = carpenterName.firstOrNull()?.uppercase() ?: "C",
                                    style      = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color      = WoodDark
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarToday, null,
                                tint     = AccentGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                today,
                                style = MaterialTheme.typography.bodySmall,
                                color = WoodWarm.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Status card
            item {
                Spacer(Modifier.height(16.dp))
                StatusCard(
                    attendanceState = attendanceState,
                    openSession     = openSession,
                    todayRecords    = todayRecords,
                    onCheckIn  = { cameraAction = "check_in";  showCamera = true },
                    onCheckOut = { cameraAction = "check_out"; showCamera = true }
                )
            }

            // Pending sync banner
            if (pendingCount > 0) {
                item {
                    Spacer(Modifier.height(12.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape  = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3DC))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CloudOff, null,
                                    tint     = Color(0xFFB8892A),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "$pendingCount record(s) pending sync",
                                        style      = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = Color(0xFF7A5500)
                                    )
                                    Text(
                                        "Tap to sync when online",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFB8892A)
                                    )
                                }
                            }
                            TextButton(
                                onClick = { viewModel.syncNow() },
                                colors  = ButtonDefaults.textButtonColors(contentColor = WoodMid)
                            ) {
                                Icon(Icons.Default.Sync, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Sync", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Status message banner
            if (status.startsWith("✅") || status.startsWith("⚠️")) {
                item {
                    Spacer(Modifier.height(10.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape  = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (status.startsWith("✅"))
                                Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                        )
                    ) {
                        Text(
                            text     = status,
                            modifier = Modifier.padding(14.dp),
                            style    = MaterialTheme.typography.bodyMedium,
                            color    = if (status.startsWith("✅"))
                                Color(0xFF2E7D32) else Color(0xFFE65100)
                        )
                    }
                }
            }

            // Today's site log title
            item {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
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
                        "Today's Site Log",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = TextDark
                    )
                    Spacer(Modifier.weight(1f))
                    Surface(
                        color = WoodCream,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            "${todayRecords.size} entries",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = WoodMid,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Empty state
            if (todayRecords.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape  = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🪚", fontSize = 40.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No site visits recorded today",
                                style     = MaterialTheme.typography.bodyLarge,
                                color     = TextMid,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Check in when you arrive at your first site",
                                style     = MaterialTheme.typography.bodySmall,
                                color     = TextLight,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                val grouped = todayRecords.groupBy { it.sessionId }
                itemsIndexed(grouped.entries.toList()) { index, (_, records) ->
                    val checkIn  = records.find { it.type == "check_in" }
                    val checkOut = records.find { it.type == "check_out" }
                    SessionHistoryCard(
                        index    = index + 1,
                        checkIn  = checkIn,
                        checkOut = checkOut
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

// ── Status Card ───────────────────────────────────────────────────

@Composable
private fun StatusCard(
    attendanceState: AttendanceState,
    openSession: AttendanceEntity?,
    todayRecords: List<AttendanceEntity>,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit
) {
    val isCheckedIn       = attendanceState == AttendanceState.CHECKED_IN
    val isProcessing      = attendanceState == AttendanceState.PROCESSING
    val sessionsCompleted = todayRecords.count { it.type == "check_out" }
    val checkInTime = openSession?.let {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(it.timestamp))
    }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(
                        if (isCheckedIn) AccentGreen.copy(alpha = 0.12f) else WoodCream,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(
                            if (isCheckedIn) AccentGreen.copy(alpha = 0.2f) else WoodWarm,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(44.dp),
                            color       = WoodMid,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector        = if (isCheckedIn) Icons.Default.Handyman else Icons.Default.Carpenter,
                            contentDescription = null,
                            tint               = if (isCheckedIn) AccentGreen else WoodMid,
                            modifier           = Modifier.size(48.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = when (attendanceState) {
                    AttendanceState.READY_TO_CHECK_IN -> "Ready for Work"
                    AttendanceState.CHECKED_IN        -> "Currently On Site"
                    AttendanceState.PROCESSING        -> "Processing…"
                },
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = if (isCheckedIn) AccentGreen else TextDark
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = when {
                    isCheckedIn && checkInTime != null ->
                        "Checked in at $checkInTime"
                    sessionsCompleted > 0              ->
                        "$sessionsCompleted site visit(s) completed today"
                    else                               ->
                        "Tap below to check in at your site"
                },
                style     = MaterialTheme.typography.bodyMedium,
                color     = TextLight,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniStat(
                    label = "Check-ins",
                    value = todayRecords.count { it.type == "check_in" }.toString(),
                    color = AccentGreen
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(DividerWood)
                )
                MiniStat(
                    label = "Check-outs",
                    value = todayRecords.count { it.type == "check_out" }.toString(),
                    color = AccentRed
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(DividerWood)
                )
                MiniStat(
                    label = "Sites Done",
                    value = sessionsCompleted.toString(),
                    color = AccentGold
                )
            }

            Spacer(Modifier.height(24.dp))

            if (!isProcessing) {
                if (!isCheckedIn) {
                    Button(
                        onClick  = onCheckIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape  = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                    ) {
                        Icon(Icons.Default.Login, null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Check In at Site",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick  = onCheckOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape  = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                    ) {
                        Icon(Icons.Default.Logout, null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Check Out from Site",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Check out before moving to your next site",
                        style     = MaterialTheme.typography.bodySmall,
                        color     = TextLight,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ── Session history card ──────────────────────────────────────────

@Composable
private fun SessionHistoryCard(
    index: Int,
    checkIn: AttendanceEntity?,
    checkOut: AttendanceEntity?
) {
    val timeFmt      = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val checkInTime  = checkIn?.let  { timeFmt.format(Date(it.timestamp)) } ?: "--"
    val checkOutTime = checkOut?.let { timeFmt.format(Date(it.timestamp)) } ?: "In Progress"
    val isOpen       = checkOut == null

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (isOpen) AccentGold.copy(alpha = 0.15f) else WoodCream,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "S$index",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = if (isOpen) AccentGoldDark else WoodMid
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Site Visit $index",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextDark
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(AccentGreen, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "In:  $checkInTime",
                        style      = MaterialTheme.typography.bodySmall,
                        color      = AccentGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (isOpen) TextLight else AccentRed, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Out: $checkOutTime",
                        style      = MaterialTheme.typography.bodySmall,
                        color      = if (isOpen) TextLight else AccentRed,
                        fontWeight = if (isOpen) FontWeight.Normal else FontWeight.Medium
                    )
                }
                val addr = checkIn?.address?.takeIf { !it.startsWith("Location:") }
                if (addr != null) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn, null,
                            tint     = TextLight,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(addr, style = MaterialTheme.typography.bodySmall, color = TextLight)
                    }
                }
            }

            val synced = checkIn?.isSynced == true && (checkOut == null || checkOut.isSynced)
            Surface(
                color = if (synced) AccentGreen.copy(alpha = 0.12f) else Color(0xFFFFF3DC),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(if (synced) AccentGreen else AccentGoldDark, CircleShape)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (synced) "Synced" else "Pending",
                        style      = MaterialTheme.typography.labelSmall,
                        color      = if (synced) AccentGreen else AccentGoldDark,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}


@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextLight)
    }
}