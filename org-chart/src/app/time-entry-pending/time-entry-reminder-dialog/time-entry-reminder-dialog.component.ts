import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { WeeklyTimeEntryDefaulter, WeeklyBreakdown, TimeEntryReminderRequest } from '../../services/reports.service';

export interface ReminderDialogData {
    recipients: WeeklyTimeEntryDefaulter[];
    isBulk: boolean;
    specificWeek?: WeeklyBreakdown;
}

@Component({
    selector: 'app-time-entry-reminder-dialog',
    templateUrl: './time-entry-reminder-dialog.component.html',
    styleUrls: ['./time-entry-reminder-dialog.component.css']
})
export class TimeEntryReminderDialogComponent implements OnInit {

    messageType: 'default' | 'custom' = 'default';
    customMessage = '';
    sending = false;

    readonly defaultMessage = `Dear {name},

This is a reminder that your time-entries are pending for the following periods:
{periods}

Please fill your timesheet at your earliest convenience by logging into the Highspring portal.

Best regards,
Highspring Team`;

    constructor(
        public dialogRef: MatDialogRef<TimeEntryReminderDialogComponent>,
        @Inject(MAT_DIALOG_DATA) public data: ReminderDialogData
    ) { }

    ngOnInit(): void {
        // Pre-fill custom message with default
        this.customMessage = this.defaultMessage;
    }

    get recipientCount(): number {
        return this.data.recipients.length;
    }

    get recipientName(): string {
        if (this.data.recipients.length === 1) {
            return this.data.recipients[0].employeeName;
        }
        return `${this.data.recipients.length} team members`;
    }

    get displayedRecipients(): WeeklyTimeEntryDefaulter[] {
        return this.data.recipients.slice(0, 5);
    }

    get remainingRecipientsCount(): number {
        return Math.max(0, this.data.recipients.length - 5);
    }

    getMissingPeriods(): string[] {
        if (this.data.specificWeek) {
            return [this.data.specificWeek.weekLabel];
        }

        // Collect all unique week labels from all recipients
        const periods = new Set<string>();
        this.data.recipients.forEach(r => {
            r.weeklyBreakdowns.forEach(w => periods.add(w.weekLabel));
        });
        return Array.from(periods);
    }

    formatMissingDay(day: string): string {
        if (!day) return '';
        const date = new Date(day);
        return date.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
    }

    sendReminder(): void {
        const request: TimeEntryReminderRequest = {
            recipientLdaps: this.data.recipients.map(r => r.ldap),
            customMessage: this.messageType === 'custom' ? this.customMessage : undefined,
            bulk: this.data.isBulk,
            missingPeriods: this.getMissingPeriods(),
            // Only include detailed day info if strict single-week context
            missingDays: this.data.specificWeek?.missingDays || [],
            wholeWeekMissing: this.data.specificWeek?.wholeWeekMissing
        };

        this.dialogRef.close(request);
    }

    cancel(): void {
        this.dialogRef.close(null);
    }
}
