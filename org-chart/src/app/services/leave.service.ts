import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { BehaviorSubject, catchError, map, Observable, tap, throwError } from 'rxjs';
import { VunnoMgmtDto } from '../leave-application/leave-application.component';
import { environment } from 'src/environments/environment';
import { User } from '../model/user';
import { BaseResponse } from '../model/base-response.model';

@Injectable({
  providedIn: 'root'
})

export class LeaveService {
  private apiUrl = `${environment.apiUrl}/api/vunno`; // Base API URL

  private managerDetailsSubject = new BehaviorSubject<any>(null);
  managerDetails$ = this.managerDetailsSubject.asObservable();

  constructor(private http: HttpClient) { }

  // Existing method to get leave details
  getLeaveDetails(ldap: string): Observable<number[]> {
    const params = new HttpParams().set('ldap', ldap);
    return this.http.get<number[]>(`${this.apiUrl}/getLeaveDetails`, { params });
  }

  // New method to fetch leave history for a specific requestor
  getUserLeaveHistory(requestorLdap: string): Observable<any[]> {
    const params = new HttpParams().set('ldap', requestorLdap);
    return this.http.get<any[]>(`${this.apiUrl}/getHistoryForUser`, { params });
  }

  // Method to fetch the approving manager automatically for a specific ldap.
  // leave.service.ts
  getManagerByLdap(ldap: string): Observable<VunnoMgmtDto[]> {
    return this.http.get<VunnoMgmtDto[]>(
      `${this.apiUrl}/lead-manager/${ldap}`
    ).pipe(
      tap(data => this.setManagerDetails(data)), // Auto-update the BehaviorSubject
      catchError(error => {
        console.error('Error fetching manager:', error);
        return throwError(() => new Error('Failed to fetch manager details'));
      })
    );
  }

  // Call this after API fetch
  setManagerDetails(data: any): void {
    this.managerDetailsSubject.next(data);
  }

  // Optional getter (if not using .subscribe in the component)
  getManagerDetailsSnapshot(): any {
    return this.managerDetailsSubject.value;
  }

  // leave.service.ts
  submitLeaveRequest(data: FormData): Observable<any> {
    return this.http.post<any>(
      `${this.apiUrl}/requestedVunno`,
      data,
      {
        headers: {
          // DO NOT set Content-Type manually; let the browser set it for FormData
        }
      }
    ).pipe(
      catchError(error => {
        console.error('API Error:', error);
        return throwError(() => new Error('Request failed. Please try again.'));
      })
    );
  }


  // leave.service.ts
  getCurrentUserLdap(): Observable<BaseResponse<User>> {
    return this.http.get<BaseResponse<User>>(`${environment.apiUrl}/api/projects/current-user`);
  }

  uploadLeaveBalance(formData: FormData, force: boolean = false): Observable<any> {
    return this.http.post(`${this.apiUrl}/upload-leave-balance?force=${force}`, formData);
  }

  approveRequest(payload: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/approve`, payload, {
      headers: new HttpHeaders({
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      })
    });
  }

  rejectRequest(payload: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/reject`, payload, {
      headers: new HttpHeaders({
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      })
    });
  }

  revokeRequest(payload: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/revoke`, payload, {
      headers: new HttpHeaders({
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      })
    });
  }

  deleteLeaveRequestById(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/deleteLeaveRequest/${id}`);
  }

  updateLeaveCategory(payload: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/category-update`, payload);
  }

  bulkUpdateCategory(payload: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/bulk/category-update`, payload)
  }

  updateLeaveRequest(request: any): Observable<any> {
    const formData = new FormData();

    // Append the JSON part (VunnoRequestDto as 'request')
    const requestBlob = new Blob(
      [JSON.stringify({
        id: request.id,
        ldap: request.ldap,
        approvingLead: request.approvingLead,
        applicationType: request.applicationType,
        lvWfhDuration: request.duration,
        leaveType: request.leaveType,
        startDate: request.startDate,
        endDate: request.endDate,
        startTime: request.startTime,
        endTime: request.endTime,
        reason: request.reason,
        timesheetProof: request.timesheetProof,
        oooProof: request.oooProof,
        backupInfo: request.backupInfo,
        status: request.status,
        documentPath: typeof request.documentPath === 'string' ? request.documentPath : undefined // handle both existing & new
      })],
      { type: 'application/json' }
    );

    formData.append('request', requestBlob);

    // Append the document if it's a new file
    if (request.documentPath instanceof File) {
      formData.append('document', request.documentPath);
    }

    // Use PUT with path variable (id)
    return this.http.put(`${this.apiUrl}/updateLeaveRequest/${request.id}`, formData);
  }

  getPendingRequestsForLead(startDate?: string, endDate?: string, directOnly: boolean = true): Observable<any[]> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    params = params.set('directOnly', String(directOnly)); // pass toggle

    return this.http.get<any>(`${this.apiUrl}/requests-for-approval`, { params })
      .pipe(map(res => res.data));
  }

  getProcessedRequestsForLead(startDate?: string, endDate?: string, directOnly: boolean = true): Observable<any[]> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    params = params.set('directOnly', String(directOnly));

    return this.http.get<any>(`${this.apiUrl}/processed-requests-for-approval`, { params })
      .pipe(map(res => res.data));
  }

  approveRequestVunno(request: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/approve`, request);
  }

  getApprovedRequestsForLead(): Observable<any[]> {
    return this.http.get<any>(`${this.apiUrl}/approved-requests`).pipe(
      map(res => res.data));
  }

  /**
   * Get audit history for a specific leave request
   */
  getAuditHistory(vunnoResponseId: number): Observable<BaseResponse<any[]>> {
    return this.http.get<BaseResponse<any[]>>(`${this.apiUrl}/audit-history/${vunnoResponseId}`);
  }
}
