package com.zyperlink.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.zyperlink.models.*
import com.zyperlink.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscoveryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    private val TAG = "DiscoveryManager"
    
    private var discoveryJob: Job? = null
    private var listenerJob: Job? = null
    private var socket: DatagramSocket? = null
    
    private val _discoveredDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<DeviceInfo>> = _discoveredDevices.asStateFlow()
    
    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()
    
    fun startDiscovery(deviceInfo: DeviceInfo) {
        if (_isDiscovering.value) {
            Log.d(TAG, "Discovery already running")
            return
        }
        
        Log.d(TAG, "Starting device discovery")
        _isDiscovering.value = true
        
        discoveryJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                socket = DatagramSocket()
                socket?.broadcast = true
                
                // Start listening for responses
                startListening()
                
                // Send discovery broadcasts
                while (isActive) {
                    broadcastDiscoveryRequest(deviceInfo)
                    delay(Constants.BROADCAST_INTERVAL.toMillis())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Discovery error", e)
            }
        }
    }
    
    fun stopDiscovery() {
        Log.d(TAG, "Stopping device discovery")
        _isDiscovering.value = false
        
        discoveryJob?.cancel()
        listenerJob?.cancel()
        
        socket?.close()
        socket = null
        
        // Clear discovered devices after stopping
        _discoveredDevices.value = emptyList()
    }
    
    private fun startListening() {
        listenerJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(1024)
            
            while (isActive && socket?.isClosed == false) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket?.receive(packet)
                    
                    val response = String(packet.data, 0, packet.length)
                    Log.d(TAG, "Received discovery response: $response")
                    
                    parseDiscoveryResponse(response, packet.address.hostAddress ?: "")
                } catch (e: Exception) {
                    if (isActive) {
                        Log.e(TAG, "Error receiving discovery response", e)
                    }
                }
            }
        }
    }
    
    private suspend fun broadcastDiscoveryRequest(deviceInfo: DeviceInfo) {
        try {
            val request = DiscoveryRequest(
                messageType = Constants.MSG_DISCOVERY_REQ,
                deviceId = deviceInfo.deviceId,
                deviceName = deviceInfo.deviceName,
                deviceType = deviceInfo.deviceType,
                capabilities = deviceInfo.capabilities,
                networkInfo = NetworkInfo(
                    ipAddress = deviceInfo.ipAddress,
                    port = deviceInfo.port
                )
            )
            
            val message = json.encodeToString(request)
            val data = message.toByteArray()
            
            // Get broadcast address
            val broadcastAddress = getBroadcastAddress()
            val packet = DatagramPacket(
                data, 
                data.size, 
                broadcastAddress, 
                Constants.DISCOVERY_PORT
            )
            
            socket?.send(packet)
            Log.d(TAG, "Sent discovery broadcast to $broadcastAddress")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending discovery broadcast", e)
        }
    }
    
    private fun parseDiscoveryResponse(response: String, senderIp: String) {
        try {
            val discoveryResponse = json.decodeFromString<DiscoveryResponse>(response)
            
            if (discoveryResponse.messageType == Constants.MSG_DISCOVERY_RESP) {
                val deviceInfo = DeviceInfo(
                    deviceId = discoveryResponse.deviceId,
                    deviceName = discoveryResponse.deviceName,
                    deviceType = discoveryResponse.deviceType,
                    status = discoveryResponse.status,
                    ipAddress = senderIp,
                    port = extractPortFromEndpoint(discoveryResponse.transferEndpoint) ?: Constants.TRANSFER_PORT_START,
                    lastSeen = System.currentTimeMillis()
                )
                
                // Update discovered devices list
                val currentDevices = _discoveredDevices.value.toMutableList()
                val existingIndex = currentDevices.indexOfFirst { it.deviceId == deviceInfo.deviceId }
                
                if (existingIndex >= 0) {
                    currentDevices[existingIndex] = deviceInfo
                } else {
                    currentDevices.add(deviceInfo)
                }
                
                _discoveredDevices.value = currentDevices
                Log.d(TAG, "Added/updated device: ${deviceInfo.deviceName}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing discovery response", e)
        }
    }
    
    private fun getBroadcastAddress(): InetAddress {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcp = wifiManager.dhcpInfo
        
        val broadcast = (dhcp.ipAddress and dhcp.netmask) or dhcp.netmask.inv()
        val bytes = byteArrayOf(
            (broadcast and 0xFF).toByte(),
            (broadcast shr 8 and 0xFF).toByte(),
            (broadcast shr 16 and 0xFF).toByte(),
            (broadcast shr 24 and 0xFF).toByte()
        )
        
        return InetAddress.getByAddress(bytes)
    }
    
    private fun extractPortFromEndpoint(endpoint: String?): Int? {
        return endpoint?.let { ep ->
            try {
                val uri = URI(ep)
                uri.port.takeIf { it != -1 }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    fun clearDiscoveredDevices() {
        _discoveredDevices.value = emptyList()
    }
}