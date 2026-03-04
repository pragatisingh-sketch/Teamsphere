#!/bin/bash

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Install prerequisite packages
install_prerequisites() {
    echo "Installing prerequisites..."
    if ! command_exists unzip; then
        echo "Installing unzip..."
        sudo apt-get update && sudo apt-get install -y unzip
    fi

    if ! command_exists curl; then
        echo "Installing curl..."
        sudo apt-get install -y curl
    fi
}

# Check and install Java 21.0.5-amzn using SDKMAN
check_and_install_java() {
    JAVA_VERSION="21.0.5-amzn"

    if command_exists java; then
        INSTALLED_JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
        if [[ "$INSTALLED_JAVA_VERSION" == "21" ]]; then
            echo "Java 21 is already installed."
            java -version
            return
        fi
    fi

    echo "Installing Java $JAVA_VERSION..."
    if ! command_exists sdk; then
        echo "SDKMAN is not installed. Installing SDKMAN..."
        curl -s "https://get.sdkman.io" | bash
        source "$HOME/.sdkman/bin/sdkman-init.sh"
    fi

    # Source SDKMAN
    if [ -f "$HOME/.sdkman/bin/sdkman-init.sh" ]; then
        source "$HOME/.sdkman/bin/sdkman-init.sh"
    else
        echo "Error: SDKMAN installation failed or not found."
        exit 1
    fi

    # Install and set Java version
    sdk install java $JAVA_VERSION
    sdk use java $JAVA_VERSION
    sdk default java $JAVA_VERSION

    echo "Java installation completed. Current version:"
    java -version
}

# Check and install Node.js v18.19.0 using nvm
check_and_install_node() {
    NODE_VERSION="v18.19.0"

    # Check if nvm is available and source it
    if [ -f "$HOME/.config/nvm/nvm.sh" ]; then
        source "$HOME/.config/nvm/nvm.sh"
    elif [ -f "$HOME/.nvm/nvm.sh" ]; then
        source "$HOME/.nvm/nvm.sh"
    fi

    # Check if Node.js is already installed with correct version
    if command_exists node; then
        INSTALLED_NODE_VERSION=$(node -v)
        if [[ "$INSTALLED_NODE_VERSION" == "$NODE_VERSION" ]]; then
            echo "Node.js $NODE_VERSION is already installed."
            echo "Current versions:"
            echo "Node.js: $(node -v)"
            echo "npm: $(npm -v)"
            return
        fi
    fi

    echo "Installing Node.js $NODE_VERSION..."

    # Install nvm if not present
    if ! command_exists nvm && [ ! -f "$HOME/.config/nvm/nvm.sh" ] && [ ! -f "$HOME/.nvm/nvm.sh" ]; then
        echo "nvm is not installed. Installing nvm..."
        curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.5/install.sh | bash

        # Try to source nvm from common locations
        if [ -f "$HOME/.config/nvm/nvm.sh" ]; then
            source "$HOME/.config/nvm/nvm.sh"
        elif [ -f "$HOME/.nvm/nvm.sh" ]; then
            source "$HOME/.nvm/nvm.sh"
        else
            echo "Error: nvm installation failed or not found."
            exit 1
        fi
    fi

    # Install and use Node.js version
    nvm install $NODE_VERSION
    nvm use $NODE_VERSION
    nvm alias default $NODE_VERSION

    echo "Node.js installation completed. Current versions:"
    echo "Node.js: $(node -v)"
    echo "npm: $(npm -v)"
    echo "nvm: $(nvm --version)"
}

# Check and install Angular 16 using npm
check_and_install_angular() {
    ANGULAR_VERSION="16"

    if command_exists ng; then
        INSTALLED_ANGULAR_VERSION=$(ng version 2>/dev/null | grep "Angular CLI:" | awk '{print $3}' | cut -d'.' -f1)
        if [[ "$INSTALLED_ANGULAR_VERSION" == "$ANGULAR_VERSION" ]]; then
            echo "Angular CLI $ANGULAR_VERSION is already installed."
            return
        fi
    fi

    echo "Installing Angular CLI $ANGULAR_VERSION..."
    npm install -g @angular/cli@$ANGULAR_VERSION

    echo "Angular CLI installation completed. Current version:"
    ng version --version 2>/dev/null || echo "Angular CLI installed successfully"
}

# Main script execution
echo "Starting full stack development environment setup..."
install_prerequisites
check_and_install_java
check_and_install_node
check_and_install_angular

echo "Setup complete. All dependencies (Java, Node.js, Angular CLI) are now installed."

