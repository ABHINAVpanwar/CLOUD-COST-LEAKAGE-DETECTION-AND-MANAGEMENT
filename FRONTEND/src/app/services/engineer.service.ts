import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class EngineerService {
  private api = 'http://localhost:8080/api/engineer';

  constructor(private http: HttpClient) {}

  getClients(): Observable<any[]> {
    return this.http.get<any[]>(`${this.api}/clients`);
  }

  getClientAnalysis(clientId: number): Observable<any> {
    return this.http.get<any>(`${this.api}/clients/${clientId}/analysis`);
  }

  bulkAnalysis(): Observable<any[]> {
    return this.http.get<any[]>(`${this.api}/bulk-analysis`);
  }

  getConfig(clientId: number): Observable<any> {
    return this.http.get<any>(`${this.api}/clients/${clientId}/config`);
  }

  saveConfig(clientId: number, config: any): Observable<any> {
    return this.http.put<any>(`${this.api}/clients/${clientId}/config`, config);
  }

  getSavings(clientId: number): Observable<any> {
    return this.http.get<any>(`${this.api}/clients/${clientId}/savings`);
  }
}
