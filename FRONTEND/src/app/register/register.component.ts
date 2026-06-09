import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  template: `
    <div class="auth-page">
      <div class="auth-card" style="max-width:520px;">

        <!-- Acknowledgement -->
        <div *ngIf="registered" style="text-align:center;padding:12px 0;">
          <div style="font-size:56px;margin-bottom:16px;"></div>
          <h2 style="color:#1a1a1a;margin-bottom:16px;">Registration Successful!</h2>
          <div style="background:#f0f0ec;border:1px solid #ccc;border-radius:10px;padding:20px;text-align:left;margin-bottom:24px;">
            <div style="display:flex;flex-direction:column;gap:10px;font-size:14px;">
              <div><span style="color:#666;width:90px;display:inline-block;">User ID</span><strong>#{{ regUserId }}</strong></div>
              <div><span style="color:#666;width:90px;display:inline-block;">Name</span><strong>{{ regName }}</strong></div>
              <div><span style="color:#666;width:90px;display:inline-block;">Email</span><strong>{{ regEmail }}</strong></div>
            </div>
          </div>
          <button class="btn btn-primary" style="width:100%;" (click)="router.navigate(['/login'])">Login Now →</button>
        </div>

        <!-- Form -->
        <div *ngIf="!registered">
          <div class="auth-logo">
            <div class="icon"></div>
            <h1>Customer Registration</h1>
            <p>Create your account to get started</p>
          </div>

          <form (ngSubmit)="onSubmit()" novalidate>
            <div class="form-group">
              <label>Customer Name *</label>
              <input type="text" class="form-control" [(ngModel)]="u.name" name="name" (blur)="t('name')" [class.invalid]="touched.name && nameErr">
              <div class="msg msg-error" style="padding:6px 10px;margin-top:4px;" *ngIf="touched.name && nameErr">{{ nameErr }}</div>
            </div>
            <div class="form-group">
              <label>Email *</label>
              <input type="email" class="form-control" [(ngModel)]="u.email" name="email" (blur)="t('email')" [class.invalid]="touched.email && emailErr">
              <div class="msg msg-error" style="padding:6px 10px;margin-top:4px;" *ngIf="touched.email && emailErr">{{ emailErr }}</div>
            </div>
            <div class="form-group">
              <label>Mobile Number *</label>
              <div style="display:flex;gap:8px;">
                <select class="form-control" style="width:110px;flex-shrink:0;" [(ngModel)]="u.cc" name="cc">
                  <option value="+91">+91</option>
                  <option value="+1">+1</option>
                  <option value="+44">+44</option>
                  <option value="+61">+61</option>
                  <option value="+971">+971</option>
                  <option value="+65">+65</option>
                </select>
                <input type="text" class="form-control" [(ngModel)]="u.mobile" name="mobile" (blur)="t('mobile')" [class.invalid]="touched.mobile && mobileErr" placeholder="8–10 digits">
              </div>
              <div class="msg msg-error" style="padding:6px 10px;margin-top:4px;" *ngIf="touched.mobile && mobileErr">{{ mobileErr }}</div>
            </div>
            <div class="form-group">
              <label>Address *</label>
              <textarea class="form-control" [(ngModel)]="u.address" name="address" rows="2" (blur)="t('address')" [class.invalid]="touched.address && addressErr"></textarea>
              <div class="msg msg-error" style="padding:6px 10px;margin-top:4px;" *ngIf="touched.address && addressErr">{{ addressErr }}</div>
            </div>
            <div class="form-grid-2">
              <div class="form-group">
                <label>Username *</label>
                <input type="text" class="form-control" [(ngModel)]="u.username" name="username" (blur)="t('username')" [class.invalid]="touched.username && usernameErr">
                <div class="msg msg-error" style="padding:6px 10px;margin-top:4px;" *ngIf="touched.username && usernameErr">{{ usernameErr }}</div>
              </div>
              <div></div>
            </div>
            <div class="form-grid-2">
              <div class="form-group">
                <label>Password *</label>
                <input type="password" class="form-control" [(ngModel)]="u.password" name="password" (blur)="t('password')" [class.invalid]="touched.password && passwordErr">
                <div class="msg msg-error" style="padding:6px 10px;margin-top:4px;" *ngIf="touched.password && passwordErr">{{ passwordErr }}</div>
              </div>
              <div class="form-group">
                <label>Confirm Password *</label>
                <input type="password" class="form-control" [(ngModel)]="confirm" name="confirm" (blur)="t('confirm')" [class.invalid]="touched.confirm && confirmErr">
                <div class="msg msg-error" style="padding:6px 10px;margin-top:4px;" *ngIf="touched.confirm && confirmErr">{{ confirmErr }}</div>
              </div>
            </div>

            <div class="msg msg-error" *ngIf="serverError">{{ serverError }}</div>

            <div style="display:flex;gap:10px;margin-top:8px;">
              <button type="submit" class="btn btn-primary" style="flex:1;" [disabled]="loading || !isValid()">
                {{ loading ? 'Registering...' : 'Register' }}
              </button>
              <button type="button" class="btn btn-secondary" (click)="reset()">Reset</button>
            </div>
            <p style="text-align:center;margin-top:16px;font-size:13px;">Already have an account? <a routerLink="/login">Sign In</a></p>
          </form>
        </div>
      </div>
    </div>
  `
})
export class RegisterComponent {
  u = { name:'', email:'', cc:'+91', mobile:'', address:'', username:'', password:'' };
  confirm = ''; serverError = ''; loading = false;
  registered = false; regUserId = ''; regName = ''; regEmail = '';
  touched: any = {};

  constructor(public router: Router, private authService: AuthService) {}

  t(f: string) { this.touched[f] = true; }

  get nameErr()     { if (!this.u.name) return 'Name is required.'; if (this.u.name.length < 3 || !/^[a-zA-Z\s]+$/.test(this.u.name)) return 'Name must be at least 3 characters long and contain only letters.'; return ''; }
  get emailErr()    { if (!this.u.email) return 'Email is required.'; if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.u.email)) return 'Enter a valid email address.'; return ''; }
  get mobileErr()   { if (!this.u.mobile) return 'Mobile number is required.'; if (!/^\d{8,10}$/.test(this.u.mobile.trim())) return 'Enter a valid mobile number (8–10 digits).'; return ''; }
  get addressErr()  { if (!this.u.address) return 'Address is required.'; if (this.u.address.length < 10) return 'Address must be at least 10 characters long.'; return ''; }
  get usernameErr() { if (!this.u.username) return 'Username is required.'; if (this.u.username.length < 5 || /\s/.test(this.u.username)) return 'Username must be at least 5 characters and contain no spaces.'; return ''; }
  get passwordErr() { if (!this.u.password) return 'Password is required.'; if (this.u.password.length < 8 || !/(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[!@#$%^&*])/.test(this.u.password)) return 'Password must be at least 8 characters and include a mix of uppercase, lowercase, number, and special character.'; return ''; }
  get confirmErr()  { if (!this.confirm) return 'Please confirm your password.'; if (this.confirm !== this.u.password) return 'Passwords do not match.'; return ''; }

  isValid() { return !this.nameErr && !this.emailErr && !this.mobileErr && !this.addressErr && !this.usernameErr && !this.passwordErr && !this.confirmErr; }

  reset() { this.u = { name:'', email:'', cc:'+91', mobile:'', address:'', username:'', password:'' }; this.confirm = ''; this.serverError = ''; this.touched = {}; }

  onSubmit() {
    this.touched = { name:true, email:true, mobile:true, address:true, username:true, password:true, confirm:true };
    if (!this.isValid()) return;
    this.loading = true; this.serverError = '';
    this.authService.register({ username: this.u.username, password: this.u.password, email: this.u.email, name: this.u.name, mobileNo: this.u.cc + this.u.mobile.trim(), address: this.u.address }).subscribe({
      next: (r: any) => { this.loading = false; this.regUserId = r.userId || r.id || 'N/A'; this.regName = r.fullName || this.u.name; this.regEmail = r.email || this.u.email; this.registered = true; },
      error: (e: any) => { this.loading = false; const m = e.error?.error || ''; if (m.toLowerCase().includes('email')) this.serverError = 'Email already registered.'; else if (m.toLowerCase().includes('username')) this.serverError = 'Username already taken.'; else this.serverError = m || 'Registration failed. Please try again.'; }
    });
  }
}
