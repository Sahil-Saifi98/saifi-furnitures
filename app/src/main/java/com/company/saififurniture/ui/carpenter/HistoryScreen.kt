package com.saififurnitures.app.ui.attendance

import android.app.DatePickerDialog
import android.app.Application
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.saififurnitures.app.data.local.AppDatabase
import com.saififurnitures.app.data.local.AttendanceEntity
import com.saififurnitures.app.data.session.SessionManager
import com.saififurnitures.app.data.remote.RetrofitClient
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import com.saififurnitures.app.navigation.NavRoutes
import com.saififurnitures.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── ViewModel ─────────────────────────────────────────────────────

// History session data class (from API)
data class HistorySession(
    val sessionId: String,
    val date: String,
    val checkInTime: String?,
    val checkOutTime: String?,
    val checkInAddress: String,
    val checkOutAddress: String?,
    val checkInSelfieUrl: String?,
    val checkOutSelfieUrl: String?,
    val isSynced: Boolean
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val dao            = AppDatabase.getDatabase(application).attendanceDao()
    private val dateFmt        = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _sessions  = MutableStateFlow<List<HistorySession>>(emptyList())
    val sessions: StateFlow<List<HistorySession>> = _sessions

    // Keep records for backward compat with grouping logic
    private val _records   = MutableStateFlow<List<AttendanceEntity>>(emptyList())
    val records: StateFlow<List<AttendanceEntity>> = _records

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _startDate = MutableStateFlow(dateFmt.format(Date()))
    val startDate: StateFlow<String> = _startDate

    private val _endDate   = MutableStateFlow(dateFmt.format(Date()))
    val endDate: StateFlow<String> = _endDate

    init { load() }

    fun setStartDate(date: String) { _startDate.value = date; load() }
    fun setEndDate(date: String)   { _endDate.value   = date; load() }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Use carpenter's own history endpoint — works with carpenter JWT token
                val apiResponse = RetrofitClient.attendanceApi.getAttendanceHistory(
                    startDate = _startDate.value,
                    endDate   = _endDate.value
                )

                if (apiResponse.isSuccessful && apiResponse.body()?.success == true) {
                    val raw = apiResponse.body()!!.data

                    // Pair check-ins and check-outs by sessionId
                    val paired = raw.groupBy { it.sessionId }
                    val apiSessions = paired.values.mapNotNull { recs ->
                        val ci = recs.find { it.type == "check_in" }
                        val co = recs.find { it.type == "check_out" }
                        val date = (ci ?: co)?.date ?: return@mapNotNull null
                        val timeFmt = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                            .apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata") }
                        HistorySession(
                            sessionId        = ci?.sessionId ?: co?.sessionId ?: "",
                            date             = date,
                            checkInTime      = ci?.let { convertUtcToIst(it.timestamp) },
                            checkOutTime     = co?.let { convertUtcToIst(it.timestamp) },
                            checkInAddress   = ci?.address ?: "",
                            checkOutAddress  = co?.address,
                            checkInSelfieUrl = ci?.selfieUrl?.takeIf { it.isNotBlank() },
                            checkOutSelfieUrl= co?.selfieUrl?.takeIf { it.isNotBlank() },
                            isSynced         = ci?.isSynced ?: co?.isSynced ?: true
                        )
                    }.sortedByDescending { it.date + (it.checkInTime ?: "") }
                    _sessions.value = apiSessions
                } else {
                    // Fallback to local DB
                    val userId = sessionManager.getUserId() ?: return@launch
                    _records.value = dao.getAttendanceByRange(userId, _startDate.value, _endDate.value)
                }
            } catch (e: Exception) {
                // Fallback to local DB on network error
                try {
                    val userId = sessionManager.getUserId() ?: return@launch
                    _records.value = dao.getAttendanceByRange(userId, _startDate.value, _endDate.value)
                } catch (_: Exception) { }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun convertUtcToIst(utc: String): String {
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
            val date = fmt.parse(utc)
            SimpleDateFormat("hh:mm a", Locale.getDefault())
                .apply { timeZone = TimeZone.getTimeZone("Asia/Kolkata") }
                .format(date!!)
        } catch (_: Exception) { utc }
    }
}

// ── Screen ────────────────────────────────────────────────────────

@Composable
fun HistoryScreen(
    navController: NavHostController,
    viewModel: HistoryViewModel = viewModel()
) {
    val context      = LocalContext.current
    val apiSessions  by viewModel.sessions.collectAsState()
    val localRecords by viewModel.records.collectAsState()
    val isLoading    by viewModel.isLoading.collectAsState()
    val startDate    by viewModel.startDate.collectAsState()
    val endDate      by viewModel.endDate.collectAsState()
    val dateFmt      = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var fullscreenSelfie by remember { mutableStateOf<String?>(null) }

    // Use API sessions if available, else build from local DB records
    val allSessions: List<HistorySession> = if (apiSessions.isNotEmpty()) {
        apiSessions
    } else {
        localRecords.groupBy { it.sessionId }.values.mapNotNull { recs ->
            val ci = recs.find { it.type == "check_in" }
            val co = recs.find { it.type == "check_out" }
            val date = (ci ?: co)?.let {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.timestamp))
            } ?: return@mapNotNull null
            HistorySession(
                sessionId        = ci?.sessionId ?: co?.sessionId ?: "",
                date             = date,
                checkInTime      = ci?.let { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(it.timestamp)) },
                checkOutTime     = co?.let { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(it.timestamp)) },
                checkInAddress   = ci?.address ?: "",
                checkOutAddress  = co?.address,
                checkInSelfieUrl = ci?.selfieUrl?.takeIf { it.isNotBlank() } ?: ci?.selfiePath?.takeIf { it.isNotBlank() },
                checkOutSelfieUrl= co?.selfieUrl?.takeIf { it.isNotBlank() } ?: co?.selfiePath?.takeIf { it.isNotBlank() },
                isSynced         = ci?.isSynced == true
            )
        }.sortedByDescending { it.date + (it.checkInTime ?: "") }
    }

    val grouped = allSessions.groupBy { it.date }

    Scaffold(
        containerColor = BgMain,
        bottomBar = {
            NavigationBar(containerColor = BgCard, tonalElevation = 8.dp) {
                NavigationBarItem(
                    selected = false,
                    onClick  = {
                        navController.navigate(NavRoutes.Attendance.route) {
                            popUpTo(NavRoutes.Attendance.route) { inclusive = true }
                        }
                    },
                    icon  = { Icon(Icons.Default.AccessTime, null) },
                    label = { Text("Attendance") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = WoodMid, selectedTextColor = WoodMid, indicatorColor = WoodCream
                    )
                )
                NavigationBarItem(
                    selected = true,
                    onClick  = { },
                    icon  = { Icon(Icons.Default.History, null) },
                    label = { Text("History") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = WoodMid, selectedTextColor = WoodMid, indicatorColor = WoodCream
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick  = { navController.navigate(NavRoutes.Profile.route) },
                    icon  = { Icon(Icons.Default.Person, null) },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = WoodMid, selectedTextColor = WoodMid, indicatorColor = WoodCream
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
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
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Text(
                        "Attendance History",
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                    Text(
                        "View your past site visits",
                        style = MaterialTheme.typography.bodySmall,
                        color = WoodWarm.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(16.dp))

                    // Date range row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(context, { _, y, m, d ->
                                    viewModel.setStartDate(dateFmt.format(
                                        Calendar.getInstance().apply { set(y, m, d) }.time))
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border   = androidx.compose.foundation.BorderStroke(1.dp, WoodWarm.copy(0.5f))
                        ) {
                            Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(14.dp), tint = AccentGold)
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text("From", style = MaterialTheme.typography.labelSmall, color = WoodWarm.copy(0.7f))
                                Text(startDate, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(context, { _, y, m, d ->
                                    viewModel.setEndDate(dateFmt.format(
                                        Calendar.getInstance().apply { set(y, m, d) }.time))
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border   = androidx.compose.foundation.BorderStroke(1.dp, WoodWarm.copy(0.5f))
                        ) {
                            Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(14.dp), tint = AccentGold)
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text("To", style = MaterialTheme.typography.labelSmall, color = WoodWarm.copy(0.7f))
                                Text(endDate, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Summary strip
            Surface(color = WoodCream) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${allSessions.size} site visit(s) found",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMid
                    )
                    Text(
                        "${allSessions.count { it.checkInTime != null }} check-ins  •  ${allSessions.count { it.checkOutTime != null }} check-outs",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLight
                    )
                }
            }

            // List
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WoodMid)
                }
            } else if (grouped.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🪚", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No records for this period",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextMid,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Try selecting a different date range",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLight,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val byDate = grouped.entries
                    .sortedByDescending { (date, _) -> date }

                LazyColumn(
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    byDate.forEach { (date, dateSessions) ->
                        item {
                            val displayDate = try {
                                val d = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
                                SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(d!!)
                            } catch (_: Exception) { date }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.width(3.dp).height(16.dp)
                                    .background(AccentGold, RoundedCornerShape(2.dp)))
                                Spacer(Modifier.width(8.dp))
                                Text(displayDate, style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold, color = TextMid)
                                Spacer(Modifier.weight(1f))
                                Surface(color = WoodCream, shape = RoundedCornerShape(20.dp)) {
                                    Text("${dateSessions.size} visit(s)",
                                        style = MaterialTheme.typography.labelSmall, color = WoodMid,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                        }

                        itemsIndexed(dateSessions) { idx, session ->
                            ApiHistorySessionCard(
                                index         = idx + 1,
                                session       = session,
                                onSelfieClick = { url -> fullscreenSelfie = url }
                            )
                        }

                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }

    // Fullscreen selfie viewer
    fullscreenSelfie?.let { url ->
        Dialog(
            onDismissRequest = { fullscreenSelfie = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black)
                    .clickable { fullscreenSelfie = null },
                contentAlignment = Alignment.Center
            ) {
                if (url.startsWith("http")) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(url).crossfade(true).build(),
                        contentDescription = "Selfie",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    val bmp = remember(url) {
                        runCatching {
                            android.graphics.BitmapFactory.decodeFile(url)?.asImageBitmap()
                        }.getOrNull()
                    }
                    if (bmp != null) {
                        Image(bmp, "Selfie",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Fit)
                    }
                }
                IconButton(
                    onClick = { fullscreenSelfie = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White,
                        modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}


// ── API-based history session card ───────────────────────────────

@Composable
private fun ApiHistorySessionCard(
    index: Int,
    session: HistorySession,
    onSelfieClick: (String) -> Unit
) {
    val isOpen  = session.checkOutTime == null
    val synced  = session.isSynced

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {

            // Top row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp)
                        .background(if (isOpen) AccentGold.copy(0.15f) else WoodCream, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("S$index", style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isOpen) AccentGoldDark else WoodMid)
                }
                Spacer(Modifier.width(10.dp))
                Text("Site Visit $index", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = TextDark,
                    modifier = Modifier.weight(1f))
                Surface(
                    color = if (synced) AccentGreen.copy(0.12f) else Color(0xFFFFF3DC),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(if (synced) "✓ Synced" else "Pending",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (synced) AccentGreen else AccentGoldDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            // Selfie thumbnails
            if (session.checkInSelfieUrl != null || session.checkOutSelfieUrl != null) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Check-in" to session.checkInSelfieUrl,
                        "Check-out" to session.checkOutSelfieUrl).forEach { (lbl, src) ->
                        Column(modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(lbl, style = MaterialTheme.typography.labelSmall,
                                color = if (lbl == "Check-in") AccentGreen else AccentRed)
                            Spacer(Modifier.height(3.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth().height(80.dp)
                                    .clip(RoundedCornerShape(8.dp)).background(WoodCream)
                                    .then(if (src != null) Modifier.clickable { onSelfieClick(src) } else Modifier),
                                contentAlignment = Alignment.Center
                            ) {
                                if (src != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(src).crossfade(true).build(),
                                        contentDescription = lbl,
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.CameraAlt, null, tint = TextLight,
                                        modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // Times
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(7.dp).background(AccentGreen, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text("In:  ${session.checkInTime ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentGreen, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Box(modifier = Modifier.size(7.dp)
                    .background(if (isOpen) TextLight else AccentRed, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text("Out: ${session.checkOutTime ?: "Not recorded"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOpen) TextLight else AccentRed)
            }

            // Address
            val addr = session.checkInAddress.takeIf { it.isNotBlank() && !it.startsWith("Location:") }
            if (addr != null) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = TextLight,
                        modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(addr, style = MaterialTheme.typography.bodySmall, color = TextLight)
                }
            }
        }
    }
}

// ── History session card ──────────────────────────────────────────

@Composable
private fun HistorySessionCard(
    index: Int,
    checkIn: AttendanceEntity?,
    checkOut: AttendanceEntity?,
    onSelfieClick: (String) -> Unit = {}
) {
    val timeFmt      = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val checkInTime  = checkIn?.let  { timeFmt.format(Date(it.timestamp)) } ?: "--"
    val checkOutTime = checkOut?.let { timeFmt.format(Date(it.timestamp)) } ?: "Not recorded"
    val isOpen       = checkOut == null

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isOpen) AccentGold.copy(0.15f) else WoodCream, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "S$index",
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color      = if (isOpen) AccentGoldDark else WoodMid
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Site Visit $index",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextDark
                )
                Spacer(Modifier.height(6.dp))

                // Check-in
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).background(AccentGreen, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text("In:  $checkInTime", style = MaterialTheme.typography.bodySmall,
                        color = AccentGreen, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(3.dp))

                // Check-out
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp)
                        .background(if (isOpen) TextLight else AccentRed, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text("Out: $checkOutTime", style = MaterialTheme.typography.bodySmall,
                        color = if (isOpen) TextLight else AccentRed,
                        fontWeight = if (isOpen) FontWeight.Normal else FontWeight.Medium)
                }

                // Selfie thumbnails
                val ciSelfie = checkIn?.selfieUrl?.takeIf { it.isNotBlank() }
                    ?: checkIn?.selfiePath?.takeIf { it.isNotBlank() && File(it).exists() }
                val coSelfie = checkOut?.selfieUrl?.takeIf { it.isNotBlank() }
                    ?: checkOut?.selfiePath?.takeIf { it.isNotBlank() && File(it).exists() }

                if (ciSelfie != null || coSelfie != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Check-in" to ciSelfie,
                            "Check-out" to coSelfie
                        ).forEach { (lbl, src) ->
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(lbl,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (lbl == "Check-in") AccentGreen else AccentRed)
                                Spacer(Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(WoodCream)
                                        .then(if (src != null) Modifier.clickable { onSelfieClick(src) } else Modifier),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (src != null) {
                                        if (src.startsWith("http")) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(src).crossfade(true).build(),
                                                contentDescription = lbl,
                                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            val bmp = remember(src) {
                                                runCatching {
                                                    android.graphics.BitmapFactory.decodeFile(src)?.asImageBitmap()
                                                }.getOrNull()
                                            }
                                            if (bmp != null) {
                                                Image(bmp, lbl,
                                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                                    contentScale = ContentScale.Crop)
                                            }
                                        }
                                        Box(
                                            modifier = Modifier.align(Alignment.BottomEnd)
                                                .background(Color.Black.copy(0.4f), RoundedCornerShape(topStart = 6.dp))
                                                .padding(3.dp)
                                        ) {
                                            Icon(Icons.Default.ZoomIn, null, tint = Color.White,
                                                modifier = Modifier.size(12.dp))
                                        }
                                    } else {
                                        Icon(Icons.Default.CameraAlt, null, tint = TextLight,
                                            modifier = Modifier.size(22.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Address
                val addr = checkIn?.address?.takeIf { !it.startsWith("Location:") }
                if (addr != null) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = TextLight, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(addr, style = MaterialTheme.typography.bodySmall, color = TextLight)
                    }
                }
            }

            // Synced badge
            val synced = checkIn?.isSynced == true && (checkOut == null || checkOut.isSynced)
            Surface(
                color = if (synced) AccentGreen.copy(0.12f) else Color(0xFFFFF3DC),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    if (synced) "✓ Synced" else "Pending",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = if (synced) AccentGreen else AccentGoldDark,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}