import { Component, OnInit } from '@angular/core';
import { UserService } from '../user.service';
import { AuthService } from '../auth.service';
import { Router } from '@angular/router';
import { PermissionService, UserRole } from '../shared/permission.service';
import { jwtDecode } from 'jwt-decode';

import { MatDialog } from '@angular/material/dialog';
import { ReportIssueDialogComponent } from '../issue-tracker/report-issue-dialog/report-issue-dialog.component';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit {
  isAdminUser = false;

  constructor(
    private userService: UserService,
    private authService: AuthService,
    private router: Router,
    private permissionService: PermissionService,
    private dialog: MatDialog
  ) { }

  ngOnInit(): void {
    // Subscribe to role changes
    this.authService.role$.subscribe(role => {
      if (role) {
        this.verifyAndUpdateRole();
      }
    });

    // Initial verification
    this.verifyAndUpdateRole();
  }

  openReportIssueDialog() {
    this.dialog.open(ReportIssueDialogComponent, {
      width: '600px',
      autoFocus: false
    });
  }

  /**
   * Verifies the user's role from the JWT token and updates local state
   * This prevents users from manipulating localStorage to gain unauthorized access
   */
  verifyAndUpdateRole(): void {
    const token = this.authService.getToken();
    const storedRole = localStorage.getItem('role');

    if (!token) {
      // No token, user should be logged out
      this.isAdminUser = false;
      this.router.navigate(['/login']);
      return;
    }

    try {
      // Decode the JWT token to get the role claim
      const decodedToken: any = jwtDecode(token);
      const tokenRole = decodedToken.role;

      // Check if the role in localStorage matches the role in the token
      if (tokenRole !== storedRole) {
        console.error('Role mismatch detected! Token role:', tokenRole, 'Stored role:', storedRole);
        // Role mismatch detected, force logout
        this.authService.logout();
        this.isAdminUser = false;
        return;
      }

      // Use the permission service to check if the user has admin privileges
      const adminRoles: UserRole[] = ['LEAD', 'MANAGER', 'ACCOUNT_MANAGER', 'ADMIN_OPS_MANAGER'] as UserRole[];
      this.permissionService.hasAnyRole(adminRoles).subscribe(hasAdminRole => {
        this.isAdminUser = hasAdminRole;
      });

    } catch (error) {
      console.error('Error verifying user role:', error);
      // Token is invalid or tampered with, force logout
      this.authService.logout();
      this.isAdminUser = false;
    }
  }

  /**
   * Secure method to check if the user has admin privileges
   * Uses the verified role status instead of directly checking localStorage
   */
  isAdmin(): boolean {
    return this.isAdminUser;
  }

  /**
   * Secure logout method that properly cleans up state
   */
  logout() {
    this.userService.isAdmin = false;
    this.isAdminUser = false;
    this.authService.logout();
    // No need to call window.location.reload() as the authService.logout()
    // already navigates to the login page
  }

  requestForm() {
    this.router.navigate(['request-form']);
  }
}
