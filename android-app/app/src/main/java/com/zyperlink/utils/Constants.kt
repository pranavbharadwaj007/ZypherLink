package com.zyperlink.utils

import java.time.Duration

object Constants {
    
    // Protocol
    const val PROTOCOL_VERSION = "1.0.0"
    const val PROTOCOL_NAME = "ZypherLink Transfer Protocol"
    
    // Network Configuration
    const val DISCOVERY_PORT = 8765
    const val TRANSFER_PORT_START = 8766
    const val TRANSFER_PORT_END = 8776
    val BROADCAST_INTERVAL = Duration.ofSeconds(3)
    val DISCOVERY_TIMEOUT = Duration.ofSeconds(10)
    val TRANSFER_TIMEOUT = Duration.ofMinutes(5)
    const val MAX_FILE_SIZE_MB = 1024L
    const val CHUNK_SIZE_KB = 1024L
    
    // Security Settings
    const val TOKEN_LENGTH = 32
    const val TOKEN_EXPIRY_MINUTES = 30L
    const val MAX_PAIRED_DEVICES = 10
    const val PAIRING_CODE_LENGTH = 6
    const val QR_CODE_VERSION = 7
    const val MAX_PAIRING_ATTEMPTS = 3
    val PAIRING_RATE_LIMIT = Duration.ofSeconds(5)
    
    // API Endpoints
    const val ENDPOINT_DISCOVERY = "/api/v1/discover"
    const val ENDPOINT_PAIR = "/api/v1/pair"
    const val ENDPOINT_UPLOAD = "/api/v1/upload"
    const val ENDPOINT_STATUS = "/api/v1/status"
    const val ENDPOINT_DEVICES = "/api/v1/devices"
    
    // Message Types
    const val MSG_DISCOVERY_REQ = "DISCOVERY_REQ"
    const val MSG_DISCOVERY_RESP = "DISCOVERY_RESP"
    const val MSG_PAIR_REQ = "PAIR_REQ"
    const val MSG_PAIR_RESP = "PAIR_RESP"
    const val MSG_TRANSFER_START = "TRANSFER_START"
    const val MSG_TRANSFER_PROGRESS = "TRANSFER_PROGRESS"
    const val MSG_TRANSFER_COMPLETE = "TRANSFER_COMPLETE"
    const val MSG_TRANSFER_ERROR = "TRANSFER_ERROR"
    
    // Device Types
    const val DEVICE_MAC_RECEIVER = "MAC_RECEIVER"
    const val DEVICE_ANDROID_SENDER = "ANDROID_SENDER"
    const val DEVICE_ANDROID_RECEIVER = "ANDROID_RECEIVER"
    
    // Error Codes
    const val ERR_INVALID_TOKEN = 4001
    const val ERR_DEVICE_NOT_PAIRED = 4002
    const val ERR_FILE_TOO_LARGE = 4003
    const val ERR_UNSUPPORTED_FILE_TYPE = 4004
    const val ERR_TRANSFER_TIMEOUT = 4005
    const val ERR_STORAGE_FULL = 4006
    const val ERR_NETWORK = 5001
    const val ERR_SERVER = 5002
    
    // UI Theme Colors (Material Design compatible)
    object Colors {
        const val PRIMARY_ORANGE = 0xFFFF6B35
        const val PRIMARY_PURPLE = 0xFF6B46C1
        const val DARK_PURPLE = 0xFF4C1D95
        const val LIGHT_ORANGE = 0xFFFFA07A
        const val BACKGROUND_DARK = 0xFF1F2937
        const val BACKGROUND_LIGHT = 0xFFF9FAFB
        const val TEXT_PRIMARY = 0xFF111827
        const val TEXT_SECONDARY = 0xFF6B7280
        const val SUCCESS_GREEN = 0xFF10B981
        const val ERROR_RED = 0xFFEF4444
    }
    
    // File Types and Limits
    val SUPPORTED_EXTENSIONS = listOf(
        ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp",
        ".mp4", ".mov", ".avi", ".mkv", ".webm",
        ".pdf", ".doc", ".docx", ".txt", ".md",
        ".zip", ".rar", ".7z",
        ".mp3", ".wav", ".flac", ".m4a"
    )
    
    const val MAX_FILES_PER_TRANSFER = 50
    const val PREVIEW_SIZE_KB = 100L
    
    // Device Capabilities
    const val CAPABILITY_SEND = "SEND"
    const val CAPABILITY_RECEIVE = "RECEIVE"
    const val CAPABILITY_CLIPBOARD = "CLIPBOARD"
    const val CAPABILITY_MULTI_FILE = "MULTI_FILE"
    
    // Device Status
    const val STATUS_AVAILABLE = "AVAILABLE"
    const val STATUS_BUSY = "BUSY"
    const val STATUS_PAIRED = "PAIRED"
    
    // Network Security (Local network ranges)
    val ALLOWED_NETWORKS = listOf(
        "192.168.0.0/16",
        "10.0.0.0/8",
        "172.16.0.0/12"
    )
    
    // File Transfer Limits
    const val MAX_TOTAL_TRANSFER_SIZE_GB = 10L
    const val MAX_FILES_PER_SESSION = 50
    
    // HTTP Headers
    const val HEADER_AUTHORIZATION = "Authorization"
    const val HEADER_CONTENT_TYPE = "Content-Type"
    const val HEADER_USER_AGENT = "User-Agent"
    const val HEADER_ACCEPT = "Accept"
    
    const val CONTENT_TYPE_JSON = "application/json"
    const val CONTENT_TYPE_MULTIPART = "multipart/form-data"
    
    const val USER_AGENT_ANDROID = "ZypherLink-Android/1.0.0"
    
    // Intent extras
    const val EXTRA_FILE_URIS = "extra_file_uris"
    const val EXTRA_DEVICE_INFO = "extra_device_info"
    
    // Notification channels
    const val NOTIFICATION_CHANNEL_TRANSFER = "transfer_channel"
    const val NOTIFICATION_CHANNEL_DISCOVERY = "discovery_channel"
    
    // SharedPreferences keys
    const val PREF_DEVICE_ID = "device_id"
    const val PREF_DEVICE_NAME = "device_name"
    const val PREF_PAIRED_DEVICES = "paired_devices"
    const val PREF_AUTH_TOKENS = "auth_tokens"
}