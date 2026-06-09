import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../services/auth.service';
import { SidebarComponent } from '../shared/sidebar.component';

@Component({
  selector: 'app-support',
  standalone: true,
  imports: [CommonModule, FormsModule, SidebarComponent],
  template: `
    <div class="layout">
      <app-sidebar role="CLIENT"></app-sidebar>
      <main class="main">
        <div class="topbar"><h2>Support</h2></div>

        <div class="grid-2" style="display:grid;grid-template-columns:1fr 1fr;gap:24px;align-items:start;">

          <!-- Submit Ticket -->
          <div class="card">
            <div class="card-header"><span class="card-title">Submit a Support Ticket</span></div>
            <form (ngSubmit)="submit()">
              <div class="form-group">
                <label>Send To *</label>
                <select class="form-control" [(ngModel)]="form.assignedTo" name="assignedTo">
                  <option value="ENGINEER">Cloud Engineer</option>
                  <option value="ADMIN">Admin</option>
                </select>
              </div>
              <div class="form-group">
                <label>Priority</label>
                <select class="form-control" [(ngModel)]="form.priority" name="priority">
                  <option value="LOW">Low</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="HIGH">High</option>
                </select>
              </div>
              <div class="form-group">
                <label>Subject *</label>
                <input type="text" class="form-control" [(ngModel)]="form.subject" name="subject"
                  placeholder="Brief description of your issue" [class.invalid]="touched && !form.subject">
              </div>
              <div class="form-group">
                <label>Message *</label>
                <textarea class="form-control" [(ngModel)]="form.message" name="message" rows="5"
                  placeholder="Describe your issue in detail..." [class.invalid]="touched && !form.message"></textarea>
              </div>
              <div class="msg msg-error" *ngIf="error">{{ error }}</div>
              <div class="msg msg-success" *ngIf="success">{{ success }}</div>
              <button type="submit" class="btn btn-primary" [disabled]="submitting">
                {{ submitting ? 'Submitting...' : 'Submit Ticket' }}
              </button>
            </form>
          </div>

          <!-- My Tickets -->
          <div class="card">
            <div class="card-header"><span class="card-title">My Tickets</span></div>
            <div *ngIf="loading" class="loading">Loading...</div>
            <div *ngIf="!loading && tickets.length === 0" class="no-data" style="padding:24px;">
              No tickets submitted yet.
            </div>
            <div *ngFor="let t of tickets" class="ticket-item">
              <div class="ticket-header">
                <strong>{{ t.subject }}</strong>
                <span class="badge" [class]="statusClass(t.status)">{{ t.status }}</span>
              </div>
              <div class="ticket-meta">
                To: <strong>{{ t.assignedTo }}</strong> &nbsp;·&nbsp;
                Priority: <span [class]="priorityClass(t.priority)">{{ t.priority }}</span> &nbsp;·&nbsp;
                {{ t.createdAt | date:'mediumDate' }}
              </div>
              <div class="ticket-msg">{{ t.message }}</div>
            </div>
          </div>

        </div>
      </main>
    </div>
  `,
  styles: [`
    .ticket-item { border:1px solid #e0e0dc; border-radius:10px; padding:14px; margin-bottom:12px; }
    .ticket-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:6px; }
    .ticket-meta { font-size:12px; color:#888; margin-bottom:6px; }
    .ticket-msg { font-size:13px; color:#444; white-space:pre-wrap; }
    .pri-high { color:#c00; font-weight:600; }
    .pri-medium { color:#888; font-weight:600; }
    .pri-low { color:#aaa; }
  `]
})
export class SupportComponent implements OnInit {
  form = { assignedTo: 'ENGINEER', priority: 'MEDIUM', subject: '', message: '' };
  submitting = false; touched = false; error = ''; success = '';
  tickets: any[] = []; loading = true;

  constructor(private http: HttpClient, private auth: AuthService) {}

  ngOnInit(): void { this.loadTickets(); }

  loadTickets(): void {
    this.http.get<any[]>('http://localhost:8080/api/support/my-tickets').subscribe({
      next: (d) => { this.tickets = d; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  submit(): void {
    this.touched = true;
    if (!this.form.subject || !this.form.message) return;
    this.submitting = true; this.error = ''; this.success = '';
    this.http.post<any>('http://localhost:8080/api/support/ticket', this.form).subscribe({
      next: (r) => {
        this.submitting = false;
        this.success = `Ticket #${r.ticketId} submitted successfully.`;
        this.form = { assignedTo: 'ENGINEER', priority: 'MEDIUM', subject: '', message: '' };
        this.touched = false;
        this.loadTickets();
      },
      error: (e) => { this.submitting = false; this.error = e.error?.error || 'Failed to submit ticket.'; }
    });
  }

  statusClass(s: string): string {
    if (s === 'RESOLVED') return 'badge-dark';
    if (s === 'IN_PROGRESS') return 'badge-outline';
    return 'badge-gray';
  }

  priorityClass(p: string): string {
    if (p === 'HIGH') return 'pri-high';
    if (p === 'MEDIUM') return 'pri-medium';
    return 'pri-low';
  }
}
