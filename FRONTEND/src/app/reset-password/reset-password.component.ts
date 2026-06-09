import { Component, OnInit } from '@angular/core';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  template: `
    <div class="auth-page">
      <div class="auth-card">
        <div class="auth-logo">
          <div class="icon"></div>
          <h1>Reset Password</h1>
          <p>Enter your new password below</p>
        </div>

        <div class="msg msg-error" *ngIf="!token">Invalid or missing reset token. Please request a new reset link.</div>

        <form (ngSubmit)="onSubmit()" *ngIf="token && !done">
          <div class="form-group">
            <label>New Password</label>
            <input type="password" class="form-control" [(ngModel)]="password" name="password" required
              placeholder="Min 8 chars, upper, lower, number, special">
          </div>
          <div class="form-group">
            <label>Confirm Password</label>
            <input type="password" class="form-control" [(ngModel)]="confirm" name="confirm" required>
          </div>
          <div class="msg msg-error" *ngIf="error">{{ error }}</div>
          <button type="submit" class="btn btn-primary" style="width:100%;margin-top:8px;" [disabled]="loading || !password || !confirm">
            {{ loading ? 'Resetting...' : 'Reset Password' }}
          </button>
        </form>

        <div class="msg msg-success" *ngIf="done">Password reset successfully. You can now log in.</div>
        <div style="text-align:center;margin-top:20px;font-size:13px;"><a routerLink="/login">Back to Login</a></div>
      </div>
    </div>
  `
})
export class ResetPasswordComponent implements OnInit {
  token = ''; password = ''; confirm = ''; loading = false; error = ''; done = false;

  constructor(private http: HttpClient, private route: ActivatedRoute) {}

  ngOnInit() {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';
  }

  onSubmit() {
    this.error = '';
    if (this.password !== this.confirm) { this.error = 'Passwords do not match.'; return; }
    if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,}$/.test(this.password)) {
      this.error = 'Password must be at least 8 characters and include uppercase, lowercase, number, and special character.';
      return;
    }
    this.loading = true;
    this.http.post('http://localhost:8080/api/auth/reset-password', { token: this.token, newPassword: this.password }).subscribe({
      next: () => { this.loading = false; this.done = true; },
      error: (e: any) => { this.loading = false; this.error = e.error?.error || 'Reset failed. The link may have expired.'; }
    });
  }
}
