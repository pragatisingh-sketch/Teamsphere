package com.vbs.capsAllocation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vbs.capsAllocation.audit.AuditStatus;
import com.vbs.capsAllocation.dto.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vbs.capsAllocation.model.*;
import com.vbs.capsAllocation.repository.*;
import com.vbs.capsAllocation.util.LoggerUtil;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.web.multipart.MultipartFile;
import com.vbs.capsAllocation.dto.DayConfigurationDto;
import com.vbs.capsAllocation.dto.LeaveBalanceUploadResultDto;

@Service
public class VunnoMgmtService {

    @Autowired
    private VunnoAuditRepository vunnoAuditRepository;

    @Autowired
    private VunnoResponseRepository vunnoResponseRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private LeaveUsageLogRepository leaveUsageLogRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmployeeRelationRepository employeeRelationRepository;

    @Autowired
    private ShiftDetailsRepository shiftDetailsRepository;

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private TimeEntryAutoGenerationService timeEntryAutoGenerationService;

    @Autowired
    private TimeEntryRepository timeEntryRepository;

    private static final Logger logger = LoggerFactory.getLogger(VunnoMgmtService.class);

    private static final ZoneId INDIA_TIMEZONE = ZoneId.of("Asia/Kolkata");

    private static final String GOOGLE_HOLIDAY_TYPE = "GOOGLE";

    public List<VunnoHistoryDto> getFilteredSheetData(String ldap) {
        List<VunnoResponse> responses = vunnoResponseRepository.findByEmployeeLdap(ldap);

        if (responses.isEmpty()) {
            logger.info("No records found in database for LDAP: {}", ldap);
            return Collections.emptyList();
        }

        List<VunnoHistoryDto> historyData = new ArrayList<>();

        for (VunnoResponse response : responses) {
            VunnoHistoryDto dto = new VunnoHistoryDto();
            dto.setId(response.getId());
            dto.setStatus(response.getStatus());
            dto.setRequestorName(response.getRequestorName());
            dto.setApplicationType(response.getApplicationType());
            dto.setLeaveType(response.getLeaveType());
            if ("Mixed".equalsIgnoreCase(response.getLeaveType())) {
                dto.setLeaveType(
                        response.getLeaveType() + VunnoRequestDto.formatMixedDetails(response.getDayConfigurations()));
            }

            // Set leaveDetails using the same logic as VunnoRequestDto
            // Parse JSON dayConfigurations string to List<DayConfigurationDto>
            List<DayConfigurationDto> dayConfigs = null;
            if (response.getDayConfigurations() != null && !response.getDayConfigurations().isEmpty()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    dayConfigs = mapper.readValue(
                            response.getDayConfigurations(),
                            mapper.getTypeFactory().constructCollectionType(List.class, DayConfigurationDto.class));
                } catch (Exception e) {
                    logger.warn("Failed to parse dayConfigurations for response ID {}: {}",
                            response.getId(), e.getMessage());
                }
            }
            dto.setLeaveDetails(
                    VunnoRequestDto.formatLeaveDetailsFromDayConfigs(dayConfigs, response.getDuration()));
            dto.setLeaveCategory(response.getLeaveCategory());
            dto.setStartDate(response.getFromDate() != null ? response.getFromDate().toString() : null);
            dto.setEndDate(response.getToDate() != null ? response.getToDate().toString() : null);
            dto.setDuration(response.getDuration());
            dto.setReason(response.getReason());
            dto.setBackupInfo(response.getBackup());
            dto.setTimesheetProof(response.getTimesheetScreenshot());
            dto.setOooProof(response.getOrgScreenshot());
            dto.setApprover(response.getApprover());
            dto.setDocumentPath(response.getDocumentPath());

            // Convert LocalTime to string (HH:mm)
            dto.setStartTime(response.getStartTime() != null ? response.getStartTime().toString() : null);
            dto.setEndTime(response.getEndTime() != null ? response.getEndTime().toString() : null);

            dto.setShiftCodeAtRequestTime(response.getShiftCodeAtRequestTime());
            dto.setTimestamp(response.getTimestamp() != null ? response.getTimestamp().toString() : null);
            historyData.add(dto);
        }

        logger.info("Returning {} records for LDAP: {}", historyData.size(), ldap);
        return historyData;
    }

    public LeaveBalanceUploadResultDto uploadLeaveBalances(MultipartFile file, UserDetails userDetails, boolean force) {
        String uploader = userDetails.getUsername();
        Integer year = Year.now().getValue();
        Integer month = LocalDate.now().getMonthValue();

        boolean alreadyUploadedThisMonth = leaveBalanceRepository.existsByMonthAndYear(month, year);
        String existingUploader = leaveBalanceRepository.findUploaderForThisMonth().orElse("Unknown");

        if (!force && alreadyUploadedThisMonth) {
            throw new IllegalStateException("Leave balances already uploaded this month by " + existingUploader + ".");
        }

        List<LeaveBalanceUploadDto> rows = parseLeaveBalanceSheet(file);

        int successCount = 0;
        List<LeaveBalanceUploadResultDto.SkippedEmployeeDto> skippedEmployees = new ArrayList<>();

        for (LeaveBalanceUploadDto row : rows) {
            String identifier = row.getLdap().trim();

            // Try LDAP lookup first, then fall back to email
            Optional<Employee> empOpt = employeeRepository.findByLdap(identifier);
            if (empOpt.isEmpty()) {
                empOpt = employeeRepository.findByEmail(identifier);
            }

            if (empOpt.isEmpty()) {
                String reason = identifier.contains("@")
                        ? "No active employee found with email: " + identifier
                        : "No active employee found with LDAP: " + identifier;
                skippedEmployees.add(new LeaveBalanceUploadResultDto.SkippedEmployeeDto(identifier, reason));
                logger.warn("Skipping leave balance upload for '{}': {}", identifier, reason);
                continue;
            }

            Employee employee = empOpt.get();

            try {
                // Only update leave types that were present in the CSV (non-null)
                if (row.getSlBalance() != null) {
                    saveOrUpdateBalance(employee, "HR-SL", row.getSlBalance(), year, month, uploader);
                }
                if (row.getClBalance() != null) {
                    saveOrUpdateBalance(employee, "HR-CL", row.getClBalance(), year, month, uploader);
                }
                if (row.getElBalance() != null) {
                    saveOrUpdateBalance(employee, "HR-EL", row.getElBalance(), year, month, uploader);
                }

                successCount++;
            } catch (Exception e) {
                logger.error("Failed to save leave balance for '{}': {}", identifier, e.getMessage());
                skippedEmployees.add(new LeaveBalanceUploadResultDto.SkippedEmployeeDto(
                        identifier, "DB save failed: " + e.getMessage()));
            }
        }

        String message = "Leave balances uploaded successfully. " + successCount + " employee(s) updated, "
                + skippedEmployees.size() + " skipped.";

        return new LeaveBalanceUploadResultDto(successCount, skippedEmployees.size(), skippedEmployees, message);
    }

    /**
     * Generates a CSV report of employees that were skipped during leave balance
     * upload.
     * 
     * @param skippedEmployees list of skipped employee entries
     * @return byte array of the CSV content
     */
    public byte[] generateSkippedReport(List<LeaveBalanceUploadResultDto.SkippedEmployeeDto> skippedEmployees) {
        StringBuilder csv = new StringBuilder();
        csv.append("LDAP/Email,Reason\n");
        for (LeaveBalanceUploadResultDto.SkippedEmployeeDto skipped : skippedEmployees) {
            // Escape fields for CSV safety
            String ldap = skipped.getLdap().replace("\"", "\"\"");
            String reason = skipped.getReason().replace("\"", "\"\"");
            csv.append("\"").append(ldap).append("\",\"").append(reason).append("\"\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private List<LeaveBalanceUploadDto> parseLeaveBalanceSheet(MultipartFile file) {
        List<LeaveBalanceUploadDto> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            CSVParser parser = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build()
                    .parse(reader);

            String currentMonth = LocalDate.now().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

            String slColumn = currentMonth + "-SL-Balance";
            String clColumn = currentMonth + "-CL-Balance";
            String elColumn = currentMonth + "-EL-Balance";

            // Check which balance columns are present in this CSV
            boolean hasSlColumn = parser.getHeaderMap().containsKey(slColumn.toLowerCase());
            boolean hasClColumn = parser.getHeaderMap().containsKey(clColumn.toLowerCase());
            boolean hasElColumn = parser.getHeaderMap().containsKey(elColumn.toLowerCase());

            logger.info("Leave balance CSV columns detected - SL: {}, CL: {}, EL: {}", hasSlColumn, hasClColumn,
                    hasElColumn);

            for (CSVRecord record : parser) {
                String ldap = record.get("ldap/email").trim();
                if (ldap.isEmpty())
                    continue;

                LeaveBalanceUploadDto dto = new LeaveBalanceUploadDto();
                dto.setLdap(ldap);
                // Only set balance if the column exists in the CSV; otherwise leave null to
                // skip update
                dto.setSlBalance(hasSlColumn ? parseDoubleSafe(record.get(slColumn)) : null);
                dto.setClBalance(hasClColumn ? parseDoubleSafe(record.get(clColumn)) : null);
                dto.setElBalance(hasElColumn ? parseDoubleSafe(record.get(elColumn)) : null);

                records.add(dto);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse leave balance sheet", e);
        }

        return records;
    }

    private Double parseDoubleSafe(String val) {
        try {
            return (Double) (val == null || val.trim().isEmpty() ? 0.0 : Double.parseDouble(val.trim()));
        } catch (NumberFormatException e) {
            return (Double) 0.0;
        }
    }

    private void saveOrUpdateBalance(Employee employee, String leaveType, Double balance, Integer year, Integer month,
            String uploader) {
        Optional<LeaveBalance> existingOpt = leaveBalanceRepository
                .findByEmployeeAndLeaveTypeAndMonthAndYear(employee, leaveType, month, year);

        ZonedDateTime nowInIndia = ZonedDateTime.now(INDIA_TIMEZONE);
        LocalDateTime auditTimeStamp = nowInIndia.toLocalDateTime();

        LeaveBalance balanceEntity = existingOpt.orElseGet(LeaveBalance::new);
        balanceEntity.setEmployee(employee);
        balanceEntity.setLeaveType(leaveType);
        balanceEntity.setBalance(balance);
        balanceEntity.setYear(year);
        balanceEntity.setMonth(month);
        balanceEntity.setSource("HR_UPLOAD");
        balanceEntity.setUploadedBy(uploader);
        balanceEntity.setUploadedAt(auditTimeStamp);
        balanceEntity.setEffectiveFrom(LocalDate.of(year, month, 1));
        leaveBalanceRepository.save(balanceEntity);
    }

    // Helper method
    private String getQuarter(LocalDate date) {
        int month = date.getMonthValue();
        if (month <= 3)
            return "Q1";
        else if (month <= 6)
            return "Q2";
        else if (month <= 9)
            return "Q3";
        else
            return "Q4";
    }

    private void writeAudit(
            VunnoResponse response,
            String actionType,
            Map<String, Object> previousValues,
            Map<String, Object> newValues,
            AuditStatus explicitStatus, // optional
            String changeReason,
            UserDetails userDetails) {

        VunnoAuditLog log = new VunnoAuditLog();
        log.setVunnoResponseId(response.getId());
        log.setActionType(actionType);

        log.setPreviousValues(toJson(previousValues));
        log.setNewValues(toJson(newValues));

        // Status resolution logic
        if (explicitStatus != null) {
            log.setPreviousStatus(explicitStatus.previousStatus());
            log.setNewStatus(explicitStatus.newStatus());
        } else {
            log.setPreviousStatus(extractStatus(previousValues));
            log.setNewStatus(extractStatus(newValues));
        }

        ZonedDateTime nowInIndia = ZonedDateTime.now(INDIA_TIMEZONE);
        log.setChangedAt(nowInIndia.toLocalDateTime());

        log.setChangedBy(userDetails.getUsername());
        log.setChangedByRole(getRoleFromUserDetails(userDetails));
        log.setChangeReason(changeReason);

        log.setChangeDescription(
                actionType + " performed. Updated fields: " + newValues.keySet());

        vunnoAuditRepository.save(log);
    }

    private String extractStatus(Map<String, Object> values) {
        if (values == null)
            return null;

        if (values.containsKey("status") && values.get("status") != null) {
            return values.get("status").toString();
        }

        if (values.containsKey("leaveCategory") && values.get("leaveCategory") != null) {
            return values.get("leaveCategory").toString();
        }

        return null;
    }

    private String toJson(Map<String, Object> map) {
        try {
            return new ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    @Transactional
    public String leaveCategoryUpdate(CategoryUpdateDto dto, UserDetails userDetails) {

        try {
            Long requestId = dto.getId();
            String newCategory = dto.getCategory();
            String comment = dto.getReason();

            // --- VALIDATIONS ---
            if (requestId == null)
                throw new IllegalArgumentException("Request ID is required");

            if (newCategory == null ||
                    (!newCategory.equalsIgnoreCase("PLANNED")
                            && !newCategory.equalsIgnoreCase("UNPLANNED"))) {
                throw new IllegalArgumentException("Invalid leave category");
            }

            if (comment == null || comment.trim().isEmpty()) {
                throw new IllegalArgumentException("Comment is required for category update");
            }

            // --- FETCH EXISTING REQUEST ---
            VunnoResponse request = vunnoResponseRepository.findById(requestId)
                    .orElseThrow(() -> new EntityNotFoundException("Leave request not found"));

            String oldCategory = request.getLeaveCategory();

            // --- NO CHANGE ---
            if (oldCategory != null && oldCategory.equalsIgnoreCase(newCategory)) {
                throw new IllegalArgumentException(
                        "Category is already set to: " + newCategory);
            }

            // --- UPDATE CATEGORY ---
            request.setLeaveCategory(newCategory);
            vunnoResponseRepository.save(request);

            // --- PREPARE AUDIT MAPS ---
            Map<String, Object> previousValues = new HashMap<>();
            previousValues.put("leaveCategory", oldCategory);

            Map<String, Object> newValues = new HashMap<>();
            newValues.put("leaveCategory", newCategory);

            // Set the Previous and New status as well

            // --- AUDIT LOG ---
            writeAudit(
                    request,
                    "CATEGORY_UPDATE",
                    previousValues,
                    newValues,
                    new AuditStatus(oldCategory, newCategory),
                    comment,
                    userDetails);

            logger.info("Leave category updated for ID {} by {}", requestId, userDetails.getUsername());
            return "Leave category updated successfully";

        } catch (IllegalArgumentException | EntityNotFoundException e) {
            logger.error("Validation error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error in leaveCategoryUpdate", e);
            throw new RuntimeException("Failed to update leave category");
        }
    }

    @Transactional
    public String requestingLeave(VunnoRequestDto requestDto) {
        try {
            logger.info("Fetching Details of lead-Manager");
            List<VunnoMgmtDto> MgmtDetails = getLeadManagerDetails(requestDto.getLdap());

            if (MgmtDetails == null || MgmtDetails.isEmpty()) {
                throw new RuntimeException("Lead manager details not found for LDAP: " + requestDto.getLdap());
            }

            for (VunnoMgmtDto dto : MgmtDetails) {
                System.out.println("MgmtDetails " + dto.getLead() + " " + dto.getManager());
            }

            VunnoMgmtDto dto = MgmtDetails.get(0);
            String program = dto.getProgramAlignment();
            String team = dto.getTeam();
            String name = dto.getName();
            String approvingLead = requestDto.getApprovingLead();
            String ldap = requestDto.getLdap();
            String applicationType = requestDto.getApplicationType();
            String leaveType = "Leave".equalsIgnoreCase(applicationType) ? requestDto.getLeaveType() : "";
            String lvwfhDuration = requestDto.getLvWfhDuration();
            String backupInfo = requestDto.getBackupInfo();
            String oooProof = requestDto.getOooProof();
            String timesheetProof = requestDto.getTimesheetProof();

            // Format dates
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate fromDate = LocalDate.parse(requestDto.getStartDate(), dateFormatter);
            LocalDate toDate = LocalDate.parse(requestDto.getEndDate(), dateFormatter);

            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime startTime = requestDto.getStartDateTime() != null
                    ? LocalTime.parse(requestDto.getStartDateTime(), timeFormatter)
                    : null;

            LocalTime endTime = requestDto.getEndDateTime() != null
                    ? LocalTime.parse(requestDto.getEndDateTime(), timeFormatter)
                    : null;

            // Fetch Employee
            Employee employee = employeeRepository.findByLdap(ldap)
                    .orElseThrow(() -> new RuntimeException("Employee not found for LDAP: " + ldap));

            String shiftCode = employee.getShift();

            ShiftDetails shift = shiftDetailsRepository.findByCode(shiftCode)
                    .orElseThrow(() -> new RuntimeException("Shift not found for code: " + shiftCode));

            LocalTime shiftStart = LocalTime.parse(shift.getStartTime());

            // Date and Time according to indian standards
            ZonedDateTime nowInIndia = ZonedDateTime.now(INDIA_TIMEZONE);
            LocalDateTime requestTimestamp = nowInIndia.toLocalDateTime();

            LocalDateTime leaveStartDateTime = LocalDateTime.of(fromDate, shiftStart);

            String leaveCategory = "";

            if ("Leave".equalsIgnoreCase(applicationType)) {
                long hoursDiff = ChronoUnit.HOURS.between(requestTimestamp, leaveStartDateTime);
                leaveCategory = hoursDiff < 24 ? "UNPLANNED" : "PLANNED";
            }

            List<Double> initialCounts = getAllCountOfLeaveWFH(ldap);

            // Create VunnoResponse entity
            VunnoResponse response = new VunnoResponse();
            response.setTimestamp(requestTimestamp);
            response.setTeam(team);
            response.setRequestorName(name);
            response.setApprover(approvingLead);
            response.setFromDate(fromDate);
            response.setToDate(toDate);
            response.setApplicationType(applicationType);
            response.setLeaveType(leaveType);
            response.setDuration(lvwfhDuration);
            response.setProgram(program);
            response.setStatus(requestDto.getStatus()); // Should be "PENDING"
            response.setBackup(backupInfo);
            response.setOrgScreenshot(cleanScreenshotUrl(oooProof));
            response.setReason(requestDto.getReason()); // save mandatory reason
            response.setDocumentPath(requestDto.getDocumentPath());
            response.setTimesheetScreenshot(cleanScreenshotUrl(timesheetProof));
            response.setEmployee(employee);
            response.setStartTime(startTime);
            response.setEndTime(endTime);
            response.setShiftCodeAtRequestTime(shiftCode);
            response.setLeaveCategory(leaveCategory);

            // Save dayConfigurations for mixed leave types
            if (requestDto.getDayConfigurations() != null && !requestDto.getDayConfigurations().isEmpty()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    String dayConfigJson = mapper.writeValueAsString(requestDto.getDayConfigurations());
                    response.setDayConfigurations(dayConfigJson);
                    logger.info("Saved day configurations for mixed leave type: {}", dayConfigJson);
                } catch (Exception e) {
                    logger.error("Failed to serialize day configurations", e);
                }
            }

            // Validation: block overlapping PENDING Leave requests BEFORE saving
            String validationError = validateNoOverlappingPendingLeave(response);
            if (validationError != null) {
                return "Leave Request Failed: " + validationError;
            }

            // Save to database
            vunnoResponseRepository.save(response);
            logger.info("Leave request saved successfully for " + ldap);

            // Generate time entries ONLY for Leave requests (NOT for WFH/CompOff)
            // Strict transactionality: If this fails, the entire transaction rolls back.
            List<TimeEntry> generatedEntries = timeEntryAutoGenerationService.generateTimeEntriesForLeave(response);
            if (!generatedEntries.isEmpty()) {
                logger.info("Generated {} time entries for leave request ID: {}",
                        generatedEntries.size(), response.getId());
            }

            // Changed logic, will now deduct the leaves now after leave is requested and
            // check
            // for business days and also deducting google holidays.
            updateUsageOrWfh(response, requestDto);

            // Recalculate balances AFTER deduction
            List<Double> updatedCounts = getAllCountOfLeaveWFH(ldap);

            // Merge: Balances from Updated (Deducted), Taken History from Initial
            // (Excluding this request)
            List<Double> finalCounts = List.of(
                    updatedCounts.get(0), // SL (Deducted)
                    updatedCounts.get(1), // CL (Deducted)
                    updatedCounts.get(2), // EL (Deducted)
                    updatedCounts.get(3), // Total Balance (Deducted)
                    initialCounts.get(4), // Total WFH (History)
                    initialCounts.get(5), // Quarterly WFH (History)
                    initialCounts.get(6) // Total Leave Taken (History)
            );

            // Triggering mail to notify managers and all leads under manager
            notificationService.triggerRequestNotification(requestDto, dto, finalCounts);

            return "Requested leave successfully saved into database";

        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            logger.error("An error occurred in requestingLeave method", e);
            return "Leave Request Failed: " + e.getMessage();
        }
    }

    // Fetch Leads Information when passed Ldap
    public List<VunnoMgmtDto> getLeadManagerDetails(String ldap) {
        if (ldap == null || ldap.isEmpty()) {
            throw new IllegalArgumentException("LDAP Cannot be Empty");
        }

        Employee employee = employeeRepository.findByLdap(ldap)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        String fullName = employee.getFirstName() + " " + employee.getLastName();

        return Collections.singletonList(new VunnoMgmtDto(
                employee.getLdap(),
                fullName,
                employee.getLevel(),
                employee.getEmail(),
                employee.getPnseProgram(),
                employee.getTeam(),
                employee.getLead(),
                employee.getProgramManager(),
                employee.getShift()));
    }

    public List<Double> getAllCountOfLeaveWFH(String ldap) {
        int currentYear = Year.now().getValue();
        String currentQuarter = getCurrentQuarter();
        double finalSL = calculateEffectiveBalance(ldap, "HR-SL", "Sick Leave");
        double finalCL = calculateEffectiveBalance(ldap, "HR-CL", "Casual Leave");
        double finalEL = calculateEffectiveBalance(ldap, "HR-EL", "Earned Leave");

        // Total WFH used
        double totalWFH = leaveUsageLogRepository.sumDaysTakenByLdapAndLeaveType(ldap, "WFH");

        // WFH used in current quarter
        double quarterlyWFH = leaveUsageLogRepository.sumDaysTakenByLdapAndLeaveTypeAndQuarter(
                ldap, "WFH", currentQuarter, String.valueOf(currentYear));

        double totalLeaveTaken = getTotalLeavesTakenInQuarter(ldap, currentYear, currentQuarter);

        System.out.print("Year " + currentYear + " Quarter " + currentQuarter + "leavetakenQu " + totalLeaveTaken);

        return List.of(finalSL, finalCL, finalEL, finalSL + finalCL + finalEL, totalWFH, quarterlyWFH, totalLeaveTaken);
    }

    private double calculateEffectiveBalance(String ldap, String hrType, String usageType) {
        // Step 1: Find latest HR-uploaded balance for this leave type
        Optional<LeaveBalance> latestBalanceOpt = leaveBalanceRepository
                .findTopByEmployee_LdapAndLeaveTypeOrderByEffectiveFromDesc(ldap, hrType);

        if (latestBalanceOpt.isEmpty())
            return 0;

        LeaveBalance latestBalance = latestBalanceOpt.get();
        double hrBalance = latestBalance.getBalance();
        LocalDate effectiveFrom = latestBalance.getEffectiveFrom();

        // Step 2: Sum usage from usage logs only after effectiveFrom
        Double used = leaveUsageLogRepository
                .sumDaysTakenByLdapAndLeaveTypeSinceDate(ldap, usageType, effectiveFrom);

        double usedLeave = used != null ? used : 0;

        // Step 3: Subtract
        return Math.max(0, hrBalance - usedLeave);
    }

    // Helper method to get current quarter as a string (Q1, Q2, Q3, or Q4)
    public String getCurrentQuarter() {
        LocalDate date = LocalDate.now();
        int month = date.getMonthValue();
        if (month >= 1 && month <= 3) {
            return "Q1";
        } else if (month >= 4 && month <= 6) {
            return "Q2";
        } else if (month >= 7 && month <= 9) {
            return "Q3";
        } else {
            return "Q4";
        }

    }

    public double getTotalLeavesTakenInQuarter(String ldap, int year, String quarter) {
        LocalDate startDate, endDate;
        switch (quarter) {
            case "Q1" -> {
                startDate = LocalDate.of(year, 1, 1);
                endDate = LocalDate.of(year, 3, 31);
            }
            case "Q2" -> {
                startDate = LocalDate.of(year, 4, 1);
                endDate = LocalDate.of(year, 6, 30);
            }
            case "Q3" -> {
                startDate = LocalDate.of(year, 7, 1);
                endDate = LocalDate.of(year, 9, 30);
            }
            case "Q4" -> {
                startDate = LocalDate.of(year, 10, 1);
                endDate = LocalDate.of(year, 12, 31);
            }
            default -> throw new IllegalArgumentException("Invalid quarter: " + quarter);
        }

        List<VunnoResponse> approvedLeaves = vunnoResponseRepository.findApprovedLeavesByLdap(ldap, startDate, endDate);
        double totalDays = 0.0;

        // Create mapper once outside the loop for efficiency
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        for (VunnoResponse resp : approvedLeaves) {
            double currentLeaveDays = 0.0;
            boolean useFallback = true;

            // Process day configurations if available
            if (resp.getDayConfigurations() != null && !resp.getDayConfigurations().isEmpty()) {
                try {
                    com.vbs.capsAllocation.dto.DayConfigurationDto[] dayConfigs = mapper.readValue(
                            resp.getDayConfigurations(),
                            com.vbs.capsAllocation.dto.DayConfigurationDto[].class);

                    // Reset count for this request to ensure no double counting if we succeed
                    // (though it started at 0.0)
                    double jsonCalculatedDays = 0.0;

                    for (com.vbs.capsAllocation.dto.DayConfigurationDto dc : dayConfigs) {
                        LocalDate date = LocalDate.parse(dc.getDate());
                        // Only count if date is within the quarter
                        if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                            // Check if it's a business day
                            if (countBusinessDays(date, date) > 0) {
                                jsonCalculatedDays += dc.getDurationValue();
                            }
                        }
                    }

                    // If we reached here, JSON parsing and processing succeeded
                    currentLeaveDays = jsonCalculatedDays;
                    useFallback = false;

                } catch (Exception e) {
                    logger.error(
                            "Error parsing day configs for total calculation for request ID {}. Falling back to iterative calculation.",
                            resp.getId(), e);
                    // useFallback remains true, currentLeaveDays remains 0.0 (or is overwritten by
                    // fallback)
                }
            }

            if (useFallback) {
                // Fallback: iterate dates logic
                currentLeaveDays = calculateDaysInQuarterIteratively(startDate, endDate, resp);
            }

            totalDays += currentLeaveDays;
        }
        return totalDays;
    }

    private double calculateDaysInQuarterIteratively(LocalDate qStart, LocalDate qEnd, VunnoResponse resp) {
        double days = 0.0;
        LocalDate current = resp.getFromDate();
        LocalDate to = resp.getToDate();
        String durationStr = resp.getDuration();
        double dailyVal = "Full Day".equals(durationStr) || "Multiple Days".equals(durationStr) ? 1.0 : 0.5;

        while (!current.isAfter(to)) {
            if (!current.isBefore(qStart) && !current.isAfter(qEnd)) {
                if (countBusinessDays(current, current) > 0) {
                    days += dailyVal;
                }
            }
            current = current.plusDays(1);
        }
        return days;
    }

    public String deleteLeaveRequestWithPermission(Long id, UserDetails userDetails) {
        String loggedInUser = userDetails.getUsername();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        Optional<VunnoResponse> optional = vunnoResponseRepository.findById(id);
        if (optional.isEmpty()) {
            throw new IllegalArgumentException("Leave request not found.");
        }

        VunnoResponse request = optional.get();

        // Restore balance by reverting leave/WFH usage logs
        revertLeaveOrWfh(request);

        ZonedDateTime nowInIndia = ZonedDateTime.now(INDIA_TIMEZONE);
        LocalDateTime auditTimeStamp = nowInIndia.toLocalDateTime();

        request.setStatus("DELETED");
        request.setDeletedAt(auditTimeStamp);
        vunnoResponseRepository.save(request);

        boolean auditLogged = logDeleteAction(request, loggedInUser, roles);
        if (!auditLogged) {
            throw new IllegalStateException("Failed to log audit for deletion.");
        }
        return "Leave Request Successfully Deleted!";
    }

    public boolean logDeleteAction(VunnoResponse request, String loggedInUser, List<String> roles) {
        try {
            ZonedDateTime nowInIndia = ZonedDateTime.now(INDIA_TIMEZONE);
            LocalDateTime auditTimeStamp = nowInIndia.toLocalDateTime();
            System.out.println("ROLES DELETE " + roles);
            VunnoAuditLog auditLog = new VunnoAuditLog();
            auditLog.setVunnoResponseId(request.getId());
            auditLog.setActionType("DELETE");
            auditLog.setPreviousStatus("PENDING");
            auditLog.setNewStatus("DELETED");
            auditLog.setChangedBy(loggedInUser);
            auditLog.setChangedByRole(String.join(",", roles));
            auditLog.setChangedAt(auditTimeStamp);
            auditLog.setChangeDescription("Deleted by LoggedIn User");

            vunnoAuditRepository.save(auditLog);
            return true;
        } catch (Exception e) {
            LoggerUtil.logError("Error logging audit record: {}", e.getMessage(), e);
            return false;
        }
    }

    public String updateLeaveRequestWithPermission(Long id, VunnoRequestDto updateRequest, UserDetails userDetails) {
        String loggedInUser = userDetails.getUsername();

        Optional<VunnoResponse> optional = vunnoResponseRepository.findById(id);
        if (optional.isEmpty()) {
            throw new IllegalArgumentException("Leave request not found.");
        }

        System.out.println("UpdatedRequest " + updateRequest);

        VunnoResponse request = optional.get();

        // Update fields based on VunnoRequestDto
        request.setFromDate(OffsetDateTime.parse(updateRequest.getStartDate()).toLocalDate());
        request.setToDate(OffsetDateTime.parse(updateRequest.getEndDate()).toLocalDate());

        // Application type and duration
        request.setApplicationType(updateRequest.getApplicationType());
        request.setDuration(updateRequest.getLvWfhDuration());

        // Leave type logic
        if ("Leave".equalsIgnoreCase(updateRequest.getApplicationType())) {
            request.setLeaveType(updateRequest.getLeaveType());
            request.setOrgScreenshot(updateRequest.getOooProof()); // ← always overwrite
            request.setTimesheetScreenshot(updateRequest.getTimesheetProof()); // ← always overwrite
            request.setBackup(updateRequest.getBackupInfo()); // ← always overwrite
            request.setReason(null); // clear WFH reason
            request.setDocumentPath(null); // clear WFH document
        } else {
            request.setLeaveType(null); // clear leaveType
            request.setReason(updateRequest.getReason()); // ← always overwrite
            request.setTimesheetScreenshot(updateRequest.getTimesheetProof()); // ← always overwrite
            request.setOrgScreenshot(updateRequest.getOooProof()); // ← always overwrite
            request.setBackup(updateRequest.getBackupInfo()); // ← also needed here

            if (updateRequest.getDocumentPath() != null && !updateRequest.getDocumentPath().isEmpty()) {
                request.setDocumentPath(updateRequest.getDocumentPath()); // New file
            } else {
                request.setDocumentPath(request.getDocumentPath()); // Keep old
            }
        }

        if (updateRequest.getStatus() != null) {
            request.setStatus(updateRequest.getStatus());
        }
        if (updateRequest.getApprovingLead() != null) {
            request.setApprover(updateRequest.getApprovingLead());
        }
        // Optionally update other fields (e.g., backup info, ooo proof, timesheet) if
        // needed
        vunnoResponseRepository.save(request);

        // Audit log
        boolean auditLogged = logUpdateAction(
                request,
                loggedInUser,
                userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()),
                request.getApplicationType() + " Request updated");

        if (!auditLogged) {
            throw new IllegalStateException("Failed to log audit for update.");
        }

        return "Request updated successfully.";
    }

    public boolean logUpdateAction(
            VunnoResponse updatedRequest,
            String changedBy,
            List<String> roles,
            String reason) {
        try {
            // Fetch the previous state from DB before update
            Optional<VunnoResponse> existingOptional = vunnoResponseRepository.findById(updatedRequest.getId());
            if (existingOptional.isEmpty()) {
                return false;
            }

            VunnoResponse previous = existingOptional.get();

            // Prepare previous and new values as JSON-like maps
            Map<String, Object> previousValues = new HashMap<>();
            Map<String, Object> newValues = new HashMap<>();

            previousValues.put("fromDate", previous.getFromDate());
            previousValues.put("toDate", previous.getToDate());
            previousValues.put("applicationType", previous.getApplicationType());
            previousValues.put("duration", previous.getDuration());
            previousValues.put("leaveType", previous.getLeaveType());
            previousValues.put("backup", previous.getBackup());
            previousValues.put("documentPath", previous.getDocumentPath());

            newValues.put("documentPath", updatedRequest.getDocumentPath());
            newValues.put("fromDate", updatedRequest.getFromDate());
            newValues.put("toDate", updatedRequest.getToDate());
            newValues.put("applicationType", updatedRequest.getApplicationType());
            newValues.put("duration", updatedRequest.getDuration());
            newValues.put("leaveType", updatedRequest.getLeaveType());
            newValues.put("backup", updatedRequest.getBackup());

            // Convert maps to JSON strings
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());

            String previousJson = mapper.writeValueAsString(previousValues);
            String newJson = mapper.writeValueAsString(newValues);

            VunnoAuditLog auditLog = new VunnoAuditLog();
            ZonedDateTime nowInIndia = ZonedDateTime.now(INDIA_TIMEZONE);
            LocalDateTime auditTimeStamp = nowInIndia.toLocalDateTime();
            auditLog.setVunnoResponseId(updatedRequest.getId());
            auditLog.setActionType("EDIT");
            auditLog.setPreviousStatus("PENDING");
            auditLog.setNewStatus(updatedRequest.getStatus());
            auditLog.setChangedBy(changedBy);
            auditLog.setChangedByRole(String.join(",", roles));
            auditLog.setChangedAt(auditTimeStamp);
            auditLog.setChangeReason(reason);
            auditLog.setPreviousValues(previousJson);
            auditLog.setNewValues(newJson);

            vunnoAuditRepository.save(auditLog);
            return true;

        } catch (Exception e) {
            LoggerUtil.logError("Error logging update audit record: {}", e.getMessage(), e);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public List<VunnoRequestDto> getProcessedRequestsForApproval(
            UserDetails userDetails,
            String statusFilter,
            boolean directOnly,
            LocalDate startDate,
            LocalDate endDate) {

        String ldap = userDetails.getUsername();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        List<String> targetLdaps = new ArrayList<>();
        boolean isLead = false;

        if (roles.contains("ROLE_ADMIN_OPS_MANAGER")) {
            targetLdaps = employeeRepository.findAllLdaps();

        } else if (roles.contains("ROLE_MANAGER")) {
            targetLdaps = directOnly
                    ? employeeRepository.findLdapsByManager(ldap)
                    : employeeRelationRepository.findLdapsBySecondaryManager(ldap);

        } else if (roles.contains("ROLE_LEAD")) {
            isLead = true;
            targetLdaps = directOnly
                    ? employeeRepository.findLdapsByLead(ldap)
                    : employeeRelationRepository.findLdapsBySecondaryLead(ldap);
        }

        if (targetLdaps.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> statuses = statusFilter != null
                ? List.of(statusFilter)
                : List.of("APPROVED", "REJECTED", "REVOKED");

        List<VunnoRequestDto> responses = vunnoResponseRepository.findProcessedDtos(
                targetLdaps, startDate, endDate, statuses);

        // Leads should NOT see WFH entries
        if (isLead) {
            responses = responses.stream()
                    .filter(r -> !"Work From Home".equalsIgnoreCase(r.getApplicationType()))
                    .toList();
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public List<VunnoRequestDto> getRequestsForApproval(
            UserDetails userDetails,
            boolean directOnly,
            LocalDate startDate,
            LocalDate endDate) {

        String approverLdap = userDetails.getUsername();
        List<String> employeeLdaps = resolveEmployeeLdaps(userDetails, approverLdap, directOnly);

        if (employeeLdaps.isEmpty()) {
            return Collections.emptyList();
        }

        List<VunnoRequestDto> responses = vunnoResponseRepository.findPendingDtos(employeeLdaps, startDate, endDate,
                "PENDING");

        // If LEAD, filter out WFH requests
        boolean isLead = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LEAD"));

        if (isLead) {
            responses = responses.stream()
                    .filter(r -> !"Work From Home".equalsIgnoreCase(r.getApplicationType()))
                    .toList();
        }

        return responses;
    }

    private List<String> resolveEmployeeLdaps(UserDetails userDetails, String approverLdap, boolean directOnly) {
        if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_LEAD"))) {
            return directOnly
                    ? employeeRepository.findLdapsByLead(approverLdap)
                    : employeeRelationRepository.findLdapsBySecondaryLead(approverLdap);
        }
        if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"))) {
            return directOnly
                    ? employeeRepository.findLdapsByManager(approverLdap)
                    : employeeRelationRepository.findLdapsBySecondaryManager(approverLdap);
        }
        // Admin Ops → see everyone
        return employeeRepository.findAll().stream().map(Employee::getLdap).toList();
    }

    private String getRoleFromUserDetails(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("UNKNOWN");
    }

    @Transactional
    public String rejectRequest(VunnoRequestDto requestDto, UserDetails userDetails) {
        VunnoResponse response = vunnoResponseRepository.findById(requestDto.getId())
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!"PENDING".equalsIgnoreCase(response.getStatus())) {
            return "Only PENDING requests can be rejected.";
        }

        // reverse reserved usage
        revertLeaveOrWfh(response);

        String previousStatus = response.getStatus();
        response.setStatus("REJECTED");
        response.setApprover(userDetails.getUsername());
        vunnoResponseRepository.save(response);

        // Auto-delete linked time entries (prevents conflicts on re-request)
        try {
            timeEntryAutoGenerationService.deleteTimeEntriesForLeave(response.getId());
            logger.info("Deleted time entries for rejected leave request ID: {}", response.getId());
        } catch (Exception e) {
            logger.error("Failed to delete time entries for leave request ID: {}", response.getId(), e);
            // Continue with leave rejection even if time entry deletion fails
        }

        writeAudit(
                response,
                "REJECT",
                Map.of("status", previousStatus),
                Map.of("status", "REJECTED"),
                null,
                requestDto.getComment(),
                userDetails);

        // Send rejection notification email
        notificationService.triggerRejectionNotification(requestDto, response, userDetails.getUsername());

        return "Request rejected successfully. Associated time entries have been deleted.";
    }

    @Transactional
    public String revokeRequest(VunnoRequestDto requestDto, UserDetails userDetails) {
        VunnoResponse response = vunnoResponseRepository.findById(requestDto.getId())
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!"APPROVED".equalsIgnoreCase(response.getStatus())) {
            return "Only APPROVED requests can be revoked.";
        }

        // reverse usage first
        revertLeaveOrWfh(response);

        String previousStatus = response.getStatus();
        response.setStatus("REVOKED");
        response.setApprover(userDetails.getUsername());
        vunnoResponseRepository.save(response);

        // Delete auto-generated time entries for this leave request
        try {
            timeEntryAutoGenerationService.deleteTimeEntriesForLeave(response.getId());
            logger.info("Deleted time entries for revoked leave request ID: {}", response.getId());
        } catch (Exception e) {
            logger.error("Failed to delete time entries for revoked leave request ID: {}", response.getId(), e);
            // Continue with revocation even if time entry deletion fails
        }

        writeAudit(
                response,
                "REVOKE",
                Map.of("status", previousStatus),
                Map.of("status", "REVOKED"),
                null,
                requestDto.getComment(),
                userDetails);

        // Send revoke notification email
        notificationService.triggerRevokeNotification(requestDto, response, userDetails.getUsername());

        return "Request revoked successfully.";
    }

    @Transactional
    public String approveRequest(VunnoRequestDto requestDto, UserDetails userDetails) {

        List<VunnoMgmtDto> MgmtDetails = getLeadManagerDetails(requestDto.getLdap());
        VunnoMgmtDto dto = MgmtDetails.get(0);

        VunnoResponse response = vunnoResponseRepository.findById(requestDto.getId())
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!"PENDING".equalsIgnoreCase(response.getStatus())) {
            return "Only PENDING requests can be approved.";
        }

        String previousStatus = response.getStatus();
        String role = getRoleFromUserDetails(userDetails);
        response.setStatus("APPROVED");
        response.setApprover(userDetails.getUsername());
        vunnoResponseRepository.save(response);

        // Auto-approve linked time entries
        int timeEntriesApproved = 0;
        try {
            timeEntriesApproved = approveLinkedTimeEntries(response);
            if (timeEntriesApproved > 0) {
                logger.info("Approved {} time entries for leave request ID: {}", timeEntriesApproved, response.getId());
            }
        } catch (Exception e) {
            logger.error("Failed to approve time entries for leave request ID: {}", response.getId(), e);
            // Continue with leave approval even if time entry approval fails
        }

        writeAudit(
                response,
                "APPROVE",
                Map.of("status", previousStatus),
                Map.of("status", "APPROVED"),
                null,
                requestDto.getComment(), // reason/comment from popup
                userDetails);

        notificationService.triggerApprovalNotification(requestDto, response, role, dto);

        if (timeEntriesApproved > 0) {
            return "Request approved successfully. " + timeEntriesApproved + " time entries approved.";
        }
        return "Request approved successfully.";
    }

    private String validateNoOverlappingPendingLeave(VunnoResponse response) {
        if (!"Leave".equalsIgnoreCase(response.getApplicationType())) {
            return null; // Only block for Leave, not WFH
        }

        Employee employee = response.getEmployee();
        LocalDate fromDate = response.getFromDate();
        LocalDate toDate = response.getToDate();

        if (employee == null || fromDate == null || toDate == null) {
            throw new LeaveBalanceException("Employee and date range are required for validation.");
        }

        List<String> conflictStatuses = List.of("PENDING");
        Long currentId = response.getId(); // null for new requests

        boolean existsOverlap = vunnoResponseRepository
                .existsOverlappingLeave(employee.getId(), fromDate, toDate, conflictStatuses, currentId);

        if (existsOverlap) {
            return "You already have a pending leave request overlapping this date range.";
        }
        return null;
    }

    private void updateUsageOrWfh(VunnoResponse response, VunnoRequestDto requestDto) {
        Employee employee = response.getEmployee();
        String applicationType = response.getApplicationType(); // "Leave" or "Work From Home"
        LocalDate fromDate = response.getFromDate();
        LocalDate toDate = response.getToDate();

        if (employee == null || applicationType == null) {
            throw new LeaveBalanceException("Employee and application type are required.");
        }

        if (fromDate == null) {
            throw new LeaveBalanceException("From date is required.");
        }

        int currentYear = fromDate.getYear();
        String currentQuarter = getQuarter(fromDate);

        // Validation: block overlapping PENDING Leave requests
        // validateNoOverlappingPendingLeave(response); // Moved to requestingLeave
        // before save

        if ("Leave".equalsIgnoreCase(applicationType)) {
            // ---- LEAVE LOGIC WITH BUSINESS DAYS ----
            String duration = response.getDuration(); // "Full Day", "Half Day AM", "Half Day PM", "Multiple Days"

            if (duration == null) {
                throw new LeaveBalanceException("Duration is required for Leave.");
            }

            String leaveType = response.getLeaveType();
            if (leaveType == null || leaveType.isBlank()) {
                throw new LeaveBalanceException("Leave type is required for Leave application.");
            }

            // PRIORITY: Check dayConfigurations first!
            if (requestDto.getDayConfigurations() != null && !requestDto.getDayConfigurations().isEmpty()) {
                logger.info("Processing day configurations for leave request (Size: {})",
                        requestDto.getDayConfigurations().size());

                for (DayConfigurationDto dayConfig : requestDto.getDayConfigurations()) {
                    LocalDate configDate = LocalDate.parse(dayConfig.getDate());

                    // Only process business days
                    long isBusinessDay = countBusinessDays(configDate, configDate);
                    if (isBusinessDay > 0) {
                        LeaveUsageLog log = new LeaveUsageLog();
                        log.setEmployee(employee);
                        log.setLeaveType(dayConfig.getLeaveType()); // Per-day leave type
                        log.setDaysTaken(dayConfig.getDurationValue()); // 1.0 or 0.5
                        log.setLeaveDate(configDate);
                        log.setYear(configDate.getYear());
                        log.setQuarter(getQuarter(configDate));
                        leaveUsageLogRepository.save(log);
                    }
                }
            } else {
                switch (duration) {
                    case "Full Day" -> {
                        long businessDays = countBusinessDays(fromDate, fromDate);
                        if (businessDays == 0) {
                            throw new LeaveBalanceException(
                                    "Selected date falls on a weekend or holiday. Cannot apply full-day leave.");
                        }

                        // Single day log
                        LeaveUsageLog log = new LeaveUsageLog();
                        log.setEmployee(employee);
                        log.setLeaveType(leaveType);
                        log.setDaysTaken(1.0);
                        log.setLeaveDate(fromDate);
                        log.setYear(fromDate.getYear());
                        log.setQuarter(getQuarter(fromDate));
                        leaveUsageLogRepository.save(log);
                    }
                    case "Half Day AM", "Half Day PM", "First Half", "Second Half" -> {
                        long businessDays = countBusinessDays(fromDate, fromDate);
                        if (businessDays == 0) {
                            throw new LeaveBalanceException(
                                    "Selected date falls on a weekend or holiday. Cannot apply half-day leave.");
                        }

                        // Single day log
                        LeaveUsageLog log = new LeaveUsageLog();
                        log.setEmployee(employee);
                        log.setLeaveType(leaveType);
                        log.setDaysTaken(0.5);
                        log.setLeaveDate(fromDate);
                        log.setYear(fromDate.getYear());
                        log.setQuarter(getQuarter(fromDate));
                        leaveUsageLogRepository.save(log);
                    }
                    case "Multiple Days" -> {
                        if (toDate == null) {
                            throw new LeaveBalanceException("To date is required for Multiple Days leave.");
                        }
                        long businessDays = countBusinessDays(fromDate, toDate);
                        if (businessDays <= 0) {
                            throw new LeaveBalanceException(
                                    "Selected range contains no working days (only weekends/holidays).");
                        }
                        // Check if we have per-day configurations (for mixed leave types)
                        if (requestDto != null && requestDto.getDayConfigurations() != null
                                && !requestDto.getDayConfigurations().isEmpty()) {
                            // MIXED LEAVE TYPE: Use per-day configurations
                            logger.info("Processing mixed leave type request with {} day configurations",
                                    requestDto.getDayConfigurations().size());

                            for (DayConfigurationDto dayConfig : requestDto.getDayConfigurations()) {
                                LocalDate configDate = LocalDate.parse(dayConfig.getDate());

                                // Only process business days
                                long isBusinessDay = countBusinessDays(configDate, configDate);
                                if (isBusinessDay > 0) {
                                    LeaveUsageLog log = new LeaveUsageLog();
                                    log.setEmployee(employee);
                                    log.setLeaveType(dayConfig.getLeaveType()); // Per-day leave type
                                    log.setDaysTaken(dayConfig.getDurationValue()); // 1.0 or 0.5
                                    log.setLeaveDate(configDate);
                                    log.setYear(configDate.getYear());
                                    log.setQuarter(getQuarter(configDate));
                                    leaveUsageLogRepository.save(log);

                                    logger.debug("Saved usage log for {} on {} with {} days of {}",
                                            employee.getLdap(), configDate, dayConfig.getDurationValue(),
                                            dayConfig.getLeaveType());
                                }
                            }
                        } else {
                            // SINGLE LEAVE TYPE: Original logic
                            LocalDate currentDate = fromDate;
                            while (!currentDate.isAfter(toDate)) {
                                long isBusinessDay = countBusinessDays(currentDate, currentDate);
                                if (isBusinessDay > 0) {
                                    LeaveUsageLog log = new LeaveUsageLog();
                                    log.setEmployee(employee);
                                    log.setLeaveType(leaveType);
                                    log.setDaysTaken(1.0);
                                    log.setLeaveDate(currentDate);
                                    log.setYear(currentDate.getYear());
                                    log.setQuarter(getQuarter(currentDate));
                                    leaveUsageLogRepository.save(log);
                                }
                                currentDate = currentDate.plusDays(1);
                            }
                        }
                    }
                    default -> throw new LeaveBalanceException("Invalid leave duration: " + duration);
                }
            }

        } else if ("Work From Home".equalsIgnoreCase(applicationType)) {
            // ---- WFH LOGIC (currently calerequestingLeavendar-based; change to
            // businessDays if policy
            // needs) ----
            double daysTaken;
            String duration = response.getDuration();

            if (duration == null) {
                throw new LeaveBalanceException("Duration is required for Work From Home.");
            }

            switch (duration) {
                case "Full Day" -> daysTaken = 1.0;
                case "Half Day AM", "Half Day PM", "First Half", "Second Half" -> daysTaken = 0.5;
                case "Multiple Days" -> {
                    if (fromDate == null || toDate == null) {
                        throw new LeaveBalanceException("FromDate or ToDate missing for Multiple Days WFH.");
                    }
                    long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
                    if (days <= 0) {
                        throw new LeaveBalanceException("Invalid WFH date range.");
                    }
                    // If you ALSO want WFH to ignore weekends/holidays:
                    long businessDays = countBusinessDays(fromDate, toDate);
                    daysTaken = (double) businessDays;
                }
                default -> throw new LeaveBalanceException("Invalid WFH duration: " + duration);
            }

            LeaveUsageLog wfhLog = new LeaveUsageLog();
            wfhLog.setEmployee(employee);
            wfhLog.setLeaveType("WFH");
            wfhLog.setDaysTaken(daysTaken);
            wfhLog.setLeaveDate(fromDate);
            wfhLog.setYear(currentYear);
            wfhLog.setQuarter(currentQuarter);
            // wfhLog.setRequestId(response.getId());

            leaveUsageLogRepository.save(wfhLog);

        } else {
            throw new LeaveBalanceException("Unknown application type: " + applicationType);
        }
    }

    public class LeaveBalanceException extends RuntimeException {
        public LeaveBalanceException(String message) {
            super(message);
        }
    }

    private void revertLeaveOrWfh(VunnoResponse response) {
        Employee employee = response.getEmployee();
        String applicationType = response.getApplicationType();
        LocalDate fromDate = response.getFromDate();
        LocalDate toDate = response.getToDate();

        if (employee == null || applicationType == null || fromDate == null) {
            // Nothing we can safely revert
            return;
        }

        int currentYear = fromDate.getYear();

        if ("Leave".equalsIgnoreCase(applicationType)) {
            String duration = response.getDuration(); // "Full Day", "Half Day AM", "Half Day PM", "Multiple Days"
            String leaveType = response.getLeaveType();

            if (leaveType == null || leaveType.isBlank()) {
                // Can't safely identify which log to revert
                return;
            }

            if (duration == null) {
                // safety: do nothing if duration is missing
                return;
            }

            // Handle Mixed leave type OR any multi-day type with configs: parse
            // dayConfigurations and delete logs for each day
            if (response.getDayConfigurations() != null && !response.getDayConfigurations().isEmpty()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                    com.vbs.capsAllocation.dto.DayConfigurationDto[] dayConfigs = mapper.readValue(
                            response.getDayConfigurations(),
                            com.vbs.capsAllocation.dto.DayConfigurationDto[].class);

                    logger.info("Reverting mixed leave type with {} day configurations", dayConfigs.length);

                    for (com.vbs.capsAllocation.dto.DayConfigurationDto dayConfig : dayConfigs) {
                        LocalDate leaveDate = LocalDate.parse(dayConfig.getDate());
                        String specificLeaveType = dayConfig.getLeaveType();
                        double daysTaken = dayConfig.getDurationValue(); // 1.0 or 0.5

                        long isBusinessDay = countBusinessDays(leaveDate, leaveDate);
                        if (isBusinessDay > 0) {
                            List<LeaveUsageLog> logs = leaveUsageLogRepository
                                    .findByEmployeeAndLeaveTypeAndLeaveDateAndYearAndDaysTaken(
                                            employee, specificLeaveType, leaveDate, leaveDate.getYear(), daysTaken);
                            if (!logs.isEmpty()) {
                                leaveUsageLogRepository.deleteAll(logs);
                                logger.info("Deleted {} usage log(s) for {} on {}", logs.size(), specificLeaveType,
                                        leaveDate);
                            }
                        }
                    }
                    return; // Exit early after handling mixed leave type
                } catch (Exception e) {
                    logger.error("Failed to parse dayConfigurations for balance restoration", e);
                    return; // Safer to do nothing than delete wrong logs
                }
            }

            switch (duration) {
                case "Full Day" -> {
                    long businessDays = countBusinessDays(fromDate, fromDate);
                    if (businessDays == 0) {
                        // Wasn't deducted anyway (non-working day), nothing to revert
                        return;
                    }

                    // Find and delete the single day log
                    List<LeaveUsageLog> logs = leaveUsageLogRepository
                            .findByEmployeeAndLeaveTypeAndLeaveDateAndYearAndDaysTaken(
                                    employee, leaveType, fromDate, fromDate.getYear(), 1.0);
                    if (!logs.isEmpty()) {
                        leaveUsageLogRepository.deleteAll(logs);
                    }
                }
                case "Half Day AM", "Half Day PM", "First Half", "Second Half" -> {
                    long businessDays = countBusinessDays(fromDate, fromDate);
                    if (businessDays == 0) {
                        return;
                    }

                    // Find and delete the half day log
                    List<LeaveUsageLog> logs = leaveUsageLogRepository
                            .findByEmployeeAndLeaveTypeAndLeaveDateAndYearAndDaysTaken(
                                    employee, leaveType, fromDate, fromDate.getYear(), 0.5);
                    if (!logs.isEmpty()) {
                        leaveUsageLogRepository.deleteAll(logs);
                    }
                }
                case "Multiple Days" -> {
                    if (toDate == null) {
                        return;
                    }
                    long businessDays = countBusinessDays(fromDate, toDate);
                    if (businessDays <= 0) {
                        return;
                    }

                    // Check if we have per-day configurations to restore accurately
                    // This block handles cases where the overall leaveType is NOT "Mixed"
                    // but the requestDto might still contain dayConfigurations (e.g., for future
                    // flexibility)
                    if (response.getDayConfigurations() != null && !response.getDayConfigurations().isEmpty()) {
                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                            com.vbs.capsAllocation.dto.DayConfigurationDto[] dayConfigs = mapper.readValue(
                                    response.getDayConfigurations(),
                                    com.vbs.capsAllocation.dto.DayConfigurationDto[].class);

                            logger.info("Reverting multiple days leave request with {} day configurations",
                                    dayConfigs.length);

                            // Restore each day's specific configuration
                            for (com.vbs.capsAllocation.dto.DayConfigurationDto dayConfig : dayConfigs) {
                                LocalDate leaveDate = LocalDate.parse(dayConfig.getDate());
                                String specificLeaveType = dayConfig.getLeaveType();
                                double daysTaken = dayConfig.getDurationValue(); // 1.0 or 0.5

                                long isBusinessDay = countBusinessDays(leaveDate, leaveDate);
                                if (isBusinessDay > 0) {
                                    List<LeaveUsageLog> logs = leaveUsageLogRepository
                                            .findByEmployeeAndLeaveTypeAndLeaveDateAndYearAndDaysTaken(
                                                    employee, specificLeaveType, leaveDate, leaveDate.getYear(),
                                                    daysTaken);
                                    if (!logs.isEmpty()) {
                                        leaveUsageLogRepository.deleteAll(logs);
                                        logger.info("Deleted {} usage log(s) for {} on {} ({} days)",
                                                logs.size(), specificLeaveType, leaveDate, daysTaken);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.error(
                                    "Failed to parse dayConfigurations for balance restoration for Multiple Days leave. Falling back to date range deletion.",
                                    e);
                            // Fallback: delete all logs in the date range for this leave type
                            // This handles cases where dayConfigs parsing failed or if the original request
                            // was a single-type multi-day leave without dayConfigs.
                            LocalDate currentDate = fromDate;
                            while (!currentDate.isAfter(toDate)) {
                                long isBusinessDay = countBusinessDays(currentDate, currentDate);
                                if (isBusinessDay > 0) {
                                    List<LeaveUsageLog> logs = leaveUsageLogRepository
                                            .findByEmployeeAndLeaveTypeAndLeaveDateAndYear(
                                                    employee, leaveType, currentDate, currentDate.getYear());
                                    if (!logs.isEmpty()) {
                                        leaveUsageLogRepository.deleteAll(logs);
                                        logger.info("Deleted {} usage log(s) for {} on {} (fallback for Multiple Days)",
                                                logs.size(), leaveType, currentDate);
                                    }
                                }
                                currentDate = currentDate.plusDays(1);
                            }
                        }
                    } else {
                        // No day_configurations: Delete all logs within date range, day by day
                        // This covers old requests and single-type multi-day requests
                        LocalDate currentDate = fromDate;
                        while (!currentDate.isAfter(toDate)) {
                            long isBusinessDay = countBusinessDays(currentDate, currentDate);
                            if (isBusinessDay > 0) {
                                List<LeaveUsageLog> logs = leaveUsageLogRepository
                                        .findByEmployeeAndLeaveTypeAndLeaveDateAndYear(
                                                employee, leaveType, currentDate, currentDate.getYear());
                                if (!logs.isEmpty()) {
                                    leaveUsageLogRepository.deleteAll(logs);
                                    logger.info("Deleted {} usage log(s) for {} on {} (no day configs)",
                                            logs.size(), leaveType, currentDate);
                                }
                            }
                            currentDate = currentDate.plusDays(1);
                        }
                    }
                }
                default -> {
                    // Unknown duration: safer to do nothing than to delete wrong logs
                    return;
                }
            }

        } else if ("Work From Home".equalsIgnoreCase(applicationType)) {
            String duration = response.getDuration();
            double daysTakenToRevert;

            if (duration == null) {
                return;
            }

            switch (duration) {
                case "Full Day" -> daysTakenToRevert = 1.0;
                case "Half Day AM", "Half Day PM" -> daysTakenToRevert = 0.5;
                case "Multiple Days" -> {
                    if (fromDate == null || toDate == null) {
                        return;
                    }
                    long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
                    if (days <= 0) {
                        return;
                    }
                    // If later you change WFH to business days, do the same as Leave
                    daysTakenToRevert = (double) days;
                }
                default -> {
                    return;
                }
            }

            List<LeaveUsageLog> wfhLogs = leaveUsageLogRepository
                    .findByEmployeeAndLeaveTypeAndLeaveDateAndYearAndDaysTaken(
                            employee, "WFH", fromDate, currentYear, daysTakenToRevert);

            if (!wfhLogs.isEmpty()) {
                leaveUsageLogRepository.deleteAll(wfhLogs);
            }
        }
    }

    private String cleanScreenshotUrl(String url) {
        if (url == null)
            return null;

        // Find first "http"
        int index = url.indexOf("http");

        if (index == -1) {
            // No valid URL — treat as empty/invalid
            return null;
        }

        // Trim garbage before the URL
        String cleaned = url.trim();

        // Optionally enforce valid http/https format
        if (!cleaned.matches("https?://.*")) {
            return null;
        }

        return cleaned;
    }

    /**
     * Get audit history for a specific leave request
     */
    public List<VunnoAuditDto> getAuditHistory(Long vunnoResponseId) {
        List<VunnoAuditLog> auditLogs = vunnoAuditRepository
                .findByVunnoResponseIdOrderByChangedAtDesc(vunnoResponseId);

        return auditLogs.stream()
                .map(this::convertAuditToDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert VunnoAuditLog entity to DTO
     */
    private VunnoAuditDto convertAuditToDto(VunnoAuditLog auditLog) {
        VunnoAuditDto dto = new VunnoAuditDto();
        dto.setId(auditLog.getId());
        dto.setVunnoResponseId(auditLog.getVunnoResponseId());
        dto.setActionType(auditLog.getActionType());
        dto.setPreviousStatus(auditLog.getPreviousStatus());
        dto.setNewStatus(auditLog.getNewStatus());
        dto.setChangedBy(auditLog.getChangedBy());
        dto.setChangedByRole(auditLog.getChangedByRole());
        dto.setChangedAt(auditLog.getChangedAt());
        dto.setChangeReason(auditLog.getChangeReason());
        dto.setChangeDescription(auditLog.getChangeDescription());
        dto.setPreviousValues(auditLog.getPreviousValues());
        dto.setNewValues(auditLog.getNewValues());
        return dto;
    }

    private long countBusinessDays(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new LeaveBalanceException("From and To dates are required.");
        }

        if (toDate.isBefore(fromDate)) {
            throw new LeaveBalanceException("End date cannot be before start date.");
        }

        // Use your existing method and then filter to GOOGLE type in code
        List<Holiday> holidaysInRange = holidayRepository
                .findHolidaysBetweenDates(fromDate, toDate, true); // isActive = true

        Set<LocalDate> googleHolidayDates = holidaysInRange.stream()
                .filter(h -> GOOGLE_HOLIDAY_TYPE.equalsIgnoreCase(h.getHolidayType()))
                .map(Holiday::getHolidayDate)
                .collect(Collectors.toSet());

        long businessDays = 0;
        LocalDate date = fromDate;

        while (!date.isAfter(toDate)) {
            DayOfWeek dow = date.getDayOfWeek();
            boolean isWeekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);
            boolean isHoliday = googleHolidayDates.contains(date);

            if (!isWeekend && !isHoliday) {
                businessDays++;
            }

            date = date.plusDays(1);
        }

        return businessDays;
    }

    /**
     * Approve all auto-generated time entries linked to a leave request
     * 
     * @param response the approved leave request
     * @return number of time entries approved
     */
    private int approveLinkedTimeEntries(VunnoResponse response) {
        // Find auto-generated time entries for this leave
        List<TimeEntry> timeEntries = timeEntryRepository.findAutoGeneratedEntriesForLeaveApproval(
                response.getEmployee().getLdap(),
                response.getFromDate(),
                response.getToDate());

        if (timeEntries.isEmpty()) {
            return 0;
        }

        // Extract IDs for bulk update
        List<Long> entryIds = timeEntries.stream()
                .map(TimeEntry::getId)
                .collect(Collectors.toList());

        // Bulk update status to APPROVED and unlock entries
        int updated = timeEntryRepository.bulkUpdateStatusAndLock(
                entryIds,
                TimeEntryStatus.APPROVED,
                false, // isLocked = false
                null, // lockedBy = null
                null, // lockedAt = null
                LocalDateTime.now());

        logger.info("Approved {} out of {} time entries for leave request ID: {}",
                updated, timeEntries.size(), response.getId());

        return updated;
    }
}