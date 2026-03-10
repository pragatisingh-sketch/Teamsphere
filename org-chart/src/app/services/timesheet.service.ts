import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { delay } from 'rxjs/operators';

export interface Attendance {
  id: string;
  ldap: string;
  masked_orgid: string;
  subrole: string;
  role: string;
  date: string;
  process: string;
  billingCode: string;
  activity: string;
  status: string;
  lead_ldap: string;
  vendor: string;
  minutes: string;
  project: string;
  team: string;
  comment: string;
}

@Injectable({
  providedIn: 'root'
})
export class TimeSheetService {
  private apiUrl = `${environment.apiUrl}/api/timesheet`;
  
  constructor(private http: HttpClient) { }

  // Get all attendance records
  getAttendanceRecords(): Observable<Attendance[]> {
    return this.http.get<Attendance[]>(`${this.apiUrl}/records`)
      .pipe(
        tap(_ => console.log('Fetched attendance records')),
        catchError(this.handleError<Attendance[]>('getAttendanceRecords', []))
      );
  }

  // Get a single attendance record by ID
  getAttendanceRecord(id: string): Observable<Attendance> {
    return this.http.get<Attendance>(`${this.apiUrl}/records/${id}`)
      .pipe(
        tap(_ => console.log(`Fetched attendance record id=${id}`)),
        catchError(this.handleError<Attendance>('getAttendanceRecord'))
      );
  }

  // Add a new attendance record
  addAttendanceRecord(record: Attendance): Observable<Attendance> {
    return this.http.post<Attendance>(`${this.apiUrl}/records`, record)
      .pipe(
        tap((newRecord: Attendance) => console.log(`Added record with id=${newRecord.id}`)),
        catchError(this.handleError<Attendance>('addAttendanceRecord'))
      );
  }

  // Update an existing attendance record
  updateAttendanceRecord(record: Attendance, formData?: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/records/${record.id}`, record)
      .pipe(
        tap(_ => console.log(`Updated record id=${record.id}`)),
        catchError(this.handleError<any>('updateAttendanceRecord'))
      );
  }

  // Delete an attendance record
  deleteAttendanceRecord(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/records/${id}`)
      .pipe(
        tap(_ => console.log(`Deleted record id=${id}`)),
        catchError(this.handleError<any>('deleteAttendanceRecord'))
      );
  }

  // Delete multiple attendance records
  deleteMultipleRecords(ids: string[]): Observable<any> {
    return this.http.post(`${this.apiUrl}/records/delete-multiple`, { ids })
      .pipe(
        tap(_ => console.log(`Deleted ${ids.length} records`)),
        catchError(this.handleError<any>('deleteMultipleRecords'))
      );
  }

  // Import records from CSV
  importFromCSV(file: File): Observable<Attendance[]> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<Attendance[]>(`${this.apiUrl}/import-csv`, formData)
      .pipe(
        tap(records => console.log(`Imported ${records.length} records from CSV`)),
        catchError(this.handleError<Attendance[]>('importFromCSV', []))
      );
  }

  // Export records to CSV
  exportToCSV(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/export-csv`, {
      responseType: 'blob'
    }).pipe(
      tap(_ => console.log('Exported records to CSV')),
      catchError(this.handleError<Blob>('exportToCSV'))
    );
  }

  // Fetch data from Bitrix (mock implementation for now)
  fetchFromBitrix(startDate?: string, endDate?: string): Observable<Attendance[]> {
    console.log('Mock Bitrix fetch with dates:', startDate, endDate);
    
    // Mock data
    const mockBitrixData: Attendance[] = [
      {
        id: 'B001',
        ldap: 'user.bitrix',
        masked_orgid: 'org456',
        subrole: 'Developer',
        role: 'Engineer',
        date: '2023-06-20',
        process: 'Development',
        billingCode: 'BIT001',
        activity: 'API Integration',
        status: 'Completed',
        lead_ldap: 'lead.bitrix',
        vendor: 'Bitrix24',
        minutes: '360',
        project: 'Bitrix Integration',
        team: 'Integration Team',
        comment: 'Imported from Bitrix'
      },
      {
        id: 'B002',
        ldap: 'john.doe',
        masked_orgid: 'org789',
        subrole: 'QA',
        role: 'Tester',
        date: '2023-06-21',
        process: 'Testing',
        billingCode: 'BIT002',
        activity: 'Test Execution',
        status: 'In Progress',
        lead_ldap: 'lead.bitrix',
        vendor: 'Bitrix24',
        minutes: '240',
        project: 'Bitrix Integration',
        team: 'QA Team',
        comment: 'Testing Bitrix integration'
      },
      {
        id: 'B003',
        ldap: 'jane.smith',
        masked_orgid: 'org123',
        subrole: 'Designer',
        role: 'UX Designer',
        date: '2023-06-22',
        process: 'Design',
        billingCode: 'BIT003',
        activity: 'UI Design',
        status: 'Completed',
        lead_ldap: 'lead.design',
        vendor: 'Bitrix24',
        minutes: '180',
        project: 'Bitrix UI',
        team: 'Design Team',
        comment: 'Completed UI mockups'
      }
    ];
    
    // Simulate network delay
    return of(mockBitrixData).pipe(
      delay(1500), // 1.5 second delay to simulate network request
      tap(records => console.log(`Fetched ${records.length} mock records from Bitrix`))
    );
  }

  getTimesheetById(id: string): Observable<Attendance> {
    return this.http.get<Attendance>(`${this.apiUrl}/records/${id}`)
      .pipe(
        tap(_ => console.log(`Fetched timesheet id=${id}`)),
        catchError(this.handleError<Attendance>('getTimesheetById'))
      );
  }

  // Error handling function
  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(`${operation} failed: ${error.message}`);
      
      // Let the app keep running by returning an empty result
      return of(result as T);
    };
  }
} 