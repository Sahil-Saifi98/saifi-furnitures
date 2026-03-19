package com.saififurnitures.app.ui.admin

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.saififurnitures.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminAttendanceScreen(
    navController: NavHostController,
    viewModel: AdminAttendanceViewModel = viewModel()
) {
    val context       = LocalContext.current
    val sessionList   by viewModel.sessionList.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()
    val errorMessage  by viewModel.errorMessage.collectAsState()
    val selectedDate  by viewModel.selectedDate.collectAsState()
    val searchQuery   by viewModel.searchQuery.collectAsState()

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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { navController.navigateUp() },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                        }
                        Surface(
                            color = AccentGold.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                "Administrator",
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
                                .size(48.dp)
                                .background(AccentGold.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PeopleAlt, null,
                                tint     = AccentGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Attendance Logs",
                                style      = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White
                            )
                            Text(
                                "${sessionList.size} sessions found",
                                style = MaterialTheme.typography.bodySmall,
                                color = WoodWarm.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Search bar
                    OutlinedTextField(
                        value       = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        modifier    = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Search by name or location…",
                                color = WoodWarm.copy(0.6f)
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, null, tint = WoodWarm.copy(0.8f))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Close, null, tint = WoodWarm.copy(0.8f))
                                }
                            }
                        },
                        singleLine = true,
                        shape      = RoundedCornerShape(12.dp),
                        colors     = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = WoodWarm.copy(alpha = 0.3f),
                            focusedBorderColor   = AccentGold,
                            unfocusedTextColor   = Color.White,
                            focusedTextColor     = Color.White,
                            cursorColor          = AccentGold
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    // Date picker button
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            DatePickerDialog(context, { _, y, m, d ->
                                val sel = Calendar.getInstance().apply { set(y, m, d) }
                                viewModel.onDateSelected(
                                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                        .format(sel.time)
                                )
                            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        shape  = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, WoodWarm.copy(0.4f)
                        )
                    ) {
                        Icon(
                            Icons.Default.CalendarToday, null,
                            modifier = Modifier.size(16.dp),
                            tint     = AccentGold
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(selectedDate, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Content
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = WoodMid)
                            Spacer(Modifier.height(12.dp))
                            Text("Loading attendance…", color = TextMid)
                        }
                    }
                }
                errorMessage != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ErrorOutline, null,
                                tint     = AccentRed,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                errorMessage ?: "",
                                color = AccentRed,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadAttendance() },
                                colors  = ButtonDefaults.buttonColors(containerColor = WoodMid)
                            ) { Text("Retry") }
                        }
                    }
                }
                sessionList.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🪵", fontSize = 56.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No sessions found",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextMid
                            )
                            Text(
                                "Try selecting a different date",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextLight
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier        = Modifier.fillMaxSize(),
                        contentPadding  = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sessionList) { session ->
                            AdminSessionCard(
                                session              = session,
                                onCheckInSelfieClick = { selectedImageUrl = session.checkInSelfieUrl },
                                onCheckOutSelfieClick= {
                                    session.checkOutSelfieUrl?.let { selectedImageUrl = it }
                                }
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

// ── Paired session card ───────────────────────────────────────────

@Composable
fun AdminSessionCard(
    session: AdminSessionRow,
    onCheckInSelfieClick: () -> Unit,
    onCheckOutSelfieClick: () -> Unit
) {
    val isOpen = session.checkOutTime == null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = BgCard),
        elevation= CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // Card header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isOpen) AccentGold.copy(alpha = 0.08f)
                        else WoodCream.copy(alpha = 0.5f),
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(WoodMid.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            session.employeeName.firstOrNull()?.uppercase() ?: "C",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = WoodMid
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            session.employeeName,
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color      = TextDark
                        )
                        Text(
                            session.employeeId,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextLight
                        )
                    }
                }

                // Status chip
                Surface(
                    color = if (isOpen) AccentGold.copy(alpha = 0.18f)
                    else AccentGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(
                                    if (isOpen) AccentGoldDark else AccentGreen,
                                    CircleShape
                                )
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            if (isOpen) "On Site" else "Completed",
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color      = if (isOpen) AccentGoldDark else AccentGreen
                        )
                    }
                }
            }

            // Date row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CalendarToday, null,
                    tint     = TextLight,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    session.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLight
                )
            }

            HorizontalDivider(color = DividerWood, thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp))

            // Check-in row
            AttendanceEventRow(
                label         = "Check In",
                time          = session.checkInTime,
                address       = session.checkInAddress,
                selfieUrl     = session.checkInSelfieUrl,
                dotColor      = AccentGreen,
                onSelfieClick = onCheckInSelfieClick
            )

            HorizontalDivider(
                color     = DividerWood.copy(alpha = 0.5f),
                thickness = 1.dp,
                modifier  = Modifier.padding(horizontal = 16.dp)
            )

            // Check-out row
            AttendanceEventRow(
                label         = "Check Out",
                time          = session.checkOutTime ?: "—",
                address       = session.checkOutAddress ?: "Not checked out yet",
                selfieUrl     = session.checkOutSelfieUrl,
                dotColor      = if (isOpen) TextLight else AccentRed,
                onSelfieClick = onCheckOutSelfieClick,
                dimmed        = isOpen
            )
        }
    }
}

// ── Single event row (check-in or check-out) ─────────────────────

@Composable
private fun AttendanceEventRow(
    label: String,
    time: String,
    address: String,
    selfieUrl: String?,
    dotColor: Color,
    onSelfieClick: () -> Unit,
    dimmed: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selfie thumbnail
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(WoodCream, CircleShape)
                .clip(CircleShape)
                .clickable(enabled = !selfieUrl.isNullOrEmpty(), onClick = onSelfieClick)
        ) {
            if (!selfieUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(selfieUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = label,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.PersonOff, null,
                        tint     = TextLight,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(dotColor, CircleShape)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color      = if (dimmed) TextLight else dotColor
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule, null,
                    tint     = TextLight,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    time,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (dimmed) TextLight else TextDark
                )
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn, null,
                    tint     = TextLight,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    address,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLight,
                    maxLines = 2
                )
            }
        }
    }
}

// ── Full screen selfie viewer ─────────────────────────────────────

@Composable
fun SelfieViewerDialog(imageUrl: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Box {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Selfie",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                )
                IconButton(
                    onClick  = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }
        }
    }
}