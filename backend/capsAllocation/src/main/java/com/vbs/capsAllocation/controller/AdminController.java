package com.vbs.capsAllocation.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vbs.capsAllocation.model.Employee;
import com.vbs.capsAllocation.model.LeadsRequest;
import com.vbs.capsAllocation.model.User;
import com.vbs.capsAllocation.model.Role;
import com.vbs.capsAllocation.repository.UserRepository;
import com.vbs.capsAllocation.service.EmployeeService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for admin operations in the system
 *
 * @author Piyush Mishra
 * @version 1.0
 */
@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class AdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    EmployeeService employeeService;

    public AdminController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Register a new user in the system
     * 
     * @param username The username for the new user
     * @param password The password for the new user
     * @param role     The role for the new user
     * @return Response with success/failure message
     */
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN_OPS_MANAGER')")
    public ResponseEntity<?> registerUser(@RequestParam String username, @RequestParam String password,
            @RequestParam Role role) {
        try {
            // Validate input parameters
            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Username is required",
                        "success", false));
            }

            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Password is required",
                        "success", false));
            }

            if (role == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Role is required",
                        "success", false));
            }

            // Validate password requirements
            if (password.length() < 8) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Password must be at least 8 characters long",
                        "success", false));
            }

            // Check if username already exists
            if (userRepository.existsByUsername(username.trim())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                        "message", "Username already exists. Please choose a different username.",
                        "success", false));
            }

            // Create new user
            User newUser = new User();
            newUser.setUsername(username.trim());
            newUser.setPassword(passwordEncoder.encode(password));
            newUser.setRole(role);
            newUser.setPasswordChangeRequired(false); // Set default value

            // Save user to database
            userRepository.save(newUser);

            // Log successful registration

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "User registration successful.",
                    "success", true,
                    "username", username.trim()));

        } catch (DataIntegrityViolationException e) {
            // Handle database constraint violations
            System.err.println("Database constraint violation during registration: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "message", "Username already exists. Please choose a different username.",
                    "success", false));
        } catch (Exception e) {
            // Handle any other unexpected errors
            System.err.println("Unexpected error during user registration: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "An error occurred during registration. Please try again later.",
                    "success", false));
        }
    }

    /**
     * Reset a user's password to the default value
     * Only ADMIN_OPS_MANAGER, LEAD, and MANAGER roles can reset passwords
     */
    @PostMapping("/reset-password-postman")
    public ResponseEntity<?> resetPasswordForUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request) {

        System.out.println("Password reset request received: " + request);
        System.out.println("Headers: " + request.keySet());
        System.out.println("Request body: " + request);

        if (userDetails == null) {
            System.out.println("ERROR: UserDetails is null - authentication issue");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication required");
        }

        // Get the current user's role
        List<String> roles = userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(Collectors.toList());

        System.out.println("User roles: " + roles);
        System.out.println("Current user: " + userDetails.getUsername());

        // Check if user has appropriate role
        boolean hasPermission = roles.contains("ROLE_ADMIN_OPS_MANAGER") ||
                roles.contains("ROLE_LEAD") ||
                roles.contains("ROLE_MANAGER");

        if (!hasPermission) {
            System.out.println("Permission denied for user: " + userDetails.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You don't have permission to reset passwords.");
        }

        String username = request.get("ldap");
        String newPassword = request.get("newPassword");

        if (username == null || newPassword == null) {
            return ResponseEntity.badRequest().body("Missing 'ldap' or 'newPassword' in request body.");
        }

        // Validate that the default password is being used
        if (!"vbsllp".equals(newPassword)) {
            return ResponseEntity.badRequest().body("Password can only be reset to the default value.");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ldap: " + username));

        // Log the password reset for audit purposes
        System.out.println("Password reset for user " + username + " by " + userDetails.getUsername());

        // Update password and mark as changed
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangeRequired(true);
        userRepository.save(user);

        return ResponseEntity.ok("Password reset successfully for user: " + username);
    }
}
