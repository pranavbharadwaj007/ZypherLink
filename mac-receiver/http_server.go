package main

import (
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"

	"github.com/gorilla/mux"
)

// HTTPServer handles all HTTP requests for file transfers and API
type HTTPServer struct {
	router       *mux.Router
	server       *http.Server
	DeviceInfo   *DeviceInfo
	tokenManager *TokenManager
	transfers    map[string]*TransferSession
	transfersMux sync.RWMutex
	history      []TransferRecord
	historyMux   sync.RWMutex
}

// TransferSession represents an active file transfer
type TransferSession struct {
	ID            string        `json:"transfer_id"`
	AuthToken     string        `json:"auth_token"`
	SenderDevice  string        `json:"sender_device"`
	Files         []FileInfo    `json:"files"`
	TotalSize     int64         `json:"total_size"`
	BytesReceived int64         `json:"bytes_received"`
	FilesReceived int           `json:"files_received"`
	Status        string        `json:"status"`
	StartTime     time.Time     `json:"start_time"`
	SaveLocation  string        `json:"save_location"`
	CurrentFile   string        `json:"current_file"`
	Speed         int64         `json:"transfer_speed_bps"`
}

// TransferRecord represents a completed transfer for history
type TransferRecord struct {
	ID           string    `json:"id"`
	SenderName   string    `json:"sender_name"`
	FileCount    int       `json:"file_count"`
	TotalSize    int64     `json:"total_size"`
	Duration     int64     `json:"duration_ms"`
	Success      bool      `json:"success"`
	Timestamp    time.Time `json:"timestamp"`
	SaveLocation string    `json:"save_location"`
}

// FileInfo represents file metadata
type FileInfo struct {
	Name      string `json:"name"`
	Size      int64  `json:"size"`
	MimeType  string `json:"mime_type"`
	Checksum  string `json:"checksum"`
	Thumbnail string `json:"thumbnail,omitempty"`
}

// API Request/Response structures

// PairRequest represents device pairing request
type PairRequest struct {
	MessageType      string `json:"message_type"`
	RequesterID      string `json:"requester_device_id"`
	RequesterName    string `json:"requester_name"`
	PairingCode      string `json:"pairing_code"`
	PublicKey        string `json:"public_key,omitempty"`
	Timestamp        int64  `json:"timestamp"`
}

// PairResponse represents pairing response
type PairResponse struct {
	MessageType   string `json:"message_type"`
	Success       bool   `json:"success"`
	DeviceID      string `json:"device_id"`
	AuthToken     string `json:"auth_token,omitempty"`
	TokenExpires  int64  `json:"token_expires_at,omitempty"`
	ErrorMessage  string `json:"error_message,omitempty"`
}

// TransferStartRequest represents file transfer initiation
type TransferStartRequest struct {
	MessageType string     `json:"message_type"`
	AuthToken   string     `json:"auth_token"`
	TransferID  string     `json:"transfer_id"`
	Files       []FileInfo `json:"files"`
	TotalSize   int64      `json:"total_size"`
}

// TransferProgressResponse represents transfer progress
type TransferProgressResponse struct {
	MessageType      string `json:"message_type"`
	TransferID       string `json:"transfer_id"`
	BytesTransferred int64  `json:"bytes_transferred"`
	TotalBytes       int64  `json:"total_bytes"`
	CurrentFile      string `json:"current_file"`
	FilesCompleted   int    `json:"files_completed"`
	TotalFiles       int    `json:"total_files"`
	TransferSpeed    int64  `json:"transfer_speed_bps"`
}

// ErrorResponse represents API error response
type ErrorResponse struct {
	Error     string                 `json:"error"`
	Message   string                 `json:"message"`
	Details   map[string]interface{} `json:"details,omitempty"`
	Timestamp int64                  `json:"timestamp"`
	RequestID string                 `json:"request_id"`
}

// NewHTTPServer creates a new HTTP server instance
func NewHTTPServer(port int, deviceInfo *DeviceInfo) *HTTPServer {
	s := &HTTPServer{
		DeviceInfo:   deviceInfo,
		tokenManager: NewTokenManager(),
		transfers:    make(map[string]*TransferSession),
		history:      make([]TransferRecord, 0),
	}

	s.setupRoutes()
	s.server = &http.Server{
		Addr:         fmt.Sprintf(":%d", port),
		Handler:      s.router,
		ReadTimeout:  30 * time.Second,
		WriteTimeout: 30 * time.Second,
	}

	return s
}

// setupRoutes configures all HTTP routes
func (s *HTTPServer) setupRoutes() {
	s.router = mux.NewRouter()

	// API routes
	api := s.router.PathPrefix("/api/v1").Subrouter()
	
	// Device pairing endpoint
	api.HandleFunc("/pair", s.handlePairing).Methods("POST")
	
	// File upload endpoint
	api.HandleFunc("/upload", s.authMiddleware(s.handleUpload)).Methods("POST")
	
	// Transfer status endpoint
	api.HandleFunc("/status/{transfer_id}", s.authMiddleware(s.handleTransferStatus)).Methods("GET")
	
	// Paired devices endpoint
	api.HandleFunc("/devices", s.authMiddleware(s.handleGetDevices)).Methods("GET")
	
	// Discovery endpoint (for future UDP-over-HTTP discovery)
	api.HandleFunc("/discover", s.handleDiscovery).Methods("POST")

	// Health check
	api.HandleFunc("/health", s.handleHealth).Methods("GET")

	// CORS middleware for local development
	s.router.Use(s.corsMiddleware)
}

// Start starts the HTTP server
func (s *HTTPServer) Start() error {
	log.Printf("🌐 Starting HTTP server on :%d", s.DeviceInfo.Port)
	return s.server.ListenAndServe()
}

// Stop stops the HTTP server
func (s *HTTPServer) Stop() {
	if s.server != nil {
		s.server.Close()
	}
}

// Middleware

// authMiddleware validates bearer token
func (s *HTTPServer) authMiddleware(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		authHeader := r.Header.Get("Authorization")
		if authHeader == "" {
			s.sendError(w, "MISSING_AUTH_TOKEN", "Authorization header required", http.StatusUnauthorized)
			return
		}

		if !strings.HasPrefix(authHeader, "Bearer ") {
			s.sendError(w, "INVALID_AUTH_FORMAT", "Invalid authorization format", http.StatusUnauthorized)
			return
		}

		token := strings.TrimPrefix(authHeader, "Bearer ")
		if !s.tokenManager.ValidateToken(token) {
			s.sendError(w, "INVALID_AUTH_TOKEN", "Token invalid or expired", http.StatusUnauthorized)
			return
		}

		next(w, r)
	}
}

// corsMiddleware adds CORS headers
func (s *HTTPServer) corsMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Access-Control-Allow-Origin", "*")
		w.Header().Set("Access-Control-Allow-Methods", "POST, GET, OPTIONS")
		w.Header().Set("Access-Control-Allow-Headers", "Accept, Content-Type, Authorization")
		
		if r.Method == "OPTIONS" {
			w.WriteHeader(http.StatusOK)
			return
		}
		
		next.ServeHTTP(w, r)
	})
}

// Route Handlers

// handlePairing handles device pairing requests
func (s *HTTPServer) handlePairing(w http.ResponseWriter, r *http.Request) {
	var pairReq PairRequest
	if err := json.NewDecoder(r.Body).Decode(&pairReq); err != nil {
		s.sendError(w, "INVALID_REQUEST", "Invalid JSON request", http.StatusBadRequest)
		return
	}

	log.Printf("🤝 Pairing request from device: %s (%s)", pairReq.RequesterName, pairReq.RequesterID)

	// Validate pairing code (this should match the current QR code)
	if !s.validatePairingCode(pairReq.PairingCode) {
		s.sendError(w, "INVALID_PAIRING_CODE", "Pairing code invalid or expired", http.StatusUnauthorized)
		return
	}

	// Generate auth token
	authToken := s.tokenManager.GenerateToken(pairReq.RequesterID)
	expiresAt := time.Now().Add(TOKEN_EXPIRY_MINUTES * time.Minute).Unix()

	// Store paired device
	// TODO: Implement device storage

	response := PairResponse{
		MessageType:  MSG_PAIR_RESP,
		Success:      true,
		DeviceID:     s.DeviceInfo.ID,
		AuthToken:    authToken,
		TokenExpires: expiresAt,
	}

	log.Printf("✅ Device paired successfully: %s", pairReq.RequesterName)
	s.sendJSON(w, response)
}

// handleUpload handles file upload requests
func (s *HTTPServer) handleUpload(w http.ResponseWriter, r *http.Request) {
	// Parse multipart form
	err := r.ParseMultipartForm(32 << 20) // 32MB max memory
	if err != nil {
		s.sendError(w, "INVALID_MULTIPART", "Failed to parse multipart form", http.StatusBadRequest)
		return
	}

	// Get metadata
	metadataStr := r.FormValue("metadata")
	if metadataStr == "" {
		s.sendError(w, "MISSING_METADATA", "Transfer metadata required", http.StatusBadRequest)
		return
	}

	var transferReq TransferStartRequest
	if err := json.Unmarshal([]byte(metadataStr), &transferReq); err != nil {
		s.sendError(w, "INVALID_METADATA", "Invalid transfer metadata", http.StatusBadRequest)
		return
	}

	log.Printf("📤 Starting file transfer: %s (%d files, %d bytes)", 
		transferReq.TransferID, len(transferReq.Files), transferReq.TotalSize)

	// Create transfer session
	session := &TransferSession{
		ID:            transferReq.TransferID,
		AuthToken:     transferReq.AuthToken,
		Files:         transferReq.Files,
		TotalSize:     transferReq.TotalSize,
		Status:        "RECEIVING",
		StartTime:     time.Now(),
		SaveLocation:  s.getDownloadPath(),
	}

	s.transfersMux.Lock()
	s.transfers[session.ID] = session
	s.transfersMux.Unlock()

	// Process uploaded files
	go s.processUploadedFiles(session, r)

	// Return initial progress
	progress := TransferProgressResponse{
		MessageType:      MSG_TRANSFER_PROGRESS,
		TransferID:       session.ID,
		BytesTransferred: 0,
		TotalBytes:       session.TotalSize,
		FilesCompleted:   0,
		TotalFiles:       len(session.Files),
	}

	s.sendJSON(w, progress)
}

// handleTransferStatus returns transfer progress
func (s *HTTPServer) handleTransferStatus(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	transferID := vars["transfer_id"]

	s.transfersMux.RLock()
	session, exists := s.transfers[transferID]
	s.transfersMux.RUnlock()

	if !exists {
		s.sendError(w, "TRANSFER_NOT_FOUND", "Transfer session not found", http.StatusNotFound)
		return
	}

	progress := TransferProgressResponse{
		MessageType:      MSG_TRANSFER_PROGRESS,
		TransferID:       session.ID,
		BytesTransferred: session.BytesReceived,
		TotalBytes:       session.TotalSize,
		CurrentFile:      session.CurrentFile,
		FilesCompleted:   session.FilesReceived,
		TotalFiles:       len(session.Files),
		TransferSpeed:    session.Speed,
	}

	s.sendJSON(w, progress)
}

// handleGetDevices returns paired devices (placeholder)
func (s *HTTPServer) handleGetDevices(w http.ResponseWriter, r *http.Request) {
	// TODO: Implement device management
	devices := map[string]interface{}{
		"devices": []interface{}{},
	}
	s.sendJSON(w, devices)
}

// handleDiscovery handles discovery requests
func (s *HTTPServer) handleDiscovery(w http.ResponseWriter, r *http.Request) {
	// TODO: Implement HTTP-based discovery as fallback
	response := map[string]interface{}{
		"message_type": MSG_DISCOVERY_RESP,
		"device_id":    s.DeviceInfo.ID,
		"device_name":  s.DeviceInfo.Name,
		"device_type":  s.DeviceInfo.Type,
		"status":       s.DeviceInfo.Status,
	}
	s.sendJSON(w, response)
}

// handleHealth returns server health status
func (s *HTTPServer) handleHealth(w http.ResponseWriter, r *http.Request) {
	health := map[string]interface{}{
		"status":    "healthy",
		"timestamp": time.Now().Unix(),
		"version":   PROTOCOL_VERSION,
	}
	s.sendJSON(w, health)
}

// Helper methods

// processUploadedFiles handles the actual file saving
func (s *HTTPServer) processUploadedFiles(session *TransferSession, r *http.Request) {
	defer func() {
		// Add to history when complete
		s.addToHistory(session)
	}()

	startTime := time.Now()
	totalBytes := int64(0)

	for i, fileInfo := range session.Files {
		session.CurrentFile = fileInfo.Name
		
		// Get file from form
		file, header, err := r.FormFile(fmt.Sprintf("file_%d", i))
		if err != nil {
			log.Printf("❌ Error getting file %s: %v", fileInfo.Name, err)
			continue
		}
		defer file.Close()

		// Save file
		savedPath, bytesWritten, err := s.saveFile(file, fileInfo, session.SaveLocation)
		if err != nil {
			log.Printf("❌ Error saving file %s: %v", fileInfo.Name, err)
			continue
		}

		totalBytes += bytesWritten
		session.BytesReceived = totalBytes
		session.FilesReceived = i + 1

		// Calculate speed
		elapsed := time.Since(startTime).Seconds()
		if elapsed > 0 {
			session.Speed = int64(float64(totalBytes) / elapsed)
		}

		log.Printf("✅ Saved file: %s (%d bytes) -> %s", header.Filename, bytesWritten, savedPath)
	}

	session.Status = "COMPLETED"
	log.Printf("🎉 Transfer completed: %s (%d files, %d bytes)", session.ID, session.FilesReceived, totalBytes)
}

// saveFile saves an uploaded file to the download directory
func (s *HTTPServer) saveFile(src io.Reader, fileInfo FileInfo, basePath string) (string, int64, error) {
	// Sanitize filename
	filename := filepath.Base(fileInfo.Name)
	if filename == "." || filename == "/" {
		filename = "uploaded_file"
	}

	// Create full path
	fullPath := filepath.Join(basePath, filename)

	// Create directory if needed
	if err := os.MkdirAll(filepath.Dir(fullPath), 0755); err != nil {
		return "", 0, err
	}

	// Create destination file
	dst, err := os.Create(fullPath)
	if err != nil {
		return "", 0, err
	}
	defer dst.Close()

	// Copy file content
	bytesWritten, err := io.Copy(dst, src)
	if err != nil {
		return "", 0, err
	}

	// TODO: Verify checksum if provided
	
	return fullPath, bytesWritten, nil
}

// getDownloadPath returns the download directory path
func (s *HTTPServer) getDownloadPath() string {
	homeDir, err := os.UserHomeDir()
	if err != nil {
		log.Printf("Warning: Could not get home directory: %v", err)
		return "./downloads"
	}
	
	downloadPath := filepath.Join(homeDir, "Downloads", "ZypherLink")
	
	// Create directory if it doesn't exist
	if err := os.MkdirAll(downloadPath, 0755); err != nil {
		log.Printf("Warning: Could not create download directory: %v", err)
		return "./downloads"
	}
	
	return downloadPath
}

// validatePairingCode validates a pairing code against current QR
func (s *HTTPServer) validatePairingCode(code string) bool {
	// Check if code is 6 digits
	if len(code) != 6 {
		return false
	}
	
	// Check if all characters are digits
	if !stringContainsOnly(code, "0123456789") {
		return false
	}
	
	// TODO: Implement actual validation against current QR code
	log.Printf("🔍 Validating pairing code: %s", code)
	return true
}

// addToHistory adds completed transfer to history
func (s *HTTPServer) addToHistory(session *TransferSession) {
	s.historyMux.Lock()
	defer s.historyMux.Unlock()

	duration := time.Since(session.StartTime).Milliseconds()
	
	record := TransferRecord{
		ID:           session.ID,
		SenderName:   "Unknown Device", // TODO: Get from paired device info
		FileCount:    session.FilesReceived,
		TotalSize:    session.BytesReceived,
		Duration:     duration,
		Success:      session.Status == "COMPLETED",
		Timestamp:    session.StartTime,
		SaveLocation: session.SaveLocation,
	}

	s.history = append(s.history, record)
	
	// Keep only last 50 transfers
	if len(s.history) > 50 {
		s.history = s.history[len(s.history)-50:]
	}
}

// GetTransferHistory returns transfer history
func (s *HTTPServer) GetTransferHistory() []TransferRecord {
	s.historyMux.RLock()
	defer s.historyMux.RUnlock()
	
	// Return a copy
	history := make([]TransferRecord, len(s.history))
	copy(history, s.history)
	return history
}

// Utility methods

// sendJSON sends JSON response
func (s *HTTPServer) sendJSON(w http.ResponseWriter, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(data); err != nil {
		log.Printf("Error encoding JSON response: %v", err)
	}
}

// sendError sends error response
func (s *HTTPServer) sendError(w http.ResponseWriter, errorCode, message string, status int) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	
	response := ErrorResponse{
		Error:     errorCode,
		Message:   message,
		Timestamp: time.Now().Unix(),
		RequestID: generateRequestID(),
	}
	
	json.NewEncoder(w).Encode(response)
}

// generateRequestID generates a unique request ID
func generateRequestID() string {
	return fmt.Sprintf("req_%d", time.Now().UnixNano())
}

// stringContainsOnly checks if string contains only specified characters
func stringContainsOnly(s, chars string) bool {
	for _, r := range s {
		if !strings.ContainsRune(chars, r) {
			return false
		}
	}
	return true
}