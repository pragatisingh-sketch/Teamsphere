import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { User } from '../app/model/user'
import { environment } from 'src/environments/environment';

interface EmployeeRelation {
  id: number;
  employee: { ldap: string };
  relationType: { id: number; name: string };
  relationValue: string;
  isPrimary: boolean;
  effectiveDate: string;
  endDate: string;
  isActive: boolean;
}

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
export class UserService {


  isAdmin: boolean | undefined;

  private baseUrl = `${environment.apiUrl}/api/employees`;


  constructor(private http: HttpClient) { }

  getUsers(): Observable<{ data: User[], message: string }> {
    return this.http.get<BaseResponse<User[]>>(this.baseUrl).pipe(
      map(response => ({
        data: response.data,
        message: response.message
      }))
    );
  }

  getAllUsers(): Observable<{ data: User[], message: string }> {
    return this.http.get<BaseResponse<User[]>>(`${this.baseUrl}/getAll`).pipe(
      map(response => ({
        data: response.data,
        message: response.message
      }))
    );
  }

  getUserById(userId: string, isInactive: boolean): Observable<{ data: any, message: string }> {
    return this.http.get<BaseResponse<any>>(`${this.baseUrl}/${userId}?isInactive=${isInactive}`).pipe(
      map(response => ({
        data: response.data,
        message: response.message
      }))
    );
  }

  addUser(formData: FormData): Observable<{ data: User, message: string }> {
    console.log(formData);
    return this.http.post<BaseResponse<User>>(this.baseUrl, formData).pipe(
      map(response => ({
        data: response.data,
        message: response.message
      }))
    );
  }

  async updateUser(formData: FormData): Promise<Observable<{ data: User, message: string }>> {
    const userBlob = formData.get('employee');
    let user: User;

    if (userBlob instanceof Blob) {
      user = JSON.parse(await userBlob.text());
    } else {
      throw new Error('Invalid employee data in FormData');
    }

    return this.http.put<BaseResponse<User>>(`${this.baseUrl}/${user.id}`, formData).pipe(
      map(response => ({
        data: response.data,
        message: response.message
      }))
    );
  }

  deleteUser(id: string): Observable<{ message: string }> {
    return this.http.delete<BaseResponse<string>>(`${this.baseUrl}/${id}`).pipe(
      map(response => ({
        message: response.message
      }))
    );
  }

  addCSV(users: User[]): Observable<{ data: User[], message: string }> {
    const url = `${this.baseUrl}/add/csv`; // Adjust the endpoint as needed
    return this.http.post<BaseResponse<User[]>>(url, users).pipe(
      map(response => ({
        data: response.data,
        message: response.message
      }))
    );
  }

  deleteUsers(userIds: number[]): Observable<{ message: string }> {
    return this.http.delete<BaseResponse<string>>(`${this.baseUrl}/delete`, {
      body: userIds,
    }).pipe(
      map(response => ({
        message: response.message
      }))
    );
  }

  /**
   * Get all leads requests with optional filters
   * @param filters Optional filters for status, startDate, and endDate
   * @returns Observable of leads requests
   */
  getAllLeadsRequests(filters?: { status?: string, startDate?: string, endDate?: string }): Observable<{ data: any[], message: string }> {
    let url = `${this.baseUrl}/getAllLeadsRequest`;
    let params = new HttpParams();

    if (filters) {
      if (filters.status) {
        params = params.set('status', filters.status);
      }
      if (filters.startDate) {
        params = params.set('startDate', filters.startDate);
      }
      if (filters.endDate) {
        params = params.set('endDate', filters.endDate);
      }
    }

    return this.http.get<BaseResponse<any[]>>(url, { params }).pipe(
      map(response => ({
        data: response.data,
        message: response.message
      }))
    );
  }

  processLeadsRequest(requestId: number, action: 'APPROVE' | 'REJECT'): Observable<{ message: string }> {
    const url = `${this.baseUrl}/approve-reject`;
    const body = new HttpParams()
      .set('requestId', requestId.toString())
      .set('action', action);

    return this.http.post<BaseResponse<string>>(url, body.toString(), {
      headers: new HttpHeaders({ 'Content-Type': 'application/x-www-form-urlencoded' })
    }).pipe(
      map(response => ({
        message: response.message
      }))
    );
  }

  /**
   * Get employee data for a specific leads request
   * @param requestId The ID of the leads request
   * @returns Observable of employee data JSON string
   */
  getEmployeeDataForRequest(requestId: number): Observable<{ data: string, message: string }> {
    const url = `${this.baseUrl}/leads-request/${requestId}/employee-data`;
    return this.http.get<BaseResponse<string>>(url).pipe(
      map(response => ({
        data: response.data,
        message: response.message
      }))
    );
  }

  /**
   * Get employee data with original data for comparison
   * @param requestId The ID of the leads request
   * @returns Observable with both current and original employee data
   */
  getEmployeeDataWithOriginal(requestId: number): Observable<{ employeeData: string, originalData: string | null, requestType: string }> {
    const url = `${this.baseUrl}/leads-request/${requestId}/employee-data-with-original`;
    return this.http.get<BaseResponse<{ employeeData: string, originalData: string | null, requestType: string }>>(url).pipe(
      map(response => ({
        employeeData: response.data.employeeData,
        originalData: response.data.originalData,
        requestType: response.data.requestType
      }))
    );
  }

  getChartData(): Observable<{ data: any, message: string }> {
    return this.http.get<BaseResponse<any>>(`${this.baseUrl}/chart-data`).pipe(
      map(response => ({
        data: response.data,
        message: response.message
      }))
    );
  }

  getLeadsOnly(): Observable<{ data: any[], message: string }> {
    return this.http.get<BaseResponse<any[]>>(`${this.baseUrl}/getLeadsOnly`).pipe(
      map(response => ({
        data: response.data,
        message: response.message
      }))
    );
  }

  getManagersOnly(): Observable<{ data: any[], message: string }> {
    return this.http.get<BaseResponse<any[]>>(`${this.baseUrl}/getManagersOnly`).pipe(
      map(response => ({
        data: response.data,
        message: response.message
      }))
    );
  }

  changeUserRole(ldap: string, newRole: string): Observable<{ message: string }> {
    const url = `${this.baseUrl}/change-role`;
    const body = { ldap, newRole };
    return this.http.post<BaseResponse<string>>(url, body).pipe(
      map(response => ({
        message: response.message
      }))
    );
  }

  /**
   * Reset a user's password to the default value
   * @param ldap The LDAP of the user to reset password for
   * @returns Observable of the response
   */
  resetUserPassword(ldap: string): Observable<string> {
    const url = `${environment.apiUrl}/admin/reset-password-postman`;
    const body = { ldap, newPassword: 'vbsllp' };

    // Add special headers for this request
    const headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });

    console.log('Resetting password for user:', ldap);

    return this.http.post<string>(url, body, {
      headers: headers,
      responseType: 'text' as 'json'
    });
  }

  /**
   * Get edit logs for a specific user
   * @param ldap The LDAP of the user to get logs for
   * @returns Observable of edit logs
   */
  getUserEditLogs(ldap: string): Observable<{ data: any[], message: string }> {
    return this.http.get<BaseResponse<any[]>>(`${this.baseUrl}/edit-logs/${ldap}`).pipe(
      map(response => ({
        data: response.data,
        message: response.message
      }))
    );
  }

  /**
   * Export users with their edit logs as CSV
   * @returns Observable of CSV file as blob
   */
  exportUsersWithLogs(): Observable<{ data: Blob, message: string }> {
    return this.http.get(`${this.baseUrl}/export-with-logs`, {
      responseType: 'blob'
    }).pipe(
      map(blob => ({
        data: blob as Blob,
        message: 'Users with logs exported successfully'
      }))
    );
  }

  /**
   * Get request counts by status for the current user
   * @returns Observable of request counts
   */
  getRequestCounts(): Observable<{ data: any, message: string }> {
    return this.http.get<BaseResponse<any>>(`${this.baseUrl}/request-counts`).pipe(
      map(response => ({
        data: response.data,
        message: response.message
      }))
    );
  }

  getAllEmployeeRelations(): Observable<EmployeeRelation[]> {
    const url = `${environment.apiUrl}/api/employee-relations`;
    return this.http.get<BaseResponse<EmployeeRelation[]>>(url).pipe(
      map(response => response.data || [])
    );
  }

  getLeadsOptions(): Observable<string[]> {
    return this.getAllEmployeeRelations().pipe(
      map(relations => relations
        .filter(r => r.relationType.id === 1 && r.isActive)
        .map(r => r.relationValue.split('_')[1]) // Extract name from 'Lead_Name'
        .filter(name => name && name.trim())
      ),
      map(names => [...new Set(names)].sort()) // Unique and sorted
    );
  }

  getManagersOptions(): Observable<string[]> {
    return this.getAllEmployeeRelations().pipe(
      map(relations => relations
        .filter(r => r.relationType.id === 2 && r.isActive)
        .map(r => r.relationValue.split('_')[1]) // Extract name from 'Manager_Name'
        .filter(name => name && name.trim())
      ),
      map(names => [...new Set(names)].sort()) // Unique and sorted
    );
  }

  delegateRole(delegationRequest: any): Observable<{ message: string }> {
    const url = `${environment.apiUrl}/api/delegation/delegate`;
    return this.http.post<BaseResponse<string>>(url, delegationRequest).pipe(
      map(response => ({
        message: response.message
      }))
    );
  }

  revertDelegation(userLdap: string): Observable<{ message: string }> {
    const url = `${environment.apiUrl}/api/delegation/revert`;
    return this.http.post<BaseResponse<string>>(url, { userLdap }).pipe(
      map(response => ({
        message: response.message
      }))
    );
  }

  getDelegationHistory(ldap: string): Observable<{ data: any[], message: string }> {
    const url = `${environment.apiUrl}/api/delegation/history/${ldap}`;
    return this.http.get<BaseResponse<any[]>>(url).pipe(
      map(response => ({
        data: response.data,
        message: response.message
      }))
    );
  }
}
