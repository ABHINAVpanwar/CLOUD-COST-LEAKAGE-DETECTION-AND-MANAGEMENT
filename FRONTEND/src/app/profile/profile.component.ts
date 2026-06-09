import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../services/auth.service';
import { SidebarComponent } from '../shared/sidebar.component';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, SidebarComponent],
  template: `
    <div class="layout">
      <app-sidebar [role]="role"></app-sidebar>
      <main class="main">
        <div class="topbar"><h2>My Profile</h2></div>

        <div style="display:grid;grid-template-columns:1fr 1fr;gap:20px;max-width:800px;">
          <div class="card">
            <div class="card-header"><span class="card-title">Profile Details</span></div>
            <div *ngIf="!editing">
              <div *ngFor="let row of profileRows" style="display:flex;padding:12px 0;border-bottom:1px solid #edf2f7;">
                <span style="width:110px;color:#718096;font-size:13px;flex-shrink:0;">{{ row.label }}</span>
                <span *ngIf="row.key !== 'role'"><strong>{{ profile[row.key] || '—' }}</strong></span>
                <span *ngIf="row.key === 'role'"><span class="badge badge-purple">{{ profile.role }}</span></span>
              </div>
              <button class="btn btn-primary" style="margin-top:20px;" (click)="startEdit()">Edit Profile</button>
            </div>
            <div *ngIf="editing">
              <div class="form-group"><label>Full Name</label><input type="text" class="form-control" [(ngModel)]="ef.fullName" name="fn"></div>
              <div class="form-group"><label>Mobile Number</label><input type="text" class="form-control" [(ngModel)]="ef.mobileNo" name="mn"></div>
              <div class="form-group"><label>Address</label><textarea class="form-control" [(ngModel)]="ef.address" name="addr" rows="3"></textarea></div>
              <div class="msg msg-error" *ngIf="err">{{ err }}</div>
              <div class="msg msg-success" *ngIf="ok">{{ ok }}</div>
              <div style="display:flex;gap:10px;margin-top:8px;">
                <button class="btn btn-primary" (click)="save()" [disabled]="saving">{{ saving ? 'Saving...' : 'Save Changes' }}</button>
                <button class="btn btn-secondary" (click)="editing=false">Cancel</button>
              </div>
            </div>
          </div>

          <div class="card">
            <div class="card-header"><span class="card-title">Account Info</span></div>
            <div style="display:flex;flex-direction:column;gap:12px;">
              <div style="background:#f5f5f0;border-radius:8px;padding:16px;">
                <div style="font-size:12px;color:#888;margin-bottom:4px;">Account Status</div>
                <span class="badge badge-green">{{ profile.status || 'ACTIVE' }}</span>
              </div>
              <div style="background:#f5f5f0;border-radius:8px;padding:16px;">
                <div style="font-size:12px;color:#888;margin-bottom:4px;">Member Since</div>
                <strong style="font-size:14px;">{{ profile.createdAt ? (profile.createdAt | date:'mediumDate') : '—' }}</strong>
              </div>
              <div style="background:#f5f5f0;border-radius:8px;padding:16px;">
                <div style="font-size:12px;color:#888;margin-bottom:4px;">Last Login</div>
                <strong style="font-size:14px;">{{ profile.lastLogin ? (profile.lastLogin | date:'medium') : 'N/A' }}</strong>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  `
})
export class ProfileComponent implements OnInit {
  profile: any = {}; ef: any = {}; editing = false; saving = false; err = ''; ok = ''; role = 'CLIENT';
  profileRows = [
    { label: 'Full Name', key: 'fullName' }, { label: 'Username', key: 'username' },
    { label: 'Email', key: 'email' }, { label: 'Mobile', key: 'mobileNo' }, { label: 'Role', key: 'role' }
  ];
  constructor(private http: HttpClient, private auth: AuthService) {}
  ngOnInit(): void {
    this.role = this.auth.getUserRole();
    this.http.get('http://localhost:8080/api/user/profile').subscribe({
      next: (d: any) => this.profile = d,
      error: () => { this.profile = { username: this.auth.getUsername(), role: this.auth.getUserRole() }; }
    });
  }
  startEdit(): void { this.ef = { fullName: this.profile.fullName, mobileNo: this.profile.mobileNo, address: this.profile.address || '' }; this.editing = true; this.err = ''; this.ok = ''; }
  save(): void {
    this.saving = true; this.err = ''; this.ok = '';
    this.http.put('http://localhost:8080/api/user/profile', this.ef).subscribe({
      next: () => { this.saving = false; this.ok = 'Profile updated successfully.'; this.profile = { ...this.profile, ...this.ef }; this.editing = false; },
      error: () => { this.saving = false; this.err = 'Failed to update profile. Please try again.'; }
    });
  }
}
