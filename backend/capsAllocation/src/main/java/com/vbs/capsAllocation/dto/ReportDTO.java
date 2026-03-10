package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Generic DTO for reports
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {
    private Long id;
    private String type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String status;
    private String description;
    
    // Additional fields can be added later as needed
}