import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from 'src/environments/environment';

// Interface for the backend BaseResponse structure
interface BaseResponse<T> {
  status: string;
  code: number;
  message: string;
  data: T;
}

@Injectable({
  providedIn: 'root'
})
export class EmployeeService {

  // Define the API endpoint for fetching employees
  private apiUrl = `${environment.apiUrl}/api/employees`;

  constructor(private http: HttpClient) { }

  // Method to fetch employees
  getEmployees(): Observable<{ data: any[], message: string }> {
    return this.http.get<BaseResponse<any[]>>(this.apiUrl).pipe(
      map(response => ({
        data: response.data,
        message: response.message
      }))
    );
  }

  getEmployeeSummary(): Observable<{ data: any[], message: string }> {
    return this.http.get<BaseResponse<any[]>>(`${this.apiUrl}/summary`).pipe(
      map(response => ({
        data: response.data,
        message: response.message
      }))
    );
  }

  getEmployeeById(userId: string): Observable<{ data: any, message: string }> {
    return this.http.get<BaseResponse<any>>(`${this.apiUrl}/${userId}`).pipe(
      map(response => ({
        data: response.data,
        message: response.message
      }))
    );
  }
}
