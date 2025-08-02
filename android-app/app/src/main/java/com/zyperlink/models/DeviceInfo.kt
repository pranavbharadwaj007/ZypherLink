package com.zyperlink.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo(
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_name") val deviceName: String,
    @SerialName("device_type") val deviceType: String,
    @SerialName("status") val status: String,
    @SerialName("ip_address") val ipAddress: String,
    @SerialName("port") val port: Int,
    @SerialName("last_seen") val lastSeen: Long = System.currentTimeMillis(),
    @SerialName("capabilities") val capabilities: List<String> = emptyList(),
    @SerialName("protocol_version") val protocolVersion: String = "1.0.0"
)

@Serializable
data class DiscoveryRequest(
    @SerialName("message_type") val messageType: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_name") val deviceName: String,
    @SerialName("device_type") val deviceType: String,
    @SerialName("protocol_version") val protocolVersion: String = "1.0.0",
    @SerialName("capabilities") val capabilities: List<String>,
    @SerialName("network_info") val networkInfo: NetworkInfo,
    @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class DiscoveryResponse(
    @SerialName("message_type") val messageType: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_name") val deviceName: String,
    @SerialName("device_type") val deviceType: String,
    @SerialName("status") val status: String,
    @SerialName("pairing_required") val pairingRequired: Boolean = true,
    @SerialName("transfer_endpoint") val transferEndpoint: String? = null,
    @SerialName("qr_pairing_data") val qrPairingData: String? = null
)

@Serializable
data class NetworkInfo(
    @SerialName("ip_address") val ipAddress: String,
    @SerialName("port") val port: Int
)

@Serializable
data class PairingRequest(
    @SerialName("message_type") val messageType: String,
    @SerialName("requester_device_id") val requesterDeviceId: String,
    @SerialName("requester_name") val requesterName: String,
    @SerialName("pairing_code") val pairingCode: String,
    @SerialName("public_key") val publicKey: String? = null,
    @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class PairingResponse(
    @SerialName("message_type") val messageType: String,
    @SerialName("success") val success: Boolean,
    @SerialName("device_id") val deviceId: String,
    @SerialName("auth_token") val authToken: String? = null,
    @SerialName("token_expires_at") val tokenExpiresAt: Long? = null,
    @SerialName("error_message") val errorMessage: String? = null
)

@Serializable
data class FileInfo(
    @SerialName("name") val name: String,
    @SerialName("size") val size: Long,
    @SerialName("mime_type") val mimeType: String? = null,
    @SerialName("checksum") val checksum: String,
    @SerialName("thumbnail") val thumbnail: String? = null
)

@Serializable
data class TransferRequest(
    @SerialName("message_type") val messageType: String,
    @SerialName("auth_token") val authToken: String,
    @SerialName("transfer_id") val transferId: String,
    @SerialName("files") val files: List<FileInfo>,
    @SerialName("total_size") val totalSize: Long
)

@Serializable
data class TransferProgress(
    @SerialName("message_type") val messageType: String,
    @SerialName("transfer_id") val transferId: String,
    @SerialName("bytes_transferred") val bytesTransferred: Long,
    @SerialName("total_bytes") val totalBytes: Long,
    @SerialName("current_file") val currentFile: String? = null,
    @SerialName("files_completed") val filesCompleted: Int = 0,
    @SerialName("total_files") val totalFiles: Int = 0,
    @SerialName("transfer_speed_bps") val transferSpeedBps: Long = 0
)

@Serializable
data class TransferComplete(
    @SerialName("message_type") val messageType: String,
    @SerialName("transfer_id") val transferId: String,
    @SerialName("success") val success: Boolean,
    @SerialName("files_transferred") val filesTransferred: Int = 0,
    @SerialName("total_bytes") val totalBytes: Long = 0,
    @SerialName("duration_ms") val durationMs: Long = 0,
    @SerialName("saved_location") val savedLocation: String? = null,
    @SerialName("error_details") val errorDetails: ErrorDetails? = null
)

@Serializable
data class ErrorDetails(
    @SerialName("error_code") val errorCode: Int,
    @SerialName("error_message") val errorMessage: String,
    @SerialName("failed_files") val failedFiles: List<String> = emptyList()
)

data class PairedDevice(
    val deviceId: String,
    val deviceName: String,
    val deviceType: String,
    val ipAddress: String,
    val port: Int,
    val authToken: String,
    val tokenExpiresAt: Long,
    val lastSeen: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false
)

data class TransferRecord(
    val id: String,
    val deviceId: String,
    val deviceName: String,
    val fileCount: Int,
    val totalSize: Long,
    val timestamp: Long,
    val success: Boolean,
    val direction: TransferDirection,
    val errorMessage: String? = null
)

enum class TransferDirection {
    SEND, RECEIVE
}

data class QRPairingData(
    val deviceId: String,
    val deviceName: String,
    val ipAddress: String,
    val port: Int,
    val pairingCode: String,
    val timestamp: Long = System.currentTimeMillis()
)