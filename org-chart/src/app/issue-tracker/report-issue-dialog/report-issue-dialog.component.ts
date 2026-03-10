import { Component, OnInit, ElementRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, FormControl } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { IssueService, Issue } from '../../services/issue.service';
import { NotificationService } from '../../shared/notification.service';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatChipInputEvent } from '@angular/material/chips';
import { Observable } from 'rxjs';
import { map, startWith } from 'rxjs/operators';
import { COMMA, ENTER, TAB } from '@angular/cdk/keycodes';

@Component({
  selector: 'app-report-issue-dialog',
  templateUrl: './report-issue-dialog.component.html',
  styleUrls: ['./report-issue-dialog.component.css']
})
export class ReportIssueDialogComponent implements OnInit {

  issueForm: FormGroup;

  // Autocomplete props
  separatorKeysCodes: number[] = [ENTER, COMMA, TAB];
  emailCtrl = new FormControl('');
  filteredEmails: Observable<string[]>;
  ccEmails: string[] = [];
  allEmails: string[] = [];

  @ViewChild('emailInput') emailInput!: ElementRef<HTMLInputElement>;

  constructor(
    private fb: FormBuilder,
    private issueService: IssueService,
    private dialogRef: MatDialogRef<ReportIssueDialogComponent>,
    private notificationService: NotificationService,
    private router: Router
  ) {
    this.issueForm = this.fb.group({
      title: ['', Validators.required],
      description: ['', Validators.required],
      type: ['Bug', Validators.required],
      priority: ['P2', Validators.required], // Default to P2 (Medium)
      ccEmails: [''],
      feature: [''],
      feedbackType: [''],
      stepsToReproduce: [''],
      severity: [''],
      occurrenceDate: [''],
      attachments: ['']
    });

    this.filteredEmails = this.emailCtrl.valueChanges.pipe(
      startWith(null),
      map((email: string | null) => (email ? this._filter(email) : this.allEmails.slice())),
    );
  }

  ngOnInit(): void {
    this.loadEmails();
  }

  loadEmails() {
    this.issueService.getEmails().subscribe({
      next: (emails) => {
        this.allEmails = emails;
      },
      error: (err) => console.error('Failed to load emails for autocomplete', err)
    });
  }

  // Chip methods
  add(event: MatChipInputEvent): void {
    const value = (event.value || '').trim();

    // Add email
    if (value && this.validateEmail(value)) {
      this.ccEmails.push(value);
    }

    // Clear the input value
    event.chipInput!.clear();
    this.emailCtrl.setValue(null);
  }

  remove(email: string): void {
    const index = this.ccEmails.indexOf(email);

    if (index >= 0) {
      this.ccEmails.splice(index, 1);
    }
  }

  selected(event: MatAutocompleteSelectedEvent): void {
    this.ccEmails.push(event.option.viewValue);
    this.emailInput.nativeElement.value = '';
    this.emailCtrl.setValue(null);
  }

  private _filter(value: string): string[] {
    const filterValue = value.toLowerCase();
    return this.allEmails.filter(email => email.toLowerCase().includes(filterValue));
  }

  private validateEmail(email: string) {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(email);
  }

  onSubmit() {
    if (this.issueForm.valid) {
      const issueData: Issue = {
        ...this.issueForm.value,
        ccEmails: this.ccEmails.join(',') // Join chips into comma-separated string
      };

      this.issueService.createIssue(issueData).subscribe({
        next: (res) => {
          this.notificationService.showNotification({
            type: 'success',
            message: 'Issue reported successfully!'
          });
          this.dialogRef.close(true);
        },
        error: (err) => {
          console.error(err);
          // Only show toast if it's NOT a 403 (handled by interceptor)
          if (err.status !== 403) {
            this.notificationService.showNotification({
              type: 'error',
              message: err.error?.message || 'Failed to report issue.'
            });
          }
        }
      });
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  viewReportedIssues() {
    this.dialogRef.close();
    this.router.navigate(['/issues']);
  }
}
