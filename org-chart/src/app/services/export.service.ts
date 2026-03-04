import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
    providedIn: 'root'
})
export class ExportService {
    private baseUrl = environment.apiUrl || 'http://localhost:8080';

    constructor(private http: HttpClient) { }

    /**
     * Export data as Excel
     * @param type Export strategy type (e.g., TIME_ENTRY_DEFAULTER, TIME_ENTRY_COMPLIANCE)
     * @param startDate Start date in yyyy-MM-dd format
     * @param endDate End date in yyyy-MM-dd format
     * @param filters Optional filters
     * @returns Observable of Blob (Excel file)
     */
    exportExcel(type: string, startDate: string, endDate: string, filters?: any): Observable<Blob> {
        const url = `${this.baseUrl}/api/exports/download`;

        const payload = {
            type: type,
            startDate: startDate,
            endDate: endDate,
            filters: filters || {}
        };

        return this.http.post(url, payload, {
            responseType: 'blob',
            headers: new HttpHeaders({
                'Content-Type': 'application/json'
            })
        });
    }
}
