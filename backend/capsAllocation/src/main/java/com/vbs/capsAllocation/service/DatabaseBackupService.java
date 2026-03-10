package com.vbs.capsAllocation.service;

import com.vbs.capsAllocation.dto.DatabaseBackupResponseDTO;

/**
 * Service interface for managing database backup operations.
 */
public interface DatabaseBackupService {

    /**
     * Creates a backup of the database and uploads it to Google Drive
     * @param initiatedBy The username of the person initiating the backup
     * @return DatabaseBackupResponseDTO with operation details
     */
    DatabaseBackupResponseDTO createAndUploadBackup(String initiatedBy);

    /**
     * Imports data from a backup file
     * @param fileId Optional file ID to import from a specific backup. If null, imports from the most recent backup.
     * @param initiatedBy The username of the person initiating the import
     * @return DatabaseBackupResponseDTO with operation details
     */
    DatabaseBackupResponseDTO importFromBackup(String fileId, String initiatedBy);

    /**
     * Starts an asynchronous import from a backup file
     * @param fileId Optional file ID to import from a specific backup. If null, imports from the most recent backup.
     * @param initiatedBy The username of the person initiating the import
     * @return DatabaseBackupResponseDTO with operation ID for tracking progress
     */
    DatabaseBackupResponseDTO startAsyncImportFromBackup(String fileId, String initiatedBy);

    /**
     * Gets the status of a database operation
     * @param operationId The operation ID to check status for
     * @return DatabaseBackupResponseDTO with current operation status, or null if not found
     */
    DatabaseBackupResponseDTO getOperationStatus(String operationId);
}
