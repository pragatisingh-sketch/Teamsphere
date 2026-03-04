import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ReleaseStep {
    id?: number;
    stepOrder: number;
    explanation: string;
    screenshotUrl?: string;
}

export interface ReleaseItem {
    id?: number;
    type: 'BUG_FIX' | 'ENHANCEMENT' | 'FEATURE' | 'HOTFIX';
    title: string;
    description?: string;
    steps: ReleaseStep[];
}

export interface Release {
    id?: number;
    version: string;
    title: string;
    releaseDate: string;
    createdBy?: string;
    createdAt?: string;
    updatedAt?: string;
    notificationSent?: boolean;
    notificationSentAt?: string;
    releaseItems: ReleaseItem[];
}

export interface Recipient {
    id?: number;
    email: string;
    name?: string;
    isActive?: boolean;
}

export interface ApiResponse<T> {
    status: string;
    message?: string;
    data: T;
}

@Injectable({
    providedIn: 'root'
})
export class ReleaseService {
    private baseUrl = `${environment.apiUrl}/api/releases`;

    constructor(private http: HttpClient) { }

    // Get current version (public)
    getCurrentVersion(): Observable<ApiResponse<{ version: string }>> {
        return this.http.get<ApiResponse<{ version: string }>>(`${this.baseUrl}/version`);
    }

    // Get all releases
    getAllReleases(): Observable<ApiResponse<Release[]>> {
        return this.http.get<ApiResponse<Release[]>>(this.baseUrl);
    }

    // Get release by ID
    getReleaseById(id: number): Observable<ApiResponse<Release>> {
        return this.http.get<ApiResponse<Release>>(`${this.baseUrl}/${id}`);
    }

    // Create release
    createRelease(release: Release): Observable<ApiResponse<Release>> {
        return this.http.post<ApiResponse<Release>>(this.baseUrl, release);
    }

    // Update release
    updateRelease(id: number, release: Release): Observable<ApiResponse<Release>> {
        return this.http.put<ApiResponse<Release>>(`${this.baseUrl}/${id}`, release);
    }

    // Delete release
    deleteRelease(id: number): Observable<ApiResponse<void>> {
        return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${id}`);
    }

    // Send notification
    sendNotification(releaseId: number, recipientEmails: string[]): Observable<ApiResponse<void>> {
        return this.http.post<ApiResponse<void>>(`${this.baseUrl}/${releaseId}/notify`, { recipientEmails });
    }

    // Get all recipients
    getAllRecipients(): Observable<ApiResponse<Recipient[]>> {
        return this.http.get<ApiResponse<Recipient[]>>(`${this.baseUrl}/recipients`);
    }

    // Add recipient
    addRecipient(email: string, name?: string): Observable<ApiResponse<Recipient>> {
        return this.http.post<ApiResponse<Recipient>>(`${this.baseUrl}/recipients`, { email, name });
    }

    // Delete recipient
    deleteRecipient(id: number): Observable<ApiResponse<void>> {
        return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/recipients/${id}`);
    }

    // Check if user is authorized (by LDAP)
    isAuthorizedUser(): boolean {
        const ldap = localStorage.getItem('username')?.toLowerCase();
        return ldap === 'piyushmi' || ldap === 'vrajoriya';
    }
}
