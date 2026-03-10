package com.vbs.capsAllocation.audit;

public record AuditStatus(
        String previousStatus,
        String newStatus
) {}
