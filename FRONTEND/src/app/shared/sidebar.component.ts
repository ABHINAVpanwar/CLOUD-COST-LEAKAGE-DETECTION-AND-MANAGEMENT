import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <aside class="sidebar">
      <div class="sidebar-logo">
        <div class="brand">CloudOpt</div>
        <div class="role-tag">{{ roleLabel }}</div>
      </div>

      <!-- Client nav -->
      <ng-container *ngIf="role === 'CLIENT'">
        <span class="nav-section-label">Main</span>
        <a routerLink="/dashboard" routerLinkActive="active" class="nav-item">Dashboard</a>
        <a routerLink="/analytics" routerLinkActive="active" class="nav-item">Usage Analytics</a>
        <a routerLink="/leakages"  routerLinkActive="active" class="nav-item">Leakage Reports</a>
        <a routerLink="/upload"    routerLinkActive="active" class="nav-item">Upload Data</a>
        <span class="nav-section-label">Tools</span>
        <a routerLink="/budget"    routerLinkActive="active" class="nav-item">Budget & Alerts</a>
        <a routerLink="/history"   routerLinkActive="active" class="nav-item">History</a>
        <a routerLink="/support"   routerLinkActive="active" class="nav-item">Support</a>
      </ng-container>

      <!-- Engineer nav -->
      <ng-container *ngIf="role === 'ENGINEER'">
        <span class="nav-section-label">Engineer</span>
        <a routerLink="/engineer" routerLinkActive="active" class="nav-item">Client Analysis</a>
      </ng-container>

      <!-- Admin nav -->
      <ng-container *ngIf="role === 'ADMIN'">
        <span class="nav-section-label">Admin</span>
        <a routerLink="/admin"     routerLinkActive="active" class="nav-item">Admin Panel</a>
        <span class="nav-section-label">Client View</span>
        <a routerLink="/dashboard" routerLinkActive="active" class="nav-item">Dashboard</a>
        <a routerLink="/analytics" routerLinkActive="active" class="nav-item">Analytics</a>
        <a routerLink="/leakages"  routerLinkActive="active" class="nav-item">Leakages</a>
        <a routerLink="/upload"    routerLinkActive="active" class="nav-item">Upload Data</a>
        <a routerLink="/budget"    routerLinkActive="active" class="nav-item">Budget</a>
      </ng-container>

      <!-- Account -->
      <span class="nav-section-label" style="margin-top:auto;">Account</span>
      <a routerLink="/profile" routerLinkActive="active" class="nav-item">Profile</a>
      <button class="nav-item logout" (click)="logout()">Logout</button>
    </aside>
  `
})
export class SidebarComponent {
  @Input() role: string = 'CLIENT';

  get roleLabel(): string {
    if (this.role === 'ADMIN') return 'Administrator';
    if (this.role === 'ENGINEER') return 'Cloud Engineer';
    return 'Client Portal';
  }

  constructor(private authService: AuthService, private router: Router) {}

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login'], { state: { loggedOut: true } });
  }
}
