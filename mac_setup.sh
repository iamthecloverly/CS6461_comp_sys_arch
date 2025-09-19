#!/bin/bash

# ==============================================================================
# Setup Script for the CSCI6461 Assembler Project
# ==============================================================================
# This script checks for the required development tools on macOS and provides
# instructions for installation if they are missing.
# ==============================================================================

# --- Helper function for printing colored text ---
print_info() {
    # Blue color for informational messages
    echo -e "\033[0;34m[INFO]\033[0m $1"
}

print_success() {
    # Green color for success messages
    echo -e "\033[0;32m[OK]\033[0m $1"
}

print_warning() {
    # Yellow color for warnings or user actions
    echo -e "\033[0;33m[ACTION]\033[0m $1"
}

echo ""
print_info "Starting environment check for the CSCI6461 Assembler project..."
echo ""

# --- 1. Check for Homebrew ---
print_info "Step 1: Checking for Homebrew (package manager)..."
if ! command -v brew &> /dev/null; then
    print_warning "Homebrew is not installed. It is required to easily install Java."
    print_warning "Please run the following command to install Homebrew, then run this script again:"
    echo ""
    echo '    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"'
    echo ""
    exit 1
else
    print_success "Homebrew is installed."
fi
echo ""

# --- 2. Check for Java Development Kit (JDK) ---
print_info "Step 2: Checking for Java Development Kit (JDK)..."
if command -v java &> /dev/null; then
    JAVA_VERSION_FULL=$(java -version 2>&1)
    # Extract version number (handles formats like "1.8.0_292" and "11.0.11")
    JAVA_VERSION=$(echo "$JAVA_VERSION_FULL" | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)

    if [[ "$JAVA_VERSION" -ge 8 ]]; then
        print_success "Java JDK version $JAVA_VERSION is installed and meets the requirement (version 8+)."
    else
        print_warning "An old version of Java (version $JAVA_VERSION) is installed."
        print_warning "Please upgrade your JDK. You can do so with Homebrew:"
        echo "    brew install openjdk"
    fi
else
    print_warning "Java JDK is not installed."
    print_warning "You can install the latest OpenJDK by running the following command:"
    echo "    brew install openjdk"
    print_warning "After installation, run this script again to verify."
fi
echo ""

# --- 3. Check for IntelliJ IDEA ---
print_info "Step 3: Checking for IntelliJ IDEA..."
# Check for both Community and Ultimate editions
if [ -d "/Applications/IntelliJ IDEA CE.app" ] || [ -d "/Applications/IntelliJ IDEA.app" ]; then
    print_success "IntelliJ IDEA is installed in your Applications folder."
else
    print_warning "IntelliJ IDEA was not found in your /Applications folder."
    print_warning "Please download and install it manually from the official website."
    echo "    Download URL: https://www.jetbrains.com/idea/download/"
fi
echo ""

# --- 4. Final Summary ---
print_info "Environment check complete."
print_info "Please ensure all checks above show [OK] before proceeding."
print_info "Once all tools are installed, follow the instructions in README.md to build and run the assembler."
echo ""

