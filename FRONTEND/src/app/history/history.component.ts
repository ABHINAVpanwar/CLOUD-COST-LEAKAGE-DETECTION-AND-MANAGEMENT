import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DashboardService } from '../services/dashboard.service';
import { AuthService } from '../services/auth.service';
import { SidebarComponent } from '../shared/sidebar.component';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [CommonModule, RouterLink, SidebarComponent],
  template: `
    <div class="layout">
      <app-sidebar [role]="role"></app-sidebar>
      <main class="main">
        <div class="topbar"><h2>Analysis History</h2></div>
        <div *ngIf="history.length === 0" class="no-data">
          <div class="icon"></div>No upload history found. <a routerLink="/upload">Upload data</a> to get started.
        </div>
        <div class="card" *ngIf="history.length > 0">
          <div class="table-wrap">
            <table class="table">
              <thead><tr><th>#</th><th>Date</th><th>Resources</th><th>Total Cost (₹)</th><th>Est. Savings (₹)</th><th>vs Previous</th></tr></thead>
              <tbody>
                <tr *ngFor="let item of history; let i = index">
                  <td>{{ i + 1 }}</td>
                  <td>{{ item.date }}</td>
                  <td>{{ item.resourceCount }}</td>
                  <td><strong>₹{{ item.totalCost | number:'1.0-0' }}</strong></td>
                  <td class="text-green">₹{{ item.estimatedSavings | number:'1.0-0' }}</td>
                  <td>
                    <span *ngIf="i === 0" class="badge badge-gray">—</span>
                    <span *ngIf="i > 0 && item.totalCost < history[i-1].totalCost" class="badge badge-green">↓ ₹{{ (history[i-1].totalCost - item.totalCost) | number:'1.0-0' }} saved</span>
                    <span *ngIf="i > 0 && item.totalCost > history[i-1].totalCost" class="badge badge-red">↑ ₹{{ (item.totalCost - history[i-1].totalCost) | number:'1.0-0' }} increase</span>
                    <span *ngIf="i > 0 && item.totalCost === history[i-1].totalCost" class="badge badge-gray">No change</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>
  `
})
export class HistoryComponent implements OnInit {
  history: any[] = []; role = 'CLIENT';
  constructor(private svc: DashboardService, private auth: AuthService) {}
  ngOnInit(): void { this.role = this.auth.getUserRole(); this.svc.getHistory().subscribe({ next: (d: any) => this.history = d }); }
}
