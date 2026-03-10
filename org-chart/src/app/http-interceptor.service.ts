import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { NotificationService } from './shared/notification.service';
import { ErrorMessageService } from './shared/error-message.service';

@Injectable()
export class HttpInterceptorService implements HttpInterceptor {

  constructor(
    private router: Router,
    private notificationService: NotificationService,
    private errorMessageService: ErrorMessageService
  ) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Get the auth token from localStorage
    const token = localStorage.getItem('authToken');

    // Get CSRF token from cookies if available
    const csrfToken = this.getCsrfToken();

    // Clone the request and add the authorization header if token exists
    if (token) {
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }

    // Add CSRF token if available and the request is not GET or HEAD
    if (csrfToken && !['GET', 'HEAD', 'OPTIONS'].includes(request.method)) {
      request = request.clone({
        setHeaders: {
          'X-XSRF-TOKEN': csrfToken
        }
      });
    }

    // Pass the cloned request to the next handler
    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        // Skip logout handling for logout requests to prevent recursion
        const isLogoutRequest = error.url?.includes('/auth/logout');

        if (error.status === 401 && !isLogoutRequest) {
          // Handle unauthorized errors (e.g., token expired)
          localStorage.removeItem('authToken');
          localStorage.removeItem('role');
          localStorage.removeItem('username');
          this.router.navigate(['/login']);
          this.notificationService.showError('Your session has expired. Please log in again.');
        } else if (error.status === 403) {
          // Handle forbidden errors
          this.notificationService.showError('You do not have permission to perform this action.');
        } else if (error.status >= 500) {
          // Handle server errors - don't show notification for login requests as they handle it themselves
          if (!error.url?.includes('/auth/login')) {
            let errorMessage = 'Server is under maintenance. Please try again later.';
            if (error.error && error.error.message) {
              errorMessage = error.error.message;
            }
            this.notificationService.showError(errorMessage);
          }
        } else if (error.status === 0) {
          // Handle network errors - don't show notification for login requests as they handle it themselves
          if (!error.url?.includes('/auth/login')) {
            this.notificationService.showError('Server is under maintenance. Please check your connection and try again.');
          }
        }
        return throwError(() => error);
      })
    );
  }

  /**
   * Get CSRF token from cookies
   * @returns CSRF token or null if not found
   */
  private getCsrfToken(): string | null {
    const cookies = document.cookie.split(';');
    for (const cookie of cookies) {
      const [name, value] = cookie.trim().split('=');
      if (name === 'XSRF-TOKEN') {
        return decodeURIComponent(value);
      }
    }
    return null;
  }
}
