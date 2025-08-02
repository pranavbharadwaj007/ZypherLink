package com.zyperlink.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zyperlink.ui.screens.HomeScreen
import com.zyperlink.ui.screens.PairingScreen
import com.zyperlink.ui.screens.TransferScreen
import com.zyperlink.ui.screens.SettingsScreen
import com.zyperlink.ui.theme.ZypherLinkTheme
import com.zyperlink.ui.viewmodels.MainViewModel
import com.zyperlink.utils.Constants
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            // Permissions granted, can proceed
        } else {
            // Some permissions denied
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestNecessaryPermissions()
        
        setContent {
            ZypherLinkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp(
                        sharedFileUris = handleSharedFiles(intent)
                    )
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        
        // Handle new shared files
        val sharedUris = handleSharedFiles(intent)
        if (sharedUris.isNotEmpty()) {
            // TODO: Handle new shared files in existing activity
        }
    }
    
    private fun requestNecessaryPermissions() {
        val permissions = mutableListOf<String>()
        
        // Storage permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.addAll(listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            ))
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        // Camera permission for QR scanning
        permissions.add(Manifest.permission.CAMERA)
        
        // Network permissions (usually granted by default)
        permissions.addAll(listOf(
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE
        ))
        
        // Check which permissions are not granted
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    
    private fun handleSharedFiles(intent: Intent?): List<Uri> {
        val sharedUris = mutableListOf<Uri>()
        
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uri ->
                    sharedUris.add(uri)
                }
            }
            
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris ->
                    sharedUris.addAll(uris)
                }
            }
        }
        
        return sharedUris
    }
}

@Composable
fun MainApp(
    sharedFileUris: List<Uri> = emptyList(),
    viewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    
    // Handle shared files
    LaunchedEffect(sharedFileUris) {
        if (sharedFileUris.isNotEmpty()) {
            viewModel.setSharedFiles(sharedFileUris)
            navController.navigate("transfer")
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToTransfer = { navController.navigate("transfer") },
                onNavigateToPairing = { navController.navigate("pairing") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        
        composable("transfer") {
            TransferScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPairing = { navController.navigate("pairing") }
            )
        }
        
        composable("pairing") {
            PairingScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}