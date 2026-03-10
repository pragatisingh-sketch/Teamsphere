package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VunnoHistoryDto {

    private Long id;
    private String status;
    private String requestorName;
    private String applicationType;
    private String leaveType;
    private String leaveDetails;
    private String leaveCategory;
    private String startDate;
    private String endDate;
    private String duration;
    private String reason;
    private String backupInfo;
    private String timesheetProof;
    private String oooProof;
    private String approver;
    private String documentPath;
    private String startTime;
    private String endTime;
    private String shiftCodeAtRequestTime;
    private String timestamp;

}
