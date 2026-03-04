import { Component, OnInit, ElementRef, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators, FormControl } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { IssueService, Issue, Comment } from '../../services/issue.service';
import { NotificationService } from '../../shared/notification.service';
import { EmployeeService } from '../../employee.service';
import { COMMA, ENTER } from '@angular/cdk/keycodes';
import { MatChipInputEvent } from '@angular/material/chips';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { Observable } from 'rxjs';
import { map, startWith } from 'rxjs/operators';

@Component({
    selector: 'app-issue-detail',
    templateUrl: './issue-detail.component.html',
    styleUrls: ['./issue-detail.component.css']
})
export class IssueDetailComponent implements OnInit {

    issueId!: number;
    issue: Issue | null = null;
    isLoading = true;
    commentForm: FormGroup;
    isReplyingTo: number | null = null;
    errorMessage: string | null = null;

    // CC Feature
    separatorKeysCodes: number[] = [ENTER, COMMA];
    ccCtrl = new FormControl('');
    filteredEmployees: Observable<string[]>;
    ccUsers: string[] = [];
    allEmployees: any[] = [];
    allEmployeeLdaps: string[] = [];
    @ViewChild('ccInput') ccInput!: ElementRef<HTMLInputElement>;

    // Inline Mentions
    filteredMentions: Observable<string[]>;
    cursorPosition = 0;
    currentMentionQuery = '';

    constructor(
        private route: ActivatedRoute,
        private issueService: IssueService,
        private employeeService: EmployeeService,
        private fb: FormBuilder,
        private notificationService: NotificationService,
        private router: Router
    ) {
        this.commentForm = this.fb.group({
            content: ['', Validators.required]
        });

        this.filteredEmployees = this.ccCtrl.valueChanges.pipe(
            startWith(null),
            map((user: string | null) => (user ? this._filter(user) : this.allEmployeeLdaps.slice()))
        );

        // Initialize filteredMentions based on content changes
        this.filteredMentions = this.commentForm.get('content')!.valueChanges.pipe(
            startWith(''),
            map(value => this._filterMentions(value || ''))
        );
    }

    ngOnInit(): void {
        const id = this.route.snapshot.paramMap.get('id');
        if (id) {
            this.issueId = +id;
            this.loadEmployees();
            this.loadIssue();
        } else {
            this.errorMessage = 'Invalid Issue ID';
        }
    }

    loadEmployees() {
        this.employeeService.getEmployeeSummary().subscribe({
            next: (res) => {
                this.allEmployees = res.data;
                if (Array.isArray(res.data)) {
                    // Prioritize email, then ldap, then username. Enable full email usage.
                    this.allEmployeeLdaps = res.data.map((e: any) => e.email || e.ldap || e.username).filter((x: any) => !!x);
                }
            },
            error: (err) => console.error('Failed to load employees', err)
        });
    }

    loadIssue(): void {
        this.isLoading = true;
        this.issueService.getAllIssues().subscribe({
            next: (issues) => {
                const found = issues.find(i => i.id === this.issueId);
                if (found) {
                    this.issue = found;
                    if (this.issue.ccEmails) {
                        this.ccUsers = this.issue.ccEmails.split(',').map(s => s.trim()).filter(s => !!s);
                    } else {
                        this.ccUsers = [];
                    }
                } else {
                    this.errorMessage = 'Issue not found';
                }
                this.isLoading = false;
            },
            error: (err) => {
                this.isLoading = false;
                this.errorMessage = 'Failed to load issue';
                console.error(err);
            }
        });
    }

    // CC Logic
    add(event: MatChipInputEvent): void {
        const value = (event.value || '').trim();
        if (value && this.allEmployeeLdaps.includes(value) && !this.ccUsers.includes(value)) {
            this.ccUsers.push(value);
            this.updateCC();
        }
        event.chipInput!.clear();
        this.ccCtrl.setValue(null);
    }

    remove(user: string): void {
        const index = this.ccUsers.indexOf(user);
        if (index >= 0) {
            this.ccUsers.splice(index, 1);
            this.updateCC();
        }
    }

    selected(event: MatAutocompleteSelectedEvent): void {
        const value = event.option.viewValue;
        if (!this.ccUsers.includes(value)) {
            this.ccUsers.push(value);
            this.updateCC();
        }
        this.ccInput.nativeElement.value = '';
        this.ccCtrl.setValue(null);
    }

    private _filter(value: string): string[] {
        const filterValue = value.toLowerCase();
        return this.allEmployeeLdaps.filter(user => user.toLowerCase().includes(filterValue));
    }

    updateCC() {
        if (!this.issue) return;
        const newCC = this.ccUsers.join(',');
        if (this.issue.ccEmails !== newCC) {
            this.issue.ccEmails = newCC;
            this.saveIssue('CC Updated');
        }
    }

    saveIssue(successMessage: string = 'Issue Updated') {
        if (!this.issue || !this.issue.id) return;
        this.issueService.updateIssue(this.issue.id, this.issue).subscribe({
            next: (res) => {
                this.issue = res; // Auto-refresh: update local state with response
                this.notificationService.showNotification({ type: 'success', message: successMessage });
            },
            error: (err) => {
                this.notificationService.showNotification({ type: 'error', message: 'Failed to update issue' });
            }
        });
    }

    updateStatus(newStatus: string) {
        if (!this.issue) return;
        this.issue.status = newStatus;
        this.saveIssue(`Status updated to ${newStatus}`);
    }

    // Inline Mention Logic
    onCommentInput(event: Event): void {
        const input = event.target as HTMLTextAreaElement;
        this.cursorPosition = input.selectionStart;
        const value = input.value;

        // Simple detection: find closest @ before cursor
        const lastAt = value.lastIndexOf('@', this.cursorPosition - 1);
        if (lastAt !== -1) {
            const textAfterAt = value.substring(lastAt + 1, this.cursorPosition);
            // Allow dots and hyphens in mention query
            if (!textAfterAt.includes(' ')) {
                this.currentMentionQuery = textAfterAt;
            } else {
                this.currentMentionQuery = '';
            }
        } else {
            this.currentMentionQuery = '';
        }

        // Trigger value changes to update filter
        this.commentForm.get('content')?.updateValueAndValidity({ emitEvent: true });
    }

    private _filterMentions(value: string): string[] {
        if (this.currentMentionQuery) {
            const filterValue = this.currentMentionQuery.toLowerCase();
            return this.allEmployeeLdaps.filter(user => user.toLowerCase().includes(filterValue));
        }
        return [];
    }

    selectedMention(event: MatAutocompleteSelectedEvent): void {
        const selectedUser = event.option.viewValue;
        const currentContent = this.commentForm.get('content')?.value;

        if (currentContent) {
            const lastAt = currentContent.lastIndexOf('@', this.cursorPosition - 1);
            if (lastAt !== -1) {
                const before = currentContent.substring(0, lastAt);
                const after = currentContent.substring(this.cursorPosition);
                // Ensure space after mention
                const newValue = `${before}@${selectedUser} ${after}`;
                this.commentForm.get('content')?.setValue(newValue);
            }
        }
    }

    addComment(): void {
        if (this.commentForm.invalid || !this.issueId) return;

        const content = this.commentForm.get('content')?.value;

        // Parse mentions from content
        const mentions: string[] = [];
        const uniqueMatches = new Set<string>();

        // 1. Find emails (e.g. user@google.com)
        const emailRegex = /\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b/g;
        let match;
        while ((match = emailRegex.exec(content)) !== null) {
            uniqueMatches.add(match[0]);
        }

        // 2. Find explicit mentions (e.g. @username or @user.name)
        // Matches @ followed by word chars/dots/hyphens, preceded by start-of-line or whitespace
        const mentionRegex = /(?:^|\s)@([a-zA-Z0-9._-]+)/g;
        while ((match = mentionRegex.exec(content)) !== null) {
            uniqueMatches.add(match[1]);
        }

        // 3. Resolve and Validate against employee list
        uniqueMatches.forEach(potentialUser => {
            // Check against LDAPs/Emails directly
            // Note: allEmployeeLdaps contains mixed LDAPs and Emails based on availability
            if (this.allEmployeeLdaps.some(u => u.toLowerCase() === potentialUser.toLowerCase())) {
                mentions.push(potentialUser);
            } else {
                // Fallback: Check if potentialUser is an LDAP that maps to a known employee
                // even if allEmployeeLdaps might have stored the email.
                // We need to check allEmployees data source for robustness.
                const found = this.allEmployees.find(e =>
                    (e.ldap && e.ldap.toLowerCase() === potentialUser.toLowerCase()) ||
                    (e.email && e.email.toLowerCase() === potentialUser.toLowerCase()) ||
                    (e.username && e.username.toLowerCase() === potentialUser.toLowerCase())
                );
                if (found) {
                    // Push the value that matched (or prefer email if we need to standardize)
                    // Backend resolves LDAPs now, so pushing what looks like a valid ID is fine.
                    mentions.push(potentialUser);
                }
            }
        });

        const commentData = {
            content,
            mentions: mentions
        };

        this.issueService.addComment(this.issueId, commentData).subscribe({
            next: (comment) => {
                this.issue?.comments?.push(comment);
                this.commentForm.reset();
                this.notificationService.showNotification({ type: 'success', message: 'Comment added' });
            },
            error: (err) => {
                this.notificationService.showNotification({ type: 'error', message: 'Failed to add comment' });
                console.error(err);
            }
        });
    }

    goBack() {
        this.router.navigate(['/issues']);
    }

    formatComment(content: string): string {
        if (!content) return '';
        // 1. Escape HTML to prevent XSS
        let safeContent = content
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");

        // 2. Wrap Mentions in Chip Spans
        // Regex: @ followed by non-whitespace (including @)
        // Fix regex range here too if needed, but [a-zA-Z0-9._-@] was actually 'hyphen between dot and @'? 
        // No, dot is 46, _ is 95, @ is 64. 95-64 is impossible. 
        // Safe regex: [a-zA-Z0-9._@-], or better [a-zA-Z0-9.@_-]
        safeContent = safeContent.replace(/@([a-zA-Z0-9._@-]+)/g, (match, username) => {
            return `<span class="mention-chip">${match}</span>`;
        });

        return safeContent;
    }
}
