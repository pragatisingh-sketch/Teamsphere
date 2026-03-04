import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { AuthService } from '../auth.service';
import { Router } from '@angular/router';
import { NotificationService } from '../shared/notification.service';

@Component({
  selector: 'app-password-reset',
  templateUrl: './password-reset.component.html',
  styleUrls: ['./password-reset.component.css']
})
export class PasswordResetComponent implements OnInit {
  passwordResetForm!: FormGroup;
  hidePassword: boolean = true;
  hideConfirmPassword: boolean = true;
  isSubmitting: boolean = false;

  // Password pattern - same as used in signup component
  private readonly passwordPattern = '^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.initializeForm();
  }

  private initializeForm(): void {
    this.passwordResetForm = this.fb.group({
      newPassword: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern(this.passwordPattern)
      ]],
      confirmPassword: ['', [
        Validators.required
      ]]
    }, {
      validators: this.passwordMatchValidator
    });
  }

  // Custom validator to check if passwords match
  private passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const newPassword = control.get('newPassword');
    const confirmPassword = control.get('confirmPassword');

    if (!newPassword || !confirmPassword) {
      return null;
    }

    if (newPassword.value !== confirmPassword.value) {
      return { passwordMismatch: true };
    }

    return null;
  }

  // Getter methods for easy access to form controls
  get newPassword() {
    return this.passwordResetForm.get('newPassword');
  }

  get confirmPassword() {
    return this.passwordResetForm.get('confirmPassword');
  }

  // Check if form has password mismatch error
  get hasPasswordMismatch(): boolean {
    return this.passwordResetForm.hasError('passwordMismatch') &&
           this.confirmPassword?.touched &&
           this.confirmPassword?.value;
  }

  onSubmit(): void {
    if (this.passwordResetForm.invalid) {
      this.markFormGroupTouched();
      return;
    }

    this.isSubmitting = true;
    const newPasswordValue = this.newPassword?.value;

    this.authService.resetPassword(newPasswordValue).subscribe({
      next: (response) => {
        this.authService.storeToken(response.token, response.role, response.username);
        this.notificationService.showSuccess('Password changed successfully');

        if (response.role !== 'USER') {
          this.router.navigate(['admin/extdashboard']);
        } else {
          this.router.navigate(['time-entry']);
        }
      },
      error: (error) => {
        this.isSubmitting = false;
        let errorMessage = 'Failed to change password';

        if (error.error?.message) {
          errorMessage = error.error.message;
        } else if (error.status === 400) {
          errorMessage = 'Invalid password format. Please check the requirements.';
        } else if (error.status === 401) {
          errorMessage = 'Session expired. Please login again.';
        } else if (error.status === 500) {
          errorMessage = 'Server error. Please try again later.';
        }

        this.notificationService.showError(errorMessage);
      }
    });
  }

  private markFormGroupTouched(): void {
    Object.keys(this.passwordResetForm.controls).forEach(key => {
      const control = this.passwordResetForm.get(key);
      control?.markAsTouched();
    });
  }
}
