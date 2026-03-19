package com.saififurnitures.app.ui.admin

import android.app.DatePickerDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.saififurnitures.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminExportScreen(
    navController: NavHostController,
    viewModel: AdminExportViewModel = viewModel()
) {
    val context      = LocalContext.current
    val isLoading    by viewModel.isLoading.collectAsState()
    val exportStatus by viewModel.exportStatus.collectAsState()
    val startDate    by viewModel.startDate.collectAsState()
    val endDate      by viewModel.endDate.collectAsState()
    val users        by viewModel.users.collectAsState()
    val selectedUser by viewModel.selectedUser.collectAsState()
    val scrollState  = rememberScrollState()
    val dateFormat   = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    var showUserDropdown by remember { mutableStateOf(false) }

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
                            Icon(Icons.Default.Download, null, tint = AccentGold, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Export Attendance",
                                style      = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White
                            )
                            Text(
                                "Download records by date range",
                                style = MaterialTheme.typography.bodySmall,
                                color = WoodWarm.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Date Range Card ───────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = BgCard),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, null, tint = WoodMid, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Date Range",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = TextDark
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Applies to both export sections below",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLight
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(context, { _, y, m, d ->
                                    viewModel.setStartDate(dateFormat.format(
                                        Calendar.getInstance().apply { set(y, m, d) }.time))
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (startDate != null) WoodMid else TextLight
                            ),
                            border   = androidx.compose.foundation.BorderStroke(
                                1.dp, if (startDate != null) WoodMid else WoodWarm
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Start Date", style = MaterialTheme.typography.labelSmall, color = TextLight)
                                Text(
                                    startDate ?: "Select",
                                    style      = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(context, { _, y, m, d ->
                                    viewModel.setEndDate(dateFormat.format(
                                        Calendar.getInstance().apply { set(y, m, d) }.time))
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (endDate != null) WoodMid else TextLight
                            ),
                            border   = androidx.compose.foundation.BorderStroke(
                                1.dp, if (endDate != null) WoodMid else WoodWarm
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("End Date", style = MaterialTheme.typography.labelSmall, color = TextLight)
                                Text(
                                    endDate ?: "Select",
                                    style      = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Export All Employees ──────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = BgCard),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(WoodMid.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Groups, null, tint = WoodMid, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "All Carpenters",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color      = TextDark
                            )
                            Text(
                                "Export attendance for all carpenters",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextLight
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick  = { viewModel.exportAllPDF() },
                            modifier = Modifier.weight(1f).height(48.dp),
                            enabled  = !isLoading,
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = WoodMid)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("PDF", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick  = { viewModel.exportAllCSV() },
                            modifier = Modifier.weight(1f).height(48.dp),
                            enabled  = !isLoading,
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = WoodMid),
                            border   = androidx.compose.foundation.BorderStroke(1.5.dp, WoodMid)
                        ) {
                            Icon(Icons.Default.TableChart, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("CSV", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Export Specific Carpenter ─────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = BgCard),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(AccentGoldDark.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, null, tint = AccentGoldDark, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Specific Carpenter",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color      = TextDark
                            )
                            Text(
                                "Export attendance for one carpenter",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextLight
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // User dropdown
                    ExposedDropdownMenuBox(
                        expanded          = showUserDropdown,
                        onExpandedChange  = { showUserDropdown = it }
                    ) {
                        OutlinedTextField(
                            value         = selectedUser?.let { "${it.name} (${it.employeeId})" }
                                ?: "Select a carpenter…",
                            onValueChange = { },
                            readOnly      = true,
                            modifier      = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon  = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showUserDropdown)
                            },
                            leadingIcon   = {
                                Icon(Icons.Default.Carpenter, null, tint = WoodMid)
                            },
                            shape         = RoundedCornerShape(12.dp),
                            colors        = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = WoodWarm,
                                focusedBorderColor   = WoodMid
                            )
                        )
                        ExposedDropdownMenu(
                            expanded         = showUserDropdown,
                            onDismissRequest = { showUserDropdown = false }
                        ) {
                            if (users.isEmpty()) {
                                DropdownMenuItem(
                                    text    = { Text("No carpenters found", color = TextLight) },
                                    onClick = { showUserDropdown = false }
                                )
                            } else {
                                users.forEach { user ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(user.name, fontWeight = FontWeight.Medium, color = TextDark)
                                                Text(
                                                    user.employeeId,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextLight
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectUser(user)
                                            showUserDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick  = { viewModel.exportUserPDF() },
                            modifier = Modifier.weight(1f).height(48.dp),
                            enabled  = !isLoading && selectedUser != null,
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = AccentGoldDark)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("PDF", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick  = { viewModel.exportUserCSV() },
                            modifier = Modifier.weight(1f).height(48.dp),
                            enabled  = !isLoading && selectedUser != null,
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = AccentGoldDark),
                            border   = androidx.compose.foundation.BorderStroke(1.5.dp, AccentGoldDark)
                        ) {
                            Icon(Icons.Default.TableChart, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("CSV", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Loading indicator ─────────────────────────────────
            if (isLoading) {
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape     = RoundedCornerShape(12.dp),
                    colors    = CardDefaults.cardColors(containerColor = WoodCream)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp),
                            color       = WoodMid,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Preparing export…", color = TextMid, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Status banner ─────────────────────────────────────
            exportStatus?.let { status ->
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape     = RoundedCornerShape(12.dp),
                    colors    = CardDefaults.cardColors(
                        containerColor = if (status.isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (status.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                            null,
                            tint     = if (status.isSuccess) AccentGreen else AccentRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            status.message,
                            color = if (status.isSuccess) Color(0xFF2E7D32) else AccentRed,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}