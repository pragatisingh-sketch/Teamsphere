#!/bin/bash

# Check if PostgreSQL is installed
if ! command -v psql &> /dev/null
then
    echo "PostgreSQL is not installed. Installing..."
    # Install PostgreSQL (for Ubuntu/Debian-based systems)
    sudo apt update
    sudo apt install -y postgresql postgresql-contrib

    echo "PostgreSQL installed successfully."
else
    echo "PostgreSQL is already installed."
fi

# Check if PostgreSQL service is running
if ! systemctl is-active --quiet postgresql
then
    echo "PostgreSQL is not running. Starting the service..."
    sudo systemctl start postgresql
    echo "PostgreSQL service started."
else
    echo "PostgreSQL service is already running."
fi

# Enable PostgreSQL service to start on boot
sudo systemctl enable postgresql

# Set password for postgres user
echo "Setting password for postgres user..."
sudo -u postgres psql -c "ALTER USER postgres PASSWORD 'voyage';"

# Check if database exists and create if it doesn't
echo "Checking if database 'vbs_allocation_caps' exists..."
DB_EXISTS=$(sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname='vbs_allocation_caps'")

if [ "$DB_EXISTS" != "1" ]; then
    echo "Creating database 'vbs_allocation_caps'..."
    sudo -u postgres createdb vbs_allocation_caps

    # Set database permissions
    sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE vbs_allocation_caps TO postgres;"

    echo "Database 'vbs_allocation_caps' created successfully."
else
    echo "Database 'vbs_allocation_caps' already exists."
fi

# Verify the setup
echo "Verifying PostgreSQL setup..."
sudo -u postgres psql -c "SELECT version();"
sudo -u postgres psql -c "\l" | grep vbs_allocation_caps

echo "PostgreSQL setup complete. Database 'vbs_allocation_caps' is ready with user 'postgres' and password 'voyage'."

