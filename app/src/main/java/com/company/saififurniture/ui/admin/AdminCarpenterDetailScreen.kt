package com.saififurnitures.app.ui.admin

import android.app.Application
import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.saififurnitures.app.data.remote.AdminAttendanceItem
import com.saififurnitures.app.data.remote.RetrofitClient
import com.saififurnitures.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── ViewModel ─────────────────────────────────────────────────────

class AdminCarpenterDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val employeeId: String = savedStateHandle["employeeId"] ?: ""
    val carpenterName: String = savedStateHandle["carpenterName"] ?: "Carpenter"

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _sessions   = MutableStateFlow<List<AdminAttendanceItem>>(emptyList())
    val sessions: StateFlow<List<AdminAttendanceItem>> = _sessions

    private val _isLoading  = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error      = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _startDate  = MutableStateFlow(dateFmt.format(Date()))
    val startDate: StateFlow<String> = _startDate

    private val _endDate    = MutableStateFlow(dateFmt.format(Date()))
    val endDate: StateFlow<String> = _endDate

    init { load() }

    fun setStartDate(date: String) { _startDate.value = date; load() }
    fun setEndDate(date: String)   { _endDate.value   = date; load() }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val response = RetrofitClient.adminApi.getUserAttendance(
                    employeeId = employeeId,
                    startDate  = _startDate.value,
                    endDate    = _endDate.value
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    _sessions.value = response.body()!!.data
                        .sortedByDescending { it.date + (it.checkInTime ?: "") }
                } else {
                    _error.value = "Failed to load: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
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

    fun formatTime(utc: String) = convertUtcToIst(utc)
}

// ── Screen ────────────────────────────────────────────────────────

@Composable
fun AdminCarpenterDetailScreen(
    navController: NavHostController,
    viewModel: AdminCarpenterDetailViewModel = viewModel()
) {
    val context    = LocalContext.current
    val sessions   by viewModel.sessions.collectAsState()
    val isLoading  by viewModel.isLoading.collectAsState()
    val error      by viewModel.error.collectAsState()
    val startDate  by viewModel.startDate.collectAsState()
    val endDate    by viewModel.endDate.collectAsState()
    val dateFmt    = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    var selectedImageUrl by remember { mutableStateOf<String?>(null) }

    Scaffold(containerColor = BgMain) { padding ->
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
                            end    = Offset(1000f, 500f)
                        )
                    )
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick  = { navController.navigateUp() },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(0.15f), CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                        }
                        Surface(
                            color = AccentGold.copy(0.2f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                "Attendance Detail",
                                style    = MaterialTheme.typography.labelMedium,
                                color    = AccentGold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(AccentGold.copy(0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                viewModel.carpenterName.firstOrNull()?.uppercase() ?: "C",
                                style      = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color      = AccentGold
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                viewModel.carpenterName,
                                style      = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White
                            )
                            Text(
                                "${sessions.size} session(s) found",
                                style = MaterialTheme.typography.bodySmall,
                                color = WoodWarm.copy(0.8f)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Date range
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
                            Icon(Icons.Default.CalendarToday, null,
                                modifier = Modifier.size(14.dp), tint = AccentGold)
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
                            Icon(Icons.Default.CalendarToday, null,
                                modifier = Modifier.size(14.dp), tint = AccentGold)
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
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${sessions.size} sessions",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMid
                    )
                    Text(
                        "${sessions.count { it.checkOutTime != null }} completed  •  ${sessions.count { it.checkOutTime == null }} open",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLight
                    )
                }
            }

            // List
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = WoodMid)
                    }
                }
                error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ErrorOutline, null, tint = AccentRed, modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(error ?: "", color = AccentRed)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { viewModel.load() },
                                colors = ButtonDefaults.buttonColors(containerColor = WoodMid)) {
                                Text("Retry")
                            }
                        }
                    }
                }
                sessions.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🪵", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("No sessions in this period",
                                style = MaterialTheme.typography.titleMedium, color = TextMid,
                                textAlign = TextAlign.Center)
                            Text("Try selecting a different date range",
                                style = MaterialTheme.typography.bodySmall, color = TextLight,
                                textAlign = TextAlign.Center)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier        = Modifier.fillMaxSize(),
                        contentPadding  = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(sessions) { session ->
                            DetailSessionCard(
                                session       = session,
                                formatTime    = { viewModel.formatTime(it) },
                                onSelfieClick = { url -> selectedImageUrl = url }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedImageUrl?.let {
        SelfieViewerDialog(imageUrl = it, onDismiss = { selectedImageUrl = null })
    }
}

// ── Detail session card ───────────────────────────────────────────

@Composable
private fun DetailSessionCard(
    session: AdminAttendanceItem,
    formatTime: (String) -> String,
    onSelfieClick: (String) -> Unit
) {
    val isOpen      = session.checkOutTime == null
    val checkInTime = session.checkInTime?.let { formatTime(it) } ?: "—"
    val checkOutTime = session.checkOutTime?.let { formatTime(it) } ?: "—"

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // Date + status header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isOpen) AccentGold.copy(0.08f) else WoodCream.copy(0.5f),
                        RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, null, tint = TextLight, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(session.date, style = MaterialTheme.typography.bodySmall, color = TextLight)
                }
                Surface(
                    color = if (isOpen) AccentGold.copy(0.18f) else AccentGreen.copy(0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        if (isOpen) "On Site" else "Completed",
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (isOpen) AccentGoldDark else AccentGreen,
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = DividerWood, thickness = 1.dp)

            // Check-in row
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(AccentGreen, CircleShape)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Check In  ·  $checkInTime",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, color = AccentGreen)
                    if (session.checkInAddress.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = TextLight, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(session.checkInAddress,
                                style = MaterialTheme.typography.bodySmall, color = TextLight, maxLines = 1)
                        }
                    }
                }
                if (!session.checkInSelfieUrl.isNullOrEmpty()) {
                    IconButton(
                        onClick  = { onSelfieClick(session.checkInSelfieUrl) },
                        modifier = Modifier.size(32.dp).background(WoodCream, CircleShape)
                    ) {
                        Icon(Icons.Default.CameraAlt, null, tint = WoodMid, modifier = Modifier.size(16.dp))
                    }
                }
            }

            HorizontalDivider(color = DividerWood.copy(0.5f), thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 14.dp))

            // Check-out row
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (isOpen) TextLight else AccentRed, CircleShape)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Check Out  ·  $checkOutTime",
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (isOpen) TextLight else AccentRed
                    )
                    val outAddr = session.checkOutAddress
                    if (!outAddr.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = TextLight, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(outAddr, style = MaterialTheme.typography.bodySmall,
                                color = TextLight, maxLines = 1)
                        }
                    }
                }
                if (!session.checkOutSelfieUrl.isNullOrEmpty()) {
                    IconButton(
                        onClick  = { onSelfieClick(session.checkOutSelfieUrl!!) },
                        modifier = Modifier.size(32.dp).background(WoodCream, CircleShape)
                    ) {
                        Icon(Icons.Default.CameraAlt, null, tint = WoodMid, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}