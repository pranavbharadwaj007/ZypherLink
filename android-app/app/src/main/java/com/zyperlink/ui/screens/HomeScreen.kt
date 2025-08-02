package com.zyperlink.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zyperlink.models.PairedDevice
import com.zyperlink.models.TransferRecord
import com.zyperlink.ui.components.DeviceCard
import com.zyperlink.ui.components.TransferHistoryCard
import com.zyperlink.ui.theme.PrimaryOrange
import com.zyperlink.ui.theme.PrimaryPurple
import com.zyperlink.ui.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    onNavigateToTransfer: () -> Unit,
    onNavigateToPairing: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val localDeviceInfo by viewModel.localDeviceInfo.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val transferHistory by viewModel.transferHistory.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    // Show snackbar for messages/errors
    LaunchedEffect(uiState.message, uiState.error) {
        // Handle UI messages (you might want to use a SnackbarHost here)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(PrimaryOrange, PrimaryPurple)
                )
            )
    ) {
        // Header
        HomeHeader(
            deviceName = localDeviceInfo?.deviceName ?: "ZypherLink",
            onNavigateToSettings = onNavigateToSettings
        )
        
        // Main content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick actions
            item {
                QuickActionsSection(
                    onNavigateToTransfer = onNavigateToTransfer,
                    onNavigateToPairing = onNavigateToPairing
                )
            }
            
            // Paired devices
            item {
                Text(
                    text = "Paired Devices",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            if (pairedDevices.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No Paired Devices",
                        subtitle = "Scan a QR code to pair with a device",
                        icon = Icons.Default.DevicesOther,
                        onAction = onNavigateToPairing,
                        actionText = "Pair Device"
                    )
                }
            } else {
                items(pairedDevices) { device ->
                    DeviceCard(
                        device = device,
                        onSendFiles = onNavigateToTransfer,
                        onRemove = { viewModel.removePairedDevice(device.deviceId) }
                    )
                }
            }
            
            // Transfer history
            item {
                Text(
                    text = "Recent Transfers",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            if (transferHistory.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No Transfer History",
                        subtitle = "Your file transfers will appear here",
                        icon = Icons.Default.History
                    )
                }
            } else {
                items(transferHistory.take(5)) { transfer ->
                    TransferHistoryCard(transfer = transfer)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeHeader(
    deviceName: String,
    onNavigateToSettings: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "ZypherLink",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
        },
        actions = {
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = PrimaryOrange
        )
    )
}

@Composable
private fun QuickActionsSection(
    onNavigateToTransfer: () -> Unit,
    onNavigateToPairing: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        QuickActionCard(
            title = "Send Files",
            subtitle = "Share files with paired devices",
            icon = Icons.Default.Send,
            onClick = onNavigateToTransfer,
            modifier = Modifier.weight(1f)
        )
        
        QuickActionCard(
            title = "Pair Device",
            subtitle = "Scan QR code to connect",
            icon = Icons.Default.QrCode,
            onClick = onNavigateToPairing,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(48.dp),
                tint = PrimaryOrange
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmptyStateCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onAction: (() -> Unit)? = null,
    actionText: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            if (onAction != null && actionText != null) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryOrange
                    )
                ) {
                    Text(actionText)
                }
            }
        }
    }
}