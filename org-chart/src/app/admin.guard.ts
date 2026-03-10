import { Injectable } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from './auth.service';
import { Observable, of } from 'rxjs';
import { NotificationService } from './shared/notification.service';
import { jwtDecode } from 'jwt-decode';

@Injectable({
  providedIn: 'root',
})
export class AdminGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> {
    // First check if user is logged in
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login']);
      return of(false);
    }

    // Get the token and verify it
    const token = this.authService.getToken();
    if (!token) {
      this.router.navigate(['/login']);
      return of(false);
    }

    try {
      // Decode the JWT token to get the role claim
      const decodedToken: any = jwtDecode(token);
      const tokenRole = decodedToken.role;

      // Get the stored role from localStorage
      const storedRole = localStorage.getItem('role');

      // Check if the role in localStorage matches the role in the token
      if (tokenRole !== storedRole) {
        console.error('Role mismatch detected! Token role:', tokenRole, 'Stored role:', storedRole);
        // Role mismatch detected, force logout
        this.authService.logout();
        this.notificationService.showNotification({
          type: 'error',
          message: 'Security violation detected. Please log in again.'
        });
        return of(false);
      }

      // Get the required roles from the route data
      const requiredRoles = route.data['roles'] as Array<string>;

      // If no role requirements, deny access
      if (!requiredRoles) {
        this.notificationService.showNotification({
          type: 'error',
          message: 'You do not have permission to access this page'
        });
        this.router.navigate(['/time-entry']);
        return of(false);
      }

      // Check if the user's role from the token is in the required roles list
      if (requiredRoles.includes(tokenRole)) {
        return of(true);
      } else {
        this.notificationService.showNotification({
          type: 'error',
          message: 'You do not have permission to access this page'
        });
        this.router.navigate(['/time-entry']);
        return of(false);
      }
    } catch (error) {
      console.error('Error verifying token in guard:', error);
      // Token is invalid or tampered with, force logout
      this.authService.logout();
      this.notificationService.showNotification({
        type: 'error',
        message: 'Invalid session. Please log in again.'
      });
      return of(false);
    }
  }
}
