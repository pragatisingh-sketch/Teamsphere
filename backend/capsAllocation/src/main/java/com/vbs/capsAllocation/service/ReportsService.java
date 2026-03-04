package com.vbs.capsAllocation.service;

import com.vbs.capsAllocation.dto.EntityDefaulterComparison;
import com.vbs.capsAllocation.dto.InsightCardDTO;
import com.vbs.capsAllocation.dto.ReportDTO;
import com.vbs.capsAllocation.dto.ComplianceDetailsDTO;
import com.vbs.capsAllocation.dto.UtilizationDetailsDTO;
import com.vbs.capsAllocation.dto.LeavesWFHDetailsDTO;
import com.vbs.capsAllocation.dto.WeeklyTimeEntryDefaulterDTO;
import com.vbs.capsAllocation.model.*;
import com.vbs.capsAllocation.repository.TimeEntryRepository;
import com.vbs.capsAllocation.repository.EmployeeRepository;
import com.vbs.capsAllocation.repository.UserRepository;
import com.vbs.capsAllocation.repository.AttendanceRepository;
import com.vbs.capsAllocation.repository.LeaveUsageLogRepository;
import com.vbs.capsAllocation.repository.VunnoResponseRepository;
import com.vbs.capsAllocation.repository.HolidayRepository;
import com.vbs.capsAllocation.util.LoggerUtil;
import com.vbs.capsAllocation.controller.ReportsController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Service class for generating various reports and insights
 */
@Service
public class ReportsService {

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Autowired
    private TimeEntryRepository timeEntryRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private LeaveUsageLogRepository leaveUsageLogRepository;

    @Autowired
    private VunnoResponseRepository vunnoResponseRepository;

    @Autowired
    private EntityDefaulterService entityDefaulterService;

    @Autowired
    private HolidayRepository holidayRepository;

    /**
     * Get dashboard insight cards with filters
     */
    public List<InsightCardDTO> getDashboardInsights(LocalDate startDate, LocalDate endDate, String userName) {
        try {
            LoggerUtil.logDebug("Generating dashboard insights with multithreading");

            User user = userRepository.findByUsername(userName)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            ;
            Role level = user.getRole();
            System.out.println("Start Date " + startDate);// 30-31
            System.out.println("End Date " + endDate);
            System.out.println("Level " + level.toString()); // ROLE, MANAGER,ADMIN_OPS_MANAGER

            CompletableFuture<InsightCardDTO> compactTableTaskForNonCompliance = CompletableFuture
                    .supplyAsync(
                            () -> createCompactTableCardForNonCompliance(startDate, endDate, level.toString(), user),
                            executorService);

            CompletableFuture<InsightCardDTO> compactTableTaskForOverUtilization = CompletableFuture
                    .supplyAsync(
                            () -> createCompactTableCardForOverUtilization(startDate, endDate, level.toString(), user),
                            executorService);

            CompletableFuture<InsightCardDTO> compactTableTaskForTimeEntryPending = CompletableFuture
                    .supplyAsync(
                            () -> createCompactTableCardForTimeEntryPending(startDate, endDate, level.toString(), user),
                            executorService);

            CompletableFuture<InsightCardDTO> compactTableTaskForAttendanceNotMarked = CompletableFuture
                    .supplyAsync(
                            () -> createCompactTableCardForAttendanceNotMarked(startDate, endDate, level.toString(),
                                    user),
                            executorService);

            CompletableFuture<InsightCardDTO> compactTableTaskForLongWeekendLeaves = CompletableFuture
                    .supplyAsync(
                            () -> createCompactTableCardForLongWeekendLeaves(startDate, endDate, level.toString(),
                                    user),
                            executorService);

            // compactTableTaskForLeavesAndWfh.get()

            // Collect results
            List<InsightCardDTO> insights = Arrays.asList(
                    compactTableTaskForNonCompliance.get(),
                    compactTableTaskForOverUtilization.get(),
                    compactTableTaskForTimeEntryPending.get(),
                    compactTableTaskForAttendanceNotMarked.get(),
                    compactTableTaskForLongWeekendLeaves.get()

            );

            LoggerUtil.logDebug("Dashboard insights generated successfully with {} cards", insights.size());
            return insights;
        } catch (Exception e) {
            LoggerUtil.logError("Error generating dashboard insights: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate dashboard insights", e);
        }
    }

    /**
     * Create a compact table
     */

    private InsightCardDTO createCompactTableCardForNonCompliance(
            LocalDate startDate,
            LocalDate endDate,
            String level,
            User loggedInUser) {

        try {
            // Get defaulter comparisons for all entity types
            Map<String, EntityDefaulterComparison> allComparisons = entityDefaulterService
                    .getAllEntityDefaulterComparison(startDate, endDate, level, loggedInUser);

            System.out.println("All comparison " + allComparisons);

            // Calculate total defaulter count across all entity types
            long totalTimeEntryDefaulters = allComparisons.getOrDefault("TimeEntry",
                    new EntityDefaulterComparison("TimeEntry", 0L, 0L, null)).getCurrentCount();
            long totalAttendanceDefaulters = allComparisons.getOrDefault("Attendance",
                    new EntityDefaulterComparison("Attendance", 0L, 0L, null)).getCurrentCount();
            long totalLeaveDefaulters = allComparisons.getOrDefault("Leaves",
                    new EntityDefaulterComparison("Leaves", 0L, 0L, null)).getCurrentCount();

            long overallCurrent = totalTimeEntryDefaulters + totalAttendanceDefaulters + totalLeaveDefaulters;
            long overallPrevious = allComparisons.values().stream()
                    .mapToLong(EntityDefaulterComparison::getPreviousCount)
                    .sum();
            System.out.println("Current count " + overallCurrent);
            System.out.println("Previous count " + overallPrevious);
            // Create table rows for each entity type
            List<InsightCardDTO.TableRowDTO> tableRows = Arrays.asList(
                    new InsightCardDTO.TableRowDTO("Time Entry", totalTimeEntryDefaulters, "access_time", "red"),
                    new InsightCardDTO.TableRowDTO("Attendance", totalAttendanceDefaulters, "people", "red"),
                    new InsightCardDTO.TableRowDTO("Leaves", totalLeaveDefaulters, "calendar_today", "orange"));

            InsightCardDTO card = new InsightCardDTO();
            card.setTitle("Overall Non-Compliance");
            card.setSubtitle("TimeEntry, Attendance & Leave");
            card.setValue(overallCurrent);
            // Set trend value to 0 to avoid showing misleading percentage
            card.setTrend(new InsightCardDTO.TrendDTO(0.0, "stable", overallCurrent + " issues"));
            card.setIcon("table_chart");
            card.setColor("primary");
            card.setType("number");
            card.setLayoutType("compact-table");

            card.setSummaryText("Summary Overview");
            card.setTableRows(tableRows);
            return card;

        } catch (Exception e) {
            LoggerUtil.logError("Error creating compact table card: {}", e.getMessage(), e);
            return createInsightCard("Compact Table", 0, "Space-Efficient Layout",
                    calculateTrend(0, 0.0), "table_chart", "primary", 0.0, "Data unavailable", "Summary Overview");

        }
    }

    private InsightCardDTO createCompactTableCardForLeavesAndWfh(LocalDate startDate, LocalDate endDate, String level,
            User loggedInuser) {
        try {
            LoggerUtil.logDebug("Creating Leaves and WFH card for period {} to {}", startDate, endDate);

            // Calculate previous date range for comparison
            var previousRange = com.vbs.capsAllocation.util.DateRangeUtils.getPreviousRange(startDate, endDate);
            LocalDate prevStartDate = previousRange.getStartDate();
            LocalDate prevEndDate = previousRange.getEndDate();

            // Get current period counts based on role
            double currentLeaves = 0.0;
            double currentWfh = 0.0;
            double previousLeaves = 0.0;
            double previousWfh = 0.0;

            // Fetch user for role-based filtering
            User currentUser = userRepository.findByUsername(loggedInuser.getUsername())
                    .orElse(null);

            if (level.equals("LEAD") && currentUser != null) {
                // Lead - only their team
                currentLeaves = leaveUsageLogRepository.countLeavesByDateRangeForLead(
                        startDate, endDate, currentUser.getUsername());
                currentWfh = leaveUsageLogRepository.countWfhByDateRangeForLead(
                        startDate, endDate, currentUser.getUsername());

                previousLeaves = leaveUsageLogRepository.countLeavesByDateRangeForLead(
                        prevStartDate, prevEndDate, currentUser.getUsername());
                previousWfh = leaveUsageLogRepository.countWfhByDateRangeForLead(
                        prevStartDate, prevEndDate, currentUser.getUsername());

            } else if (level.equals("MANAGER") && currentUser != null) {
                // Manager - their team
                currentLeaves = leaveUsageLogRepository.countLeavesByDateRangeForManager(
                        startDate, endDate, currentUser.getUsername());
                currentWfh = leaveUsageLogRepository.countWfhByDateRangeForManager(
                        startDate, endDate, currentUser.getUsername());

                previousLeaves = leaveUsageLogRepository.countLeavesByDateRangeForManager(
                        prevStartDate, prevEndDate, currentUser.getUsername());
                previousWfh = leaveUsageLogRepository.countWfhByDateRangeForManager(
                        prevStartDate, prevEndDate, currentUser.getUsername());

            } else {
                // Admin/OPS Manager - full visibility
                currentLeaves = leaveUsageLogRepository.countLeavesByDateRange(startDate, endDate);
                currentWfh = leaveUsageLogRepository.countWfhByDateRange(startDate, endDate);

                previousLeaves = leaveUsageLogRepository.countLeavesByDateRange(prevStartDate, prevEndDate);
                previousWfh = leaveUsageLogRepository.countWfhByDateRange(prevStartDate, prevEndDate);
            }

            // Calculate totals and trends
            double currentTotal = currentLeaves + currentWfh;
            double previousTotal = previousLeaves + previousWfh;
            double trendPercentage = calculateTrendPercentage((long) currentTotal, (long) previousTotal);

            LoggerUtil.logDebug("Current: Leaves={}, WFH={}, Total={}", currentLeaves, currentWfh, currentTotal);
            LoggerUtil.logDebug("Previous: Leaves={}, WFH={}, Total={}", previousLeaves, previousWfh, previousTotal);
            LoggerUtil.logDebug("Trend: {}%", trendPercentage * 100);

            // Create table rows data
            List<InsightCardDTO.TableRowDTO> tableRows = Arrays.asList(
                    new InsightCardDTO.TableRowDTO("Leaves", Math.round(currentLeaves), "event_busy", "orange"),
                    new InsightCardDTO.TableRowDTO("Work From Home", Math.round(currentWfh), "home", "blue"));

            // Create the compact table card
            InsightCardDTO card = new InsightCardDTO();
            card.setTitle("Leaves & WFH");
            card.setSubtitle("Leave and Work From Home Overview");
            card.setValue(Math.round(currentTotal));
            card.setTrend(calculateTrend((int) currentTotal, trendPercentage));
            card.setIcon("calendar_today");
            card.setColor("primary");
            card.setType("number");
            card.setLayoutType("compact-table");
            card.setSummaryText("Summary Overview");
            card.setTableRows(tableRows);

            LoggerUtil.logDebug("Leaves and WFH card created successfully");
            return card;

        } catch (Exception e) {
            LoggerUtil.logError("Error creating Leaves and WFH card: {}", e.getMessage(), e);
            return createInsightCard("Leaves & WFH", 0, "Leave and Work From Home Overview",
                    calculateTrend(0, 0.0), "calendar_today", "primary", 0.0, "Data unavailable", "Summary Overview");
        }
    }

    private InsightCardDTO createCompactTableCardForOverUtilization(LocalDate startDate, LocalDate endDate,
            String level, User loggedInuser) {
        try {
            LoggerUtil.logDebug("Creating Overall Utilization Report for period {} to {}", startDate, endDate);

            // Get current period utilization data
            Map<String, Long> currentUtilization = calculateUtilizationMetrics(startDate, endDate, level,
                    loggedInuser.getUsername());

            long fullyUtilized = currentUtilization.getOrDefault("fully", 0L);
            long partiallyUtilized = currentUtilization.getOrDefault("partially", 0L);
            long zeroUtilization = currentUtilization.getOrDefault("zero", 0L);

            // Fetch holidays from database for the date range
            List<Holiday> holidays = holidayRepository.findHolidaysBetweenDates(startDate, endDate, true);
            Set<LocalDate> holidayDates = holidays.stream()
                    .map(Holiday::getHolidayDate)
                    .collect(Collectors.toSet());

            // Calculate total working days excluding weekends and holidays
            int totalWorkingDays = com.vbs.capsAllocation.util.DateRangeUtils.getWorkingDaysExcludingHolidays(
                    startDate, endDate, holidayDates);
            LoggerUtil.logDebug("Total working days (excluding {} holidays): {}", holidayDates.size(),
                    totalWorkingDays);

            // Calculate utilization percentage (Full=1, Partial=0.5, Zero=0) /
            // totalWorkingDays * 100
            double currentUtilizationPercentage = calculateUtilizationPercentage(currentUtilization, totalWorkingDays);

            LoggerUtil.logDebug("Current Utilization: Fully={}, Partially={}, Zero={}, Percentage={}%",
                    fullyUtilized, partiallyUtilized, zeroUtilization, currentUtilizationPercentage);

            // Create table rows data
            long totalCount = fullyUtilized + partiallyUtilized + zeroUtilization;

            String fullyUtilizedStr = totalCount > 0 ? String.format("%.1f%%", (fullyUtilized * 100.0) / totalCount)
                    : "0.0%";
            String partiallyUtilizedStr = totalCount > 0
                    ? String.format("%.1f%%", (partiallyUtilized * 100.0) / totalCount)
                    : "0.0%";
            String zeroUtilizationStr = totalCount > 0 ? String.format("%.1f%%", (zeroUtilization * 100.0) / totalCount)
                    : "0.0%";

            // Create table rows data with percentages
            List<InsightCardDTO.TableRowDTO> tableRows = Arrays.asList(
                    new InsightCardDTO.TableRowDTO("Fully Utilized", fullyUtilizedStr, "check_circle", "green"),
                    new InsightCardDTO.TableRowDTO("Partially Utilized", partiallyUtilizedStr, "timelapse", "orange"),
                    new InsightCardDTO.TableRowDTO("Zero Utilization", zeroUtilizationStr, "cancel", "red"));

            // Create the compact table card with percentage as main value
            InsightCardDTO card = new InsightCardDTO();
            card.setTitle("Overall Utilization");
            card.setSubtitle("Resource Utilization Overview");
            card.setValue(fullyUtilized); // Show utilization count as main value
            // Set trend value to 0 to avoid showing misleading percentage, show actual % in
            // label
            card.setTrend(new InsightCardDTO.TrendDTO(0.0, currentUtilizationPercentage >= 75 ? "up" : "stable",
                    String.format("%.1f%% utilized", currentUtilizationPercentage)));
            card.setIcon("pie_chart");
            card.setColor("primary");
            card.setType("number");
            card.setLayoutType("compact-table");
            card.setSummaryText("Summary Overview");
            card.setTableRows(tableRows);

            LoggerUtil.logDebug("Overall Utilization card created successfully with {}% utilization",
                    currentUtilizationPercentage);
            return card;

        } catch (Exception e) {
            LoggerUtil.logError("Error creating Overall Utilization card: {}", e.getMessage(), e);
            return createInsightCard("Overall Utilization", 0, "Resource Utilization Overview",
                    calculateTrend(0, 0.0), "pie_chart", "primary", 0.0, "Data unavailable", "Summary Overview");
        }
    }

    /**
     * Create a compact table card for Time-Entry Pending report
     * Shows top 3 employees who haven't filled time entries
     */
    private InsightCardDTO createCompactTableCardForTimeEntryPending(
            LocalDate startDate,
            LocalDate endDate,
            String level,
            User user) {
        try {
            // Get weekly defaulters using the existing method
            List<WeeklyTimeEntryDefaulterDTO> defaulters = getWeeklyTimeEntryDefaulters(
                    startDate, endDate, user.getUsername(), null);

            int totalDefaulters = defaulters.size();

            // Get top 3 defaulters by name (already sorted by missing weeks count)
            List<InsightCardDTO.TableRowDTO> tableRows = new ArrayList<>();
            for (int i = 0; i < Math.min(3, defaulters.size()); i++) {
                WeeklyTimeEntryDefaulterDTO d = defaulters.get(i);
                tableRows.add(new InsightCardDTO.TableRowDTO(
                        d.getEmployeeName(),
                        d.getMissingWeeksCount() + " wks",
                        "person",
                        d.getMissingWeeksCount() >= 3 ? "red" : "orange"));
            }

            // If less than 3 defaulters, the card will show fewer rows
            if (tableRows.isEmpty()) {
                tableRows.add(new InsightCardDTO.TableRowDTO("No pending entries", 0, "check_circle", "green"));
            }

            InsightCardDTO card = new InsightCardDTO();
            card.setTitle("Time Entry Pending");
            card.setSubtitle("Weekly Time-Entry Compliance");
            card.setValue(totalDefaulters);
            // Set trend value to 0 to avoid showing misleading percentage
            card.setTrend(new InsightCardDTO.TrendDTO(
                    0.0, // No percentage change to display
                    totalDefaulters > 0 ? "down" : "stable",
                    totalDefaulters + " count"));
            card.setIcon("schedule");
            card.setColor("warn");
            card.setType("number");
            card.setLayoutType("compact-table");
            card.setSummaryText("Summary Overview");
            card.setTableRows(tableRows);

            LoggerUtil.logDebug("Time Entry Pending card created with {} defaulters", totalDefaulters);
            return card;

        } catch (Exception e) {
            LoggerUtil.logError("Error creating Time Entry Pending card: {}", e.getMessage(), e);
            return createInsightCard("Time Entry Pending", 0, "Weekly Time-Entry Compliance",
                    calculateTrend(0, 0.0), "schedule", "warn", 0.0, "Data unavailable", "Summary Overview");
        }
    }

    /**
     * Calculate utilization metrics from time entries
     * Returns a map with keys: "fully", "partially", "zero"
     * 
     * SIMPLIFIED LOGIC:
     * - Only counts days where users actually submitted timesheet entries
     * - Fully Utilized: >= 480 productive minutes on a day
     * - Partially Utilized: 1-479 productive minutes on a day
     * - Zero Utilized: 0 productive minutes on a day (only
     * absenteeism/non-productive)
     * 
     * Productive Activities: PRODUCTION, TRAINING, MEETING, EVENTS, OTHER_PROJECTS
     * Non-Productive: ABSENTEEISM, DOWNTIME, COVID_SYSTEM_ISSUE, NO_VOLUME,
     * HOLIDAY, COMPOFF
     */
    private Map<String, Long> calculateUtilizationMetrics(LocalDate startDate, LocalDate endDate, String level,
            String loggedInusername) {
        try {
            // Fetch time entries based on role level
            List<TimeEntry> timeEntries;

            if (level.equals("LEAD")) {
                User currentUser = userRepository.findByUsername(loggedInusername).orElse(null);
                if (currentUser != null) {
                    timeEntries = timeEntryRepository.findUtilizationDataForLead(
                            startDate, endDate, currentUser.getUsername());
                } else {
                    timeEntries = new ArrayList<>();
                }
            } else if (level.equals("MANAGER")) {
                User currentUser = userRepository.findByUsername(loggedInusername).orElse(null);
                if (currentUser != null) {
                    timeEntries = timeEntryRepository.findUtilizationDataForManager(
                            startDate, endDate, currentUser.getUsername());
                } else {
                    timeEntries = new ArrayList<>();
                }
            } else {
                // Admin or OPS Manager - full visibility
                timeEntries = timeEntryRepository.findUtilizationDataForAdmin(startDate, endDate);
            }

            // Group by user and date, sum productive minutes
            Map<String, Map<LocalDate, Integer>> userDateProductiveMinutes = new HashMap<>();

            // Track all dates where users have ANY entry (to know which days to count)
            Map<String, Set<LocalDate>> userDatesWithEntries = new HashMap<>();

            for (TimeEntry entry : timeEntries) {
                if (entry.getStatus() == TimeEntryStatus.APPROVED || entry.getStatus() == TimeEntryStatus.PENDING) {
                    String userKey = entry.getUser().getUsername();

                    // Skip holidays - they don't count towards utilization/compliance (treated like
                    // weekends)
                    if (entry.getActivity() == Activity.HOLIDAY) {
                        continue;
                    }

                    LocalDate date = entry.getEntryDate();

                    // Track that this user has an entry on this date
                    userDatesWithEntries.putIfAbsent(userKey, new HashSet<>());
                    userDatesWithEntries.get(userKey).add(date);

                    // Sum up productive minutes only
                    if (isProductiveActivity(entry.getActivity())) {
                        userDateProductiveMinutes.putIfAbsent(userKey, new HashMap<>());
                        Map<LocalDate, Integer> dateMinutes = userDateProductiveMinutes.get(userKey);
                        dateMinutes.put(date, dateMinutes.getOrDefault(date, 0) + entry.getTimeInMins());
                    }
                }
            }

            // Count utilization categories
            long fullyUtilized = 0;
            long partiallyUtilized = 0;
            long zeroUtilization = 0;

            // For each user, check only the days they submitted entries
            for (Map.Entry<String, Set<LocalDate>> userEntry : userDatesWithEntries.entrySet()) {
                String user = userEntry.getKey();
                Set<LocalDate> datesWithEntries = userEntry.getValue();
                Map<LocalDate, Integer> productiveMinutes = userDateProductiveMinutes.getOrDefault(user,
                        new HashMap<>());

                // Only check days where user submitted a timesheet
                for (LocalDate date : datesWithEntries) {
                    int minutes = productiveMinutes.getOrDefault(date, 0);

                    if (minutes == 0) {
                        // User submitted entry but all were non-productive (e.g., ABSENTEEISM)
                        zeroUtilization++;
                    } else if (minutes >= 480) {
                        // Full day of productive work
                        fullyUtilized++;
                    } else {
                        // Partial day of productive work
                        partiallyUtilized++;
                    }
                }
            }

            Map<String, Long> result = new HashMap<>();
            result.put("fully", fullyUtilized);
            result.put("partially", partiallyUtilized);
            result.put("zero", zeroUtilization);

            LoggerUtil.logDebug("Utilization calculated - Fully: {}, Partially: {}, Zero: {}",
                    fullyUtilized, partiallyUtilized, zeroUtilization);

            return result;

        } catch (Exception e) {
            LoggerUtil.logError("Error calculating utilization metrics: {}", e.getMessage(), e);
            return Map.of("fully", 0L, "partially", 0L, "zero", 0L);
        }
    }

    /**
     * Determine if an activity is considered productive (counts towards
     * utilization)
     * 
     * Productive activities:
     * - PRODUCTION: Direct productive work
     * - TRAINING: Employee learning and development
     * - MEETING: Work-related meetings
     * - EVENTS: Work-related events
     * - OTHER_PROJECTS: Working on other projects
     * 
     * Non-productive activities:
     * - ABSENTEEISM: Employee absent
     * - DOWNTIME: Not working
     * - COVID_SYSTEM_ISSUE: System issues preventing work
     * - NO_VOLUME: No work available
     * - HOLIDAY: Official holidays
     * - COMPOFF: Compensatory time off
     */
    private boolean isProductiveActivity(Activity activity) {
        return activity == Activity.PRODUCTION ||
                activity == Activity.TRAINING ||
                activity == Activity.MEETING ||
                activity == Activity.EVENTS ||
                activity == Activity.OTHER_PROJECTS;
    }

    /**
     * Calculate overall utilization percentage
     * Formula: (Fully * 1 + Partially * 0.5) / totalWorkingDays * 100
     * 
     * @param utilization      Map containing "fully", "partially", "zero" counts
     * @param totalWorkingDays Total working days in the date range (same for all
     *                         employees)
     * @return Utilization percentage rounded to 1 decimal place
     */
    private double calculateUtilizationPercentage(Map<String, Long> utilization, int totalWorkingDays) {
        long fully = utilization.getOrDefault("fully", 0L);
        long partially = utilization.getOrDefault("partially", 0L);

        if (totalWorkingDays == 0) {
            return 0.0;
        }

        // Full day = 1 contribution, Partial day = 0.5 contribution, Zero = 0
        // contribution
        double weighted = (fully * 1.0) + (partially * 0.5);
        double percentage = (weighted / totalWorkingDays) * 100.0;

        return Math.round(percentage * 10.0) / 10.0; // Round to 1 decimal place
    }

    /**
     * Calculate trend for utilization percentage
     * Positive trend = improvement (more utilization)
     * Negative trend = decline (less utilization)
     */
    private InsightCardDTO.TrendDTO calculateUtilizationTrend(double trendPercentage) {
        String direction = trendPercentage > 0 ? "up" : trendPercentage < 0 ? "down" : "stable";
        String label = String.format("%+.1f%%", trendPercentage);

        return new InsightCardDTO.TrendDTO(trendPercentage, direction, label);
    }

    /**
     * Create trend display showing the current utilization percentage
     * For utilization, higher is better, so we show it as positive/green
     */
    private InsightCardDTO.TrendDTO createPercentageTrend(double utilizationPercentage) {
        // For utilization percentage display, we want to show it as a positive metric
        // High utilization (>75%) = green/up, Medium (50-75%) = stable, Low (<50%) =
        // red/down
        String direction;
        if (utilizationPercentage >= 75) {
            direction = "up"; // Good utilization
        } else if (utilizationPercentage >= 50) {
            direction = "stable"; // Moderate utilization
        } else {
            direction = "down"; // Low utilization
        }

        String label = String.format("%.1f%%", utilizationPercentage);

        return new InsightCardDTO.TrendDTO(utilizationPercentage, direction, label);
    }

    private InsightCardDTO createCompactTableCard() {
        try {
            LoggerUtil.logDebug("Creating compact table card example");

            // Create table rows data
            List<InsightCardDTO.TableRowDTO> tableRows = Arrays.asList(
                    new InsightCardDTO.TableRowDTO("Time Entry", 642, "access_time", "red"),
                    new InsightCardDTO.TableRowDTO("Attendance", 305, "people", "red"),
                    new InsightCardDTO.TableRowDTO("Leaves", 70, "calendar_today", "orange"));

            // Create the compact table card
            InsightCardDTO card = new InsightCardDTO();
            card.setTitle("Overall Non-Compliance");
            card.setSubtitle("TimeEntry, Attendance & Leave Compliance");
            card.setValue(735);
            card.setTrend(calculateTrend(735, 0.15));
            card.setIcon("table_chart");
            card.setColor("primary");
            card.setType("number");
            card.setLayoutType("compact-table");
            card.setSummaryText("Summary Overview");
            card.setTableRows(tableRows);

            LoggerUtil.logDebug("Compact table card created successfully");
            return card;

        } catch (Exception e) {
            LoggerUtil.logError("Error creating compact table card: {}", e.getMessage(), e);
            return createInsightCard("Compact Table", 0, "Space-Efficient Layout",
                    calculateTrend(0, 0.0), "table_chart", "primary", 0.0, "Data unavailable", "Summary Overview");
        }
    }

    /**
     * Helper method to create an insight card
     */
    private InsightCardDTO createInsightCard(String title, Object value, String subtitle,
            InsightCardDTO.TrendDTO trend, String icon, String color,
            double progress, String secondaryValue, String secondaryLabel) {
        return new InsightCardDTO(title, value, subtitle, trend, icon, color, "number", progress, secondaryValue,
                secondaryLabel);
    }

    /**
     * Helper method to calculate trend
     */
    private InsightCardDTO.TrendDTO calculateTrend(double currentValue, double changeRate) {
        double changeAmount = currentValue * changeRate;
        double trendValue = changeAmount / currentValue * 100;

        String direction = trendValue > 0 ? "up" : trendValue < 0 ? "down" : "stable";
        String label = trendValue >= 0 ? String.format("+%.1f%%", trendValue) : String.format("%.1f%%", trendValue);

        return new InsightCardDTO.TrendDTO(trendValue, direction, label);
    }

    public double calculateTrendPercentage(long current, long previous) {
        java.math.BigDecimal bd;

        // If there was no previous data → 100% growth by default
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }

        // (current - previous) / previous * 100
        double trend = ((double) (current - previous) / previous) * 100.0;

        // Use BigDecimal for precise decimal formatting
        bd = new java.math.BigDecimal(trend);
        bd = bd.setScale(1, java.math.RoundingMode.HALF_UP);
        return bd.doubleValue() / 100.0;
    }

    // Placeholder methods required by ReportsController - to be implemented later
    public Map<String, Object> getTimeEntrySummary(LocalDate startDate, LocalDate endDate) {
        return Map.of("message", "Method not yet implemented");
    }

    public Map<String, Object> getProjectAllocation(LocalDate startDate, LocalDate endDate) {
        return Map.of("message", "Method not yet implemented");
    }

    public Map<String, Object> getEmployeeProductivity(LocalDate startDate, LocalDate endDate) {
        return Map.of("message", "Method not yet implemented");
    }

    public Map<String, Object> getAttendanceReport(LocalDate startDate, LocalDate endDate) {
        return Map.of("message", "Method not yet implemented");
    }

    public Map<String, Object> getProjectStatusReport(LocalDate startDate, LocalDate endDate) {
        return Map.of("message", "Method not yet implemented");
    }

    public Map<String, Object> getApprovalStatistics(LocalDate startDate, LocalDate endDate) {
        return Map.of("message", "Method not yet implemented");
    }

    public Map<String, Object> getDepartmentAllocation(LocalDate startDate, LocalDate endDate) {
        return Map.of("message", "Method not yet implemented");
    }

    public Map<String, Object> getOvertimeReport(LocalDate startDate, LocalDate endDate) {
        return Map.of("message", "Method not yet implemented");
    }

    public Map<String, Object> getCustomReport(ReportsController.CustomReportRequest request) {
        return Map.of("message", "Method not yet implemented");
    }

    public ReportDTO getReportByType(String type) {
        return new ReportDTO();
    }

    /**
     * Get compliance details with dummy data
     */
    public List<ComplianceDetailsDTO> getComplianceDetails(LocalDate startDate, LocalDate endDate) {
        LoggerUtil.logDebug("Fetching compliance details from {} to {}", startDate, endDate);

        // Return dummy data for now
        List<ComplianceDetailsDTO> dummyData = Arrays.asList(
                new ComplianceDetailsDTO(1001L, "John Doe", "Engineering", 5, 2, 1, 8),
                new ComplianceDetailsDTO(1002L, "Jane Smith", "Engineering", 3, 1, 0, 4),
                new ComplianceDetailsDTO(1003L, "Robert Johnson", "Product", 7, 3, 2, 12),
                new ComplianceDetailsDTO(1004L, "Sarah Wilson", "Product", 2, 0, 0, 2),
                new ComplianceDetailsDTO(1005L, "Michael Brown", "Sales", 4, 2, 1, 7),
                new ComplianceDetailsDTO(1006L, "Lisa Garcia", "Design", 1, 0, 0, 1),
                new ComplianceDetailsDTO(1007L, "Christopher Lee", "Sales", 6, 4, 3, 13),
                new ComplianceDetailsDTO(1008L, "Amanda Taylor", "Marketing", 2, 1, 0, 3),
                new ComplianceDetailsDTO(1009L, "David Lee", "Executive", 0, 0, 0, 0),
                new ComplianceDetailsDTO(1010L, "Jennifer Kim", "Finance", 3, 1, 1, 5),
                new ComplianceDetailsDTO(1011L, "Mark Rodriguez", "HR", 1, 0, 0, 1),
                new ComplianceDetailsDTO(1012L, "Emily Chen", "Customer Success", 4, 2, 1, 7));

        LoggerUtil.logDebug("Returned {} compliance records", dummyData.size());
        return dummyData;
    }

    /**
     * Get utilization details with dummy data
     */
    /**
     * Get utilization details with real data
     */
    /**
     * Get utilization details with real data
     */
    @Transactional(readOnly = true)
    public List<UtilizationDetailsDTO> getUtilizationDetails(LocalDate startDate, LocalDate endDate,
            String loggedInUser,
            Map<String, Object> filters) {
        LoggerUtil.logDebug("Fetching utilization details from {} to {} for user {}", startDate, endDate, loggedInUser);

        try {
            User user = userRepository.findByUsername(loggedInUser)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            String role = user.getRole().toString();

            // 1. Fetch Time Entries based on Role
            List<TimeEntry> timeEntries;
            if ("LEAD".equals(role)) {
                timeEntries = timeEntryRepository.findUtilizationDataForLead(startDate, endDate, loggedInUser);
            } else if ("MANAGER".equals(role)) {
                timeEntries = timeEntryRepository.findUtilizationDataForManager(startDate, endDate, loggedInUser);
            } else {
                timeEntries = timeEntryRepository.findUtilizationDataForAdmin(startDate, endDate);
            }

            // 2. Fetch holidays from database for the date range
            List<Holiday> holidays = holidayRepository.findHolidaysBetweenDates(startDate, endDate, true);
            Set<LocalDate> holidayDates = holidays.stream()
                    .map(Holiday::getHolidayDate)
                    .collect(Collectors.toSet());
            LoggerUtil.logDebug("Found {} holidays in range {} to {}", holidayDates.size(), startDate, endDate);

            // 3. Group by User, Calculate Metrics, and track entry dates per user
            Map<String, UtilizationMetrics> userMetrics = new HashMap<>();
            Map<String, Set<LocalDate>> userEntryDates = new HashMap<>(); // Track which dates each user has entries

            for (TimeEntry entry : timeEntries) {
                if (entry.getStatus() == TimeEntryStatus.APPROVED || entry.getStatus() == TimeEntryStatus.PENDING) {
                    String username = entry.getUser().getUsername();

                    // Skip holidays WITH HOLIDAY ACTIVITY - they don't count towards utilization
                    // But we DO track entries on holiday DATES with other activities
                    if (entry.getActivity() == Activity.HOLIDAY) {
                        continue;
                    }

                    userMetrics.putIfAbsent(username, new UtilizationMetrics());
                    userEntryDates.putIfAbsent(username, new HashSet<>());

                    UtilizationMetrics metrics = userMetrics.get(username);
                    boolean isProductive = isProductiveActivity(entry.getActivity());

                    // Debug logging for tracing entry processing
                    if ("vrajoriya".equals(username)) {
                        LoggerUtil.logDebug("ENTRY DEBUG - vrajoriya: date={}, activity={}, mins={}, isProductive={}",
                                entry.getEntryDate(), entry.getActivity(), entry.getTimeInMins(), isProductive);
                    }

                    metrics.addEntry(entry.getEntryDate(), entry.getTimeInMins(), isProductive);

                    // Track this date as having an entry for this user
                    userEntryDates.get(username).add(entry.getEntryDate());
                }
            }

            // 4. Fetch Employee Details
            List<String> usernames = new ArrayList<>(userMetrics.keySet());

            // Try fetching by LDAP
            List<Employee> employeesByLdap = employeeRepository.findByLdapIn(usernames);
            Map<String, Employee> employeeMap = employeesByLdap.stream()
                    .collect(Collectors.toMap(Employee::getLdap, e -> e));

            // Identify missing users and try fetching by Email
            List<String> missingUsers = usernames.stream()
                    .filter(u -> !employeeMap.containsKey(u))
                    .collect(Collectors.toList());

            if (!missingUsers.isEmpty()) {
                LoggerUtil.logDebug("Found {} missing users, trying email lookup", missingUsers.size());
                List<Employee> employeesByEmail = employeeRepository.findByEmailIn(missingUsers);

                for (Employee emp : employeesByEmail) {
                    // Map by email (which matches the username in this case)
                    if (emp.getEmail() != null) {
                        employeeMap.put(emp.getEmail(), emp);
                    }
                }
            }

            // 5. Build DTOs and Apply Filters
            List<UtilizationDetailsDTO> result = new ArrayList<>();

            for (String username : userMetrics.keySet()) {
                Employee emp = employeeMap.get(username);
                if (emp == null) {
                    LoggerUtil.logDebug("Skipping user {} - No employee record found", username);
                    continue; // Skip if no employee record found
                }

                // Apply Filters
                if (filters != null) {
                    if (filters.containsKey("team") && !filters.get("team").equals(emp.getTeam()))
                        continue;
                    if (filters.containsKey("project") && !filters.get("project").equals(emp.getProcess()))
                        continue;
                    if (filters.containsKey("program") && !filters.get("program").equals(emp.getPnseProgram()))
                        continue;
                    if (filters.containsKey("manager") && !filters.get("manager").equals(emp.getProgramManager()))
                        continue;
                }

                // Calculate effective working days for this user:
                // Base working days (weekdays - holidays) + any weekend/holiday days where user
                // has entries
                Set<LocalDate> thisUserEntryDates = userEntryDates.getOrDefault(username, Collections.emptySet());
                int effectiveWorkingDays = com.vbs.capsAllocation.util.DateRangeUtils.getEffectiveWorkingDaysForUser(
                        startDate, endDate, holidayDates, thisUserEntryDates);

                LoggerUtil.logDebug("User {} - entry dates: {}, effective working days: {}",
                        username, thisUserEntryDates.size(), effectiveWorkingDays);

                UtilizationMetrics metrics = userMetrics.get(username);
                metrics.calculate(effectiveWorkingDays);

                result.add(new UtilizationDetailsDTO(
                        emp.getId(),
                        emp.getFirstName() + " " + emp.getLastName(),
                        emp.getTeam(),
                        emp.getProgramManager(),
                        emp.getProcess(),
                        emp.getPnseProgram(),
                        metrics.fullyUtilized,
                        metrics.partiallyUtilized,
                        metrics.zeroUtilization,
                        metrics.utilizationPercentage));
            }

            LoggerUtil.logDebug("Total time entries: {}", timeEntries.size());
            LoggerUtil.logDebug("Unique users in time entries: {}", userMetrics.size());
            LoggerUtil.logDebug("Employees found: {}", employeeMap.size());
            LoggerUtil.logDebug("Final result size: {}", result.size());

            return result;

        } catch (Exception e) {
            LoggerUtil.logError("Error fetching utilization details: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get top 3 low utilization users
     */
    /**
     * Get top 3 low utilization users
     */
    @Transactional(readOnly = true)
    public List<UtilizationDetailsDTO> getTopLowUtilization(LocalDate startDate, LocalDate endDate, String loggedInUser,
            Map<String, Object> filters) {
        List<UtilizationDetailsDTO> allDetails = getUtilizationDetails(startDate, endDate, loggedInUser, filters);

        // Sort by utilization percentage (ascending) to find lowest utilization
        return allDetails.stream()
                .sorted(Comparator.comparingDouble(UtilizationDetailsDTO::getUtilizationPercentage))
                .limit(3)
                .collect(Collectors.toList());
    }

    // Helper class for metrics calculation
    private static class UtilizationMetrics {
        Map<LocalDate, Integer> dailyMinutes = new HashMap<>();
        int fullyUtilized = 0;
        int partiallyUtilized = 0;
        int zeroUtilization = 0;
        double utilizationPercentage = 0.0;

        void addEntry(LocalDate date, int minutes, boolean isProductive) {
            if (isProductive) {
                dailyMinutes.put(date, dailyMinutes.getOrDefault(date, 0) + minutes);
            } else {
                dailyMinutes.putIfAbsent(date, 0); // Ensure date is tracked even if 0 productive minutes
            }
        }

        /**
         * Calculate utilization metrics using the total working days in the date range
         * as the denominator.
         * Formula: (fullyUtilized * 1 + partiallyUtilized * 0.5) / totalWorkingDays *
         * 100
         *
         * @param totalWorkingDays The total number of working days in the date range
         *                         (same for all employees)
         */
        void calculate(int totalWorkingDays) {
            fullyUtilized = 0;
            partiallyUtilized = 0;
            zeroUtilization = 0;

            for (int minutes : dailyMinutes.values()) {
                if (minutes == 0)
                    zeroUtilization++;
                else if (minutes >= 480)
                    fullyUtilized++;
                else
                    partiallyUtilized++;
            }

            // Use total working days in the date range as the denominator (same for
            // everyone)
            if (totalWorkingDays > 0) {
                // Full day = 1, Partial day = 0.5, Zero = 0
                double weighted = (fullyUtilized * 1.0) + (partiallyUtilized * 0.5);
                utilizationPercentage = Math.round((weighted / totalWorkingDays) * 100.0 * 10.0) / 10.0;
            }
        }
    }

    /**
     * Get leaves and WFH details with dummy data
     */
    public List<LeavesWFHDetailsDTO> getLeavesWFHDetails(LocalDate startDate, LocalDate endDate) {
        LoggerUtil.logDebug("Fetching leaves/WFH details from {} to {}", startDate, endDate);

        // Return dummy data for now
        List<LeavesWFHDetailsDTO> dummyData = Arrays.asList(
                new LeavesWFHDetailsDTO(1001L, "John Doe", "Engineering", 3, 5, 8),
                new LeavesWFHDetailsDTO(1002L, "Jane Smith", "Engineering", 2, 4, 6),
                new LeavesWFHDetailsDTO(1003L, "Robert Johnson", "Product", 5, 3, 8),
                new LeavesWFHDetailsDTO(1004L, "Sarah Wilson", "Product", 1, 6, 7),
                new LeavesWFHDetailsDTO(1005L, "Michael Brown", "Sales", 4, 2, 6),
                new LeavesWFHDetailsDTO(1006L, "Lisa Garcia", "Design", 2, 7, 9),
                new LeavesWFHDetailsDTO(1007L, "Christopher Lee", "Sales", 6, 1, 7),
                new LeavesWFHDetailsDTO(1008L, "Amanda Taylor", "Marketing", 3, 4, 7),
                new LeavesWFHDetailsDTO(1009L, "David Lee", "Executive", 2, 3, 5),
                new LeavesWFHDetailsDTO(1010L, "Jennifer Kim", "Finance", 4, 5, 9),
                new LeavesWFHDetailsDTO(1011L, "Mark Rodriguez", "HR", 1, 3, 4),
                new LeavesWFHDetailsDTO(1012L, "Emily Chen", "Customer Success", 3, 6, 9));

        LoggerUtil.logDebug("Returned {} leaves/WFH records", dummyData.size());
        return dummyData;
    }

    /**
     * Get top 3 defaulters for a specific type
     */
    public List<com.vbs.capsAllocation.dto.TopDefaulterDTO> getTopDefaulters(
            String type,
            LocalDate startDate,
            LocalDate endDate,
            String userName,
            java.util.Map<String, Object> filters) {

        User user = userRepository.findByUsername(userName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return entityDefaulterService.getTopDefaulters(type, startDate, endDate, user.getRole().toString(), user,
                filters);
    }

    /**
     * Get detailed list of defaulters for a specific type
     */
    public java.util.List<com.vbs.capsAllocation.dto.DefaulterDetailDTO> getAllDefaulters(
            String type,
            LocalDate startDate,
            LocalDate endDate,
            String loggedInUser,
            java.util.Map<String, Object> filters) {

        User user = userRepository.findByUsername(loggedInUser)
                .orElseThrow(() -> new RuntimeException("User not found"));

        DefaulterTypeService<?> service = getServiceForType(type);
        return service.getDefaultersList(startDate, endDate, user.getRole().name(), user, filters);
    }

    public java.util.Map<String, java.util.List<String>> getFilterOptions() {
        java.util.Map<String, java.util.List<String>> options = new java.util.HashMap<>();
        options.put("Team", employeeRepository.findDistinctTeams());
        options.put("Project", employeeRepository.findDistinctProjects());
        options.put("Program", employeeRepository.findDistinctPrograms());
        options.put("Manager", employeeRepository.findDistinctManagers());
        options.put("Lead", employeeRepository.findDistinctLeads());
        return options;
    }

    private DefaulterTypeService<?> getServiceForType(String type) {
        if ("TimeEntry".equalsIgnoreCase(type)) {
            return timeEntryDefaulterService;
        } else if ("Attendance".equalsIgnoreCase(type)) {
            return attendanceDefaulterService;
        } else if ("Leaves".equalsIgnoreCase(type)) {
            return leaveDefaulterService;
        } else {
            throw new IllegalArgumentException("Unknown defaulter type: " + type);
        }
    }

    @Autowired
    private com.vbs.capsAllocation.service.impl.TimeEntryDefaulterService timeEntryDefaulterService;

    @Autowired
    private com.vbs.capsAllocation.service.impl.AttendanceDefaulterService attendanceDefaulterService;

    @Autowired
    private com.vbs.capsAllocation.service.impl.LeaveDefaulterService leaveDefaulterService;

    /**
     * Get detailed issues for a specific user by type and date range
     * Used for the issue details modal when clicking on issue counts
     */
    @Transactional(readOnly = true)
    public List<com.vbs.capsAllocation.dto.IssueDetailDTO> getUserIssues(
            String type, String employeeLdap, LocalDate startDate, LocalDate endDate) {

        LoggerUtil.logDebug("Fetching user issues - type: {}, ldap: {}, from {} to {}",
                type, employeeLdap, startDate, endDate);

        List<com.vbs.capsAllocation.dto.IssueDetailDTO> issues = new ArrayList<>();

        switch (type.toLowerCase()) {
            case "timeentry":
                List<TimeEntry> timeEntries = timeEntryRepository.findDefaulterIssuesByLdap(
                        employeeLdap, startDate, endDate);
                for (TimeEntry te : timeEntries) {
                    issues.add(com.vbs.capsAllocation.dto.IssueDetailDTO.fromTimeEntry(
                            te.getId(),
                            te.getEntryDate(),
                            te.getProject() != null ? te.getProject().getProjectName() : "N/A",
                            te.getProcess(),
                            te.getActivity() != null ? te.getActivity().name() : "N/A",
                            te.getTimeInMins(),
                            te.getStatus() != null ? te.getStatus().name() : "N/A",
                            te.getComment(),
                            te.getCreatedAt(),
                            te.getUpdatedAt()));
                }
                break;

            case "attendance":
                List<Attendance> attendances = attendanceRepository.findDefaulterIssuesByLdap(
                        employeeLdap, startDate, endDate);
                for (Attendance a : attendances) {
                    issues.add(com.vbs.capsAllocation.dto.IssueDetailDTO.fromAttendance(
                            a.getId(),
                            a.getEntryDate(),
                            a.getEntryTimestamp(),
                            a.getExitTimestamp(),
                            a.getLateLoginReason(),
                            a.getLateOrEarlyLogoutReason(),
                            a.getIsOutsideOffice(),
                            a.getLateOrEarlyCheckout()));
                }
                break;

            case "leaves":
                List<VunnoResponse> leaves = vunnoResponseRepository.findUnplannedLeaveIssuesByLdap(
                        employeeLdap, startDate, endDate);
                for (VunnoResponse v : leaves) {
                    issues.add(com.vbs.capsAllocation.dto.IssueDetailDTO.fromLeave(
                            v.getId(),
                            v.getFromDate(),
                            v.getToDate(),
                            v.getLeaveType(),
                            v.getLeaveCategory(),
                            v.getDuration(),
                            v.getApplicationType(),
                            v.getStatus(),
                            v.getStartTime(),
                            v.getEndTime(),
                            v.getTimestamp()));
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown issue type: " + type);
        }

        LoggerUtil.logDebug("Found {} issues for user {}", issues.size(), employeeLdap);
        return issues;
    }

    /**
     * Get list of users with missing time-entries grouped by weeks.
     * Each user will have a count of weeks they have not filled entries.
     * 
     * Business Rules:
     * - Only includes ACTIVE employees (status = 'Active')
     * - Date range is bounded by employee's start date (DOJ)
     * - Week is defined as Monday to Friday (5 working days)
     * 
     * @param startDate The start of the date range to analyze
     * @param endDate   The end of the date range to analyze
     * @param userName  The logged-in user's username (for role-based filtering)
     * @param filters   Optional filters (team, manager, etc.)
     * @return List of WeeklyTimeEntryDefaulterDTO sorted by missingWeeksCount
     *         (highest first)
     */
    @Transactional(readOnly = true)
    public List<com.vbs.capsAllocation.dto.WeeklyTimeEntryDefaulterDTO> getWeeklyTimeEntryDefaulters(
            LocalDate startDate, LocalDate endDate, String userName, Map<String, Object> filters) {

        LoggerUtil.logDebug("Fetching weekly time entry defaulters from {} to {} for user {}",
                startDate, endDate, userName);

        try {
            User user = userRepository.findByUsername(userName)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userName));
            String role = user.getRole().toString();

            // 1. Get all ACTIVE employees based on role visibility
            List<Employee> activeEmployees;
            if ("LEAD".equals(role)) {
                activeEmployees = employeeRepository.findActiveEmployeesForLead(userName);
            } else if ("MANAGER".equals(role)) {
                activeEmployees = employeeRepository.findActiveEmployeesForManager(userName);
            } else {
                // Admin/OPS Manager - see all active employees
                activeEmployees = employeeRepository.findByStatus("Active");
            }

            LoggerUtil.logDebug("Found {} active employees", activeEmployees.size());

            // 2. Fetch holidays to exclude them from working days calculation
            List<Holiday> holidays = holidayRepository.findHolidaysBetweenDates(startDate, endDate, true);
            Set<LocalDate> holidayDates = holidays.stream()
                    .map(Holiday::getHolidayDate)
                    .collect(Collectors.toSet());

            // 3. Get all time entries in the date range for tracking which days have
            // entries
            Map<String, Set<LocalDate>> employeeFilledDates = new HashMap<>();

            List<TimeEntry> allTimeEntries;
            if ("LEAD".equals(role)) {
                allTimeEntries = timeEntryRepository.findUtilizationDataForLead(startDate, endDate, userName);
            } else if ("MANAGER".equals(role)) {
                allTimeEntries = timeEntryRepository.findUtilizationDataForManager(startDate, endDate, userName);
            } else {
                allTimeEntries = timeEntryRepository.findUtilizationDataForAdmin(startDate, endDate);
            }

            // Build map of LDAP -> dates with entries
            for (TimeEntry entry : allTimeEntries) {
                if (entry.getStatus() == TimeEntryStatus.APPROVED || entry.getStatus() == TimeEntryStatus.PENDING) {
                    String ldap = entry.getLdap();
                    employeeFilledDates.putIfAbsent(ldap, new HashSet<>());
                    employeeFilledDates.get(ldap).add(entry.getEntryDate());
                }
            }

            // 4. Process each active employee
            List<com.vbs.capsAllocation.dto.WeeklyTimeEntryDefaulterDTO> results = new ArrayList<>();

            for (Employee emp : activeEmployees) {
                // Apply filters
                if (filters != null) {
                    if (filters.containsKey("team")
                            && !String.valueOf(filters.get("team")).equalsIgnoreCase(emp.getTeam())) {
                        continue;
                    }
                    if (filters.containsKey("manager")
                            && !String.valueOf(filters.get("manager")).equalsIgnoreCase(emp.getProgramManager())) {
                        continue;
                    }
                }

                // Calculate effective start date (bounded by employee's start date)
                LocalDate empStartDate = null;
                String empStartDateStr = emp.getStartDate();
                if (empStartDateStr != null && !empStartDateStr.isEmpty()) {
                    try {
                        empStartDate = LocalDate.parse(empStartDateStr);
                    } catch (Exception e) {
                        // If parsing fails, try common date formats
                        try {
                            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
                                    .ofPattern("dd-MM-yyyy");
                            empStartDate = LocalDate.parse(empStartDateStr, formatter);
                        } catch (Exception e2) {
                            LoggerUtil.logDebug("Could not parse start date for employee {}: {}", emp.getLdap(),
                                    empStartDateStr);
                        }
                    }
                }
                LocalDate effectiveStartDate = (empStartDate != null && empStartDate.isAfter(startDate))
                        ? empStartDate
                        : startDate;

                // Skip if employee hasn't started yet within the range
                if (effectiveStartDate.isAfter(endDate)) {
                    continue;
                }

                // Get dates filled by this employee
                Set<LocalDate> filledDates = employeeFilledDates.getOrDefault(emp.getLdap(), new HashSet<>());

                // Analyze weekly breakdown
                List<com.vbs.capsAllocation.dto.WeeklyTimeEntryDefaulterDTO.WeeklyBreakdown> weeklyBreakdowns = analyzeWeeklyBreakdown(
                        effectiveStartDate, endDate, filledDates, holidayDates);

                // Only add if there are missing weeks
                if (!weeklyBreakdowns.isEmpty()) {
                    com.vbs.capsAllocation.dto.WeeklyTimeEntryDefaulterDTO dto = new com.vbs.capsAllocation.dto.WeeklyTimeEntryDefaulterDTO(
                            emp.getId(),
                            emp.getLdap(),
                            emp.getFirstName() + " " + emp.getLastName(),
                            emp.getEmail(),
                            emp.getTeam(),
                            emp.getProgramManager(),
                            weeklyBreakdowns.size(),
                            weeklyBreakdowns);
                    results.add(dto);
                }
            }

            // Sort by missingWeeksCount (highest first)
            results.sort((a, b) -> Integer.compare(b.getMissingWeeksCount(), a.getMissingWeeksCount()));

            LoggerUtil.logDebug("Found {} employees with missing time entries", results.size());
            return results;

        } catch (Exception e) {
            LoggerUtil.logError("Error fetching weekly time entry defaulters: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch weekly time entry defaulters", e);
        }
    }

    /**
     * Analyze which weeks have missing entries and break down by specific days.
     * 
     * @param startDate    The effective start date for this employee
     * @param endDate      The end date of the range
     * @param filledDates  Set of dates where the employee has time entries
     * @param holidayDates Set of holiday dates to exclude
     * @return List of WeeklyBreakdown for weeks with missing entries
     */
    private List<com.vbs.capsAllocation.dto.WeeklyTimeEntryDefaulterDTO.WeeklyBreakdown> analyzeWeeklyBreakdown(
            LocalDate startDate, LocalDate endDate, Set<LocalDate> filledDates, Set<LocalDate> holidayDates) {

        List<com.vbs.capsAllocation.dto.WeeklyTimeEntryDefaulterDTO.WeeklyBreakdown> breakdowns = new ArrayList<>();

        // Find the Monday of the week containing startDate
        LocalDate weekStart = startDate.with(java.time.DayOfWeek.MONDAY);
        if (weekStart.isBefore(startDate)) {
            // If the Monday is before the effective start, adjust
            weekStart = startDate;
        }

        java.time.format.DateTimeFormatter labelFormatter = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy");

        while (!weekStart.isAfter(endDate)) {
            // Calculate week end (Friday of this week or endDate, whichever is earlier)
            LocalDate weekEnd = weekStart.with(java.time.DayOfWeek.FRIDAY);
            if (weekEnd.isAfter(endDate)) {
                weekEnd = endDate;
            }

            // Get actual working days in this week (Mon-Fri, excluding holidays and
            // weekends)
            List<LocalDate> workingDaysInWeek = new ArrayList<>();
            LocalDate day = weekStart;
            while (!day.isAfter(weekEnd)) {
                java.time.DayOfWeek dow = day.getDayOfWeek();
                // Only count Mon-Fri and non-holidays
                if (dow != java.time.DayOfWeek.SATURDAY &&
                        dow != java.time.DayOfWeek.SUNDAY &&
                        !holidayDates.contains(day)) {
                    workingDaysInWeek.add(day);
                }
                day = day.plusDays(1);
            }

            // Find missing days
            List<LocalDate> missingDays = workingDaysInWeek.stream()
                    .filter(d -> !filledDates.contains(d))
                    .collect(Collectors.toList());

            // Only add if there are missing days
            if (!missingDays.isEmpty()) {
                boolean wholeWeekMissing = missingDays.size() == workingDaysInWeek.size();

                // Create week label (e.g., "29 Dec 2025 - 2 Jan 2026")
                LocalDate actualWeekStart = weekStart.with(java.time.DayOfWeek.MONDAY);

                // EXCLUDE CURRENT WEEK
                // We don't want to flag the current ongoing week as missing, even if days are
                // missing
                LocalDate currentWeekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY);

                // Debug logging
                LoggerUtil.logDebug("analyzeWeeklyBreakdown - actualWeekStart: {}, currentWeekStart: {}, equals: {}",
                        actualWeekStart, currentWeekStart, actualWeekStart.equals(currentWeekStart));

                if (actualWeekStart.equals(currentWeekStart)) {
                    // Skip current week
                    LoggerUtil.logDebug("Skipping current week: {}", actualWeekStart);
                    weekStart = weekStart.plusWeeks(1).with(java.time.DayOfWeek.MONDAY);
                    continue;
                }

                LocalDate actualWeekEnd = weekStart.with(java.time.DayOfWeek.FRIDAY);
                String weekLabel = actualWeekStart.format(labelFormatter) + " - "
                        + actualWeekEnd.format(labelFormatter);

                com.vbs.capsAllocation.dto.WeeklyTimeEntryDefaulterDTO.WeeklyBreakdown breakdown = new com.vbs.capsAllocation.dto.WeeklyTimeEntryDefaulterDTO.WeeklyBreakdown(
                        actualWeekStart,
                        actualWeekEnd,
                        weekLabel,
                        wholeWeekMissing,
                        missingDays);
                breakdowns.add(breakdown);
            }

            // Move to next week
            weekStart = weekStart.plusWeeks(1).with(java.time.DayOfWeek.MONDAY);
        }

        return breakdowns;
    }

    /**
     * Get daily attendance defaulters for a specific date
     * Returns employees who haven't marked attendance on the given date
     */
    /**
     * Get daily attendance defaulters
     */
    @Transactional(readOnly = true)
    public List<com.vbs.capsAllocation.dto.DailyAttendanceDefaulterDTO> getDailyAttendanceDefaulters(
            LocalDate date, String userName, Map<String, Object> filters) {
        try {
            LoggerUtil.logDebug("Fetching daily attendance defaulters for date: {}", date);

            User user = userRepository.findByUsername(userName)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            String role = user.getRole().toString();

            // Get all active employees based on role
            List<Employee> activeEmployees;
            if ("LEAD".equals(role)) {
                activeEmployees = employeeRepository.findActiveEmployeesForLead(userName);
            } else if ("MANAGER".equals(role)) {
                activeEmployees = employeeRepository.findActiveEmployeesForManager(userName);
            } else {
                activeEmployees = employeeRepository.findByStatus("Active");
            }

            // Get all employees who have marked attendance for this date - using repository query to avoid lazy loading
            List<String> ldapsWithAttendance = attendanceRepository.findEmployeeLdapsByEntryDate(date);

            // Get all employees who have approved leave for this date from VunnoResponse - using repository query
            List<String> ldapsOnLeave = vunnoResponseRepository.findEmployeeLdapsOnApprovedLeaveForDate(date);

            LoggerUtil.logDebug("Found {} ldaps with attendance, {} on approved leave",
                    ldapsWithAttendance.size(), ldapsOnLeave.size());

            // Get all employees who have ABSENTEEISM time entry for this date
            List<String> ldapsWithAbsenteeism = timeEntryRepository.findAll().stream()
                    .filter(te -> te.getEntryDate().equals(date))
                    .filter(te -> te.getActivity() == Activity.ABSENTEEISM)
                    .filter(te -> te.getStatus() == TimeEntryStatus.APPROVED
                            || te.getStatus() == TimeEntryStatus.PENDING)
                    .map(te -> te.getLdap())
                    .distinct()
                    .collect(Collectors.toList());

            LoggerUtil.logDebug("Found {} ldaps with absenteeism entries", ldapsWithAbsenteeism.size());

            // Build list of defaulters
            List<com.vbs.capsAllocation.dto.DailyAttendanceDefaulterDTO> defaulters = new ArrayList<>();

            for (Employee emp : activeEmployees) {
                // Apply filters
                if (filters != null) {
                    if (filters.containsKey("team")
                            && !String.valueOf(filters.get("team")).equalsIgnoreCase(emp.getTeam())) {
                        continue;
                    }
                    if (filters.containsKey("manager")
                            && !String.valueOf(filters.get("manager")).equalsIgnoreCase(emp.getProgramManager())) {
                        continue;
                    }
                }

                // Check if employee hasn't marked attendance AND is not on approved leave AND
                // hasn't marked absenteeism
                if (!ldapsWithAttendance.contains(emp.getLdap())
                        && !ldapsOnLeave.contains(emp.getLdap())
                        && !ldapsWithAbsenteeism.contains(emp.getLdap())) {
                    // Find last attendance date
                    LocalDate lastAttendanceDate = attendanceRepository
                            .findByEmployeeLdapAndEntryDateBetween(emp.getLdap(),
                                    date.minusMonths(1), date.minusDays(1))
                            .stream()
                            .map(a -> a.getEntryDate())
                            .max(LocalDate::compareTo)
                            .orElse(null);

                    com.vbs.capsAllocation.dto.DailyAttendanceDefaulterDTO dto = new com.vbs.capsAllocation.dto.DailyAttendanceDefaulterDTO(
                            emp.getId(),
                            emp.getFirstName() + " " + emp.getLastName(),
                            emp.getLdap(),
                            emp.getEmail(),
                            emp.getTeam(),
                            emp.getProgramManager(),
                            lastAttendanceDate);
                    defaulters.add(dto);
                }
            }

            // Sort by name
            defaulters.sort((a, b) -> a.getEmployeeName().compareTo(b.getEmployeeName()));

            LoggerUtil.logDebug("Found {} attendance defaulters for date {}", defaulters.size(), date);
            return defaulters;

        } catch (Exception e) {
            LoggerUtil.logError("Error fetching daily attendance defaulters: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch daily attendance defaulters", e);
        }
    }

    /**
     * Create a compact table card for Attendance Not Marked Today
     * Shows top 3 employees who haven't marked attendance
     */
    @Transactional(readOnly = true)
    private InsightCardDTO createCompactTableCardForAttendanceNotMarked(
            LocalDate startDate,
            LocalDate endDate,
            String level,
            User user) {
        try {
            LoggerUtil.logDebug("Creating Attendance Not Marked card for today");

            // Get attendance defaulters for today
            LocalDate today = LocalDate.now();
            List<com.vbs.capsAllocation.dto.DailyAttendanceDefaulterDTO> defaulters = getDailyAttendanceDefaulters(
                    today, user.getUsername(), null);

            int totalDefaulters = defaulters.size();

            // Get top 3 defaulters
            List<InsightCardDTO.TableRowDTO> tableRows = new ArrayList<>();
            for (int i = 0; i < Math.min(3, defaulters.size()); i++) {
                com.vbs.capsAllocation.dto.DailyAttendanceDefaulterDTO d = defaulters.get(i);
                tableRows.add(new InsightCardDTO.TableRowDTO(
                        d.getEmployeeName(),
                        d.getDepartment() != null ? d.getDepartment() : "N/A",
                        "how_to_reg",
                        "orange"));
            }

            // If no defaulters, show success message
            if (tableRows.isEmpty()) {
                tableRows.add(new InsightCardDTO.TableRowDTO(
                        "All attendance marked", 0, "check_circle", "green"));
            }

            InsightCardDTO card = new InsightCardDTO();
            card.setTitle("Attendance Not Marked Today");
            card.setSubtitle("Daily Attendance Compliance");
            card.setValue(totalDefaulters);
            card.setTrend(new InsightCardDTO.TrendDTO(
                    0.0,
                    totalDefaulters > 0 ? "down" : "stable",
                    totalDefaulters + " pending"));
            card.setIcon("how_to_reg");
            card.setColor("warn");
            card.setType("number");
            card.setLayoutType("compact-table");
            card.setSummaryText("Summary Overview");
            card.setTableRows(tableRows);

            LoggerUtil.logDebug("Attendance Not Marked card created with {} defaulters", totalDefaulters);
            return card;

        } catch (Exception e) {
            LoggerUtil.logError("Error creating Attendance Not Marked card: {}", e.getMessage(), e);
            return createInsightCard("Attendance Not Marked Today", 0, "Daily Attendance Compliance",
                    calculateTrend(0, 0.0), "how_to_reg", "warn", 0.0, "Data unavailable", "Summary Overview");
        }
    }

    /**
     * Get employees who take leaves during long weekends
     * Detects patterns like: Friday + Weekend + Holiday(Monday) or Holiday(Friday)
     * + Weekend + Monday
     */
    public List<com.vbs.capsAllocation.dto.LongWeekendLeaveDTO> getLongWeekendLeavePatterns(
            LocalDate startDate,
            LocalDate endDate,
            String userName,
            Map<String, Object> filters) {
        try {
            LoggerUtil.logDebug("Fetching long weekend leave patterns from {} to {}", startDate, endDate);

            // Get user role to determine data access
            User user = userRepository.findByUsername(userName)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            String role = user.getRole().toString();

            // Get approved leaves from VunnoResponse based on role
            List<VunnoResponse> allLeaves;
            if ("LEAD".equals(role)) {
                allLeaves = vunnoResponseRepository.findAll().stream()
                        .filter(v -> v.getEmployee() != null && v.getEmployee().getLead() != null
                                && userName.equals(v.getEmployee().getLead()))
                        .filter(v -> "APPROVED".equalsIgnoreCase(v.getStatus()))
                        .filter(v -> v.getApplicationType() != null
                                && v.getApplicationType().toLowerCase().contains("leave"))
                        .filter(v -> v.getFromDate() != null && v.getToDate() != null)
                        .filter(v -> !v.getToDate().isBefore(startDate) && !v.getFromDate().isAfter(endDate))
                        .collect(Collectors.toList());
            } else if ("MANAGER".equals(role)) {
                allLeaves = vunnoResponseRepository.findAll().stream()
                        .filter(v -> v.getEmployee() != null && v.getEmployee().getProgramManager() != null
                                && userName.equals(v.getEmployee().getProgramManager()))
                        .filter(v -> "APPROVED".equalsIgnoreCase(v.getStatus()))
                        .filter(v -> v.getApplicationType() != null
                                && v.getApplicationType().toLowerCase().contains("leave"))
                        .filter(v -> v.getFromDate() != null && v.getToDate() != null)
                        .filter(v -> !v.getToDate().isBefore(startDate) && !v.getFromDate().isAfter(endDate))
                        .collect(Collectors.toList());
            } else {
                allLeaves = vunnoResponseRepository.findAll().stream()
                        .filter(v -> "APPROVED".equalsIgnoreCase(v.getStatus()))
                        .filter(v -> v.getApplicationType() != null
                                && v.getApplicationType().toLowerCase().contains("leave"))
                        .filter(v -> v.getFromDate() != null && v.getToDate() != null)
                        .filter(v -> !v.getToDate().isBefore(startDate) && !v.getFromDate().isAfter(endDate))
                        .collect(Collectors.toList());
            }

            // Get all holidays in the date range
            List<Holiday> holidays = holidayRepository.findHolidaysBetweenDates(startDate, endDate, true);
            Set<LocalDate> holidayDates = holidays.stream()
                    .map(Holiday::getHolidayDate)
                    .collect(Collectors.toSet());

            // Group leaves by employee
            Map<Long, List<VunnoResponse>> leavesByEmployee = allLeaves.stream()
                    .collect(Collectors.groupingBy(l -> l.getEmployee().getId()));

            // Analyze each employee's leave patterns
            List<com.vbs.capsAllocation.dto.LongWeekendLeaveDTO> patterns = new ArrayList<>();

            for (Map.Entry<Long, List<VunnoResponse>> entry : leavesByEmployee.entrySet()) {
                Employee emp = entry.getValue().get(0).getEmployee();

                // Apply filters
                if (filters != null) {
                    if (filters.containsKey("team")
                            && !String.valueOf(filters.get("team")).equalsIgnoreCase(emp.getTeam())) {
                        continue;
                    }
                    if (filters.containsKey("manager")
                            && !String.valueOf(filters.get("manager")).equalsIgnoreCase(emp.getProgramManager())) {
                        continue;
                    }
                }

                List<VunnoResponse> empLeaves = entry.getValue();
                List<com.vbs.capsAllocation.dto.LongWeekendLeaveDTO.LongWeekendInstance> instances = detectLongWeekendPatternsFromVunno(
                        empLeaves, holidayDates);

                if (!instances.isEmpty()) {
                    com.vbs.capsAllocation.dto.LongWeekendLeaveDTO dto = new com.vbs.capsAllocation.dto.LongWeekendLeaveDTO(
                            emp.getId(),
                            emp.getFirstName() + " " + emp.getLastName(),
                            emp.getLdap(),
                            emp.getEmail(),
                            emp.getTeam(),
                            emp.getProgramManager(),
                            instances.size(),
                            instances);
                    patterns.add(dto);
                }
            }

            // Sort by occurrence count (descending)
            patterns.sort((a, b) -> Integer.compare(b.getOccurrenceCount(), a.getOccurrenceCount()));

            LoggerUtil.logDebug("Found {} employees with long weekend leave patterns", patterns.size());
            return patterns;

        } catch (Exception e) {
            LoggerUtil.logError("Error fetching long weekend leave patterns: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Detect long weekend patterns for a single employee
     * A long weekend is considered when:
     * 1. Friday leave + Weekend + Holiday (Monday) = 4+ days
     * 2. Holiday (Friday) + Weekend + Monday leave = 4+ days
     * 3. Multiple consecutive days including Friday/Monday adjacent to
     * weekend+holiday
     */
    private List<com.vbs.capsAllocation.dto.LongWeekendLeaveDTO.LongWeekendInstance> detectLongWeekendPatterns(
            List<LeaveUsageLog> leaves, Set<LocalDate> holidayDates) {

        List<com.vbs.capsAllocation.dto.LongWeekendLeaveDTO.LongWeekendInstance> instances = new ArrayList<>();

        // Sort leaves by date
        leaves.sort(Comparator.comparing(LeaveUsageLog::getLeaveDate));

        for (LeaveUsageLog leave : leaves) {
            LocalDate leaveDate = leave.getLeaveDate();
            java.time.DayOfWeek dayOfWeek = leaveDate.getDayOfWeek();

            // Check if this is a Friday leave
            if (dayOfWeek == java.time.DayOfWeek.FRIDAY) {
                LocalDate saturday = leaveDate.plusDays(1);
                LocalDate sunday = leaveDate.plusDays(2);
                LocalDate monday = leaveDate.plusDays(3);

                // Check if Monday is a holiday
                if (holidayDates.contains(monday)) {
                    int totalDays = 4; // Friday (leave) + Sat + Sun + Monday (holiday)
                    String pattern = "Friday leave + Weekend + Holiday (Monday)";

                    instances.add(new com.vbs.capsAllocation.dto.LongWeekendLeaveDTO.LongWeekendInstance(
                            leaveDate,
                            monday,
                            leave.getLeaveType(),
                            totalDays,
                            pattern));
                }
            }

            // Check if this is a Monday leave
            if (dayOfWeek == java.time.DayOfWeek.MONDAY) {
                LocalDate friday = leaveDate.minusDays(3);
                LocalDate saturday = leaveDate.minusDays(2);
                LocalDate sunday = leaveDate.minusDays(1);

                // Check if Friday is a holiday
                if (holidayDates.contains(friday)) {
                    int totalDays = 4; // Holiday (Friday) + Sat + Sun + Monday (leave)
                    String pattern = "Holiday (Friday) + Weekend + Monday leave";

                    instances.add(new com.vbs.capsAllocation.dto.LongWeekendLeaveDTO.LongWeekendInstance(
                            friday,
                            leaveDate,
                            leave.getLeaveType(),
                            totalDays,
                            pattern));
                }
            }
        }

        return instances;
    }

    /**
     * Detect long weekend patterns from VunnoResponse (leave applications)
     * A long weekend is considered when leaves + weekends + holidays create 3+
     * consecutive non-working days
     * Examples:
     * - Thursday leave + Friday holiday + Weekend = 4 days
     * - Friday leave + Weekend + Monday holiday = 4 days
     * - Weekend + Monday leave + Tuesday holiday = 4 days
     */
    private List<com.vbs.capsAllocation.dto.LongWeekendLeaveDTO.LongWeekendInstance> detectLongWeekendPatternsFromVunno(
            List<VunnoResponse> leaves, Set<LocalDate> holidayDates) {

        List<com.vbs.capsAllocation.dto.LongWeekendLeaveDTO.LongWeekendInstance> instances = new ArrayList<>();
        Set<String> alreadyDetected = new HashSet<>(); // Avoid duplicates using "startDate_endDate" key

        for (VunnoResponse leave : leaves) {
            LocalDate fromDate = leave.getFromDate();
            LocalDate toDate = leave.getToDate();

            // Expand leave period to individual dates
            List<LocalDate> leaveDates = new ArrayList<>();
            LocalDate current = fromDate;
            while (!current.isAfter(toDate)) {
                leaveDates.add(current);
                current = current.plusDays(1);
            }

            // For each leave date, check if it forms a long weekend pattern
            for (LocalDate leaveDate : leaveDates) {
                LocalDate start = leaveDate;
                LocalDate end = leaveDate;

                // Expand backwards to include adjacent non-working days
                LocalDate prev = start.minusDays(1);
                while (isNonWorkingDay(prev, leaveDates, holidayDates)) {
                    start = prev;
                    prev = prev.minusDays(1);
                }

                // Expand forwards to include adjacent non-working days
                LocalDate next = end.plusDays(1);
                while (isNonWorkingDay(next, leaveDates, holidayDates)) {
                    end = next;
                    next = next.plusDays(1);
                }

                // Count total consecutive days
                long totalDays = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;

                // Check if this qualifies as a long weekend pattern
                // Must have: 3+ total days AND at least one holiday
                boolean hasHoliday = containsHoliday(start, end, holidayDates);

                if (totalDays >= 3 && hasHoliday) {
                    // Create unique key to avoid duplicate detection
                    String key = start.toString() + "_" + end.toString();
                    if (!alreadyDetected.contains(key)) {
                        String pattern = buildPatternDescription(start, end, leaveDates, holidayDates);

                        instances.add(new com.vbs.capsAllocation.dto.LongWeekendLeaveDTO.LongWeekendInstance(
                                start,
                                end,
                                leave.getLeaveType(),
                                (int) totalDays,
                                pattern));
                        alreadyDetected.add(key);
                    }
                }
            }
        }

        return instances;
    }

    /**
     * Check if a date is a non-working day (leave, holiday, or weekend)
     */
    private boolean isNonWorkingDay(LocalDate date, List<LocalDate> leaveDates, Set<LocalDate> holidayDates) {
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        return leaveDates.contains(date)
                || holidayDates.contains(date)
                || dayOfWeek == java.time.DayOfWeek.SATURDAY
                || dayOfWeek == java.time.DayOfWeek.SUNDAY;
    }

    /**
     * Check if the date range contains at least one holiday
     */
    private boolean containsHoliday(LocalDate start, LocalDate end, Set<LocalDate> holidayDates) {
        LocalDate current = start;
        while (!current.isAfter(end)) {
            if (holidayDates.contains(current)) {
                return true;
            }
            current = current.plusDays(1);
        }
        return false;
    }

    /**
     * Build human-readable pattern description
     * Examples: "Thu (leave) + Fri (holiday) + Weekend", "Weekend + Mon (leave) +
     * Tue (holiday)"
     */
    private String buildPatternDescription(LocalDate start, LocalDate end,
            List<LocalDate> leaveDates, Set<LocalDate> holidayDates) {
        StringBuilder pattern = new StringBuilder();
        LocalDate current = start;

        while (!current.isAfter(end)) {
            java.time.DayOfWeek dayOfWeek = current.getDayOfWeek();
            String dayName = dayOfWeek.toString().substring(0, 3); // MON -> Mon

            // Determine what type of day this is
            if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
                // Group weekends together
                if (pattern.length() > 0 && !pattern.toString().endsWith("Weekend")) {
                    pattern.append(" + ");
                }
                if (!pattern.toString().endsWith("Weekend")) {
                    pattern.append("Weekend");
                }
            } else if (leaveDates.contains(current)) {
                if (pattern.length() > 0 && !pattern.toString().endsWith("Weekend")) {
                    pattern.append(" + ");
                } else if (pattern.toString().endsWith("Weekend")) {
                    pattern.append(" + ");
                }
                pattern.append(dayName.substring(0, 1).toUpperCase())
                        .append(dayName.substring(1).toLowerCase())
                        .append(" (leave)");
            } else if (holidayDates.contains(current)) {
                if (pattern.length() > 0 && !pattern.toString().endsWith("Weekend")) {
                    pattern.append(" + ");
                } else if (pattern.toString().endsWith("Weekend")) {
                    pattern.append(" + ");
                }
                pattern.append(dayName.substring(0, 1).toUpperCase())
                        .append(dayName.substring(1).toLowerCase())
                        .append(" (holiday)");
            }

            current = current.plusDays(1);
        }

        return pattern.toString();
    }

    /**
     * Create a compact table card for Long Weekend Leaves
     * Shows top 3 employees who take leaves during long weekends
     */
    private InsightCardDTO createCompactTableCardForLongWeekendLeaves(
            LocalDate startDate,
            LocalDate endDate,
            String level,
            User user) {
        try {
            LoggerUtil.logDebug("Creating Long Weekend Leaves card");

            List<com.vbs.capsAllocation.dto.LongWeekendLeaveDTO> patterns = getLongWeekendLeavePatterns(
                    startDate, endDate, user.getUsername(), null);

            int totalPatterns = patterns.stream()
                    .mapToInt(com.vbs.capsAllocation.dto.LongWeekendLeaveDTO::getOccurrenceCount)
                    .sum();

            // Get top 3 employees by occurrence count
            List<InsightCardDTO.TableRowDTO> tableRows = new ArrayList<>();
            for (int i = 0; i < Math.min(3, patterns.size()); i++) {
                com.vbs.capsAllocation.dto.LongWeekendLeaveDTO p = patterns.get(i);
                tableRows.add(new InsightCardDTO.TableRowDTO(
                        p.getEmployeeName(),
                        p.getOccurrenceCount() + " times",
                        "event_busy",
                        p.getOccurrenceCount() >= 3 ? "red" : "orange"));
            }

            // If no patterns, show success message
            if (tableRows.isEmpty()) {
                tableRows.add(new InsightCardDTO.TableRowDTO(
                        "No patterns detected", 0, "check_circle", "green"));
            }

            InsightCardDTO card = new InsightCardDTO();
            card.setTitle("Long Weekend Leave Patterns");
            card.setSubtitle("Strategic Leave Analysis");
            card.setValue(totalPatterns);
            card.setTrend(new InsightCardDTO.TrendDTO(
                    0.0,
                    totalPatterns > 0 ? "neutral" : "stable",
                    patterns.size() + " employees"));
            card.setIcon("event_busy");
            card.setColor("accent");
            card.setType("number");
            card.setLayoutType("compact-table");
            card.setSummaryText("Summary Overview");
            card.setTableRows(tableRows);

            LoggerUtil.logDebug("Long Weekend Leaves card created with {} patterns", totalPatterns);
            return card;

        } catch (Exception e) {
            LoggerUtil.logError("Error creating Long Weekend Leaves card: {}", e.getMessage(), e);
            return createInsightCard("Long Weekend Leave Patterns", 0, "Strategic Leave Analysis",
                    calculateTrend(0, 0.0), "event_busy", "accent", 0.0, "Data unavailable", "Summary Overview");
        }
    }
}