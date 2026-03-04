import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpHeaders } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { finalize, catchError, switchMap } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { LoaderService } from './shared/loader.service';
import { Router } from '@angular/router';
import {jwtDecode} from 'jwt-decode'
import { NotificationService } from './shared/notification.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private activeRequests = 0;

  constructor(
    private authService: AuthService,
    private loaderService: LoaderService,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = this.authService.getToken();

    // Increment the active requests counter
    this.activeRequests++;

    // Only show the loader if this is the first active request
    if (this.activeRequests === 1) {
      setTimeout(() => {
        this.loaderService.showLoader();
      }, 0);
    }

    // Skip token expiration check for logout requests to prevent recursion
    const isLogoutRequest = req.url.includes('/auth/logout');

    if (token && !isLogoutRequest && this.isTokenExpired(token)) {
      this.authService.logout();

      this.notificationService.showNotification({
        type: 'error',
        message: 'Session expired. Please login again.'
      });

      // Decrement the counter and hide loader if no more requests
      this.activeRequests--;
      if (this.activeRequests === 0) {
        setTimeout(() => {
          this.loaderService.hideLoader();
        }, 0);
      }

      this.router.navigate(['/login']);
      return of();
    }

    // Use simplified processing
    return this.processRequest(req, next, token);


  }

  private isTokenExpired(token: string): boolean {
    try {
      const decodedToken: any = jwtDecode(token);
      const expirationDate = new Date(0);
      expirationDate.setUTCSeconds(decodedToken.exp);
      return expirationDate < new Date();
    } catch (error) {
      console.error('Error decoding token:', error);
      return true;
    }
  }

  /**
   * Get CSRF token from cookies
   * @returns CSRF token or null if not found
   */
  private getCsrfToken(): string | null {
    const cookies = document.cookie.split(';');

    for (const cookie of cookies) {
      const trimmedCookie = cookie.trim();
      if (trimmedCookie) {
        const [name, value] = trimmedCookie.split('=');
        if (name === 'XSRF-TOKEN' && value) {
          return decodeURIComponent(value);
        }
      }
    }

    return null;
  }

  /**
   * Process the request with proper headers (simplified)
   */
  private processRequest(req: HttpRequest<any>, next: HttpHandler, token: string | null): Observable<HttpEvent<any>> {
    let modifiedRequest = req;

    if (token) {
      modifiedRequest = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }

    return next.handle(modifiedRequest).pipe(
      finalize(() => {
        // Decrement the counter
        this.activeRequests--;

        // Only hide the loader if there are no more active requests
        if (this.activeRequests === 0) {
          setTimeout(() => {
            this.loaderService.hideLoader();
          }, 0);
        }
      }),
      catchError((error) => {
        // Skip logout for logout requests to prevent recursion
        const isLogoutRequest = req.url.includes('/auth/logout');

        if (error.status === 401 && !isLogoutRequest) {
          this.authService.logout();

          this.notificationService.showNotification({
            type: 'error',
            message: 'Session expired. Please login again.'
          });
          this.router.navigate(['/login']);
        } else if (error.status === 403) {
          // Handle forbidden errors
          const role = localStorage.getItem('role');

          let message = 'You do not have permission to perform this action.';
          if (req.url.includes('/dropdown-configurations')) {
            message = `Access denied for dropdown management. Required role: ADMIN_OPS_MANAGER. Current role: ${role || 'None'}`;
          }

          this.notificationService.showNotification({
            type: 'error',
            message: message
          });
        }
        throw error;
      })
    );
  }
}
