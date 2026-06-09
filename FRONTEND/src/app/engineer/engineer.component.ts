import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { EngineerService } from '../services/engineer.service';
import { AuthService } from '../services/auth.service';
import { SidebarComponent } from '../shared/sidebar.component';

@Component({
  selector: 'app-engineer',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, SidebarComponent],
  template: `
    <div class="layout">
      <app-sidebar role="ENGINEER"></app-sidebar>

      <main class="main">
        <div class="topbar">
          <h2>Engineer Dashboard</h2>
          <div style="display:flex;gap:8px;">
            <button class="btn" [class.btn-primary]="tab==='clients'" [class.btn-secondary]="tab!=='clients'" (click)="tab='clients'">Clients</button>
            <button class="btn" [class.btn-primary]="tab==='bulk'" [class.btn-secondary]="tab!=='bulk'" (click)="runBulk()">Bulk Analysis</button>
            <button class="btn" [class.btn-primary]="tab==='savings'" [class.btn-secondary]="tab!=='savings'" (click)="tab='savings'">Savings</button>
            <button class="btn" [class.btn-primary]="tab==='support'" [class.btn-secondary]="tab!=='support'" (click)="tab='support';loadSupportTickets()">Support Inbox</button>
          </div>
        </div>

        <!-- Clients List -->
        <div *ngIf="tab==='clients' && !selectedClient">
          <h2>Assigned Clients</h2>
          <div *ngIf="clients.length===0" class="no-data">No clients found.</div>
          <div class="clients-grid">
            <div *ngFor="let c of clients" class="client-card" (click)="selectClient(c)">
              <div class="client-name">{{ c.fullName || c.username }}</div>
              <div class="client-meta">{{ c.email }}</div>
              <div class="client-stats">
                <span>{{ c.resourceCount }} resources</span>
                <span>₹{{ c.totalCost | number:'1.0-0' }}</span>
              </div>
              <div class="client-status" [class]="c.status==='ACTIVE'?'active':'inactive'">{{ c.status }}</div>
            </div>
          </div>
        </div>

        <!-- Client Analysis Detail -->
        <div *ngIf="tab==='clients' && selectedClient">
          <div class="back-bar">
            <button class="btn-back" (click)="selectedClient=null; analysis=null">← Back to Clients</button>
            <h2>Analysis: {{ selectedClient.fullName || selectedClient.username }}</h2>
          </div>

          <div *ngIf="!analysis" class="loading">Loading analysis...</div>

          <div *ngIf="analysis">
            <!-- Summary -->
            <div class="stats-grid">
              <div class="stat-card blue"><div class="sv">₹{{ analysis.totalCost | number:'1.0-0' }}</div><div class="sl">Total Cost</div></div>
              <div class="stat-card green"><div class="sv">₹{{ analysis.estimatedSavings | number:'1.0-0' }}</div><div class="sl">Est. Savings</div></div>
              <div class="stat-card red"><div class="sv">{{ analysis.leakageCount }}</div><div class="sl">Leakages</div></div>
              <div class="stat-card purple"><div class="sv">{{ analysis.totalResources }}</div><div class="sl">Resources</div></div>
            </div>

            <!-- Threshold Config (US015 + US016) -->
            <div class="card">
              <h3>Threshold & Cost Configuration</h3>
              <div class="config-grid">
                <div class="form-group">
                  <label>CPU Idle Threshold (%)</label>
                  <input type="number" class="form-control" [(ngModel)]="config.cpuIdleThreshold" min="0" max="100">
                </div>
                <div class="form-group">
                  <label>CPU Underutilized Threshold (%)</label>
                  <input type="number" class="form-control" [(ngModel)]="config.cpuUnderutilizedThreshold" min="0" max="100">
                </div>
                <div class="form-group">
                  <label>Uptime Idle Threshold (%)</label>
                  <input type="number" class="form-control" [(ngModel)]="config.uptimeIdleThreshold" min="0" max="100">
                </div>
                <div class="form-group">
                  <label>Cost Coefficient (multiplier)</label>
                  <input type="number" class="form-control" [(ngModel)]="config.costCoefficient" min="0.1" step="0.1">
                  <small>e.g. 1.2 = 20% markup on base cost</small>
                </div>
              </div>
              <div class="config-meta" *ngIf="config.updatedAt">
                Last updated: {{ config.updatedAt | date:'medium' }} by {{ config.updatedBy }}
              </div>
              <div class="success-msg" *ngIf="configSaved">Configuration saved and analysis recalculated.</div>
              <div class="success-msg" *ngIf="configError" style="color:#c00;">{{ configError }}</div>
              <button class="btn btn-primary" (click)="saveConfig()" [disabled]="savingConfig">
                {{ savingConfig ? 'Saving...' : 'Save & Recalculate' }}
              </button>
            </div>

            <!-- Leakages -->
            <div class="card">
              <h3>Detected Leakages</h3>
              <div *ngIf="analysis.leakages.length===0" class="no-data-sm">No leakages detected with current thresholds.</div>
              <table class="table" *ngIf="analysis.leakages.length>0">
                <thead><tr><th>Resource</th><th>Type</th><th>CPU%</th><th>Uptime%</th><th>Cost</th><th>Issue</th><th>Est. Waste</th></tr></thead>
                <tbody>
                  <tr *ngFor="let l of analysis.leakages">
                    <td>{{ l.resourceName }}</td>
                    <td>{{ l.resourceType }}</td>
                    <td class="text-red">{{ l.cpuUsage }}%</td>
                    <td>{{ l.uptime }}%</td>
                    <td>₹{{ l.cost | number:'1.0-0' }}</td>
                    <td><span class="issue-badge">{{ l.issue }}</span></td>
                    <td class="text-red">₹{{ l.estimatedWaste | number:'1.0-0' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <!-- Recommendations -->
            <div class="card">
              <h3>Recommendations</h3>
              <div *ngIf="analysis.recommendations.length===0" class="no-data-sm">No recommendations for current thresholds.</div>
              <div *ngFor="let r of analysis.recommendations" class="rec-item" [class]="'rec-'+r.severity">
                <div class="rec-header">
                  <strong>{{ r.resource }}</strong>
                  <span class="sev-badge" [class]="'sev-'+r.severity">{{ r.severity | uppercase }}</span>
                </div>
                <div style="font-size:12px;color:#666;margin-bottom:4px;">{{ r.resourceType }}</div>
                <div style="margin-bottom:4px;">Issue: {{ r.issue }}</div>
                <div>Action: {{ r.action }}</div>
                <div class="savings-text">Est. Savings: ₹{{ r.savings | number:'1.0-0' }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Bulk Analysis (US017) -->
        <div *ngIf="tab==='bulk'">
          <h2>Bulk Analysis Results</h2>
          <div *ngIf="bulkLoading" class="loading">Running bulk analysis...</div>
          <div *ngIf="!bulkLoading && bulkResults.length===0" class="no-data">No data available.</div>
          <div class="card" *ngIf="!bulkLoading && bulkResults.length>0">
            <table class="table">
              <thead><tr><th>Client</th><th>Resources</th><th>Total Cost</th><th>Est. Savings</th><th>Leakages</th></tr></thead>
              <tbody>
                <tr *ngFor="let r of bulkResults">
                  <td>{{ r.clientName }}</td>
                  <td>{{ r.totalResources }}</td>
                  <td>₹{{ r.totalCost | number:'1.0-0' }}</td>
                  <td class="text-green">₹{{ r.estimatedSavings | number:'1.0-0' }}</td>
                  <td [class]="r.leakageCount>0?'text-red':''">{{ r.leakageCount }}</td>
                </tr>
                <tr class="total-row">
                  <td><strong>TOTAL</strong></td>
                  <td>{{ bulkTotal.resources }}</td>
                  <td>₹{{ bulkTotal.cost | number:'1.0-0' }}</td>
                  <td class="text-green">₹{{ bulkTotal.savings | number:'1.0-0' }}</td>
                  <td>{{ bulkTotal.leakages }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Savings Tracking (US018) -->
        <div *ngIf="tab==='savings'">
          <h2>Savings Tracking</h2>
          <div *ngIf="clients.length===0" class="no-data">No clients available.</div>
          <div class="card" *ngIf="clients.length>0">
            <div class="form-group">
              <label>Select Client</label>
              <select class="form-control" [(ngModel)]="savingsClientId" (change)="loadSavings()">
                <option value="">-- Select --</option>
                <option *ngFor="let c of clients" [value]="c.id">{{ c.fullName || c.username }}</option>
              </select>
            </div>
            <div *ngIf="savingsData">
              <div class="stats-grid" style="margin-top:16px;">
                <div class="stat-card green"><div class="sv">₹{{ savingsData.currentSavings | number:'1.0-0' }}</div><div class="sl">Current Est. Savings</div></div>
                <div class="stat-card blue"><div class="sv">₹{{ savingsData.currentCost | number:'1.0-0' }}</div><div class="sl">Current Total Cost</div></div>
              </div>
              <h4 style="margin-top:20px;">Configuration Change History</h4>
              <div *ngIf="savingsData.configChanges.length===0" class="no-data-sm">No configuration changes recorded.</div>
              <table class="table" *ngIf="savingsData.configChanges.length>0">
                <thead><tr><th>Timestamp</th><th>Details</th></tr></thead>
                <tbody>
                  <tr *ngFor="let c of savingsData.configChanges">
                    <td>{{ c.timestamp | date:'medium' }}</td>
                    <td>{{ c.details }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- Support Inbox -->
        <div *ngIf="tab==='support'">
          <h2>Support Inbox</h2>
          <div *ngIf="supportLoading" class="loading">Loading tickets...</div>
          <div *ngIf="!supportLoading && supportTickets.length===0" class="no-data">No support tickets assigned to you.</div>
          <div *ngFor="let t of supportTickets" class="card" style="margin-bottom:12px;">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;">
              <strong style="font-size:15px;">{{ t.subject }}</strong>
              <div style="display:flex;gap:8px;align-items:center;">
                <span class="badge" [class]="t.priority==='HIGH'?'badge-red':t.priority==='MEDIUM'?'badge-orange':'badge-gray'">{{ t.priority }}</span>
                <select class="form-control" style="width:140px;padding:4px 8px;font-size:12px;" [(ngModel)]="t.status" (change)="updateTicketStatus(t)">
                  <option value="OPEN">OPEN</option>
                  <option value="IN_PROGRESS">IN PROGRESS</option>
                  <option value="RESOLVED">RESOLVED</option>
                </select>
              </div>
            </div>
            <div style="font-size:12px;color:#888;margin-bottom:8px;">From: <strong>{{ t.clientName }}</strong> ({{ t.clientUsername }}) · {{ t.createdAt | date:'mediumDate' }}</div>
            <div style="font-size:13px;color:#444;white-space:pre-wrap;">{{ t.message }}</div>
          </div>
        </div>

      </main>
    </div>
  `,
  styles: [`
    .clients-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:16px;}
    .client-card{background:white;border-radius:10px;padding:20px;border:1px solid #e0e0dc;cursor:pointer;transition:transform 0.2s;}
    .client-card:hover{transform:translateY(-2px);border-color:#aaa;}
    .client-name{font-weight:700;font-size:16px;color:#1a1a1a;margin-bottom:4px;}
    .client-meta{font-size:13px;color:#666;margin-bottom:8px;}
    .client-stats{display:flex;justify-content:space-between;font-size:13px;color:#333;margin-bottom:8px;}
    .client-status{display:inline-block;padding:2px 10px;border-radius:12px;font-size:12px;font-weight:600;}
    .active{background:#e0e0dc;color:#1a1a1a;} .inactive{background:#f0f0ec;color:#555;}
    .back-bar{display:flex;align-items:center;gap:16px;margin-bottom:20px;}
    .btn-back{background:#e0e0dc;border:none;padding:8px 16px;border-radius:6px;cursor:pointer;font-size:14px;}
    .stats-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-bottom:20px;}
    .stat-card{background:white;border-radius:10px;padding:16px;border:1px solid #e0e0dc;}
    .sv{font-size:22px;font-weight:700;color:#1a1a1a;} .sl{font-size:12px;color:#666;margin-top:4px;}
    .card{background:white;border-radius:10px;padding:20px;border:1px solid #e0e0dc;margin-bottom:20px;}
    .card h3{margin:0 0 16px;color:#1a1a1a;}
    .config-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:16px;margin-bottom:12px;}
    .form-group{margin-bottom:4px;}
    .form-group label{display:block;margin-bottom:5px;font-weight:500;font-size:13px;color:#333;}
    .form-group small{color:#666;font-size:11px;}
    .form-control{width:100%;padding:8px 10px;border:1.5px solid #e0e0dc;border-radius:6px;font-size:14px;box-sizing:border-box;}
    .config-meta{font-size:12px;color:#666;margin-bottom:12px;}
    .success-msg{color:#1a1a1a;font-size:13px;margin-bottom:8px;}
    .btn{padding:10px 20px;border:none;border-radius:6px;cursor:pointer;font-size:14px;font-weight:500;}
    .btn-primary{background:#1a1a1a;color:white;}
    .btn-secondary{background:#f0f0ec;color:#1a1a1a;border:1px solid #ccc;}
    .btn-primary:disabled{background:#aaa;cursor:not-allowed;}
    .table{width:100%;border-collapse:collapse;font-size:13px;}
    .table th{background:#f5f5f0;padding:10px 12px;text-align:left;color:#333;font-weight:600;border-bottom:2px solid #e0e0dc;}
    .table td{padding:10px 12px;border-bottom:1px solid #e0e0dc;}
    .total-row td{font-weight:700;background:#f5f5f0;}
    .text-red{color:#1a1a1a;font-weight:600;} .text-green{color:#1a1a1a;font-weight:600;}
    .issue-badge{background:#f0f0ec;color:#1a1a1a;padding:2px 8px;border-radius:12px;font-size:12px;}
    .rec-item{border-radius:8px;padding:14px;margin-bottom:10px;border-left:4px solid #aaa;background:#f5f5f0;}
    .rec-high{border-color:#1a1a1a;} .rec-medium{border-color:#555;}
    .rec-header{display:flex;justify-content:space-between;margin-bottom:6px;}
    .sev-badge{padding:2px 10px;border-radius:12px;font-size:11px;font-weight:700;background:#e0e0dc;color:#1a1a1a;}
    .savings-text{font-size:13px;margin-top:4px;}
    @media(max-width:900px){.clients-grid{grid-template-columns:1fr 1fr;}.stats-grid{grid-template-columns:1fr 1fr;}}
  `]
})
export class EngineerComponent implements OnInit {
  tab = 'clients';
  clients: any[] = [];
  selectedClient: any = null;
  analysis: any = null;
  config: any = { cpuIdleThreshold: 10, cpuUnderutilizedThreshold: 60, uptimeIdleThreshold: 80, costCoefficient: 1.0 };
  savingConfig = false;
  configSaved = false;
  configError = '';
  bulkResults: any[] = [];
  bulkLoading = false;
  bulkTotal: any = { resources: 0, cost: 0, savings: 0, leakages: 0 };
  savingsClientId = '';
  savingsData: any = null;

  supportTickets: any[] = []; supportLoading = false;

  constructor(private engineerService: EngineerService, private authService: AuthService, private router: Router, private http: HttpClient) {}

  ngOnInit(): void {
    this.engineerService.getClients().subscribe({ next: (data) => this.clients = data, error: () => {} });
  }

  selectClient(client: any): void {
    this.selectedClient = client;
    this.analysis = null;
    this.configSaved = false;
    this.engineerService.getClientAnalysis(client.id).subscribe({
      next: (data) => {
        this.analysis = data;
        this.config = { ...data.config };
      }
    });
  }

  saveConfig(): void {
    this.configError = '';
    if (this.config.cpuIdleThreshold >= this.config.cpuUnderutilizedThreshold) {
      this.configError = 'CPU Idle Threshold must be less than CPU Underutilized Threshold.';
      return;
    }
    if (this.config.costCoefficient <= 0) {
      this.configError = 'Cost Coefficient must be greater than 0.';
      return;
    }
    this.savingConfig = true;
    this.configSaved = false;
    this.engineerService.saveConfig(this.selectedClient.id, this.config).subscribe({
      next: () => {
        this.savingConfig = false;
        this.configSaved = true;
        // Reload analysis with new config
        this.engineerService.getClientAnalysis(this.selectedClient.id).subscribe({
          next: (data) => { this.analysis = data; this.config = { ...data.config }; }
        });
      },
      error: (e: any) => { this.savingConfig = false; this.configError = e.error?.error || 'Failed to save configuration.'; }
    });
  }

  runBulk(): void {
    this.tab = 'bulk';
    this.bulkLoading = true;
    this.bulkResults = [];
    this.engineerService.bulkAnalysis().subscribe({
      next: (data) => {
        this.bulkResults = data;
        this.bulkTotal = data.reduce((acc: any, r: any) => ({
          resources: acc.resources + r.totalResources,
          cost: acc.cost + r.totalCost,
          savings: acc.savings + r.estimatedSavings,
          leakages: acc.leakages + r.leakageCount
        }), { resources: 0, cost: 0, savings: 0, leakages: 0 });
        this.bulkLoading = false;
      },
      error: () => { this.bulkLoading = false; }
    });
  }

  loadSavings(): void {
    if (!this.savingsClientId) return;
    this.engineerService.getSavings(Number(this.savingsClientId)).subscribe({
      next: (data) => this.savingsData = data
    });
  }

  loadSupportTickets(): void {
    this.supportLoading = true;
    this.http.get<any[]>('http://localhost:8080/api/support/engineer-tickets').subscribe({
      next: (d) => { this.supportTickets = d; this.supportLoading = false; },
      error: () => { this.supportLoading = false; }
    });
  }

  updateTicketStatus(ticket: any): void {
    this.http.put(`http://localhost:8080/api/support/ticket/${ticket.id}/status`, { status: ticket.status }).subscribe();
  }

  logout(): void { /* handled by sidebar */ }
}
