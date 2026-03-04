package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for database backup import requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseBackupRequestDTO {
    
    /**
     * Optional Google Drive file ID to import from a specific backup
     * If null, imports from the most recent backup
     */
    private String fileId;
}
