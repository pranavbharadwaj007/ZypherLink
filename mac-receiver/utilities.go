package main

import (
	"crypto/md5"
	"crypto/rand"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"log"
	"net"
	"os"
	"sync"
	"time"

	"github.com/skip2/go-qrcode"
)

// TokenManager handles authentication tokens
type TokenManager struct {
	tokens   map[string]*TokenInfo
	tokensMux sync.RWMutex
}

// TokenInfo represents a stored authentication token
type TokenInfo struct {
	Token     string    `json:"token"`
	DeviceID  string    `json:"device_id"`
	CreatedAt time.Time `json:"created_at"`
	ExpiresAt time.Time `json:"expires_at"`
	Active    bool      `json:"active"`
}

// QRManager handles QR code generation for device pairing
type QRManager struct {
	deviceInfo    *DeviceInfo
	currentQR     string
	currentCode   string
	qrExpiry      time.Time
	qrMux         sync.RWMutex
}

// QRPairingData represents the data encoded in QR codes
type QRPairingData struct {
	DeviceID        string `json:"device_id"`
	DeviceName      string `json:"device_name"`
	DeviceType      string `json:"device_type"`
	PairingCode     string `json:"pairing_code"`
	NetworkEndpoint string `json:"network_endpoint"`
	PublicKey       string `json:"public_key,omitempty"`
	ExpiresAt       int64  `json:"expires_at"`
	ProtocolVersion string `json:"protocol_version"`
}

// DiscoveryServer handles UDP device discovery
type DiscoveryServer struct {
	port       int
	deviceInfo *DeviceInfo
	conn       *net.UDPConn
	running    bool
	runMux     sync.RWMutex
}

// DeviceManager manages paired devices
type DeviceManager struct {
	pairedDevices map[string]*PairedDevice
	devicesMux    sync.RWMutex
}

// PairedDevice represents a paired Android device
type PairedDevice struct {
	DeviceID     string    `json:"device_id"`
	DeviceName   string    `json:"device_name"`
	DeviceType   string    `json:"device_type"`
	PairedAt     time.Time `json:"paired_at"`
	LastSeen     time.Time `json:"last_seen"`
	IsOnline     bool      `json:"is_online"`
	TrustLevel   string    `json:"trust_level"`
	TransferCount int       `json:"transfer_count"`
}

// TokenManager Implementation

// NewTokenManager creates a new token manager
func NewTokenManager() *TokenManager {
	return &TokenManager{
		tokens: make(map[string]*TokenInfo),
	}
}

// GenerateToken creates a new authentication token for a device
func (tm *TokenManager) GenerateToken(deviceID string) string {
	tm.tokensMux.Lock()
	defer tm.tokensMux.Unlock()

	// Generate random token
	tokenBytes := make([]byte, TOKEN_LENGTH)
	rand.Read(tokenBytes)
	token := hex.EncodeToString(tokenBytes)

	// Store token info
	tm.tokens[token] = &TokenInfo{
		Token:     token,
		DeviceID:  deviceID,
		CreatedAt: time.Now(),
		ExpiresAt: time.Now().Add(TOKEN_EXPIRY_MINUTES * time.Minute),
		Active:    true,
	}

	log.Printf("🔑 Generated auth token for device: %s (expires in %d minutes)", 
		deviceID, TOKEN_EXPIRY_MINUTES)
	
	// Clean up expired tokens
	go tm.cleanupExpiredTokens()

	return token
}

// ValidateToken checks if a token is valid and not expired
func (tm *TokenManager) ValidateToken(token string) bool {
	tm.tokensMux.RLock()
	defer tm.tokensMux.RUnlock()

	tokenInfo, exists := tm.tokens[token]
	if !exists {
		return false
	}

	if !tokenInfo.Active || time.Now().After(tokenInfo.ExpiresAt) {
		return false
	}

	return true
}

// GetTokenInfo returns information about a token
func (tm *TokenManager) GetTokenInfo(token string) *TokenInfo {
	tm.tokensMux.RLock()
	defer tm.tokensMux.RUnlock()

	if tokenInfo, exists := tm.tokens[token]; exists {
		return tokenInfo
	}
	return nil
}

// RevokeToken deactivates a token
func (tm *TokenManager) RevokeToken(token string) {
	tm.tokensMux.Lock()
	defer tm.tokensMux.Unlock()

	if tokenInfo, exists := tm.tokens[token]; exists {
		tokenInfo.Active = false
		log.Printf("🚫 Revoked auth token for device: %s", tokenInfo.DeviceID)
	}
}

// cleanupExpiredTokens removes expired tokens
func (tm *TokenManager) cleanupExpiredTokens() {
	tm.tokensMux.Lock()
	defer tm.tokensMux.Unlock()

	now := time.Now()
	for token, tokenInfo := range tm.tokens {
		if now.After(tokenInfo.ExpiresAt) {
			delete(tm.tokens, token)
		}
	}
}

// QRManager Implementation

// NewQRManager creates a new QR code manager
func NewQRManager(deviceInfo *DeviceInfo) *QRManager {
	return &QRManager{
		deviceInfo: deviceInfo,
	}
}

// GenerateNewQR creates a new QR code with fresh pairing data
func (qm *QRManager) GenerateNewQR() string {
	qm.qrMux.Lock()
	defer qm.qrMux.Unlock()

	// Generate 6-digit pairing code
	qm.currentCode = generatePairingCode()
	qm.qrExpiry = time.Now().Add(10 * time.Minute)

	// Create pairing data
	pairingData := QRPairingData{
		DeviceID:        qm.deviceInfo.ID,
		DeviceName:      qm.deviceInfo.Name,
		DeviceType:      qm.deviceInfo.Type,
		PairingCode:     qm.currentCode,
		NetworkEndpoint: fmt.Sprintf("http://%s:%d/api/v1/pair", qm.deviceInfo.IPAddress, qm.deviceInfo.Port),
		ExpiresAt:       qm.qrExpiry.Unix(),
		ProtocolVersion: PROTOCOL_VERSION,
	}

	// Convert to JSON and encode as Base64
	jsonData, err := json.Marshal(pairingData)
	if err != nil {
		log.Printf("Error marshaling QR data: %v", err)
		return ""
	}

	base64Data := base64.StdEncoding.EncodeToString(jsonData)

	// Generate QR code
	qrCode, err := qrcode.Encode(base64Data, qrcode.Medium, 256)
	if err != nil {
		log.Printf("Error generating QR code: %v", err)
		return ""
	}

	qm.currentQR = base64.StdEncoding.EncodeToString(qrCode)

	log.Printf("📋 Generated new QR code - Pairing code: %s (expires at %s)", 
		qm.currentCode, qm.qrExpiry.Format("15:04:05"))

	return qm.currentQR
}

// GetCurrentQRCode returns the current QR code
func (qm *QRManager) GetCurrentQRCode() string {
	qm.qrMux.RLock()
	defer qm.qrMux.RUnlock()

	// Check if QR code is expired
	if time.Now().After(qm.qrExpiry) {
		return ""
	}

	return qm.currentQR
}

// ValidatePairingCode checks if a pairing code matches the current one
func (qm *QRManager) ValidatePairingCode(code string) bool {
	qm.qrMux.RLock()
	defer qm.qrMux.RUnlock()

	if time.Now().After(qm.qrExpiry) {
		return false
	}

	return qm.currentCode == code
}

// DiscoveryServer Implementation

// NewDiscoveryServer creates a new UDP discovery server
func NewDiscoveryServer(port int, deviceInfo *DeviceInfo) *DiscoveryServer {
	return &DiscoveryServer{
		port:       port,
		deviceInfo: deviceInfo,
		running:    false,
	}
}

// Start begins listening for UDP discovery broadcasts
func (ds *DiscoveryServer) Start() error {
	ds.runMux.Lock()
	defer ds.runMux.Unlock()

	if ds.running {
		return fmt.Errorf("discovery server already running")
	}

	addr, err := net.ResolveUDPAddr("udp", fmt.Sprintf(":%d", ds.port))
	if err != nil {
		return err
	}

	ds.conn, err = net.ListenUDP("udp", addr)
	if err != nil {
		return err
	}

	ds.running = true
	log.Printf("🔍 Discovery server listening on UDP port %d", ds.port)

	go ds.handleDiscoveryRequests()
	return nil
}

// Stop stops the discovery server
func (ds *DiscoveryServer) Stop() {
	ds.runMux.Lock()
	defer ds.runMux.Unlock()

	if !ds.running {
		return
	}

	ds.running = false
	if ds.conn != nil {
		ds.conn.Close()
	}
	log.Println("🛑 Discovery server stopped")
}

// handleDiscoveryRequests processes incoming UDP discovery requests
func (ds *DiscoveryServer) handleDiscoveryRequests() {
	buffer := make([]byte, 1024)

	for ds.running {
		n, clientAddr, err := ds.conn.ReadFromUDP(buffer)
		if err != nil {
			if ds.running {
				log.Printf("Error reading UDP packet: %v", err)
			}
			continue
		}

		// Parse discovery request
		var discoveryReq map[string]interface{}
		if err := json.Unmarshal(buffer[:n], &discoveryReq); err != nil {
			log.Printf("Invalid discovery request from %s", clientAddr)
			continue
		}

		log.Printf("🔍 Discovery request from %s: %s", 
			clientAddr, discoveryReq["device_name"])

		// Send discovery response
		go ds.sendDiscoveryResponse(clientAddr)
	}
}

// sendDiscoveryResponse sends a discovery response to a client
func (ds *DiscoveryServer) sendDiscoveryResponse(clientAddr *net.UDPAddr) {
	response := map[string]interface{}{
		"message_type":     MSG_DISCOVERY_RESP,
		"device_id":        ds.deviceInfo.ID,
		"device_name":      ds.deviceInfo.Name,
		"device_type":      ds.deviceInfo.Type,
		"status":           ds.deviceInfo.Status,
		"pairing_required": true,
		"transfer_endpoint": fmt.Sprintf("http://%s:%d/api/v1/upload", 
			ds.deviceInfo.IPAddress, ds.deviceInfo.Port),
		"protocol_version": PROTOCOL_VERSION,
	}

	responseData, err := json.Marshal(response)
	if err != nil {
		log.Printf("Error marshaling discovery response: %v", err)
		return
	}

	_, err = ds.conn.WriteToUDP(responseData, clientAddr)
	if err != nil {
		log.Printf("Error sending discovery response: %v", err)
	}
}

// DeviceManager Implementation

// NewDeviceManager creates a new device manager
func NewDeviceManager() *DeviceManager {
	return &DeviceManager{
		pairedDevices: make(map[string]*PairedDevice),
	}
}

// AddPairedDevice adds a new paired device
func (dm *DeviceManager) AddPairedDevice(deviceID, deviceName, deviceType string) {
	dm.devicesMux.Lock()
	defer dm.devicesMux.Unlock()

	device := &PairedDevice{
		DeviceID:      deviceID,
		DeviceName:    deviceName,
		DeviceType:    deviceType,
		PairedAt:      time.Now(),
		LastSeen:      time.Now(),
		IsOnline:      true,
		TrustLevel:    "trusted",
		TransferCount: 0,
	}

	dm.pairedDevices[deviceID] = device
	log.Printf("🤝 Added paired device: %s (%s)", deviceName, deviceID)
}

// GetPairedDevices returns all paired devices
func (dm *DeviceManager) GetPairedDevices() []PairedDevice {
	dm.devicesMux.RLock()
	defer dm.devicesMux.RUnlock()

	devices := make([]PairedDevice, 0, len(dm.pairedDevices))
	for _, device := range dm.pairedDevices {
		devices = append(devices, *device)
	}

	return devices
}

// UpdateDeviceLastSeen updates the last seen time for a device
func (dm *DeviceManager) UpdateDeviceLastSeen(deviceID string) {
	dm.devicesMux.Lock()
	defer dm.devicesMux.Unlock()

	if device, exists := dm.pairedDevices[deviceID]; exists {
		device.LastSeen = time.Now()
		device.IsOnline = true
	}
}

// Utility Functions

// generateDeviceID creates a unique device identifier
func generateDeviceID() string {
	// Use MAC address and hostname to create a stable device ID
	hostname, _ := os.Hostname()
	interfaces, _ := net.Interfaces()
	
	var macAddr string
	for _, iface := range interfaces {
		if iface.Flags&net.FlagUp != 0 && iface.Flags&net.FlagLoopback == 0 {
			macAddr = iface.HardwareAddr.String()
			break
		}
	}

	// Create MD5 hash of hostname + MAC address
	hasher := md5.New()
	hasher.Write([]byte(hostname + macAddr + "zyperlink"))
	return hex.EncodeToString(hasher.Sum(nil))
}

// generatePairingCode creates a 6-digit pairing code
func generatePairingCode() string {
	code := make([]byte, 3)
	rand.Read(code)
	
	// Convert to 6-digit number
	num := int(code[0])<<16 | int(code[1])<<8 | int(code[2])
	return fmt.Sprintf("%06d", num%1000000)
}

// getDeviceName returns a friendly device name
func getDeviceName() string {
	hostname, err := os.Hostname()
	if err != nil {
		return "Unknown Mac"
	}
	
	// Clean up hostname
	if len(hostname) > 20 {
		hostname = hostname[:20]
	}
	
	return hostname
}