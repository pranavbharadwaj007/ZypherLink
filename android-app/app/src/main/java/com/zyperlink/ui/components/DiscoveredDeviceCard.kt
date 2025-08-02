package com.zyperlink.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zyperlink.models.DeviceInfo
import com.zyperlink.ui.theme.PrimaryOrange
import com.zyperlink.ui.theme.SuccessGreen
import com.zyperlink.utils.Constants
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveredDeviceCard(
    device: DeviceInfo,
    onPair: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Device header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.deviceName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = "${device.ipAddress}:${device.port}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                // Status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier.size(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .aspectRatio(1f)
                        ) {
                            androidx.compose.foundation.Canvas(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                drawCircle(color = SuccessGreen)
                            }
                        }
                    }
                    
                    Text(
                        text = getStatusText(device.status),
                        style = MaterialTheme.typography.labelSmall,
                        color = SuccessGreen
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Device info chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoChip(
                    icon = getDeviceTypeIcon(device.deviceType),
                    text = getDeviceTypeName(device.deviceType)
                )
                
                if (device.capabilities.contains(Constants.CAPABILITY_RECEIVE)) {
                    InfoChip(
                        icon = Icons.Default.FileDownload,
                        text = "Receives Files"
                    )
                }
                
                if (device.capabilities.contains(Constants.CAPABILITY_MULTI_FILE)) {
                    InfoChip(
                        icon = Icons.Default.LibraryAdd,
                        text = "Multi-File"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Last seen info
            Text(
                text = "Discovered: ${formatDiscoveryTime(device.lastSeen)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Pairing info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Requires QR Code Pairing",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Scan the QR code shown on this device",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                Icon(
                    Icons.Default.QrCode,
                    contentDescription = "QR Code required",
                    tint = PrimaryOrange,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getDeviceTypeIcon(deviceType: String): ImageVector {
    return when (deviceType) {
        Constants.DEVICE_MAC_RECEIVER -> Icons.Default.Computer
        Constants.DEVICE_ANDROID_SENDER -> Icons.Default.Phone
        Constants.DEVICE_ANDROID_RECEIVER -> Icons.Default.PhoneAndroid
        else -> Icons.Default.DevicesOther
    }
}

private fun getDeviceTypeName(deviceType: String): String {
    return when (deviceType) {
        Constants.DEVICE_MAC_RECEIVER -> "macOS"
        Constants.DEVICE_ANDROID_SENDER -> "Android"
        Constants.DEVICE_ANDROID_RECEIVER -> "Android"
        else -> "Unknown"
    }
}

private fun getStatusText(status: String): String {
    return when (status) {
        Constants.STATUS_AVAILABLE -> "Available"
        Constants.STATUS_BUSY -> "Busy"
        Constants.STATUS_PAIRED -> "Paired"
        else -> status
    }
}

private fun formatDiscoveryTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 5000 -> "Just now"
        diff < 60_000 -> "Few seconds ago"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        else -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}