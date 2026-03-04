package com.vbs.capsAllocation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignProjectDTO {
    @NotNull(message = "Project ID is required")
    private Long projectId;
    
    @NotEmpty(message = "At least one user ID must be provided")
    private List<Long> userIds;
    private LocalDate assignedDate;
    private String status;
}
