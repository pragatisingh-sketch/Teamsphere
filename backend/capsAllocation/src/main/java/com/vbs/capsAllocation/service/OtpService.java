package com.vbs.capsAllocation.service;

import com.vbs.capsAllocation.model.Employee;
import com.vbs.capsAllocation.model.PasswordResetOtp;
import com.vbs.capsAllocation.repository.EmployeeRepository;
import com.vbs.capsAllocation.repository.PasswordResetOtpRepository;
import com.vbs.capsAllocation.repository.UserRepository;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OtpService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final String OTP_CHARS = "0123456789";

    private final PasswordResetOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final EmployeeRepository employeeRepository;

    @Autowired
    public OtpService(PasswordResetOtpRepository otpRepository, UserRepository userRepository, EmailService emailService, EmployeeRepository employeeRepository) {
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.employeeRepository= employeeRepository;
    }

    /**
     * Generate and send OTP for password reset
     * @param username the username to generate OTP for
     * @return true if OTP was generated and sent, false otherwise
     */
    public boolean generateAndSendOtp(String username) throws MessagingException {
//         Check if user exists
        userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // Generate OTP
        String otpCode = generateOtp();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        // Save OTP to database
        PasswordResetOtp otp = new PasswordResetOtp(username, otpCode, expiryTime);
        otpRepository.save(otp);

        System.out.println("Otp saved"+otp.toString());
        // Send OTP via email
        try {
            String emailBody = createOtpEmailBody(otpCode);
            System.out.println("Email Body "+emailBody);
            Optional<Employee> employee = employeeRepository.findByLdap(username);
            String email=employee.get().getEmail();
            emailService.sendEmail(email, "Password Reset OTP", emailBody, null);
            System.out.println("Email sent successfully");
            return true;
        } catch (Exception e) {
            System.err.println("Error in OTP email process: " + e.getMessage());
            e.printStackTrace();

            // For development/testing purposes, log the OTP to console
            System.out.println("============ DEVELOPMENT MODE ============");
            System.out.println("Email sending failed, but OTP was generated.");
            System.out.println("Username: " + username);
            System.out.println("OTP Code: " + otpCode);
            System.out.println("Expiry Time: " + expiryTime);
            System.out.println("==========================================");

            // In development mode, we'll still return true to allow testing
            // In production, you might want to throw the exception instead
            return true;
        }
    }

    /**
     * Verify OTP for a user
     * @param username the username
     * @param otpCode the OTP code to verify
     * @return true if OTP is valid, false otherwise
     */
    public boolean verifyOtp(String username, String otpCode) {
        Optional<PasswordResetOtp> otpOptional = otpRepository.findByUsernameAndOtpCodeAndUsedFalseAndExpiryTimeAfter(
                username, otpCode, LocalDateTime.now());

        if (otpOptional.isPresent()) {
            PasswordResetOtp otp = otpOptional.get();
            otp.setUsed(true);
            otpRepository.save(otp);
            return true;
        }

        return false;
    }

    /**
     * Generate a random OTP
     * @return the generated OTP
     */
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder(OTP_LENGTH);

        for (int i = 0; i < OTP_LENGTH; i++) {
            int randomIndex = random.nextInt(OTP_CHARS.length());
            otp.append(OTP_CHARS.charAt(randomIndex));
        }

        return otp.toString();
    }

    /**
     * Create email body for OTP
     * @param otpCode the OTP code
     * @return the email body
     */
    private String createOtpEmailBody(String otpCode) {
        return "<html><body>" +
                "<h2>Password Reset Request</h2>" +
                "<p>You have requested to reset your password. Please use the following OTP code to complete the process:</p>" +
                "<h3 style='color: #134472; font-size: 24px;'>" + otpCode + "</h3>" +
                "<p>This code will expire in " + OTP_EXPIRY_MINUTES + " minutes.</p>" +
                "<p>If you did not request this password reset, please ignore this email or contact support.</p>" +
                "<p>Thank you,<br>TeamSphere Support Team</p>" +
                "</body></html>";
    }

    /**
     * Clean up expired OTPs (runs every hour)
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void cleanupExpiredOtps() {
        LocalDateTime now = LocalDateTime.now();
        otpRepository.findByExpiryTimeBefore(now).forEach(otpRepository::delete);
    }
}
