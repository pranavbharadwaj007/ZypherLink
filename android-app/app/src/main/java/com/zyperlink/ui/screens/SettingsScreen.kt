package com.zyperlink.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zyperlink.ui.theme.PrimaryOrange
import com.zyperlink.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val localDeviceInfo by viewModel.localDeviceInfo.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val transferHistory by viewModel.transferHistory.collectAsState()
    
    var showDeviceNameDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top bar
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PrimaryOrange,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Device Information Section
            item {
                SettingsSection(
                    title = "Device Information"
                ) {
                    SettingsItem(
                        icon = Icons.Default.PhoneAndroid,
                        title = "Device Name",
                        subtitle = localDeviceInfo?.deviceName ?: "Unknown",
                        onClick = { showDeviceNameDialog = true }
                    )
                    
                    SettingsItem(
                        icon = Icons.Default.Fingerprint,
                        title = "Device ID",
                        subtitle = localDeviceInfo?.deviceId?.take(8) + "..." ?: "Unknown",
                        showArrow = false
                    )
                    
                    SettingsItem(
                        icon = Icons.Default.Wifi,
                        title = "IP Address",
                        subtitle = localDeviceInfo?.ipAddress ?: "Not Connected",
                        showArrow = false
                    )
                }
            }
            
            // Connection Stats Section
            item {
                SettingsSection(
                    title = "Statistics"
                ) {
                    SettingsItem(
                        icon = Icons.Default.Devices,
                        title = "Paired Devices",
                        subtitle = "${pairedDevices.size} devices",
                        showArrow = false
                    )
                    
                    SettingsItem(
                        icon = Icons.Default.History,
                        title = "Transfer History",
                        subtitle = "${transferHistory.size} transfers",
                        showArrow = false
                    )
                    
                    val onlineDevices = pairedDevices.count { it.isOnline }
                    SettingsItem(
                        icon = Icons.Default.CloudDone,
                        title = "Online Devices",
                        subtitle = "$onlineDevices online",
                        showArrow = false
                    )
                }
            }
            
            // Actions Section
            item {
                SettingsSection(
                    title = "Actions"
                ) {
                    SettingsItem(
                        icon = Icons.Default.Refresh,
                        title = "Refresh Devices",
                        subtitle = "Check online status of paired devices",
                        onClick = { viewModel.refreshDevices() }
                    )
                    
                    SettingsItem(
                        icon = Icons.Default.DeleteSweep,
                        title = "Clear All Data",
                        subtitle = "Remove all paired devices and history",
                        onClick = { showClearDataDialog = true },
                        isDestructive = true
                    )
                }
            }
            
            // About Section
            item {
                SettingsSection(
                    title = "About"
                ) {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "Version",
                        subtitle = "1.0.0",
                        showArrow = false
                    )
                    
                    SettingsItem(
                        icon = Icons.Default.Security,
                        title = "Protocol Version",
                        subtitle = "ZypherLink 1.0.0",
                        showArrow = false
                    )
                }
            }
        }
    }
    
    // Device Name Dialog
    if (showDeviceNameDialog) {
        DeviceNameDialog(
            currentName = localDeviceInfo?.deviceName ?: "",
            onDismiss = { showDeviceNameDialog = false },
            onConfirm = { newName ->
                viewModel.updateDeviceName(newName)
                showDeviceNameDialog = false
            }
        )
    }
    
    // Clear Data Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear All Data") },
            text = { 
                Text("This will remove all paired devices and transfer history. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // viewModel.clearAllData() // Implement this method
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryOrange,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    showArrow: Boolean = true,
    isDestructive: Boolean = false
) {
    val contentColor = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    Surface(
        onClick = onClick ?: {},
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDestructive) contentColor else PrimaryOrange
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = contentColor
                    )
                    
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
            }
            
            if (showArrow && onClick != null) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun DeviceNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Device Name") },
        text = {
            Column {
                Text("Enter a new name for this device:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Device Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newName.trim()) },
                enabled = newName.trim().isNotEmpty() && newName.trim() != currentName
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}