package com.zyperlink.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zyperlink.models.DeviceInfo
import com.zyperlink.models.PairedDevice
import com.zyperlink.models.TransferRecord
import com.zyperlink.network.DiscoveryManager
import com.zyperlink.services.DeviceManager
import com.zyperlink.services.PairingManager
import com.zyperlink.services.TransferManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val deviceManager: DeviceManager,
    private val discoveryManager: DiscoveryManager,
    private val pairingManager: PairingManager,
    private val transferManager: TransferManager
) : ViewModel() {
    
    // Local device info
    val localDeviceInfo: StateFlow<DeviceInfo?> = deviceManager.localDeviceInfo
    
    // Discovered devices (for pairing)
    val discoveredDevices: StateFlow<List<DeviceInfo>> = discoveryManager.discoveredDevices
    val isDiscovering: StateFlow<Boolean> = discoveryManager.isDiscovering
    
    // Paired devices
    val pairedDevices: StateFlow<List<PairedDevice>> = deviceManager.pairedDevices
    
    // Transfer history
    val transferHistory: StateFlow<List<TransferRecord>> = deviceManager.transferHistory
    
    // Transfer progress
    val transferProgress = transferManager.transferProgress
    val isTransferring: StateFlow<Boolean> = transferManager.isTransferring
    
    // Shared files from external apps
    private val _sharedFiles = MutableStateFlow<List<Uri>>(emptyList())
    val sharedFiles: StateFlow<List<Uri>> = _sharedFiles.asStateFlow()
    
    // UI state
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    init {
        // Monitor paired devices and update their online status
        viewModelScope.launch {
            pairedDevices.collect { devices ->
                devices.forEach { device ->
                    checkDeviceOnlineStatus(device)
                }
            }
        }
    }
    
    fun startDiscovery() {
        localDeviceInfo.value?.let { deviceInfo ->
            discoveryManager.startDiscovery(deviceInfo)
        }
    }
    
    fun stopDiscovery() {
        discoveryManager.stopDiscovery()
    }
    
    fun pairWithDevice(qrData: String) {
        viewModelScope.launch {
            localDeviceInfo.value?.let { localDevice ->
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val result = pairingManager.pairWithDevice(qrData, localDevice)
                
                result.fold(
                    onSuccess = { pairedDevice ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            message = "Successfully paired with ${pairedDevice.deviceName}"
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Pairing failed"
                        )
                    }
                )
            }
        }
    }
    
    fun sendFiles(fileUris: List<Uri>, targetDevice: PairedDevice) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = transferManager.sendFiles(fileUris, targetDevice)
            
            result.fold(
                onSuccess = { transferComplete ->
                    // Add to transfer history
                    val transferRecord = TransferRecord(
                        id = transferComplete.transferId,
                        deviceId = targetDevice.deviceId,
                        deviceName = targetDevice.deviceName,
                        fileCount = transferComplete.filesTransferred,
                        totalSize = transferComplete.totalBytes,
                        timestamp = System.currentTimeMillis(),
                        success = transferComplete.success,
                        direction = com.zyperlink.models.TransferDirection.SEND
                    )
                    
                    deviceManager.addTransferRecord(transferRecord)
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Files sent successfully to ${targetDevice.deviceName}"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Transfer failed"
                    )
                }
            )
        }
    }
    
    fun removePairedDevice(deviceId: String) {
        deviceManager.removePairedDevice(deviceId)
        _uiState.value = _uiState.value.copy(message = "Device removed")
    }
    
    fun setSharedFiles(uris: List<Uri>) {
        _sharedFiles.value = uris
    }
    
    fun clearSharedFiles() {
        _sharedFiles.value = emptyList()
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    private fun checkDeviceOnlineStatus(device: PairedDevice) {
        viewModelScope.launch {
            val result = pairingManager.verifyPairedDevice(device)
            result.fold(
                onSuccess = { isOnline ->
                    deviceManager.updateDeviceOnlineStatus(device.deviceId, isOnline)
                },
                onFailure = {
                    deviceManager.updateDeviceOnlineStatus(device.deviceId, false)
                }
            )
        }
    }
    
    fun refreshDevices() {
        viewModelScope.launch {
            pairedDevices.value.forEach { device ->
                checkDeviceOnlineStatus(device)
            }
        }
    }
    
    fun updateDeviceName(newName: String) {
        deviceManager.updateDeviceName(newName)
        _uiState.value = _uiState.value.copy(message = "Device name updated")
    }
    
    override fun onCleared() {
        super.onCleared()
        stopDiscovery()
    }
    
    data class UiState(
        val isLoading: Boolean = false,
        val message: String? = null,
        val error: String? = null
    )
}