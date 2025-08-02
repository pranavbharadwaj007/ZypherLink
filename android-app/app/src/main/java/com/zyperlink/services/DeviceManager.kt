package com.zyperlink.services

import android.content.Context
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.zyperlink.models.DeviceInfo
import com.zyperlink.models.PairedDevice
import com.zyperlink.models.TransferRecord
import com.zyperlink.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.NetworkInterface
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    private val TAG = "DeviceManager"
    
    private val preferences: SharedPreferences = context.getSharedPreferences(
        "zyperlink_prefs", Context.MODE_PRIVATE
    )
    
    private val _pairedDevices = MutableStateFlow<List<PairedDevice>>(emptyList())
    val pairedDevices: StateFlow<List<PairedDevice>> = _pairedDevices.asStateFlow()
    
    private val _transferHistory = MutableStateFlow<List<TransferRecord>>(emptyList())
    val transferHistory: StateFlow<List<TransferRecord>> = _transferHistory.asStateFlow()
    
    private val _localDeviceInfo = MutableStateFlow<DeviceInfo?>(null)
    val localDeviceInfo: StateFlow<DeviceInfo?> = _localDeviceInfo.asStateFlow()
    
    init {
        loadStoredData()
        initializeLocalDevice()
    }
    
    private fun loadStoredData() {
        // Load paired devices
        val pairedDevicesJson = preferences.getString(Constants.PREF_PAIRED_DEVICES, "[]")
        try {
            val devices = json.decodeFromString<List<PairedDevice>>(pairedDevicesJson ?: "[]")
            _pairedDevices.value = devices
            Log.d(TAG, "Loaded ${devices.size} paired devices")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading paired devices", e)
        }
        
        // Load transfer history (simplified for now)
        _transferHistory.value = emptyList()
    }
    
    private fun initializeLocalDevice() {
        val deviceId = getOrCreateDeviceId()
        val deviceName = getDeviceName()
        val ipAddress = getLocalIPAddress()
        
        val localDevice = DeviceInfo(
            deviceId = deviceId,
            deviceName = deviceName,
            deviceType = Constants.DEVICE_ANDROID_SENDER,
            status = Constants.STATUS_AVAILABLE,
            ipAddress = ipAddress,
            port = Constants.TRANSFER_PORT_START,
            capabilities = listOf(
                Constants.CAPABILITY_SEND,
                Constants.CAPABILITY_RECEIVE,
                Constants.CAPABILITY_MULTI_FILE
            )
        )
        
        _localDeviceInfo.value = localDevice
        Log.d(TAG, "Initialized local device: $deviceName ($deviceId)")
    }
    
    private fun getOrCreateDeviceId(): String {
        var deviceId = preferences.getString(Constants.PREF_DEVICE_ID, null)
        
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString().replace("-", "")
            preferences.edit()
                .putString(Constants.PREF_DEVICE_ID, deviceId)
                .apply()
            Log.d(TAG, "Generated new device ID: $deviceId")
        }
        
        return deviceId
    }
    
    private fun getDeviceName(): String {
        val storedName = preferences.getString(Constants.PREF_DEVICE_NAME, null)
        
        if (storedName != null) {
            return storedName
        }
        
        // Generate device name based on device info
        val deviceName = try {
            val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
            val model = Build.MODEL
            "$manufacturer $model"
        } catch (e: Exception) {
            "Android Device"
        }
        
        preferences.edit()
            .putString(Constants.PREF_DEVICE_NAME, deviceName)
            .apply()
        
        return deviceName
    }
    
    private fun getLocalIPAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                
                if (networkInterface.isLoopback || !networkInterface.isUp) {
                    continue
                }
                
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    
                    if (!address.isLoopbackAddress && 
                        !address.isLinkLocalAddress && 
                        address.hostAddress?.contains(":") == false) {
                        
                        return address.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local IP address", e)
        }
        
        return "127.0.0.1"
    }
    
    fun addPairedDevice(device: PairedDevice) {
        val currentDevices = _pairedDevices.value.toMutableList()
        val existingIndex = currentDevices.indexOfFirst { it.deviceId == device.deviceId }
        
        if (existingIndex >= 0) {
            currentDevices[existingIndex] = device
            Log.d(TAG, "Updated paired device: ${device.deviceName}")
        } else {
            currentDevices.add(device)
            Log.d(TAG, "Added new paired device: ${device.deviceName}")
        }
        
        _pairedDevices.value = currentDevices
        savePairedDevices()
    }
    
    fun removePairedDevice(deviceId: String) {
        val currentDevices = _pairedDevices.value.toMutableList()
        val removed = currentDevices.removeAll { it.deviceId == deviceId }
        
        if (removed) {
            _pairedDevices.value = currentDevices
            savePairedDevices()
            Log.d(TAG, "Removed paired device with ID: $deviceId")
        }
    }
    
    fun updateDeviceOnlineStatus(deviceId: String, isOnline: Boolean) {
        val currentDevices = _pairedDevices.value.toMutableList()
        val deviceIndex = currentDevices.indexOfFirst { it.deviceId == deviceId }
        
        if (deviceIndex >= 0) {
            val updatedDevice = currentDevices[deviceIndex].copy(
                isOnline = isOnline,
                lastSeen = System.currentTimeMillis()
            )
            currentDevices[deviceIndex] = updatedDevice
            _pairedDevices.value = currentDevices
            Log.d(TAG, "Updated device ${updatedDevice.deviceName} online status: $isOnline")
        }
    }
    
    private fun savePairedDevices() {
        try {
            val json = json.encodeToString(_pairedDevices.value)
            preferences.edit()
                .putString(Constants.PREF_PAIRED_DEVICES, json)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving paired devices", e)
        }
    }
    
    fun addTransferRecord(record: TransferRecord) {
        val currentHistory = _transferHistory.value.toMutableList()
        currentHistory.add(0, record) // Add to beginning
        
        // Keep only recent transfers (limit to 50)
        if (currentHistory.size > 50) {
            currentHistory.removeAt(currentHistory.size - 1)
        }
        
        _transferHistory.value = currentHistory
        Log.d(TAG, "Added transfer record: ${record.deviceName}")
    }
    
    fun updateDeviceName(newName: String) {
        preferences.edit()
            .putString(Constants.PREF_DEVICE_NAME, newName)
            .apply()
        
        _localDeviceInfo.value?.let { currentDevice ->
            _localDeviceInfo.value = currentDevice.copy(deviceName = newName)
        }
        
        Log.d(TAG, "Updated device name to: $newName")
    }
    
    fun getPairedDevice(deviceId: String): PairedDevice? {
        return _pairedDevices.value.find { it.deviceId == deviceId }
    }
    
    fun clearAllData() {
        preferences.edit().clear().apply()
        _pairedDevices.value = emptyList()
        _transferHistory.value = emptyList()
        initializeLocalDevice()
        Log.d(TAG, "Cleared all device data")
    }
}