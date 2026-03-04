import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { Subscription } from 'rxjs';
import { TableConfig, TableEvent } from '../shared/components/reusable-table/table-config.interface';
import { DateFilterService, DateRange } from '../services/date-filter.service';
import { AuthService } from '../auth.service';
import { NotificationService } from '../shared/notification.service';
import {
    ReportsService,
    WeeklyTimeEntryDefaulter,
    WeeklyBreakdown,
    TimeEntryReminderRequest
} from '../services/reports.service';
import { TimeEntryReminderDialogComponent } from './time-entry-reminder-dialog/time-entry-reminder-dialog.component';

interface TopDefaulter {
    employeeId: number;
    employeeName: string;
    department: string;
    missingWeeksCount: number;
}

@Component({
    selector: 'app-time-entry-pending',
    templateUrl: './time-entry-pending.component.html',
    styleUrls: ['./time-entry-pending.component.css']
})
export class TimeEntryPendingComponent implements OnInit, OnDestroy {

    // Top 3 Defaulters
    topDefaulters: TopDefaulter[] = [];

    // All Defaulters
    allDefaulters: WeeklyTimeEntryDefaulter[] = [];
    filteredDefaulters: WeeklyTimeEntryDefaulter[] = [];

    // Expanded row for inline detail (replacing bottom panel)
    expandedRowLdap: string | null = null;
    expandedDefaulter: WeeklyTimeEntryDefaulter | null = null;

    // Selection for bulk actions - using checkbox state
    selectedRows: WeeklyTimeEntryDefaulter[] = [];

    // Table Configuration
    tableConfig: TableConfig = {} as TableConfig;

    // State
    loading = false;
    dateRange: DateRange | null = null;
    startDate: string = '';
    endDate: string = '';

    // Filters
    filters: { [key: string]: string } = {};
    userRole: string = 'USER';

    // Accordion state
    isMainTableOpen = true;

    private dateSubscription?: Subscription;

    constructor(
        private reportsService: ReportsService,
        private dateFilterService: DateFilterService,
        private authService: AuthService,
        private router: Router,
        private dialog: MatDialog,
        private notificationService: NotificationService
    ) { }

    ngOnInit(): void {
        // Get user role
        this.authService.role$.subscribe(role => {
            if (role) {
                this.userRole = role;
            }
        });

        this.dateSubscription = this.dateFilterService.dateRange$.subscribe(range => {
            this.dateRange = range;
            this.startDate = this.formatDateForApi(range.start);
            this.endDate = this.formatDateForApi(range.end);
            this.loadAllData();
        });

        this.initializeTableConfig();
    }

    ngOnDestroy(): void {
        if (this.dateSubscription) {
            this.dateSubscription.unsubscribe();
        }
    }

    initializeTableConfig(): void {
        this.tableConfig = {
            columns: [
                { key: 'employeeName', label: 'Name', sortable: true, filterable: true, searchable: true, width: '180px' },
                { key: 'email', label: 'Email', sortable: true, searchable: true, width: '200px' },
                { key: 'department', label: 'Team', sortable: true, filterable: true, searchable: true, width: '120px' },
                { key: 'manager', label: 'Manager', sortable: true, filterable: true, searchable: true, width: '120px' },
                {
                    key: 'missingWeeksCount',
                    label: 'Missing Weeks',
                    sortable: true,
                    type: 'number' as const,
                    width: '130px',
                    clickable: true,
                    format: (value: number, row: WeeklyTimeEntryDefaulter) => {
                        const isExpanded = this.expandedRowLdap === row.ldap;
                        return value > 0
                            ? `<span style="color: #f44336; font-weight: bold; cursor: pointer;">${value} week${value > 1 ? 's' : ''} ${isExpanded ? '▲' : '▼'}</span>`
                            : `${value}`;
                    },
                    cellClick: (value: number, row: WeeklyTimeEntryDefaulter) => {
                        if (value > 0) {
                            this.toggleRowExpansion(row);
                        }
                    }
                }
            ],
            showGlobalSearch: true,
            showColumnFilters: true,
            showColumnToggle: true,
            showPagination: true,
            pageSize: 10,
            showExport: true,
            exportFileName: 'time_entry_pending_report',
            showSelection: true,
            exportConfig: {
                enabled: true,
                formats: ['csv', 'excel'],
                excelExportType: 'TIME_ENTRY_DEFAULTER',
                csvFilename: 'time-entry-defaulters.csv',
                excelFilename: 'time-entry-defaulters.xlsx'
            }
        };
    }

    // Handle all table events from reusable-table
    onTableEvent(event: TableEvent): void {
        switch (event.type) {
            case 'rowClick':
                this.onRowClick(event.data);
                break;
            case 'selectionChange':
                // Selection data is in event.data.selectedRows
                this.onSelectionChange(event.data?.selectedRows || []);
                break;
            case 'cellClick':
                // Cell click is handled by column config
                break;
        }
    }

    // Handle selection change from reusable-table
    onSelectionChange(selectedRows: WeeklyTimeEntryDefaulter[]): void {
        this.selectedRows = selectedRows;
    }

    // Toggle select all checkbox
    toggleSelectAll(event: Event): void {
        const checked = (event.target as HTMLInputElement).checked;
        this.selectedRows = checked ? [...this.filteredDefaulters] : [];
    }

    // Toggle individual row selection
    toggleRowSelection(defaulter: WeeklyTimeEntryDefaulter, event: Event): void {
        const checked = (event.target as HTMLInputElement).checked;
        if (checked) {
            if (!this.isSelected(defaulter)) {
                this.selectedRows = [...this.selectedRows, defaulter];
            }
        } else {
            this.selectedRows = this.selectedRows.filter(r => r.ldap !== defaulter.ldap);
        }
    }

    // Check if a row is selected
    isSelected(defaulter: WeeklyTimeEntryDefaulter): boolean {
        return this.selectedRows.some(r => r.ldap === defaulter.ldap);
    }

    // Check if all rows are selected
    isAllSelected(): boolean {
        return this.filteredDefaulters.length > 0 &&
            this.selectedRows.length === this.filteredDefaulters.length;
    }

    // Check if some but not all rows are selected (for indeterminate state)
    isSomeSelected(): boolean {
        return this.selectedRows.length > 0 &&
            this.selectedRows.length < this.filteredDefaulters.length;
    }

    // Handle row click from reusable-table - opens side panel
    onRowClick(row: WeeklyTimeEntryDefaulter): void {
        if (this.expandedDefaulter?.ldap === row.ldap) {
            this.expandedDefaulter = null;
        } else {
            this.expandedDefaulter = row;
        }
    }

    // Close the side panel
    closePanel(): void {
        this.expandedDefaulter = null;
    }

    // Toggle inline expansion for a row (legacy, keeping for compatibility)
    toggleRowExpansion(row: WeeklyTimeEntryDefaulter): void {
        this.onRowClick(row);
    }

    // Check if a row is expanded
    isRowExpanded(row: WeeklyTimeEntryDefaulter): boolean {
        return this.expandedDefaulter?.ldap === row.ldap;
    }

    handleUnifiedFilterChange(event: { type: string, value: string | null }): void {
        const key = event.type.toLowerCase();
        if (event.value) {
            this.filters[key] = event.value;
        } else {
            delete this.filters[key];
        }
        this.loadAllData();
    }

    loadAllData(): void {
        if (!this.dateRange) return;
        this.loading = true;

        const filterParams: { team?: string; manager?: string } = {};
        if (this.filters['team']) filterParams.team = this.filters['team'];
        if (this.filters['manager']) filterParams.manager = this.filters['manager'];

        this.reportsService.getWeeklyTimeEntryDefaulters(
            this.startDate,
            this.endDate,
            filterParams
        ).subscribe({
            next: (response) => {
                if (response.status === 'success') {
                    this.allDefaulters = response.data || [];
                    this.filteredDefaulters = [...this.allDefaulters];

                    // Get top 3
                    this.topDefaulters = this.allDefaulters.slice(0, 3).map(d => ({
                        employeeId: d.employeeId,
                        employeeName: d.employeeName,
                        department: d.department,
                        missingWeeksCount: d.missingWeeksCount
                    }));
                }
                this.loading = false;
            },
            error: (error) => {
                console.error('Error loading data:', error);
                this.notificationService.showError('Failed to load time entry pending data');
                this.loading = false;
            }
        });
    }

    // Reminder actions - open unified dialog for week-specific reminders
    sendReminderForWeek(defaulter: WeeklyTimeEntryDefaulter, week: WeeklyBreakdown): void {
        const dialogRef = this.dialog.open(TimeEntryReminderDialogComponent, {
            width: '550px',
            data: {
                recipients: [defaulter],
                isBulk: false,
                specificWeek: week
            }
        });

        dialogRef.afterClosed().subscribe(result => {
            if (result) {
                // Result from TimeEntryReminderDialogComponent is already a TimeEntryReminderRequest
                // we just need to send it
                this.sendReminder(result);
            }
        });
    }

    // Open dialog for custom message (bulk or individual with custom message)
    openReminderDialog(defaulters: WeeklyTimeEntryDefaulter[], isBulk: boolean): void {
        const dialogRef = this.dialog.open(TimeEntryReminderDialogComponent, {
            width: '500px',
            data: {
                recipients: defaulters,
                isBulk: isBulk
            }
        });

        dialogRef.afterClosed().subscribe(result => {
            if (result) {
                this.sendReminder(result);
            }
        });
    }

    sendBulkReminder(): void {
        if (this.selectedRows.length === 0) {
            this.notificationService.showWarning('Please select at least one employee');
            return;
        }
        this.openReminderDialog(this.selectedRows, true);
    }

    sendIndividualReminder(defaulter: WeeklyTimeEntryDefaulter): void {
        this.openReminderDialog([defaulter], false);
    }

    private sendReminder(request: TimeEntryReminderRequest): void {
        this.reportsService.sendTimeEntryReminder(request).subscribe({
            next: (response) => {
                if (response.status === 'success') {
                    const data = response.data;
                    this.notificationService.showSuccess(
                        `Reminders sent successfully! ${data.success} sent, ${data.failed} failed`
                    );
                    this.selectedRows = [];
                    this.expandedRowLdap = null;
                    this.expandedDefaulter = null;
                } else {
                    this.notificationService.showError(response.message || 'Failed to send reminders');
                }
            },
            error: (error) => {
                console.error('Error sending reminders:', error);
                this.notificationService.showError('Failed to send reminders. Please try again.');
            }
        });
    }

    formatDate(date: Date): string {
        return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
    }

    formatDateForApi(date: Date): string {
        const year = date.getFullYear();
        const month = ('0' + (date.getMonth() + 1)).slice(-2);
        const day = ('0' + date.getDate()).slice(-2);
        return `${year}-${month}-${day}`;
    }

    formatMissingDays(days: string[]): string {
        if (!days || days.length === 0) return '';
        return days.map(d => {
            const date = new Date(d);
            return date.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
        }).join(', ');
    }

    formatMissingDay(day: string): string {
        if (!day) return '';
        const date = new Date(day);
        return date.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
    }

    goBack(): void {
        this.router.navigate(['/reports']);
    }
}
