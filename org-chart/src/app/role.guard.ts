import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { map, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class RoleGuard implements CanActivate {
  constructor(private authService: AuthService, private router: Router) {}

  canActivate(): Observable<boolean> {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login']);
      return new Observable(subscriber => subscriber.next(false));
    }

    return this.authService.checkPasswordStatus().pipe(
      map(response => {
        if (response.passwordChangeRequired) {
          this.router.navigate(['/password-reset']);
          return false;
        }
        return true;
      })
    );
  }
}
