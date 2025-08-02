package com.zyperlink.network

import com.zyperlink.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    @POST("/api/v1/pair")
    suspend fun pairDevice(@Body request: PairingRequest): Response<PairingResponse>
    
    @GET("/api/v1/status")
    suspend fun getStatus(@Header("Authorization") authToken: String): Response<Map<String, Any>>
    
    @GET("/api/v1/devices")
    suspend fun getPairedDevices(@Header("Authorization") authToken: String): Response<List<PairedDevice>>
    
    @Multipart
    @POST("/api/v1/upload")
    suspend fun uploadFiles(
        @Header("Authorization") authToken: String,
        @Part("transfer_info") transferInfo: RequestBody,
        @Part files: List<MultipartBody.Part>
    ): Response<TransferComplete>
    
    @POST("/api/v1/upload/start")
    suspend fun startTransfer(
        @Header("Authorization") authToken: String,
        @Body request: TransferRequest
    ): Response<Map<String, Any>>
    
    
    @GET("/api/v1/upload/status/{transferId}")
    suspend fun getTransferStatus(
        @Header("Authorization") authToken: String,
        @Path("transferId") transferId: String
    ): Response<TransferProgress>
}