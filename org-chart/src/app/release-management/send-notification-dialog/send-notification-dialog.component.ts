import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialog } from '@angular/material/dialog';
import { ReleaseService, Release, Recipient } from '../../services/release.service';
import { NotificationService } from '../../shared/notification.service';
import { ConfirmationDialogComponent } from '../../confirm-dialog/confirmation-dialog.component';

@Component({
    selector: 'app-send-notification-dialog',
    templateUrl: './send-notification-dialog.component.html',
    styleUrls: ['./send-notification-dialog.component.css']
})
export class SendNotificationDialogComponent implements OnInit {
    release: Release;
    recipients: Recipient[] = [];
    selectedRecipients: string[] = [];
    newEmail = '';
    isSending = false;
    isLoading = true;

    constructor(
        private releaseService: ReleaseService,
        private notificationService: NotificationService,
        private dialog: MatDialog,
        public dialogRef: MatDialogRef<SendNotificationDialogComponent>,
        @Inject(MAT_DIALOG_DATA) public data: { release: Release }
    ) {
        this.release = data.release;
    }

    ngOnInit(): void {
        this.loadRecipients();
    }

    loadRecipients(): void {
        this.isLoading = true;
        this.releaseService.getAllRecipients().subscribe({
            next: (response) => {
                if (response.status === 'success') {
                    this.recipients = response.data;
                    // Auto-select all active recipients
                    this.selectedRecipients = this.recipients.map(r => r.email);
                }
                this.isLoading = false;
            },
            error: () => {
                this.isLoading = false;
            }
        });
    }

    toggleRecipient(email: string): void {
        const index = this.selectedRecipients.indexOf(email);
        if (index === -1) {
            this.selectedRecipients.push(email);
        } else {
            this.selectedRecipients.splice(index, 1);
        }
    }

    isSelected(email: string): boolean {
        return this.selectedRecipients.includes(email);
    }

    addNewEmail(): void {
        if (!this.newEmail || !this.isValidEmail(this.newEmail)) {
            this.notificationService.showWarning('Please enter a valid email address');
            return;
        }

        if (this.selectedRecipients.includes(this.newEmail)) {
            this.notificationService.showWarning('Email already added');
            return;
        }

        // Add to selected list
        this.selectedRecipients.push(this.newEmail);

        // Also save to database for future use
        this.releaseService.addRecipient(this.newEmail).subscribe({
            next: (response) => {
                if (response.status === 'success') {
                    this.recipients.push(response.data);
                }
            }
        });

        this.newEmail = '';
    }

    isValidEmail(email: string): boolean {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    sendNotification(): void {
        if (this.selectedRecipients.length === 0) {
            this.notificationService.showWarning('Please select at least one recipient');
            return;
        }

        this.isSending = true;
        this.releaseService.sendNotification(this.release.id!, this.selectedRecipients).subscribe({
            next: () => {
                this.notificationService.showSuccess('Release notification sent successfully!');
                this.dialogRef.close(true);
            },
            error: (err) => {
                this.notificationService.showError(err.error?.message || 'Failed to send notification');
                this.isSending = false;
            }
        });
    }

    deleteRecipient(recipient: Recipient): void {
        if (!recipient.id) return;

        const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
            width: '400px',
            data: {
                title: 'Remove Recipient',
                message: `Are you sure you want to remove ${recipient.email} from saved recipients?`,
                confirmButtonText: 'Remove',
                color: 'warn'
            }
        });

        dialogRef.afterClosed().subscribe(result => {
            if (result) {
                this.releaseService.deleteRecipient(recipient.id!).subscribe({
                    next: () => {
                        // Remove from local list
                        this.recipients = this.recipients.filter(r => r.id !== recipient.id);
                        // Remove from selected if present
                        this.selectedRecipients = this.selectedRecipients.filter(e => e !== recipient.email);
                        this.notificationService.showSuccess('Recipient removed');
                    },
                    error: (err) => {
                        this.notificationService.showError(err.error?.message || 'Failed to remove recipient');
                    }
                });
            }
        });
    }

    onCancel(): void {
        this.dialogRef.close(false);
    }
}
