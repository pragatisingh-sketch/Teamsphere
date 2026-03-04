import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { ReleaseService } from '../services/release.service';

@Injectable({
    providedIn: 'root'
})
export class ReleaseGuard implements CanActivate {

    constructor(
        private releaseService: ReleaseService,
        private router: Router
    ) { }

    canActivate(): boolean {
        if (this.releaseService.isAuthorizedUser()) {
            return true;
        }

        this.router.navigate(['/dashboard']);
        return false;
    }
}
