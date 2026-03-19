package com.saififurnitures.app.ui.attendance

import android.app.DatePickerDialog
import android.app.Application
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.saififurnitures.app.data.local.AppDatabase
import com.saififurnitures.app.data.local.AttendanceEntity
import com.saififurnitures.app.data.session.SessionManager
import com.saififurnitures.app.navigation.NavRoutes
import com.saififurnitures.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ── ViewModel ─────────────────────────────────────────────────────

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val dao            = AppDatabase.getDatabase(application).attendanceDao()
    private val sessionManager = SessionManager(application)
    private val dateFmt        = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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
                val userId = sessionManager.getUserId() ?: return@launch
                _records.value = dao.getAttendanceByRange(userId, _startDate.value, _endDate.value)
            } finally {
                _isLoading.value = false
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────

@Composable
fun HistoryScreen(
    navController: NavHostController,
    viewModel: HistoryViewModel = viewModel()
) {
    val context    = LocalContext.current
    val records    by viewModel.records.collectAsState()
    val isLoading  by viewModel.isLoading.collectAsState()
    val startDate  by viewModel.startDate.collectAsState()
    val endDate    by viewModel.endDate.collectAsState()
    val dateFmt    = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    var fullscreenSelfie by remember { mutableStateOf<String?>(null) }

    // Group by date → sessionId
    val grouped = records.groupBy { it.sessionId }
    val byDate  = grouped.values
        .mapNotNull { recs ->
            val ci = recs.find { it.type == "check_in" }
            val co = recs.find { it.type == "check_out" }
            val date = ci?.let {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Date(it.timestamp))
            } ?: return@mapNotNull null
            Triple(date, ci, co)
        }
        .sortedByDescending { it.first }
        .groupBy { it.first }

    Scaffold(
        containerColor = BgMain,
        bottomBar = {
            NavigationBar(containerColor = BgCard, tonalElevation = 8.dp) {
                NavigationBarItem(
                    selected = false,
                    onClick  = { navController.navigate(NavRoutes.Attendance) { launchSingleTop = true } },
                    icon     = { Icon(Icons.Default.HomeWork, null) },
                    label    = { Text("Attendance") }
                )
                NavigationBarItem(
                    selected = true,
                    onClick  = {},
                    icon     = { Icon(Icons.Default.History, null) },
                    label    = { Text("History") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick  = { navController.navigate(NavRoutes.Profile) { launchSingleTop = true } },
                    icon     = { Icon(Icons.Default.Person, null) },
                    label    = { Text("Profile") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(listOf(WoodDark, WoodMid.copy(0.3f), BgMain))
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("My History", style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Check-in & Check-out records", style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(0.7f))
                    }
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Default.Refresh, null, tint = AccentGold)
                    }
                }

                // Date pickers
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("From", style = MaterialTheme.typography.labelSmall, color = TextLight)
                            TextButton(onClick = {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(context,
                                    { _, y, m, d ->
                                        viewModel.setStartDate(dateFmt.format(
                                            Calendar.getInstance().also { it.set(y, m, d) }.time))
                                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)).show()
                            }) {
                                Text(startDate, color = WoodMid, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Icon(Icons.Default.ArrowForward, null, tint = TextLight, modifier = Modifier.size(16.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("To", style = MaterialTheme.typography.labelSmall, color = TextLight)
                            TextButton(onClick = {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(context,
                                    { _, y, m, d ->
                                        viewModel.setEndDate(dateFmt.format(
                                            Calendar.getInstance().also { it.set(y, m, d) }.time))
                                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)).show()
                            }) {
                                Text(endDate, color = WoodMid, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Surface(
                            color = WoodMid,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { viewModel.load() }
                        ) {
                            Text("Search", color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentGold)
                    }
                } else if (byDate.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.History, null, tint = TextLight,
                                modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No records found", style = MaterialTheme.typography.titleMedium,
                                color = TextLight)
                            Text("Try a different date range", style = MaterialTheme.typography.bodySmall,
                                color = TextLight.copy(0.6f))
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        byDate.forEach { (date, sessions) ->
                            item {
                                val displayDate = try {
                                    val d = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
                                    SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(d!!)
                                } catch (_: Exception) { date }

                                Row(
                                    modifier = Modifier.fillMaxWidth()
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
                                        Text("${sessions.size} visit(s)",
                                            style = MaterialTheme.typography.labelSmall, color = WoodMid,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                                }
                            }

                            itemsIndexed(sessions) { idx, (_, checkIn, checkOut) ->
                                HistorySessionCard(
                                    index    = idx + 1,
                                    checkIn  = checkIn,
                                    checkOut = checkOut,
                                    onSelfieClick = { url -> fullscreenSelfie = url }
                                )
                            }

                            item { Spacer(Modifier.height(8.dp)) }
                        }
                    }
                }
            }
        }
    }

    // Fullscreen selfie viewer
    if (fullscreenSelfie != null) {
        Dialog(
            onDismissRequest = { fullscreenSelfie = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black)
                    .clickable { fullscreenSelfie = null },
                contentAlignment = Alignment.Center
            ) {
                val src = fullscreenSelfie!!
                if (src.startsWith("http")) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(src)
                            .crossfade(true).build(),
                        contentDescription = "Selfie",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    val bitmap = remember(src) {
                        val f = File(src)
                        if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap()
                        else null
                    }
                    if (bitmap != null) {
                        Image(bitmap, "Selfie", modifier = Modifier.fillMaxWidth(),
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

// ── History session card ──────────────────────────────────────────

@Composable
private fun HistorySessionCard(
    index: Int,
    checkIn: AttendanceEntity?,
    checkOut: AttendanceEntity?,
    onSelfieClick: (String) -> Unit
) {
    val timeFmt      = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val checkInTime  = checkIn?.let  { timeFmt.format(Date(it.timestamp)) } ?: "--"
    val checkOutTime = checkOut?.let { timeFmt.format(Date(it.timestamp)) } ?: "Not recorded"
    val isOpen       = checkOut == null
    val synced       = checkIn?.isSynced == true && (checkOut == null || checkOut.isSynced)

    // Best selfie URL: prefer Cloudinary url, fallback to local file path
    val checkInSelfie  = checkIn?.selfieUrl?.takeIf  { it.isNotBlank() }
        ?: checkIn?.selfiePath?.takeIf { it.isNotBlank() && File(it).exists() }
    val checkOutSelfie = checkOut?.selfieUrl?.takeIf  { it.isNotBlank() }
        ?: checkOut?.selfiePath?.takeIf { it.isNotBlank() && File(it).exists() }

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {

            // Top row: session number + synced badge
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
                    Text(
                        if (synced) "✓ Synced" else "Pending",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (synced) AccentGreen else AccentGoldDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Selfie thumbnails row
            if (checkInSelfie != null || checkOutSelfie != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Check-in selfie
                    SelfieThumb(
                        source = checkInSelfie,
                        label  = "Check-in",
                        color  = AccentGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { checkInSelfie?.let { onSelfieClick(it) } }
                    )
                    // Check-out selfie
                    SelfieThumb(
                        source = checkOutSelfie,
                        label  = "Check-out",
                        color  = AccentRed,
                        modifier = Modifier.weight(1f),
                        onClick = { checkOutSelfie?.let { onSelfieClick(it) } }
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            // Times
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(7.dp).background(AccentGreen, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text("In:  $checkInTime", style = MaterialTheme.typography.bodySmall,
                    color = AccentGreen, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Box(modifier = Modifier.size(7.dp)
                    .background(if (isOpen) TextLight else AccentRed, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text("Out: $checkOutTime", style = MaterialTheme.typography.bodySmall,
                    color = if (isOpen) TextLight else AccentRed,
                    fontWeight = if (isOpen) FontWeight.Normal else FontWeight.Medium)
            }

            // Address
            val addr = checkIn?.address?.takeIf { !it.startsWith("Location:") }
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

@Composable
private fun SelfieThumb(
    source: String?,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color,
            fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(WoodCream)
                .clickable(enabled = source != null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (source != null) {
                if (source.startsWith("http")) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(source).crossfade(true).build(),
                        contentDescription = label,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val bitmap = remember(source) {
                        val f = File(source)
                        if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap()
                        else null
                    }
                    if (bitmap != null) {
                        Image(bitmap, label, modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.BrokenImage, null, tint = TextLight,
                            modifier = Modifier.size(28.dp))
                    }
                }
                // Tap to expand hint
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd)
                        .background(Color.Black.copy(0.35f), RoundedCornerShape(topStart = 8.dp))
                        .padding(4.dp)
                ) {
                    Icon(Icons.Default.ZoomIn, null, tint = Color.White,
                        modifier = Modifier.size(14.dp))
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, null, tint = TextLight,
                        modifier = Modifier.size(24.dp))
                    Text("No photo", style = MaterialTheme.typography.labelSmall,
                        color = TextLight)
                }
            }
        }
    }
}