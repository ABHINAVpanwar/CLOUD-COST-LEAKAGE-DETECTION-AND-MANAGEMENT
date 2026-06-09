import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class BudgetService {
  private api = 'http://localhost:8080/api/budget';

  constructor(private http: HttpClient) {}

  getBudget(): Observable<any> {
    return this.http.get<any>(this.api);
  }

  setBudget(data: any): Observable<any> {
    return this.http.post<any>(this.api, data);
  }
}
