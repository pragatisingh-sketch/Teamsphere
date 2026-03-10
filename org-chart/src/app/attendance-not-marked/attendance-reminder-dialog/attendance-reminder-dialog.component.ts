import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ReportsService } from '../../services/reports.service';
import { NotificationService } from '../../shared/notification.service';

export interface AttendanceReminderDialogData {
    employees: Array<{
        ldap: string;
        employeeName: string;
        email: string;
    }>;
    date: Date;
}

@Component({
    selector: 'app-attendance-reminder-dialog',
    templateUrl: './attendance-reminder-dialog.component.html',
    styleUrls: ['./attendance-reminder-dialog.component.css']
})
export class AttendanceReminderDialogComponent implements OnInit {
    reminderForm: FormGroup;
    sending = false;

    constructor(
        public dialogRef: MatDialogRef<AttendanceReminderDialogComponent>,
        @Inject(MAT_DIALOG_DATA) public data: AttendanceReminderDialogData,
        private fb: FormBuilder,
        private reportsService: ReportsService,
        private notificationService: NotificationService
    ) {
        this.reminderForm = this.fb.group({
            customMessage: ['', Validators.maxLength(500)]
        });
    }

    ngOnInit(): void { }

    get totalRecipients(): number {
        return this.data.employees.length;
    }

    get recipientNames(): string {
        if (this.totalRecipients <= 3) {
            return this.data.employees.map(e => e.employeeName).join(', ');
        }
        return `${this.data.employees.slice(0, 3).map(e => e.employeeName).join(', ')} and ${this.totalRecipients - 3} others`;
    }

    onCancel(): void {
        this.dialogRef.close();
    }

    onSend(): void {
        if (this.reminderForm.invalid || this.sending) {
            return;
        }

        this.sending = true;
        const ldaps = this.data.employees.map(e => e.ldap);
        const customMessage = this.reminderForm.value.customMessage || '';

        const request = {
            recipientLdaps: ldaps,
            customMessage: customMessage,
            isBulk: ldaps.length > 1
        };

        this.reportsService.sendAttendanceReminder(request).subscribe({
            next: (response) => {
                this.sending = false;
                if (response.status === 'success') {
                    const result = response.data;
                    const successCount = result.success || 0;
                    const failedCount = result.failed || 0;

                    if (failedCount === 0) {
                        this.notificationService.showSuccess(
                            `Attendance reminders sent successfully to ${successCount} recipient(s)`
                        );
                    } else {
                        this.notificationService.showWarning(
                            `Sent to ${successCount} recipient(s). Failed for ${failedCount} recipient(s).`
                        );
                    }
                    this.dialogRef.close({ success: true });
                } else {
                    this.notificationService.showError('Failed to send attendance reminders');
                    this.sending = false;
                }
            },
            error: (error) => {
                console.error('Error sending reminders:', error);
                this.notificationService.showError('Failed to send attendance reminders');
                this.sending = false;
            }
        });
    }
}
