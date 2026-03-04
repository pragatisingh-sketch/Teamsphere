import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface FAQ {
    id?: number;
    question: string;
    answer: string;
    category?: string;
    displayOrder?: number;
    isActive?: boolean;
    createdAt?: string;
    createdBy?: string;
    updatedAt?: string;
    updatedBy?: string;
}

export interface FAQResponse<T> {
    status: string;
    message: string;
    data: T;
}

@Injectable({
    providedIn: 'root'
})
export class FAQService {
    private apiUrl = `${environment.apiUrl}/api/faqs`;

    constructor(private http: HttpClient) { }

    getActiveFAQs(): Observable<FAQResponse<FAQ[]>> {
        return this.http.get<FAQResponse<FAQ[]>>(this.apiUrl);
    }

    getAllFAQs(): Observable<FAQResponse<FAQ[]>> {
        return this.http.get<FAQResponse<FAQ[]>>(`${this.apiUrl}/all`);
    }

    canManageFAQs(): Observable<FAQResponse<{ canManage: boolean }>> {
        return this.http.get<FAQResponse<{ canManage: boolean }>>(`${this.apiUrl}/can-manage`);
    }

    createFAQ(faq: FAQ): Observable<FAQResponse<FAQ>> {
        return this.http.post<FAQResponse<FAQ>>(this.apiUrl, faq);
    }

    updateFAQ(id: number, faq: FAQ): Observable<FAQResponse<FAQ>> {
        return this.http.put<FAQResponse<FAQ>>(`${this.apiUrl}/${id}`, faq);
    }

    deleteFAQ(id: number): Observable<FAQResponse<void>> {
        return this.http.delete<FAQResponse<void>>(`${this.apiUrl}/${id}`);
    }

    initializeDefaultFAQs(): Observable<FAQResponse<void>> {
        return this.http.post<FAQResponse<void>>(`${this.apiUrl}/initialize`, {});
    }
}
