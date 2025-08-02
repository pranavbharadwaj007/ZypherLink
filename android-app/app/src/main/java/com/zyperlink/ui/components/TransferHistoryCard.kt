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
import com.zyperlink.models.TransferDirection
import com.zyperlink.models.TransferRecord
import com.zyperlink.ui.theme.ErrorRed
import com.zyperlink.ui.theme.PrimaryOrange
import com.zyperlink.ui.theme.PrimaryPurple
import com.zyperlink.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransferHistoryCard(
    transfer: TransferRecord,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Transfer info
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Direction icon
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (transfer.direction) {
                        TransferDirection.SEND -> PrimaryOrange.copy(alpha = 0.1f)
                        TransferDirection.RECEIVE -> PrimaryPurple.copy(alpha = 0.1f)
                    }
                ) {
                    Icon(
                        imageVector = when (transfer.direction) {
                            TransferDirection.SEND -> Icons.Default.Upload
                            TransferDirection.RECEIVE -> Icons.Default.Download
                        },
                        contentDescription = null,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp),
                        tint = when (transfer.direction) {
                            TransferDirection.SEND -> PrimaryOrange
                            TransferDirection.RECEIVE -> PrimaryPurple
                        }
                    )
                }
                
                // Transfer details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transfer.deviceName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = "${transfer.fileCount} files • ${formatFileSize(transfer.totalSize)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Text(
                        text = formatTransferTime(transfer.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            
            // Status indicator
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (transfer.success) SuccessGreen.copy(alpha = 0.1f) else ErrorRed.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (transfer.success) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (transfer.success) SuccessGreen else ErrorRed
                        )
                        
                        Text(
                            text = if (transfer.success) "Success" else "Failed",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (transfer.success) SuccessGreen else ErrorRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                if (!transfer.success && transfer.errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = transfer.errorMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = ErrorRed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes == 0L) return "0 B"
    
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    
    return String.format(
        "%.1f %s",
        bytes / Math.pow(1024.0, digitGroups.toDouble()),
        units[digitGroups]
    )
}

private fun formatTransferTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}