package com.zyperlink.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zyperlink.models.PairedDevice
import com.zyperlink.ui.components.DeviceCard
import com.zyperlink.ui.components.FilePreviewCard
import com.zyperlink.ui.theme.PrimaryOrange
import com.zyperlink.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPairing: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val sharedFiles by viewModel.sharedFiles.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val transferProgress by viewModel.transferProgress.collectAsState()
    val isTransferring by viewModel.isTransferring.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    var selectedFiles by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedDevice by remember { mutableStateOf<PairedDevice?>(null) }
    
    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedFiles = uris
    }
    
    // Initialize with shared files
    LaunchedEffect(sharedFiles) {
        if (sharedFiles.isNotEmpty()) {
            selectedFiles = sharedFiles
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top bar
        TopAppBar(
            title = { Text("Send Files") },
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
        
        // Main content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // File selection section
            item {
                FileSelectionSection(
                    selectedFiles = selectedFiles,
                    onSelectFiles = { filePickerLauncher.launch("*/*") },
                    onRemoveFile = { uri ->
                        selectedFiles = selectedFiles.filter { it != uri }
                    }
                )
            }
            
            // Device selection section
            item {
                Text(
                    text = "Select Destination",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            if (pairedDevices.isEmpty()) {
                item {
                    EmptyDevicesCard(onNavigateToPairing = onNavigateToPairing)
                }
            } else {
                items(pairedDevices.filter { it.isOnline }) { device ->
                    SelectableDeviceCard(
                        device = device,
                        isSelected = selectedDevice?.deviceId == device.deviceId,
                        onSelect = { selectedDevice = device }
                    )
                }
            }
            
            // Transfer button
            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        selectedDevice?.let { device ->
                            viewModel.sendFiles(selectedFiles, device)
                        }
                    },
                    enabled = selectedFiles.isNotEmpty() && selectedDevice != null && !isTransferring,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryOrange
                    )
                ) {
                    if (isTransferring) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Transferring...")
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send ${selectedFiles.size} Files")
                    }
                }
            }
            
            // Transfer progress
            transferProgress?.let { progress ->
                item {
                    TransferProgressCard(progress = progress)
                }
            }
        }
    }
    
    // Handle transfer completion
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            // Clear selected files on successful transfer
            if (it.contains("successfully")) {
                selectedFiles = emptyList()
                selectedDevice = null
                viewModel.clearSharedFiles()
            }
            viewModel.clearMessage()
        }
    }
}

@Composable
private fun FileSelectionSection(
    selectedFiles: List<Uri>,
    onSelectFiles: () -> Unit,
    onRemoveFile: (Uri) -> Unit
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
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Selected Files",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedButton(onClick = onSelectFiles) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Files")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (selectedFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No files selected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                selectedFiles.forEach { uri ->
                    FilePreviewCard(
                        uri = uri,
                        onRemove = { onRemoveFile(uri) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectableDeviceCard(
    device: PairedDevice,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryOrange.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
            width = 2.dp,
            brush = androidx.compose.ui.graphics.SolidColor(PrimaryOrange)
        ) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = PrimaryOrange
                )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.deviceName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${device.ipAddress}:${device.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Icon(
                Icons.Default.Computer,
                contentDescription = null,
                tint = if (isSelected) PrimaryOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun EmptyDevicesCard(
    onNavigateToPairing: () -> Unit
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
                Icons.Default.DevicesOther,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No Paired Devices",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Pair with a device to start sending files",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onNavigateToPairing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryOrange
                )
            ) {
                Icon(Icons.Default.QrCode, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pair Device")
            }
        }
    }
}

@Composable
private fun TransferProgressCard(
    progress: com.zyperlink.models.TransferProgress
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
                .padding(16.dp)
        ) {
            Text(
                text = "Transfer Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = if (progress.totalBytes > 0) {
                    progress.bytesTransferred.toFloat() / progress.totalBytes.toFloat()
                } else 0f,
                modifier = Modifier.fillMaxWidth(),
                color = PrimaryOrange
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${progress.filesCompleted}/${progress.totalFiles} files",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Text(
                    text = "${(progress.bytesTransferred * 100 / progress.totalBytes)}%",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}