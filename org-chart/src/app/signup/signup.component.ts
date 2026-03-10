import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from 'src/environments/environment';
import { Router } from '@angular/router';

@Component({
  selector: 'app-signup',
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.css'],
})
export class SignupComponent {
  username: string = '';
  password: string = '';
  role: string = '';
  confirmPassword: string = '';
  passwordPattern = '^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$';


  constructor(private http: HttpClient, private router: Router) {}

  onSignup() {
    if (this.password !== this.confirmPassword) {
      alert('Passwords do not match!');
      return;
    }

    // Validate password requirements
    if (!this.password.match(this.passwordPattern)) {
      alert('Password must contain at least one lowercase letter, one uppercase letter, one digit, and one special character, and be at least 8 characters long.');
      return;
    }

    const apiUrl = `${environment.apiUrl}/admin/register?username=${this.username}&password=${this.password}&role=${this.role}`;

    this.http.post<any>(apiUrl, {}).subscribe(
      (response) => {
        if (response.success) {
          alert(response.message);
          this.router.navigate(['/login']);
        } else {
          alert(response.message || 'Signup failed. Please try again.');
        }
      },
      (error) => {
        console.error('Signup error:', error);
        if (error.error && error.error.message) {
          alert(error.error.message);
        } else if (error.status === 409) {
          alert('Username already exists. Please choose a different username.');
        } else if (error.status === 400) {
          alert('Invalid input. Please check your information and try again.');
        } else {
          alert('Signup failed. Please try again later.');
        }
      }
    );
  }
}
