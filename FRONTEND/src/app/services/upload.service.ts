import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class UploadService {
  private apiUrl = 'http://localhost:8080/api/upload';

  constructor(private http: HttpClient) {}

  uploadFile(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post(`${this.apiUrl}/csv`, formData);
  }

  addManualEntry(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/manual`, data);
  }
}
