package com.vbs.capsAllocation.dto;

import java.util.List;

/**
 * DTO for requesting time-entry reminder emails.
 * Supports both individual and bulk reminder sending.
 */
public class TimeEntryReminderRequestDTO {

    private List<String> recipientLdaps; // LDAPs of users to send reminders
    private String customMessage; // Optional custom message (null = default)
    private boolean bulk; // true for bulk send
    private List<String> missingPeriods; // Optional: specific periods to mention in email
    private List<String> missingDays; // Optional: specific missing dates
    private boolean wholeWeekMissing; // Optional: boolean to indicate if whole week is missing

    // Default constructor
    public TimeEntryReminderRequestDTO() {
    }

    // Full constructor
    public TimeEntryReminderRequestDTO(List<String> recipientLdaps, String customMessage,
            boolean bulk, List<String> missingPeriods, List<String> missingDays, boolean wholeWeekMissing) {
        this.recipientLdaps = recipientLdaps;
        this.customMessage = customMessage;
        this.bulk = bulk;
        this.missingPeriods = missingPeriods;
        this.missingDays = missingDays;
        this.wholeWeekMissing = wholeWeekMissing;
    }

    // Getters and Setters
    public List<String> getRecipientLdaps() {
        return recipientLdaps;
    }

    public void setRecipientLdaps(List<String> recipientLdaps) {
        this.recipientLdaps = recipientLdaps;
    }

    public String getCustomMessage() {
        return customMessage;
    }

    public void setCustomMessage(String customMessage) {
        this.customMessage = customMessage;
    }

    public boolean isBulk() {
        return bulk;
    }

    public void setBulk(boolean bulk) {
        this.bulk = bulk;
    }

    public List<String> getMissingPeriods() {
        return missingPeriods;
    }

    public void setMissingPeriods(List<String> missingPeriods) {
        this.missingPeriods = missingPeriods;
    }

    public List<String> getMissingDays() {
        return missingDays;
    }

    public void setMissingDays(List<String> missingDays) {
        this.missingDays = missingDays;
    }

    public boolean isWholeWeekMissing() {
        return wholeWeekMissing;
    }

    public void setWholeWeekMissing(boolean wholeWeekMissing) {
        this.wholeWeekMissing = wholeWeekMissing;
    }
}
