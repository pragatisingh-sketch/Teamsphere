package com.vbs.capsAllocation.service.export;

import java.time.LocalDate;
import java.util.Map;

/**
 * Strategy interface for generating exports in different formats.
 * Each implementation represents a different type of export (e.g.,
 * TIME_ENTRY_DEFAULTER, TIME_ENTRY_COMPLIANCE).
 */
public interface ExportStrategy {

    /**
     * Generate an Excel file as a byte array
     * 
     * @param startDate Start date for the export data range
     * @param endDate   End date for the export data range
     * @param userName  Username of the logged-in user (for role-based filtering)
     * @param filters   Optional filters (team, manager, etc.)
     * @return Excel file as byte array
     */
    byte[] generateExcel(LocalDate startDate, LocalDate endDate, String userName, Map<String, Object> filters);

    /**
     * Get the export type identifier for this strategy
     * 
     * @return Export type (e.g., "TIME_ENTRY_DEFAULTER", "TIME_ENTRY_COMPLIANCE")
     */
    String getExportType();
}
