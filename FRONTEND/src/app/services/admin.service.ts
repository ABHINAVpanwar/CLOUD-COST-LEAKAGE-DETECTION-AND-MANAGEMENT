import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface User {
  id: number;
  username: string;
  email: string;
  fullName: string;
  mobileNo: string;
  role: string;
  status: string;
  accountLocked: boolean;
  createdAt: string;
  lastLogin: string;
}

export interface AuditLog {
  id: number;
  userId: number;
  username: string;
  action: string;
  entityType: string;
  entityId: string;
  details: string;
  ipAddress: string;
  createdAt: string;
}

export interface AdminDashboardData {
  totalUsers: number;
  totalClients: number;
  totalEngineers: number;
  activeUsers: number;
  inactiveUsers: number;
  lockedUsers: number;
  totalAnalyses: number;
  totalUploads: number;
  totalEstimatedSavings: number;
  recentUsers: User[];
  recentAuditLogs: AuditLog[];
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = 'http://localhost:8080/api/admin';

  constructor(private http: HttpClient) {}

  getDashboard(): Observable<AdminDashboardData> {
    return this.http.get<AdminDashboardData>(`${this.apiUrl}/dashboard`);
  }

  getUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/users`);
  }

  searchUsers(query: string): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/users/search?q=${query}`);
  }

  getUser(id: number): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/users/${id}`);
  }

  createUser(user: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/users`, user);
  }

  updateUser(id: number, user: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/users/${id}`, user);
  }

  deactivateUser(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/users/${id}`);
  }

  activateUser(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/users/${id}/activate`, {});
  }

  unlockUser(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/users/${id}/unlock`, {});
  }

  resetPassword(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/users/${id}/reset-password`, {});
  }

  getAuditLogs(username?: string, days?: number): Observable<AuditLog[]> {
    let url = `${this.apiUrl}/audit-logs`;
    const params: string[] = [];
    if (username) params.push(`username=${username}`);
    if (days) params.push(`days=${days}`);
    if (params.length) url += '?' + params.join('&');
    return this.http.get<AuditLog[]>(url);
  }

  getSettings(): Observable<Record<string, string>> {
    return this.http.get<Record<string, string>>(`${this.apiUrl}/settings`);
  }

  updateSettings(settings: Record<string, string>): Observable<any> {
    return this.http.put(`${this.apiUrl}/settings`, settings);
  }

  getStats(): Observable<any> {
    return this.http.get(`${this.apiUrl}/stats`);
  }
}