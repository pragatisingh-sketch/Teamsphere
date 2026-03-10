import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { NotificationService } from '../shared/notification.service';

@Component({
  selector: 'app-forgot-password',
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.css']
})
export class ForgotPasswordComponent {
  username: string = '';
  isSubmitting: boolean = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  onSubmit() {
    if (!this.username || this.username.trim() === '') {
      this.notificationService.showNotification({
        type: 'error',
        message: 'Please enter your username'
      });
      return;
    }

    this.isSubmitting = true;
    console.log('Submitting forgot password request for username:', this.username);

    // Store username in session storage immediately as a backup
    sessionStorage.setItem('reset_username', this.username);

    this.authService.forgotPassword(this.username).subscribe({
      next: (response) => {
        console.log('Forgot password response:', response);
        this.isSubmitting = false;

        this.notificationService.showSuccess(response.message || 'OTP has been sent to your email');

        // Navigate to OTP verification page with username - no setTimeout
        console.log("Navigating to otp-verification with username:", this.username);

        // Use navigateByUrl instead of navigate for more direct routing
        this.router.navigateByUrl(`/otp-verification?username=${encodeURIComponent(this.username)}`);
      },
      error: (error) => {
        console.error('Forgot password error:', error);
        this.isSubmitting = false;

        // Show error notification
        const errorMessage = error.error?.message || 'Failed to process request. Please try again.';
        this.notificationService.showError(errorMessage);

        // For development/testing purposes, navigate to OTP verification anyway
        console.log('Development mode: Navigating to OTP verification despite error');

        // Use navigateByUrl instead of navigate for more direct routing
        this.router.navigateByUrl(`/otp-verification?username=${encodeURIComponent(this.username)}`);
      }
    });
  }
}
