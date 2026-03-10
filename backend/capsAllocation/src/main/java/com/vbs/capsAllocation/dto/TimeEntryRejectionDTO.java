package com.vbs.capsAllocation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntryRejectionDTO {
    @NotNull(message = "Time entry ID is required")
    private Long timeEntryId;
    
    @NotBlank(message = "Rejection comment is required")
    private String rejectionComment;
}
