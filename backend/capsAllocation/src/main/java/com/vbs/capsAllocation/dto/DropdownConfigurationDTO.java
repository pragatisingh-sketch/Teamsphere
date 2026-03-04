package com.vbs.capsAllocation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for returning dropdown configuration data to the frontend
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DropdownConfigurationDTO {

    private Long id;
    private String dropdownType;
    private String optionValue;
    private String displayName;
    private Boolean isActive;
    private Integer sortOrder;
    private String createdBy;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
