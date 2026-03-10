package com.vbs.capsAllocation.controller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for role management in the system
 * 
 * @author Piyush Mishra
 * @version 1.0
 */
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/superadmin/only")
    public String superAdminOnly() {
        return "This is a SUPER_ADMIN-only page.";
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @GetMapping("/admin/only")
    public String adminOnly() {
        return "This is an ADMIN-only page.";
    }

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @GetMapping("/user/only")
    public String userOnly() {
        return "This is a USER or higher access page.";
    }
}
