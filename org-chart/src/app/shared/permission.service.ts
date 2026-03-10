import { Injectable } from '@angular/core';
import { AuthService } from '../auth.service';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { jwtDecode } from 'jwt-decode';

// Define valid role types
export type UserRole = 'ADMIN_OPS_MANAGER' | 'ACCOUNT_MANAGER' | 'MANAGER' | 'LEAD' | 'USER';

/**
 * Service for handling role-based permissions in the application
 * This centralizes permission checks and makes them more secure
 */
@Injectable({
  providedIn: 'root'
})
export class PermissionService {

  // Define role hierarchy with proper typing
  private readonly ROLE_HIERARCHY: Record<UserRole, UserRole[]> = {
    'ADMIN_OPS_MANAGER': ['ADMIN_OPS_MANAGER', 'ACCOUNT_MANAGER', 'MANAGER', 'LEAD', 'USER'],
    'ACCOUNT_MANAGER': ['ACCOUNT_MANAGER', 'MANAGER', 'LEAD', 'USER'],
    'MANAGER': ['MANAGER', 'LEAD', 'USER'],
    'LEAD': ['LEAD', 'USER'],
    'USER': ['USER']
  };

  constructor(private authService: AuthService) { }

  /**
   * Get the user's role from the JWT token
   * This is more secure than reading from localStorage
   * @returns The role from the token, or null if invalid
   */
  private getRoleFromToken(): UserRole | null {
    try {
      const token = this.authService.getToken();
      if (!token) return null;

      const decodedToken: any = jwtDecode(token);
      const tokenRole = decodedToken.role;

      // Verify the role is valid
      if (this.isValidRole(tokenRole)) {
        return tokenRole;
      }
      return null;
    } catch (error) {
      console.error('Error decoding token in permission service:', error);
      return null;
    }
  }

  /**
   * Verify that the role in localStorage matches the role in the JWT token
   * @returns True if the roles match, false otherwise
   */
  private verifyRoleConsistency(): boolean {
    const tokenRole = this.getRoleFromToken();
    const storedRole = localStorage.getItem('role');

    if (!tokenRole || !storedRole) return false;

    // Check if the roles match
    return tokenRole === storedRole;
  }

  /**
   * Check if the current user has a specific role
   * @param role The role to check
   * @returns Observable<boolean> True if the user has the role
   */
  hasRole(role: UserRole): Observable<boolean> {
    // First verify role consistency
    if (!this.verifyRoleConsistency()) {
      console.error('Role mismatch detected in permission service');
      // Force logout on role mismatch
      this.authService.logout();
      return of(false);
    }

    // Get role from token for secure verification
    const tokenRole = this.getRoleFromToken();
    if (!tokenRole) return of(false);

    return of(tokenRole === role);
  }

  /**
   * Check if the current user has any of the specified roles
   * @param roles Array of roles to check
   * @returns Observable<boolean> True if the user has any of the roles
   */
  hasAnyRole(roles: UserRole[]): Observable<boolean> {
    // First verify role consistency
    if (!this.verifyRoleConsistency()) {
      console.error('Role mismatch detected in permission service');
      // Force logout on role mismatch
      this.authService.logout();
      return of(false);
    }

    // Get role from token for secure verification
    const tokenRole = this.getRoleFromToken();
    if (!tokenRole) return of(false);

    return of(roles.includes(tokenRole));
  }

  /**
   * Check if the current user has permission to perform an action
   * This uses the role hierarchy to determine permissions
   * @param requiredRole The minimum role required for the action
   * @returns Observable<boolean> True if the user has permission
   */
  hasPermission(requiredRole: UserRole): Observable<boolean> {
    // First verify role consistency
    if (!this.verifyRoleConsistency()) {
      console.error('Role mismatch detected in permission service');
      // Force logout on role mismatch
      this.authService.logout();
      return of(false);
    }

    // Get role from token for secure verification
    const tokenRole = this.getRoleFromToken();

    // Check if role is valid
    if (!tokenRole || !this.isValidRole(tokenRole)) {
      return of(false);
    }

    // Check if the current role has permission for the required role
    return of(this.ROLE_HIERARCHY[tokenRole].includes(requiredRole));
  }

  /**
   * Check if a role is a valid UserRole
   * @param role The role to check
   * @returns boolean True if the role is valid
   */
  private isValidRole(role: string): role is UserRole {
    return role === 'ADMIN_OPS_MANAGER' ||
           role === 'ACCOUNT_MANAGER' ||
           role === 'MANAGER' ||
           role === 'LEAD' ||
           role === 'USER';
  }

  /**
   * Check if the current user can edit user roles
   * Only ADMIN_OPS_MANAGER can edit roles
   * @returns Observable<boolean>
   */
  canEditRoles(): Observable<boolean> {
    return this.hasRole('ADMIN_OPS_MANAGER');
  }

  /**
   * Check if the current user can reset passwords
   * ADMIN_OPS_MANAGER, LEAD, and MANAGER can reset passwords
   * @returns Observable<boolean>
   */
  canResetPasswords(): Observable<boolean> {
    return this.hasAnyRole(['ADMIN_OPS_MANAGER', 'LEAD', 'MANAGER'] as UserRole[]);
  }

  /**
   * Check if the current user can edit dropdown fields
   * Only ADMIN_OPS_MANAGER can edit dropdown fields
   * @returns Observable<boolean>
   */
  canEditDropdowns(): Observable<boolean> {
    return this.hasRole('ADMIN_OPS_MANAGER');
  }

  /**
   * Check if the current user can manage projects
   * Only ADMIN_OPS_MANAGER can manage projects
   * @returns Observable<boolean>
   */
  canManageProjects(): Observable<boolean> {
    return this.hasRole('ADMIN_OPS_MANAGER');
  }
}
