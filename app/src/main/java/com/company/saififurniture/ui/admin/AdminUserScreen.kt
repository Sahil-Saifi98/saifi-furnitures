package com.saififurnitures.app.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.saififurnitures.app.data.remote.AdminUser
import com.saififurnitures.app.navigation.NavRoutes
import com.saififurnitures.app.ui.theme.*

@Composable
fun AdminUsersScreen(
    navController: NavHostController,
    viewModel: AdminUsersViewModel = viewModel()
) {
    val users         by viewModel.users.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()
    val errorMessage  by viewModel.errorMessage.collectAsState()
    val actionStatus  by viewModel.actionStatus.collectAsState()
    val newName       by viewModel.newName.collectAsState()
    val newEmployeeId by viewModel.newEmployeeId.collectAsState()
    val newEmail      by viewModel.newEmail.collectAsState()
    val newPassword   by viewModel.newPassword.collectAsState()
    val isAdding      by viewModel.isAdding.collectAsState()

    var showAddDialog    by remember { mutableStateOf(false) }
    var userToDelete     by remember { mutableStateOf<AdminUser?>(null) }
    var passwordVisible  by remember { mutableStateOf(false) }

    // Action status snackbar
    LaunchedEffect(actionStatus) {
        if (actionStatus != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearActionStatus()
        }
    }

    // Delete confirmation dialog
    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Remove Carpenter", fontWeight = FontWeight.Bold, color = TextDark) },
            text  = {
                Text(
                    "Are you sure you want to remove ${user.name} (${user.employeeId})? This cannot be undone.",
                    color = TextMid
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCarpenter(user)
                    userToDelete = null
                }) {
                    Text("Remove", color = AccentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancel", color = TextMid)
                }
            },
            containerColor = BgCard
        )
    }

    // Add carpenter dialog
    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                shape  = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(WoodMid.copy(0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PersonAdd, null, tint = WoodMid, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Add New Carpenter",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = TextDark
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // Name
                    OutlinedTextField(
                        value         = newName,
                        onValueChange = { viewModel.onNameChange(it) },
                        label         = { Text("Full Name *") },
                        leadingIcon   = { Icon(Icons.Default.Person, null, tint = WoodMid) },
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp),
                        singleLine    = true,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = WoodMid,
                            unfocusedBorderColor = WoodWarm,
                            focusedLabelColor    = WoodMid
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    // Carpenter ID
                    OutlinedTextField(
                        value         = newEmployeeId,
                        onValueChange = { viewModel.onEmployeeIdChange(it) },
                        label         = { Text("Carpenter ID *") },
                        placeholder   = { Text("e.g. SAIFI-004") },
                        leadingIcon   = { Icon(Icons.Default.Badge, null, tint = WoodMid) },
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp),
                        singleLine    = true,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = WoodMid,
                            unfocusedBorderColor = WoodWarm,
                            focusedLabelColor    = WoodMid
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    // Email
                    OutlinedTextField(
                        value         = newEmail,
                        onValueChange = { viewModel.onEmailChange(it) },
                        label         = { Text("Email (optional)") },
                        leadingIcon   = { Icon(Icons.Default.Email, null, tint = WoodMid) },
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp),
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = WoodMid,
                            unfocusedBorderColor = WoodWarm,
                            focusedLabelColor    = WoodMid
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    // Password
                    OutlinedTextField(
                        value         = newPassword,
                        onValueChange = { viewModel.onPasswordChange(it) },
                        label         = { Text("Password *") },
                        leadingIcon   = { Icon(Icons.Default.Lock, null, tint = WoodMid) },
                        trailingIcon  = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility
                                    else Icons.Default.VisibilityOff,
                                    null, tint = TextLight
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp),
                        singleLine    = true,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = WoodMid,
                            unfocusedBorderColor = WoodWarm,
                            focusedLabelColor    = WoodMid
                        )
                    )

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick  = { showAddDialog = false },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = TextMid)
                        ) { Text("Cancel") }

                        Button(
                            onClick  = {
                                viewModel.addCarpenter()
                                if (newName.isNotBlank()) showAddDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(12.dp),
                            enabled  = !isAdding,
                            colors   = ButtonDefaults.buttonColors(containerColor = WoodMid)
                        ) {
                            if (isAdding) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Add", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

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
                                "Administrator",
                                style    = MaterialTheme.typography.labelMedium,
                                color    = AccentGold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(AccentGold.copy(0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Group, null, tint = AccentGold, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Carpenters",
                                    style      = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color.White
                                )
                                Text(
                                    "${users.size} registered",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WoodWarm.copy(0.8f)
                                )
                            }
                        }

                        // Add button
                        Button(
                            onClick  = { showAddDialog = true },
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = AccentGold)
                        ) {
                            Icon(Icons.Default.PersonAdd, null,
                                modifier = Modifier.size(18.dp), tint = WoodDark)
                            Spacer(Modifier.width(6.dp))
                            Text("Add", color = WoodDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Action status banner
            actionStatus?.let { status ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color    = if (status.isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (status.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                            null,
                            tint     = if (status.isSuccess) AccentGreen else AccentRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            status.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (status.isSuccess) Color(0xFF2E7D32) else AccentRed
                        )
                    }
                }
            }

            // Content
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = WoodMid)
                    }
                }
                errorMessage != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ErrorOutline, null, tint = AccentRed, modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(errorMessage ?: "", color = AccentRed)
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.loadUsers() },
                                colors  = ButtonDefaults.buttonColors(containerColor = WoodMid)
                            ) { Text("Retry") }
                        }
                    }
                }
                users.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🪵", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("No carpenters registered yet", color = TextMid,
                                style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                colors  = ButtonDefaults.buttonColors(containerColor = WoodMid)
                            ) {
                                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Add First Carpenter")
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier        = Modifier.fillMaxSize(),
                        contentPadding  = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(users) { user ->
                            CarpenterCard(
                                user          = user,
                                onViewClick   = {
                                    navController.navigate(
                                        NavRoutes.AdminCarpenterDetail.createRoute(user.employeeId, user.name)
                                    )
                                },
                                onDeleteClick = { userToDelete = user }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Carpenter list card ───────────────────────────────────────────

@Composable
fun CarpenterCard(
    user: AdminUser,
    onViewClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(WoodMid.copy(0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    user.name.firstOrNull()?.uppercase() ?: "C",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = WoodMid
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    user.name,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = TextDark
                )
                Text(
                    user.employeeId,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLight
                )
                if (user.email.isNotBlank()) {
                    Text(
                        user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLight
                    )
                }
            }

            // Status dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        if (user.isActive) AccentGreen else TextLight,
                        CircleShape
                    )
            )

            Spacer(Modifier.width(10.dp))

            // View button
            IconButton(
                onClick  = onViewClick,
                modifier = Modifier
                    .size(36.dp)
                    .background(WoodCream, CircleShape)
            ) {
                Icon(
                    Icons.Default.Visibility, null,
                    tint     = WoodMid,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(6.dp))

            // Delete button
            IconButton(
                onClick  = onDeleteClick,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFFFEBEE), CircleShape)
            ) {
                Icon(
                    Icons.Default.DeleteOutline, null,
                    tint     = AccentRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}