package main

import "time"

// ZypherLink Protocol Constants
const (
	PROTOCOL_VERSION = "1.0.0"
	PROTOCOL_NAME    = "ZypherLink Transfer Protocol"
)

// Network Configuration
const (
	DISCOVERY_PORT      = 8765
	TRANSFER_PORT_START = 8766
	TRANSFER_PORT_END   = 8776
	BROADCAST_INTERVAL  = 3 * time.Second
	DISCOVERY_TIMEOUT   = 10 * time.Second
	TRANSFER_TIMEOUT    = 5 * time.Minute
	MAX_FILE_SIZE_MB    = 1024
	CHUNK_SIZE_KB       = 1024
)

// Security Settings
const (
	TOKEN_LENGTH         = 32
	TOKEN_EXPIRY_MINUTES = 30
	MAX_PAIRED_DEVICES   = 10
	PAIRING_CODE_LENGTH  = 6
	QR_CODE_VERSION      = 7
	MAX_PAIRING_ATTEMPTS = 3
	PAIRING_RATE_LIMIT   = 5 * time.Second
)

// API Endpoints
const (
	ENDPOINT_DISCOVERY = "/api/v1/discover"
	ENDPOINT_PAIR      = "/api/v1/pair"
	ENDPOINT_UPLOAD    = "/api/v1/upload"
	ENDPOINT_STATUS    = "/api/v1/status"
	ENDPOINT_DEVICES   = "/api/v1/devices"
)

// Message Types
const (
	MSG_DISCOVERY_REQ    = "DISCOVERY_REQ"
	MSG_DISCOVERY_RESP   = "DISCOVERY_RESP"
	MSG_PAIR_REQ         = "PAIR_REQ"
	MSG_PAIR_RESP        = "PAIR_RESP"
	MSG_TRANSFER_START   = "TRANSFER_START"
	MSG_TRANSFER_PROGRESS = "TRANSFER_PROGRESS"
	MSG_TRANSFER_COMPLETE = "TRANSFER_COMPLETE"
	MSG_TRANSFER_ERROR   = "TRANSFER_ERROR"
)

// Device Types
const (
	DEVICE_MAC_RECEIVER     = "MAC_RECEIVER"
	DEVICE_ANDROID_SENDER   = "ANDROID_SENDER"
	DEVICE_ANDROID_RECEIVER = "ANDROID_RECEIVER"
)

// Error Codes
const (
	ERR_INVALID_TOKEN         = 4001
	ERR_DEVICE_NOT_PAIRED     = 4002
	ERR_FILE_TOO_LARGE       = 4003
	ERR_UNSUPPORTED_FILE_TYPE = 4004
	ERR_TRANSFER_TIMEOUT      = 4005
	ERR_STORAGE_FULL         = 4006
	ERR_NETWORK              = 5001
	ERR_SERVER               = 5002
)

// UI Theme Colors
const (
	COLOR_PRIMARY_ORANGE   = "#FF6B35"
	COLOR_PRIMARY_PURPLE   = "#6B46C1"
	COLOR_DARK_PURPLE      = "#4C1D95"
	COLOR_LIGHT_ORANGE     = "#FFA07A"
	COLOR_BACKGROUND_DARK  = "#1F2937"
	COLOR_BACKGROUND_LIGHT = "#F9FAFB"
	COLOR_TEXT_PRIMARY     = "#111827"
	COLOR_TEXT_SECONDARY   = "#6B7280"
	COLOR_SUCCESS_GREEN    = "#10B981"
	COLOR_ERROR_RED        = "#EF4444"
)

// Device Capabilities
const (
	CAPABILITY_SEND       = "SEND"
	CAPABILITY_RECEIVE    = "RECEIVE"
	CAPABILITY_CLIPBOARD  = "CLIPBOARD"
	CAPABILITY_MULTI_FILE = "MULTI_FILE"
)

// Device Status
const (
	STATUS_AVAILABLE = "AVAILABLE"
	STATUS_BUSY      = "BUSY"
	STATUS_PAIRED    = "PAIRED"
)

// File Transfer Limits
const (
	MAX_TOTAL_TRANSFER_SIZE_GB = 10
	MAX_FILES_PER_SESSION      = 50
	PREVIEW_SIZE_KB           = 100
)

// HTTP Headers
const (
	HEADER_AUTHORIZATION = "Authorization"
	HEADER_CONTENT_TYPE  = "Content-Type"
	HEADER_USER_AGENT    = "User-Agent"
	HEADER_ACCEPT        = "Accept"

	CONTENT_TYPE_JSON      = "application/json"
	CONTENT_TYPE_MULTIPART = "multipart/form-data"

	USER_AGENT_MAC = "ZypherLink-macOS/1.0.0"
)

// File Types and Extensions
var (
	SUPPORTED_EXTENSIONS = []string{
		".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp",
		".mp4", ".mov", ".avi", ".mkv", ".webm",
		".pdf", ".doc", ".docx", ".txt", ".md",
		".zip", ".rar", ".7z",
		".mp3", ".wav", ".flac", ".m4a",
	}

	MAX_FILES_PER_TRANSFER = 50
)

// Network Security
var (
	ALLOWED_NETWORKS = []string{
		"192.168.0.0/16",
		"10.0.0.0/8",
		"172.16.0.0/12",
	}
)