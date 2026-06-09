import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DashboardService } from '../services/dashboard.service';
import { AuthService } from '../services/auth.service';
import { SidebarComponent } from '../shared/sidebar.component';
declare const Chart: any;

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, SidebarComponent],
  template: `
    <div class="layout">
      <app-sidebar [role]="role"></app-sidebar>
      <main class="main">
        <div class="topbar"><h2>Resource Utilization Analytics</h2></div>

        <!-- Charts row (US007) -->
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:20px;margin-bottom:20px;" *ngIf="utilization.length > 0">
          <div class="card">
            <div class="card-header"><span class="card-title">CPU & RAM Usage</span></div>
            <canvas id="cpuRamChart" height="220"></canvas>
          </div>
          <div class="card">
            <div class="card-header"><span class="card-title">Storage & Uptime</span></div>
            <canvas id="storageChart" height="220"></canvas>
          </div>
        </div>

        <!-- Filter + Table (US007) -->
        <div class="card">
          <div class="card-header">
            <span class="card-title">Resource Details</span>
            <select class="form-control" style="width:200px;" [(ngModel)]="filterType" (change)="applyFilter()">
              <option value="">All Resource Types</option>
              <option>VM</option><option>Storage</option><option>Database</option><option>Network</option>
            </select>
          </div>
          <div *ngIf="filtered.length === 0" class="no-data">
            <div class="icon"></div>No utilization data available. <a routerLink="/upload">Upload data</a> to get started.
          </div>
          <div class="table-wrap" *ngIf="filtered.length > 0">
            <table class="table">
              <thead><tr><th>Resource</th><th>Type</th><th>Provider</th><th>Region</th><th>CPU %</th><th>RAM %</th><th>Storage GB</th><th>Uptime %</th><th>Cost (₹)</th></tr></thead>
              <tbody>
                <tr *ngFor="let r of filtered">
                  <td><strong>{{ r.name }}</strong></td>
                  <td><span class="badge badge-blue">{{ r.resourceType }}</span></td>
                  <td>{{ r.provider }}</td>
                  <td>{{ r.region }}</td>
                  <td [class]="r.cpuUsage < 20 ? 'text-red' : 'text-green'">{{ r.cpuUsage }}%</td>
                  <td>{{ r.ramUsage }}%</td>
                  <td>{{ r.storageUsage }} GB</td>
                  <td [class]="r.uptime < 50 ? 'text-red' : 'text-green'">{{ r.uptime }}%</td>
                  <td><strong>₹{{ r.cost | number:'1.0-0' }}</strong></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Recommendations (US010) -->
        <div class="topbar" style="margin-top:8px;"><h2>Optimization Recommendations</h2></div>
        <div *ngIf="recs.length === 0" class="no-data"><div class="icon"></div>No optimization opportunities found.</div>
        <div *ngFor="let r of recs" class="card">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;">
            <strong>{{ r.resource }}</strong>
            <span class="badge" [class]="r.severity==='high'?'badge-red':r.severity==='medium'?'badge-orange':'badge-green'">
              {{ r.severity | uppercase }}
            </span>
          </div>
          <div style="font-size:13px;display:flex;flex-direction:column;gap:4px;">
            <span>Issue: {{ r.issue }}</span>
            <span>Action: {{ r.action }}</span>
            <span>Estimated Savings: {{ r.savings }}</span>
          </div>
        </div>
      </main>
    </div>
  `
})
export class AnalyticsComponent implements OnInit {
  utilization: any[] = []; filtered: any[] = []; recs: any[] = [];
  filterType = ''; role = 'CLIENT';
  private cpuChart: any = null;
  private storageChart: any = null;

  constructor(private svc: DashboardService, private auth: AuthService) {}

  ngOnInit(): void {
    this.role = this.auth.getUserRole();
    this.svc.getUtilization().subscribe({
      next: (d: any) => {
        this.utilization = d || [];
        this.applyFilter();
        if (this.utilization.length > 0) setTimeout(() => this.renderCharts(), 100);
      }
    });
    this.svc.getRecommendations().subscribe({ next: (d: any) => this.recs = d });
  }

  applyFilter(): void {
    this.filtered = this.filterType
      ? this.utilization.filter(r => r.resourceType === this.filterType)
      : [...this.utilization];
  }

  renderCharts(): void {
    const top = this.utilization.slice(0, 8);
    const labels = top.map((r: any) => r.name);

    const cpuCanvas = document.getElementById('cpuRamChart') as HTMLCanvasElement;
    if (cpuCanvas) {
      if (this.cpuChart) this.cpuChart.destroy();
      this.cpuChart = new Chart(cpuCanvas, {
        type: 'bar',
        data: {
          labels,
          datasets: [
            { label: 'CPU %', data: top.map((r: any) => r.cpuUsage), backgroundColor: '#6366f1' },
            { label: 'RAM %', data: top.map((r: any) => r.ramUsage), backgroundColor: '#22c55e' }
          ]
        },
        options: { responsive: true, scales: { y: { beginAtZero: true, max: 100 } }, plugins: { legend: { position: 'bottom' } } }
      });
    }

    const stCanvas = document.getElementById('storageChart') as HTMLCanvasElement;
    if (stCanvas) {
      if (this.storageChart) this.storageChart.destroy();
      this.storageChart = new Chart(stCanvas, {
        type: 'bar',
        data: {
          labels,
          datasets: [
            { label: 'Storage GB', data: top.map((r: any) => r.storageUsage), backgroundColor: '#f59e0b', yAxisID: 'y' },
            { label: 'Uptime %', data: top.map((r: any) => r.uptime), backgroundColor: '#06b6d4', yAxisID: 'y1' }
          ]
        },
        options: {
          responsive: true,
          scales: {
            y: { beginAtZero: true, position: 'left', title: { display: true, text: 'Storage GB' } },
            y1: { beginAtZero: true, max: 100, position: 'right', title: { display: true, text: 'Uptime %' }, grid: { drawOnChartArea: false } }
          },
          plugins: { legend: { position: 'bottom' } }
        }
      });
    }
  }
}
