package main

import (
	"context"
	"fmt"
	"log"
	"net"
	"sync"
	"time"

	"github.com/wailsapp/wails/v2/pkg/runtime"
)

// App struct
type App struct {
	ctx           context.Context
	server        *ZypherServer
	discoveryPort int
	transferPort  int
	isRunning     bool
	mutex         sync.RWMutex
}

// ZypherServer holds the HTTP server and discovery service
type ZypherServer struct {
	HTTPServer      *HTTPServer
	DiscoveryServer *DiscoveryServer
	DeviceManager   *DeviceManager
	QRManager       *QRManager
}

// DeviceInfo represents this macOS device
type DeviceInfo struct {
	ID           string    `json:"device_id"`
	Name         string    `json:"device_name"`
	Type         string    `json:"device_type"`
	Status       string    `json:"status"`
	IPAddress    string    `json:"ip_address"`
	Port         int       `json:"port"`
	LastSeen     time.Time `json:"last_seen"`
	Capabilities []string  `json:"capabilities"`
}

// NewApp creates a new App application struct
func NewApp() *App {
	return &App{
		discoveryPort: DISCOVERY_PORT,
		transferPort:  TRANSFER_PORT_START,
		isRunning:     false,
	}
}

// OnStartup is called when the app starts
func (a *App) OnStartup(ctx context.Context) {
	a.ctx = ctx
	log.Println("🚀 ZypherLink macOS Receiver starting...")

	// Initialize server components
	a.initializeServer()
	
	// Start the server
	go a.startServer()
	
	// Give the server a moment to start
	go func() {
		time.Sleep(2 * time.Second)
		if a.ctx != nil {
			runtime.EventsEmit(a.ctx, "app-ready", true)
		}
	}()
}

// OnDomReady is called after front-end resources have been loaded
func (a *App) OnDomReady(ctx context.Context) {
	log.Println("🌐 Frontend DOM ready")
	runtime.WindowSetTitle(ctx, "ZypherLink - macOS Receiver")
	runtime.WindowCenter(ctx)
}

// OnBeforeClose is called when the application is about to quit
func (a *App) OnBeforeClose(ctx context.Context) (prevent bool) {
	log.Println("🛑 Shutting down ZypherLink server...")
	a.stopServer()
	return false
}

// OnShutdown is called when the application is shutting down
func (a *App) OnShutdown(ctx context.Context) {
	log.Println("👋 ZypherLink shut down complete")
}

// TestConnection - Simple method to test if binding works
func (a *App) TestConnection() string {
	log.Println("🧪 Frontend called TestConnection")
	return "Connection working! 🎉"
}

// GetDeviceInfo returns current device information
func (a *App) GetDeviceInfo() map[string]interface{} {
	log.Println("🔍 Frontend requesting device info...")
	
	// Get real device info
	hostname := getDeviceName()
	localIP := a.getLocalIPAddress()
	
	result := map[string]interface{}{
		"device_name": hostname,
		"ip_address":  localIP,
		"port":        a.transferPort,
		"device_type": DEVICE_MAC_RECEIVER,
		"status":      STATUS_AVAILABLE,
	}
	
	log.Printf("📱 Returning device info: %+v", result)
	return result
}

// GetQRCode returns the current pairing QR code
func (a *App) GetQRCode() string {
	log.Println("📋 Frontend requesting QR code...")
	
	if a.server == nil || a.server.QRManager == nil {
		log.Println("⚠️ QR Manager not initialized, creating one...")
		
		// Create minimal device info for QR generation
		deviceInfo := &DeviceInfo{
			ID:        generateDeviceID(),
			Name:      getDeviceName(),
			Type:      DEVICE_MAC_RECEIVER,
			Status:    STATUS_AVAILABLE,
			IPAddress: a.getLocalIPAddress(),
			Port:      a.transferPort,
		}
		
		qrManager := NewQRManager(deviceInfo)
		qrCode := qrManager.GenerateNewQR()
		log.Printf("📋 Generated temporary QR code, length: %d", len(qrCode))
		return qrCode
	}
	
	qrCode := a.server.QRManager.GetCurrentQRCode()
	if qrCode == "" {
		log.Println("🔄 No QR code available, generating new one...")
		qrCode = a.server.QRManager.GenerateNewQR()
	}
	
	log.Printf("📋 Returning QR code length: %d", len(qrCode))
	return qrCode
}

// RegenerateQR creates a new QR code
func (a *App) RegenerateQR() string {
	log.Println("🔄 Frontend requesting QR regeneration...")
	
	// Create minimal device info for QR generation
	deviceInfo := &DeviceInfo{
		ID:        generateDeviceID(),
		Name:      getDeviceName(),
		Type:      DEVICE_MAC_RECEIVER,
		Status:    STATUS_AVAILABLE,
		IPAddress: a.getLocalIPAddress(),
		Port:      a.transferPort,
	}
	
	qrManager := NewQRManager(deviceInfo)
	qrCode := qrManager.GenerateNewQR()
	
	log.Println("🔄 Generated new QR code for pairing")
	
	// Notify frontend
	if a.ctx != nil {
		runtime.EventsEmit(a.ctx, "qr-updated", qrCode)
	}
	
	return qrCode
}

// GetServerStatus returns current server status
func (a *App) GetServerStatus() map[string]interface{} {
	log.Println("📊 Frontend requesting server status...")
	
	a.mutex.RLock()
	running := a.isRunning
	a.mutex.RUnlock()

	status := map[string]interface{}{
		"running":        running,
		"discovery_port": a.discoveryPort,
		"transfer_port":  a.transferPort,
		"device_count":   0,
		"transfer_count": 0,
	}

	if a.server != nil && a.server.DeviceManager != nil {
		status["device_count"] = len(a.server.DeviceManager.GetPairedDevices())
	}

	if a.server != nil && a.server.HTTPServer != nil {
		status["transfer_count"] = len(a.server.HTTPServer.GetTransferHistory())
	}

	log.Printf("📊 Returning server status: %+v", status)
	return status
}

// ToggleServerStatus starts/stops the server
func (a *App) ToggleServerStatus() bool {
	log.Println("🔀 Frontend requesting server toggle...")
	
	a.mutex.RLock()
	running := a.isRunning
	a.mutex.RUnlock()

	if running {
		a.stopServer()
		a.sendNotification("ZypherLink Stopped", "Server stopped - not accepting files")
		log.Println("🛑 Server stopped by user")
		return false
	} else {
		go a.startServer()
		log.Println("🚀 Server started by user")
		return true
	}
}

// GetPairedDevices returns list of paired devices
func (a *App) GetPairedDevices() []PairedDevice {
	log.Println("👥 Frontend requesting paired devices...")
	
	if a.server == nil || a.server.DeviceManager == nil {
		return []PairedDevice{}
	}
	return a.server.DeviceManager.GetPairedDevices()
}

// GetTransferHistory returns recent file transfers
func (a *App) GetTransferHistory() []TransferRecord {
	log.Println("📁 Frontend requesting transfer history...")
	
	if a.server == nil || a.server.HTTPServer == nil {
		return []TransferRecord{}
	}
	return a.server.HTTPServer.GetTransferHistory()
}

// Internal methods

func (a *App) initializeServer() {
	a.mutex.Lock()
	defer a.mutex.Unlock()

	// Get local IP address
	localIP := a.getLocalIPAddress()
	
	// Create device info for this Mac
	deviceInfo := &DeviceInfo{
		ID:           generateDeviceID(),
		Name:         getDeviceName(),
		Type:         DEVICE_MAC_RECEIVER,
		Status:       STATUS_AVAILABLE,
		IPAddress:    localIP,
		Port:         a.transferPort,
		LastSeen:     time.Now(),
		Capabilities: []string{CAPABILITY_RECEIVE, CAPABILITY_MULTI_FILE},
	}

	// Initialize components
	a.server = &ZypherServer{
		HTTPServer:      NewHTTPServer(a.transferPort, deviceInfo),
		DiscoveryServer: NewDiscoveryServer(a.discoveryPort, deviceInfo),
		DeviceManager:   NewDeviceManager(),
		QRManager:       NewQRManager(deviceInfo),
	}

	log.Printf("📱 Device initialized: %s (%s)", deviceInfo.Name, deviceInfo.ID)
	log.Printf("🌐 Local IP: %s, Transfer Port: %d", localIP, a.transferPort)
}

func (a *App) startServer() {
	a.mutex.Lock()
	a.isRunning = true
	a.mutex.Unlock()

	log.Println("🔥 Starting ZypherLink server components...")

	// Start HTTP server for file transfers
	go func() {
		if err := a.server.HTTPServer.Start(); err != nil {
			log.Printf("❌ HTTP Server error: %v", err)
			a.sendNotification("Server Error", fmt.Sprintf("Failed to start HTTP server: %v", err))
		}
	}()

	// Start UDP discovery server
	go func() {
		if err := a.server.DiscoveryServer.Start(); err != nil {
			log.Printf("❌ Discovery Server error: %v", err)
			a.sendNotification("Discovery Error", fmt.Sprintf("Failed to start discovery: %v", err))
		}
	}()

	// Generate initial QR code
	go func() {
		time.Sleep(2 * time.Second) // Wait for servers to start
		if a.server != nil && a.server.QRManager != nil {
			qrCode := a.server.QRManager.GenerateNewQR()
			log.Println("🔄 Generated initial QR code for pairing")
			
			// Notify frontend
			if a.ctx != nil {
				runtime.EventsEmit(a.ctx, "qr-updated", qrCode)
			}
		}
	}()

	log.Println("✅ All server components started successfully")
	a.sendNotification("ZypherLink Ready", "Ready to receive files from Android devices")
}

func (a *App) stopServer() {
	a.mutex.Lock()
	defer a.mutex.Unlock()

	if !a.isRunning {
		return
	}

	a.isRunning = false

	if a.server != nil {
		if a.server.HTTPServer != nil {
			a.server.HTTPServer.Stop()
		}
		if a.server.DiscoveryServer != nil {
			a.server.DiscoveryServer.Stop()
		}
	}
}

func (a *App) getLocalIPAddress() string {
	conn, err := net.Dial("udp", "8.8.8.8:80")
	if err != nil {
		log.Printf("Warning: Could not determine local IP: %v", err)
		return "127.0.0.1"
	}
	defer conn.Close()

	localAddr := conn.LocalAddr().(*net.UDPAddr)
	return localAddr.IP.String()
}

func (a *App) sendNotification(title, message string) {
	if a.ctx != nil {
		runtime.EventsEmit(a.ctx, "notification", map[string]string{
			"title":   title,
			"message": message,
			"type":    "info",
		})
	}
}