import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { environment } from '../../environments/environment';

/**
 * Base response wrapper for all API responses
 * Matches the backend BaseResponse structure
 */
interface BaseResponse<T> {
  status: string;
  code: number;
  message: string;
  data: T;
}

/**
 * Database backup operation response
 * Matches the backend DatabaseBackupResponseDTO structure
 */
interface DatabaseBackupResponse {
  operationId: string;
  operationType: string;
  status: string;
  fileId?: string;
  fileName?: string;
  fileSize?: number;
  timestamp: string;
  initiatedBy: string;
  details: string;
  durationMs?: number;
}

/**
 * Database backup request for import operations
 * Matches the backend DatabaseBackupRequestDTO structure
 */
interface DatabaseBackupRequest {
  fileId?: string;
}

@Injectable({
  providedIn: 'root'
})
export class DatabaseService {
  private apiUrl = `${environment.apiUrl}/api/database`;

  constructor(private http: HttpClient) { }

  /**
   * Trigger a database backup
   * @returns Observable with the response message from the server
   */
  triggerBackup(): Observable<string> {
    return this.http.post<BaseResponse<DatabaseBackupResponse>>(`${this.apiUrl}/backup`, {})
      .pipe(
        map(response => {
          if (response.status === 'success') {
            return response.message;
          } else {
            throw new Error(response.message || 'Database backup failed');
          }
        }),
        catchError(error => {
          // Handle different error structures
          if (error.error?.message) {
            return throwError(() => new Error(error.error.message));
          } else if (error.message) {
            return throwError(() => new Error(error.message));
          } else {
            return throwError(() => new Error('Failed to backup database. Please try again later.'));
          }
        })
      );
  }

  /**
   * Import data from a backup file
   * @param fileId Optional Google Drive file ID to import from a specific backup
   * @returns Observable with the response message from the server
   */
  importFromBackup(fileId?: string): Observable<string> {
    const payload: DatabaseBackupRequest = fileId ? { fileId } : {};
    return this.http.post<BaseResponse<DatabaseBackupResponse>>(`${this.apiUrl}/import`, payload)
      .pipe(
        map(response => {
          if (response.status === 'success') {
            return response.message;
          } else {
            throw new Error(response.message || 'Database import failed');
          }
        }),
        catchError(error => {
          // Handle different error structures
          if (error.error?.message) {
            return throwError(() => new Error(error.error.message));
          } else if (error.message) {
            return throwError(() => new Error(error.message));
          } else {
            return throwError(() => new Error('Failed to import database. Please try again later.'));
          }
        })
      );
  }

  /**
   * Trigger a database backup (detailed response)
   * @returns Observable with the full response from the server
   */
  triggerBackupDetailed(): Observable<DatabaseBackupResponse> {
    return this.http.post<BaseResponse<DatabaseBackupResponse>>(`${this.apiUrl}/backup`, {})
      .pipe(
        map(response => {
          if (response.status === 'success' && response.data) {
            return response.data;
          } else if (response.status === 'error' && response.data) {
            // Backend returned error status with operation details
            return response.data;
          } else {
            throw new Error(response.message || 'Database backup failed');
          }
        }),
        catchError(error => {
          // Handle different error structures
          if (error.error?.message) {
            return throwError(() => new Error(error.error.message));
          } else if (error.message) {
            return throwError(() => new Error(error.message));
          } else {
            return throwError(() => new Error('Failed to backup database. Please try again later.'));
          }
        })
      );
  }

  /**
   * Import data from a backup file (detailed response)
   * @param fileId Optional Google Drive file ID to import from a specific backup
   * @returns Observable with the full response from the server
   */
  importFromBackupDetailed(fileId?: string): Observable<DatabaseBackupResponse> {
    const payload: DatabaseBackupRequest = fileId ? { fileId } : {};
    return this.http.post<BaseResponse<DatabaseBackupResponse>>(`${this.apiUrl}/import`, payload)
      .pipe(
        map(response => {
          if (response.status === 'success' && response.data) {
            return response.data;
          } else if (response.status === 'error' && response.data) {
            // Backend returned error status with operation details
            return response.data;
          } else {
            throw new Error(response.message || 'Database import failed');
          }
        }),
        catchError(error => {
          // Handle different error structures
          if (error.error?.message) {
            return throwError(() => new Error(error.error.message));
          } else if (error.message) {
            return throwError(() => new Error(error.message));
          } else {
            return throwError(() => new Error('Failed to import database. Please try again later.'));
          }
        })
      );
  }

  /**
   * Get the full BaseResponse for backup operations
   * Useful for components that need access to status codes and full response structure
   * @returns Observable with the complete BaseResponse
   */
  triggerBackupFullResponse(): Observable<BaseResponse<DatabaseBackupResponse>> {
    return this.http.post<BaseResponse<DatabaseBackupResponse>>(`${this.apiUrl}/backup`, {})
      .pipe(
        catchError(error => {
          // Transform error to match expected structure
          const errorResponse: BaseResponse<DatabaseBackupResponse> = {
            status: 'error',
            code: error.status || 500,
            message: error.error?.message || 'Failed to backup database',
            data: null as any
          };
          return throwError(() => errorResponse);
        })
      );
  }

  /**
   * Get the full BaseResponse for import operations
   * Useful for components that need access to status codes and full response structure
   * @param fileId Optional Google Drive file ID to import from a specific backup
   * @returns Observable with the complete BaseResponse
   */
  importFromBackupFullResponse(fileId?: string): Observable<BaseResponse<DatabaseBackupResponse>> {
    const payload: DatabaseBackupRequest = fileId ? { fileId } : {};
    return this.http.post<BaseResponse<DatabaseBackupResponse>>(`${this.apiUrl}/import`, payload)
      .pipe(
        catchError(error => {
          // Transform error to match expected structure
          const errorResponse: BaseResponse<DatabaseBackupResponse> = {
            status: 'error',
            code: error.status || 500,
            message: error.error?.message || 'Failed to import database',
            data: null as any
          };
          return throwError(() => errorResponse);
        })
      );
  }

  /**
   * Check the status of a database operation
   * @param operationId The operation ID to check status for
   * @returns Observable with the operation status
   */
  getOperationStatus(operationId: string): Observable<DatabaseBackupResponse> {
    return this.http.get<BaseResponse<DatabaseBackupResponse>>(`${this.apiUrl}/status/${operationId}`)
      .pipe(
        map(response => {
          if (response.status === 'success' && response.data) {
            return response.data;
          } else {
            throw new Error(response.message || 'Failed to get operation status');
          }
        }),
        catchError(error => {
          // Handle different error structures
          if (error.error?.message) {
            return throwError(() => new Error(error.error.message));
          } else if (error.message) {
            return throwError(() => new Error(error.message));
          } else {
            return throwError(() => new Error('Failed to get operation status. Please try again later.'));
          }
        })
      );
  }

  /**
   * Upload employee relations CSV file
   * @param formData FormData containing the CSV file
   * @returns Observable with the response message
   */
  uploadEmployeeRelationCsv(formData: FormData): Observable<string> {
    return this.http.post<BaseResponse<string>>(`${environment.apiUrl}/api/employee-relations/upload-csv`, formData)
      .pipe(
        map(response => {
          if (response.status === 'success') {
            return response.message || 'CSV uploaded successfully';
          } else {
            throw new Error(response.message || 'CSV upload failed');
          }
        }),
        catchError(error => {
          // Handle different error structures
          if (error.error?.message) {
            return throwError(() => new Error(error.error.message));
          } else if (error.message) {
            return throwError(() => new Error(error.message));
          } else {
            return throwError(() => new Error('Failed to upload CSV. Please try again later.'));
          }
        })
      );
  }
}
