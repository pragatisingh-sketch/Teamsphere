package com.vbs.capsAllocation.controller;

import com.vbs.capsAllocation.dto.BaseResponse;
import com.vbs.capsAllocation.dto.DatabaseBackupRequestDTO;
import com.vbs.capsAllocation.dto.DatabaseBackupResponseDTO;
import com.vbs.capsAllocation.service.DatabaseBackupService;
import com.vbs.capsAllocation.util.LoggerUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing database backup operations in the Time Sheet System
 *
 * @author Piyush Mishra
 * @version 1.0
 */
@RestController
@RequestMapping("/api/database")
public class DatabaseBackupController {

    @Autowired
    private DatabaseBackupService databaseBackupService;

    @org.springframework.beans.factory.annotation.Value("${app.features.backup-enabled:false}")
    private boolean backupEnabled;

    public DatabaseBackupController(DatabaseBackupService databaseBackupService) {
        this.databaseBackupService = databaseBackupService;
    }

    /**
     * Endpoint to manually trigger a database backup
     * Only accessible to users with ADMIN_OPS_MANAGER role
     * 
     * @param userDetails The authenticated user details
     * @return ResponseEntity with success or error message
     */
    @PostMapping("/backup")
    @PreAuthorize("hasRole('ADMIN_OPS_MANAGER')")
    public ResponseEntity<BaseResponse<DatabaseBackupResponseDTO>> triggerBackup(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (!backupEnabled) {
            LoggerUtil.logError("Backup attempted by {} but backup feature is disabled.", userDetails.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.error("Database backup feature is disabled in this environment.",
                            HttpStatus.FORBIDDEN.value()));
        }

        try {
            LoggerUtil.logDebug("Manual database backup triggered by user: {}", userDetails.getUsername());

            DatabaseBackupResponseDTO backupResult = databaseBackupService
                    .createAndUploadBackup(userDetails.getUsername());

            // Check the actual operation status and respond accordingly
            if ("SUCCESS".equals(backupResult.getStatus())) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(BaseResponse.success("Database backup completed successfully", backupResult,
                                HttpStatus.CREATED.value()));
            } else {
                // Operation failed - return error response with details from the service
                String errorMessage = backupResult.getDetails() != null ? backupResult.getDetails()
                        : "Database backup failed";
                LoggerUtil.logError("Database backup failed for user {}: {}", userDetails.getUsername(), errorMessage);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(BaseResponse.error("Database backup failed: " + errorMessage, backupResult,
                                HttpStatus.INTERNAL_SERVER_ERROR.value()));
            }
        } catch (Exception e) {
            LoggerUtil.logError("Error during manual database backup: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to create database backup: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Endpoint to import data from a backup (asynchronous)
     * Only accessible to users with ADMIN_OPS_MANAGER role
     * 
     * @param userDetails The authenticated user details
     * @param requestDTO  Optional request body containing fileId to specify which
     *                    backup to import
     * @return ResponseEntity with operation ID for tracking progress
     */
    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN_OPS_MANAGER')")
    public ResponseEntity<BaseResponse<DatabaseBackupResponseDTO>> importFromBackup(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody(required = false) DatabaseBackupRequestDTO requestDTO) {

        if (!backupEnabled) {
            LoggerUtil.logError("Database import attempted by {} but backup feature is disabled.",
                    userDetails.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.error("Database import feature is disabled in this environment.", null,
                            HttpStatus.FORBIDDEN.value()));
        }

        try {
            String fileId = (requestDTO != null) ? requestDTO.getFileId() : null;

            if (fileId != null) {
                LoggerUtil.logDebug("Database import from specific backup triggered by user: {} with fileId: {}",
                        userDetails.getUsername(), fileId);
            } else {
                LoggerUtil.logDebug("Database import from most recent backup triggered by user: {}",
                        userDetails.getUsername());
            }

            // Start asynchronous import and return operation ID immediately
            DatabaseBackupResponseDTO importResult = databaseBackupService.startAsyncImportFromBackup(fileId,
                    userDetails.getUsername());

            return ResponseEntity.ok(BaseResponse.success(
                    "Database import started successfully. Use the operation ID to check status.", importResult));

        } catch (Exception e) {
            LoggerUtil.logError("Error starting database import for user {}: {}", userDetails.getUsername(),
                    e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to start database import: " + e.getMessage(), null,
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Endpoint to check the status of a database operation
     * Only accessible to users with ADMIN_OPS_MANAGER role
     * 
     * @param operationId The operation ID to check status for
     * @param userDetails The authenticated user details
     * @return ResponseEntity with operation status
     */
    @GetMapping("/status/{operationId}")
    @PreAuthorize("hasRole('ADMIN_OPS_MANAGER')")
    public ResponseEntity<BaseResponse<DatabaseBackupResponseDTO>> getOperationStatus(
            @PathVariable String operationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            LoggerUtil.logDebug("Operation status check requested by user: {} for operation: {}",
                    userDetails.getUsername(), operationId);

            DatabaseBackupResponseDTO statusResult = databaseBackupService.getOperationStatus(operationId);

            if (statusResult != null) {
                return ResponseEntity.ok(BaseResponse.success("Operation status retrieved successfully", statusResult));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(BaseResponse.error("Operation not found", null, HttpStatus.NOT_FOUND.value()));
            }

        } catch (Exception e) {
            LoggerUtil.logError("Error retrieving operation status for user {}: {}", userDetails.getUsername(),
                    e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to retrieve operation status: " + e.getMessage(), null,
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}
