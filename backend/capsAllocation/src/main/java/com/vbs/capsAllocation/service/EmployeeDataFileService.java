package com.vbs.capsAllocation.service;

import java.util.Optional;

/**
 * Service interface for managing employee data in JSON file storage
 */
public interface EmployeeDataFileService {
    
    /**
     * Store employee data in JSON file and return a unique key
     * @param employeeData The employee data as JSON string
     * @return Unique key to retrieve the data later
     */
    String storeEmployeeData(String employeeData);

    /**
     * Store employee data with original data for comparison
     * @param employeeData The new/modified employee data as JSON string
     * @param originalData The original employee data as JSON string (for comparison)
     * @return Unique key to retrieve the data later
     */
    String storeEmployeeDataWithOriginal(String employeeData, String originalData);
    
    /**
     * Retrieve employee data by key
     * @param key The unique key for the employee data
     * @return Optional containing the employee data JSON string if found
     */
    Optional<String> getEmployeeData(String key);

    /**
     * Retrieve original employee data by key (for comparison)
     * @param key The unique key for the employee data
     * @return Optional containing the original employee data JSON string if found
     */
    Optional<String> getOriginalEmployeeData(String key);
    
    /**
     * Remove employee data by key
     * @param key The unique key for the employee data
     * @return true if data was removed, false if key not found
     */
    boolean removeEmployeeData(String key);
    
    /**
     * Check if employee data exists for the given key
     * @param key The unique key for the employee data
     * @return true if data exists, false otherwise
     */
    boolean existsEmployeeData(String key);
    
    /**
     * Initialize the JSON file if it doesn't exist
     */
    void initializeDataFile();

    /**
     * Get all stored employee data keys (for maintenance/cleanup purposes)
     * @return Set of all keys in the storage
     */
    java.util.Set<String> getAllKeys();

    /**
     * Clean up old employee data entries older than specified days
     * @param daysOld Number of days - entries older than this will be removed
     * @return Number of entries removed
     */
    int cleanupOldData(int daysOld);
}
