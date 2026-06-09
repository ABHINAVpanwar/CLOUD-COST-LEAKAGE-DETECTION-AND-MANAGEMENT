import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  template: `
    <div class="auth-page">
      <div class="auth-card">
        <div class="auth-logo">
          <div class="icon"></div>
          <h1>Cloud Cost Optimizer</h1>
          <p>Sign in to your account</p>
        </div>

        <div class="msg msg-success" *ngIf="loggedOut">You have been logged out successfully.</div>

        <form (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label>Username</label>
            <input type="text" class="form-control" [(ngModel)]="username" name="username" placeholder="Enter username" required>
          </div>
          <div class="form-group">
            <label>Password</label>
            <input type="password" class="form-control" [(ngModel)]="password" name="password" placeholder="Enter password" required>
          </div>
          <div class="msg msg-error" *ngIf="errorMessage">{{ errorMessage }}</div>
          <button type="submit" class="btn btn-primary" style="width:100%;margin-top:8px;" [disabled]="loading">
            {{ loading ? 'Signing in...' : 'Sign In' }}
          </button>
        </form>

        <div style="display:flex;justify-content:space-between;margin-top:20px;font-size:13px;">
          <a routerLink="/forgot-password">Forgot Password?</a>
          <a routerLink="/register">Create Account</a>
        </div>
      </div>
    </div>
  `
})
export class LoginComponent {
  username = '';
  password = '';
  errorMessage = '';
  loading = false;
  loggedOut = false;

  constructor(private authService: AuthService, private router: Router) {
    this.loggedOut = this.router.getCurrentNavigation()?.extras?.state?.['loggedOut'] === true;
  }

  onSubmit(): void {
    if (!this.username || !this.password) { this.errorMessage = 'Please enter username and password.'; return; }
    this.loading = true;
    this.errorMessage = '';
    this.authService.login(this.username, this.password).subscribe({
      next: (response: any) => {
        this.loading = false;
        const rawRole: string = response.role || '';
        const role = rawRole.startsWith('ROLE_') ? rawRole.substring(5) : rawRole;
        if (role === 'ADMIN') this.router.navigate(['/admin']);
        else if (role === 'ENGINEER') this.router.navigate(['/engineer']);
        else this.router.navigate(['/dashboard']);
      },
      error: (err: any) => {
        this.loading = false;
        const msg = err.error?.error || '';
        if (msg.toLowerCase().includes('locked until')) {
          const lockedUntil = new Date(msg.replace('Account is locked until ', '').trim());
          const mins = Math.ceil((lockedUntil.getTime() - Date.now()) / 60000);
          this.errorMessage = `Your account is locked due to too many failed attempts. Try again in ${mins} minute${mins !== 1 ? 's' : ''}.`;
        } else {
          this.errorMessage = 'Invalid username or password.';
        }
      }
    });
  }
}
