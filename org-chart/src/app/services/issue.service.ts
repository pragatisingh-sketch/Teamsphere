
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment'; // Adjust if environment path differs

export interface Issue {
    id?: number;
    title: string;
    description: string;
    type: string;
    status?: string;
    priority: string;
    reporterEmail?: string;
    ccEmails?: string;
    reporterName?: string;
    feature?: string;
    feedbackType?: string;
    stepsToReproduce?: string;
    severity?: string;
    occurrenceDate?: string;
    attachments?: string;
    comments?: Comment[];
    createdAt?: string;
    updatedAt?: string;
}

export interface Comment {
    id?: number;
    content: string;
    authorName?: string;
    authorEmail?: string;
    authorRole?: string;
    createdAt?: string;
    parentId?: number;
    replies?: Comment[];
    mentions?: string[];
}

@Injectable({
    providedIn: 'root'
})
export class IssueService {

    private apiUrl = `${environment.apiUrl}/api/issues`;

    constructor(private http: HttpClient) { }

    createIssue(issue: Issue): Observable<Issue> {
        return this.http.post<Issue>(this.apiUrl, issue);
    }

    updateIssue(id: number, issue: Issue): Observable<Issue> {
        return this.http.put<Issue>(`${this.apiUrl}/${id}`, issue);
    }

    getAllIssues(): Observable<Issue[]> {
        return this.http.get<Issue[]>(this.apiUrl);
    }

    getEmails(): Observable<string[]> {
        return this.http.get<string[]>(`${this.apiUrl}/emails`);
    }

    addComment(issueId: number, comment: Comment): Observable<Comment> {
        return this.http.post<Comment>(`${this.apiUrl}/${issueId}/comments`, comment);
    }
}
