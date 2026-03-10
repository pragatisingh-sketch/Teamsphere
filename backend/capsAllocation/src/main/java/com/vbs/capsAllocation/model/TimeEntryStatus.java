package com.vbs.capsAllocation.model;

public enum TimeEntryStatus {
    PENDING,
    APPROVED,
    REJECTED,
    SUBMITTED // For auto-generated entries (from leave requests) that need approval
}
