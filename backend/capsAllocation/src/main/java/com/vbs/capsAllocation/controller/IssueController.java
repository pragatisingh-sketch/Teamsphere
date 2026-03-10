package com.vbs.capsAllocation.controller;

import com.vbs.capsAllocation.dto.IssueDTO;
import com.vbs.capsAllocation.model.IssueDetails;
import com.vbs.capsAllocation.service.IssueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/issues")
@CrossOrigin(origins = "*") // Adjust as per security requirements
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @PostMapping
    public ResponseEntity<IssueDetails> createIssue(@RequestBody IssueDTO issueDTO, Principal principal) {
        // Principal is null if security not configured to inject it, but usually
        // standard Spring Security
        // For now, if principal is null, we might need to rely on DTO or handle
        // gracefully
        String username = (principal != null) ? principal.getName() : "anonymous";
        // Or if you want to support manual username passing for testing:
        // if (username.equals("anonymous") && issueDTO.getReporterName() != null)
        // username = issueDTO.getReporterName();
        // But better to rely on secure principal.

        IssueDetails createdIssue = issueService.createIssue(issueDTO, username);
        return ResponseEntity.ok(createdIssue);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IssueDetails> updateIssue(@PathVariable Long id, @RequestBody IssueDTO issueDTO,
            Principal principal) {
        String username = (principal != null) ? principal.getName() : "anonymous";
        IssueDetails updatedIssue = issueService.updateIssue(id, issueDTO, username);
        return ResponseEntity.ok(updatedIssue);
    }

    @GetMapping
    public ResponseEntity<List<IssueDetails>> getAllIssues() {
        return ResponseEntity.ok(issueService.getAllIssues());
    }

    @GetMapping("/emails")
    public ResponseEntity<List<String>> getAllEmails() {
        return ResponseEntity.ok(issueService.getAllEmployeeEmails());
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<com.vbs.capsAllocation.model.Comment> addComment(@PathVariable Long id,
            @RequestBody com.vbs.capsAllocation.dto.CommentDTO commentDTO, Principal principal) {
        String username = (principal != null) ? principal.getName() : "anonymous";
        return ResponseEntity.ok(issueService.addComment(id, commentDTO, username));
    }
}
