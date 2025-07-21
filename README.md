# 📦 ZypherLink: Cross-Device Transfer Tool

A secure and fast file transfer system between macOS and Android devices.

## Project Structure

- `android-app/` - Kotlin/Jetpack Compose Android application
- `mac-receiver/` - Go + Wails macOS desktop application  
- `protocol-specs/` - Shared protocol definitions and schemas
- `shared/` - Common constants and utilities
- `assets/` - Icons, themes, and visual resources

## Development

See individual README files in each directory for setup instructions.

## Architecture

- **Network Protocol**: HTTP-based file transfer with UDP discovery
- **Security**: Token-based authentication with QR code pairing
- **UI Theme**: Orange and dark purple across all platforms
