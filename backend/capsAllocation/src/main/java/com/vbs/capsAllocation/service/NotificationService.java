package com.vbs.capsAllocation.service;

import com.vbs.capsAllocation.dto.VunnoMgmtDto;
import com.vbs.capsAllocation.dto.VunnoRequestDto;
import com.vbs.capsAllocation.model.Employee;
import com.vbs.capsAllocation.model.EmployeeRelation;
import com.vbs.capsAllocation.model.User;
import com.vbs.capsAllocation.model.VunnoResponse;
import com.vbs.capsAllocation.repository.EmployeeRelationRepository;
import com.vbs.capsAllocation.repository.EmployeeRepository;
import com.vbs.capsAllocation.repository.UserRepository;
import com.vbs.capsAllocation.util.EmailTemplateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class NotificationService {

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmailTemplateUtil emailTemplateUtil;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRelationRepository employeeRelationRepository;

    private static final String ACCOUNT_MANAGER_EMAIL = "dsheoran@google.com";

    public void triggerRequestNotification(VunnoRequestDto requestDto, VunnoMgmtDto dto,
            List<Double> alreadyDeductedCounts) {
        try {
            final String DOMAIN = "@google.com";
            String ldap = requestDto.getLdap();

            boolean isWFHMoreThan3Days = isWFHMoreThan3Days(requestDto);
            System.out.println("Return Value of isWFH Days " + isWFHMoreThan3Days);

            // Balances are ALREADY deducted - no need to deduct again!
            // The counts passed from VunnoMgmtService are already post-deduction
            String emailBody = emailTemplateUtil.getVunnoNotificationEmail(requestDto, dto, alreadyDeductedCounts,
                    isWFHMoreThan3Days);

            // Use Sets to avoid duplicates
            Set<String> toSet = new HashSet<>();
            Set<String> ccSet = new HashSet<>();

            // Always CC requestor
            ccSet.add(ldap + DOMAIN);

            // Employee (primary manager / primary lead)
            Employee emp = employeeRepository.findByLdap(ldap)
                    .orElseThrow(() -> new RuntimeException("Employee not found: " + ldap));

            // Primary manager -> TO (if present)
            boolean managerAdded = false;
            if (emp.getProgramManager() != null && !emp.getProgramManager().isBlank()) {
                toSet.add(emp.getProgramManager() + DOMAIN);
                managerAdded = true;
            }

            // Primary lead -> CC
            if (emp.getLead() != null && !emp.getLead().isBlank()) {
                ccSet.add(emp.getLead() + DOMAIN);
            }

            // Secondary manager / lead from employee_relation
            List<EmployeeRelation> relations = employeeRelationRepository.findByEmployeeId(emp.getId());
            for (EmployeeRelation rel : relations) {
                if (!Boolean.TRUE.equals(rel.getIsActive()))
                    continue;

                String relationTypeName = null;
                if (rel.getRelationType() != null && rel.getRelationType().getName() != null) {
                    relationTypeName = rel.getRelationType().getName();
                } else {
                    relationTypeName = rel.getRelationValue();
                }

                Employee related = rel.getRelatedEmployee();
                if (related == null)
                    continue;

                if ("MANAGER".equalsIgnoreCase(relationTypeName)) {
                    toSet.add(related.getLdap() + DOMAIN);
                    managerAdded = true;
                } else if ("LEAD".equalsIgnoreCase(relationTypeName)) {
                    ccSet.add(related.getLdap() + DOMAIN);
                }
            }

            // If requestor is a LEAD, ensure their manager/secondary manager was included
            Optional<User> userOpt = userRepository.findByUsername(ldap);
            if (userOpt.isPresent() && userOpt.get().getRole() != null &&
                    "LEAD".equalsIgnoreCase(userOpt.get().getRole().toString())) {
                if (!managerAdded) {
                    // Try again to find any manager relations (defensive: same check as above)
                    for (EmployeeRelation rel : relations) {
                        if (!Boolean.TRUE.equals(rel.getIsActive()))
                            continue;
                        String relationTypeName = rel.getRelationType() != null ? rel.getRelationType().getName()
                                : rel.getRelationValue();
                        if ("MANAGER".equalsIgnoreCase(relationTypeName) && rel.getRelatedEmployee() != null) {
                            toSet.add(rel.getRelatedEmployee().getLdap() + DOMAIN);
                            managerAdded = true;
                            break;
                        }
                    }
                }
                if (!managerAdded) {
                    // Nothing to add — log so admins/developers can check data completeness
                    System.out.println(
                            "Warning: Requestor is LEAD but no primary or secondary manager found for " + ldap);
                }
            }

            // Special case: WFH > 3 days = notify account manager in CC
            if (isWFHMoreThan3Days) {
                System.out.println("Account Manager Added: " + ACCOUNT_MANAGER_EMAIL);
                toSet.add(ACCOUNT_MANAGER_EMAIL);
            }

            // Build subject
            String leaveTypeInfo = "";
            if ("Leave".equalsIgnoreCase(requestDto.getApplicationType())) {
                String lType = requestDto.getLeaveType();
                if ("Mixed".equalsIgnoreCase(lType) && requestDto.getDayConfigurations() != null) {
                    lType += VunnoRequestDto.formatMixedDetails(requestDto.getDayConfigurations());
                } else if (lType != null && requestDto.getDayConfigurations() != null
                        && !requestDto.getDayConfigurations().isEmpty()) {
                    // For single leave type, add total days
                    lType += VunnoRequestDto.formatSingleTypeDetails(requestDto.getDayConfigurations());
                }
                if (lType != null) {
                    leaveTypeInfo = " (" + lType + ")";
                }
            }

            String subject = String.format("Teamsphere Request%s | %s | %s - %s | %s",
                    leaveTypeInfo,
                    ldap,
                    requestDto.getStartDate(),
                    requestDto.getEndDate(),
                    requestDto.getLvWfhDuration());

            // Convert sets to lists and send
            List<String> toList = new ArrayList<>(toSet); // may be empty if no manager configured
            List<String> ccList = new ArrayList<>(ccSet);

            // Debug
            System.out.println("Sending request mail TO: " + toList);
            System.out.println("CC: " + ccList);

            emailService.sendEmail(toList, ccList, subject, emailBody);

        } catch (Exception e) {
            System.err.println("Error sending request notification email: " + e.getMessage());
            throw new RuntimeException("Failed to send request notification email", e);
        }
    }

    public void triggerApprovalNotification(VunnoRequestDto requestDto, VunnoResponse response, String role,
            VunnoMgmtDto dto) {
        try {
            final String DOMAIN = "@google.com";
            String ldap = requestDto.getLdap(); // Requestor
            String managerLdap = dto.getManager(); // Primary manager from DTO (if available)

            Set<String> ccSet = new HashSet<>();
            List<String> toList = new ArrayList<>();

            // Approval for Leaves as well as WFH.
            // toList.add("vbs-teams@googlegroups.com");

            // CC everyone else (requestor, primary manager, primary lead, secondary
            // manager/lead)
            ccSet.add(ldap + DOMAIN);
            if (managerLdap != null && !managerLdap.isBlank()) {
                ccSet.add(managerLdap + DOMAIN);
            }

            // Primary lead from employee table
            Employee emp = employeeRepository.findByLdap(ldap)
                    .orElse(null);
            if (emp != null) {
                if (emp.getLead() != null && !emp.getLead().isBlank()) {
                    ccSet.add(emp.getLead() + DOMAIN);
                }

                // secondary relations
                List<EmployeeRelation> relations = employeeRelationRepository.findByEmployeeId(emp.getId());
                for (EmployeeRelation rel : relations) {
                    if (!Boolean.TRUE.equals(rel.getIsActive()))
                        continue;

                    String relationTypeName = null;
                    if (rel.getRelationType() != null && rel.getRelationType().getName() != null) {
                        relationTypeName = rel.getRelationType().getName();
                    } else {
                        relationTypeName = rel.getRelationValue();
                    }

                    Employee related = rel.getRelatedEmployee();
                    if (related == null)
                        continue;

                    if ("MANAGER".equalsIgnoreCase(relationTypeName)) {
                        ccSet.add(related.getLdap() + DOMAIN);
                    } else if ("LEAD".equalsIgnoreCase(relationTypeName)) {
                        ccSet.add(related.getLdap() + DOMAIN);
                    }
                }
            }

            // Subject and body
            String subject = String.format("Teamsphere [REQUEST APPROVED] | %s | %s | %s - %s",
                    ldap,
                    response.getApplicationType(),
                    response.getFromDate(),
                    response.getToDate());

            String emailBody = emailTemplateUtil.getVunnoApprovalEmail(
                    response.getBackup(),
                    response.getApplicationType(),
                    response.getFromDate().toString(),
                    response.getToDate().toString(),
                    response.getDuration());

            // Debug
            System.out.println("Sending approval mail TO: " + toList);
            System.out.println("CC: " + new ArrayList<>(ccSet));

            emailService.sendEmail(toList, new ArrayList<>(ccSet), subject, emailBody);

        } catch (Exception e) {
            System.err.println("Error sending approval notification email: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send approval notification email", e);
        }
    }

    public void triggerRejectionNotification(VunnoRequestDto requestDto, VunnoResponse response, String rejectedBy) {
        try {
            final String DOMAIN = "@google.com";
            String ldap = requestDto.getLdap(); // Requestor

            // TO: Requestor whose request was rejected
            List<String> toList = new ArrayList<>();
            toList.add(ldap + DOMAIN);

            // CC: Person who rejected the request
            List<String> ccList = new ArrayList<>();
            ccList.add(rejectedBy + DOMAIN);

            // Subject
            String subject = String.format("Teamsphere [REQUEST REJECTED] | %s | %s | %s - %s",
                    ldap,
                    response.getApplicationType(),
                    response.getFromDate(),
                    response.getToDate());

            // Email body
            String comment = requestDto.getComment() != null ? requestDto.getComment() : "";
            String emailBody = emailTemplateUtil.getVunnoRejectionEmail(
                    response.getBackup(),
                    response.getApplicationType(),
                    response.getFromDate().toString(),
                    response.getToDate().toString(),
                    response.getDuration(),
                    rejectedBy,
                    comment);

            // Debug
            System.out.println("Sending rejection mail TO: " + toList);
            System.out.println("CC: " + ccList);

            emailService.sendEmail(toList, ccList, subject, emailBody);

        } catch (Exception e) {
            System.err.println("Error sending rejection notification email: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send rejection notification email", e);
        }
    }

    public void triggerRevokeNotification(VunnoRequestDto requestDto, VunnoResponse response, String revokedBy) {
        try {
            final String DOMAIN = "@google.com";
            String ldap = requestDto.getLdap(); // Requestor

            // TO: Requestor whose request was revoked
            List<String> toList = new ArrayList<>();
            toList.add(ldap + DOMAIN);

            // CC: Person who revoked the request
            List<String> ccList = new ArrayList<>();
            ccList.add(revokedBy + DOMAIN);

            // Subject
            String subject = String.format("Teamsphere [REQUEST REVOKED] | %s | %s | %s - %s",
                    ldap,
                    response.getApplicationType(),
                    response.getFromDate(),
                    response.getToDate());

            // Email body
            String comment = requestDto.getComment() != null ? requestDto.getComment() : "";
            String emailBody = emailTemplateUtil.getVunnoRevokeEmail(
                    response.getBackup(),
                    response.getApplicationType(),
                    response.getFromDate().toString(),
                    response.getToDate().toString(),
                    response.getDuration(),
                    revokedBy,
                    comment);

            // Debug
            System.out.println("Sending revoke mail TO: " + toList);
            System.out.println("CC: " + ccList);

            emailService.sendEmail(toList, ccList, subject, emailBody);

        } catch (Exception e) {
            System.err.println("Error sending revoke notification email: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send revoke notification email", e);
        }
    }

    /**
     * Calculate the number of days for a WFH request based on duration and dates
     * 
     * @param requestDto The WFH request DTO
     * @return The number days for the WFH request
     */
    public double calculateWFHDays(VunnoRequestDto requestDto) {
        String duration = requestDto.getLvWfhDuration();
        if (duration == null) {
            throw new RuntimeException("Duration is required for WFH calculation.");
        }

        duration = duration.trim();

        switch (duration) {
            case "Full Day":
                return 1.0;
            case "Half Day AM":
            case "Half Day PM":
                return 0.5;
            case "Multiple Days":
                if (requestDto.getStartDate() == null || requestDto.getEndDate() == null) {
                    throw new RuntimeException("Start date and end date are required for Multiple Days WFH.");
                }
                try {
                    LocalDate startDate = LocalDate.parse(requestDto.getStartDate());
                    LocalDate endDate = LocalDate.parse(requestDto.getEndDate());
                    long days = ChronoUnit.DAYS.between(startDate, endDate) + 1; // Inclusive
                    return (double) days;
                } catch (Exception e) {
                    throw new RuntimeException("Invalid date format for WFH calculation: " + e.getMessage());
                }
            default:
                throw new RuntimeException("Invalid WFH duration: " + duration);
        }
    }

    /**
     * Check if a WFH request exceeds 3 days (strictly greater than 3)
     * 
     * @param requestDto The WFH request DTO
     * @return true if the request is for more than 3 days, false otherwise
     */
    public boolean isWFHMoreThan3Days(VunnoRequestDto requestDto) {
        if (!"Work From Home".equalsIgnoreCase(requestDto.getApplicationType())) {
            return false;
        }

        try {
            double days = calculateWFHDays(requestDto);
            // Changed to strictly greater than 3 (so 3 days exactly will NOT trigger
            // account manager email)
            return days >= 3.0;
        } catch (Exception e) {
            System.err.println("Error calculating WFH days for validation: " + e.getMessage());
            return false;
        }
    }

}
