import { Component, OnInit } from '@angular/core';
import { AdminService, AdminDashboardData, User, AuditLog } from '../services/admin.service';
import { AuthService } from '../services/auth.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { SidebarComponent } from '../shared/sidebar.component';

@Component({
  selector: 'app-admin',
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.css'],
  imports: [CommonModule, FormsModule, RouterLink, SidebarComponent],
  standalone: true
})
export class AdminComponent implements OnInit {
  dashboardData: AdminDashboardData | null = null;
  activeTab: string = 'dashboard';
  users: User[] = [];
  auditLogs: AuditLog[] = [];
  settings: Record<string, string> = {};
  stats: any = {};
  loading: boolean = false;
  searchTerm: string = '';
  successMsg: string = '';
  // Pagination
  page = 1; pageSize = 10;
  get pagedUsers(): User[] { return this.users.slice((this.page-1)*this.pageSize, this.page*this.pageSize); }
  get totalPages(): number { return Math.ceil(this.users.length / this.pageSize); }
  get pages(): number[] { return Array.from({length: this.totalPages}, (_, i) => i + 1); }
  
  // User form
  showUserModal: boolean = false;
  editingUser: User | null = null;
  userForm = {
    username: '',
    email: '',
    fullName: '',
    mobileNo: '',
    address: '',
    role: 'CLIENT'
  };
  
  // Settings form
  settingsForm: Record<string, string> = {};
  
  // Filter for logs
  logFilterUsername: string = '';
  logFilterDays: number = 7;

  supportTickets: any[] = []; supportLoading = false;
  clientUsers: any[] = [];
  selectedLeakageUserId = '';
  leakageRows: any[] = []; leakageTotalWaste = 0; leakagesLoading = false;

  constructor(
    private adminService: AdminService,
    private authService: AuthService,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.loadDashboard();
    this.loadUsers();
    this.loadAuditLogs();
    this.loadSettings();
    this.loadStats();
  }

  setActiveTab(tab: string): void {
    this.activeTab = tab;
    if (tab === 'users') this.loadUsers();
    if (tab === 'audit-logs') this.loadAuditLogs();
    if (tab === 'settings') this.loadSettings();
    if (tab === 'stats') this.loadStats();
    if (tab === 'support') this.loadSupportTickets();
    if (tab === 'leakages') this.loadClientUsers();
  }

  loadDashboard(): void {
    this.loading = true;
    this.adminService.getDashboard().subscribe({
      next: (data) => {
        this.dashboardData = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading dashboard', err);
        this.loading = false;
      }
    });
  }

  loadUsers(): void {
    this.loading = true;
    if (this.searchTerm) {
      this.adminService.searchUsers(this.searchTerm).subscribe({
        next: (data) => {
          this.users = data;
          this.loading = false;
        },
        error: (err) => {
          console.error('Error loading users', err);
          this.loading = false;
        }
      });
    } else {
      this.adminService.getUsers().subscribe({
        next: (data) => {
          this.users = data;
          this.loading = false;
        },
        error: (err) => {
          console.error('Error loading users', err);
          this.loading = false;
        }
      });
    }
  }

  searchUsers(): void {
    this.loadUsers();
  }

  loadAuditLogs(): void {
    this.loading = true;
    this.adminService.getAuditLogs(this.logFilterUsername || undefined, this.logFilterDays).subscribe({
      next: (data) => {
        this.auditLogs = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading audit logs', err);
        this.loading = false;
      }
    });
  }

  loadSettings(): void {
    this.adminService.getSettings().subscribe({
      next: (data) => {
        this.settings = data;
        this.settingsForm = { ...data };
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading settings', err);
        this.loading = false;
      }
    });
  }

  loadStats(): void {
    this.adminService.getStats().subscribe({
      next: (data) => {
        this.stats = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading stats', err);
        this.loading = false;
      }
    });
  }

  openCreateUserModal(): void {
    this.editingUser = null;
    this.userForm = {
      username: '',
      email: '',
      fullName: '',
      mobileNo: '',
      address: '',
      role: 'CLIENT'
    };
    this.showUserModal = true;
  }

  editUser(user: User): void {
    this.editingUser = user;
    this.userForm = {
      username: user.username,
      email: user.email,
      fullName: user.fullName,
      mobileNo: user.mobileNo,
      address: '',
      role: user.role
    };
    this.showUserModal = true;
  }

  saveUser(): void {
    if (this.editingUser) {
      this.adminService.updateUser(this.editingUser.id, {
        fullName: this.userForm.fullName,
        mobileNo: this.userForm.mobileNo,
        address: this.userForm.address,
        role: this.userForm.role
      }).subscribe({
        next: () => { this.loadUsers(); this.showUserModal = false; this.showSuccess('User updated successfully.'); },
        error: (err) => console.error('Error updating user', err)
      });
    } else {
      this.adminService.createUser(this.userForm).subscribe({
        next: () => { this.loadUsers(); this.showUserModal = false; this.showSuccess('User created successfully.'); },
        error: (err) => console.error('Error creating user', err)
      });
    }
  }

  toggleUserStatus(user: User): void {
    if (user.status === 'LOCKED') {
      if (!confirm(`Unlock account for "${user.username}"?`)) return;
      this.adminService.unlockUser(user.id).subscribe({
        next: () => { this.loadUsers(); this.showSuccess(`Account unlocked for "${user.username}".`); },
        error: (err) => console.error('Error unlocking user', err)
      });
      return;
    }
    const action = user.status === 'ACTIVE' ? 'deactivate' : 'activate';
    if (!confirm(`Are you sure you want to ${action} user "${user.username}"?`)) return;
    const req = user.status === 'ACTIVE'
      ? this.adminService.deactivateUser(user.id)
      : this.adminService.activateUser(user.id);
    req.subscribe({
      next: () => { this.loadUsers(); this.showSuccess(`User "${user.username}" ${action}d successfully.`); },
      error: (err) => console.error('Error toggling user status', err)
    });
  }

  resetUserPassword(user: User): void {
    if (!confirm(`Reset password for "${user.username}"? A new password will be generated.`)) return;
    this.adminService.resetPassword(user.id).subscribe({
      next: (response) => {
        this.showSuccess(`Password reset for "${user.username}". New password: ${response.newPassword}`);
      },
      error: (err) => console.error('Error resetting password', err)
    });
  }

  saveSettings(): void {
    this.adminService.updateSettings(this.settingsForm).subscribe({
      next: () => { this.showSuccess('Settings saved successfully.'); this.loadSettings(); },
      error: (err) => console.error('Error saving settings', err)
    });
  }

  showSuccess(msg: string): void {
    this.successMsg = msg;
    setTimeout(() => this.successMsg = '', 5000);
  }

  loadClientUsers(): void {
    this.adminService.getUsers().subscribe({
      next: (users) => {
        this.clientUsers = users.filter((u: any) => u.role === 'CLIENT' || u.role === 'ROLE_CLIENT');
        this.leakageRows = []; this.selectedLeakageUserId = '';
      },
      error: () => {}
    });
  }

  loadLeakages(): void {
    if (!this.selectedLeakageUserId) return;
    this.leakagesLoading = true; this.leakageRows = [];
    this.http.get<any[]>(`http://localhost:8080/api/dashboard/leakages/${this.selectedLeakageUserId}`).subscribe({
      next: (d) => {
        this.leakageRows = d;
        this.leakageTotalWaste = d.reduce((s, r) => s + (r.estimatedWastedCost || 0), 0);
        this.leakagesLoading = false;
      },
      error: () => { this.leakagesLoading = false; }
    });
  }

  loadSupportTickets(): void {
    this.supportLoading = true;
    this.http.get<any[]>('http://localhost:8080/api/support/admin-tickets').subscribe({
      next: (d) => { this.supportTickets = d; this.supportLoading = false; },
      error: () => { this.supportLoading = false; }
    });
  }

  updateSupportStatus(ticket: any): void {
    this.http.put(`http://localhost:8080/api/support/ticket/${ticket.id}/status`, { status: ticket.status }).subscribe();
  }

  private readonly DEFAULT_USERS = ['admin', 'engineer', 'client'];

  isDefaultUser(user: User | null): boolean {
    return user !== null && this.DEFAULT_USERS.includes(user.username);
  }

  getRoleBadgeClass(role: string): string {
    switch(role) {
      case 'ADMIN': return 'badge-danger';
      case 'ENGINEER': return 'badge-warning';
      default: return 'badge-info';
    }
  }

  getStatusBadgeClass(status: string): string {
    switch(status) {
      case 'ACTIVE': return 'badge-success';
      case 'INACTIVE': return 'badge-secondary';
      case 'LOCKED': return 'badge-danger';
      default: return 'badge-secondary';
    }
  }

  getActionBadgeClass(action: string): string {
    if (action.includes('LOGIN')) return 'badge-info';
    if (action.includes('CREATE')) return 'badge-success';
    if (action.includes('UPDATE') || action.includes('CONFIG')) return 'badge-warning';
    if (action.includes('DELETE') || action.includes('DEACTIVATE')) return 'badge-danger';
    return 'badge-secondary';
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login'], { state: { loggedOut: true } });
  }
}