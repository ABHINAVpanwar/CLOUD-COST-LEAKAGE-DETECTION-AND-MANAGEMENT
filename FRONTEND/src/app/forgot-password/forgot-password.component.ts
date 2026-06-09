import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  template: `
    <div class="auth-page">
      <div class="auth-card">
        <div class="auth-logo">
          <div class="icon"></div>
          <h1>Forgot Password</h1>
          <p>Enter your registered email to get a reset token</p>
        </div>

        <form (ngSubmit)="onSubmit()" *ngIf="!token">
          <div class="form-group">
            <label>Email Address</label>
            <input type="email" class="form-control" [(ngModel)]="email" name="email" required placeholder="example@gmail.com">
          </div>
          <div class="msg msg-error" *ngIf="error">{{ error }}</div>
          <button type="submit" class="btn btn-primary" style="width:100%;margin-top:8px;" [disabled]="loading || !email">
            {{ loading ? 'Generating...' : 'Get Reset Token' }}
          </button>
        </form>

        <div *ngIf="token">
          <div class="msg msg-success">Token generated. Copy it and use it on the reset page.</div>
          <div style="background:#f0f0ec;border:1px solid #ccc;border-radius:8px;padding:16px;margin-top:12px;">
            <div style="font-size:11px;color:#666;margin-bottom:6px;text-transform:uppercase;letter-spacing:0.5px;">Your Reset Token (valid 30 min)</div>
            <code style="font-size:13px;word-break:break-all;color:#1a1a1a;">{{ token }}</code>
          </div>
          <a routerLink="/reset-password" [queryParams]="{token: token}" class="btn btn-primary" style="width:100%;margin-top:16px;display:flex;justify-content:center;">
            Proceed to Reset Password
          </a>
        </div>

        <div style="text-align:center;margin-top:20px;font-size:13px;"><a routerLink="/login">Back to Login</a></div>
      </div>
    </div>
  `
})
export class ForgotPasswordComponent {
  email = ''; loading = false; token = ''; error = '';
  constructor(private http: HttpClient) {}
  onSubmit() {
    this.loading = true; this.error = '';
    this.http.post<any>('http://localhost:8080/api/auth/forgot-password', { email: this.email }).subscribe({
      next: (r) => { this.loading = false; if (r.token) { this.token = r.token; } else { this.error = 'No account found with that email.'; } },
      error: () => { this.loading = false; this.error = 'Something went wrong. Please try again.'; }
    });
  }
}
