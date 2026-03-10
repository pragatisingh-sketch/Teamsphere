package com.vbs.capsAllocation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for batch creation of time entries
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchTimeEntryDTO {
    @NotNull(message = "Source time entry is required")
    @Valid
    private CreateTimeEntryDTO sourceEntry;
    
    @NotEmpty(message = "Target dates are required")
    private List<String> targetDates;
}
