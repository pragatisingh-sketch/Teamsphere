package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for database backup operation responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseBackupResponseDTO {
    
    /**
     * Unique identifier for the backup operation
     */
    private String operationId;
    
    /**
     * Type of operation (BACKUP or IMPORT)
     */
    private String operationType;
    
    /**
     * Status of the operation (SUCCESS, FAILED, IN_PROGRESS)
     */
    private String status;
    
    /**
     * Google Drive file ID (for backup operations)
     */
    private String fileId;
    
    /**
     * Name of the backup file
     */
    private String fileName;
    
    /**
     * Size of the backup file in bytes
     */
    private Long fileSize;
    
    /**
     * Timestamp when the operation was initiated
     */
    private LocalDateTime timestamp;
    
    /**
     * Username of the person who initiated the operation
     */
    private String initiatedBy;
    
    /**
     * Additional details or error message
     */
    private String details;
    
    /**
     * Duration of the operation in milliseconds
     */
    private Long durationMs;
    
    /**
     * Create a success response for backup operation
     */
    public static DatabaseBackupResponseDTO backupSuccess(String operationId, String fileId, String fileName, 
                                                         Long fileSize, String initiatedBy, Long durationMs) {
        DatabaseBackupResponseDTO response = new DatabaseBackupResponseDTO();
        response.setOperationId(operationId);
        response.setOperationType("BACKUP");
        response.setStatus("SUCCESS");
        response.setFileId(fileId);
        response.setFileName(fileName);
        response.setFileSize(fileSize);
        response.setTimestamp(LocalDateTime.now());
        response.setInitiatedBy(initiatedBy);
        response.setDurationMs(durationMs);
        response.setDetails("Database backup completed successfully");
        return response;
    }
    
    /**
     * Create a success response for import operation
     */
    public static DatabaseBackupResponseDTO importSuccess(String operationId, String fileName, 
                                                        String initiatedBy, Long durationMs) {
        DatabaseBackupResponseDTO response = new DatabaseBackupResponseDTO();
        response.setOperationId(operationId);
        response.setOperationType("IMPORT");
        response.setStatus("SUCCESS");
        response.setFileName(fileName);
        response.setTimestamp(LocalDateTime.now());
        response.setInitiatedBy(initiatedBy);
        response.setDurationMs(durationMs);
        response.setDetails("Database import completed successfully");
        return response;
    }
    
    /**
     * Create a failure response
     */
    public static DatabaseBackupResponseDTO failure(String operationType, String initiatedBy, String errorDetails) {
        DatabaseBackupResponseDTO response = new DatabaseBackupResponseDTO();
        response.setOperationId(java.util.UUID.randomUUID().toString());
        response.setOperationType(operationType);
        response.setStatus("FAILED");
        response.setTimestamp(LocalDateTime.now());
        response.setInitiatedBy(initiatedBy);
        response.setDetails(errorDetails);
        return response;
    }

    /**
     * Create an in-progress response
     */
    public static DatabaseBackupResponseDTO inProgress(String operationId, String operationType, String initiatedBy) {
        DatabaseBackupResponseDTO response = new DatabaseBackupResponseDTO();
        response.setOperationId(operationId);
        response.setOperationType(operationType);
        response.setStatus("IN_PROGRESS");
        response.setTimestamp(LocalDateTime.now());
        response.setInitiatedBy(initiatedBy);
        response.setDetails("Operation is in progress");
        return response;
    }
}
