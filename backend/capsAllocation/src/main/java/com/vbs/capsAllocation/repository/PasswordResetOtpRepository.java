package com.vbs.capsAllocation.repository;

import com.vbs.capsAllocation.model.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {
    
    // Find the most recent valid OTP for a user
    @Query("SELECT o FROM PasswordResetOtp o WHERE o.username = ?1 AND o.used = false AND o.expiryTime > ?2 ORDER BY o.expiryTime DESC")
    Optional<PasswordResetOtp> findValidOtpForUser(String username, LocalDateTime now);
    
    // Find all expired OTPs
    List<PasswordResetOtp> findByExpiryTimeBefore(LocalDateTime now);
    
    // Find by username and OTP code
    Optional<PasswordResetOtp> findByUsernameAndOtpCodeAndUsedFalseAndExpiryTimeAfter(String username, String otpCode, LocalDateTime now);
}
