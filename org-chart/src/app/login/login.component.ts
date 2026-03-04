import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { UserService } from '../user.service';
import { NotificationService } from '../shared/notification.service';
import { ErrorMessageService } from '../shared/error-message.service';
import { LoginResponse } from '../model/user';
import { HttpErrorResponse } from '@angular/common/http';
import { LocationCacheService } from '../services/location-cache.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  username: string = '';
  password: string = '';
  hidePassword: boolean = true;
  showMaintenancePage: boolean = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private userService: UserService,
    private notificationService: NotificationService,
    private errorMessageService: ErrorMessageService,
    private locationCacheService: LocationCacheService
  ) { }

  ngOnInit() {
    const token = localStorage.getItem('authToken');
    const role = localStorage.getItem('role');
    const username = localStorage.getItem('username');

    // Warm-up location cache asynchronously 
    this.locationCacheService.getLocation().catch(err => {
      console.warn("Location cache warmup failed:", err);
    });

    if (token && role) {
      // Set admin status based on role
      const adminRoles = ['LEAD', 'MANAGER', 'ACCOUNT_MANAGER', 'ADMIN_OPS_MANAGER'];
      this.userService.isAdmin = adminRoles.includes(role);

      // Restore AuthService state
      this.authService.storeToken(token, role, username || '');

      // Redirect to the correct dashboard
      if (this.userService.isAdmin) {
        this.router.navigate(['reports']);
      } else {
        this.router.navigate(['vunno']);
      }
    }
  }

  onSubmit() {
    this.authService.login(this.username, this.password).subscribe({
      next: (response: LoginResponse) => {
        this.authService.storeToken(response.token, response.role, response.username);

        // Warm up fresh location
        this.locationCacheService.getLocation(true).catch(err => {
          console.warn("Location cache refresh failed:", err);
        });

        if (response.passwordChangeRequired) {
          this.router.navigate(['/password-reset']);
          return;
        }

        const adminRoles = ['LEAD', 'MANAGER', 'ACCOUNT_MANAGER', 'ADMIN_OPS_MANAGER'];
        this.userService.isAdmin = adminRoles.includes(response.role);

        if (this.userService.isAdmin) {
          this.router.navigate(['/reports']);
        } else {
          this.router.navigate(['vunno']);
        }
      },
      error: (err: HttpErrorResponse) => {
        console.error('Login error:', err);
        this.handleLoginError(err);
      },
    });
  }

  private handleLoginError(error: HttpErrorResponse) {
    if (this.errorMessageService.isNetworkError(error.status, error.message)) {
      this.showMaintenancePage = true;
      return;
    }

    const errorMessage = error.error?.message || error.message || '';
    if (this.errorMessageService.isMaintenanceError(errorMessage)) {
      this.showMaintenancePage = true;
      return;
    }

    if (error.status === 401 || error.status === 403) {
      const message = error.error?.message || 'Invalid credentials. Please check your username and password.';
      this.notificationService.showError(message);
      return;
    }

    if (error.status >= 500) {
      this.showMaintenancePage = true;
      return;
    }

    const fallbackMessage = 'Invalid credentials. Please try again.';
    const message = error.error?.message || fallbackMessage;
    this.notificationService.showError(message);
  }

  hideMaintenance() {
    this.showMaintenancePage = false;
  }
}
