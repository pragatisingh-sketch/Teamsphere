import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { TimeEntryHierarchicalSummary, TimeSummaryFilters } from '../model/time-summary.interface';
import { environment } from '../../environments/environment';
import { Project } from '../model/project.interface';
import { User } from '../model/user.interface';

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
export class TimeSummaryService {
  private baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  getHierarchicalSummary(filters: TimeSummaryFilters): Observable<TimeEntryHierarchicalSummary[]> {
    let params = new HttpParams();

    if (filters.userId) {
      params = params.set('userId', filters.userId.toString());
    }
    if (filters.projectId) {
      params = params.set('projectId', filters.projectId.toString());
    }
    if (filters.startDate) {
      const year = filters.startDate.getFullYear();
      const month = String(filters.startDate.getMonth() + 1).padStart(2, '0');
      const day = String(filters.startDate.getDate()).padStart(2, '0');
      params = params.set('startDate', `${year}-${month}-${day}`);
    }
    if (filters.endDate) {
      const year = filters.endDate.getFullYear();
      const month = String(filters.endDate.getMonth() + 1).padStart(2, '0');
      const day = String(filters.endDate.getDate()).padStart(2, '0');
      params = params.set('endDate', `${year}-${month}-${day}`);
    }
    console.log(params);
    return this.http.get<BaseResponse<TimeEntryHierarchicalSummary[]>>(`${this.baseUrl}/api/time-entries/hierarchical-summary`, { params }).pipe(
      map(response => response.data)
    );
  }

  getAllProjects(): Observable<Project[]> {
    return this.http.get<BaseResponse<Project[]>>(`${this.baseUrl}/api/projects`).pipe(
      map(response => response.data)
    );
  }

  getAllUsers(): Observable<User[]> {
    return this.http.get<BaseResponse<User[]>>(`${this.baseUrl}/api/time-entries/ldaps`).pipe(
      map(response => response.data)
    );
  }

}