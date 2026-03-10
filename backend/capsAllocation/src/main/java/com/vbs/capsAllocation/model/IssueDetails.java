package com.vbs.capsAllocation.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "issue_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssueDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String type; // e.g., Bug, Suggestion, Enhancement

    private String status; // e.g., Open, Resolved, Fixed

    private String priority; // e.g., High, Medium, Low

    private String reporterEmail;

    private String ccEmails; // Comma separated emails

    @ManyToOne
    @JoinColumn(name = "reporter_id")
    private Employee reporter;

    private String feature;

    private String feedbackType; // e.g., UI, Backend, Performance

    @Column(columnDefinition = "TEXT")
    private String stepsToReproduce;

    private String severity; // e.g., Critical, Major, Minor

    private LocalDateTime occurrenceDate;

    @Column(columnDefinition = "TEXT")
    private String attachments; // Comma separated URLs

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private java.util.List<Comment> comments = new java.util.ArrayList<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "Open";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
