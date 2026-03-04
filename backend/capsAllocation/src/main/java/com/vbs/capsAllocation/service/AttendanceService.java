package com.vbs.capsAllocation.service;

import com.vbs.capsAllocation.dto.AttendanceRequest;
import com.vbs.capsAllocation.dto.AttendanceResponseDto;
import com.vbs.capsAllocation.dto.CheckInStatusResponse;
import com.vbs.capsAllocation.dto.CheckoutResponseDto;
import com.vbs.capsAllocation.model.*;
import com.vbs.capsAllocation.repository.*;
import com.vbs.capsAllocation.service.impl.ShiftDetailsServiceImpl;
import com.vbs.capsAllocation.util.GeoUtil;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.*;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AttendanceService {

    @Autowired
    private GeoUtil geoUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeRelationRepository employeeRelationRepository;

    @Autowired
    private ShiftDetailsServiceImpl shiftDetailsService;

    @Autowired
    private AttendanceAuditService attendanceAuditService;

    private static final ZoneId INDIA_TIMEZONE = ZoneId.of("Asia/Kolkata");

    private static final Logger logger = LoggerFactory.getLogger(AttendanceService.class);

    public List<AttendanceResponseDto> getAttendanceRecords(
            String ldap, LocalDate startDate, LocalDate endDate, boolean directOnly, boolean self) {

        User user = userRepository.findByUsername(ldap)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Role role = user.getRole();

        // Default to last 7 days if no dates given
        if (startDate == null || endDate == null) {
            LocalDate today = ZonedDateTime.now(INDIA_TIMEZONE).toLocalDate();
            endDate = today;
            startDate = today.minusDays(6);
        }

        // Only LEAD, MANAGER, and regular employees can view their own attendance
        if (self) {
            if (role == Role.LEAD || role == Role.USER || role == Role.MANAGER) {
                return attendanceRepository
                        .findByEmployeeLdapAndEntryDateBetween(ldap, startDate, endDate);
            }
            // For ADMIN_OPS_MANAGER, self=true is invalid, fall through to team logic
        }

        Set<String> teamLdaps = new HashSet<>();

        if (role == Role.ADMIN_OPS_MANAGER) {
            teamLdaps.addAll(employeeRepository.findAllLdaps()); // Admin sees everyone
        } else if (role == Role.MANAGER) {
            if (directOnly) {
                teamLdaps.addAll(employeeRepository.findLdapsByManager(ldap)); // only direct
            } else {
                teamLdaps.addAll(employeeRelationRepository.findLdapsBySecondaryManager(ldap)); // only secondary
            }
        } else if (role == Role.LEAD) {
            if (directOnly) {
                teamLdaps.addAll(employeeRepository.findLdapsByLead(ldap)); // only direct
            } else {
                teamLdaps.addAll(employeeRelationRepository.findLdapsBySecondaryLead(ldap)); // only secondary
            }
        } else {
            // Regular user → only self
            return attendanceRepository.findByEmployeeLdapAndEntryDateBetween(ldap, startDate, endDate);
        }

        if (teamLdaps.isEmpty()) {
            return List.of();
        }

        return attendanceRepository.findByLdapsAndDateRange(new ArrayList<>(teamLdaps), startDate, endDate);
    }

    private AttendanceResponseDto convertToDTO(Attendance attendance) {
        Employee emp = attendance.getEmployee();

        AttendanceResponseDto dto = new AttendanceResponseDto();
        dto.setId(attendance.getId());
        dto.setLdap(emp.getLdap());
        dto.setTeam(emp.getTeam());
        dto.setName(emp.getFirstName() + " " + emp.getLastName());
        dto.setEntryDate(attendance.getEntryDate());
        dto.setEntryTimestamp(attendance.getEntryTimestamp());
        dto.setLateLoginReason(attendance.getLateLoginReason());
        dto.setIsOutsideOffice(attendance.getIsOutsideOffice());
        dto.setIsDefaulter(attendance.getIsDefaulter());
        dto.setComment(attendance.getComment());

        return dto;
    }

    public Attendance markAttendance(AttendanceRequest request) {
        // 1. Validate and fetch employee
        String ldap = request.getLdap();
        Employee employee = employeeRepository.findByLdap(ldap)
                .orElseThrow(() -> new RuntimeException("Employee not found for LDAP: " + ldap));

        ShiftDetails shift = shiftDetailsService.getShift(employee.getShift());
        LocalTime shiftStart = LocalTime.parse(shift.getStartTime());

        // 2. Get current date and time in India timezone
        ZonedDateTime nowInIndia = ZonedDateTime.now(INDIA_TIMEZONE);
        LocalDate today = nowInIndia.toLocalDate();
        LocalDateTime checkInTime = nowInIndia.toLocalDateTime();

        List<Attendance> markedAttendance = attendanceRepository.findByEmployeeLdapAndEntryDate(ldap, today);
        if (markedAttendance.size() == 1) {
            return markedAttendance.get(0);
        }

        // 3. Validate location and time
        boolean isInside = isUserInsideOffice(
                request.getLatitude(),
                request.getLongitude());

        boolean isLate = isLateCheckIn(checkInTime, shiftStart);
        boolean isDefaulter = isLate || !isInside;

        // PRODUCTION LOGGING - CHECK-IN
        logger.info(
                "CHECK-IN | LDAP: {} | Date: {} | Time: {} | Device: {} | Coordinates: [{}, {}] | Accuracy: {}m | Location: {} | Status: {}",
                ldap,
                today,
                checkInTime.toLocalTime(),
                request.getDeviceType() != null ? request.getDeviceType() : "Unknown",
                request.getLatitude(),
                request.getLongitude(),
                request.getAccuracy() != null ? String.format("%.1f", request.getAccuracy()) : "N/A",
                isInside ? "INSIDE" : "OUTSIDE",
                isLate ? "LATE" : "ON-TIME");

        // 5. Create and save attendance record
        Attendance entry = new Attendance();
        entry.setEmployee(employee); // Setting the relationship
        entry.setEntryDate(today);
        entry.setEntryTimestamp(checkInTime);
        entry.setLateLoginReason(request.getReason());
        entry.setIsOutsideOffice(!isInside);
        entry.setIsDefaulter(isDefaulter);
        entry.setComment(request.getComment());
        if (shiftStart.isAfter(checkInTime.toLocalTime()))
            entry.setLateOrEarlyCheckout("Early-CheckIn");
        else
            entry.setLateOrEarlyCheckout(isLate ? "Late-CheckIn" : "OnTime-CheckIn");

        attendanceRepository.save(entry);

        return entry;
    }

    private boolean isLateCheckIn(LocalDateTime checkInTime, LocalTime shiftStartTime) {
        return checkInTime.toLocalTime().isAfter(shiftStartTime.plusMinutes(30)); // After 8 AM
    }

    public boolean isUserInsideOffice(double userLat, double userLng) {
        List<double[]> officePolygon = geoUtil.getOfficePolygon();
        boolean isInside = GeoUtil.isPointInsidePolygon(userLat, userLng, officePolygon);

        // Log for debugging
        if (!isInside) {
            logger.warn("User marked as OUTSIDE office: lat={}, lng={} - Please verify coordinates",
                    userLat, userLng);
        }

        return isInside;
    }

    public CheckInStatusResponse getCheckInStatus(String ldap) {
        logger.debug("Getting check-in status for LDAP: {}", ldap);
        LocalDate today = ZonedDateTime.now(INDIA_TIMEZONE).toLocalDate();
        logger.debug("Querying attendance for LDAP: {} on date: {}", ldap, today);

        return attendanceRepository
                .findTopByEmployee_LdapOrderByEntryTimestampDesc(ldap)
                .map(attendance -> {
                    logger.debug("Found attendance record for LDAP: {}, ID: {}, Timestamp: {}",
                            ldap, attendance.getId(), attendance.getEntryTimestamp());
                    if (attendance.getEntryDate().isBefore(today) && attendance.getExitTimestamp() != null)
                        return this.buildNotCheckedInResponse();
                    return this.buildCheckedInResponse(attendance);
                })
                .orElseGet(() -> {
                    logger.debug("No attendance record found for LDAP: {} on date: {}", ldap, today);
                    return this.buildNotCheckedInResponse();
                });
    }

    private CheckInStatusResponse buildCheckedInResponse(Attendance attendance) {
        LocalTime checkInTime = attendance.getEntryTimestamp().toLocalTime();
        LocalTime checkOutTime = attendance.getExitTimestamp() != null
                ? attendance.getExitTimestamp().toLocalTime()
                : null;

        return new CheckInStatusResponse(
                "Checked in at " + checkInTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                true,
                checkInTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                checkInTime.isAfter(LocalTime.of(8, 0)), // Late if after 8:00 AM
                checkOutTime != null ? "Checked out at " + checkOutTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                        : null,
                attendance.getIsCheckOutOutsideOffice() != null,
                checkOutTime != null ? checkOutTime.format(DateTimeFormatter.ofPattern("HH:mm")) : null,
                attendance.getLateOrEarlyCheckout(),
                attendance.getEntryTimestamp(),
                attendance.getExitTimestamp(),
                attendance.getIsOutsideOffice(),
                attendance.getIsCheckOutOutsideOffice());
    }

    private CheckInStatusResponse buildNotCheckedInResponse() {
        return new CheckInStatusResponse(
                "Not checked in today",
                false,
                null,
                false,
                "Not checked out",
                false,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    @Transactional
    public CheckoutResponseDto checkoutAttendence(AttendanceRequest request) {

        String ldap = request.getLdap();

        List<Attendance> attendances = attendanceRepository.findByEmployee_LdapAndExitDateIsNull(ldap);
        if (attendances.isEmpty()) {
            logger.warn("❌ CHECKOUT FAILED | LDAP: {} | Reason: Already checked out", ldap);
            throw new RuntimeException("You have already checked out for today");
        }

        Attendance markedAttendance = attendances.get(attendances.size() - 1);
        Employee employee = markedAttendance.getEmployee();
        ShiftDetails shift = shiftDetailsService.getShift(employee.getShift());

        ZonedDateTime nowInIndia = ZonedDateTime.now(INDIA_TIMEZONE);
        LocalDateTime currentTime = nowInIndia.toLocalDateTime();
        LocalDate today = nowInIndia.toLocalDate();

        boolean isInside = isUserInsideOffice(request.getLatitude(), request.getLongitude());

        // Compute checkout reason and status based on shift timings
        String reason = computeLogoutReason(markedAttendance, shift, request, currentTime);
        String status = computeCheckoutStatus(markedAttendance, shift, currentTime);

        // PRODUCTION LOGGING - CHECK-OUT
        logger.info(
                "CHECK-OUT | LDAP: {} | Date: {} | Time: {} | Device: {} | Coordinates: [{}, {}] | Accuracy: {}m | Location: {} | Status: {}",
                ldap,
                today,
                currentTime.toLocalTime(),
                request.getDeviceType() != null ? request.getDeviceType() : "Unknown",
                request.getLatitude(),
                request.getLongitude(),
                request.getAccuracy() != null ? String.format("%.1f", request.getAccuracy()) : "N/A",
                isInside ? "INSIDE " : "OUTSIDE ",
                status);

        // Update checkout info in DB
        attendanceRepository.updateCheckout(
                markedAttendance.getId(),
                today,
                currentTime,
                !isInside,
                reason,
                status);

        logger.debug("Checkout process completed successfully for employee: {}", ldap);
        return buildCheckoutDto(markedAttendance, ldap, employee, currentTime, today, !isInside, reason, status);
    }

    private CheckoutResponseDto buildCheckoutDto(Attendance attendance, String ldap, Employee employee,
            LocalDateTime exitTimestamp, LocalDate exitDate,
            boolean checkoutOutside, String reason, String status) {
        return CheckoutResponseDto.builder()
                .ldap(ldap)
                .name(employee.getFirstName() + " " + employee.getLastName())
                .entryDate(attendance.getEntryDate())
                .exitDate(exitDate)
                .checkinWhileOutsideOffice(attendance.getIsOutsideOffice())
                .checkoutWhileOutsideOffice(checkoutOutside)
                .entryTimestamp(attendance.getEntryTimestamp())
                .exitTimestamp(exitTimestamp)
                .lateLoginReason(attendance.getLateLoginReason())
                .lateOrEarlyLogoutReason(reason)
                .loginComment(attendance.getComment())
                .lateOrEarlyCheckout(status)
                .build();
    }

    /**
     * Determine if checkout was early, on-time, or late, based on the shift's end
     * time and grace period.
     */
    private String computeCheckoutStatus(Attendance attendance, ShiftDetails shift, LocalDateTime currentTime) {
        LocalTime start = LocalTime.parse(shift.getStartTime());
        LocalTime end = LocalTime.parse(shift.getEndTime());
        int grace = shift.getGracePeriodMinutes() != null ? shift.getGracePeriodMinutes() : 0;

        LocalDate entryDate = attendance.getEntryTimestamp().toLocalDate();
        LocalDate endDate = entryDate;

        // Handle overnight shift: end time on next day
        if (end.isBefore(start)) {
            endDate = endDate.plusDays(1);
        }

        LocalDateTime shiftEndTime = LocalDateTime.of(endDate, end);
        LocalDateTime earlyLimit = shiftEndTime.minusMinutes(grace);
        LocalDateTime lateLimit = shiftEndTime.plusMinutes(grace);

        boolean earlyCheckout = currentTime.isBefore(earlyLimit);
        boolean lateCheckout = currentTime.isAfter(lateLimit);

        String prefix = attendance.getLateOrEarlyCheckout() != null
                ? attendance.getLateOrEarlyCheckout() + ", "
                : "";

        if (earlyCheckout)
            return prefix + "Early-Checkout";
        else if (lateCheckout)
            return prefix + "Late-Checkout";
        else
            return prefix + "OnTime Checkout";
    }

    /**
     * Generate a logout reason string (if user checked out early or late).
     */
    private String computeLogoutReason(Attendance attendance, ShiftDetails shift,
            AttendanceRequest request, LocalDateTime currentTime) {
        LocalTime start = LocalTime.parse(shift.getStartTime());
        LocalTime end = LocalTime.parse(shift.getEndTime());
        int grace = shift.getGracePeriodMinutes() != null ? shift.getGracePeriodMinutes() : 0;

        LocalDate entryDate = attendance.getEntryTimestamp().toLocalDate();
        LocalDate endDate = entryDate;

        if (end.isBefore(start)) {
            endDate = endDate.plusDays(1);
        }

        LocalDateTime shiftEndTime = LocalDateTime.of(endDate, end);
        LocalDateTime earlyLimit = shiftEndTime.minusMinutes(grace);
        LocalDateTime lateLimit = shiftEndTime.plusMinutes(grace);

        boolean earlyCheckout = currentTime.isBefore(earlyLimit);
        boolean lateCheckout = currentTime.isAfter(lateLimit);

        String reasonComment = buildReasonComment(request.getReason(), request.getComment());

        if (earlyCheckout) {
            return reasonComment.isEmpty() ? "Early Checkout" : reasonComment;
        } else if (lateCheckout) {
            return reasonComment.isEmpty() ? "Late Checkout" : reasonComment;
        } else {
            return "OnTime Checkout";
        }
    }

    private String buildReasonComment(String reason, String comment) {
        if (reason == null && comment == null)
            return "";
        if (reason == null)
            return comment;
        if (comment == null)
            return reason;
        return reason + " - " + comment;
    }

    /**
     * Update attendance status with authorization and audit logging
     */
    @Transactional
    public Attendance updateAttendanceStatus(Long attendanceId, String newStatus, String reason,
            org.springframework.security.core.userdetails.UserDetails userDetails) {
        // Get attendance record
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Attendance record not found"));

        // Get user info
        String userLdap = userDetails.getUsername();
        User user = userRepository.findByUsername(userLdap)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Check authorization
        String employeeLdap = attendance.getEmployee().getLdap();
        if (!canUserModifyAttendance(userLdap, employeeLdap, user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not authorized to modify this attendance record");
        }

        // Store previous status for audit
        String previousStatus = attendance.getLateOrEarlyCheckout();

        // Update status
        attendance.setLateOrEarlyCheckout(newStatus);
        Attendance savedAttendance = attendanceRepository.save(attendance);

        // Log audit
        attendanceAuditService.logStatusUpdate(
                savedAttendance,
                previousStatus,
                newStatus,
                reason,
                userDetails);

        return savedAttendance;
    }

    /**
     * Update compliance status with authorization and audit logging
     */
    @Transactional
    public Attendance updateComplianceStatus(Long attendanceId, Boolean isDefaulter, String reason,
            org.springframework.security.core.userdetails.UserDetails userDetails) {
        // Get attendance record
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Attendance record not found"));

        // Get user info
        String userLdap = userDetails.getUsername();
        User user = userRepository.findByUsername(userLdap)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Check authorization
        String employeeLdap = attendance.getEmployee().getLdap();
        if (!canUserModifyAttendance(userLdap, employeeLdap, user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not authorized to modify this attendance record");
        }

        // Store previous compliance for audit
        Boolean previousCompliance = attendance.getIsDefaulter();

        // Update compliance
        attendance.setIsDefaulter(isDefaulter);
        Attendance savedAttendance = attendanceRepository.save(attendance);

        // Log audit
        attendanceAuditService.logComplianceUpdate(
                savedAttendance,
                previousCompliance,
                isDefaulter,
                reason,
                userDetails);

        return savedAttendance;
    }

    /**
     * Check if user can modify attendance record based on role and relationship
     */
    private boolean canUserModifyAttendance(String userLdap, String employeeLdap, Role userRole) {
        // Admin ops manager can modify all records
        if (userRole == Role.ADMIN_OPS_MANAGER) {
            return true;
        }

        // User cannot modify their own record
        if (userLdap.equals(employeeLdap)) {
            return false;
        }

        // Manager and Lead can modify their direct and secondary reports
        if (userRole == Role.MANAGER || userRole == Role.LEAD) {
            // Get employee by LDAP
            Employee employee = employeeRepository.findByLdap(employeeLdap).orElse(null);
            if (employee == null) {
                return false;
            }

            // Check if user is the direct lead (stored in lead field)
            if (employee.getLead() != null && employee.getLead().equals(userLdap)) {
                return true;
            }

            // Check if user is secondary lead/manager through EmployeeRelation
            List<EmployeeRelation> relations = employeeRelationRepository.findByEmployeeId(employee.getId());
            for (EmployeeRelation relation : relations) {
                if (relation.getRelatedEmployee() != null &&
                        relation.getRelatedEmployee().getLdap().equals(userLdap) &&
                        relation.getIsActive() != null &&
                        relation.getIsActive()) {
                    return true;
                }
            }
        }

        return false;
    }
}