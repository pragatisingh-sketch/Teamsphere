import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { environment } from 'src/environments/environment';
import { catchError, map, Observable, of, throwError } from 'rxjs';
import { AttendanceRequest } from 'src/app/model/attendance-request.model';
import { CheckInStatusResponse } from '../model/check-in-status.model';


@Injectable({
  providedIn: 'root'
})

export class AttendanceService {

  constructor(private http: HttpClient) { }

  private apiUrl = `${environment.apiUrl}/api/atom`; // Base API URL

  // attendance.service.ts - Updated to use secure endpoint
  getCheckInStatus(ldap?: string): Observable<CheckInStatusResponse> {
    // Use the secure endpoint that doesn't require LDAP parameter
    // The backend will use the authenticated user's LDAP automatically
    return this.http.get<CheckInStatusResponse>(`${this.apiUrl}/checkingIn`).pipe(
      map(response => ({
        status: response.status || '',
        checkedIn: response.checkedIn || false,
        checkInTime: response.checkInTime || '',
        isLate: response.isLate || false,
        checkedOutStatus: response.checkedOutStatus || '',
        checkedOut: response.checkedOut || false,
        checkOutTime: response.checkOutTime || null,
        earlyOrLateCheckOut: response.earlyOrLateCheckOut || '',
        checkinDateTime: response.checkinDateTime || null,
        checkOutDateTime: response.checkOutDateTime || null
      })),
      catchError(() => of({
        status: 'Error checking status',
        checkedIn: false,
        checkInTime: '',
        isLate: false,
        checkedOutStatus: '',
        checkedOut: false,
        checkOutTime: null,
        earlyOrLateCheckOut: '',
        checkinDateTime: null,
        checkOutDateTime: null
      }))
    );
  }


  markAttendance(payload: AttendanceRequest): Observable<any> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
    });

    return this.http.post<any>(
      `${this.apiUrl}/mark`,
      payload,
      {
        headers
      }
    ).pipe(
      catchError(error => {
        console.error('API Error:', error);
        return throwError(() => new Error(
          error.error?.message ||
          error.message ||
          'Failed to mark attendance'
        ));
      })
    );
  }

  getAttendanceByLdap(ldap: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/records/${ldap}`).pipe(
      catchError(error => {
        console.error('API Error:', error);
        return throwError(() => new Error('Failed to fetch attendance records'));
      })
    );
  }

  getShiftDetails(shiftCode: string): Observable<{
    code: string;
    startTime: string;
    endTime: string;
    maxLoginTime: string;
  }> {
    return this.http.get<any>(`${this.apiUrl}/shift-details/${shiftCode}`).pipe(
      map(shift => ({
        code: shift.code,
        startTime: shift.startTime,
        endTime: shift.endTime,
        maxLoginTime: shift.maxLoginTime
      }))
    );
  }

  checkoutAttendance(payload: AttendanceRequest): Observable<any> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
    });

    return this.http.post<any>(
      `${this.apiUrl}/checkout`,
      payload,
      {
        headers
      }
    ).pipe(
      catchError(error => {
        console.error('Checkout API Error:', error);
        return throwError(() => new Error(
          error.error?.message ||
          error.message ||
          'Failed to checkout attendance'
        ));
      })
    );
  }

  /**
   * Update attendance status
   */
  updateAttendanceStatus(attendanceId: number, newStatus: string, reason: string): Observable<any> {
    const payload = {
      newStatus: newStatus,
      reason: reason
    };

    return this.http.put<any>(
      `${this.apiUrl}/update-status/${attendanceId}`,
      payload
    ).pipe(
      catchError(error => {
        console.error('Update status API Error:', error);
        return throwError(() => new Error(
          error.error?.message ||
          error.message ||
          'Failed to update attendance status'
        ));
      })
    );
  }

  /**
   * Update compliance status
   */
  updateComplianceStatus(attendanceId: number, isDefaulter: boolean, reason: string): Observable<any> {
    const payload = {
      isDefaulter: isDefaulter,
      reason: reason
    };

    return this.http.put<any>(
      `${this.apiUrl}/update-compliance/${attendanceId}`,
      payload
    ).pipe(
      catchError(error => {
        console.error('Update compliance API Error:', error);
        return throwError(() => new Error(
          error.error?.message ||
          error.message ||
          'Failed to update compliance status'
        ));
      })
    );
  }

  /**
   * Get audit history for an attendance record
   */
  getAuditHistory(attendanceId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/audit-history/${attendanceId}`).pipe(
      catchError(error => {
        console.error('Audit history API Error:', error);
        return throwError(() => new Error(
          error.error?.message ||
          error.message ||
          'Failed to fetch audit history'
        ));
      })
    );
  }
}
