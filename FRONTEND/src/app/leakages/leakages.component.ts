import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DashboardService } from '../services/dashboard.service';
import { AuthService } from '../services/auth.service';
import { SidebarComponent } from '../shared/sidebar.component';

@Component({
  selector: 'app-leakages',
  standalone: true,
  imports: [CommonModule, RouterLink, SidebarComponent],
  template: `
    <div class="layout">
      <app-sidebar [role]="role"></app-sidebar>
      <main class="main">
        <div class="topbar"><h2>Cost Leakage Report</h2></div>

        <div *ngIf="loading" class="loading">Loading leakage report...</div>

        <div *ngIf="!loading && error" class="no-data">
          <div class="icon">⚠️</div>{{ error }}
        </div>

        <div *ngIf="!loading && !error && leakages.length === 0" class="no-data">
          <div class="icon">✅</div>
          No idle or underutilized resources found. Your cloud is well optimized!
          <div style="margin-top:12px;font-size:12px;color:#aaa;">Upload cloud usage data to see leakage analysis.</div>
        </div>

        <div *ngIf="!loading && !error && leakages.length > 0">
          <div class="stats-grid" style="grid-template-columns:repeat(3,1fr);">
            <div class="stat-card red"><div class="stat-value">{{ leakages.length }}</div><div class="stat-label">Leakages Detected</div></div>
            <div class="stat-card orange"><div class="stat-value">₹{{ totalWaste | number:'1.0-0' }}</div><div class="stat-label">Total Estimated Waste</div></div>
            <div class="stat-card green"><div class="stat-value">₹{{ totalWaste | number:'1.0-0' }}</div><div class="stat-label">Potential Savings</div></div>
          </div>
          <div class="card">
            <div class="table-wrap">
              <table class="table">
                <thead><tr><th>Resource</th><th>Type</th><th>Provider</th><th>CPU %</th><th>Uptime %</th><th>Monthly Cost</th><th>Issue</th><th>Est. Waste</th><th>Action</th></tr></thead>
                <tbody>
                  <tr *ngFor="let l of leakages">
                    <td><strong>{{ l.resourceName }}</strong></td>
                    <td><span class="badge badge-blue">{{ l.resourceType }}</span></td>
                    <td>{{ l.provider }}</td>
                    <td class="text-red">{{ l.cpuUsage }}%</td>
                    <td [class]="l.uptime < 50 ? 'text-red' : ''">{{ l.uptime }}%</td>
                    <td>₹{{ l.cost | number:'1.0-0' }}</td>
                    <td><span class="badge badge-red">{{ l.issue }}</span></td>
                    <td class="text-red"><strong>₹{{ l.estimatedWastedCost | number:'1.0-0' }}</strong></td>
                    <td style="font-size:12px;">{{ l.action }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </main>
    </div>
  `
})
export class LeakagesComponent implements OnInit {
  leakages: any[] = []; totalWaste = 0; role = 'CLIENT'; loading = true; error = '';
  constructor(private svc: DashboardService, private auth: AuthService) {}
  ngOnInit(): void {
    this.role = this.auth.getUserRole();
    this.svc.getLeakages().subscribe({
      next: (d: any) => { this.loading = false; this.leakages = d; this.totalWaste = d.reduce((s: number, r: any) => s + (r.estimatedWastedCost || 0), 0); },
      error: () => { this.loading = false; this.error = 'Failed to load leakage report. Please try again.'; }
    });
  }
}
