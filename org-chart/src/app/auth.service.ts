import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable } from 'rxjs';
import { environment } from '../environments/environment';
import { LoginResponse } from './model/user';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private apiUrl = environment.apiUrl;
  private tokenKey = 'authToken'; // Key for storing JWT token in localStorage
  private roleSubject = new BehaviorSubject<string | null>(null);
  role$ = this.roleSubject.asObservable(); // Observable for role changes
  private isLoggingOut = false; // Flag to prevent multiple simultaneous logout calls

  constructor(private http: HttpClient, private router: Router) {
    const storedRole = localStorage.getItem('role');
    if (storedRole) {
      this.roleSubject.next(storedRole);
    }
  }

  login(username: string, password: string) {
    const apiUrl = `${this.apiUrl}/auth/login`;
    return this.http.post<LoginResponse>(apiUrl, { username, password });
  }

  storeToken(token: string, role: string, username: string) {
    localStorage.setItem(this.tokenKey, token);
    localStorage.setItem("role", role);
    localStorage.setItem("username", username);
    this.roleSubject.next(role);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  logout() {
    // Prevent multiple simultaneous logout calls
    if (this.isLoggingOut) {
      return;
    }

    this.isLoggingOut = true;

    // Clear local storage and navigate to login immediately to prevent recursive calls
    const token = this.getToken();

    // Clear local storage first
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem("role");
    localStorage.removeItem("username");
    this.roleSubject.next(null);

    // Navigate to login
    this.router.navigate(['/login']).then(() => {
      // Reset the flag after navigation is complete
      this.isLoggingOut = false;
    });

    // Try to invalidate token on server (but don't wait for it or handle errors that could cause recursion)
    if (token) {
      // Use a separate HTTP request that bypasses the auth interceptor
      this.invalidateTokenOnServer(token);
    }
  }

  private invalidateTokenOnServer(token: string) {
    // Create a request that bypasses the auth interceptor by using a different approach
    const headers = { 'Authorization': `Bearer ${token}` };

    this.http.post(`${this.apiUrl}/auth/logout`, {}, { headers }).subscribe({
      next: () => {
        console.log('Token invalidated on server');
      },
      error: (err) => {
        console.error('Error invalidating token:', err);
        // Don't call logout again here to avoid recursion
      }
    });
  }

  resetPassword(newPassword: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/reset-password`, { newPassword });
  }

  checkPasswordStatus(): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/check-password-status`, {});
  }

  // Forgot password - request OTP
  forgotPassword(username: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/forgot-password`, { username });
  }

  // Verify OTP
  verifyOtp(username: string, otpCode: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/verify-otp`, { username, otpCode });
  }

  // Reset password with OTP
  resetPasswordWithOtp(username: string, newPassword: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/reset-password-with-otp`, { username, newPassword });
  }
}
