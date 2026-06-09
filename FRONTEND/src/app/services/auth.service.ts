import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth';
  private tokenKey = 'auth_token';
  private userRoleKey = 'user_role';
  private usernameKey = 'username';
  
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasToken());
  private userRoleSubject = new BehaviorSubject<string>(this.getStoredRole());

  constructor(private http: HttpClient) {}

  login(username: string, password: string): Observable<any> {
    // Clear any stale session data before logging in
    this.removeToken();
    this.removeUserRole();
    this.removeUsername();
    const credentials = { username: username, password: password };
    return this.http.post(`${this.apiUrl}/login`, credentials).pipe(
      tap((response: any) => {
        if (response.token) {
          this.setToken(response.token);
          this.setUsername(response.username);
          // Backend returns "ROLE_ADMIN" — strip the prefix for frontend use
          const rawRole: string = response.role || 'CLIENT';
          const role = rawRole.startsWith('ROLE_') ? rawRole.substring(5) : rawRole;
          this.setUserRole(role);
          this.isAuthenticatedSubject.next(true);
          this.userRoleSubject.next(role);
        }
      })
    );
  }

  register(userData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, userData);
  }

  logout(): void {
    this.removeToken();
    this.removeUserRole();
    this.removeUsername();
    this.isAuthenticatedSubject.next(false);
    this.userRoleSubject.next('');
  }

  isAuthenticated(): boolean {
    return this.hasToken();
  }

  getAuthStatus(): Observable<boolean> {
    return this.isAuthenticatedSubject.asObservable();
  }

  getUserRole(): string {
    return this.getStoredRole();
  }

  getUserRoleObservable(): Observable<string> {
    return this.userRoleSubject.asObservable();
  }

  isAdmin(): boolean {
    return this.getStoredRole() === 'ADMIN';
  }

  isEngineer(): boolean {
    return this.getStoredRole() === 'ENGINEER';
  }

  isClient(): boolean {
    return this.getStoredRole() === 'CLIENT';
  }

  getUsername(): string {
    return localStorage.getItem(this.usernameKey) || '';
  }

  private setToken(token: string): void {
    localStorage.setItem(this.tokenKey, token);
  }

  private getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  private removeToken(): void {
    localStorage.removeItem(this.tokenKey);
  }

  private hasToken(): boolean {
    return !!this.getToken();
  }

  private setUserRole(role: string): void {
    localStorage.setItem(this.userRoleKey, role);
  }

  private getStoredRole(): string {
    return localStorage.getItem(this.userRoleKey) || 'CLIENT';
  }

  private removeUserRole(): void {
    localStorage.removeItem(this.userRoleKey);
  }

  private setUsername(username: string): void {
    localStorage.setItem(this.usernameKey, username);
  }

  private removeUsername(): void {
    localStorage.removeItem(this.usernameKey);
  }
}
