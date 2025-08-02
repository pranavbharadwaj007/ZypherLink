package com.zyperlink.services

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.zyperlink.models.*
import com.zyperlink.network.ApiService
import com.zyperlink.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val retrofit: Retrofit
) {
    private val TAG = "TransferManager"
    
    private val _transferProgress = MutableStateFlow<TransferProgress?>(null)
    val transferProgress: StateFlow<TransferProgress?> = _transferProgress.asStateFlow()
    
    private val _isTransferring = MutableStateFlow(false)
    val isTransferring: StateFlow<Boolean> = _isTransferring.asStateFlow()
    
    private var currentTransferJob: Job? = null
    
    suspend fun sendFiles(
        fileUris: List<Uri>,
        targetDevice: PairedDevice
    ): Result<TransferComplete> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting file transfer to ${targetDevice.deviceName}")
            _isTransferring.value = true
            
            // Create API service for target device
            val apiService = createApiServiceForDevice(targetDevice)
            
            // Prepare file information
            val fileInfoList = mutableListOf<FileInfo>()
            val fileParts = mutableListOf<MultipartBody.Part>()
            var totalSize = 0L
            
            for (uri in fileUris) {
                val fileInfo = createFileInfo(uri)
                if (fileInfo != null) {
                    fileInfoList.add(fileInfo)
                    totalSize += fileInfo.size
                    
                    // Create multipart body part
                    val filePart = createMultipartBodyPart(uri, fileInfo.name)
                    if (filePart != null) {
                        fileParts.add(filePart)
                    }
                }
            }
            
            if (fileInfoList.isEmpty()) {
                return@withContext Result.failure(Exception("No valid files to transfer"))
            }
            
            // Generate transfer ID
            val transferId = UUID.randomUUID().toString().replace("-", "").take(16)
            
            // Create transfer request
            val transferRequest = TransferRequest(
                messageType = Constants.MSG_TRANSFER_START,
                authToken = targetDevice.authToken,
                transferId = transferId,
                files = fileInfoList,
                totalSize = totalSize
            )
            
            // Start transfer
            currentTransferJob = CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Initialize progress
                    _transferProgress.value = TransferProgress(
                        messageType = Constants.MSG_TRANSFER_PROGRESS,
                        transferId = transferId,
                        bytesTransferred = 0,
                        totalBytes = totalSize,
                        totalFiles = fileInfoList.size
                    )
                    
                    // Start the transfer on the server
                    val startResponse = apiService.startTransfer(
                        "Bearer ${targetDevice.authToken}",
                        transferRequest
                    )
                    
                    if (!startResponse.isSuccessful) {
                        throw Exception("Failed to start transfer: ${startResponse.message()}")
                    }
                    
                    // Upload files
                    val transferInfo = """
                        {
                            "transfer_id": "$transferId",
                            "file_count": ${fileInfoList.size},
                            "total_size": $totalSize
                        }
                    """.trimIndent()
                    
                    val response = apiService.uploadFiles(
                        "Bearer ${targetDevice.authToken}",
                        transferInfo.toRequestBody("application/json".toMediaTypeOrNull()),
                        fileParts
                    )
                    
                    if (response.isSuccessful) {
                        val result = response.body() ?: TransferComplete(
                            messageType = Constants.MSG_TRANSFER_COMPLETE,
                            transferId = transferId,
                            success = true
                        )
                        
                        Log.d(TAG, "Transfer completed successfully")
                        Result.success(result)
                    } else {
                        throw Exception("Transfer failed: ${response.message()}")
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Transfer error", e)
                    Result.failure(e)
                }
            }
            
            currentTransferJob?.join()
            return@withContext Result.success(TransferComplete(
                messageType = Constants.MSG_TRANSFER_COMPLETE,
                transferId = transferId,
                success = true,
                filesTransferred = fileInfoList.size,
                totalBytes = totalSize
            ))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in sendFiles", e)
            Result.failure(e)
        } finally {
            _isTransferring.value = false
            _transferProgress.value = null
        }
    }
    
    private fun createApiServiceForDevice(device: PairedDevice): ApiService {
        val baseUrl = "http://${device.ipAddress}:${device.port}/"
        val deviceRetrofit = retrofit.newBuilder()
            .baseUrl(baseUrl)
            .build()
        
        return deviceRetrofit.create(ApiService::class.java)
    }
    
    private suspend fun createFileInfo(uri: Uri): FileInfo? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(uri, null, null, null, null)
            
            cursor?.use {
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
                    
                    // Calculate checksum
                    val checksum = calculateChecksum(uri)
                    
                    return@withContext FileInfo(
                        name = fileName,
                        size = fileSize,
                        mimeType = mimeType,
                        checksum = checksum
                    )
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error creating file info for $uri", e)
            null
        }
    }
    
    private suspend fun calculateChecksum(uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val inputStream = context.contentResolver.openInputStream(uri)
            
            inputStream?.use { stream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating checksum", e)
            "0".repeat(64) // Return zero hash on error
        }
    }
    
    private fun createMultipartBodyPart(uri: Uri, fileName: String): MultipartBody.Part? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File(context.cacheDir, fileName)
            
            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            val requestBody = tempFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("files", fileName, requestBody)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating multipart body part", e)
            null
        }
    }
    
    fun cancelTransfer() {
        Log.d(TAG, "Cancelling current transfer")
        currentTransferJob?.cancel()
        _isTransferring.value = false
        _transferProgress.value = null
    }
}