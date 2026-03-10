package com.vbs.capsAllocation.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "password_reset_otps")
public class PasswordResetOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String otpCode;

    @Column(nullable = false)
    private LocalDateTime expiryTime;

    private boolean used;

    // Constructor for creating a new OTP
    public PasswordResetOtp(String username, String otpCode, LocalDateTime expiryTime) {
        this.username = username;
        this.otpCode = otpCode;
        this.expiryTime = expiryTime;
        this.used = false;
    }

    // Check if OTP is expired
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }
}
