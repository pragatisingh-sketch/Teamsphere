#!/bin/bash

# Configuration
# Default to /home/piyushm locally, but use the provided paths for production
BACKUP_DIR="${1:-$HOME/teamsphere/backups}"
DB_NAME="vbs_allocation_caps"
DB_USER="postgres"
DB_HOST="localhost"
DB_PORT="5432"

# Date format for the filename
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="$BACKUP_DIR/${DB_NAME}_$TIMESTAMP.sql"

# Ensure backup directory exists
mkdir -p "$BACKUP_DIR"

# Check if we can write to the directory
if [ ! -w "$BACKUP_DIR" ]; then
    echo "Error: Cannot write to backup directory $BACKUP_DIR"
    exit 1
fi

# Export password for pg_dump (use env var or default)
export PGPASSWORD="${DB_PASSWORD:-voyage}"

echo "Starting backup for database '$DB_NAME' to '$BACKUP_FILE'..."

# Perform the backup
if pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -F p -f "$BACKUP_FILE" "$DB_NAME"; then
    echo "Backup completed successfully."
    
    # Delete backups older than 30 days
    echo "Cleaning up backups older than 30 days..."
    find "$BACKUP_DIR" -name "${DB_NAME}_*.sql" -type f -mtime +30 -print -delete
    
    echo "Backup process finished."
else
    echo "Error: Backup failed."
    rm -f "$BACKUP_FILE" # Remove partial file
    exit 1
fi

