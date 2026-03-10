import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ReportsService, IssueDetail } from '../../services/reports.service';
import { TableConfig } from '../../shared/components/reusable-table/table-config.interface';

export interface IssueDetailsDialogData {
    employeeId: number;
    employeeLdap: string;
    employeeName: string;
    issueType: 'TimeEntry' | 'Attendance' | 'Leaves';
    startDate: string;
    endDate: string;
}

@Component({
    selector: 'app-issue-details-dialog',
    templateUrl: './issue-details-dialog.component.html',
    styleUrls: ['./issue-details-dialog.component.css']
})
export class IssueDetailsDialogComponent implements OnInit {

    issues: IssueDetail[] = [];
    loading = true;
    tableConfig: TableConfig = {} as TableConfig;

    constructor(
        @Inject(MAT_DIALOG_DATA) public data: IssueDetailsDialogData,
        private dialogRef: MatDialogRef<IssueDetailsDialogComponent>,
        private reportsService: ReportsService
    ) { }

    ngOnInit(): void {
        this.initializeTableConfig();
        this.loadIssues();
    }

    initializeTableConfig(): void {
        const baseConfig: TableConfig = {
            columns: [],
            showGlobalSearch: true,
            showPagination: true,
            pageSize: 10,
            showExport: true,
            exportFileName: `${this.data.employeeName}_${this.data.issueType}_issues`
        };

        switch (this.data.issueType) {
            case 'TimeEntry':
                baseConfig.columns = [
                    { key: 'date', label: 'Date', sortable: true, type: 'date', format: (v) => this.formatDate(v) },
                    { key: 'project', label: 'Project', sortable: true, filterable: true },
                    { key: 'activity', label: 'Activity', sortable: true, filterable: true },
                    { key: 'timeInMins', label: 'Time (mins)', sortable: true, type: 'number' },
                    { key: 'status', label: 'Status', sortable: true, type: 'custom', template: 'badge' },
                    { key: 'createdAt', label: 'Created At', sortable: true, format: (v) => this.formatDateTime(v) },
                    { key: 'updatedAt', label: 'Updated At', sortable: true, format: (v) => this.formatDateTime(v) }
                ];
                break;

            case 'Attendance':
                baseConfig.columns = [
                    { key: 'date', label: 'Date', sortable: true, type: 'date', format: (v) => this.formatDate(v) },
                    { key: 'entryTimestamp', label: 'Check-in Time', sortable: true, format: (v) => this.formatDateTime(v) },
                    { key: 'exitTimestamp', label: 'Check-out Time', sortable: true, format: (v) => this.formatDateTime(v) },
                    { key: 'lateLoginReason', label: 'Late Login Reason', sortable: true },
                    { key: 'lateOrEarlyLogoutReason', label: 'Logout Reason', sortable: true },
                    { key: 'isOutsideOffice', label: 'Outside Office', sortable: true, format: (v) => v ? 'Yes' : 'No' }
                ];
                break;

            case 'Leaves':
                baseConfig.columns = [
                    { key: 'fromDate', label: 'From Date', sortable: true, type: 'date', format: (v) => this.formatDate(v) },
                    { key: 'toDate', label: 'To Date', sortable: true, type: 'date', format: (v) => this.formatDate(v) },
                    { key: 'leaveType', label: 'Leave Type', sortable: true, filterable: true },
                    { key: 'leaveCategory', label: 'Category', sortable: true, filterable: true },
                    { key: 'duration', label: 'Duration', sortable: true },
                    { key: 'createdAt', label: 'Created At', sortable: true, format: (v) => this.formatDateTime(v) }
                ];
                break;
        }

        this.tableConfig = baseConfig;
    }

    loadIssues(): void {
        this.loading = true;
        this.reportsService.getUserIssues(
            this.data.issueType,
            this.data.employeeLdap,
            this.data.startDate,
            this.data.endDate
        ).subscribe({
            next: (response) => {
                if (response.status === 'success' && response.data) {
                    this.issues = response.data;
                } else {
                    this.issues = [];
                }
                this.loading = false;
            },
            error: (err) => {
                console.error('Error loading issues:', err);
                this.issues = [];
                this.loading = false;
            }
        });
    }

    formatDate(date: string): string {
        if (!date) return '-';
        return new Date(date).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    }

    formatDateTime(dateTime: string): string {
        if (!dateTime) return '-';
        return new Date(dateTime).toLocaleString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    close(): void {
        this.dialogRef.close();
    }

    getIssueTypeLabel(): string {
        switch (this.data.issueType) {
            case 'TimeEntry': return 'Time Entry Issues';
            case 'Attendance': return 'Attendance Issues';
            case 'Leaves': return 'Unplanned Leaves';
            default: return 'Issues';
        }
    }
}
