package com.vbs.capsAllocation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VunnoRequestDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("ldap")
    private String ldap;

    @JsonProperty("applicationType")
    private String applicationType;

    @JsonProperty("leaveType")
    private String leaveType;

    @JsonProperty("lvWfhDuration")
    private String lvWfhDuration;

    @JsonProperty("startDate")
    private String startDate;

    @JsonProperty("endDate")
    private String endDate;

    private String startDateTime;
    private String endDateTime;

    @JsonProperty("backupInfo")
    private String backupInfo;

    @JsonProperty("oooProof")
    private String oooProof;

    @JsonProperty("timesheetProof")
    private String timesheetProof;

    @JsonProperty("status")
    private String status;

    @JsonProperty("requestorName")
    private String requestorName;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("document")
    private String documentPath;

    @JsonProperty("role")
    private String role;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    private String approvingLead;

    private String leaveCategory;
    private String comment;

    // For mixed leave type requests: per-day breakdown with leave type for each day
    @JsonProperty("dayConfigurations")
    private List<DayConfigurationDto> dayConfigurations;

    private String actionType;
    private LocalDateTime changedAt;
    private String changedBy;

    public VunnoRequestDto(
            Long id,
            String ldap,
            String approvingLead,
            String applicationType,
            String leaveType,
            String lvWfhDuration,
            LocalDate startDate,
            LocalDate endDate,
            String backupInfo,
            String oooProof,
            String timesheetProof,
            String status,
            String requestorName,
            String reason,
            String documentPath,
            LocalDateTime timestamp,
            String leaveCategory,
            String actionType,
            LocalDateTime changedAt,
            String changedBy,
            String dayConfigurationsJson) {
        this.id = id;
        this.ldap = ldap;
        this.approvingLead = approvingLead != null ? approvingLead : "";
        this.applicationType = applicationType;
        this.leaveType = leaveType;
        if ("Mixed".equalsIgnoreCase(this.leaveType)) {
            this.leaveType = this.leaveType + formatMixedDetails(dayConfigurationsJson);
        } else if (dayConfigurationsJson != null && !dayConfigurationsJson.isEmpty()) {
            // For single leave type, show total days
            this.leaveType = this.leaveType + formatSingleTypeDetails(dayConfigurationsJson);
        }
        this.lvWfhDuration = lvWfhDuration;
        this.startDate = startDate != null ? startDate.toString() : null;
        this.endDate = endDate != null ? endDate.toString() : null;
        this.backupInfo = backupInfo;
        this.oooProof = oooProof;
        this.timesheetProof = timesheetProof;
        this.status = status;
        this.requestorName = requestorName;
        this.reason = reason;
        this.documentPath = documentPath;
        this.timestamp = timestamp;
        this.leaveCategory = leaveCategory;
        this.actionType = actionType;
        this.changedAt = changedAt;
        this.changedBy = changedBy;

        // CRITICAL: Parse and populate dayConfigurations List field
        // This fixes: "Multiple Days" bug and email balance calculation bug
        if (dayConfigurationsJson != null && !dayConfigurationsJson.isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                this.dayConfigurations = mapper.readValue(
                        dayConfigurationsJson,
                        new TypeReference<List<DayConfigurationDto>>() {
                        });
            } catch (Exception e) {
                // Parsing failed - initialize as empty to avoid null pointer
                this.dayConfigurations = new ArrayList<>();
                System.err.println("Failed to parse dayConfigurations JSON: " + e.getMessage());
            }
        } else {
            this.dayConfigurations = new ArrayList<>();
        }
    }

    public VunnoRequestDto(
            Long id,
            String ldap,
            String approvingLead,
            String applicationType,
            String leaveType,
            String lvWfhDuration,
            LocalDate startDate,
            LocalDate endDate,
            String backupInfo,
            String oooProof,
            String timesheetProof,
            String status,
            String requestorName,
            String reason,
            String documentPath,
            LocalDateTime timestamp,
            String leaveCategory,
            String dayConfigurationsJson) {
        this.id = id;
        this.ldap = ldap;
        this.approvingLead = approvingLead != null ? approvingLead : "";
        this.applicationType = applicationType;
        this.leaveType = leaveType;
        if ("Mixed".equalsIgnoreCase(this.leaveType)) {
            this.leaveType = this.leaveType + formatMixedDetails(dayConfigurationsJson);
        } else if (dayConfigurationsJson != null && !dayConfigurationsJson.isEmpty()) {
            // For single leave type, show total days
            this.leaveType = this.leaveType + formatSingleTypeDetails(dayConfigurationsJson);
        }
        this.lvWfhDuration = lvWfhDuration;
        this.startDate = startDate != null ? startDate.toString() : null;
        this.endDate = endDate != null ? endDate.toString() : null;
        this.backupInfo = backupInfo;
        this.oooProof = oooProof;
        this.timesheetProof = timesheetProof;
        this.status = status;
        this.requestorName = requestorName;
        this.reason = reason;
        this.documentPath = documentPath;
        this.timestamp = timestamp;
        this.leaveCategory = leaveCategory;

        // CRITICAL: Parse and populate dayConfigurations List field
        // This fixes: "Multiple Days" bug and email balance calculation bug
        if (dayConfigurationsJson != null && !dayConfigurationsJson.isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                this.dayConfigurations = mapper.readValue(
                        dayConfigurationsJson,
                        new TypeReference<List<DayConfigurationDto>>() {
                        });
            } catch (Exception e) {
                // Parsing failed - initialize as empty to avoid null pointer
                this.dayConfigurations = new ArrayList<>();
                System.err.println("Failed to parse dayConfigurations JSON: " + e.getMessage());
            }
        } else {
            this.dayConfigurations = new ArrayList<>();
        }
    }

    public static String formatMixedDetails(String json) {
        if (json == null || json.isEmpty())
            return "";
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<DayConfigurationDto> configs = mapper.readValue(json, new TypeReference<List<DayConfigurationDto>>() {
            });

            // Map: Leave Type -> Total Days
            Map<String, Double> counts = new HashMap<>();
            for (DayConfigurationDto dto : configs) {
                // Determine leave type key (SL, CL, EL)
                String key = dto.getLeaveType();
                if (key.contains("Sick"))
                    key = "SL";
                else if (key.contains("Casual"))
                    key = "CL";
                else if (key.contains("Earned"))
                    key = "EL";

                counts.put(key, counts.getOrDefault(key, 0.0) + dto.getDurationValue());
            }

            String details = counts.entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining(", "));

            return " (" + details + ")";
        } catch (Exception e) {
            return "";
        }
    }

    public static String formatMixedDetails(List<DayConfigurationDto> configs) {
        if (configs == null || configs.isEmpty())
            return "";
        try {
            // Map: Leave Type -> Total Days
            Map<String, Double> counts = new HashMap<>();
            for (DayConfigurationDto dto : configs) {
                // Determine leave type key (SL, CL, EL)
                String key = dto.getLeaveType();
                if (key == null)
                    continue; // Safety check

                if (key.contains("Sick"))
                    key = "SL";
                else if (key.contains("Casual"))
                    key = "CL";
                else if (key.contains("Earned"))
                    key = "EL";

                counts.put(key, counts.getOrDefault(key, 0.0) + dto.getDurationValue());
            }

            String details = counts.entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining(", "));

            return " (" + details + ")";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Format single leave type with total days
     * e.g., "Casual Leave" becomes "Casual Leave (1.5)"
     */
    public static String formatSingleTypeDetails(String json) {
        if (json == null || json.isEmpty())
            return "";
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<DayConfigurationDto> configs = mapper.readValue(json, new TypeReference<List<DayConfigurationDto>>() {
            });

            // Calculate total days
            double totalDays = 0.0;
            for (DayConfigurationDto dto : configs) {
                totalDays += dto.getDurationValue();
            }

            return " (" + totalDays + ")";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Format single leave type with total days (List version)
     * e.g., "Casual Leave" becomes "Casual Leave (1.5)"
     */
    public static String formatSingleTypeDetails(List<DayConfigurationDto> configs) {
        if (configs == null || configs.isEmpty())
            return "";
        try {
            // Calculate total days
            double totalDays = 0.0;
            for (DayConfigurationDto dto : configs) {
                totalDays += dto.getDurationValue();
            }

            return " (" + totalDays + ")";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Computed property for leave details display
     * No database column needed - computed from dayConfigurations JSON
     */
    @JsonProperty("leaveDetails")
    public String getLeaveDetails() {
        return formatLeaveDetailsFromDayConfigs(this.dayConfigurations, this.lvWfhDuration);
    }

    /**
     * Format leave details from day configurations
     * Shows total days and day-by-day breakdown with AM/PM info
     */
    public static String formatLeaveDetailsFromDayConfigs(
            List<DayConfigurationDto> configs,
            String durationFallback) {

        if (configs == null || configs.isEmpty()) {
            // Fallback for old records without day configurations
            return inferFromDurationType(durationFallback);
        }

        // Calculate total
        double total = configs.stream()
                .mapToDouble(DayConfigurationDto::getDurationValue)
                .sum();

        // Format day-by-day with smart grouping
        String dayBreakdown = formatDayBreakdown(configs);

        return String.format("%.1f day%s (%s)",
                total,
                total == 1.0 ? "" : "s",
                dayBreakdown);
    }

    /**
     * Fallback for old records without day configurations
     * For old records, lvWfhDuration is just a number (e.g., "2.0", "1.5")
     * We can't infer the day-by-day breakdown, so just show total
     */
    private static String inferFromDurationType(String durationType) {
        if (durationType == null || durationType.trim().isEmpty()) {
            return "";
        }

        // Try to parse as a number (for old records)
        try {
            double value = Double.parseDouble(durationType.trim());
            return String.format("%.1f day%s", value, value == 1.0 ? "" : "s");
        } catch (NumberFormatException e) {
            // Not a number, might be an old label format
            switch (durationType) {
                case "Full Day":
                    return "1.0 day (Full)";
                case "Half Day - Morning":
                case "Half Day AM":
                    return "0.5 days (AM)";
                case "Half Day - Afternoon":
                case "Half Day PM":
                    return "0.5 days (PM)";
                case "Multiple Days":
                    return durationType;
                default:
                    return durationType;
            }
        }
    }

    /**
     * Format day-by-day breakdown with smart grouping
     * Example: "Feb 12: AM, Feb 13-15: Full, Feb 16: PM"
     */
    private static String formatDayBreakdown(List<DayConfigurationDto> configs) {
        if (configs.isEmpty())
            return "";

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < configs.size(); i++) {
            DayConfigurationDto config = configs.get(i);
            LocalDate date = LocalDate.parse(config.getDate());
            String durationType = extractDurationType(config.getDuration());

            // Check if we can group consecutive full days
            if ("Full".equals(durationType)) {
                int endIndex = findConsecutiveFullDays(configs, i);
                if (endIndex > i) {
                    // Group: "Feb 12-15: Full"
                    LocalDate endDate = LocalDate.parse(configs.get(endIndex).getDate());
                    sb.append(formatDateRange(date, endDate)).append(": Full");
                    i = endIndex; // Skip grouped items
                } else {
                    sb.append(formatDate(date)).append(": Full");
                }
            } else {
                sb.append(formatDate(date)).append(": ").append(durationType);
            }

            if (i < configs.size() - 1)
                sb.append(", ");
        }

        return sb.toString();
    }

    /**
     * Extract duration type from duration label
     */
    private static String extractDurationType(String durationLabel) {
        if (durationLabel == null)
            return "Full";

        // Handle new "First Half" and "Second Half" labels (return descriptive labels)
        if (durationLabel.contains("First Half"))
            return "First Half";
        if (durationLabel.contains("Second Half"))
            return "Second Half";

        // Handle legacy "AM"/"PM" labels
        if (durationLabel.contains("AM"))
            return "AM";
        if (durationLabel.contains("PM"))
            return "PM";

        return "Full";
    }

    /**
     * Find consecutive full days for grouping
     * Returns the index of the last consecutive full day
     */
    private static int findConsecutiveFullDays(List<DayConfigurationDto> configs, int startIndex) {
        int endIndex = startIndex;
        LocalDate currentDate = LocalDate.parse(configs.get(startIndex).getDate());

        for (int i = startIndex + 1; i < configs.size(); i++) {
            DayConfigurationDto nextConfig = configs.get(i);
            LocalDate nextDate = LocalDate.parse(nextConfig.getDate());
            String durationType = extractDurationType(nextConfig.getDuration());

            // Check if next day is consecutive and full
            if ("Full".equals(durationType) && nextDate.equals(currentDate.plusDays(1))) {
                endIndex = i;
                currentDate = nextDate;
            } else {
                break;
            }
        }

        return endIndex;
    }

    /**
     * Format single date (e.g., "Feb 12")
     */
    private static String formatDate(LocalDate date) {
        return date.getMonth().toString().substring(0, 3) + " " + date.getDayOfMonth();
    }

    /**
     * Format date range (e.g., "Feb 12-15")
     */
    private static String formatDateRange(LocalDate start, LocalDate end) {
        if (start.getMonth().equals(end.getMonth())) {
            // Same month: "Feb 12-15"
            return formatDate(start) + "-" + end.getDayOfMonth();
        } else {
            // Different months: "Feb 28-Mar 2"
            return formatDate(start) + "-" + formatDate(end);
        }
    }
}
