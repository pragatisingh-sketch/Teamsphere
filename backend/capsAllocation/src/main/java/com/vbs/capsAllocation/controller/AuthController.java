package com.vbs.capsAllocation.controller;

import com.vbs.capsAllocation.dto.AuthRequest;
import com.vbs.capsAllocation.dto.AuthResponse;
import com.vbs.capsAllocation.model.Role;
import com.vbs.capsAllocation.model.User;
import com.vbs.capsAllocation.repository.UserRepository;
import com.vbs.capsAllocation.service.OtpService;
import com.vbs.capsAllocation.service.TokenBlacklistService;
import com.vbs.capsAllocation.util.JwtUtil;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import com.fasterxml.jackson.core.JsonProcessingException;


import java.util.Date;
import java.util.Map;

/**
 * Controller for handling authentication in the system
 *
 * @author Piyush Mishra
 * @version 1.0
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final OtpService otpService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                      TokenBlacklistService tokenBlacklistService, OtpService otpService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistService = tokenBlacklistService;
        this.otpService = otpService;
    }



    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest) {
        try {
            // Authenticate user with Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));

            // Get user from database
            User user = userRepository.findByUsername(authRequest.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Generate JWT token with user's actual role from database
            String token = jwtUtil.generateToken(authRequest.getUsername(), user.getRole().name());
            AuthResponse response = new AuthResponse(token, user.getRole().name(), authRequest.getUsername());

            // Handle null passwordChangeRequired field
            Boolean passwordChangeRequired = user.isPasswordChangeRequired();
            response.setPasswordChangeRequired(passwordChangeRequired != null ? passwordChangeRequired : false);

            // Log successful login
            System.out.println("Successful login for user: " + authRequest.getUsername());
            return ResponseEntity.ok(response);
        } catch (UsernameNotFoundException e) {
            // Log the error for debugging
            System.err.println("User not found: " + authRequest.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse("Invalid credentials. Please check your username and password.", null, authRequest.getUsername()));
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            // Log the error for debugging
            System.err.println("Bad credentials for user: " + authRequest.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse("Invalid credentials. Please check your username and password.", null, authRequest.getUsername()));
        } catch (org.springframework.security.authentication.DisabledException e) {
            // Log the error for debugging
            System.err.println("Account disabled for user: " + authRequest.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse("Your account has been disabled. Please contact administrator.", null, authRequest.getUsername()));
        } catch (org.springframework.security.authentication.LockedException e) {
            // Log the error for debugging
            System.err.println("Account locked for user: " + authRequest.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse("Your account has been locked. Please contact administrator.", null, authRequest.getUsername()));
        } catch (Exception e) {
            // Log the error for debugging
            System.err.println("Unexpected authentication error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse("An error occurred during login. Please try again later.", null, authRequest.getUsername()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String newPassword = request.get("newPassword");
        String username = userDetails.getUsername();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Update password and mark as changed
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangeRequired(false);
        userRepository.save(user);

        // Generate new token with updated info
        String token = jwtUtil.generateToken(username, user.getRole().name());
        return ResponseEntity.ok(new AuthResponse(token, user.getRole().name(),username));
    }

    @PostMapping("/check-password-status")
    public ResponseEntity<?> checkPasswordStatus(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return ResponseEntity.ok(Map.of(
            "passwordChangeRequired", user.isPasswordChangeRequired()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // Get token expiration date
            Date expiryDate = jwtUtil.getExpirationDateFromToken(token);

            // Add token to blacklist
            tokenBlacklistService.blacklistToken(token, expiryDate);

            return ResponseEntity.ok(Map.of(
                "message", "Logged out successfully"
            ));
        }

        return ResponseEntity.badRequest().body(Map.of(
            "message", "Invalid token format"
        ));
    }

    /**
     * Admin endpoint to check the token blacklist status
     * @return Blacklist status information
     */
    @GetMapping("/token-blacklist-status")
    @PreAuthorize("hasRole('ADMIN_OPS_MANAGER')")
    public ResponseEntity<?> getTokenBlacklistStatus() {
        int blacklistSize = tokenBlacklistService.getBlacklistSize();

        return ResponseEntity.ok(Map.of(
            "blacklistSize", blacklistSize,
            "timestamp", new Date()
        ));
    }

    /**
     * Initiate password reset by sending OTP to user's email
     * @param request Map containing username
     * @return Response with success/failure message
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String username = request.get("username");

        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", "Username is required"
            ));
        }

        try {
            // Check if user exists
            if (!userRepository.findByUsername(username).isPresent()) {
                // For security reasons, always return success even if user doesn't exist
                return ResponseEntity.ok(Map.of(
                    "message", "If your account exists, an OTP has been sent to your email"
                ));
            }

            try {
                // Generate and send OTP
                otpService.generateAndSendOtp(username);

                return ResponseEntity.ok(Map.of(
                    "message", "If your account exists, an OTP has been sent to your email"
                ));
            } catch (MessagingException e) {
                System.err.println("Email sending error in controller: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "Failed to send OTP email"
                ));
            } catch (Exception e) {
                System.err.println("General error in forgot password: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "An error occurred while processing your request"
                ));
            }
        } catch (Exception e) {
            System.err.println("Outer exception in forgot password: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "message", "An error occurred while processing your request"
            ));
        }
    }

    /**
     * Verify OTP entered by user
     * @param request Map containing username and OTP
     * @return Response with success/failure message
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String otpCode = request.get("otpCode");

        if (username == null || username.trim().isEmpty() || otpCode == null || otpCode.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", "Username and OTP are required"
            ));
        }

        boolean isValid = otpService.verifyOtp(username, otpCode);

        if (isValid) {
            return ResponseEntity.ok(Map.of(
                "message", "OTP verified successfully",
                "verified", true
            ));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "message", "Invalid or expired OTP",
                "verified", false
            ));
        }
    }

    /**
     * Reset password after OTP verification
     * @param request Map containing username, OTP, and new password
     * @return Response with success/failure message
     */
    @PostMapping("/reset-password-with-otp")
    public ResponseEntity<?> resetPasswordWithOtp(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String newPassword = request.get("newPassword");

        if (username == null || username.trim().isEmpty() || newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", "Username and new password are required"
            ));
        }

        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Update password
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setPasswordChangeRequired(false);
            userRepository.save(user);

            // Generate new token
            String token = jwtUtil.generateToken(username, user.getRole().name());

            return ResponseEntity.ok(new AuthResponse(token, user.getRole().name(), username));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "message", "User not found"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "message", "An error occurred while resetting password"
            ));
        }
    }
}
