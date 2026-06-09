import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({ providedIn: 'root' })
export class AdminGuard implements CanActivate {
  constructor(private authService: AuthService, private router: Router) {}

  canActivate(): boolean {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login']);
      return false;
    }
    if (this.authService.isAdmin()) {
      return true;
    }
    // Authenticated but not admin — redirect to their own dashboard
    if (this.authService.isEngineer()) this.router.navigate(['/engineer']);
    else this.router.navigate(['/dashboard']);
    return false;
  }
}
