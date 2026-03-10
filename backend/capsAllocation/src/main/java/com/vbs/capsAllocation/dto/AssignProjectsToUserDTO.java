package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignProjectsToUserDTO {
    private Long userId;
    private List<Long> projectIds;
    private LocalDate assignedDate;
    private String status;
}