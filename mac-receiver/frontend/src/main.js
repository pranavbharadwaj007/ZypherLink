// Import Wails runtime and Go bindings
import '../wailsjs/runtime/runtime.js';
import * as App from '../wailsjs/go/main/App.js';

class ZypherLinkApp {
    constructor() {
        this.isServerRunning = false;
        this.currentQRCode = null;
        this.updateInterval = null;
        this.isWailsReady = false;
        
        console.log('🚀 ZypherLink Frontend initializing...');
        this.waitForWails();
    }

    waitForWails() {
        let attempts = 0;
        const maxAttempts = 50;
        
        const checkWails = () => {
            attempts++;
            console.log(`🔍 Checking Wails readiness... (attempt ${attempts})`);
            this.log(`Checking Wails... (${attempts}/${maxAttempts})`);
            
            if (window.go && window.go.main && window.go.main.App) {
                console.log('✅ Wails is ready!');
                this.log('✅ Wails ready! Initializing app...');
                this.isWailsReady = true;
                this.testConnection();
                this.initializeApp();
                return;
            }
            
            if (attempts >= maxAttempts) {
                console.error('❌ Wails failed to initialize after 5 seconds');
                this.log('❌ Wails failed to initialize');
                this.showError('Failed to connect to application backend');
                return;
            }
            
            setTimeout(checkWails, 100);
        };
        
        checkWails();
    }

    async testConnection() {
        try {
            const result = await App.TestConnection();
            console.log('🧪 Connection test result:', result);
            this.log(`Connection test: ${result}`);
        } catch (error) {
            console.error('❌ Connection test failed:', error);
            this.log(`❌ Connection test failed: ${error}`);
        }
    }

    async initializeApp() {
        console.log('🎯 Initializing ZypherLink app...');
        this.log('🎯 Initializing ZypherLink app...');
        
        try {
            this.setupEventListeners();
            this.setupWailsEventListeners();
            
            await this.loadInitialDataWithRetry();
            this.startPeriodicUpdates();
            
            this.showNotification('ZypherLink Ready', 'Application initialized successfully', 'success');
            this.log('✅ App fully initialized');
        } catch (error) {
            console.error('❌ Failed to initialize app:', error);
            this.log(`❌ Initialization error: ${error}`);
            this.showNotification('Initialization Error', 'Failed to start application', 'error');
        }
    }

    async loadInitialDataWithRetry() {
        const maxRetries = 5;
        
        for (let i = 0; i < maxRetries; i++) {
            try {
                console.log(`📊 Loading initial data (attempt ${i + 1})...`);
                this.log(`📊 Loading initial data (attempt ${i + 1})...`);
                
                await this.updateDeviceInfo();
                await this.updateServerStatus();
                await this.updateQRCode();
                await this.updateTransferHistory();
                
                console.log('✅ Initial data loaded successfully');
                this.log('✅ Initial data loaded successfully');
                return;
                
            } catch (error) {
                console.warn(`⚠️ Attempt ${i + 1} failed:`, error);
                this.log(`⚠️ Attempt ${i + 1} failed: ${error}`);
                
                if (i === maxRetries - 1) {
                    throw error;
                }
                
                await new Promise(resolve => setTimeout(resolve, 1000));
            }
        }
    }

    setupEventListeners() {
        console.log('🎧 Setting up event listeners...');
        
        const toggleBtn = document.getElementById('toggleServerBtn');
        if (toggleBtn) {
            toggleBtn.addEventListener('click', () => {
                console.log('🔀 Toggle server button clicked');
                this.toggleServer();
            });
        }

        const regenerateBtn = document.getElementById('regenerateBtn');
        if (regenerateBtn) {
            regenerateBtn.addEventListener('click', () => {
                console.log('🔄 Regenerate QR button clicked');
                this.regenerateQR();
            });
        }
    }

    setupWailsEventListeners() {
        if (!window.runtime) {
            console.warn('⚠️ Wails runtime not available');
            return;
        }

        console.log('📡 Setting up Wails event listeners...');

        window.runtime.EventsOn('qr-updated', (qrCode) => {
            console.log('📋 QR code updated event received');
            this.displayQRCode(qrCode);
        });

        window.runtime.EventsOn('notification', (notification) => {
            console.log('📢 Notification event received:', notification);
            this.showNotification(notification.title, notification.message, notification.type);
        });

        window.runtime.EventsOn('transfer-progress', (progress) => {
            console.log('📈 Transfer progress event received:', progress);
            this.updateTransferProgress(progress);
        });

        window.runtime.EventsOn('app-ready', () => {
            console.log('🎉 App ready event received');
        });
    }

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
        }, 5000);
    }

    async updateDeviceInfo() {
        try {
            console.log('📱 Updating device info...');
            const deviceInfo = await App.GetDeviceInfo();
            console.log('📱 Device info received:', deviceInfo);
            this.log(`Device info: ${JSON.stringify(deviceInfo)}`);
            
            if (deviceInfo) {
                this.setElementText('deviceName', deviceInfo.device_name || 'Unknown Device');
                this.setElementText('ipAddress', deviceInfo.ip_address || 'Not Available');
                this.setElementText('transferPort', deviceInfo.port ? `${deviceInfo.port}` : 'Not Set');
            }
        } catch (error) {
            console.error('❌ Failed to get device info:', error);
            this.log(`❌ Device info error: ${error}`);
            this.setElementText('deviceName', 'Error loading...');
            this.setElementText('ipAddress', 'Error loading...');
            this.setElementText('transferPort', 'Error loading...');
        }
    }

    async updateServerStatus() {
        try {
            console.log('📊 Updating server status...');
            const status = await App.GetServerStatus();
            console.log('📊 Server status received:', status);
            
            if (status) {
                this.isServerRunning = status.running;
                
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
                
                this.setElementText('deviceCount', status.device_count || 0);
                this.setElementText('transferCount', status.transfer_count || 0);
            }
        } catch (error) {
            console.error('❌ Failed to get server status:', error);
            this.log(`❌ Status error: ${error}`);
        }
    }

    async updateQRCode() {
        try {
            console.log('📋 Updating QR code...');
            this.log('📋 Getting QR code...');
            this.showQRPlaceholder('Loading QR code...');
            
            const qrCode = await App.GetQRCode();
            console.log('📋 QR code received, length:', qrCode ? qrCode.length : 0);
            
            if (qrCode && qrCode.length > 0) {
                this.displayQRCode(qrCode);
            } else {
                console.log('🔄 No QR code available, requesting generation...');
                this.log('⚠️ No QR code available, generating...');
                const newQRCode = await App.RegenerateQR();
                if (newQRCode) {
                    this.displayQRCode(newQRCode);
                } else {
                    this.showQRPlaceholder('Failed to generate QR code');
                }
            }
        } catch (error) {
            console.error('❌ Failed to get QR code:', error);
            this.log(`❌ QR code error: ${error}`);
            this.showQRPlaceholder('Error loading QR code');
        }
    }

    displayQRCode(qrCode) {
        console.log('🖼️ Displaying QR code...');
        this.log('✅ QR code displayed');
        
        const qrPlaceholder = document.getElementById('qrPlaceholder');
        const qrImage = document.getElementById('qrImage');
        
        if (qrCode && qrCode.length > 0) {
            if (qrPlaceholder) qrPlaceholder.style.display = 'none';
            if (qrImage) {
                qrImage.src = `data:image/png;base64,${qrCode}`;
                qrImage.style.display = 'block';
                console.log('✅ QR code displayed successfully');
            }
            
            this.currentQRCode = qrCode;
            this.setElementText('pairingCode', '******');
            this.setElementText('qrExpiry', this.formatExpiryTime());
            
        } else {
            this.showQRPlaceholder('Invalid QR code data');
        }
    }

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

    async updateTransferHistory() {
        try {
            const history = await App.GetTransferHistory();
            this.displayTransferHistory(history || []);
        } catch (error) {
            console.error('❌ Failed to get transfer history:', error);
        }
    }

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

    async toggleServer() {
        try {
            console.log('🔀 Toggling server status...');
            this.log('🔀 Toggling server...');
            const newStatus = await App.ToggleServerStatus();
            
            if (newStatus) {
                this.showNotification('Server Started', 'ZypherLink is now accepting file transfers', 'success');
                this.log('Server started');
            } else {
                this.showNotification('Server Stopped', 'ZypherLink is no longer accepting files', 'info');
                this.log('Server stopped');
            }
            
            setTimeout(() => this.updateServerStatus(), 500);
            
        } catch (error) {
            console.error('❌ Failed to toggle server:', error);
            this.log(`❌ Toggle error: ${error}`);
            this.showNotification('Server Error', 'Failed to change server status', 'error');
        }
    }

    async regenerateQR() {
        try {
            console.log('🔄 Regenerating QR code...');
            this.log('🔄 Regenerating QR code...');
            this.showQRPlaceholder('Generating new QR code...');
            
            const newQRCode = await App.RegenerateQR();
            if (newQRCode) {
                this.displayQRCode(newQRCode);
                this.showNotification('QR Code Updated', 'New pairing code generated', 'success');
                this.log('✅ New QR code generated');
            } else {
                this.showQRPlaceholder('Failed to generate QR code');
                this.showNotification('QR Generation Failed', 'Could not generate new QR code', 'error');
                this.log('❌ QR generation failed');
            }
            
        } catch (error) {
            console.error('❌ Failed to regenerate QR:', error);
            this.log(`❌ QR regeneration error: ${error}`);
            this.showQRPlaceholder('Error generating QR code');
            this.showNotification('QR Generation Failed', 'Could not generate new QR code', 'error');
        }
    }

    updateTransferProgress(progress) {
        console.log('📈 Updating transfer progress:', progress);
        setTimeout(() => this.updateTransferHistory(), 1000);
    }

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
        
        setTimeout(() => {
            if (notification.parentNode) {
                notification.style.animation = 'slideOut 0.3s ease';
                setTimeout(() => notification.remove(), 300);
            }
        }, 5000);
    }

    showError(message) {
        console.error('💥 Critical error:', message);
        this.log(`💥 Critical error: ${message}`);
        
        this.setElementText('deviceName', 'Connection Error');
        this.setElementText('ipAddress', 'Check Console');
        this.setElementText('transferPort', 'See Logs');
        this.setElementText('statusText', 'Backend Error');
        
        this.showQRPlaceholder('Connection Error - Check Console');
    }

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
        const expiry = new Date(Date.now() + 10 * 60 * 1000);
        return expiry.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }

    log(message) {
        console.log(message);
        const logContent = document.getElementById('logContent');
        if (logContent) {
            const timestamp = new Date().toLocaleTimeString();
            logContent.innerHTML += `<br>${timestamp}: ${message}`;
            logContent.scrollTop = logContent.scrollHeight;
        }
    }

    destroy() {
        if (this.updateInterval) {
            clearInterval(this.updateInterval);
        }
    }
}

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

// Add slideOut animation
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