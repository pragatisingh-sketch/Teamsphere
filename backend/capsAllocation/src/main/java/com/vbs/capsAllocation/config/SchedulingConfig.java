package com.vbs.capsAllocation.config;

import com.vbs.capsAllocation.service.DatabaseBackupService;
import com.vbs.capsAllocation.service.EarnedLeaveAccrualService;
import com.vbs.capsAllocation.service.TokenBlacklistService;
import com.vbs.capsAllocation.dto.DatabaseBackupResponseDTO;
import com.vbs.capsAllocation.util.LoggerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configuration for scheduled tasks in the application
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

    private final TokenBlacklistService tokenBlacklistService;
    private final DatabaseBackupService databaseBackupService;
    private final EarnedLeaveAccrualService earnedLeaveAccrualService;

    @Autowired
    public SchedulingConfig(TokenBlacklistService tokenBlacklistService,
            DatabaseBackupService databaseBackupService,
            EarnedLeaveAccrualService earnedLeaveAccrualService) {
        this.tokenBlacklistService = tokenBlacklistService;
        this.databaseBackupService = databaseBackupService;
        this.earnedLeaveAccrualService = earnedLeaveAccrualService;
    }

    /**
     * Clean up expired tokens from the blacklist every hour
     * This prevents memory leaks from accumulating blacklisted tokens
     */
    @Scheduled(fixedRate = 3600000) // Run every hour (3600000 ms)
    public void cleanupExpiredTokens() {
        int removedCount = tokenBlacklistService.cleanupExpiredTokens();
        LoggerUtil.logDebug("Token blacklist cleanup: Removed {} expired tokens", removedCount);
    }

    /**
     * Weekly database backup task
     * Runs every Sunday at 1:00 AM
     */
    // @Scheduled(cron = "0 * * * * *") // Run every 1 minute
    @Scheduled(cron = "0 0 1 * * SUN") // Run at 1:00 AM every Sunday
    public void weeklyDatabaseBackup() {
        LoggerUtil.logDebug("Starting weekly database backup...");
        try {
            DatabaseBackupResponseDTO response = databaseBackupService.createAndUploadBackup("scheduled-task");
            if ("SUCCESS".equals(response.getStatus())) {
                LoggerUtil.logDebug("Weekly database backup completed successfully. Operation ID: {}, File ID: {}",
                        response.getOperationId(), response.getFileId());
            } else {
                LoggerUtil.logError("Weekly database backup failed: {}", response.getDetails());
            }
        } catch (Exception e) {
            LoggerUtil.logError("Error during weekly database backup: {}", e.getMessage(), e);
        }
    }

    /**
     * Monthly Earned Leave (EL) accrual task.
     * Runs at midnight IST on the 1st of every month.
     * Credits {@value EarnedLeaveAccrualService#MONTHLY_EL_DAYS} EL days
     * to every active employee (source = "SYSTEM").
     * The job is idempotent — employees who already have an HR-EL record
     * for the current month/year are automatically skipped.
     */
    // @Scheduled(cron = "0 * * * * *")
    @Scheduled(cron = "0 0 0 1 * *", zone = "Asia/Kolkata")
    public void monthlyELAccrual() {
        LoggerUtil.logDebug("Starting monthly EL accrual scheduled task...");
        try {
            earnedLeaveAccrualService.accrueMonthlyEL();
            LoggerUtil.logDebug("Monthly EL accrual scheduled task completed successfully.");
        } catch (Exception e) {
            LoggerUtil.logError("Error during monthly EL accrual: {}", e.getMessage(), e);
        }
    }
}
