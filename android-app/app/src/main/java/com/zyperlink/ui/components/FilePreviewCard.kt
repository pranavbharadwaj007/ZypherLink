package com.zyperlink.ui.components

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile

@Composable
fun FilePreviewCard(
    uri: Uri,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fileInfo = remember(uri) { getFileInfo(context, uri) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // File type icon
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Icon(
                        imageVector = getFileTypeIcon(fileInfo.mimeType, fileInfo.name),
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // File details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileInfo.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = formatFileSize(fileInfo.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Remove button
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove file",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private data class FileInfo(
    val name: String,
    val size: Long,
    val mimeType: String?
)

private fun getFileInfo(context: Context, uri: Uri): FileInfo {
    val contentResolver = context.contentResolver
    val cursor = contentResolver.query(uri, null, null, null, null)
    
    return cursor?.use {
        if (it.moveToFirst()) {
            val displayNameIndex = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeIndex = it.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val mimeTypeIndex = it.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            
            val fileName = if (displayNameIndex >= 0) {
                it.getString(displayNameIndex)
            } else {
                DocumentFile.fromSingleUri(context, uri)?.name ?: "unknown_file"
            }
            
            val fileSize = if (sizeIndex >= 0) {
                it.getLong(sizeIndex)
            } else {
                DocumentFile.fromSingleUri(context, uri)?.length() ?: 0
            }
            
            val mimeType = if (mimeTypeIndex >= 0) {
                it.getString(mimeTypeIndex)
            } else {
                contentResolver.getType(uri)
            }
            
            FileInfo(fileName, fileSize, mimeType)
        } else {
            FileInfo("unknown_file", 0, null)
        }
    } ?: FileInfo("unknown_file", 0, null)
}

private fun getFileTypeIcon(mimeType: String?, fileName: String): ImageVector {
    return when {
        mimeType?.startsWith("image/") == true -> Icons.Default.Image
        mimeType?.startsWith("video/") == true -> Icons.Default.VideoFile
        mimeType?.startsWith("audio/") == true -> Icons.Default.AudioFile
        mimeType?.startsWith("text/") == true -> Icons.Default.TextSnippet
        mimeType == "application/pdf" -> Icons.Default.PictureAsPdf
        fileName.endsWith(".zip", true) || 
        fileName.endsWith(".rar", true) || 
        fileName.endsWith(".7z", true) -> Icons.Default.Archive
        mimeType?.startsWith("application/") == true -> Icons.Default.Description
        else -> Icons.Default.InsertDriveFile
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