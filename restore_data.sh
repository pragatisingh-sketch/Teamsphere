#!/bin/bash
set -e  # exit on any error

DB_NAME="vbs_allocation_caps"
DUMP_FILE="/tmp/vbs_allocation_caps_20250928_010000.sql"
TMP_FILE="/tmp/$(basename "$DUMP_FILE")"

# Ensure dump file is readable by postgres
if [ ! -r "$DUMP_FILE" ]; then
    echo "Dump file is not readable. Copying to /tmp..."
    cp "$DUMP_FILE" "$TMP_FILE"
    chown postgres:postgres "$TMP_FILE"
    chmod 644 "$TMP_FILE"
    DUMP_FILE="$TMP_FILE"
fi

# Terminate active connections
echo "Terminating connections to $DB_NAME..."
psql -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$DB_NAME';"

# Drop database if exists
echo "Dropping database $DB_NAME..."
psql -d postgres -c "DROP DATABASE IF EXISTS $DB_NAME;"

# Create database
echo "Creating database $DB_NAME..."
psql -d postgres -c "CREATE DATABASE $DB_NAME;"

# Restore database from dump
echo "Restoring data from $DUMP_FILE..."
psql -d $DB_NAME -f "$DUMP_FILE"

echo "✅ Database $DB_NAME restored successfully!"

