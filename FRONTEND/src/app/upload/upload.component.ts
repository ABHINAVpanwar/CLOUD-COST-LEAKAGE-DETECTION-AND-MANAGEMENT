import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { UploadService } from '../services/upload.service';
import { AuthService } from '../services/auth.service';
import { SidebarComponent } from '../shared/sidebar.component';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink, SidebarComponent],
  template: `
    <div class="layout">
      <app-sidebar [role]="role"></app-sidebar>
      <main class="main">
        <div class="topbar"><h2>Upload Cloud Usage Data</h2></div>

        <!-- File Upload -->
        <div class="card">
          <div class="card-header"><span class="card-title">Upload CSV File</span></div>
          <p style="color:#718096;font-size:13px;margin-bottom:16px;">
            Format: <code>resource_type, resource_name, provider, region, cpu_usage, ram_usage, storage_usage, cost, uptime</code>
          </p>
          <div class="upload-zone" (click)="fi.click()" [class.has-file]="selectedFile">
            <input #fi type="file" (change)="onFile($event)" accept=".csv,.txt" style="display:none">
            <div *ngIf="!selectedFile">
              <div style="font-size:32px;margin-bottom:8px;"></div>
              <div style="font-weight:600;">Click to select a CSV file</div>
              <div style="font-size:12px;color:#888;margin-top:4px;">Supported: .csv, .txt</div>
            </div>
            <div *ngIf="selectedFile">
              <div style="font-weight:600;">{{ selectedFile.name }}</div>
            </div>
          </div>
          <div class="msg msg-error" *ngIf="uploadErr" style="margin-top:12px;">{{ uploadErr }}</div>
          <div class="msg msg-success" *ngIf="uploadOk" style="margin-top:12px;">{{ uploadOk }}</div>
          <button class="btn btn-primary" style="margin-top:12px;" (click)="upload()" [disabled]="!selectedFile || uploading">
            {{ uploading ? 'Uploading...' : 'Upload File' }}
          </button>
        </div>

        <!-- Manual Entry -->
        <div class="card">
          <div class="card-header"><span class="card-title">Manual Data Entry</span></div>
          <form (ngSubmit)="submitManual()">
            <div class="form-grid-3">
              <div class="form-group">
                <label>Resource Type *</label>
                <select class="form-control" [(ngModel)]="m.resourceType" name="rt">
                  <option>VM</option><option>Storage</option><option>Database</option><option>Network</option>
                </select>
              </div>
              <div class="form-group">
                <label>Resource Name *</label>
                <input type="text" class="form-control" [(ngModel)]="m.resourceName" name="rn" [class.invalid]="mt && !m.resourceName">
                <div class="msg msg-error" style="padding:4px 8px;margin-top:4px;" *ngIf="mt && !m.resourceName">Resource name is required.</div>
              </div>
              <div class="form-group">
                <label>Provider</label>
                <input type="text" class="form-control" [(ngModel)]="m.provider" name="pv">
              </div>
              <div class="form-group">
                <label>Region</label>
                <input type="text" class="form-control" [(ngModel)]="m.region" name="rg">
              </div>
              <div class="form-group">
                <label>CPU Usage (%)</label>
                <input type="number" class="form-control" [(ngModel)]="m.cpuUsage" name="cpu" min="0" max="100">
              </div>
              <div class="form-group">
                <label>RAM Usage (%)</label>
                <input type="number" class="form-control" [(ngModel)]="m.ramUsage" name="ram" min="0" max="100">
              </div>
              <div class="form-group">
                <label>Storage (GB)</label>
                <input type="number" class="form-control" [(ngModel)]="m.storageUsage" name="st" min="0">
              </div>
              <div class="form-group">
                <label>Cost (₹)</label>
                <input type="number" class="form-control" [(ngModel)]="m.cost" name="cost" min="0">
              </div>
              <div class="form-group">
                <label>Uptime (%)</label>
                <input type="number" class="form-control" [(ngModel)]="m.uptime" name="up" min="0" max="100">
              </div>
            </div>
            <div class="msg msg-error" *ngIf="manualErr">{{ manualErr }}</div>
            <div class="msg msg-success" *ngIf="manualOk">{{ manualOk }}</div>
            <div style="display:flex;gap:10px;margin-top:8px;">
              <button type="submit" class="btn btn-primary" [disabled]="submitting">{{ submitting ? 'Submitting...' : 'Submit Data' }}</button>
              <button type="button" class="btn btn-secondary" (click)="resetManual()">Reset</button>
            </div>
          </form>
        </div>
      </main>
    </div>
  `,
  styles: [`
    .upload-zone { border:2px dashed #ccc;border-radius:10px;padding:36px;text-align:center;cursor:pointer;transition:all 0.2s; }
    .upload-zone:hover,.upload-zone.has-file { border-color:#1a1a1a;background:#f0f0ec; }
  `]
})
export class UploadComponent implements OnInit {
  selectedFile: File | null = null; uploading = false; uploadErr = ''; uploadOk = '';
  m = { resourceType:'VM', resourceName:'', provider:'AWS', region:'us-east-1', cpuUsage:50, ramUsage:50, storageUsage:100, cost:1000, uptime:90 };
  submitting = false; manualErr = ''; manualOk = ''; mt = false; role = 'CLIENT';

  constructor(private svc: UploadService, private auth: AuthService) {}
  ngOnInit(): void { this.role = this.auth.getUserRole(); }

  onFile(e: any): void {
    const f = e.target.files[0]; this.uploadErr = ''; this.uploadOk = '';
    if (!f) return;
    if (!f.name.toLowerCase().endsWith('.csv') && !f.name.toLowerCase().endsWith('.txt')) { this.uploadErr = 'Invalid file format. Only CSV files are allowed.'; return; }
    this.selectedFile = f;
  }

  upload(): void {
    if (!this.selectedFile) return;
    this.uploading = true; this.uploadErr = ''; this.uploadOk = '';
    this.svc.uploadFile(this.selectedFile).subscribe({
      next: (r: any) => { this.uploading = false; this.uploadOk = r.message || 'File uploaded successfully.'; this.selectedFile = null; },
      error: (e: any) => { this.uploading = false; this.uploadErr = e.error?.error || 'Error processing file. Please try again.'; }
    });
  }

  submitManual(): void {
    this.mt = true; if (!this.m.resourceName) return;
    this.submitting = true; this.manualErr = ''; this.manualOk = '';
    this.svc.addManualEntry(this.m).subscribe({
      next: () => { this.submitting = false; this.manualOk = 'Data submitted successfully.'; this.resetManual(); },
      error: (e: any) => { this.submitting = false; this.manualErr = e.error?.error || 'Error processing data. Please try again.'; }
    });
  }

  resetManual(): void { this.m = { resourceType:'VM', resourceName:'', provider:'AWS', region:'us-east-1', cpuUsage:50, ramUsage:50, storageUsage:100, cost:1000, uptime:90 }; this.mt = false; this.manualErr = ''; }
}
