#!/bin/bash

# TeamSphere Installer Package Creator
# Version: 1.0
# Description: Creates a distributable installer package for TeamSphere

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PACKAGE_NAME="teamsphere-installer"
PACKAGE_VERSION="1.0.0"
BUILD_DIR="build"
PACKAGE_DIR="$BUILD_DIR/$PACKAGE_NAME-$PACKAGE_VERSION"

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if required source directories exist
check_sources() {
    log_info "Checking source directories..."

    if [ ! -d "org-chart" ]; then
        log_error "Frontend source directory 'org-chart' not found"
        exit 1
    fi

    if [ ! -d "backend/capsAllocation" ]; then
        log_error "Backend source directory 'backend/capsAllocation' not found"
        exit 1
    fi

    log_success "Source directories found"
}

# Create build directory structure
create_build_structure() {
    log_info "Creating build directory structure..."

    # Clean previous build
    rm -rf $BUILD_DIR

    # Create package directory
    mkdir -p $PACKAGE_DIR

    log_success "Build directory created"
}

# Build frontend
build_frontend() {
    log_info "Building frontend..."

    if [ ! -d "org-chart" ]; then
        log_error "Frontend source directory 'org-chart' not found"
        exit 1
    fi

    cd org-chart

    # Install dependencies if node_modules doesn't exist
    if [ ! -d "node_modules" ]; then
        log_info "Installing frontend dependencies..."
        npm install
    fi

    # Build for production
    log_info "Building frontend for production..."
    npm run build

    cd ..
    log_success "Frontend built successfully"
}

# Build backend
build_backend() {
    log_info "Building backend..."

    if [ ! -d "backend/capsAllocation" ]; then
        log_error "Backend source directory 'backend/capsAllocation' not found"
        exit 1
    fi

    cd backend/capsAllocation

    # Build with Maven
    log_info "Building backend with Maven..."
    if [ -f "./mvnw" ]; then
        ./mvnw clean package -DskipTests
    else
        mvn clean package -DskipTests
    fi

    cd ../..
    log_success "Backend built successfully"
}

# Copy built artifacts (excluding source files)
copy_built_artifacts() {
    log_info "Copying built artifacts..."

    # Copy frontend built files
    log_info "Copying frontend build artifacts..."
    if [ ! -d "org-chart/dist/org-chart" ]; then
        log_error "Frontend build artifacts not found. Please run build first."
        exit 1
    fi
    mkdir -p $PACKAGE_DIR/frontend
    cp -r org-chart/dist/org-chart/* $PACKAGE_DIR/frontend/

    # Copy backend JAR file and essential configuration
    log_info "Copying backend build artifacts..."
    if [ ! -f "backend/capsAllocation/target/capsAllocation-0.0.1-SNAPSHOT.jar" ]; then
        log_error "Backend JAR file not found. Please run build first."
        exit 1
    fi
    mkdir -p $PACKAGE_DIR/backend
    cp backend/capsAllocation/target/capsAllocation-0.0.1-SNAPSHOT.jar $PACKAGE_DIR/backend/teamsphere.jar

    # Copy essential backend configuration files
    mkdir -p $PACKAGE_DIR/backend/config
    if [ -f "backend/capsAllocation/src/main/resources/application.properties" ]; then
        cp backend/capsAllocation/src/main/resources/application.properties $PACKAGE_DIR/backend/config/
    fi

    # Copy any credential files if they exist
    if [ -d "backend/capsAllocation/src/main/resources" ]; then
        find backend/capsAllocation/src/main/resources -name "*.json" -exec cp {} $PACKAGE_DIR/backend/config/ \; 2>/dev/null || true
    fi

    # Copy database dump file
    log_info "Copying database dump file..."
    if [ -f "db.dump" ]; then
        mkdir -p $PACKAGE_DIR/database
        cp db.dump $PACKAGE_DIR/database/
        log_success "Database dump file copied"
    else
        log_warning "Database dump file (db.dump) not found. Database will be initialized empty."
    fi

    log_success "Built artifacts copied"
}

# Copy installer scripts
copy_installer_scripts() {
    log_info "Copying installer scripts..."

    # Check if scripts exist in root, if not copy from build directory
    if [ -f "teamsphere-installer.sh" ]; then
        # Copy main installer scripts from root
        cp teamsphere-installer.sh $PACKAGE_DIR/
        cp teamsphere-uninstaller.sh $PACKAGE_DIR/
        cp teamsphere-manager.sh $PACKAGE_DIR/
        cp INSTALLER_README.md $PACKAGE_DIR/README.md
    elif [ -f "build/teamsphere-installer-1.0.0/teamsphere-installer.sh" ]; then
        # Copy from existing build directory
        log_info "Using existing installer scripts from build directory..."
        cp build/teamsphere-installer-1.0.0/teamsphere-installer.sh $PACKAGE_DIR/
        cp build/teamsphere-installer-1.0.0/teamsphere-uninstaller.sh $PACKAGE_DIR/
        cp build/teamsphere-installer-1.0.0/teamsphere-manager.sh $PACKAGE_DIR/
        cp build/teamsphere-installer-1.0.0/README.md $PACKAGE_DIR/README.md
    else
        # Create installer scripts inline
        log_info "Creating installer scripts..."
        create_installer_scripts
    fi

    # Make scripts executable
    chmod +x $PACKAGE_DIR/teamsphere-installer.sh
    chmod +x $PACKAGE_DIR/teamsphere-uninstaller.sh
    chmod +x $PACKAGE_DIR/teamsphere-manager.sh

    log_success "Installer scripts copied"
}

# Create installer scripts inline
create_installer_scripts() {
    log_info "Creating installer scripts inline..."

    # Create main installer script
    cat > $PACKAGE_DIR/teamsphere-installer.sh << 'EOF'
#!/bin/bash

# TeamSphere Linux Installer
# Version: 1.0
# Description: Installs TeamSphere application on Linux systems

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
APP_NAME="teamsphere"
APP_USER="teamsphere"
APP_HOME="/opt/teamsphere"
SERVICE_NAME="teamsphere"
DB_NAME="vbs_allocation_caps"
DB_USER="teamsphere"
DB_PASSWORD=""
FRONTEND_PORT="80"
BACKEND_PORT="8080"
SERVER_IP=""
SYSTEM_TYPE=""

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if running as root
check_root() {
    if [[ $EUID -eq 0 ]]; then
        log_error "This script should not be run as root. Please run as a regular user with sudo privileges."
        exit 1
    fi
}

# Check if user has sudo privileges
check_sudo() {
    if ! sudo -n true 2>/dev/null; then
        log_error "This script requires sudo privileges. Please ensure your user has sudo access."
        exit 1
    fi
}

# Detect system type and configure IP
detect_system_type() {
    log_info "Detecting system type for CORS configuration..."

    echo ""
    echo "Please select your system type:"
    echo "1) Regular Linux system (localhost)"
    echo "2) Chromebook with Linux (Penguin/WSL-like environment)"
    echo "3) WSL (Windows Subsystem for Linux)"
    echo "4) Custom IP address"
    echo ""

    while true; do
        read -p "Enter your choice (1-4): " choice
        case $choice in
            1)
                SYSTEM_TYPE="regular"
                SERVER_IP="localhost"
                log_info "Selected: Regular Linux system"
                break
                ;;
            2)
                SYSTEM_TYPE="chromebook"
                # Get the IP address using hostname -I for Chromebook/Penguin
                SERVER_IP=$(hostname -I | awk '{print $1}')
                if [ -z "$SERVER_IP" ]; then
                    log_warning "Could not detect IP automatically. Please enter manually."
                    read -p "Enter your IP address: " SERVER_IP
                fi
                log_info "Selected: Chromebook/Penguin environment with IP: $SERVER_IP"
                break
                ;;
            3)
                SYSTEM_TYPE="wsl"
                # Get the IP address using hostname -I for WSL
                SERVER_IP=$(hostname -I | awk '{print $1}')
                if [ -z "$SERVER_IP" ]; then
                    log_warning "Could not detect IP automatically. Please enter manually."
                    read -p "Enter your IP address: " SERVER_IP
                fi
                log_info "Selected: WSL environment with IP: $SERVER_IP"
                break
                ;;
            4)
                SYSTEM_TYPE="custom"
                read -p "Enter your custom IP address: " SERVER_IP
                log_info "Selected: Custom IP address: $SERVER_IP"
                break
                ;;
            *)
                log_error "Invalid choice. Please enter 1, 2, 3, or 4."
                ;;
        esac
    done

    log_success "System type configured: $SYSTEM_TYPE with IP: $SERVER_IP"
}

# Detect Linux distribution
detect_distro() {
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        DISTRO=$ID
        VERSION=$VERSION_ID
    else
        log_error "Cannot detect Linux distribution"
        exit 1
    fi

    log_info "Detected distribution: $DISTRO $VERSION"
}

# Check system requirements
check_requirements() {
    log_info "Checking system requirements..."

    # Check available memory (minimum 2GB)
    MEMORY_KB=$(grep MemTotal /proc/meminfo | awk '{print $2}')
    MEMORY_GB=$((MEMORY_KB / 1024 / 1024))

    if [ $MEMORY_GB -lt 2 ]; then
        log_warning "System has less than 2GB RAM. Application may run slowly."
    fi

    # Check available disk space (minimum 5GB)
    AVAILABLE_SPACE=$(df / | awk 'NR==2 {print $4}')
    AVAILABLE_GB=$((AVAILABLE_SPACE / 1024 / 1024))

    if [ $AVAILABLE_GB -lt 5 ]; then
        log_error "Insufficient disk space. At least 5GB required."
        exit 1
    fi

    log_success "System requirements check passed"
}

# Install system dependencies
install_dependencies() {
    log_info "Installing system dependencies..."

    case $DISTRO in
        ubuntu|debian)
            sudo apt-get update
            sudo apt-get install -y curl wget gnupg2 software-properties-common apt-transport-https ca-certificates
            sudo apt-get install -y postgresql postgresql-contrib nginx
            ;;
        centos|rhel|fedora)
            if command -v dnf &> /dev/null; then
                sudo dnf update -y
                sudo dnf install -y curl wget gnupg2 ca-certificates
                sudo dnf install -y postgresql postgresql-server postgresql-contrib nginx
            else
                sudo yum update -y
                sudo yum install -y curl wget gnupg2 ca-certificates
                sudo yum install -y postgresql postgresql-server postgresql-contrib nginx
            fi
            ;;
        *)
            log_error "Unsupported distribution: $DISTRO"
            exit 1
            ;;
    esac

    log_success "System dependencies installed"
}

# Install Java 17
install_java() {
    log_info "Installing Java 17..."

    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -n1 | awk -F '"' '{print $2}' | awk -F '.' '{print $1}')
        if [ "$JAVA_VERSION" -ge 17 ]; then
            log_success "Java $JAVA_VERSION is already installed"
            return
        fi
    fi

    case $DISTRO in
        ubuntu|debian)
            sudo apt-get install -y openjdk-17-jdk
            ;;
        centos|rhel|fedora)
            if command -v dnf &> /dev/null; then
                sudo dnf install -y java-17-openjdk java-17-openjdk-devel
            else
                sudo yum install -y java-17-openjdk java-17-openjdk-devel
            fi
            ;;
    esac

    # Set JAVA_HOME
    JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:bin/java::")
    echo "export JAVA_HOME=$JAVA_HOME" | sudo tee -a /etc/environment
    export JAVA_HOME=$JAVA_HOME

    log_success "Java 17 installed successfully"
}

# Note: Node.js installation removed since we're using pre-built frontend artifacts

# Create application user
create_app_user() {
    log_info "Creating application user..."

    if id "$APP_USER" &>/dev/null; then
        log_success "User $APP_USER already exists"
    else
        sudo useradd -r -s /bin/false -d $APP_HOME $APP_USER
        log_success "User $APP_USER created"
    fi
}

# Create application directories
create_directories() {
    log_info "Creating application directories..."

    sudo mkdir -p $APP_HOME/{bin,config,logs,data,frontend}
    sudo mkdir -p /var/log/$APP_NAME
    sudo mkdir -p /etc/$APP_NAME

    sudo chown -R $APP_USER:$APP_USER $APP_HOME
    sudo chown -R $APP_USER:$APP_USER /var/log/$APP_NAME

    log_success "Application directories created"
}

# Create additional log directories for the application
create_log_directories() {
    log_info "Creating additional log directories..."

    # Create the specific log directories that the application expects
    sudo mkdir -p $APP_HOME/TeamSphereErrorLogs
    sudo mkdir -p $APP_HOME/TeamSphereDebugLogs

    # Set proper ownership
    sudo chown -R $APP_USER:$APP_USER $APP_HOME/TeamSphereErrorLogs
    sudo chown -R $APP_USER:$APP_USER $APP_HOME/TeamSphereDebugLogs

    # Set proper permissions
    sudo chmod 755 $APP_HOME/TeamSphereErrorLogs
    sudo chmod 755 $APP_HOME/TeamSphereDebugLogs

    log_success "Additional log directories created"
}

# Setup PostgreSQL database
setup_database() {
    log_info "Setting up PostgreSQL database..."

    # Initialize PostgreSQL if needed
    if [ "$DISTRO" = "centos" ] || [ "$DISTRO" = "rhel" ] || [ "$DISTRO" = "fedora" ]; then
        if [ ! -f /var/lib/pgsql/data/postgresql.conf ]; then
            sudo postgresql-setup initdb
        fi
    fi

    # Start and enable PostgreSQL
    sudo systemctl start postgresql
    sudo systemctl enable postgresql

    # Set postgres user password to 'voyage'
    log_info "Setting postgres user password..."
    sudo -u postgres psql -c "ALTER USER postgres WITH PASSWORD 'voyage';" 2>/dev/null || true

    # Set default password if not provided
    if [ -z "$DB_PASSWORD" ]; then
        DB_PASSWORD="voyage"
        log_info "Using default database password: $DB_PASSWORD"
    fi

    # Create database and user
    sudo -u postgres psql -c "CREATE DATABASE $DB_NAME;" 2>/dev/null || true
    sudo -u postgres psql -c "CREATE USER $DB_USER WITH PASSWORD '$DB_PASSWORD';" 2>/dev/null || true
    sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_USER;" 2>/dev/null || true
    sudo -u postgres psql -c "ALTER DATABASE $DB_NAME OWNER TO $DB_USER;" 2>/dev/null || true
    sudo -u postgres psql -c "ALTER USER $DB_USER CREATEDB;" 2>/dev/null || true

    # Grant permissions on existing tables and sequences (if any)
    sudo -u postgres psql -d $DB_NAME -c "GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $DB_USER;" 2>/dev/null || true
    sudo -u postgres psql -d $DB_NAME -c "GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO $DB_USER;" 2>/dev/null || true
    sudo -u postgres psql -d $DB_NAME -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $DB_USER;" 2>/dev/null || true

    log_success "PostgreSQL database setup completed"
}

# Test database connection and fix any issues
test_database_connection() {
    log_info "Testing database connection..."

    # Test the connection with the created credentials
    if PGPASSWORD="$DB_PASSWORD" psql -h localhost -U $DB_USER -d $DB_NAME -c "SELECT 1;" >/dev/null 2>&1; then
        log_success "Database connection test passed"
    else
        log_warning "Database connection test failed, attempting to fix permissions..."

        # Reset password to ensure it's correct
        sudo -u postgres psql -c "ALTER USER $DB_USER PASSWORD '$DB_PASSWORD';" 2>/dev/null || true

        # Re-grant all permissions
        sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_USER;" 2>/dev/null || true
        sudo -u postgres psql -c "ALTER DATABASE $DB_NAME OWNER TO $DB_USER;" 2>/dev/null || true
        sudo -u postgres psql -d $DB_NAME -c "GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $DB_USER;" 2>/dev/null || true
        sudo -u postgres psql -d $DB_NAME -c "GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO $DB_USER;" 2>/dev/null || true

        # Test again
        if PGPASSWORD="$DB_PASSWORD" psql -h localhost -U $DB_USER -d $DB_NAME -c "SELECT 1;" >/dev/null 2>&1; then
            log_success "Database connection fixed and working"
        else
            log_error "Database connection still failing. Please check PostgreSQL configuration."
            exit 1
        fi
    fi
}

# Restore database from dump file
restore_database() {
    log_info "Restoring database from dump file..."

    # Check if database dump file exists
    if [ ! -f "database/db.dump" ]; then
        log_warning "Database dump file not found. Skipping database restoration."
        log_info "Database will be initialized empty and populated by the application."
        return 0
    fi

    log_info "Found database dump file. Proceeding with restoration..."

    # First, drop and recreate the database to ensure clean restoration
    log_info "Preparing database for restoration..."
    sudo -u postgres psql -c "DROP DATABASE IF EXISTS $DB_NAME;" 2>/dev/null || true
    sudo -u postgres psql -c "CREATE DATABASE $DB_NAME;" 2>/dev/null || true
    sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_USER;" 2>/dev/null || true
    sudo -u postgres psql -c "ALTER DATABASE $DB_NAME OWNER TO $DB_USER;" 2>/dev/null || true

    # Restore the database using pg_restore
    log_info "Restoring database from dump file..."
    if PGPASSWORD="voyage" pg_restore -U postgres -d $DB_NAME -h localhost -p 5432 --clean --if-exists database/db.dump 2>/dev/null; then
        log_success "Database restored successfully from dump file"
    else
        log_warning "pg_restore failed, trying alternative restoration method..."

        # Try using psql if pg_restore fails (in case it's a SQL dump)
        if PGPASSWORD="voyage" psql -U postgres -d $DB_NAME -h localhost -p 5432 -f database/db.dump >/dev/null 2>&1; then
            log_success "Database restored successfully using psql"
        else
            log_warning "Database restoration failed. The application will initialize with an empty database."
            log_info "You can manually restore the database later using:"
            log_info "  pg_restore -U postgres -d $DB_NAME -h localhost -p 5432 --clean --if-exists database/db.dump"
            log_info "  or"
            log_info "  psql -U postgres -d $DB_NAME -h localhost -p 5432 -f database/db.dump"
        fi
    fi

    # Re-grant permissions after restoration
    log_info "Setting up database permissions after restoration..."
    sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_USER;" 2>/dev/null || true
    sudo -u postgres psql -c "ALTER DATABASE $DB_NAME OWNER TO $DB_USER;" 2>/dev/null || true
    sudo -u postgres psql -d $DB_NAME -c "GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $DB_USER;" 2>/dev/null || true
    sudo -u postgres psql -d $DB_NAME -c "GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO $DB_USER;" 2>/dev/null || true
    sudo -u postgres psql -d $DB_NAME -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $DB_USER;" 2>/dev/null || true

    log_success "Database restoration process completed"
}

# Deploy frontend using pre-built artifacts
deploy_frontend() {
    log_info "Deploying frontend..."

    if [ ! -d "frontend" ]; then
        log_error "Frontend build artifacts not found"
        exit 1
    fi

    # Copy built files to application directory
    sudo cp -r frontend/* $APP_HOME/frontend/
    sudo chown -R $APP_USER:$APP_USER $APP_HOME/frontend/

    log_success "Frontend deployed successfully"
}

# Deploy backend using pre-built artifacts
deploy_backend() {
    log_info "Deploying backend..."

    if [ ! -f "backend/teamsphere.jar" ]; then
        log_error "Backend JAR file not found"
        exit 1
    fi

    # Copy pre-built JAR file to application directory
    sudo cp backend/teamsphere.jar $APP_HOME/bin/teamsphere.jar
    sudo chown $APP_USER:$APP_USER $APP_HOME/bin/teamsphere.jar

    # Copy configuration files if they exist
    if [ -d "backend/config" ]; then
        sudo cp -r backend/config/* $APP_HOME/config/ 2>/dev/null || true
        sudo chown -R $APP_USER:$APP_USER $APP_HOME/config/
    fi

    log_success "Backend deployed successfully"
}

# Create application configuration
create_config() {
    log_info "Creating application configuration..."

    # Create application.properties
    sudo tee $APP_HOME/config/application.properties > /dev/null <<APPEOF
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/$DB_NAME
spring.datasource.username=$DB_USER
spring.datasource.password=$DB_PASSWORD
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.default_schema=public

# Server Configuration
server.port=$BACKEND_PORT

# File Upload Configuration
spring.web.resources.static-locations=file:$APP_HOME/data/uploads/
spring.servlet.multipart.max-file-size=1MB
spring.servlet.multipart.max-request-size=1MB

# Logging Configuration
logging.level.root=INFO
logging.level.org.springframework=INFO
logging.level.com.vbs.capsAllocation=DEBUG
logging.file.name=$APP_HOME/logs/teamsphere.log
logging.file.max-size=10MB
logging.file.max-history=30

# Mail Configuration (update with your SMTP settings)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=vbs-tms@vacobinary.in
spring.mail.password=uyyr zywa edwb kbeu
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Google Sheets Configuration (update with your credentials)
google.sheets.spreadsheetId=your-spreadsheet-id
google.sheets.credentials=classpath:ops-excellence-credentials.json
google.sheets.sheetName=Response
APPEOF

    # Create environment configuration for frontend based on system type
    sudo tee $APP_HOME/frontend/assets/env.js > /dev/null <<ENVEOF
window.env = {
    SERVER_IP: "$SERVER_IP"
};
ENVEOF

    sudo chown -R $APP_USER:$APP_USER $APP_HOME/config/
    sudo chown $APP_USER:$APP_USER $APP_HOME/frontend/assets/env.js

    log_success "Application configuration created"
}

# Note: CORS configuration update removed since we're using pre-built JAR files
# The CORS configuration is already compiled into the JAR file

# Create systemd service
create_service() {
    log_info "Creating systemd service..."

    sudo tee /etc/systemd/system/$SERVICE_NAME.service > /dev/null <<SERVICEEOF
[Unit]
Description=TeamSphere Application
After=network.target postgresql.service
Requires=postgresql.service

[Service]
Type=simple
User=$APP_USER
Group=$APP_USER
WorkingDirectory=$APP_HOME
ExecStart=/usr/bin/java -jar $APP_HOME/bin/teamsphere.jar --spring.config.location=file:$APP_HOME/config/application.properties
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=$SERVICE_NAME

# Environment variables
Environment=JAVA_HOME=$JAVA_HOME
Environment=SPRING_PROFILES_ACTIVE=production

# Security settings
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ReadWritePaths=$APP_HOME/logs $APP_HOME/data /var/log/$APP_NAME $APP_HOME/TeamSphereErrorLogs $APP_HOME/TeamSphereDebugLogs

[Install]
WantedBy=multi-user.target
SERVICEEOF

    sudo systemctl daemon-reload
    sudo systemctl enable $SERVICE_NAME

    log_success "Systemd service created"
}

# Configure Nginx
configure_nginx() {
    log_info "Configuring Nginx for system type: $SYSTEM_TYPE..."

    # Backup original configuration
    sudo cp /etc/nginx/sites-available/default /etc/nginx/sites-available/default.backup 2>/dev/null || true

    # Determine server_name based on system type
    if [ "$SYSTEM_TYPE" = "regular" ]; then
        SERVER_NAME="localhost"
    else
        SERVER_NAME="localhost $SERVER_IP"
    fi

    # Create TeamSphere Nginx configuration
    sudo tee /etc/nginx/sites-available/$APP_NAME > /dev/null <<NGINXEOF
server {
    listen $FRONTEND_PORT;
    server_name $SERVER_NAME;

    # Frontend static files
    location / {
        root $APP_HOME/frontend;
        try_files \$uri \$uri/ /index.html;
        index index.html;

        # Add CORS headers for frontend
        add_header 'Access-Control-Allow-Origin' '*' always;
        add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
        add_header 'Access-Control-Allow-Headers' 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization' always;
    }

    # Backend API proxy
    location /api/ {
        proxy_pass http://localhost:$BACKEND_PORT/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;

        # CORS headers for API
        add_header 'Access-Control-Allow-Origin' '*' always;
        add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
        add_header 'Access-Control-Allow-Headers' 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization' always;
        add_header 'Access-Control-Allow-Credentials' 'true' always;

        if (\$request_method = 'OPTIONS') {
            add_header 'Access-Control-Allow-Origin' '*' always;
            add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
            add_header 'Access-Control-Allow-Headers' 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization' always;
            add_header 'Access-Control-Max-Age' 1728000;
            add_header 'Content-Type' 'text/plain; charset=utf-8';
            add_header 'Content-Length' 0;
            return 204;
        }
    }

    # Static assets
    location /assets/ {
        root $APP_HOME/frontend;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # Logs
    access_log /var/log/nginx/$APP_NAME.access.log;
    error_log /var/log/nginx/$APP_NAME.error.log;
}
NGINXEOF

    # Enable the site
    sudo ln -sf /etc/nginx/sites-available/$APP_NAME /etc/nginx/sites-enabled/
    sudo rm -f /etc/nginx/sites-enabled/default

    # Test Nginx configuration
    sudo nginx -t

    # Start and enable Nginx
    sudo systemctl start nginx
    sudo systemctl enable nginx

    log_success "Nginx configured successfully"
}

# Start services and verify installation
start_and_verify_services() {
    log_info "Starting services and verifying installation..."

    # Start the TeamSphere service
    sudo systemctl start $SERVICE_NAME

    # Wait a moment for the service to start
    sleep 5

    # Check if the service is running
    if sudo systemctl is-active --quiet $SERVICE_NAME; then
        log_success "TeamSphere service started successfully"
    else
        log_warning "TeamSphere service may not have started properly"
        log_info "Checking service logs..."
        sudo journalctl -u $SERVICE_NAME -n 10 --no-pager
    fi

    # Ensure Nginx is running
    sudo systemctl restart nginx

    # Test if the frontend is accessible
    if curl -s http://localhost/ | grep -q "TeamSphere"; then
        log_success "Frontend is accessible"
    else
        log_warning "Frontend may not be properly configured"
    fi

    log_success "Service verification completed"
}

# Create default admin user
create_default_admin_user() {
    log_info "Creating default admin user..."

    # Wait for the application to be fully ready
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        log_info "Waiting for application to be ready (attempt $attempt/$max_attempts)..."

        # Check if the application is responding
        if curl -s http://localhost:$BACKEND_PORT/actuator/health >/dev/null 2>&1; then
            log_success "Application is ready"
            break
        fi

        if [ $attempt -eq $max_attempts ]; then
            log_warning "Application may not be fully ready, but proceeding with user creation..."
            break
        fi

        sleep 10
        attempt=$((attempt + 1))
    done

    # Create the default admin user using the signup endpoint
    log_info "Creating admin user with username 'admin' and role 'ADMIN_OPS_MANAGER'..."

    local signup_response
    signup_response=$(curl -s -w "%{http_code}" -o /tmp/signup_response.txt \
        -X POST "http://localhost:$BACKEND_PORT/admin/register?username=admin&password=adminpassword&role=ADMIN_OPS_MANAGER" \
        -H "Content-Type: application/json" \
        2>/dev/null)

    local http_code="${signup_response: -3}"
    local response_body=$(cat /tmp/signup_response.txt 2>/dev/null || echo "No response")

    if [ "$http_code" = "201" ] || [ "$http_code" = "200" ]; then
        log_success "Default admin user created successfully"
        log_info "Response: $response_body"
        log_info "Admin credentials:"
        log_info "  Username: admin"
        log_info "  Password: adminpassword"
        log_info "  Role: ADMIN_OPS_MANAGER"
        log_warning "Please change the default password after first login!"
    else
        log_warning "Failed to create admin user (HTTP $http_code)"
        log_info "Response: $response_body"
        log_info "You can manually create an admin user later using:"
        log_info "  curl -X POST 'http://localhost:$BACKEND_PORT/admin/register?username=admin&password=adminpassword&role=ADMIN_OPS_MANAGER'"
    fi

    # Clean up temporary file
    rm -f /tmp/signup_response.txt
}

# Main installation function
main() {
    log_info "Starting TeamSphere installation..."

    check_root
    check_sudo
    detect_system_type
    detect_distro
    check_requirements
    install_dependencies
    install_java
    create_app_user
    create_directories
    create_log_directories
    setup_database
    test_database_connection
    restore_database
    deploy_frontend
    deploy_backend
    create_config
    create_service
    configure_nginx
    start_and_verify_services
    create_default_admin_user

    log_success "TeamSphere installation completed successfully!"
    log_info ""
    log_info "=== IMPORTANT CREDENTIALS ==="
    log_info "Database password: $DB_PASSWORD"
    log_info "PostgreSQL admin password: voyage"
    log_info "Default Admin User:"
    log_info "  Username: admin"
    log_info "  Password: adminpassword"
    log_info "  Role: ADMIN_OPS_MANAGER"
    log_warning "SECURITY: Please change the default admin password after first login!"
    log_info "Please save these credentials securely."
    log_info ""
    log_info "=== DATABASE INFORMATION ==="
    log_info "Database has been restored from the provided dump file (db.dump)."
    log_info "Database Name: $DB_NAME"
    log_info "Database User: $DB_USER"
    log_info "If you need to manually restore the database later, use:"
    log_info "  pg_restore -U postgres -d $DB_NAME -h localhost -p 5432 --clean --if-exists /path/to/db.dump"
    log_info ""
    log_info "=== SYSTEM CONFIGURATION ==="
    log_info "System Type: $SYSTEM_TYPE"
    log_info "Server IP: $SERVER_IP"
    log_info ""
    log_info "=== CORS CONFIGURATION ==="
    log_info "The application is pre-configured to accept requests from:"
    log_info "  - http://localhost:4200 (Angular dev server)"
    log_info "  - http://localhost:8080 (Backend default port)"
    log_info "  - http://localhost:80 (Production frontend)"
    if [ "$SYSTEM_TYPE" != "regular" ]; then
        log_info "  - http://$SERVER_IP (System IP)"
        log_info "  - http://$SERVER_IP:4200 (Angular dev server on system IP)"
        log_info "  - http://$SERVER_IP:8080 (Backend on system IP)"
        log_info "  - http://$SERVER_IP:80 (Production frontend on system IP)"
    fi
    log_info "CORS configuration is built into the application JAR file."
    log_info ""
    log_info "=== APPLICATION ACCESS ==="
    log_info "Services have been started automatically."
    if [ "$SYSTEM_TYPE" = "regular" ]; then
        log_info "Web Interface: http://localhost:$FRONTEND_PORT"
        log_info "Backend API: http://localhost:$BACKEND_PORT"
    else
        log_info "Web Interface: http://$SERVER_IP:$FRONTEND_PORT"
        log_info "Alternative: http://localhost:$FRONTEND_PORT"
        log_info "Backend API: http://$SERVER_IP:$BACKEND_PORT"
        log_info "Alternative: http://localhost:$BACKEND_PORT"
    fi
    log_info ""
    log_info "=== SERVICE MANAGEMENT ==="
    log_info "Check status: sudo systemctl status $SERVICE_NAME"
    log_info "View logs: sudo journalctl -u $SERVICE_NAME -f"
    log_info "Restart: sudo systemctl restart $SERVICE_NAME"
    log_info ""
    log_info "=== TROUBLESHOOTING ==="
    log_info "If you encounter any issues, check the logs and ensure all services are running:"
    log_info "  sudo systemctl status $SERVICE_NAME nginx postgresql"
    log_info ""
    log_info "You can now log in to the application using the admin credentials above."

    # Auto-launch browser
    log_info ""
    log_info "=== LAUNCHING APPLICATION ==="
    log_info "Opening TeamSphere in your default browser..."

    # Function to detect and launch browser
    launch_browser() {
        # Determine the correct URL based on system type
        if [ "$SYSTEM_TYPE" = "regular" ]; then
            local url="http://localhost:\$FRONTEND_PORT"
        else
            local url="http://\$SERVER_IP:\$FRONTEND_PORT"
        fi

        # Check if we're in a desktop environment
        if [ -n "\$DISPLAY" ] || [ -n "\$WAYLAND_DISPLAY" ]; then
            # Try different browser commands in order of preference
            if command -v xdg-open >/dev/null 2>&1; then
                log_info "Launching browser with xdg-open..."
                xdg-open "\$url" >/dev/null 2>&1 &
            elif command -v gnome-open >/dev/null 2>&1; then
                log_info "Launching browser with gnome-open..."
                gnome-open "\$url" >/dev/null 2>&1 &
            elif command -v firefox >/dev/null 2>&1; then
                log_info "Launching Firefox..."
                firefox "\$url" >/dev/null 2>&1 &
            elif command -v google-chrome >/dev/null 2>&1; then
                log_info "Launching Google Chrome..."
                google-chrome "\$url" >/dev/null 2>&1 &
            elif command -v chromium-browser >/dev/null 2>&1; then
                log_info "Launching Chromium..."
                chromium-browser "\$url" >/dev/null 2>&1 &
            elif command -v opera >/dev/null 2>&1; then
                log_info "Launching Opera..."
                opera "\$url" >/dev/null 2>&1 &
            else
                log_warning "No suitable browser found for auto-launch."
                log_info "Please manually open: \$url"
                return 1
            fi

            log_success "Browser launched! TeamSphere should open shortly."
            log_info "If the browser doesn't open, manually navigate to: \$url"
            return 0
        else
            log_info "No desktop environment detected (headless system)."
            log_info "Please manually open: \$url in your browser"
            return 1
        fi
    }

    # Launch browser
    launch_browser

    log_info ""
    log_info "=== QUICK START ==="
    log_info "1. Login with username: admin, password: adminpassword"
    log_info "2. Change the default password in user settings"
    log_info "3. Start using TeamSphere!"
}

# Run main function
main "$@"
EOF

    # Create manager script
    cat > $PACKAGE_DIR/teamsphere-manager.sh << 'EOF'
#!/bin/bash

# TeamSphere Management Script
# Version: 1.0
# Description: Manages TeamSphere application lifecycle

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SERVICE_NAME="teamsphere"
APP_HOME="/opt/teamsphere"

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if service exists
check_service() {
    if ! systemctl list-unit-files | grep -q "$SERVICE_NAME.service"; then
        log_error "TeamSphere service not found. Please run the installer first."
        exit 1
    fi
}

# Start the application
start_app() {
    log_info "Starting TeamSphere application..."
    sudo systemctl start $SERVICE_NAME
    sudo systemctl start nginx
    log_success "TeamSphere started successfully"
}

# Stop the application
stop_app() {
    log_info "Stopping TeamSphere application..."
    sudo systemctl stop $SERVICE_NAME
    log_success "TeamSphere stopped successfully"
}

# Restart the application
restart_app() {
    log_info "Restarting TeamSphere application..."
    sudo systemctl restart $SERVICE_NAME
    sudo systemctl restart nginx
    log_success "TeamSphere restarted successfully"
}

# Check application status
status_app() {
    log_info "Checking TeamSphere status..."
    echo ""
    echo "=== TeamSphere Service Status ==="
    sudo systemctl status $SERVICE_NAME --no-pager
    echo ""
    echo "=== Nginx Status ==="
    sudo systemctl status nginx --no-pager
    echo ""
    echo "=== PostgreSQL Status ==="
    sudo systemctl status postgresql --no-pager
}

# View application logs
logs_app() {
    log_info "Showing TeamSphere logs..."
    sudo journalctl -u $SERVICE_NAME -n 50 --no-pager
}

# Follow application logs
follow_logs() {
    log_info "Following TeamSphere logs (Ctrl+C to exit)..."
    sudo journalctl -u $SERVICE_NAME -f
}

# Enable auto-start
enable_app() {
    log_info "Enabling TeamSphere auto-start..."
    sudo systemctl enable $SERVICE_NAME
    sudo systemctl enable nginx
    sudo systemctl enable postgresql
    log_success "Auto-start enabled"
}

# Disable auto-start
disable_app() {
    log_info "Disabling TeamSphere auto-start..."
    sudo systemctl disable $SERVICE_NAME
    log_success "Auto-start disabled"
}

# Create backup
backup_app() {
    log_info "Creating TeamSphere backup..."
    BACKUP_DIR="/tmp/teamsphere-backup-$(date +%Y%m%d-%H%M%S)"
    mkdir -p $BACKUP_DIR

    # Backup application files
    sudo cp -r $APP_HOME $BACKUP_DIR/

    # Backup database
    sudo -u postgres pg_dump vbs_allocation_caps > $BACKUP_DIR/database.sql

    # Create archive
    tar -czf $BACKUP_DIR.tar.gz -C /tmp $(basename $BACKUP_DIR)
    rm -rf $BACKUP_DIR

    log_success "Backup created: $BACKUP_DIR.tar.gz"
}

# Launch browser
launch_browser() {
    log_info "Opening TeamSphere in your default browser..."
    local url="http://localhost"

    # Check if we're in a desktop environment
    if [ -n "$DISPLAY" ] || [ -n "$WAYLAND_DISPLAY" ]; then
        # Try different browser commands in order of preference
        if command -v xdg-open >/dev/null 2>&1; then
            log_info "Launching browser with xdg-open..."
            xdg-open "$url" >/dev/null 2>&1 &
        elif command -v gnome-open >/dev/null 2>&1; then
            log_info "Launching browser with gnome-open..."
            gnome-open "$url" >/dev/null 2>&1 &
        elif command -v firefox >/dev/null 2>&1; then
            log_info "Launching Firefox..."
            firefox "$url" >/dev/null 2>&1 &
        elif command -v google-chrome >/dev/null 2>&1; then
            log_info "Launching Google Chrome..."
            google-chrome "$url" >/dev/null 2>&1 &
        elif command -v chromium-browser >/dev/null 2>&1; then
            log_info "Launching Chromium..."
            chromium-browser "$url" >/dev/null 2>&1 &
        elif command -v opera >/dev/null 2>&1; then
            log_info "Launching Opera..."
            opera "$url" >/dev/null 2>&1 &
        else
            log_warning "No suitable browser found for auto-launch."
            log_info "Please manually open: $url"
            return 1
        fi

        log_success "Browser launched! TeamSphere should open shortly."
        log_info "If the browser doesn't open, manually navigate to: $url"
        return 0
    else
        log_info "No desktop environment detected (headless system)."
        log_info "Please manually open: $url in your browser"
        return 1
    fi
}

# Show usage
show_usage() {
    echo "TeamSphere Management Script"
    echo ""
    echo "Usage: $0 {start|stop|restart|status|logs|follow|enable|disable|backup|open}"
    echo ""
    echo "Commands:"
    echo "  start    - Start the TeamSphere application"
    echo "  stop     - Stop the TeamSphere application"
    echo "  restart  - Restart the TeamSphere application"
    echo "  status   - Show application status"
    echo "  logs     - Show recent application logs"
    echo "  follow   - Follow application logs in real-time"
    echo "  enable   - Enable auto-start on boot"
    echo "  disable  - Disable auto-start on boot"
    echo "  backup   - Create a backup of the application"
    echo "  open     - Open TeamSphere in your default browser"
    echo ""
}

# Main function
main() {
    case "${1:-}" in
        start)
            check_service
            start_app
            ;;
        stop)
            check_service
            stop_app
            ;;
        restart)
            check_service
            restart_app
            ;;
        status)
            check_service
            status_app
            ;;
        logs)
            check_service
            logs_app
            ;;
        follow)
            check_service
            follow_logs
            ;;
        enable)
            check_service
            enable_app
            ;;
        disable)
            check_service
            disable_app
            ;;
        backup)
            check_service
            backup_app
            ;;
        open)
            launch_browser
            ;;
        *)
            show_usage
            exit 1
            ;;
    esac
}

# Run main function
main "$@"
EOF

    # Create uninstaller script
    cat > $PACKAGE_DIR/teamsphere-uninstaller.sh << 'EOF'
#!/bin/bash

# TeamSphere Uninstaller
# Version: 1.0
# Description: Removes TeamSphere application from the system

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
APP_NAME="teamsphere"
APP_USER="teamsphere"
APP_HOME="/opt/teamsphere"
SERVICE_NAME="teamsphere"
DB_NAME="vbs_allocation_caps"
DB_USER="teamsphere"

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Confirm uninstallation
confirm_uninstall() {
    echo ""
    log_warning "This will completely remove TeamSphere from your system!"
    echo ""
    echo "What would you like to remove?"
    echo "1) Application only (keep database and user data)"
    echo "2) Application + Database (keep system dependencies)"
    echo "3) Everything (application, database, and dependencies)"
    echo "4) Cancel"
    echo ""
    read -p "Enter your choice (1-4): " choice

    case $choice in
        1) REMOVE_LEVEL="app" ;;
        2) REMOVE_LEVEL="app_db" ;;
        3) REMOVE_LEVEL="all" ;;
        4) log_info "Uninstallation cancelled"; exit 0 ;;
        *) log_error "Invalid choice"; exit 1 ;;
    esac
}

# Stop services
stop_services() {
    log_info "Stopping services..."

    sudo systemctl stop $SERVICE_NAME 2>/dev/null || true
    sudo systemctl disable $SERVICE_NAME 2>/dev/null || true

    log_success "Services stopped"
}

# Remove application files
remove_application() {
    log_info "Removing application files..."

    # Remove systemd service
    sudo rm -f /etc/systemd/system/$SERVICE_NAME.service
    sudo systemctl daemon-reload

    # Remove application directory
    sudo rm -rf $APP_HOME
    sudo rm -rf /var/log/$APP_NAME
    sudo rm -rf /etc/$APP_NAME

    # Remove nginx configuration
    sudo rm -f /etc/nginx/sites-available/$APP_NAME
    sudo rm -f /etc/nginx/sites-enabled/$APP_NAME

    # Restore default nginx site
    sudo ln -sf /etc/nginx/sites-available/default /etc/nginx/sites-enabled/ 2>/dev/null || true

    # Remove application user
    sudo userdel $APP_USER 2>/dev/null || true

    log_success "Application files removed"
}

# Remove database
remove_database() {
    log_info "Removing database..."

    sudo -u postgres dropdb $DB_NAME 2>/dev/null || true
    sudo -u postgres dropuser $DB_USER 2>/dev/null || true

    log_success "Database removed"
}

# Remove dependencies
remove_dependencies() {
    log_info "Removing system dependencies..."

    # Detect distribution
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        DISTRO=$ID
    else
        log_warning "Cannot detect distribution, skipping dependency removal"
        return
    fi

    case $DISTRO in
        ubuntu|debian)
            sudo apt-get remove --purge -y openjdk-17-jdk nodejs postgresql postgresql-contrib nginx
            sudo apt-get autoremove -y
            ;;
        centos|rhel|fedora)
            if command -v dnf &> /dev/null; then
                sudo dnf remove -y java-17-openjdk java-17-openjdk-devel nodejs npm postgresql postgresql-server postgresql-contrib nginx
            else
                sudo yum remove -y java-17-openjdk java-17-openjdk-devel nodejs npm postgresql postgresql-server postgresql-contrib nginx
            fi
            ;;
    esac

    log_success "Dependencies removed"
}

# Main uninstallation function
main() {
    log_info "Starting TeamSphere uninstallation..."

    confirm_uninstall

    stop_services
    remove_application

    if [ "$REMOVE_LEVEL" = "app_db" ] || [ "$REMOVE_LEVEL" = "all" ]; then
        remove_database
    fi

    if [ "$REMOVE_LEVEL" = "all" ]; then
        remove_dependencies
    fi

    log_success "TeamSphere uninstallation completed!"

    if [ "$REMOVE_LEVEL" = "app" ]; then
        log_info "Database and user data preserved"
    elif [ "$REMOVE_LEVEL" = "app_db" ]; then
        log_info "System dependencies preserved (Java, Node.js, PostgreSQL, Nginx)"
    fi
}

# Run main function
main "$@"
EOF

    # Create README
    cat > $PACKAGE_DIR/README.md << 'EOF'
# TeamSphere Linux Installer

This installer package allows you to install and run TeamSphere application locally on any Linux system.

## Overview

TeamSphere is a comprehensive team management application consisting of:
- **Frontend**: Angular 16 application for the user interface
- **Backend**: Spring Boot application providing REST APIs
- **Database**: PostgreSQL for data storage with pre-populated data from included dump file
- **Web Server**: Nginx for serving frontend and proxying backend requests

### Database Restoration
This installer includes a database dump file (db.dump) that will be automatically restored during installation, providing:
- Pre-configured database schema
- Sample data and initial configurations
- Ready-to-use application state

## System Requirements

### Minimum Requirements
- **OS**: Ubuntu 18.04+, Debian 10+, CentOS 7+, RHEL 7+, Fedora 30+
- **RAM**: 2GB (4GB recommended)
- **Disk Space**: 5GB free space
- **Network**: Internet connection for downloading dependencies

### Supported Distributions
- Ubuntu 18.04, 20.04, 22.04, 24.04
- Debian 10, 11, 12
- CentOS 7, 8
- RHEL 7, 8, 9
- Fedora 30+

## Installation

### Prerequisites
1. A regular user account with sudo privileges
2. Internet connection for downloading dependencies

### Quick Installation

1. **Download the installer files** to your system
2. **Make the installer executable**:
   ```bash
   chmod +x teamsphere-installer.sh
   ```

3. **Run the installer**:
   ```bash
   ./teamsphere-installer.sh
   ```

4. **Select your system type** when prompted:
   - **Regular Linux system**: For standard Linux installations
   - **Chromebook with Linux (Penguin)**: For Chromebook users running Linux
   - **WSL (Windows Subsystem for Linux)**: For Windows users with WSL
   - **Custom IP address**: For custom network configurations

5. **Follow the installation process** - the installer will:
   - Detect your system type and configure CORS accordingly
   - Check system requirements
   - Install dependencies (Java 17, Node.js, PostgreSQL, Nginx)
   - Create application user and directories
   - Build and deploy the application
   - Configure services with proper IP settings
   - Set up the database

6. **Start the application**:
   ```bash
   sudo systemctl start teamsphere
   ```

### What Gets Installed

The installer will install and configure:

- **Java 17 OpenJDK** - Required for the Spring Boot backend
- **Node.js 18 LTS** - Required for building the Angular frontend
- **PostgreSQL** - Database server with restored data from included dump file
- **Nginx** - Web server and reverse proxy
- **TeamSphere Application** - Installed in `/opt/teamsphere`
- **Systemd Service** - For managing the application lifecycle
- **Database Restoration** - Automatically restores database from included db.dump file

## Usage

### Managing the Application

Use the provided management script for common operations:

```bash
# Make the manager script executable
chmod +x teamsphere-manager.sh

# Start the application
./teamsphere-manager.sh start

# Stop the application
./teamsphere-manager.sh stop

# Restart the application
./teamsphere-manager.sh restart

# Check application status
./teamsphere-manager.sh status

# View logs
./teamsphere-manager.sh logs

# Follow logs in real-time
./teamsphere-manager.sh follow

# Enable auto-start on boot
./teamsphere-manager.sh enable

# Create a backup
./teamsphere-manager.sh backup
```

### Accessing the Application

Once installed and started:

- **Web Interface**: http://localhost (port 80)
- **Backend API**: http://localhost:8080
- **Database**: localhost:5432

## Uninstallation

To completely remove TeamSphere:

1. **Make the uninstaller executable**:
   ```bash
   chmod +x teamsphere-uninstaller.sh
   ```

2. **Run the uninstaller**:
   ```bash
   ./teamsphere-uninstaller.sh
   ```

3. **Follow the prompts** to choose what to remove:
   - Application files and configuration
   - Database and user data
   - System dependencies

## Support

For issues and support:
1. Check the troubleshooting section above
2. Review application logs
3. Ensure all services are running
4. Verify configuration files

## License

This installer is provided as-is for deploying the TeamSphere application locally.
EOF

    log_success "All installer scripts created"
}

# Create version info file
create_version_info() {
    log_info "Creating version info..."

    cat > $PACKAGE_DIR/VERSION << EOF
TeamSphere Installer Package
Version: $PACKAGE_VERSION
Build Date: $(date)
Build Host: $(hostname)

Components:
- Frontend: Angular 16
- Backend: Spring Boot 3.4.0
- Database: PostgreSQL
- Web Server: Nginx

System Requirements:
- Linux (Ubuntu 18.04+, Debian 10+, CentOS 7+, RHEL 7+, Fedora 30+)
- 2GB RAM minimum (4GB recommended)
- 5GB disk space
- Internet connection for dependencies
EOF

    log_success "Version info created"
}

# Create installation guide
create_installation_guide() {
    log_info "Creating quick installation guide..."

    cat > $PACKAGE_DIR/QUICK_START.md << 'EOF'
# TeamSphere Quick Installation Guide

## Prerequisites
- Linux system (Ubuntu, Debian, CentOS, RHEL, Fedora)
- User account with sudo privileges
- Internet connection

## Installation Steps

1. **Extract the package** (if downloaded as archive):
   ```bash
   tar -xzf teamsphere-installer-*.tar.gz
   cd teamsphere-installer-*
   ```

2. **Run the installer**:
   ```bash
   ./teamsphere-installer.sh
   ```

3. **Select your system type** when prompted:
   - Regular Linux system (localhost)
   - Chromebook with Linux (Penguin/WSL-like)
   - WSL (Windows Subsystem for Linux)
   - Custom IP address

4. **Start the application**:
   ```bash
   sudo systemctl start teamsphere
   ```

4. **Access the application**:
   - For regular Linux: http://localhost
   - For Chromebook/WSL: http://[your-detected-ip]
   - The installer will display the correct URL for your system

## Management

Use the management script for common operations:

```bash
# Check status
./teamsphere-manager.sh status

# View logs
./teamsphere-manager.sh logs

# Restart application
./teamsphere-manager.sh restart
```

## Uninstallation

To remove TeamSphere:
```bash
./teamsphere-uninstaller.sh
```

For detailed instructions, see README.md
EOF

    log_success "Quick start guide created"
}

# Create checksums
create_checksums() {
    log_info "Creating checksums..."

    cd $PACKAGE_DIR
    find . -type f -exec sha256sum {} \; > CHECKSUMS.sha256
    cd - > /dev/null

    log_success "Checksums created"
}

# Create the final package archive
create_archive() {
    log_info "Creating package archive..."

    cd $BUILD_DIR
    tar -czf "$PACKAGE_NAME-$PACKAGE_VERSION.tar.gz" "$PACKAGE_NAME-$PACKAGE_VERSION"
    cd - > /dev/null

    # Calculate archive size
    ARCHIVE_SIZE=$(du -h "$BUILD_DIR/$PACKAGE_NAME-$PACKAGE_VERSION.tar.gz" | cut -f1)

    log_success "Package archive created: $BUILD_DIR/$PACKAGE_NAME-$PACKAGE_VERSION.tar.gz ($ARCHIVE_SIZE)"
}

# Generate package info
generate_package_info() {
    log_info "Generating package information..."

    cat > $BUILD_DIR/PACKAGE_INFO.txt << EOF
TeamSphere Linux Installer Package
==================================

Package: $PACKAGE_NAME-$PACKAGE_VERSION.tar.gz
Version: $PACKAGE_VERSION
Build Date: $(date)
Archive Size: $(du -h "$BUILD_DIR/$PACKAGE_NAME-$PACKAGE_VERSION.tar.gz" | cut -f1)

Contents:
- TeamSphere application source code
- Pre-compiled JAR and class files
- Installation scripts
- Management tools
- Documentation

Installation:
1. Extract: tar -xzf $PACKAGE_NAME-$PACKAGE_VERSION.tar.gz
2. Enter directory: cd $PACKAGE_NAME-$PACKAGE_VERSION
3. Run installer: ./teamsphere-installer.sh

System Requirements:
- Linux (Ubuntu 18.04+, Debian 10+, CentOS 7+, RHEL 7+, Fedora 30+)
- 2GB RAM minimum (4GB recommended)
- 5GB disk space
- Internet connection for dependencies

Support:
- See README.md for detailed instructions
- See QUICK_START.md for quick installation
- Use teamsphere-manager.sh for application management

SHA256 Checksum:
$(sha256sum "$BUILD_DIR/$PACKAGE_NAME-$PACKAGE_VERSION.tar.gz" | cut -d' ' -f1)
EOF

    log_success "Package information generated"
}

# Validate package
validate_package() {
    log_info "Validating package..."

    # Check if archive was created
    if [ ! -f "$BUILD_DIR/$PACKAGE_NAME-$PACKAGE_VERSION.tar.gz" ]; then
        log_error "Package archive not found"
        exit 1
    fi

    # Test archive integrity
    if ! tar -tzf "$BUILD_DIR/$PACKAGE_NAME-$PACKAGE_VERSION.tar.gz" > /dev/null; then
        log_error "Package archive is corrupted"
        exit 1
    fi

    # Check required files in archive
    REQUIRED_FILES=(
        "teamsphere-installer.sh"
        "teamsphere-uninstaller.sh"
        "teamsphere-manager.sh"
        "README.md"
        "QUICK_START.md"
        "VERSION"
        "frontend/index.html"
        "backend/teamsphere.jar"
    )

    for file in "${REQUIRED_FILES[@]}"; do
        if ! tar -tzf "$BUILD_DIR/$PACKAGE_NAME-$PACKAGE_VERSION.tar.gz" | grep -q "$PACKAGE_NAME-$PACKAGE_VERSION/$file"; then
            log_error "Required file missing in package: $file"
            exit 1
        fi
    done

    log_success "Package validation passed"
}

# Create self-extracting binary installer
create_self_extracting_installer() {
    log_info "Creating self-extracting binary installer..."

    # Ensure the archive exists
    if [ ! -f "$BUILD_DIR/$PACKAGE_NAME-$PACKAGE_VERSION.tar.gz" ]; then
        log_error "Archive file not found: $BUILD_DIR/$PACKAGE_NAME-$PACKAGE_VERSION.tar.gz"
        exit 1
    fi

    # Create the self-extracting script
    cat > $BUILD_DIR/teamsphere-installer.bin << 'SELFEXTRACT_EOF'
#!/bin/bash

# TeamSphere Self-Extracting Installer
# This script contains the installer and all necessary files

INSTALLER_NAME="TeamSphere Installer"
TEMP_DIR="/tmp/teamsphere-install-$$"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Extract the archive
extract_installer() {
    log_info "Extracting $INSTALLER_NAME..."

    # Create temporary directory
    mkdir -p "$TEMP_DIR"

    # Find the start of the binary data
    local archive_start=$(awk '/^__ARCHIVE_BELOW__$/{print NR + 1; exit 0; }' "$0")

    if [ -z "$archive_start" ]; then
        log_error "Archive marker not found in installer"
        cleanup_and_exit 1
    fi

    # Extract the embedded archive
    tail -n +$archive_start "$0" | tar -xzf - -C "$TEMP_DIR"

    if [ $? -ne 0 ]; then
        log_error "Failed to extract installer files"
        cleanup_and_exit 1
    fi

    log_success "Installer files extracted to $TEMP_DIR"
}

# Cleanup function
cleanup_and_exit() {
    local exit_code=${1:-0}
    if [ -d "$TEMP_DIR" ]; then
        rm -rf "$TEMP_DIR"
    fi
    exit $exit_code
}

# Trap to ensure cleanup on exit
trap 'cleanup_and_exit' EXIT INT TERM

# Main execution
main() {
    echo ""
    echo "=================================================="
    echo "           TeamSphere Linux Installer"
    echo "=================================================="
    echo ""

    log_info "Starting $INSTALLER_NAME..."

    # Extract installer files
    extract_installer

    # Change to the extracted directory and find the package directory
    cd "$TEMP_DIR"

    # Find the package directory (should be teamsphere-installer-1.0.0)
    local package_dir=$(find . -maxdepth 1 -type d -name "teamsphere-installer-*" | head -1)

    if [ -z "$package_dir" ]; then
        log_error "Package directory not found in extracted files"
        cleanup_and_exit 1
    fi

    cd "$package_dir"

    # Make the installer script executable
    chmod +x teamsphere-installer.sh

    # Run the installer
    log_info "Launching installer..."
    ./teamsphere-installer.sh

    local installer_exit_code=$?

    if [ $installer_exit_code -eq 0 ]; then
        log_success "Installation completed successfully!"
    else
        log_error "Installation failed with exit code $installer_exit_code"
    fi

    # Cleanup will be handled by the trap
    exit $installer_exit_code
}

# Run main function
main "$@"

# Archive marker - do not remove this line
__ARCHIVE_BELOW__
SELFEXTRACT_EOF

    # Append the tar.gz archive to the script
    cat "$BUILD_DIR/$PACKAGE_NAME-$PACKAGE_VERSION.tar.gz" >> "$BUILD_DIR/teamsphere-installer.bin"

    # Make the binary executable
    chmod +x "$BUILD_DIR/teamsphere-installer.bin"

    log_success "Self-extracting binary installer created: $BUILD_DIR/teamsphere-installer.bin"
}

# Main function
main() {
    log_info "Creating TeamSphere installer package..."

    check_sources
    build_frontend
    build_backend
    create_build_structure
    copy_built_artifacts
    copy_installer_scripts
    create_version_info
    create_installation_guide
    create_checksums
    create_archive
    create_self_extracting_installer
    generate_package_info

    echo ""
    log_success "TeamSphere installer package created successfully!"
    echo ""
    log_info "Package Details:"
    echo "  Binary Installer: $BUILD_DIR/teamsphere-installer.bin"
    echo "  Archive File: $BUILD_DIR/$PACKAGE_NAME-$PACKAGE_VERSION.tar.gz"
    echo "  Size (Binary): $(du -h "$BUILD_DIR/teamsphere-installer.bin" | cut -f1)"
    echo "  Size (Archive): $(du -h "$BUILD_DIR/$PACKAGE_NAME-$PACKAGE_VERSION.tar.gz" | cut -f1)"
    echo "  SHA256 (Binary): $(sha256sum "$BUILD_DIR/teamsphere-installer.bin" | cut -d' ' -f1)"
    echo ""
    log_info "To distribute:"
    echo "  1. Share the binary installer: $BUILD_DIR/teamsphere-installer.bin"
    echo "  2. Include the package info: $BUILD_DIR/PACKAGE_INFO.txt"
    echo ""
    log_info "Installation on target system:"
    echo "  1. chmod +x teamsphere-installer.bin"
    echo "  2. ./teamsphere-installer.bin"
    echo ""
    log_success "The installer now contains pre-built frontend and backend artifacts!"
}

# Run main function
main "$@"
