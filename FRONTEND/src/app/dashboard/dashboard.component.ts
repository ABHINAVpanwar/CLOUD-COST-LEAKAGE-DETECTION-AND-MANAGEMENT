import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { DashboardService } from '../services/dashboard.service';
import { AuthService } from '../services/auth.service';
import { BudgetService } from '../services/budget.service';
import { SidebarComponent } from '../shared/sidebar.component';
declare const Chart: any;

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, SidebarComponent],
  template: `
    <div class="layout">
      <app-sidebar [role]="role"></app-sidebar>
      <main class="main">
        <div class="topbar">
          <h2>Dashboard</h2>
          <div class="topbar-actions">
            <button class="btn btn-success btn-sm" (click)="downloadReport()" [disabled]="downloading">
              {{ downloading ? 'Downloading...' : 'Download Report' }}
            </button>
            <div class="user-chip">
              <div class="avatar">{{ username[0].toUpperCase() }}</div>
              {{ username }}
            </div>
          </div>
        </div>
        <div class="msg msg-success" *ngIf="downloadMsg === 'ok'" style="margin-bottom:12px;">Report downloaded successfully.</div>
        <div class="msg msg-error" *ngIf="downloadMsg === 'err'" style="margin-bottom:12px;">Report download failed. Please try again.</div>

        <div class="msg msg-warning" *ngIf="budgetAlert?.alertTriggered">{{ budgetAlert.alertMessage }}</div>

        <div *ngIf="noData" class="no-data">
          <div class="icon"></div>
          No usage data available. Please <a routerLink="/upload">upload cloud usage data</a>.
        </div>

        <div *ngIf="!noData">
          <!-- Welcome message (US003) -->
          <div class="msg msg-info" style="margin-bottom:20px;font-size:15px;">{{ welcomeMessage }}</div>

          <div class="stats-grid">
            <div class="stat-card blue">
              <div class="stat-value">₹{{ totalSpend | number:'1.0-0' }}</div>
              <div class="stat-label">Total Cloud Spend</div>
            </div>
            <div class="stat-card green">
              <div class="stat-value">₹{{ estimatedSavings | number:'1.0-0' }}</div>
              <div class="stat-label">Estimated Savings</div>
            </div>
            <div class="stat-card red">
              <div class="stat-value">{{ totalLeakages }}</div>
              <div class="stat-label">Cost Leakages</div>
            </div>
            <div class="stat-card purple">
              <div class="stat-value">{{ totalResources }}</div>
              <div class="stat-label">Total Resources</div>
            </div>
          </div>

          <!-- Cost by Service chart (US003, US009) -->
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:20px;">
            <div class="card">
              <div class="card-header"><span class="card-title">Cost by Service</span></div>
              <div *ngIf="(costByService | keyvalue).length === 0" class="text-gray">No cost data available.</div>
              <canvas id="costChart" height="220" *ngIf="(costByService | keyvalue).length > 0"></canvas>
            </div>
            <div class="card">
              <div class="card-header"><span class="card-title">Usage Overview</span></div>
              <canvas id="usageChart" height="220" *ngIf="utilization.length > 0"></canvas>
              <div *ngIf="utilization.length === 0" class="text-gray">No utilization data available.</div>
            </div>
          </div>

          <!-- Leakage Summary (US003, US008) -->
          <div class="card">
            <div class="card-header">
              <span class="card-title">Leakage Summary</span>
              <a routerLink="/leakages" class="btn btn-outline btn-sm">View Full Report →</a>
            </div>
            <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:12px;">
              <div *ngFor="let item of leakageSummary | keyvalue" style="background:#f5f5f0;border-radius:10px;padding:16px;text-align:center;">
                <div style="font-size:28px;font-weight:700;color:#1a1a1a;">{{ item.value }}</div>
                <div style="font-size:12px;color:#666;margin-top:4px;">{{ item.key }}</div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  `
})
export class DashboardComponent implements OnInit {
  welcomeMessage = ''; totalSpend = 0; estimatedSavings = 0; totalLeakages = 0; totalResources = 0;
  costByService: any = {}; leakageSummary: any = {}; utilization: any[] = [];
  noData = false; username = ''; role = 'CLIENT';
  budgetAlert: any = null; downloading = false; downloadMsg = '';
  private costChartInstance: any = null;
  private usageChartInstance: any = null;

  constructor(private dashboardService: DashboardService, private authService: AuthService,
              private budgetService: BudgetService, private http: HttpClient) {}

  ngOnInit(): void {
    this.username = this.authService.getUsername();
    this.role = this.authService.getUserRole();
    this.dashboardService.getDashboardData().subscribe({
      next: (d: any) => {
        this.welcomeMessage = d.welcomeMessage || ('Welcome, ' + this.username + '!');
        this.totalSpend = d.totalSpend || 0;
        this.estimatedSavings = d.estimatedSavings || 0;
        this.totalLeakages = d.totalLeakages || 0;
        this.costByService = d.costByService || {};
        this.leakageSummary = d.leakageSummary || {};
        this.noData = this.totalSpend === 0 && Object.keys(this.costByService).length === 0;
        if (!this.noData) setTimeout(() => this.renderCostChart(), 100);
      },
      error: () => { this.noData = true; }
    });
    this.dashboardService.getUtilization().subscribe({
      next: (d: any) => {
        this.utilization = d || [];
        this.totalResources = this.utilization.length;
        if (this.utilization.length > 0) setTimeout(() => this.renderUsageChart(), 100);
      },
      error: () => {}
    });
    this.budgetService.getBudget().subscribe({ next: (d: any) => { if (d?.alertTriggered) this.budgetAlert = d; }, error: () => {} });
  }

  renderCostChart(): void {
    const canvas = document.getElementById('costChart') as HTMLCanvasElement;
    if (!canvas) return;
    if (this.costChartInstance) this.costChartInstance.destroy();
    const labels = Object.keys(this.costByService);
    const data = Object.values(this.costByService).map(Number);
    this.costChartInstance = new Chart(canvas, {
      type: 'doughnut',
      data: { labels, datasets: [{ data, backgroundColor: ['#6366f1','#22c55e','#f59e0b','#ef4444','#06b6d4','#a855f7'] }] },
      options: { responsive: true, plugins: { legend: { position: 'bottom' } } }
    });
  }

  renderUsageChart(): void {
    const canvas = document.getElementById('usageChart') as HTMLCanvasElement;
    if (!canvas) return;
    if (this.usageChartInstance) this.usageChartInstance.destroy();
    const labels = this.utilization.slice(0, 8).map((r: any) => r.name);
    this.usageChartInstance = new Chart(canvas, {
      type: 'bar',
      data: {
        labels,
        datasets: [
          { label: 'CPU %', data: this.utilization.slice(0,8).map((r:any) => r.cpuUsage), backgroundColor: '#6366f1' },
          { label: 'RAM %', data: this.utilization.slice(0,8).map((r:any) => r.ramUsage), backgroundColor: '#22c55e' }
        ]
      },
      options: { responsive: true, scales: { y: { beginAtZero: true, max: 100 } }, plugins: { legend: { position: 'bottom' } } }
    });
  }

  getBarWidth(value: any): number {
    const max = Math.max(...Object.values(this.costByService).map((v: any) => Number(v)), 1);
    return Math.round((Number(value) / max) * 100);
  }

  downloadReport(): void {
    this.downloading = true; this.downloadMsg = '';
    this.http.get('http://localhost:8080/api/report/download/csv', { responseType: 'blob' }).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a'); a.href = url;
        a.download = `cloud-cost-report-${new Date().toISOString().slice(0,10)}.csv`;
        a.click(); window.URL.revokeObjectURL(url);
        this.downloading = false;
        this.downloadMsg = 'ok';
        setTimeout(() => this.downloadMsg = '', 4000);
      },
      error: () => { this.downloading = false; this.downloadMsg = 'err'; }
    });
  }
}
