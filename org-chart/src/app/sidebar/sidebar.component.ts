import { Component, OnInit, OnDestroy } from '@angular/core';
import { UserService } from '../user.service';
import { AuthService } from '../auth.service';
import { Router } from '@angular/router';
import { PermissionService, UserRole } from '../shared/permission.service';
import { jwtDecode } from 'jwt-decode';
import { NotificationCountService, RequestCounts } from '../shared/notification-count.service';
import { Subscription } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from '../confirm-dialog/confirmation-dialog.component';
import { ReportIssueDialogComponent } from '../issue-tracker/report-issue-dialog/report-issue-dialog.component';
import { FAQDialogComponent } from '../shared/components/faq-dialog/faq-dialog.component';
import { NotificationService } from '../shared/notification.service';
import { ReleaseService } from '../services/release.service';

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent implements OnInit, OnDestroy {
  showForm = false;
  collapsed = false;
  userName: string = '';
  userRole: string = '';
  isAdminUser = false;
  requestCounts: RequestCounts = { pending: 0, approved: 0, rejected: 0, total: 0 };
  private countSubscription: Subscription | undefined;
  hasActiveDelegation = false;
  delegationPartnerLdap: string | null = null;
  isDelegator: boolean = false;
  currentVersion: string = '';

  constructor(
    private userService: UserService,
    private authService: AuthService,
    private router: Router,
    private permissionService: PermissionService,
    private notificationCountService: NotificationCountService,
    private dialog: MatDialog,
    private notificationService: NotificationService,
    private releaseService: ReleaseService
  ) {
    this.getUserInfo();
  }

  ngOnInit(): void {
    // Subscribe to role changes
    this.authService.role$.subscribe(role => {
      if (role) {
        this.verifyAndUpdateRole();
      }
    });

    // Initial verification
    this.verifyAndUpdateRole();

    // Subscribe to request counts for admin users
    this.countSubscription = this.notificationCountService.requestCounts$.subscribe(
      counts => {
        this.requestCounts = counts;
      }
    );

    this.checkActiveDelegation();
    this.fetchCurrentVersion();
  }

  fetchCurrentVersion(): void {
    this.releaseService.getCurrentVersion().subscribe({
      next: (response) => {
        if (response.status === 'success' && response.data?.version) {
          this.currentVersion = response.data.version;
        }
      },
      error: () => {
        // Silently fail - version display is not critical
        this.currentVersion = '';
      }
    });
  }

  checkActiveDelegation() {
    const ldap = localStorage.getItem('username');
    if (ldap) {
      this.userService.getDelegationHistory(ldap).subscribe(
        (response) => {
          const activeDelegation = response.data.find((d: any) => d.status === 'ACTIVE');
          this.hasActiveDelegation = !!activeDelegation;
          if (activeDelegation) {
            // Check if current user is the delegator
            this.isDelegator = activeDelegation.delegatorLdap === ldap;

            // If I am the delegatee, show the delegator. If I am the delegator, show the delegatee.
            // Basically show the "other" person.
            this.delegationPartnerLdap = activeDelegation.delegateeLdap === ldap
              ? activeDelegation.delegatorLdap
              : activeDelegation.delegateeLdap;
          } else {
            this.delegationPartnerLdap = null;
            this.isDelegator = false;
          }
        },
        (error) => {
          console.error('Error checking delegation history', error);
        }
      );
    }
  }

  revertDelegation() {
    const ldap = localStorage.getItem('username');
    if (!ldap) return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Revert Delegation',
        message: 'Are you sure you want to revert your delegation and reclaim your role?',
        confirmButtonText: 'Revert'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.userService.revertDelegation(ldap).subscribe(
          (response) => {
            this.notificationService.showSuccess(response.message);
            this.hasActiveDelegation = false;
            this.delegationPartnerLdap = null;
            // Logout to force re-authentication and role refresh
            this.authService.logout();
          },
          (error) => {
            this.notificationService.showError(error.error?.message || 'Failed to revert delegation');
          }
        );
      }
    });
  }

  ngOnDestroy(): void {
    if (this.countSubscription) {
      this.countSubscription.unsubscribe();
    }
  }

  toggleSidebar() {
    this.collapsed = !this.collapsed;
  }

  toggleForm() {
    this.showForm = !this.showForm;
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
   * Check if the user is specifically an Admin Ops Manager
   */
  isAdminOpsManager(): boolean {
    return this.userRole === 'ADMIN_OPS_MANAGER';
  }

  /**
   * Check if user can manage releases (piyushmi or vrajoriya only)
   */
  isReleaseManager(): boolean {
    return this.releaseService.isAuthorizedUser();
  }

  logout() {
    this.userService.isAdmin = false;
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  /**
   * Gets user information and triggers role verification
   * This ensures that the displayed user info is consistent with the verified role
   */
  getUserInfo() {
    this.userName = localStorage.getItem('username') || 'User';
    this.userRole = localStorage.getItem('role') || 'USER';

    // Verify role whenever user info is accessed
    this.verifyAndUpdateRole();
  }

  /**
   * Get tooltip text for the request notification badge
   */
  getRequestTooltip(): string {
    const pendingCount = this.requestCounts.pending;
    const totalCount = this.requestCounts.total;

    if (pendingCount === 0) {
      return 'No pending requests';
    } else if (pendingCount === 1) {
      return `1 pending request out of ${totalCount} total`;
    } else {
      return `${pendingCount} pending requests out of ${totalCount} total`;
    }
  }
  openReportIssueDialog() {
    this.dialog.open(ReportIssueDialogComponent, {
      width: '600px',
      autoFocus: false
    });
  }

  openFaqDialog() {
    this.dialog.open(FAQDialogComponent, {
      width: '700px',
      maxHeight: '90vh',
      autoFocus: false
    });
  }
}
