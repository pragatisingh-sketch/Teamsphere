package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssueDTO {
    private Long id;
    private String title;
    private String description;
    private String type;
    private String status;
    private String priority;
    private String reporterEmail;
    private String ccEmails;
    private String reporterName;
    private String feature;
    private String feedbackType;
    private String stepsToReproduce;
    private String severity;
    private LocalDateTime occurrenceDate;
    private String attachments;
    private java.util.List<CommentDTO> comments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
