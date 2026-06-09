import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private apiUrl = 'http://localhost:8080/api/dashboard';

  constructor(private http: HttpClient) {}

  getDashboardData(): Observable<any> {
    return this.http.get(`${this.apiUrl}/data`);
  }

  getUtilization(): Observable<any> {
    return this.http.get(`${this.apiUrl}/utilization`);
  }

  getRecommendations(): Observable<any> {
    return this.http.get(`${this.apiUrl}/recommendations`);
  }

  getLeakages(): Observable<any> {
    return this.http.get(`${this.apiUrl}/leakages`);
  }

  getHistory(): Observable<any> {
    return this.http.get(`${this.apiUrl}/history`);
  }
}
