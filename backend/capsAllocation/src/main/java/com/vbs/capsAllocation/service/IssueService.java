package com.vbs.capsAllocation.service;

import com.vbs.capsAllocation.dto.CommentDTO;
import com.vbs.capsAllocation.dto.IssueDTO;
import com.vbs.capsAllocation.model.Comment;
import com.vbs.capsAllocation.model.Employee;
import com.vbs.capsAllocation.model.IssueDetails;
import com.vbs.capsAllocation.repository.EmployeeRepository; // Assuming this exists
import com.vbs.capsAllocation.repository.IssueRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final EmailService emailService;
    private final EmployeeRepository employeeRepository;

    private static final List<String> ADMIN_EMAILS = Arrays.asList("piyush.mishra@highspring.in",
            "vaibhav.rajoriya@highspring.in");

    public IssueService(IssueRepository issueRepository, EmailService emailService,
            EmployeeRepository employeeRepository) {
        this.issueRepository = issueRepository;
        this.emailService = emailService;
        this.employeeRepository = employeeRepository;
    }

    public IssueDetails createIssue(IssueDTO issueDTO, String username) {
        IssueDetails issue = new IssueDetails();
        issue.setTitle(issueDTO.getTitle());
        issue.setDescription(issueDTO.getDescription());
        issue.setType(issueDTO.getType());
        issue.setPriority(issueDTO.getPriority());
        issue.setReporterEmail(issueDTO.getReporterEmail());
        issue.setCcEmails(issueDTO.getCcEmails());
        issue.setFeature(issueDTO.getFeature());
        issue.setFeedbackType(issueDTO.getFeedbackType());
        issue.setStepsToReproduce(issueDTO.getStepsToReproduce());
        issue.setSeverity(issueDTO.getSeverity());
        issue.setOccurrenceDate(issueDTO.getOccurrenceDate());
        issue.setAttachments(issueDTO.getAttachments());

        // Find reporter by username/LDAP
        Employee reporter = employeeRepository.findByLdap(username).orElse(null);
        if (reporter != null) {
            issue.setReporter(reporter);
            // Fallback if reporter email is not provided in DTO
            if (issue.getReporterEmail() == null || issue.getReporterEmail().isEmpty()) {
                issue.setReporterEmail(reporter.getEmail());
            }
        }

        IssueDetails savedIssue = issueRepository.save(issue);
        sendIssueCreatedEmail(savedIssue);
        return savedIssue;
    }

    public IssueDetails updateIssue(Long id, IssueDTO issueDTO, String username) {
        IssueDetails issue = issueRepository.findById(id).orElseThrow(() -> new RuntimeException("Issue not found"));

        String oldStatus = issue.getStatus();
        String oldType = issue.getType();

        if (issueDTO.getStatus() != null)
            issue.setStatus(issueDTO.getStatus());
        if (issueDTO.getType() != null)
            issue.setType(issueDTO.getType());
        if (issueDTO.getPriority() != null)
            issue.setPriority(issueDTO.getPriority());
        if (issueDTO.getDescription() != null)
            issue.setDescription(issueDTO.getDescription());
        if (issueDTO.getCcEmails() != null)
            issue.setCcEmails(issueDTO.getCcEmails());

        // Auto-comment on Status Change
        if (issueDTO.getStatus() != null && !oldStatus.equals(issueDTO.getStatus())) {
            Comment systemComment = new Comment();
            systemComment.setContent("Status changed from **" + oldStatus + "** to **" + issueDTO.getStatus() + "**");
            systemComment.setIssue(issue);

            // Find author
            Employee author = employeeRepository.findByLdap(username).orElse(null);
            if (author != null) {
                systemComment.setAuthorName(author.getFirstName() + " " + author.getLastName());
                systemComment.setAuthorEmail(author.getEmail());
                systemComment.setAuthorRole(author.getLevel()); // Or "System" if preferred, but user requested "person
                                                                // like a normal bug"
            } else {
                systemComment.setAuthorName(username); // Fallback
                systemComment.setAuthorEmail(username); // Fallback
                systemComment.setAuthorRole("USER");
            }

            issue.getComments().add(systemComment);
        }

        IssueDetails savedIssue = issueRepository.save(issue);

        if (!oldStatus.equals(savedIssue.getStatus()) || !oldType.equals(savedIssue.getType())) {
            sendIssueUpdatedEmail(savedIssue, oldStatus, oldType);
        }

        return savedIssue;
    }

    public List<IssueDetails> getAllIssues() {
        return issueRepository.findAll();
    }

    public Comment addComment(Long issueId, CommentDTO commentDTO, String username) {
        IssueDetails issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        Comment comment = new Comment();
        comment.setContent(commentDTO.getContent());
        comment.setIssue(issue);

        // Find author
        Employee author = employeeRepository.findByLdap(username).orElse(null);
        if (author != null) {
            comment.setAuthorName(author.getFirstName() + " " + author.getLastName());
            comment.setAuthorEmail(author.getEmail());
            comment.setAuthorRole(author.getLevel());
        } else {
            comment.setAuthorName(username); // Fallback
        }

        if (commentDTO.getParentId() != null) {
            Comment parent = issue.getComments().stream()
                    .filter(c -> c.getId().equals(commentDTO.getParentId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            comment.setParent(parent);
            parent.getReplies().add(comment);
        } else {
            issue.getComments().add(comment);
        }

        issueRepository.save(issue);
        sendCommentNotification(issue, comment, commentDTO.getMentions());

        // Return the saved comment (might need to fetch back from issue to get ID if
        // cascade persist didn't populate it immediately in this context, but usually
        // it does)
        return comment;
    }

    public List<String> getAllEmployeeEmails() {
        return employeeRepository.findAll().stream()
                .map(Employee::getEmail)
                .filter(email -> email != null && !email.isEmpty())
                .collect(Collectors.toList());
    }

    private void sendCommentNotification(IssueDetails issue, Comment comment, List<String> mentions) {
        String subject = "[" + issue.getStatus().toUpperCase() + "] Comment on Issue: " + issue.getTitle();
        String body = "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; }"
                +
                ".email-container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); overflow: hidden; }"
                +
                ".header { background: linear-gradient(135deg, #1967d2 0%, #1557b0 100%); color: #ffffff; padding: 24px; text-align: center; }"
                +
                ".header h2 { margin: 0; font-size: 18px; font-weight: 500; }" +
                ".content { padding: 24px; }" +
                ".message { background-color: #e8f0fe; border-left: 4px solid #1967d2; padding: 12px 16px; margin-bottom: 20px; border-radius: 0 4px 4px 0; }"
                +
                ".comment-box { background-color: #f8f9fa; border-radius: 8px; padding: 16px; margin-top: 16px; }" +
                ".author { font-weight: 600; color: #202124; margin-bottom: 8px; }" +
                ".comment-content { color: #5f6368; line-height: 1.6; }" +
                ".footer { padding: 16px 24px; background-color: #f8f9fa; text-align: center; border-top: 1px solid #e0e0e0; }"
                +
                ".footer a { color: #1967d2; text-decoration: none; font-weight: 500; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='email-container'>" +
                "<div class='header'><h2>TeamSphere Issue Tracker</h2></div>" +
                "<div class='content'>" +
                "<div class='message'>New comment added on Issue #" + issue.getId() + ": <strong>" + issue.getTitle()
                + "</strong></div>" +
                "<div class='comment-box'>" +
                "<div class='author'>" + comment.getAuthorName() + " commented:</div>" +
                "<div class='comment-content'>" + comment.getContent() + "</div>" +
                "</div>" +
                "</div>" +
                "<div class='footer'>" +
                "<a href='https://teamsphere.in/issues/" + issue.getId() + "'>View Issue Details</a>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        // Send to reporter and admins
        List<String> toList = new java.util.ArrayList<>(ADMIN_EMAILS);
        if (issue.getReporterEmail() != null)
            toList.add(issue.getReporterEmail());

        // Add Mentions to TO list
        if (mentions != null && !mentions.isEmpty()) {
            for (String mention : mentions) {
                if (mention == null || mention.trim().isEmpty())
                    continue;

                String cleanMention = mention.trim();

                if (cleanMention.contains("@")) {
                    // Assume it's an email
                    toList.add(cleanMention);
                } else {
                    // Assume it's an LDAP or Username -> Resolve to Email
                    employeeRepository.findByLdap(cleanMention).ifPresent(emp -> {
                        if (emp.getEmail() != null && !emp.getEmail().isEmpty()) {
                            toList.add(emp.getEmail());
                        }
                    });
                }
            }
        }

        // CC existing CCs
        List<String> ccList = new java.util.ArrayList<>();
        if (issue.getCcEmails() != null && !issue.getCcEmails().isEmpty()) {
            String[] ccs = issue.getCcEmails().split(",");
            for (String cc : ccs) {
                ccList.add(cc.trim());
            }
        }

        emailService.sendEmail(toList, ccList, subject, body);
    }

    private void sendIssueCreatedEmail(IssueDetails issue) {
        String subject = "[" + issue.getStatus().toUpperCase() + "] New Issue: " + issue.getTitle();
        String body = buildEmailBody(issue, "A new issue has been reported.");

        // Combine reporter + admin emails for 'TO' list if needed, or just send to
        // admins and cc reporter
        // Requirement: mail to admins and reporter, cc others.
        // EmailService.sendEmail(to, subject, body, cc) supports single string CC.
        // We have a list version too: sendEmail(List<String> toList, List<String>
        // ccList, String subject, String body)

        List<String> toList = new java.util.ArrayList<>(ADMIN_EMAILS);
        if (issue.getReporterEmail() != null && !issue.getReporterEmail().isEmpty()) {
            toList.add(issue.getReporterEmail());
        }

        List<String> ccList = new java.util.ArrayList<>();
        if (issue.getCcEmails() != null && !issue.getCcEmails().isEmpty()) {
            String[] ccs = issue.getCcEmails().split(",");
            for (String cc : ccs) {
                ccList.add(cc.trim());
            }
        }

        emailService.sendEmail(toList, ccList, subject, body);
    }

    private void sendIssueUpdatedEmail(IssueDetails issue, String oldStatus, String oldType) {
        String subject = "[" + issue.getStatus().toUpperCase() + "] Issue Updated: " + issue.getTitle();
        String body = buildEmailBody(issue, "The issue has been updated.<br><br>" +
                "<strong style='color: #1967d2;'>Change Summary:</strong><br>" +
                "<span style='color: #666;'>Status:</span> " + oldStatus + " → <strong>" + issue.getStatus()
                + "</strong><br>" +
                "<span style='color: #666;'>Type:</span> " + oldType + " → <strong>" + issue.getType() + "</strong>");

        List<String> toList = new java.util.ArrayList<>(ADMIN_EMAILS);
        if (issue.getReporterEmail() != null && !issue.getReporterEmail().isEmpty()) {
            toList.add(issue.getReporterEmail());
        }

        List<String> ccList = new java.util.ArrayList<>();
        if (issue.getCcEmails() != null && !issue.getCcEmails().isEmpty()) {
            String[] ccs = issue.getCcEmails().split(",");
            for (String cc : ccs) {
                ccList.add(cc.trim());
            }
        }

        emailService.sendEmail(toList, ccList, subject, body);
    }

    private String buildEmailBody(IssueDetails issue, String headerMessage) {
        String reporterInfo = issue.getReporter() != null
                ? issue.getReporter().getFirstName() + " " + issue.getReporter().getLastName()
                : (issue.getReporterEmail() != null ? issue.getReporterEmail() : "N/A");

        return "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; }"
                +
                ".email-container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); overflow: hidden; }"
                +
                ".header { background: linear-gradient(135deg, #1967d2 0%, #1557b0 100%); color: #ffffff; padding: 24px; text-align: center; }"
                +
                ".header h2 { margin: 0; font-size: 18px; font-weight: 500; }" +
                ".content { padding: 24px; }" +
                ".message { background-color: #e8f0fe; border-left: 4px solid #1967d2; padding: 12px 16px; margin-bottom: 20px; border-radius: 0 4px 4px 0; }"
                +
                ".details-table { width: 100%; border-collapse: collapse; margin-top: 16px; }" +
                ".details-table th { text-align: left; padding: 12px; background-color: #f8f9fa; border-bottom: 2px solid #e0e0e0; color: #5f6368; font-weight: 600; font-size: 12px; text-transform: uppercase; }"
                +
                ".details-table td { padding: 12px; border-bottom: 1px solid #e0e0e0; color: #202124; }" +
                ".details-table tr:last-child td { border-bottom: none; }" +
                ".badge { display: inline-block; padding: 4px 10px; border-radius: 12px; font-size: 12px; font-weight: 500; }"
                +
                ".badge-status { background-color: #e8f0fe; color: #1967d2; }" +
                ".badge-priority { background-color: #fce8e6; color: #c5221f; }" +
                ".badge-type { background-color: #e6f4ea; color: #137333; }" +
                ".footer { padding: 16px 24px; background-color: #f8f9fa; text-align: center; border-top: 1px solid #e0e0e0; }"
                +
                ".footer a { color: #1967d2; text-decoration: none; font-weight: 500; }" +
                ".footer a:hover { text-decoration: underline; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='email-container'>" +
                "<div class='header'><h2>TeamSphere Issue Tracker</h2></div>" +
                "<div class='content'>" +
                "<div class='message'>" + headerMessage + "</div>" +
                "<table class='details-table'>" +
                "<tr><th colspan='2'>Issue Details</th></tr>" +
                "<tr><td style='width: 120px; font-weight: 500; color: #5f6368;'>Issue ID</td><td>#" + issue.getId()
                + "</td></tr>" +
                "<tr><td style='font-weight: 500; color: #5f6368;'>Title</td><td><strong>" + issue.getTitle()
                + "</strong></td></tr>" +
                "<tr><td style='font-weight: 500; color: #5f6368;'>Type</td><td><span class='badge badge-type'>"
                + issue.getType() + "</span></td></tr>" +
                "<tr><td style='font-weight: 500; color: #5f6368;'>Priority</td><td><span class='badge badge-priority'>"
                + issue.getPriority() + "</span></td></tr>" +
                "<tr><td style='font-weight: 500; color: #5f6368;'>Status</td><td><span class='badge badge-status'>"
                + issue.getStatus() + "</span></td></tr>" +
                "<tr><td style='font-weight: 500; color: #5f6368;'>Reporter</td><td>" + reporterInfo + "</td></tr>" +
                "<tr><td style='font-weight: 500; color: #5f6368;'>Description</td><td>"
                + (issue.getDescription() != null ? issue.getDescription() : "N/A") + "</td></tr>" +
                "</table>" +
                "</div>" +
                "<div class='footer'>" +
                "<a href='https://teamsphere.in/issues/" + issue.getId() + "'>View Issue Details</a>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}
