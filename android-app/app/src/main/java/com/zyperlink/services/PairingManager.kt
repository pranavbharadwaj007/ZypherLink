package com.zyperlink.services

import android.content.Context
import android.util.Log
import com.zyperlink.models.*
import com.zyperlink.network.ApiService
import com.zyperlink.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PairingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val retrofit: Retrofit,
    private val json: Json,
    private val deviceManager: DeviceManager
) {
    private val TAG = "PairingManager"
    
    suspend fun pairWithDevice(
        qrData: String,
        localDeviceInfo: DeviceInfo
    ): Result<PairedDevice> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting pairing process")
            
            // Parse QR code data
            val pairingData = parseQRData(qrData)
                ?: return@withContext Result.failure(Exception("Invalid QR code data"))
            
            // Create API service for target device
            val apiService = createApiServiceForDevice(pairingData.ipAddress, pairingData.port)
            
            // Create pairing request
            val pairingRequest = PairingRequest(
                messageType = Constants.MSG_PAIR_REQ,
                requesterDeviceId = localDeviceInfo.deviceId,
                requesterName = localDeviceInfo.deviceName,
                pairingCode = pairingData.pairingCode
            )
            
            // Send pairing request
            val response = apiService.pairDevice(pairingRequest)
            
            if (response.isSuccessful) {
                val pairingResponse = response.body()
                if (pairingResponse?.success == true && pairingResponse.authToken != null) {
                    
                    val pairedDevice = PairedDevice(
                        deviceId = pairingResponse.deviceId,
                        deviceName = pairingData.deviceName,
                        deviceType = Constants.DEVICE_MAC_RECEIVER,
                        ipAddress = pairingData.ipAddress,
                        port = pairingData.port,
                        authToken = pairingResponse.authToken,
                        tokenExpiresAt = pairingResponse.tokenExpiresAt ?: (System.currentTimeMillis() + Constants.TOKEN_EXPIRY_MINUTES * 60 * 1000),
                        isOnline = true
                    )
                    
                    // Save paired device
                    deviceManager.addPairedDevice(pairedDevice)
                    
                    Log.d(TAG, "Successfully paired with ${pairingData.deviceName}")
                    Result.success(pairedDevice)
                } else {
                    val errorMsg = pairingResponse?.errorMessage ?: "Pairing failed"
                    Log.e(TAG, "Pairing failed: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                Log.e(TAG, "Pairing request failed: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during pairing", e)
            Result.failure(e)
        }
    }
    
    private fun parseQRData(qrData: String): QRPairingData? {
        return try {
            // QR data format: JSON containing device info and pairing code
            json.decodeFromString<QRPairingData>(qrData)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing QR data", e)
            
            // Try alternative format (base64 encoded)
            try {
                val decodedData = String(Base64.getDecoder().decode(qrData))
                json.decodeFromString<QRPairingData>(decodedData)
            } catch (e2: Exception) {
                Log.e(TAG, "Error parsing base64 QR data", e2)
                null
            }
        }
    }
    
    private fun createApiServiceForDevice(ipAddress: String, port: Int): ApiService {
        val baseUrl = "http://$ipAddress:$port/"
        val deviceRetrofit = retrofit.newBuilder()
            .baseUrl(baseUrl)
            .build()
        
        return deviceRetrofit.create(ApiService::class.java)
    }
    
    suspend fun verifyPairedDevice(device: PairedDevice): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val apiService = createApiServiceForDevice(device.ipAddress, device.port)
            val response = apiService.getStatus("Bearer ${device.authToken}")
            
            if (response.isSuccessful) {
                Log.d(TAG, "Device ${device.deviceName} is online")
                Result.success(true)
            } else {
                Log.w(TAG, "Device ${device.deviceName} is offline or token expired")
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying device ${device.deviceName}", e)
            Result.success(false)
        }
    }
    
    suspend fun refreshDeviceToken(device: PairedDevice): Result<PairedDevice> = withContext(Dispatchers.IO) {
        try {
            // For now, we'll implement token refresh by re-pairing
            // In a production app, you'd have a dedicated refresh endpoint
            Log.d(TAG, "Token refresh not implemented - would need re-pairing")
            Result.failure(Exception("Token refresh requires re-pairing"))
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing token for ${device.deviceName}", e)
            Result.failure(e)
        }
    }
}