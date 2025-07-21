// ZypherLink macOS Receiver Frontend Logic

class ZypherLinkApp {
    constructor() {
        this.isServerRunning = false;
        this.currentQRCode = null;
        this.updateInterval = null;
        this.isWailsReady = false;
        
        console.log('🚀 ZypherLink Frontend initializing...');
        this.waitForWails();
    }

    /**
     * Wait for Wails to be ready before initializing
     */
    waitForWails() {
        let attempts = 0;
        const maxAttempts = 50; // 5 seconds max wait time
        
        const checkWails = () => {
            attempts++;
            console.log(`🔍 Checking Wails readiness... (attempt ${attempts})`);
            
            if (window.go && window.go.main && window.go.main.App) {
                console.log('✅ Wails is ready!');
                this.isWailsReady = true;
                this.testConnection();
                this.initializeApp();
                return;
            }
            
            if (attempts >= maxAttempts) {
                console.error('❌ Wails failed to initialize after 5 seconds');
                this.showError('Failed to connect to application backend');
                return;
            }
            
            setTimeout(checkWails, 100);
        };
        
        checkWails();
    }

    /**
     * Test the Go-JS connection
     */
    async testConnection() {
        try {
            const result = await window.go.main.App.TestConnection();
            console.log('🧪 Connection test result:', result);
        } catch (error) {
            console.error('❌ Connection test failed:', error);
        }
    }

    /**
     * Initialize the application
     */
    async initializeApp() {
        console.log('🎯 Initializing ZypherLink app...');
        
        try {
            this.setupEventListeners();
            this.setupWailsEventListeners();
            
            // Load initial data with retry
            await this.loadInitialDataWithRetry();
            
            // Start periodic updates
            this.startPeriodicUpdates();
            
            this.showNotification('ZypherLink Ready', 'Application initialized successfully', 'success');
        } catch (error) {
            console.error('❌ Failed to initialize app:', error);
            this.showNotification('Initialization Error', 'Failed to start application', 'error');
        }
    }

    /**
     * Load initial data with retry mechanism
     */
    async loadInitialDataWithRetry() {
        const maxRetries = 5;
        
        for (let i = 0; i < maxRetries; i++) {
            try {
                console.log(`📊 Loading initial data (attempt ${i + 1})...`);
                
                await this.updateDeviceInfo();
                await this.updateServerStatus();
                await this.updateQRCode();
                await this.updateTransferHistory();
                
                console.log('✅ Initial data loaded successfully');
                return; // Success, exit retry loop
                
            } catch (error) {
                console.warn(`⚠️ Attempt ${i + 1} failed:`, error);
                
                if (i === maxRetries - 1) {
                    throw error; // Final attempt failed
                }
                
                // Wait before retry
                await new Promise(resolve => setTimeout(resolve, 1000));
            }
        }
    }

    /**
     * Set up event listeners for UI interactions
     */
    setupEventListeners() {
        console.log('🎧 Setting up event listeners...');
        
        // Toggle server button
        const toggleBtn = document.getElementById('toggleServerBtn');
        if (toggleBtn) {
            toggleBtn.addEventListener('click', () => {
                console.log('🔀 Toggle server button clicked');
                this.toggleServer();
            });
        }

        // Regenerate QR button
        const regenerateBtn = document.getElementById('regenerateBtn');
        if (regenerateBtn) {
            regenerateBtn.addEventListener('click', () => {
                console.log('🔄 Regenerate QR button clicked');
                this.regenerateQR();
            });
        }
    }

    /**
     * Set up Wails event listeners
     */
    setupWailsEventListeners() {
        if (!window.runtime) {
            console.warn('⚠️ Wails runtime not available');
            return;
        }

        console.log('📡 Setting up Wails event listeners...');

        // QR code updates
        window.runtime.EventsOn('qr-updated', (qrCode) => {
            console.log('📋 QR code updated event received');
            this.displayQRCode(qrCode);
        });

        // Notifications from backend
        window.runtime.EventsOn('notification', (notification) => {
            console.log('📢 Notification event received:', notification);
            this.showNotification(notification.title, notification.message, notification.type);
        });

        // Transfer progress updates
        window.runtime.EventsOn('transfer-progress', (progress) => {
            console.log('📈 Transfer progress event received:', progress);
            this.updateTransferProgress(progress);
        });

        // App ready event
        window.runtime.EventsOn('app-ready', () => {
            console.log('🎉 App ready event received');
        });
    }

    /**
     * Start periodic updates for dynamic content
     */
    startPeriodicUpdates() {
        console.log('⏰ Starting periodic updates...');
        
        this.updateInterval = setInterval(async () => {
            if (!this.isWailsReady) return;
            
            try {
                await this.updateServerStatus();
                await this.updateTransferHistory();
            } catch (error) {
                console.warn('⚠️ Periodic update failed:', error);
            }
        }, 5000); // Update every 5 seconds
    }

    /**
     * Update device information display
     */
    async updateDeviceInfo() {
        try {
            console.log('📱 Updating device info...');
            const deviceInfo = await window.go.main.App.GetDeviceInfo();
            console.log('📱 Device info received:', deviceInfo);
            
            if (deviceInfo) {
                this.setElementText('deviceName', deviceInfo.device_name || 'Unknown Device');
                this.setElementText('ipAddress', deviceInfo.ip_address || 'Not Available');
                this.setElementText('transferPort', deviceInfo.port ? `${deviceInfo.port}` : 'Not Set');
            }
        } catch (error) {
            console.error('❌ Failed to get device info:', error);
            this.setElementText('deviceName', 'Error loading...');
            this.setElementText('ipAddress', 'Error loading...');
            this.setElementText('transferPort', 'Error loading...');
        }
    }

    /**
     * Update server status display
     */
    async updateServerStatus() {
        try {
            console.log('📊 Updating server status...');
            const status = await window.go.main.App.GetServerStatus();
            console.log('📊 Server status received:', status);
            
            if (status) {
                this.isServerRunning = status.running;
                
                // Update status indicator
                const statusDot = document.querySelector('.status-dot');
                const toggleBtn = document.getElementById('toggleServerBtn');
                
                if (this.isServerRunning) {
                    statusDot?.classList.remove('offline');
                    this.setElementText('statusText', 'Server Running');
                    this.setElementText('toggleText', 'Stop Server');
                    toggleBtn?.classList.remove('btn-start');
                } else {
                    statusDot?.classList.add('offline');
                    this.setElementText('statusText', 'Server Stopped');
                    this.setElementText('toggleText', 'Start Server');
                    toggleBtn?.classList.add('btn-start');
                }
                
                // Update stats
                this.setElementText('deviceCount', status.device_count || 0);
                this.setElementText('transferCount', status.transfer_count || 0);
            }
        } catch (error) {
            console.error('❌ Failed to get server status:', error);
        }
    }

    /**
     * Update QR code display
     */
    async updateQRCode() {
        try {
            console.log('📋 Updating QR code...');
            this.showQRPlaceholder('Loading QR code...');
            
            const qrCode = await window.go.main.App.GetQRCode();
            console.log('📋 QR code received, length:', qrCode ? qrCode.length : 0);
            
            if (qrCode && qrCode.length > 0) {
                this.displayQRCode(qrCode);
            } else {
                console.log('🔄 No QR code available, requesting generation...');
                const newQRCode = await window.go.main.App.RegenerateQR();
                if (newQRCode) {
                    this.displayQRCode(newQRCode);
                } else {
                    this.showQRPlaceholder('Failed to generate QR code');
                }
            }
        } catch (error) {
            console.error('❌ Failed to get QR code:', error);
            this.showQRPlaceholder('Error loading QR code');
        }
    }

    /**
     * Display QR code image
     */
    displayQRCode(qrCode) {
        console.log('🖼️ Displaying QR code...');
        
        const qrPlaceholder = document.getElementById('qrPlaceholder');
        const qrImage = document.getElementById('qrImage');
        
        if (qrCode && qrCode.length > 0) {
            // Hide placeholder and show QR image
            if (qrPlaceholder) qrPlaceholder.style.display = 'none';
            if (qrImage) {
                qrImage.src = `data:image/png;base64,${qrCode}`;
                qrImage.style.display = 'block';
                console.log('✅ QR code displayed successfully');
            }
            
            this.currentQRCode = qrCode;
            
            // Update QR info (placeholder values for now)
            this.setElementText('pairingCode', '******');
            this.setElementText('qrExpiry', this.formatExpiryTime());
            
        } else {
            this.showQRPlaceholder('Invalid QR code data');
        }
    }

    /**
     * Show QR placeholder with message
     */
    showQRPlaceholder(message = 'Generating QR Code...') {
        console.log('📋 Showing QR placeholder:', message);
        
        const qrPlaceholder = document.getElementById('qrPlaceholder');
        const qrImage = document.getElementById('qrImage');
        
        if (qrImage) qrImage.style.display = 'none';
        if (qrPlaceholder) {
            qrPlaceholder.style.display = 'flex';
            const text = qrPlaceholder.querySelector('p');
            if (text) text.textContent = message;
        }
    }

    /**
     * Update transfer history display
     */
    async updateTransferHistory() {
        try {
            const history = await window.go.main.App.GetTransferHistory();
            this.displayTransferHistory(history || []);
        } catch (error) {
            console.error('❌ Failed to get transfer history:', error);
        }
    }

    /**
     * Display transfer history in the UI
     */
    displayTransferHistory(history) {
        const transfersList = document.getElementById('transfersList');
        
        if (!transfersList) return;
        
        if (history.length === 0) {
            transfersList.innerHTML = `
                <div class="no-transfers">
                    <p>No recent transfers</p>
                    <small>Files received from Android devices will appear here</small>
                </div>
            `;
            return;
        }
        
        const transfersHTML = history.map(transfer => `
            <div class="transfer-item">
                <div class="transfer-info">
                    <div class="transfer-name">${this.escapeHtml(transfer.sender_name || 'Unknown Device')}</div>
                    <div class="transfer-details">
                        ${transfer.file_count} files • ${this.formatFileSize(transfer.total_size)} • ${this.formatDate(transfer.timestamp)}
                    </div>
                </div>
                <div class="transfer-status ${transfer.success ? 'success' : 'error'}">
                    ${transfer.success ? 'Success' : 'Failed'}
                </div>
            </div>
        `).join('');
        
        transfersList.innerHTML = transfersHTML;
    }

    /**
     * Toggle server on/off
     */
    async toggleServer() {
        try {
            console.log('🔀 Toggling server status...');
            const newStatus = await window.go.main.App.ToggleServerStatus();
            
            if (newStatus) {
                this.showNotification('Server Started', 'ZypherLink is now accepting file transfers', 'success');
            } else {
                this.showNotification('Server Stopped', 'ZypherLink is no longer accepting files', 'info');
            }
            
            // Update UI immediately
            setTimeout(() => this.updateServerStatus(), 500);
            
        } catch (error) {
            console.error('❌ Failed to toggle server:', error);
            this.showNotification('Server Error', 'Failed to change server status', 'error');
        }
    }

    /**
     * Regenerate QR code
     */
    async regenerateQR() {
        try {
            console.log('🔄 Regenerating QR code...');
            this.showQRPlaceholder('Generating new QR code...');
            
            const newQRCode = await window.go.main.App.RegenerateQR();
            if (newQRCode) {
                this.displayQRCode(newQRCode);
                this.showNotification('QR Code Updated', 'New pairing code generated', 'success');
            } else {
                this.showQRPlaceholder('Failed to generate QR code');
                this.showNotification('QR Generation Failed', 'Could not generate new QR code', 'error');
            }
            
        } catch (error) {
            console.error('❌ Failed to regenerate QR:', error);
            this.showQRPlaceholder('Error generating QR code');
            this.showNotification('QR Generation Failed', 'Could not generate new QR code', 'error');
        }
    }

    /**
     * Update transfer progress display
     */
    updateTransferProgress(progress) {
        console.log('📈 Updating transfer progress:', progress);
        // This would update a progress bar or transfer status
        // For now, just refresh the transfer history
        setTimeout(() => this.updateTransferHistory(), 1000);
    }

    /**
     * Show notification to user
     */
    showNotification(title, message, type = 'info') {
        console.log(`📢 Notification: [${type}] ${title}: ${message}`);
        
        const container = document.getElementById('notificationContainer');
        if (!container) return;
        
        const notification = document.createElement('div');
        notification.className = `notification ${type}`;
        notification.innerHTML = `
            <div class="notification-title">${this.escapeHtml(title)}</div>
            <div class="notification-message">${this.escapeHtml(message)}</div>
        `;
        
        container.appendChild(notification);
        
        // Auto-remove after 5 seconds
        setTimeout(() => {
            if (notification.parentNode) {
                notification.style.animation = 'slideOut 0.3s ease';
                setTimeout(() => notification.remove(), 300);
            }
        }, 5000);
    }

    /**
     * Show error message when Wails connection fails
     */
    showError(message) {
        console.error('💥 Critical error:', message);
        
        // Update UI to show error state
        this.setElementText('deviceName', 'Connection Error');
        this.setElementText('ipAddress', 'Check Console');
        this.setElementText('transferPort', 'See Logs');
        this.setElementText('statusText', 'Backend Error');
        
        this.showQRPlaceholder('Connection Error - Check Console');
    }

    /**
     * Utility functions
     */
    setElementText(id, text) {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = text;
        } else {
            console.warn(`⚠️ Element not found: ${id}`);
        }
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    formatFileSize(bytes) {
        if (bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
    }

    formatDate(timestamp) {
        if (!timestamp) return 'Unknown';
        const date = new Date(timestamp);
        return date.toLocaleString();
    }

    formatExpiryTime() {
        // This would calculate actual expiry time
        // For now, return placeholder
        const expiry = new Date(Date.now() + 10 * 60 * 1000); // 10 minutes from now
        return expiry.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }

    /**
     * Cleanup when app is closing
     */
    destroy() {
        if (this.updateInterval) {
            clearInterval(this.updateInterval);
        }
    }
}

// Add slideOut animation to CSS
const style = document.createElement('style');
style.textContent = `
    @keyframes slideOut {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(100%);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);

// Initialize app when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    console.log('🌟 DOM loaded, starting ZypherLink...');
    window.zyperLinkApp = new ZypherLinkApp();
});

// Cleanup on page unload
window.addEventListener('beforeunload', () => {
    if (window.zyperLinkApp) {
        window.zyperLinkApp.destroy();
    }
});