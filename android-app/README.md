# ZypherLink Android App

The Android companion app for ZypherLink - a secure cross-device file transfer system.

## Features

- 📱 **File Sending**: Send files from Android to macOS devices
- 🔗 **Device Pairing**: QR code-based secure pairing with macOS devices
- 🔍 **Network Discovery**: Automatic discovery of ZypherLink devices on local network
- 📊 **Transfer History**: Track all your file transfers
- 🎨 **Modern UI**: Material Design 3 with ZypherLink branding
- 🔒 **Secure**: Token-based authentication and encrypted transfers

## Architecture

### Core Components

- **Network Layer**: Retrofit + OkHttp for HTTP transfers, UDP for device discovery
- **UI**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM with Hilt dependency injection
- **Storage**: SharedPreferences for device/pairing data
- **Background Tasks**: Coroutines for network operations

### Key Files

- `MainActivity.kt` - Main entry point with navigation
- `MainViewModel.kt` - Central ViewModel managing app state
- `DeviceManager.kt` - Manages paired devices and local device info
- `TransferManager.kt` - Handles file transfers to paired devices
- `PairingManager.kt` - Manages QR code pairing with devices
- `DiscoveryManager.kt` - UDP-based network device discovery

## Setup

### Prerequisites

- Android Studio Hedgehog | 2023.1.1 or later
- Android SDK 24+ (Android 7.0)
- JDK 8 or later

### Building

1. Open the `android-app` directory in Android Studio
2. Sync Gradle files
3. Build and run on device or emulator

### Permissions

The app requires these permissions:
- Camera (for QR code scanning)
- Storage (for file access)
- Network (for device discovery and file transfers)

## Usage

### Pairing with macOS Device

1. Ensure both devices are on the same Wi-Fi network
2. Open ZypherLink on your macOS device
3. In the Android app, tap "Pair Device" 
4. Tap "Open Camera" and scan the QR code from the macOS app
5. Devices will be paired automatically

### Sending Files

1. Tap "Send Files" on the home screen
2. Select files using "Add Files" button
3. Choose the destination device from paired devices
4. Tap "Send Files" to start transfer

### Managing Devices

- View paired devices on the home screen
- Remove devices by tapping the delete button
- Check device online status (green = online, red = offline)
- Refresh device status in Settings

## Protocol Compatibility

This Android app implements the ZypherLink Transfer Protocol v1.0.0, ensuring compatibility with:
- macOS receiver app
- Future ZypherLink implementations

### Network Protocol

- **Discovery**: UDP broadcasts on port 8765
- **Transfer**: HTTP POST requests on ports 8766-8776
- **Authentication**: Bearer token-based auth
- **File Format**: Multipart form data with JSON metadata

## Development

### Key Dependencies

- Jetpack Compose - UI toolkit
- Hilt - Dependency injection
- Retrofit - HTTP client
- Kotlinx Serialization - JSON parsing
- ZXing - QR code scanning
- Coroutines - Async operations

### Testing

- Unit tests: `src/test/`
- UI tests: `src/androidTest/`
- Run tests: `./gradlew test`

## Security

- All transfers use HTTPS when available
- Token-based authentication prevents unauthorized access
- QR codes contain time-limited pairing tokens
- Network restricted to local subnet ranges
- No persistent storage of sensitive auth tokens

## Troubleshooting

### Common Issues

1. **Can't discover devices**: Ensure both devices on same Wi-Fi network
2. **Pairing fails**: Check QR code is clearly visible and current
3. **Transfer fails**: Verify device is online and reachable
4. **Permissions denied**: Grant camera and storage permissions in Settings

### Debug Logs

Enable debug logging by building in debug mode. Check logcat for:
- `DiscoveryManager` - Network discovery issues
- `TransferManager` - File transfer problems  
- `PairingManager` - Pairing failures

## Contributing

This is part of the ZypherLink project. See main project README for contribution guidelines.