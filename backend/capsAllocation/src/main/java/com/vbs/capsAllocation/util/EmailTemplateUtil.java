package com.vbs.capsAllocation.util;

import com.vbs.capsAllocation.dto.VunnoMgmtDto;
import com.vbs.capsAllocation.dto.VunnoRequestDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EmailTemplateUtil {

        public String getVunnoApprovalEmail(String backupInfo, String type, String fromDate, String toDate,
                        String duration) {
                String backupDisplay = "Work from Home".equalsIgnoreCase(type) ? "N/A (WFH)" : backupInfo;
                String message = "Work from Home".equalsIgnoreCase(type) ? ""
                                : "Please coordinate with your backup in your absence.";

                return "<!DOCTYPE html>"
                                + "<html><head>"
                                + "<style>"
                                + "body { font-family: Arial, sans-serif; line-height: 1.6; margin: 20px; }"
                                + "h3 { color: #2c5aa0; margin-bottom: 20px; }"
                                + "p { margin: 10px 0; }"
                                + "hr { margin: 20px 0; border: 1px solid #ddd; }"
                                + ".info-section { background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin: 15px 0; }"
                                + ".highlight { background-color: #fff3cd; padding: 10px; border-radius: 5px; margin: 15px 0; }"
                                + "</style>"
                                + "</head><body>"
                                + "<h3>Your " + type + " request has been approved!</h3>"
                                + "<div class='info-section'>"
                                + "<p><b>Duration:</b> " + duration + "</p>"
                                + "<p><b>From:</b> " + fromDate + "</p>"
                                + "<p><b>To:</b> " + toDate + "</p>"
                                + "<p><b>Backup:</b> " + backupDisplay + "</p>"
                                + "</div>"
                                + (message.isEmpty() ? "" : "<hr><p>" + message + "</p>")
                                + "</body></html>";
        }

        public String getVunnoRejectionEmail(String backupInfo, String type, String fromDate, String toDate,
                        String duration, String rejectedBy, String comment) {
                String backupDisplay = "Work from Home".equalsIgnoreCase(type) ? "N/A (WFH)" : backupInfo;
                String commentSection = (comment != null && !comment.trim().isEmpty())
                                ? "<div class='highlight'><p><b>Rejection Reason:</b> " + comment + "</p></div>"
                                : "";

                return "<!DOCTYPE html>"
                                + "<html><head>"
                                + "<style>"
                                + "body { font-family: Arial, sans-serif; line-height: 1.6; margin: 20px; }"
                                + "h3 { color: #d9534f; margin-bottom: 20px; }"
                                + "p { margin: 10px 0; }"
                                + "hr { margin: 20px 0; border: 1px solid #ddd; }"
                                + ".info-section { background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin: 15px 0; }"
                                + ".highlight { background-color: #f8d7da; padding: 10px; border-radius: 5px; margin: 15px 0; border-left: 4px solid #d9534f; }"
                                + "</style>"
                                + "</head><body>"
                                + "<h3>Your " + type + " request has been rejected</h3>"
                                + "<div class='info-section'>"
                                + "<p><b>Duration:</b> " + duration + "</p>"
                                + "<p><b>From:</b> " + fromDate + "</p>"
                                + "<p><b>To:</b> " + toDate + "</p>"
                                + "<p><b>Backup:</b> " + backupDisplay + "</p>"
                                + "<p><b>Rejected By:</b> " + rejectedBy + "</p>"
                                + "</div>"
                                + commentSection
                                + "<hr><p>If you have any questions, please contact your team lead or manager.</p>"
                                + "</body></html>";
        }

        public String getVunnoRevokeEmail(String backupInfo, String type, String fromDate, String toDate,
                        String duration, String revokedBy, String comment) {
                String backupDisplay = "Work from Home".equalsIgnoreCase(type) ? "N/A (WFH)" : backupInfo;
                String commentSection = (comment != null && !comment.trim().isEmpty())
                                ? "<div class='highlight'><p><b>Revoke Reason:</b> " + comment + "</p></div>"
                                : "";

                return "<!DOCTYPE html>"
                                + "<html><head>"
                                + "<style>"
                                + "body { font-family: Arial, sans-serif; line-height: 1.6; margin: 20px; }"
                                + "h3 { color: #f0ad4e; margin-bottom: 20px; }"
                                + "p { margin: 10px 0; }"
                                + "hr { margin: 20px 0; border: 1px solid #ddd; }"
                                + ".info-section { background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin: 15px 0; }"
                                + ".highlight { background-color: #fcf8e3; padding: 10px; border-radius: 5px; margin: 15px 0; border-left: 4px solid #f0ad4e; }"
                                + "</style>"
                                + "</head><body>"
                                + "<h3>Your " + type + " request has been revoked</h3>"
                                + "<div class='info-section'>"
                                + "<p><b>Duration:</b> " + duration + "</p>"
                                + "<p><b>From:</b> " + fromDate + "</p>"
                                + "<p><b>To:</b> " + toDate + "</p>"
                                + "<p><b>Backup:</b> " + backupDisplay + "</p>"
                                + "<p><b>Revoked By:</b> " + revokedBy + "</p>"
                                + "</div>"
                                + commentSection
                                + "<hr><p>If you have any questions, please contact your team lead or manager.</p>"
                                + "</body></html>";
        }

        public String getVunnoNotificationEmail(VunnoRequestDto requestDto, VunnoMgmtDto dto, List<Double> counts,
                        boolean isWFHMoreThan3Days) {

                String applicationType = requestDto.getApplicationType();
                boolean isLeave = "Leave".equalsIgnoreCase(applicationType);
                boolean isDuration = "Multiple Days".equalsIgnoreCase(requestDto.getLvWfhDuration());

                String requestReason = isLeave ? requestDto.getBackupInfo() : requestDto.getReason();
                String requestFor = isLeave ? requestDto.getLeaveType() : "NA (WFH)";
                String extraSectionTitle = isLeave ? "Backup Information" : "Comment";
                String oooLink = isLeave ? requestDto.getOooProof() : "NA";
                String duration = isDuration ? "Multiple Days" : requestDto.getLvWfhDuration();

                // Balances from counts
                double slBalance = counts.get(0);
                double clBalance = counts.get(1);
                double elBalance = counts.get(2);
                double totalBalance = counts.get(3);
                double totalWFHQuaterly = counts.get(5);
                double totalLeavesTakenThisQuarter = counts.size() > 4 ? counts.get(6) : 0.0;

                // Create balance note for Leave requests
                String balanceNote = isLeave
                                ? "<p style='background-color: #fff3cd; padding: 10px; border-left: 4px solid #ffc107; margin: 10px 0;'>"
                                                + "<b>📌 Important:</b> The leave balances shown below are <b>AFTER deducting this request</b>. "
                                                + "This represents the remaining balance if you approve this request."
                                                + "</p>"
                                : "";

                return "<!DOCTYPE html>"
                                + "<html><head>"
                                + "<base target=\"_top\">"
                                + "</head><body>"
                                + "<p style='font-size: 1.17em;'><b>" + applicationType
                                + " Request Notification</b></p>"
                                + "<p>You have received a new request with the following details:</p>"

                                // Request Details List
                                + "<h4>Request Details</h4>"
                                + "<ul>"
                                + "<li><b>Requester:</b> " + requestDto.getLdap() + "</li>"
                                + "<li><b>Leave Duration:</b> " + requestDto.getStartDate() + " - "
                                + requestDto.getEndDate() + "</li>"
                                + "<li><b>Request Type:</b> " + requestFor + "</li>"
                                + "<li><b>Request Duration:</b> " + duration + "</li>"
                                + "<li><b>" + extraSectionTitle + ":</b> " + requestReason + "</li>"
                                + "<li><b>Calendar OOO SS:</b> <a href='" + oooLink + "'>" + oooLink + "</a></li>"
                                + "</ul>"

                                // Balance Note (only for Leave requests)
                                + balanceNote

                                // Leave Balances List
                                + "<h4>Leave Balances and WFH Summary</h4>"
                                + "<ul>"
                                + "<li><b>Sick Leave (SL):</b> " + slBalance + "</li>"
                                + "<li><b>Casual Leave (CL):</b> " + clBalance + "</li>"
                                + "<li><b>Earned Leave (EL):</b> " + elBalance + "</li>"
                                + "<li><b>Total Leave Balance:</b> " + totalBalance + "</li>"
                                + "<li><b>Total Leave Taken in Current Quarter:</b> " + totalLeavesTakenThisQuarter
                                + "</li>"
                                + "<li><b>Total WFH Taken in Current Quarter:</b> " + totalWFHQuaterly + "</li>"
                                + "</ul>"
                                + "<div>"
                                + "<p><b>NOTE: Please approve or deny this request via Teamsphere.</b></p>"
                                + (isWFHMoreThan3Days
                                                ? "<p><b>SPECIAL NOTICE: This WFH request is for more than 3 days and has been redirected to the Account Manager for approval.</b></p>"
                                                : "<p><b>WFH Requests can only be approved by manager.</b></p>")
                                + "<p><b>Leave Requests can be approved by any leads involved in this mail.</b></p>"
                                + "</div>"
                                + "</body></html>";
        }

}
