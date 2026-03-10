import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { LeaveService } from '../../../../services/leave.service';

export interface VunnoAuditDialogData {
    vunnoResponseId: number;
    requestorName: string;
    leaveType: string;
}

export interface VunnoAuditEntry {
    id: number;
    vunnoResponseId: number;
    actionType: string;
    previousStatus: string;
    newStatus: string;
    changedBy: string;
    changedByRole: string;
    changedAt: string;
    changeReason: string;
    changeDescription: string;
    previousValues: string;
    newValues: string;
}

@Component({
    selector: 'app-vunno-audit-dialog',
    standalone: true,
    imports: [
        CommonModule,
        MatDialogModule,
        MatIconModule,
        MatProgressSpinnerModule,
        MatButtonModule
    ],
    templateUrl: './vunno-audit-dialog.component.html',
    styleUrls: ['./vunno-audit-dialog.component.css']
})
export class VunnoAuditDialogComponent {
    auditHistory: VunnoAuditEntry[] = [];
    loading = true;
    error: string | null = null;

    constructor(
        public dialogRef: MatDialogRef<VunnoAuditDialogComponent>,
        @Inject(MAT_DIALOG_DATA) public data: VunnoAuditDialogData,
        private leaveService: LeaveService
    ) {
        this.loadAuditHistory();
    }

    loadAuditHistory(): void {
        this.loading = true;
        this.error = null;

        this.leaveService.getAuditHistory(this.data.vunnoResponseId).subscribe({
            next: (response) => {
                if (response && response.data) {
                    this.auditHistory = response.data;
                }
                this.loading = false;
            },
            error: (error) => {
                console.error('Error loading audit history:', error);
                this.error = 'Failed to load audit history';
                this.loading = false;
            }
        });
    }

    close(): void {
        this.dialogRef.close();
    }

    getActionTypeColor(actionType: string): string {
        switch (actionType) {
            case 'CATEGORY_UPDATE':
                return 'category-update';
            case 'STATUS_UPDATE':
                return 'status-update';
            default:
                return 'default-action';
        }
    }

    getActionTypeIcon(actionType: string): string {
        switch (actionType) {
            case 'CATEGORY_UPDATE':
                return 'category';
            case 'STATUS_UPDATE':
                return 'update';
            default:
                return 'history';
        }
    }

    formatDate(dateString: string): string {
        const date = new Date(dateString);
        return date.toLocaleString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    /**
     * Format value from JSON string to display format
     * Prioritizes using status columns (previous_status/new_status) when available
     * Falls back to parsing JSON values only if status columns are empty
     */
    formatValue(value: string | null, actionType: string, statusValue?: string | null): string {
        // First priority: Use the status column if available (for APPROVE, REJECT, REVOKE, CATEGORY_UPDATE)
        if (statusValue) {
            return statusValue;
        }

        // Second priority: If no value at all, return 'Not set'
        if (!value) {
            return 'Not set';
        }

        // Third priority: For CATEGORY_UPDATE, try to parse JSON as fallback
        if (actionType === 'CATEGORY_UPDATE') {
            try {
                const parsed = JSON.parse(value);
                if (parsed.leaveCategory !== undefined) {
                    if (parsed.leaveCategory === null) {
                        return 'Not set';
                    }
                    return parsed.leaveCategory;
                }
            } catch (e) {
                // If parsing fails, continue to return original value
            }
        }

        // For STATUS_UPDATE or if parsing fails, try to parse status from JSON
        if (actionType === 'STATUS_UPDATE' || actionType === 'APPROVE' || actionType === 'REJECT' || actionType === 'REVOKE') {
            try {
                const parsed = JSON.parse(value);
                if (parsed.status !== undefined) {
                    return parsed.status || 'Not set';
                }
            } catch (e) {
                // If parsing fails, return original value
            }
        }

        // Last resort: return the original value
        return value;
    }
}
