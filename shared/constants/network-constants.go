package constants

import "time"

// ZypherLink Protocol Constants
const (
	ProtocolVersion = "1.0.0"
	ProtocolName    = "ZypherLink Transfer Protocol"
)

// Network Configuration
const (
	DiscoveryPort      = 8765
	TransferPortStart  = 8766
	TransferPortEnd    = 8776
	BroadcastInterval  = 3 * time.Second
	DiscoveryTimeout   = 10 * time.Second
	TransferTimeout    = 5 * time.Minute
	MaxFileSizeMB      = 1024
	ChunkSizeKB        = 1024
)

// Security Settings
const (
	TokenLength        = 32
	TokenExpiryMinutes = 30
	MaxPairedDevices   = 10
	PairingCodeLength  = 6
	QRCodeVersion      = 7
	MaxPairingAttempts = 3
	PairingRateLimit   = 5 * time.Second
)

// API Endpoints
const (
	EndpointDiscovery = "/api/v1/discover"
	EndpointPair      = "/api/v1/pair"
	EndpointUpload    = "/api/v1/upload"
	EndpointStatus    = "/api/v1/status"
	EndpointDevices   = "/api/v1/devices"
)

// Message Types
const (
	MsgDiscoveryReq   = "DISCOVERY_REQ"
	MsgDiscoveryResp  = "DISCOVERY_RESP"
	MsgPairReq        = "PAIR_REQ"
	MsgPairResp       = "PAIR_RESP"
	MsgTransferStart  = "TRANSFER_START"
	MsgTransferProg   = "TRANSFER_PROGRESS"
	MsgTransferComp   = "TRANSFER_COMPLETE"
	MsgTransferError  = "TRANSFER_ERROR"
)

// Device Types
const (
	DeviceMacReceiver     = "MAC_RECEIVER"
	DeviceAndroidSender   = "ANDROID_SENDER"
	DeviceAndroidReceiver = "ANDROID_RECEIVER"
)

// Error Codes
const (
	ErrInvalidToken        = 4001
	ErrDeviceNotPaired     = 4002
	ErrFileTooLarge       = 4003
	ErrUnsupportedFileType = 4004
	ErrTransferTimeout     = 4005
	ErrStorageFull        = 4006
	ErrNetwork            = 5001
	ErrServer             = 5002
)

// UI Theme Colors
const (
	ColorPrimaryOrange   = "#FF6B35"
	ColorPrimaryPurple   = "#6B46C1"
	ColorDarkPurple      = "#4C1D95"
	ColorLightOrange     = "#FFA07A"
	ColorBackgroundDark  = "#1F2937"
	ColorBackgroundLight = "#F9FAFB"
	ColorTextPrimary     = "#111827"
	ColorTextSecondary   = "#6B7280"
	ColorSuccessGreen    = "#10B981"
	ColorErrorRed        = "#EF4444"
)

// File Types and Limits
var (
	SupportedExtensions = []string{
		".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp",
		".mp4", ".mov", ".avi", ".mkv", ".webm",
		".pdf", ".doc", ".docx", ".txt", ".md",
		".zip", ".rar", ".7z",
		".mp3", ".wav", ".flac", ".m4a",
	}
	
	MaxFilesPerTransfer = 50
	PreviewSizeKB      = 100
)

// Device Capabilities
const (
	CapabilitySend      = "SEND"
	CapabilityReceive   = "RECEIVE" 
	CapabilityClipboard = "CLIPBOARD"
	CapabilityMultiFile = "MULTI_FILE"
)

// Device Status
const (
	StatusAvailable = "AVAILABLE"
	StatusBusy      = "BUSY"
	StatusPaired    = "PAIRED"
)

// Network Security
var (
	AllowedNetworks = []string{
		"192.168.0.0/16",
		"10.0.0.0/8", 
		"172.16.0.0/12",
	}
)

// File Transfer Limits
const (
	MaxTotalTransferSizeGB = 10
	MaxFilesPerSession     = 50
)

// HTTP Headers
const (
	HeaderAuthorization = "Authorization"
	HeaderContentType   = "Content-Type"
	HeaderUserAgent     = "User-Agent"
	HeaderAccept        = "Accept"
	
	ContentTypeJSON      = "application/json"
	ContentTypeMultipart = "multipart/form-data"
	
	UserAgentMac = "ZypherLink-macOS/1.0.0"
)