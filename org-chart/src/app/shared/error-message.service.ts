import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ErrorMessageService {

  constructor() { }

  /**
   * Extract error message from HTTP error response
   * @param error The HTTP error response
   * @param fallbackMessage Default message if no specific error found
   * @returns Extracted error message
   */
  extractErrorMessage(error: any, fallbackMessage: string = 'An error occurred. Please try again.'): string {
    if (error?.error?.message) {
      return error.error.message;
    }
    if (error?.message) {
      return error.message;
    }
    return fallbackMessage;
  }

  /**
   * Transform technical error messages into user-friendly messages
   * @param errorMessage The raw error message from backend
   * @returns User-friendly error message
   */
  transformErrorMessage(errorMessage: string): string {
    if (!errorMessage) {
      return 'An unexpected error occurred. Please try again.';
    }

    // Convert to lowercase for easier matching
    const lowerMessage = errorMessage.toLowerCase();

    // Database constraint violations
    if (this.isDuplicateKeyError(lowerMessage)) {
      return this.handleDuplicateKeyError(errorMessage);
    }

    // Foreign key constraint violations
    if (this.isForeignKeyError(lowerMessage)) {
      return this.handleForeignKeyError(errorMessage);
    }

    // Not null constraint violations
    if (this.isNotNullError(lowerMessage)) {
      return this.handleNotNullError(errorMessage);
    }

    // Check constraint violations
    if (this.isCheckConstraintError(lowerMessage)) {
      return this.handleCheckConstraintError(errorMessage);
    }

    // Connection/timeout errors
    if (this.isConnectionError(lowerMessage)) {
      return 'Unable to connect to the server. Please check your internet connection and try again.';
    }

    // Authentication/authorization errors
    if (this.isAuthError(lowerMessage)) {
      return this.handleAuthError(errorMessage);
    }

    // Validation errors
    if (this.isValidationError(lowerMessage)) {
      return this.handleValidationError(errorMessage);
    }

    // File/upload errors
    if (this.isFileError(lowerMessage)) {
      return this.handleFileError(errorMessage);
    }

    // Business logic errors (keep these as they are usually user-friendly)
    if (this.isBusinessLogicError(lowerMessage)) {
      return errorMessage;
    }

    // If no specific pattern matches, try to extract meaningful parts
    return this.extractMeaningfulMessage(errorMessage);
  }

  private isDuplicateKeyError(message: string): boolean {
    return message.includes('duplicate key') ||
           message.includes('unique constraint') ||
           message.includes('already exists') ||
           message.includes('duplicate entry');
  }

  private handleDuplicateKeyError(message: string): string {
    // Extract field information from constraint name or message
    if (message.includes('ldap')) {
      return 'This LDAP ID is already registered. Please use a different LDAP ID.';
    }
    if (message.includes('email')) {
      return 'This email address is already registered. Please use a different email.';
    }
    if (message.includes('username')) {
      return 'This username is already taken. Please choose a different username.';
    }
    if (message.includes('project') && message.includes('code')) {
      return 'This project code already exists. Please use a different project code.';
    }
    if (message.includes('employee_id')) {
      return 'This employee ID is already assigned. Please use a different employee ID.';
    }

    return 'This record already exists. Please check your input and try again.';
  }

  private isForeignKeyError(message: string): boolean {
    return message.includes('foreign key') ||
           message.includes('violates foreign key constraint') ||
           message.includes('referenced record');
  }

  private handleForeignKeyError(message: string): string {
    if (message.includes('project')) {
      return 'The selected project is not valid or has been removed.';
    }
    if (message.includes('user') || message.includes('employee')) {
      return 'The selected user is not valid or has been removed.';
    }
    if (message.includes('lead')) {
      return 'The selected lead is not valid or has been removed.';
    }

    return 'The selected item is not valid. Please refresh the page and try again.';
  }

  private isNotNullError(message: string): boolean {
    return message.includes('not null') ||
           message.includes('cannot be null') ||
           message.includes('required field');
  }

  private handleNotNullError(message: string): string {
    // Extract field name if possible
    const fieldMatch = message.match(/column "([^"]+)"/);
    if (fieldMatch) {
      const fieldName = this.formatFieldName(fieldMatch[1]);
      return `${fieldName} is required and cannot be empty.`;
    }

    return 'Required fields are missing. Please fill in all required information.';
  }

  private isCheckConstraintError(message: string): boolean {
    return message.includes('check constraint') ||
           message.includes('violates check');
  }

  private handleCheckConstraintError(message: string): string {
    if (message.includes('date')) {
      return 'Invalid date range. Please check your date inputs.';
    }
    if (message.includes('time')) {
      return 'Invalid time value. Please enter a valid time.';
    }
    if (message.includes('status')) {
      return 'Invalid status value. Please select a valid status.';
    }

    return 'The entered data does not meet the required criteria. Please check your inputs.';
  }

  private isConnectionError(message: string): boolean {
    return message.includes('connection') ||
           message.includes('timeout') ||
           message.includes('network') ||
           message.includes('unreachable') ||
           message.includes('server is under maintenance') ||
           message.includes('maintenance') ||
           message.includes('service unavailable') ||
           message.includes('temporarily unavailable');
  }

  /**
   * Check if the error indicates server maintenance or unavailability
   * @param message The error message to check
   * @returns true if it's a maintenance/server error
   */
  isMaintenanceError(message: string): boolean {
    const lowerMessage = message.toLowerCase();
    return lowerMessage.includes('server is under maintenance') ||
           lowerMessage.includes('maintenance') ||
           lowerMessage.includes('service unavailable') ||
           lowerMessage.includes('temporarily unavailable') ||
           lowerMessage.includes('server error') ||
           lowerMessage.includes('internal server error');
  }

  /**
   * Check if the error is a network connectivity issue
   * @param status HTTP status code
   * @param message Error message
   * @returns true if it's a network error
   */
  isNetworkError(status: number, message?: string): boolean {
    // Status 0 typically indicates network connectivity issues
    if (status === 0) {
      return true;
    }

    // 5xx errors are server-side issues
    if (status >= 500) {
      return true;
    }

    // Check message content for network-related terms
    if (message) {
      const lowerMessage = message.toLowerCase();
      return lowerMessage.includes('network') ||
             lowerMessage.includes('connection') ||
             lowerMessage.includes('timeout') ||
             lowerMessage.includes('unreachable') ||
             lowerMessage.includes('failed to fetch');
    }

    return false;
  }

  private isAuthError(message: string): boolean {
    return message.includes('unauthorized') ||
           message.includes('forbidden') ||
           message.includes('access denied') ||
           message.includes('permission');
  }

  private handleAuthError(message: string): string {
    if (message.includes('session') || message.includes('expired')) {
      return 'Your session has expired. Please log in again.';
    }
    if (message.includes('permission') || message.includes('forbidden')) {
      return 'You do not have permission to perform this action.';
    }

    return 'Authentication failed. Please log in again.';
  }

  private isValidationError(message: string): boolean {
    return message.includes('validation') ||
           message.includes('invalid format') ||
           message.includes('must be') ||
           message.includes('should be');
  }

  private handleValidationError(message: string): string {
    // Keep validation messages as they are usually user-friendly
    return message;
  }

  private isFileError(message: string): boolean {
    return message.includes('file') ||
           message.includes('upload') ||
           message.includes('size limit') ||
           message.includes('format not supported');
  }

  private handleFileError(message: string): string {
    if (message.includes('size')) {
      return 'File size is too large. Please choose a smaller file.';
    }
    if (message.includes('format') || message.includes('type')) {
      return 'File format is not supported. Please choose a different file type.';
    }

    return 'File upload failed. Please try again with a different file.';
  }

  private isBusinessLogicError(message: string): boolean {
    // These are usually already user-friendly messages from business logic
    return message.includes('cannot') ||
           message.includes('already has') ||
           message.includes('exceeds') ||
           message.includes('insufficient') ||
           message.includes('not allowed') ||
           (message.includes('total time') && message.includes('hours'));
  }

  private extractMeaningfulMessage(message: string): string {
    // Remove SQL-specific technical details
    let cleanMessage = message
      .replace(/\[.*?\]/g, '') // Remove [bracketed] content
      .replace(/SQL \[.*?\]/g, '') // Remove SQL statements
      .replace(/constraint \[.*?\]/g, '') // Remove constraint names
      .replace(/Detail: Key \(.*?\)/g, '') // Remove key details
      .replace(/could not execute statement/gi, '') // Remove execution errors
      .replace(/ERROR:/gi, '') // Remove ERROR prefix
      .trim();

    // If the cleaned message is too short or still technical, provide a generic message
    if (cleanMessage.length < 10 || this.isTechnicalMessage(cleanMessage)) {
      return 'An error occurred while processing your request. Please try again.';
    }

    // Capitalize first letter and ensure proper punctuation
    cleanMessage = cleanMessage.charAt(0).toUpperCase() + cleanMessage.slice(1);
    if (!cleanMessage.endsWith('.') && !cleanMessage.endsWith('!') && !cleanMessage.endsWith('?')) {
      cleanMessage += '.';
    }

    return cleanMessage;
  }

  private isTechnicalMessage(message: string): boolean {
    const technicalTerms = [
      'null pointer', 'exception', 'stack trace', 'java.', 'org.springframework',
      'hibernate', 'jdbc', 'sql', 'database', 'connection pool', 'transaction'
    ];

    const lowerMessage = message.toLowerCase();
    return technicalTerms.some(term => lowerMessage.includes(term));
  }

  private formatFieldName(fieldName: string): string {
    // Convert snake_case to Title Case
    return fieldName
      .split('_')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }
}
