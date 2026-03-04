import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { TableConfig } from '../shared/components/reusable-table/table-config.interface';
import { DateFilterService, DateRange } from '../services/date-filter.service';
import { AuthService } from '../auth.service';
import { ReportsService } from '../services/reports.service';
import { IssueDetailsDialogComponent, IssueDetailsDialogData } from './issue-details-dialog/issue-details-dialog.component';

interface DefaulterDetail {
  employeeId: number;
  employeeName: string;
  email: string;
  department: string;
  manager: string;
  project: string;
  program: string;
  issueCount: number;
  status: string;
  lastIncidentDate: string;
  ldap?: string; // Employee LDAP for fetching detailed issues
}

interface TopDefaulter {
  employeeId: number;
  employeeName: string;
  department: string;
  issueCount: number;
  avatarUrl?: string;
}

@Component({
  selector: 'app-compliance-details',
  templateUrl: './compliance-details.component.html',
  styleUrls: ['./compliance-details.component.css']
})
export class ComplianceDetailsComponent implements OnInit, OnDestroy {

  // Top 3 Defaulters
  topTimeEntryDefaulters: TopDefaulter[] = [];
  topAttendanceDefaulters: TopDefaulter[] = [];
  topLeavesDefaulters: TopDefaulter[] = [];

  // Detailed Lists
  timeEntryDefaulters: DefaulterDetail[] = [];
  attendanceDefaulters: DefaulterDetail[] = [];
  leavesDefaulters: DefaulterDetail[] = [];

  // Filtered Lists
  filteredTimeEntryDefaulters: DefaulterDetail[] = [];
  filteredAttendanceDefaulters: DefaulterDetail[] = [];
  filteredLeavesDefaulters: DefaulterDetail[] = [];

  tableConfig: TableConfig = {} as TableConfig;
  timeEntryTableConfig: TableConfig = {} as TableConfig;
  attendanceTableConfig: TableConfig = {} as TableConfig;
  leavesTableConfig: TableConfig = {} as TableConfig;
  loading = false;
  dateRange: DateRange | null = null;
  startDate: string = '';
  endDate: string = '';

  // Accordion State
  isTimeEntryOpen = true;
  isAttendanceOpen = false;
  isLeavesOpen = false;

  // Filters
  filters: { [key: string]: string } = {};
  userRole: string = 'USER';

  private dateSubscription?: Subscription;

  constructor(
    private reportsService: ReportsService,
    private dateFilterService: DateFilterService,
    private authService: AuthService,
    private router: Router,
    private dialog: MatDialog
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

  handleUnifiedFilterChange(event: { type: string, value: string | null }): void {
    // Update filters based on event
    const key = event.type.toLowerCase();
    if (event.value) {
      this.filters[key] = event.value;
    } else {
      delete this.filters[key];
    }
    this.loadAllData();
  }

  ngOnDestroy(): void {
    if (this.dateSubscription) {
      this.dateSubscription.unsubscribe();
    }
  }

  initializeTableConfig(): void {
    // Base columns configuration
    const createColumns = (issueType: 'TimeEntry' | 'Attendance' | 'Leaves') => [
      { key: 'employeeName', label: 'Name', sortable: true, filterable: true, searchable: true, width: '150px' },
      { key: 'email', label: 'Email', sortable: true, searchable: true, width: '200px' },
      { key: 'department', label: 'Team', sortable: true, filterable: true, searchable: true, width: '120px' },
      { key: 'manager', label: 'Manager', sortable: true, filterable: true, searchable: true, width: '120px' },
      { key: 'project', label: 'Process', sortable: true, filterable: true, searchable: true, width: '120px' },
      { key: 'program', label: 'Program', sortable: true, filterable: true, searchable: true, width: '120px' },
      {
        key: 'issueCount',
        label: 'No of Instances',
        sortable: true,
        type: 'number' as const,
        width: '120px',
        clickable: true,
        format: (value: number) => value > 0 ? `<span style="color: #f44336; font-weight: bold; cursor: pointer; text-decoration: underline;">${value}</span>` : `${value}`,
        cellClick: (value: number, row: DefaulterDetail) => {
          if (value > 0) {
            this.openIssueDetailsDialog(row, issueType);
          }
        }
      },
      { key: 'lastIncidentDate', label: 'Last Incident', sortable: true, width: '120px' }
    ];

    const baseConfig = {
      showGlobalSearch: true,
      showColumnFilters: true,
      showColumnToggle: true,
      showPagination: true,
      pageSize: 10,
      showExport: true
    };

    // Create type-specific table configs
    this.timeEntryTableConfig = {
      ...baseConfig,
      columns: createColumns('TimeEntry'),
      exportFileName: 'time_entry_compliance_report',
      exportConfig: {
        enabled: true,
        formats: ['excel'],
        excelExportType: 'TIME_ENTRY_COMPLIANCE',
        excelFilename: 'time-entry-compliance.xlsx'
      }
    };

    this.attendanceTableConfig = {
      ...baseConfig,
      columns: createColumns('Attendance'),
      exportFileName: 'attendance_compliance_report',
      exportConfig: {
        enabled: true,
        formats: ['csv', 'excel'],
        excelExportType: 'ATTENDANCE_COMPLIANCE',
        csvFilename: 'attendance-compliance.csv',
        excelFilename: 'attendance-compliance.xlsx'
      }
    };

    this.leavesTableConfig = {
      ...baseConfig,
      columns: createColumns('Leaves'),
      exportFileName: 'leaves_compliance_report',
      exportConfig: {
        enabled: true,
        formats: ['csv', 'excel'],
        excelExportType: 'LEAVES_COMPLIANCE',
        csvFilename: 'leaves-compliance.csv',
        excelFilename: 'leaves-compliance.xlsx'
      }
    };

    // Keep old tableConfig for backwards compatibility
    this.tableConfig = this.timeEntryTableConfig;
  }

  /**
   * Opens the issue details dialog when clicking on issue count
   */
  openIssueDetailsDialog(row: DefaulterDetail, issueType: 'TimeEntry' | 'Attendance' | 'Leaves'): void {
    // Use email as LDAP if ldap is not available (email format should match LDAP)
    const employeeLdap = row.ldap || row.email.split('@')[0];

    const dialogData: IssueDetailsDialogData = {
      employeeId: row.employeeId,
      employeeLdap: employeeLdap,
      employeeName: row.employeeName,
      issueType: issueType,
      startDate: this.startDate,
      endDate: this.endDate
    };

    this.dialog.open(IssueDetailsDialogComponent, {
      data: dialogData,
      width: '900px',
      maxWidth: '95vw',
      maxHeight: '90vh',
      panelClass: 'issue-details-dialog-container'
    });
  }

  loadAllData(): void {
    if (!this.dateRange) return;
    this.loading = true;

    // Use the already formatted date strings
    const start = this.startDate;
    const end = this.endDate;

    console.log('Loading data for:', { start, end, filters: this.filters, role: this.userRole });

    // Load Top 3
    this.reportsService.getTopDefaulters('TimeEntry', start, end, this.filters).subscribe({
      next: (res) => {
        this.topTimeEntryDefaulters = res.data || [];
        console.log('Top TimeEntry Defaulters:', this.topTimeEntryDefaulters);
      },
      error: (err) => console.error('Error loading Top TimeEntry Defaulters:', err)
    });

    this.reportsService.getTopDefaulters('Attendance', start, end, this.filters).subscribe({
      next: (res) => {
        this.topAttendanceDefaulters = res.data || [];
        console.log('Top Attendance Defaulters:', this.topAttendanceDefaulters);
      },
      error: (err) => console.error('Error loading Top Attendance Defaulters:', err)
    });

    this.reportsService.getTopDefaulters('Leaves', start, end, this.filters).subscribe({
      next: (res) => {
        this.topLeavesDefaulters = res.data || [];
        console.log('Top Leaves Defaulters:', this.topLeavesDefaulters);
      },
      error: (err) => console.error('Error loading Top Leaves Defaulters:', err)
    });

    // Load Detailed Lists
    this.reportsService.getAllDefaulters('TimeEntry', start, end, this.filters).subscribe({
      next: (res) => {
        this.timeEntryDefaulters = res.data || [];
        this.filteredTimeEntryDefaulters = this.timeEntryDefaulters;
        console.log('TimeEntry Defaulters:', this.timeEntryDefaulters);
      },
      error: (err) => console.error('Error loading TimeEntry Defaulters:', err)
    });

    this.reportsService.getAllDefaulters('Attendance', start, end, this.filters).subscribe({
      next: (res) => {
        this.attendanceDefaulters = res.data || [];
        this.filteredAttendanceDefaulters = this.attendanceDefaulters;
        console.log('Attendance Defaulters:', this.attendanceDefaulters);
      },
      error: (err) => console.error('Error loading Attendance Defaulters:', err)
    });

    this.reportsService.getAllDefaulters('Leaves', start, end, this.filters).subscribe({
      next: (res) => {
        this.leavesDefaulters = res.data || [];
        this.filteredLeavesDefaulters = this.leavesDefaulters;
        console.log('Leaves Defaulters:', this.leavesDefaulters);
      },
      error: (err) => console.error('Error loading Leaves Defaulters:', err)
    });

    this.loading = false;
  }

  applyFilters(): void {
    // Debounce or just reload
    this.loadAllData();
  }

  toggleAccordion(section: 'time' | 'attendance' | 'leaves'): void {
    if (section === 'time') this.isTimeEntryOpen = !this.isTimeEntryOpen;
    if (section === 'attendance') this.isAttendanceOpen = !this.isAttendanceOpen;
    if (section === 'leaves') this.isLeavesOpen = !this.isLeavesOpen;
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

  goBack(): void {
    this.router.navigate(['/reports']);
  }
}
