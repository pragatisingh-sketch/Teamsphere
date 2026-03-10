package com.vbs.capsAllocation.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "holidays")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Holiday {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private LocalDate holidayDate;
    
    @Column(nullable = false, length = 255)
    private String holidayName;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false, length = 50)
    private String holidayType = "GOOGLE"; // GOOGLE, NATIONAL, REGIONAL, etc.
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "uploaded_by", length = 100)
    private String uploadedBy; // LDAP of the person who uploaded
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public Holiday(LocalDate holidayDate, String holidayName, String description, String uploadedBy) {
        this.holidayDate = holidayDate;
        this.holidayName = holidayName;
        this.description = description;
        this.uploadedBy = uploadedBy;
        this.holidayType = "GOOGLE";
        this.isActive = true;
    }
}
