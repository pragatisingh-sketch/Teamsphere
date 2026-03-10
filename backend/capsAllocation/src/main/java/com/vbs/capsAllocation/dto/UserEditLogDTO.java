package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEditLogDTO {
    private Long id;
    private String userLdap;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private String changedBy;
    private Timestamp changedAt;
}
