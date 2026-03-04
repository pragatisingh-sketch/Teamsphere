package com.vbs.capsAllocation.util;

import com.vbs.capsAllocation.model.LeadsRequest;
import com.vbs.capsAllocation.repository.LeadRepository;
import com.vbs.capsAllocation.service.EmployeeDataFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Utility class for migrating existing employee data from blob storage to JSON file storage
 */
@Component
public class EmployeeDataMigrationUtil {

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private EmployeeDataFileService employeeDataFileService;

    /**
     * Migrate existing employee data from blob storage to JSON file storage
     * This method should be called once to migrate existing data
     */
    public void migrateExistingData() {
        LoggerUtil.logInfo(EmployeeDataMigrationUtil.class, "Starting migration of existing employee data from blob to JSON file storage");
        
        try {
            // Get all leads requests that might have blob data
            List<LeadsRequest> allRequests = leadRepository.findAll();
            int migratedCount = 0;
            int skippedCount = 0;
            
            for (LeadsRequest request : allRequests) {
                try {
                    // Check if request already has a data key (already migrated)
                    if (request.getEmployeeDataKey() != null) {
                        skippedCount++;
                        continue;
                    }
                    
                    // For this migration, we'll need to manually provide the employee data
                    // since the blob field has been removed from the model
                    LoggerUtil.logInfo(EmployeeDataMigrationUtil.class, "Request ID {} needs manual data migration", request.getId());
                    skippedCount++;
                    
                } catch (Exception e) {
                    LoggerUtil.logError("Error migrating request ID {}: {}", request.getId(), e.getMessage(), e);
                }
            }
            
            LoggerUtil.logInfo(EmployeeDataMigrationUtil.class, "Migration completed. Migrated: {}, Skipped: {}", migratedCount, skippedCount);
            
        } catch (Exception e) {
            LoggerUtil.logError("Error during migration: {}", e.getMessage(), e);
            throw new RuntimeException("Migration failed", e);
        }
    }
    
    /**
     * Manually migrate a specific request with provided employee data
     * This method can be used to migrate individual requests when you have the employee data
     */
    public void migrateSpecificRequest(Long requestId, String employeeDataJson) {
        try {
            LeadsRequest request = leadRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found: " + requestId));
            
            if (request.getEmployeeDataKey() != null) {
                LoggerUtil.logInfo(EmployeeDataMigrationUtil.class, "Request ID {} already has employee data key: {}", requestId, request.getEmployeeDataKey());
                return;
            }
            
            // Store the employee data in file and get the key
            String employeeDataKey = employeeDataFileService.storeEmployeeData(employeeDataJson);
            
            // Update the request with the new key
            request.setEmployeeDataKey(employeeDataKey);
            leadRepository.save(request);
            
            LoggerUtil.logInfo(EmployeeDataMigrationUtil.class, "Successfully migrated request ID {} with key: {}", requestId, employeeDataKey);
            
        } catch (Exception e) {
            LoggerUtil.logError("Error migrating specific request ID {}: {}", requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to migrate request: " + requestId, e);
        }
    }
    
    /**
     * Bulk migrate multiple requests with provided data
     * Expected format: Map<Long, String> where key is requestId and value is employeeDataJson
     */
    public void bulkMigrateRequests(java.util.Map<Long, String> requestDataMap) {
        LoggerUtil.logInfo(EmployeeDataMigrationUtil.class, "Starting bulk migration for {} requests", requestDataMap.size());
        
        int successCount = 0;
        int failureCount = 0;
        
        for (java.util.Map.Entry<Long, String> entry : requestDataMap.entrySet()) {
            try {
                migrateSpecificRequest(entry.getKey(), entry.getValue());
                successCount++;
            } catch (Exception e) {
                LoggerUtil.logError("Failed to migrate request ID {}: {}", entry.getKey(), e.getMessage(), e);
                failureCount++;
            }
        }
        
        LoggerUtil.logInfo(EmployeeDataMigrationUtil.class, "Bulk migration completed. Success: {}, Failures: {}", successCount, failureCount);
    }
}
