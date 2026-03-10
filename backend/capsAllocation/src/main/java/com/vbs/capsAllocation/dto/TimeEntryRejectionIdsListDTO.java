package com.vbs.capsAllocation.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntryRejectionIdsListDTO {

    @NotNull(message = "Time entry IDs are required")
    private List<Long> timeEntryId;

    @NotBlank(message = "Rejection comment is required")
    private String rejectionComment;
} 