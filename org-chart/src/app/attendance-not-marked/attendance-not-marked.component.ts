import { Component, OnInit, ViewChild } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ReportsService } from '../services/reports.service';
import { NotificationService } from '../shared/notification.service';
import { AttendanceReminderDialogComponent } from './attendance-reminder-dialog/attendance-reminder-dialog.component';
import { ReusableTableComponent } from '../shared/components/reusable-table/reusable-table.component';

export interface AttendanceDefaulter {
    employeeId: number;
    employeeName: string;
    ldap: string;
    email: string;
    department: string;
    manager: string;
    lastAttendanceDate: string | null;
}

@Component({
    selector: 'app-attendance-not-marked',
    templateUrl: './attendance-not-marked.component.html',
    styleUrls: ['./attendance-not-marked.component.css']
})
export class AttendanceNotMarkedComponent implements OnInit {
    @ViewChild(ReusableTableComponent) tableComponent!: ReusableTableComponent;

    allDefaulters: AttendanceDefaulter[] = [];
    filteredDefaulters: AttendanceDefaulter[] = [];
    selectedDate: Date = new Date();
    loading = false;
    tableConfig: any;

    constructor(
        private reportsService: ReportsService,
        private dialog: MatDialog,
        private notificationService: NotificationService
    ) { }

    ngOnInit(): void {
        this.initializeTableConfig();
        this.loadAllData();
    }

    initializeTableConfig(): void {
        const columns: any[] = [
            {
                key: 'employeeName',
                label: 'Employee Name',
                sortable: true,
                filterable: true,
                cell: (row: AttendanceDefaulter) => row.employeeName
            },
            {
                key: 'ldap',
                label: 'LDAP',
                sortable: true,
                filterable: true,
                cell: (row: AttendanceDefaulter) => row.ldap
            },
            {
                key: 'email',
                label: 'Email',
                sortable: true,
                filterable: true,
                cell: (row: AttendanceDefaulter) => row.email
            },
            {
                key: 'department',
                label: 'Department',
                sortable: true,
                filterable: true,
                cell: (row: AttendanceDefaulter) => row.department || 'N/A'
            },
            {
                key: 'manager',
                label: 'Manager',
                sortable: true,
                filterable: true,
                cell: (row: AttendanceDefaulter) => row.manager || 'N/A'
            },
            {
                key: 'lastAttendanceDate',
                label: 'Last Attendance',
                sortable: true,
                cell: (row: AttendanceDefaulter) => row.lastAttendanceDate
                    ? new Date(row.lastAttendanceDate).toLocaleDateString()
                    : 'Never'
            }
        ];

        this.tableConfig = {
            columns,
            showSearch: true,
            showFilter: true,
            searchPlaceholder: 'Search employees...',
            showToggle: true,
            showPagination: true,
            pageSize: 25,
            pageSizeOptions: [10, 25, 50, 100],
            showSelection: true,
            exportConfig: {
                enabled: true,
                formats: ['csv', 'excel'],
                csvFilename: 'attendance-defaulters.csv',
                excelFilename: 'attendance-defaulters.xlsx'
            },
            actions: [
                {
                    label: 'Send Reminder',
                    icon: 'send',
                    color: 'primary',
                    isEnabled: (rows: AttendanceDefaulter[]) => rows.length > 0,
                    action: (rows: AttendanceDefaulter[]) => this.openReminderDialog(rows)
                }
            ]
        };
    }

    loadAllData(): void {
        this.loading = true;
        const filterParams: any = {};

        this.reportsService.getDailyAttendanceDefaulters(
            this.selectedDate,
            filterParams
        ).subscribe({
            next: (response) => {
                if (response.status === 'success') {
                    this.allDefaulters = response.data || [];
                    this.filteredDefaulters = [...this.allDefaulters];
                }
                this.loading = false;
            },
            error: (error) => {
                console.error('Error loading data:', error);
                this.notificationService.showError('Failed to load attendance defaulters');
                this.loading = false;
            }
        });
    }

    onDateChange(newDate: Date): void {
        this.selectedDate = newDate;
        this.loadAllData();
    }

    openReminderDialog(selectedEmployees: AttendanceDefaulter[]): void {
        const dialogRef = this.dialog.open(AttendanceReminderDialogComponent, {
            width: '600px',
            data: {
                employees: selectedEmployees,
                date: this.selectedDate
            }
        });

        dialogRef.afterClosed().subscribe(result => {
            if (result?.success) {
                this.notificationService.showSuccess('Attendance reminders sent successfully');
                // Clear selection after sending
                if (this.tableComponent) {
                    this.tableComponent.selection.clear();
                }
            }
        });
    }

    refreshData(): void {
        this.loadAllData();
    }
}
