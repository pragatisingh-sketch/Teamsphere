package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * DTO for export requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequest {

    /**
     * Type of export (e.g., TIME_ENTRY_DEFAULTER, TIME_ENTRY_COMPLIANCE)
     */
    private String type;

    /**
     * Start date for data range
     */
    private LocalDate startDate;

    /**
     * End date for data range
     */
    private LocalDate endDate;

    /**
     * Optional filters (team, manager, etc.)
     */
    private Map<String, Object> filters;
}
