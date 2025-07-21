#!/bin/bash

echo "🔧 ZypherLink Final Setup & Run"
echo "==============================="

# Stop any running instance
echo "🛑 Stopping any running instances..."
pkill -f "wails dev" 2>/dev/null || echo "No running instance found"

# Navigate to the project directory
cd mac-receiver || {
    echo "❌ Error: Please run this from the zyperlink-project directory"
    exit 1
}

echo "📁 Working in: $(pwd)"

# Step 1: Clean up ports
echo "🔌 Freeing up ports..."
lsof -ti:8765 | xargs kill -9 2>/dev/null || echo "Port 8765 free"
lsof -ti:8766 | xargs kill -9 2>/dev/null || echo "Port 8766 free"

# Step 2: Setup frontend directory structure
echo "📂 Setting up frontend structure..."
mkdir -p frontend/dist
mkdir -p frontend/src

# Step 3: Copy frontend files to dist (where Wails looks)
echo "📋 Copying frontend files..."
if [ -f "frontend/index.html" ]; then
    cp frontend/index.html frontend/dist/
    echo "✅ Copied index.html to dist/"
else
    echo "❌ Error: frontend/index.html not found!"
    echo "Please make sure you have the new minimal index.html file"
    exit 1
fi

# Copy any CSS/JS files if they exist
cp frontend/src/* frontend/dist/ 2>/dev/null || echo "No additional src files to copy"

# Step 4: Test Go compilation
echo "🧪 Testing Go compilation..."
go mod tidy
go build -o test-build .

if [ $? -eq 0 ]; then
    echo "✅ Go compilation successful"
    rm -f test-build
else
    echo "❌ Go compilation failed - check errors above"
    exit 1
fi

# Step 5: Check Wails doctor
echo "🩺 Checking Wails installation..."
wails doctor

# Step 6: Generate Wails bindings
echo "🔗 Generating Wails bindings..."
wails generate

# Step 7: Start the application
echo ""
echo "🚀 Starting ZypherLink..."
echo ""
echo "🎯 Look for these success indicators:"
echo "✅ 'Wails ready! Initializing app...'"
echo "✅ Device info loads with real values"
echo "✅ QR code appears"
echo "✅ Buttons become clickable"
echo "✅ No 'Backend Error' status"
echo ""
echo "🐛 If still broken, check the browser console (Cmd+Option+I)"
echo ""

# Start development mode
exec wails dev