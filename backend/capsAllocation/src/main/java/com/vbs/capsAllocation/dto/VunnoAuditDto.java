package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VunnoAuditDto {

    private Long id;
    private Long vunnoResponseId;
    private String actionType;
    private String previousStatus;
    private String newStatus;
    private String changedBy;
    private String changedByRole;
    private LocalDateTime changedAt;
    private String changeReason;
    private String changeDescription;
    private String previousValues;
    private String newValues;

}
