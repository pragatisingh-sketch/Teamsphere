/**
 * Example of how to use the new error handling system
 * This file demonstrates the before/after patterns for error handling
 */

import { NotificationService } from './notification.service';
import { ErrorMessageService } from './error-message.service';

export class ErrorHandlingExample {

  constructor(
    private notificationService: NotificationService,
    private errorMessageService: ErrorMessageService
  ) {}

  // ❌ OLD WAY - Manual error handling (DON'T USE)
  oldErrorHandling(error: any) {
    let errorMessage = 'Failed to process request. Please try again.';
    if (error.error && error.error.message) {
      errorMessage = error.error.message;
    }

    this.notificationService.showNotification({
      type: 'error',
      message: errorMessage
    });
  }

  // ✅ NEW WAY - Simplified error handling (USE THIS)
  newErrorHandling(error: any) {
    // Option 1: Let the notification service handle everything
    const errorMessage = this.errorMessageService.extractErrorMessage(
      error, 
      'Failed to process request. Please try again.'
    );
    this.notificationService.showError(errorMessage);

    // Option 2: Even simpler - direct extraction
    this.notificationService.showError(
      error.error?.message || 'Failed to process request. Please try again.'
    );
  }

  // ✅ SUCCESS NOTIFICATIONS - Use simplified methods
  showSuccessMessage(response: any) {
    this.notificationService.showSuccess(
      response.message || 'Operation completed successfully!'
    );
  }

  // ✅ WARNING NOTIFICATIONS
  showWarningMessage(message: string) {
    this.notificationService.showWarning(message);
  }

  // ✅ INFO NOTIFICATIONS
  showInfoMessage(message: string) {
    this.notificationService.showInfo(message);
  }

  /**
   * Example of how database errors are transformed:
   * 
   * INPUT: "Could not execute statement [ERROR: duplicate key value violates unique constraint "ukrghs6qdg65k92vitg3aajsrpf" Detail: Key (ldap)=(tester) already exists.]"
   * OUTPUT: "This LDAP ID is already registered. Please use a different LDAP ID."
   * 
   * INPUT: "foreign key constraint violation on user_id"
   * OUTPUT: "The selected user is not valid or has been removed."
   * 
   * INPUT: "column "email" cannot be null"
   * OUTPUT: "Email is required and cannot be empty."
   */
  demonstrateErrorTransformation() {
    const technicalErrors = [
      'Could not execute statement [ERROR: duplicate key value violates unique constraint "ukrghs6qdg65k92vitg3aajsrpf" Detail: Key (ldap)=(tester) already exists.]',
      'foreign key constraint violation on user_id',
      'column "email" cannot be null',
      'connection timeout after 30 seconds',
      'java.lang.NullPointerException: Cannot invoke method',
      'Total time for 2024-01-15 would exceed 8 hours'
    ];

    technicalErrors.forEach(error => {
      const userFriendly = this.errorMessageService.transformErrorMessage(error);
      console.log(`Technical: ${error}`);
      console.log(`User-friendly: ${userFriendly}`);
      console.log('---');
    });
  }
}

/**
 * USAGE PATTERNS:
 * 
 * 1. For HTTP Error Responses:
 * ```typescript
 * error: (error) => {
 *   const errorMessage = error.error?.message || 'Default fallback message';
 *   this.notificationService.showError(errorMessage);
 * }
 * ```
 * 
 * 2. For Success Responses:
 * ```typescript
 * next: (response) => {
 *   if (response.status === 'success') {
 *     this.notificationService.showSuccess(response.message || 'Success!');
 *   } else {
 *     this.notificationService.showError(response.message || 'Failed');
 *   }
 * }
 * ```
 * 
 * 3. For Business Logic Warnings:
 * ```typescript
 * if (someCondition) {
 *   this.notificationService.showWarning('Please check your input');
 * }
 * ```
 * 
 * 4. For Information Messages:
 * ```typescript
 * this.notificationService.showInfo('Data loaded successfully');
 * ```
 */
