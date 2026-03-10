import { Component, OnInit, ViewChild } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { LeaveService } from 'src/app/services/leave.service';
import { NotificationService } from 'src/app/shared/notification.service';
import { MatDialog } from '@angular/material/dialog';
import { DocumentPreviewDialogComponent } from './document-preview-dialog/document-preview-dialog.component';
import { MatButtonToggleChange } from '@angular/material/button-toggle';
import * as Papa from 'papaparse';
import { FormGroup, FormControl } from '@angular/forms';
import { ConfirmationDialogComponent } from 'src/app/confirm-dialog/confirmation-dialog.component';
import { SelectionModel } from '@angular/cdk/collections';
import { CategoryUpdateDialogComponent } from './category-update-dialog/category-update-dialog.component';

@Component({
  selector: 'app-requests',
  templateUrl: './requests.component.html',
  styleUrls: ['./requests.component.css']
})
export class RequestsComponent implements OnInit {
  displayedColumns: string[] = [
    'timestamp', 'ldap', 'name', 'requestType', 'leaveType', 'leaveDetails', 'leaveCategory',
    'startDate', 'endDate', 'duration', 'document', 'approvingLead', 'changedBy', 'reason', 'backupInfo', 'status',
  ];
  columnDisplayNames: Record<string, string> = {
    timestamp: 'Requested On',
    ldap: 'LDAP',
    name: 'Name',
    requestType: 'Request Category',
    leaveType: 'Request Type',
    leaveDetails: 'Request Breakdown',
    startDate: 'Start Date',
    endDate: 'End Date',
    duration: 'Duration',
    leaveCategory: 'Planning Type',
    approvingLead: 'Approving Lead',
    changedBy: 'Approved By',
    backupInfo: 'Backup Info',
    reason: 'Reason',
    status: 'Status',
    document: 'Document',
  };

  columnSearchText: string = '';
  allColumnsSelected: boolean = false;
  showColumnToggle: boolean = false;
  showColumnFilters = false;
  userRole: string = '';
  userName: string = '';
  processedDataSource: MatTableDataSource<any> = new MatTableDataSource();
  processedRequests: any[] = []; // Raw data

  currentFilterMenuState: {
    columnKey: string | null,
    searchText: string,
    tempSelected: Set<any>
  } = { columnKey: null, searchText: '', tempSelected: new Set() };

  filteredMenuOptions: any[] = [];
  appliedFilters: Record<string, Set<any>> = {};

  lastAppliedStartDate?: string;
  lastAppliedEndDate?: string;

  // if you have a toggle for direct reports
  showMyDirectReports: boolean = true;

  // Tab DataSources
  pendingDataSource: MatTableDataSource<any> = new MatTableDataSource();
  processedApprovedDataSource = new MatTableDataSource<any>();
  processedRejectedDataSource = new MatTableDataSource<any>();
  processedRevokedDataSource = new MatTableDataSource<any>();

  // Filters
  processedStatuses: string[] = ['APPROVED', 'REJECTED', 'REVOKED'];
  selectedStatus: string = '';

  dateRange = new FormGroup({
    start: new FormControl<Date | null>(null),
    end: new FormControl<Date | null>(null)
  });

  // ViewChilds
  @ViewChild('pendingPaginator') pendingPaginator!: MatPaginator;
  @ViewChild('processedPaginator') processedPaginator!: MatPaginator;
  @ViewChild('approvedPaginator') approvedPaginator!: MatPaginator;
  @ViewChild('rejectedPaginator') rejectedPaginator!: MatPaginator;
  @ViewChild('revokedPaginator') revokedPaginator!: MatPaginator;
  @ViewChild('pendingSort') pendingSort!: MatSort;
  @ViewChild('processedSort') processedSort!: MatSort;

  // Default to Pending Requests tab (index 1)
  selectedTabIndex: number = 1;

  constructor(
    private leaveService: LeaveService,
    private notificationService: NotificationService,
    private dialog: MatDialog,
  ) { }

  ngOnInit(): void {
    this.userRole = (localStorage.getItem('role') || '').toUpperCase();
    this.userName = (localStorage.getItem('username') || '').toLowerCase();
    this.fetchPendingRequests();
    this.fetchProcessedRequests();
  }

  isLeadOrManager(): boolean {
    return this.userRole === 'LEAD' || this.userRole === 'MANAGER';
  }

  toggleOnBehalfMode(isChecked: boolean) {
    this.showMyDirectReports = isChecked;
    console.log('Request Toggle changed:', isChecked);

    // 🔄 Refresh data with current date filter
    this.fetchPendingRequests(true);
    this.fetchProcessedRequests(true);
  }

  ngAfterViewInit(): void {
    if (this.processedApprovedDataSource) {
      this.processedApprovedDataSource.paginator = this.approvedPaginator;
      this.processedApprovedDataSource.sort = this.processedSort;
      this.setupSortingDataAccessor(this.processedApprovedDataSource);
    }
    if (this.processedRejectedDataSource) {
      this.processedRejectedDataSource.paginator = this.rejectedPaginator;
      this.processedRejectedDataSource.sort = this.processedSort;
      this.setupSortingDataAccessor(this.processedRejectedDataSource);
    }
    if (this.processedRevokedDataSource) {
      this.processedRevokedDataSource.paginator = this.revokedPaginator;
      this.processedRevokedDataSource.sort = this.processedSort;
      this.setupSortingDataAccessor(this.processedRevokedDataSource);
    }

    this.pendingDataSource.paginator = this.pendingPaginator;
    this.pendingDataSource.sort = this.pendingSort;
    this.setupSortingDataAccessor(this.pendingDataSource);
  }

  /**
   * Setup custom sorting data accessor to handle column name mappings
   */
  setupSortingDataAccessor(dataSource: MatTableDataSource<any>): void {
    dataSource.sortingDataAccessor = (item, property) => {
      switch (property) {
        case 'name':
          return item.requestorName || '';
        case 'requestType':
          return item.applicationType || '';
        case 'duration':
          return item.lvWfhDuration || 0;
        case 'leaveType':
          return item.leaveType || '';
        case 'startDate':
          return item.startDate ? new Date(item.startDate).getTime() : 0;
        case 'endDate':
          return item.endDate ? new Date(item.endDate).getTime() : 0;
        case 'timestamp':
          return item.timestamp ? new Date(item.timestamp).getTime() : 0;
        case 'status':
          return item.status || '';
        default:
          return item[property] || '';
      }
    };
  }

  openPreviewDialog(row: any): void {
    const baseUrl = 'https://teamsphere.in/'; // Use for Main Production

    // const baseUrl = 'http://localhost:8080/';  URL for Development Purposes

    const documentPath = row.documentPath || row.document || null;

    this.dialog.open(DocumentPreviewDialogComponent, {
      width: '600px',
      data: {
        reason: row.reason,
        documentPath: documentPath ? baseUrl + documentPath : null
      }
    });
  }

  getLastWeekStartDate(): Date {
    const date = new Date();
    date.setDate(date.getDate() - 7);
    return date;
  }

  fetchPendingRequests(isFilter: boolean = false): void {
    const selectedStart = this.dateRange?.get('start')?.value;
    const selectedEnd = this.dateRange?.get('end')?.value;

    let startDate: string;
    let endDate: string;

    const today = new Date();

    if (isFilter && selectedStart && selectedEnd) {
      // User explicitly applied a filter
      startDate = this.formatDate(selectedStart);
      endDate = this.formatDate(selectedEnd);

      this.lastAppliedStartDate = startDate;
      this.lastAppliedEndDate = endDate;
    } else if (this.lastAppliedStartDate && this.lastAppliedEndDate) {
      // Reuse last filter if exists
      startDate = this.lastAppliedStartDate;
      endDate = this.lastAppliedEndDate;
    } else if (
      this.userRole === 'LEAD' ||
      this.userRole === 'MANAGER' ||
      this.userRole === 'ADMIN_OPS_MANAGER'
    ) {
      // Default: Show ALL pending requests (Wide range)
      // This covers historical and future pending requests initially
      const past = new Date(today);
      past.setFullYear(today.getFullYear() - 1); // 1 year back
      const future = new Date(today);
      future.setFullYear(today.getFullYear() + 1); // 1 year forward

      startDate = this.formatDate(past);
      endDate = this.formatDate(future);
    } else {
      // Default for USER (normal employee) → also wide range if they have access
      const past = new Date(today);
      past.setFullYear(today.getFullYear() - 1);
      const future = new Date(today);
      future.setFullYear(today.getFullYear() + 1);

      startDate = this.formatDate(past);
      endDate = this.formatDate(future);
    }

    this.leaveService.getPendingRequestsForLead(startDate, endDate, this.showMyDirectReports).subscribe({
      next: (requests) => {
        // Sort by timestamp descending (latest first)
        const sortedRequests = requests.sort((a: any, b: any) => {
          const dateA = new Date(a.timestamp).getTime();
          const dateB = new Date(b.timestamp).getTime();
          return dateB - dateA; // Descending order
        });

        this.pendingDataSource = new MatTableDataSource(sortedRequests);
        this.pendingDataSource.paginator = this.pendingPaginator;
        this.pendingDataSource.sort = this.pendingSort;
        this.setupSortingDataAccessor(this.pendingDataSource);

        // Set default sort to timestamp descending
        if (this.pendingSort) {
          this.pendingSort.active = 'timestamp';
          this.pendingSort.direction = 'desc';
        }
      },
      error: () => {
        this.notificationService.showNotification({
          type: 'error',
          message: 'Failed to fetch pending requests.'
        });
      }
    });
  }


  private formatDate(date: Date): string {
    // Convert to local date only (ignore timezone shift)
    const year = date.getFullYear();
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const day = date.getDate().toString().padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  canApprove(request: any): boolean {
    const isLead = this.userRole === 'LEAD';
    const isWFH = request.applicationType === 'Work From Home';
    const isDsheoran = this.userName === 'dsheoran';

    // Compute duration in days using start and end dates
    const start = new Date(request.startDate);
    const end = new Date(request.endDate);
    const durationInDays = Math.floor((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1;

    const isLongWFH = durationInDays >= 3;

    // Leads cannot approve any WFH
    if (isLead && isWFH) return false;

    // Only dsheoran can approve WFH of 3+ days
    if (isWFH && isLongWFH && !isDsheoran) return false;

    return true;
  }

  fetchProcessedRequests(isFilter: boolean = false): void {
    const selectedStart = this.dateRange?.get('start')?.value;
    const selectedEnd = this.dateRange?.get('end')?.value;

    let startDate: string;
    let endDate: string;

    const today = new Date();

    if (isFilter && selectedStart && selectedEnd) {
      startDate = this.formatDate(selectedStart);
      endDate = this.formatDate(selectedEnd);

      this.lastAppliedStartDate = startDate;
      this.lastAppliedEndDate = endDate;
    } else if (this.lastAppliedStartDate && this.lastAppliedEndDate) {
      startDate = this.lastAppliedStartDate;
      endDate = this.lastAppliedEndDate;
    } else if (
      this.userRole === 'LEAD' ||
      this.userRole === 'MANAGER' ||
      this.userRole === 'ADMIN_OPS_MANAGER'
    ) {
      startDate = this.formatDate(today);
      endDate = this.formatDate(today);
    } else {
      endDate = this.formatDate(today);
      const pastWeek = new Date(today);
      pastWeek.setDate(today.getDate() - 6);
      startDate = this.formatDate(pastWeek);
    }

    this.leaveService.getProcessedRequestsForLead(startDate, endDate, this.showMyDirectReports).subscribe({
      next: (requests) => {
        // Sort by timestamp descending (latest first)
        const sortedRequests = (requests || []).sort((a: any, b: any) => {
          const dateA = new Date(a.timestamp).getTime();
          const dateB = new Date(b.timestamp).getTime();
          return dateB - dateA; // Descending order
        });

        this.processedRequests = sortedRequests;
        this.processedApprovedDataSource.data = this.processedRequests.filter(r => r.status === 'APPROVED');
        this.processedRejectedDataSource.data = this.processedRequests.filter(r => r.status === 'REJECTED');
        this.processedRevokedDataSource.data = this.processedRequests.filter(r => r.status === 'REVOKED');

        // Setup sorting for all data sources
        this.setupSortingDataAccessor(this.processedApprovedDataSource);
        this.setupSortingDataAccessor(this.processedRejectedDataSource);
        this.setupSortingDataAccessor(this.processedRevokedDataSource);

        // Load audit history status for visible requests
        this.loadAuditHistoryStatus(this.processedRequests);
        // Only set default on initial load, preserve selection on refresh
        if (!this.selectedStatus) {
          this.selectedStatus = 'APPROVED';
        }
        setTimeout(() => this.updatePaginatorAndSort(), 0);
      },
      error: () => {
        this.notificationService.showNotification({
          type: 'error',
          message: 'Failed to fetch processed requests.'
        });
      }
    });
  }

  openFilterMenu(columnKey: string, trigger: any) {
    this.currentFilterMenuState = {
      columnKey,
      searchText: '',
      tempSelected: new Set(this.appliedFilters[columnKey] || [])
    };
    this.filteredMenuOptions = this.getUniqueColumnValues(columnKey);
  }

  isFilterActive(columnKey: string): boolean {
    return !!(this.appliedFilters[columnKey] && this.appliedFilters[columnKey].size > 0);
  }

  // Get unique values for a column
  getUniqueColumnValues(columnKey: string): any[] {
    let data = [...this.getProcessedDataSource().data, ...this.pendingDataSource.data];
    const values = Array.from(new Set(data.map(row => row[columnKey] || 'NA')));
    return values.filter(v =>
      v.toString().toLowerCase().includes(this.currentFilterMenuState.searchText.toLowerCase())
    );
  }

  toggleTempSelection(value: any, checked: boolean) {
    if (checked) this.currentFilterMenuState.tempSelected.add(value);
    else this.currentFilterMenuState.tempSelected.delete(value);
  }

  // Check if value is temp selected
  isTempSelected(value: any): boolean {
    return this.currentFilterMenuState.tempSelected.has(value);
  }

  downloadCSV(type: 'processed' | 'pending') {
    let dataSource;

    if (type === 'processed') {
      dataSource = this.getProcessedDataSource(); // use selectedStatus table
    } else {
      dataSource = this.pendingDataSource;
    }

    const csv = Papa.unparse(dataSource.filteredData || dataSource.data);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);

    // Dynamic filename
    const fileName =
      type === 'processed'
        ? `processed_requests_${this.selectedStatus.toLowerCase()}.csv`
        : 'pending_requests.csv';

    link.setAttribute('download', fileName);
    link.click();
  }

  getFilteredColumns(): string[] {
    if (!this.columnSearchText) {
      return this.displayedColumns;
    }
    return this.displayedColumns.filter(col =>
      this.columnDisplayNames[col]?.toLowerCase().includes(this.columnSearchText.toLowerCase())
    );
  }

  isColumnDisplayed(column: string): boolean {
    return this.displayedColumns.includes(column);
  }

  toggleColumn(column: string) {
    if (this.displayedColumns.includes(column)) {
      this.displayedColumns = this.displayedColumns.filter(col => col !== column);
    } else {
      this.displayedColumns.push(column);
    }
  }

  toggleAllColumns(checked: boolean) {
    if (checked) {
      this.displayedColumns = Object.keys(this.columnDisplayNames);
    } else {
      this.displayedColumns = [];
    }
    this.allColumnsSelected = checked;
  }

  toggleSelectAllTemp(checked: boolean) {
    if (checked) {
      this.getUniqueColumnValues(this.currentFilterMenuState.columnKey!)
        .forEach(v => this.currentFilterMenuState.tempSelected.add(v));
    } else {
      this.currentFilterMenuState.tempSelected.clear();
    }
  }

  isAllTempSelected(): boolean {
    const all = this.getUniqueColumnValues(this.currentFilterMenuState.columnKey!);
    return all.length > 0 && all.every(v => this.currentFilterMenuState.tempSelected.has(v));
  }

  isSomeTempSelected(): boolean {
    const all = this.getUniqueColumnValues(this.currentFilterMenuState.columnKey!);
    return this.currentFilterMenuState.tempSelected.size > 0 && !this.isAllTempSelected();
  }

  // Apply filter
  onFilterApplied() {
    if (this.currentFilterMenuState.columnKey) {
      this.appliedFilters[this.currentFilterMenuState.columnKey] =
        new Set(this.currentFilterMenuState.tempSelected);
    }
    this.applyFilters();
  }

  // Clear filter
  clearColumnFilter() {
    if (this.currentFilterMenuState.columnKey) {
      delete this.appliedFilters[this.currentFilterMenuState.columnKey];
    }
    this.applyFilters();
  }

  // Apply filters across datasources
  applyFilters() {
    const filterFn = (data: any) => {
      return Object.keys(this.appliedFilters).every(col => {
        const allowed = this.appliedFilters[col];
        return allowed.size === 0 || allowed.has(data[col] || 'NA');
      });
    };

    this.getProcessedDataSource().filterPredicate = filterFn;
    this.getProcessedDataSource().filter = Math.random().toString();

    this.pendingDataSource.filterPredicate = filterFn;
    this.pendingDataSource.filter = Math.random().toString();
  }

  updatePaginatorAndSort(): void {
    if (this.selectedStatus === 'APPROVED' && this.processedApprovedDataSource) {
      this.processedApprovedDataSource.paginator = this.approvedPaginator;
      this.processedApprovedDataSource.sort = this.processedSort;
    } else if (this.selectedStatus === 'REJECTED' && this.processedRejectedDataSource) {
      this.processedRejectedDataSource.paginator = this.rejectedPaginator;
      this.processedRejectedDataSource.sort = this.processedSort;
    } else if (this.selectedStatus === 'REVOKED' && this.processedRevokedDataSource) {
      this.processedRevokedDataSource.paginator = this.revokedPaginator;
      this.processedRevokedDataSource.sort = this.processedSort;
    }
  }

  getProcessedDataSource(): MatTableDataSource<any> {
    switch (this.selectedStatus) {
      case 'APPROVED': return this.processedApprovedDataSource;
      case 'REJECTED': return this.processedRejectedDataSource;
      case 'REVOKED': return this.processedRevokedDataSource;
      default: return new MatTableDataSource();
    }
  }

  approveRequest(row: any): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '420px',
      data: {
        title: 'Approve Request',
        message: `Are you sure you want to approve the leave for ${row.requestorName}?\n\nℹ️ Note: All associated time entries will be automatically approved.`,
        showCommentField: true,
        commentLabel: 'Approval Comment (optional)',
        color: 'primary',
        confirmButtonText: 'Approve'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (!result?.confirmed) return; // user cancelled

      const payload = {
        ...row,
        comment: result.comment || '',
        status: 'APPROVED'
      };

      this.leaveService.approveRequest(payload).subscribe({
        next: () => {
          this.notificationService.showSuccess('Leave Approved Successfully');
          this.fetchPendingRequests();
          this.fetchProcessedRequests();
        },
        error: (error) => {
          const errMsg = error?.error?.message || 'Failed to approve request.';
          this.notificationService.showError(errMsg);
        }
      });
    });
  }

  rejectRequest(row: any): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '420px',
      data: {
        title: 'Reject Request',
        message: `Are you sure you want to reject the leave for ${row.requestorName}?\n\n⚠️ Warning: All associated auto-generated time entries will be DELETED.\n\nThe employee will be able to re-request leave for these dates.`,
        showCommentField: true,
        commentLabel: 'Rejection Comment (required)',
        color: 'warn',
        confirmButtonText: 'Reject'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (!result?.confirmed) return;

      if (!result.comment || result.comment.trim() === '') {
        this.notificationService.showError('Rejection comment is required.');
        return;
      }

      const payload = {
        ...row,
        comment: result.comment,
        status: 'REJECTED'
      };

      this.leaveService.rejectRequest(payload).subscribe({
        next: () => {
          this.notificationService.showSuccess('Request Rejected Successfully');
          this.fetchPendingRequests();
          this.fetchProcessedRequests();
        },
        error: (error) => {
          const errMsg = error?.error?.message || 'Failed to reject request.';
          this.notificationService.showError(errMsg);
        }
      });
    });
  }

  revokeRequest(row: any): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '420px',
      data: {
        title: 'Revoke Request',
        message: `Are you sure you want to revoke the leave request?\n\n⚠️ Important: Associated time entries have already been approved and will NOT be automatically deleted. Please manually delete them from the Time Entries section after revoking this request.`,
        showCommentField: true,
        commentLabel: 'Revoke Reason (required)',
        color: 'warn',
        confirmButtonText: 'Revoke'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (!result?.confirmed) return;

      if (!result.comment || result.comment.trim() === '') {
        this.notificationService.showError('Revoke reason is required.');
        return;
      }

      const payload = {
        ...row,
        comment: result.comment,
        status: 'REVOKED'
      };

      this.leaveService.revokeRequest(payload).subscribe({
        next: () => {
          this.notificationService.showSuccess('Request Revoked Successfully');
          this.fetchPendingRequests();
          this.fetchProcessedRequests();
        },
        error: (error) => {
          const errMsg = error?.error?.message || 'Failed to revoke request.';
          this.notificationService.showError(errMsg);
        }
      });
    });
  }

  openCategoryUpdateDialog(row: any): void {
    if (!row) return;

    const dialogRef = this.dialog.open(CategoryUpdateDialogComponent, {
      width: '640px',
      maxWidth: '90vw',
      height: 'auto',
      maxHeight: '90vh',
      panelClass: 'category-update-dialog',
      data: {
        mode: 'single',
        rows: [row],
        userRole: this.userRole
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (!result || !result.confirmed) return;

      const payload = {
        ids: result.payload?.ids || [row.id],
        category: result.payload?.category,
        reason: result.payload?.reason || ''
      };

      if (payload.ids.length === 1) {
        // SINGLE
        const requestBody = {
          id: payload.ids[0],
          category: payload.category.toUpperCase(),
          reason: payload.reason
        };

        this.leaveService.updateLeaveCategory(requestBody)
          .subscribe({
            next: () => {
              this.notificationService.showSuccess('Category updated successfully');
              this.fetchPendingRequests();
              this.fetchProcessedRequests();
            },
            error: err => {
              this.notificationService.showError(err.error?.message || 'Failed to update category');
            }
          });

      } else {
        // BULK
        const requestBody = {
          ids: payload.ids,
          category: payload.category.toUpperCase(),
          reason: payload.reason
        };

        this.leaveService.bulkUpdateCategory(requestBody)
          .subscribe({
            next: resp => {
              this.notificationService.showSuccess('Bulk category update completed successfully');
              this.fetchPendingRequests();
              this.fetchProcessedRequests();
            },
            error: err => {
              this.notificationService.showError(err.error?.message || 'Bulk update failed');
            }
          });
      }
    });
  }

  applyDateFilter() {
    const { start, end } = this.dateRange.value;

    if (!start || !end) {
      // If user clears filter → reset API data to default
      this.fetchPendingRequests(false);
      this.fetchProcessedRequests(false);
      return;
    }

    // 👇 Call APIs with filter enabled
    this.fetchPendingRequests(true);
    this.fetchProcessedRequests(true);
  }


  onStatusChange(event: MatButtonToggleChange): void {
    this.selectedStatus = event.value;
    setTimeout(() => this.updatePaginatorAndSort(), 0); // fixes paginator not binding
  }

  isPastEndDate(endDate: string): boolean {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const leaveEndDate = new Date(endDate);
    leaveEndDate.setHours(0, 0, 0, 0);
    return leaveEndDate < today;
  }

  // Cache of record IDs that have audit history (for highlighting)
  private recordsWithAuditHistory = new Set<number>();

  /**
   * Check if a request has audit history (has been modified)
   */
  hasAuditHistory(request: any): boolean {
    return this.recordsWithAuditHistory.has(request.id);
  }

  /**
   * Load audit history status for all visible requests
   * Only highlights rows that have CATEGORY_UPDATE entries
   */
  private loadAuditHistoryStatus(requests: any[]): void {
    // Only check for leads/managers/admins
    if (!this.isLeadOrManager()) {
      return;
    }

    requests.forEach(request => {
      this.leaveService.getAuditHistory(request.id).subscribe({
        next: (response) => {
          if (response && response.data && response.data.length > 0) {
            // Only highlight if there's a CATEGORY_UPDATE action
            const hasCategoryUpdate = response.data.some(
              (entry: any) => entry.actionType === 'CATEGORY_UPDATE'
            );
            if (hasCategoryUpdate) {
              this.recordsWithAuditHistory.add(request.id);
            }
          }
        },
        error: () => {
          // Silently ignore errors
        }
      });
    });
  }

  /**
   * Open audit history dialog for a leave request
   */
  viewAuditHistory(row: any): void {
    import('./vunno-audit-dialog/vunno-audit-dialog.component').then(m => {
      this.dialog.open(m.VunnoAuditDialogComponent, {
        width: '700px',
        data: {
          vunnoResponseId: row.id,
          requestorName: row.requestorName,
          leaveType: row.leaveType
        }
      });
    });
  }
}