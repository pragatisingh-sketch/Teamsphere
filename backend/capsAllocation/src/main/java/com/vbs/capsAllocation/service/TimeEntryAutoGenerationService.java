package com.vbs.capsAllocation.service;

import com.vbs.capsAllocation.model.*;
import com.vbs.capsAllocation.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for auto-generating time entries when leave requests are submitted
 * ONLY for Leave requests, NOT for WFH/CompOff
 */
@Service
public class TimeEntryAutoGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(TimeEntryAutoGenerationService.class);

    @Autowired
    private TimeEntryRepository timeEntryRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProjectRepository userProjectRepository;

    @Autowired
    private HolidayRepository holidayRepository;

    /**
     * Generates time entries when leave is REQUESTED
     * Time entries are created in PENDING status and locked
     * ONLY for Leave requests, NOT for WFH/CompOff
     * 
     * @param leaveRequest - The leave request being submitted
     * @return List of created time entries
     */
    @Transactional
    public List<TimeEntry> generateTimeEntriesForLeave(VunnoResponse leaveRequest) {
        try {
            // Check if time entries should be generated
            if (!shouldGenerateTimeEntries(leaveRequest)) {
                logger.info("Skipping time entry generation for {} request", leaveRequest.getApplicationType());
                return new ArrayList<>();
            }

            logger.info("Generating time entries for leave request ID: {}", leaveRequest.getId());

            List<TimeEntry> entries = new ArrayList<>();

            // Get project ID for this user (from recent entries or mapping)
            Long projectId = getProjectIdForLeave(leaveRequest.getEmployee().getLdap());
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

            // Get user and lead
            User user = userRepository.findByUsername(leaveRequest.getEmployee().getLdap())
                    .orElseThrow(() -> new RuntimeException("User not found: " + leaveRequest.getEmployee().getLdap()));
            User lead = userRepository.findByUsername(leaveRequest.getApprover())
                    .orElse(null);

            // Check if day configurations are available (for mixed or multi-day requests)
            boolean hasDayConfigurations = leaveRequest.getDayConfigurations() != null
                    && !leaveRequest.getDayConfigurations().isEmpty();

            if (hasDayConfigurations) {
                // Handle using dayConfigurations (precise)
                entries = generateEntriesFromDayConfigurations(leaveRequest, project, user, lead);
            } else {
                // Handle regular leave type (all days same leave type)
                entries = generateEntriesFromDateRange(leaveRequest, project, user, lead);
            }

            // Save all entries
            List<TimeEntry> savedEntries = timeEntryRepository.saveAll(entries);
            logger.info("Successfully created {} time entries for leave request ID: {}",
                    savedEntries.size(), leaveRequest.getId());

            return savedEntries;

        } catch (Exception e) {
            logger.error("Error generating time entries for leave request ID: {}",
                    leaveRequest.getId(), e);
            throw new RuntimeException("Failed to generate time entries: " + e.getMessage(), e);
        }
    }

    /**
     * Generate time entries from dayConfigurations (for mixed leave types)
     */
    private List<TimeEntry> generateEntriesFromDayConfigurations(
            VunnoResponse leaveRequest, Project project, User user, User lead) {
        List<TimeEntry> entries = new ArrayList<>();

        try {
            // Parse dayConfigurations JSON
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            com.vbs.capsAllocation.dto.DayConfigurationDto[] dayConfigs = mapper.readValue(
                    leaveRequest.getDayConfigurations(),
                    com.vbs.capsAllocation.dto.DayConfigurationDto[].class);

            logger.info("Parsed {} day configurations for mixed leave type", dayConfigs.length);

            // Create one time entry per day using its specific configuration
            for (com.vbs.capsAllocation.dto.DayConfigurationDto dayConfig : dayConfigs) {
                LocalDate entryDate = LocalDate.parse(dayConfig.getDate());
                boolean isGoogleHoliday = isGoogleHoliday(entryDate);
                Optional<String> googleHolidayName = getGoogleHolidayName(entryDate);

                TimeEntry entry = new TimeEntry();
                entry.setUser(user);
                entry.setProject(project);
                entry.setEntryDate(entryDate);
                entry.setLdap(leaveRequest.getEmployee().getLdap());
                entry.setLead(lead);
                entry.setProcess(isGoogleHoliday ? "Holiday" : getProcessForTimeEntry(dayConfig.getLeaveType()));
                entry.setActivity(isGoogleHoliday ? Activity.HOLIDAY : Activity.ABSENTEEISM);
                entry.setTimeInMins(calculateTimeInMinutesFromDuration(dayConfig.getDurationValue()));
                entry.setAttendanceType(isGoogleHoliday ? getAttendanceTypeForGoogleHoliday(
                        leaveRequest.getShiftCodeAtRequestTime()) : getAttendanceTypeForDay(
                        leaveRequest.getShiftCodeAtRequestTime(),
                        dayConfig.getLeaveType(),
                        dayConfig.getDuration()));
                String comment = dayConfig.getLeaveType() + " - Auto Generated";
                if ("LWP".equalsIgnoreCase(dayConfig.getLeaveType())) {
                    comment = "LWP - Auto-Generated";
                }
                if (isGoogleHoliday) {
                    comment = "Google Holiday - "
                            + googleHolidayName.orElse("Holiday")
                            + " (Auto Generated)";
                }
                entry.setComment(comment);
                entry.setStatus(TimeEntryStatus.SUBMITTED); // Auto-generated entries need approval
                entry.setIsOvertime(false);
                entry.setIsDefaulter(false);

                // Set timestamps to match leave request timestamp
                entry.setCreatedAt(leaveRequest.getTimestamp());
                entry.setUpdatedAt(leaveRequest.getTimestamp());

                // Lock the entry
                entry.setIsLocked(true);
                entry.setLockedBy("SYSTEM");
                entry.setLockedAt(leaveRequest.getTimestamp());
                entry.setAutoGenerated(true);
                entry.setSourceLeaveId(leaveRequest.getId());

                entries.add(entry);
            }

        } catch (Exception e) {
            logger.error("Failed to parse day configurations", e);
            throw new RuntimeException("Failed to parse day configurations: " + e.getMessage(), e);
        }

        return entries;
    }

    /**
     * Generate time entries from date range (for regular single leave type)
     */
    private List<TimeEntry> generateEntriesFromDateRange(
            VunnoResponse leaveRequest, Project project, User user, User lead) {
        List<TimeEntry> entries = new ArrayList<>();

        // Calculate dates to generate entries for
        List<LocalDate> leaveDates = calculateLeaveDates(
                leaveRequest.getFromDate(),
                leaveRequest.getToDate(),
                leaveRequest.getDuration());

        logger.info("Creating {} time entries for dates: {}", leaveDates.size(), leaveDates);

        // For each leave date, create a time entry
        for (LocalDate date : leaveDates) {
            boolean isGoogleHoliday = isGoogleHoliday(date);
            Optional<String> googleHolidayName = getGoogleHolidayName(date);
            TimeEntry entry = new TimeEntry();
            entry.setUser(user);
            entry.setProject(project);
            entry.setEntryDate(date);
            entry.setLdap(leaveRequest.getEmployee().getLdap());
            entry.setLead(lead);
            entry.setProcess(isGoogleHoliday ? "Holiday" : getProcessForTimeEntry(leaveRequest.getLeaveType()));
            entry.setActivity(isGoogleHoliday ? Activity.HOLIDAY : Activity.ABSENTEEISM);
            entry.setTimeInMins(calculateTimeInMinutes(
                    leaveRequest.getShiftCodeAtRequestTime(),
                    leaveRequest.getDuration()));
            entry.setAttendanceType(isGoogleHoliday ? getAttendanceTypeForGoogleHoliday(
                    leaveRequest.getShiftCodeAtRequestTime()) : getAttendanceType(
                    leaveRequest.getShiftCodeAtRequestTime(),
                    leaveRequest.getLeaveType()));
            entry.setComment(isGoogleHoliday
                    ? "Google Holiday - " + googleHolidayName.orElse("Holiday") + " (Auto Generated)"
                    : generateComment(leaveRequest));
            entry.setStatus(TimeEntryStatus.SUBMITTED); // Auto-generated entries need approval
            entry.setIsOvertime(false);
            entry.setIsDefaulter(false);

            // Set timestamps to match leave request timestamp
            entry.setCreatedAt(leaveRequest.getTimestamp());
            entry.setUpdatedAt(leaveRequest.getTimestamp());

            // Lock the entry
            entry.setIsLocked(true);
            entry.setLockedBy("SYSTEM");
            entry.setLockedAt(leaveRequest.getTimestamp());
            entry.setAutoGenerated(true);
            entry.setSourceLeaveId(leaveRequest.getId());

            entries.add(entry);
        }

        return entries;
    }

    /**
     * Check if time entries should be generated for this request
     * Returns true ONLY for Leave and CompOff, false for WFH
     */
    private boolean shouldGenerateTimeEntries(VunnoResponse request) {
        String applicationType = request.getApplicationType();

        // Generate for Leave requests ONLY (excluding CompOff)
        if ("Leave".equalsIgnoreCase(applicationType)) {
            // Check if it's CompOff
            if ("CompOff".equalsIgnoreCase(request.getLeaveType())) {
                return false;
            }
            return true;
        }

        // Do NOT generate for WFH - users fill their own time entries
        if ("Work From Home".equalsIgnoreCase(applicationType)) {
            return false;
        }

        return false;
    }

    /**
     * Get project ID for leave time entries
     * Strategy: Use most recent project from user's time entries
     * Fallback: Use primary project from user_project_mapping
     */
    private Long getProjectIdForLeave(String ldap) {
        // Try to get most recent time entry project (last 15 days)
        List<TimeEntry> recentEntries = timeEntryRepository
                .findRecentByLdap(ldap, LocalDate.now().minusDays(15));

        if (!recentEntries.isEmpty()) {
            return recentEntries.get(0).getProject().getId();
        }

        // Fallback: Get primary project from user_project_mapping
        User user = userRepository.findByUsername(ldap)
                .orElseThrow(() -> new RuntimeException("User not found: " + ldap));

        List<UserProject> assignedProjects = userProjectRepository.findByUser(user);

        if (assignedProjects.isEmpty()) {
            throw new RuntimeException("No Project was assigned to user " + ldap + ". Cannot generate time entries.");
        }

        // Filter for Active status if possible, otherwise take the first one
        // Assuming status is stored as String "Active"
        return assignedProjects.stream()
                .filter(up -> "Active".equalsIgnoreCase(up.getStatus()))
                .findFirst()
                .map(up -> up.getProject().getId())
                .orElse(assignedProjects.get(0).getProject().getId());
    }

    /**
     * Get process for time entries based on leave type string
     * Overloaded version for use with day configurations
     */
    private String getProcessForTimeEntry(String leaveType) {
        // CompOff has its own process
        if ("CompOff".equalsIgnoreCase(leaveType)) {
            return "CompOff";
        }

        // All other leaves use Absenteeism
        return "Absenteeism";
    }

    /**
     * Calculates time in minutes based on shift and duration
     * Full Day = 480 mins (8 hours)
     * Half Day = 240 mins (4 hours)
     */
    private int calculateTimeInMinutes(String shiftCode, String duration) {
        if ("Half Day".equalsIgnoreCase(duration) ||
                "Half Day AM".equalsIgnoreCase(duration) ||
                "Half Day PM".equalsIgnoreCase(duration)) {
            return 240;
        }
        return 480; // Full day
    }

    /**
     * Maps shift code and leave type to attendance type
     * Format: "S1/Leave" or "S1/CompOff"
     */
    private String getAttendanceType(String shiftCode, String leaveType) {
        if ("LWP".equalsIgnoreCase(leaveType)) {
            return "LWP";
        }
        if ("CompOff".equalsIgnoreCase(leaveType)) {
            return shiftCode + "/CompOff";
        }
        return shiftCode + "/Leave";
    }

    private boolean isGoogleHoliday(LocalDate date) {
        return holidayRepository.findByHolidayDateAndIsActive(date, true)
                .map(holiday -> "GOOGLE".equalsIgnoreCase(holiday.getHolidayType()))
                .orElse(false);
    }

    private String getAttendanceTypeForGoogleHoliday(String shiftCode) {
        String effectiveShift = (shiftCode == null || shiftCode.isBlank()) ? "S1" : shiftCode;
        return effectiveShift + "/GO";
    }

    private Optional<String> getGoogleHolidayName(LocalDate date) {
        return holidayRepository.findByHolidayDateAndIsActive(date, true)
                .filter(holiday -> "GOOGLE".equalsIgnoreCase(holiday.getHolidayType()))
                .map(Holiday::getHolidayName);
    }

    /**
     * Generates comment for time entry
     * - Leave: Shows leave type (e.g., "Casual Leave", "Sick Leave")
     * - CompOff: Uses user-provided reason from leave request
     */
    private String generateComment(VunnoResponse leaveRequest) {
        String leaveType = "Auto-Generated - " + leaveRequest.getLeaveType();

        if ("LWP".equalsIgnoreCase(leaveType)) {
            return "Auto-Generated - LWP";
        }

        // For regular leaves, just show the leave type
        return leaveType != null ? leaveType : "Leave";
    }

    /**
     * Calculate leave dates considering duration type
     */
    private List<LocalDate> calculateLeaveDates(LocalDate fromDate, LocalDate toDate, String duration) {
        List<LocalDate> dates = new ArrayList<>();

        // For single day leaves (Full Day, Half Day AM, Half Day PM)
        if ("Full Day".equalsIgnoreCase(duration) ||
                "Half Day AM".equalsIgnoreCase(duration) ||
                "Half Day PM".equalsIgnoreCase(duration)) {
            dates.add(fromDate);
            return dates;
        }

        // For Multiple Days, include all dates from start to end
        LocalDate current = fromDate;
        while (!current.isAfter(toDate)) {
            dates.add(current);
            current = current.plusDays(1);
        }

        return dates;
    }

    /**
     * Calculate time in minutes from duration value (for day configurations)
     * 
     * @param durationValue 1.0 for full day, 0.5 for half day
     * @return time in minutes
     */
    private int calculateTimeInMinutesFromDuration(Double durationValue) {
        if (durationValue != null && durationValue == 0.5) {
            return 240; // Half day = 4 hours
        }
        return 480; // Full day = 8 hours
    }

    /**
     * Get attendance type for a specific day configuration
     * Handles both leave types and duration for proper attendance type code
     */
    private String getAttendanceTypeForDay(String shiftCode, String leaveType, String duration) {
        if ("LWP".equalsIgnoreCase(leaveType)) {
            return "LWP";
        }

        if ("CompOff".equalsIgnoreCase(leaveType)) {
            return shiftCode + "/CompOff";
        }

        // For Sick Leave with half day
        if ("Sick Leave".equalsIgnoreCase(leaveType)) {
            if (duration != null && (duration.contains("Half") || duration.contains("AM") || duration.contains("PM"))) {
                return shiftCode + "/SLH"; // Sick Leave Half
            }
            return shiftCode + "/SL"; // Sick Leave Full
        }

        // For Casual/Earned Leave
        if ("Casual Leave".equalsIgnoreCase(leaveType) || "Earned Leave".equalsIgnoreCase(leaveType)) {
            if (duration != null && (duration.contains("Half") || duration.contains("AM") || duration.contains("PM"))) {
                return shiftCode + "/H"; // Half day
            }
            return shiftCode + "/Leave"; // Full day
        }

        // Default
        return shiftCode + "/Leave";
    }

    /**
     * Delete time entries associated with a leave request
     * Called when leave is rejected or revoked
     */
    @Transactional
    public void deleteTimeEntriesForLeave(Long leaveRequestId) {
        try {
            logger.info("Deleting time entries for leave request ID: {}", leaveRequestId);
            timeEntryRepository.deleteBySourceLeaveId(leaveRequestId);
            logger.info("Successfully deleted time entries for leave request ID: {}", leaveRequestId);
        } catch (Exception e) {
            logger.error("Error deleting time entries for leave request ID: {}", leaveRequestId, e);
            throw new RuntimeException("Failed to delete time entries: " + e.getMessage(), e);
        }
    }
}
