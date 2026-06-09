import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BudgetService } from '../services/budget.service';
import { AuthService } from '../services/auth.service';
import { SidebarComponent } from '../shared/sidebar.component';

@Component({
  selector: 'app-budget',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, SidebarComponent],
  template: `
    <div class="layout">
      <app-sidebar [role]="role"></app-sidebar>
      <main class="main">
        <div class="topbar"><h2>Budget & Alerts</h2></div>

        <div class="msg msg-warning" *ngIf="budget?.alertTriggered">{{ budget.alertMessage }}</div>

        <div class="card" *ngIf="budget?.configured">
          <div class="card-header"><span class="card-title">Current Budget Status</span></div>
          <div style="margin-bottom:8px;display:flex;justify-content:space-between;font-size:13px;">
            <span>₹0</span><span>₹{{ budget.monthlyLimit | number:'1.0-0' }}</span>
          </div>
          <div style="background:#e0e0dc;border-radius:8px;height:20px;position:relative;overflow:hidden;">
            <div [style.width]="(budget.usagePercent > 100 ? 100 : budget.usagePercent) + '%'"
                 style="height:100%;border-radius:8px;transition:width 0.5s;background:#1a1a1a;"></div>
          </div>
          <div style="display:flex;justify-content:space-between;margin-top:8px;font-size:13px;color:#4a5568;">
            <span>Spent: <strong>₹{{ budget.currentSpend | number:'1.0-0' }}</strong> ({{ budget.usagePercent }}%)</span>
            <span>Limit: <strong>₹{{ budget.monthlyLimit | number:'1.0-0' }}</strong></span>
          </div>
        </div>

        <div class="card">
          <div class="card-header"><span class="card-title">{{ budget?.configured ? 'Update' : 'Set' }} Budget Threshold</span></div>
          <div class="form-grid-2">
            <div class="form-group">
              <label>Monthly Budget Limit (₹)</label>
              <input type="number" class="form-control" [(ngModel)]="form.monthlyLimit" min="0" placeholder="e.g. 50000">
            </div>
            <div class="form-group">
              <label>Alert at (% of budget)</label>
              <input type="number" class="form-control" [(ngModel)]="form.alertPercentage" min="1" max="100" placeholder="e.g. 80">
              <small>Alert when spend reaches this % of your monthly limit</small>
            </div>
          </div>
          <div class="msg msg-error" *ngIf="err">{{ err }}</div>
          <div class="msg msg-success" *ngIf="ok">{{ ok }}</div>
          <button class="btn btn-primary" (click)="save()" [disabled]="saving">{{ saving ? 'Saving...' : 'Save Budget' }}</button>
        </div>
      </main>
    </div>
  `
})
export class BudgetComponent implements OnInit {
  budget: any = null; form = { monthlyLimit: 0, alertPercentage: 80 };
  saving = false; err = ''; ok = ''; role = 'CLIENT';
  constructor(private svc: BudgetService, private auth: AuthService) {}
  ngOnInit(): void { this.role = this.auth.getUserRole(); this.load(); }
  load(): void { this.svc.getBudget().subscribe({ next: (d: any) => { this.budget = d; if (d.configured) { this.form.monthlyLimit = d.monthlyLimit; this.form.alertPercentage = d.alertPercentage; } } }); }
  save(): void {
    if (!this.form.monthlyLimit || this.form.monthlyLimit <= 0) { this.err = 'Please enter a valid monthly limit.'; return; }
    this.saving = true; this.err = ''; this.ok = '';
    this.svc.setBudget(this.form).subscribe({ next: () => { this.saving = false; this.ok = 'Budget threshold saved successfully.'; this.load(); }, error: () => { this.saving = false; this.err = 'Failed to save. Please try again.'; } });
  }
}
