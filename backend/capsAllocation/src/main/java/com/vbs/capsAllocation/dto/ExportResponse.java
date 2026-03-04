package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for export response containing file data and metadata
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportResponse {

    /**
     * Export file data as byte array
     */
    private byte[] fileData;

    /**
     * Filename for the export
     */
    private String filename;

    /**
     * Content type (e.g.,
     * "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
     */
    private String contentType;
}
