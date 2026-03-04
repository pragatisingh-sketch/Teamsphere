import { Component, OnInit, ViewChild } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ReportsService } from '../services/reports.service';
import { NotificationService } from '../shared/notification.service';
import { ReusableTableComponent } from '../shared/components/reusable-table/reusable-table.component';
import { animate, state, style, transition, trigger } from '@angular/animations';

export interface LongWeekendLeavePattern {
    employeeId: number;
    employeeName: string;
    ldap: string;
    email: string;
    department: string;
    manager: string;
    occurrenceCount: number;
    instances: LongWeekendInstance[];
}

export interface LongWeekendInstance {
    startDate: string;
    endDate: string;
    leaveType: string;
    totalDays: number;
    pattern: string;
}

@Component({
    selector: 'app-long-weekend-leave',
    templateUrl: './long-weekend-leave.component.html',
    styleUrls: ['./long-weekend-leave.component.css'],
    animations: [
        trigger('detailExpand', [
            state('collapsed', style({ height: '0px', minHeight: '0' })),
            state('expanded', style({ height: '*' })),
            transition('expanded <=> collapsed', animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)')),
        ]),
    ],
})
export class LongWeekendLeaveComponent implements OnInit {
    @ViewChild(ReusableTableComponent) tableComponent!: ReusableTableComponent;

    allPatterns: LongWeekendLeavePattern[] = [];
    filteredPatterns: LongWeekendLeavePattern[] = [];
    startDate!: Date;
    endDate!: Date;
    loading = false;
    tableConfig: any;
    expandedElement: LongWeekendLeavePattern | null = null;

    constructor(
        private reportsService: ReportsService,
        private dialog: MatDialog,
        private notificationService: NotificationService
    ) { }

    ngOnInit(): void {
        // Set default date range (last 3 months)
        this.endDate = new Date();
        this.startDate = new Date();
        this.startDate.setMonth(this.startDate.getMonth() - 3);

        this.initializeTableConfig();
        this.loadAllData();
    }

    initializeTableConfig(): void {
        const columns: any[] = [
            {
                key: 'employeeName',
                header: 'Employee Name',
                sortable: true,
                filterable: true,
                cell: (row: LongWeekendLeavePattern) => row.employeeName
            },
            {
                key: 'ldap',
                header: 'LDAP',
                sortable: true,
                filterable: true,
                cell: (row: LongWeekendLeavePattern) => row.ldap
            },
            {
                key: 'department',
                header: 'Department',
                sortable: true,
                filterable: true,
                cell: (row: LongWeekendLeavePattern) => row.department || 'N/A'
            },
            {
                key: 'manager',
                header: 'Manager',
                sortable: true,
                filterable: true,
                cell: (row: LongWeekendLeavePattern) => row.manager || 'N/A'
            },
            {
                key: 'occurrenceCount',
                header: 'Occurrences',
                sortable: true,
                cell: (row: LongWeekendLeavePattern) => row.occurrenceCount.toString()
            }
        ];

        this.tableConfig = {
            columns,
            title: 'Long Weekend Leave Patterns',
            showSearch: true,
            showFilter: true,
            searchPlaceholder: 'Search employees...',
            showToggle: true,
            showPagination: true,
            pageSize: 25,
            pageSizeOptions: [10, 25, 50, 100],
            exportConfig: {
                enabled: true,
                formats: ['csv', 'excel'],
                csvFilename: 'long-weekend-patterns.csv',
                excelFilename: 'long-weekend-patterns.xlsx'
            }
        };
    }

    loadAllData(): void {
        this.loading = true;
        const filterParams: any = {};

        this.reportsService.getLongWeekendLeavePatterns(
            this.startDate,
            this.endDate,
            filterParams
        ).subscribe({
            next: (response) => {
                if (response.status === 'success') {
                    this.allPatterns = response.data || [];
                    this.filteredPatterns = [...this.allPatterns];
                }
                this.loading = false;
            },
            error: (error) => {
                console.error('Error loading data:', error);
                this.notificationService.showError('Failed to load long weekend leave patterns');
                this.loading = false;
            }
        });
    }

    onDateRangeChange(): void {
        this.loadAllData();
    }

    refreshData(): void {
        this.loadAllData();
    }

    toggleRow(pattern: LongWeekendLeavePattern): void {
        this.expandedElement = this.expandedElement === pattern ? null : pattern;
    }

    isExpanded(pattern: LongWeekendLeavePattern): boolean {
        return this.expandedElement === pattern;
    }
}
