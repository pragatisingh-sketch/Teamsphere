import { Component, Input, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { FormControl, FormGroup } from '@angular/forms';
import { saveAs } from 'file-saver';
import * as XLSX from 'xlsx';
import { LeaveService } from 'src/app/services/leave.service';
import { NotificationService } from 'src/app/shared/notification.service';
import { ConfirmationDialogComponent } from 'src/app/confirm-dialog/confirmation-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from "@angular/material/icon";
import { EditLeaveRequestComponent } from './edit-leave-request/edit-leave-request.component';
import { DocumentPreviewDialogComponent } from '../requests/document-preview-dialog/document-preview-dialog.component';

@Component({
  selector: 'app-history',
  templateUrl: './history.component.html',
  styleUrls: ['./history.component.css'],
})
export class HistoryComponent implements OnInit {
  @Input() ldap!: string;
  @Input() leaveHistory: any[] = [];
  @Input() userRole?: string;
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(private leaveService: LeaveService, private notificationService: NotificationService,
    private dialog: MatDialog
  ) { }

  dataSource = new MatTableDataSource<any>();
  displayedColumns: string[] = [
    'timestamp',
    'ldap',
    'applicationType',
    'leaveType',
    'leaveDetails',
    'leaveCategory',
    'duration',
    'startDate',
    'endDate',
    'shiftCodeAtRequestTime',
    'documentPath',
    'oooProof',
    'backupInfo',
    'reason',
    'status',
    'actions'
  ]; allColumns: string[] = [...this.displayedColumns];

  columnDisplayNames: { [key: string]: string } = {
    timestamp: 'Requested On',
    ldap: 'LDAP',
    leaveType: 'Request Type',
    leaveDetails: 'Request Breakdown',
    leaveCategory: 'Planning Type',
    startDate: 'Start Date',
    endDate: 'End Date',
    applicationType: 'Request Category',
    duration: 'Duration',
    shiftCodeAtRequestTime: 'Shift',
    oooProof: 'OOO SS',
    reason: 'Reason',
    documentPath: 'Document',
    status: 'Status',
    backupInfo: 'Backup Info',
    actions: 'Actions'
  };


  columnSearchText = '';
  showColumnFilters = false;
  showColumnToggle = false;

  // Column filter infrastructure (matching Requests tab)
  currentFilterMenuState: {
    columnKey: string | null,
    searchText: string,
    tempSelected: Set<any>
  } = { columnKey: null, searchText: '', tempSelected: new Set() };

  filteredMenuOptions: any[] = [];
  appliedFilters: Record<string, Set<any>> = {};

  dateRange = new FormGroup({
    start: new FormControl<Date | null>(null),
    end: new FormControl<Date | null>(null)
  });

  ngOnInit(): void {
    this.fetchLeaveHistory(this.ldap);
  }

  ngAfterViewInit() {
    if (this.paginator) {
      this.dataSource.paginator = this.paginator;
    }
    if (this.sort) {
      this.dataSource.sort = this.sort;
    }
  }

  openPreviewDialog(row: any): void {
    const baseUrlProd = 'https://teamsphere.in/'; //URL for Development Purposes

    const baseUrl = 'http://localhost:8080/'; // Use environment-specific base URL

    const documentPath = row.documentPath || row.document || null;

    this.dialog.open(DocumentPreviewDialogComponent, {
      width: '600px',
      data: {
        reason: row.reason,
        documentPath: documentPath ? baseUrl + documentPath : null
      }
    });
  }

  ngOnChanges(): void {
    if (this.ldap) {
      this.fetchLeaveHistory(this.ldap);
    }
  }

  private formatTimestamp(timestamp: string): string {
    if (!timestamp) return '';
    const date = new Date(timestamp);

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');

    return `${year}-${month}-${day} ${hours}:${minutes}`;
  }


  fetchLeaveHistory(ldap: string): void {
    console.log("Inside the history component");

    this.leaveService.getUserLeaveHistory(ldap).subscribe({
      next: (data: any[]) => {
        console.log("Data Of history ", data)
        this.leaveHistory = data.map(item => ({
          id: item.id,
          timestamp: this.formatTimestamp(item.timestamp),
          timestampRaw: item.timestamp, // Keep raw timestamp for sorting
          ldap: ldap,
          status: item.status,
          leaveType: item.leaveType,
          leaveDetails: item.leaveDetails || 'N/A',
          leaveCategory: item.leaveCategory && item.leaveCategory.trim() !== '' ? item.leaveCategory : 'N/A',
          applicationType: item.applicationType,
          startDate: item.startDate ? new Date(item.startDate) : null,
          endDate: item.endDate ? new Date(item.endDate) : null,
          shiftCodeAtRequestTime: item.shiftCodeAtRequestTime || '',
          duration: item.duration,
          approver: item.approver,
          reason: item.reason,
          oooProof: item.oooProof,
          documentPath: item.documentPath,
          backupInfo: item.backupInfo,
        }));

        // Sort by timestamp descending (latest first)
        this.leaveHistory.sort((a, b) => {
          const dateA = new Date(a.timestampRaw).getTime();
          const dateB = new Date(b.timestampRaw).getTime();
          return dateB - dateA; // Descending order
        });

        this.dataSource.data = this.leaveHistory;

        if (this.sort) {
          this.dataSource.sort = this.sort;
          // Set default sort to timestamp descending
          this.sort.active = 'timestamp';
          this.sort.direction = 'desc';
        }
        if (this.paginator) {
          this.paginator.firstPage();
        }
      },
      error: (error) => {
        console.error('Error fetching leave history:', error);
      }
    });
  }


  applyGlobalFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value.trim().toLowerCase();
    this.dataSource.filter = filterValue;
  }

  applyDateFilter(): void {
    const start = this.dateRange.get('start')?.value;
    const end = this.dateRange.get('end')?.value;

    if (!start && !end) {
      this.dataSource.data = this.leaveHistory;
      this.applyFilters();
      return;
    }

    // Filter to show leaves that overlap with the selected date range
    const filtered = this.leaveHistory.filter(entry => {
      const leaveStart = new Date(entry.startDate);
      const leaveEnd = new Date(entry.endDate);

      // Set time to start of day for accurate comparison
      leaveStart.setHours(0, 0, 0, 0);
      leaveEnd.setHours(23, 59, 59, 999);

      const filterStart = start ? new Date(start) : null;
      const filterEnd = end ? new Date(end) : null;

      if (filterStart) filterStart.setHours(0, 0, 0, 0);
      if (filterEnd) filterEnd.setHours(23, 59, 59, 999);

      // Check if leave period overlaps with filter range
      // Leave overlaps if: leaveStart <= filterEnd AND leaveEnd >= filterStart
      if (filterStart && filterEnd) {
        return leaveStart <= filterEnd && leaveEnd >= filterStart;
      } else if (filterStart) {
        return leaveEnd >= filterStart;
      } else if (filterEnd) {
        return leaveStart <= filterEnd;
      }

      return true;
    });

    this.dataSource.data = filtered;
    this.applyFilters();
  }

  toggleColumnFilters(): void {
    this.showColumnFilters = !this.showColumnFilters;
  }

  toggleAllColumns(checked: boolean): void {
    this.displayedColumns = checked ? [...this.allColumns] : [];
  }

  isColumnDisplayed(column: string): boolean {
    return this.displayedColumns.includes(column);
  }

  toggleColumn(column: string): void {
    const index = this.displayedColumns.indexOf(column);
    if (index >= 0) {
      this.displayedColumns.splice(index, 1);
    } else {
      this.displayedColumns.push(column);
    }
  }

  getFilteredColumns(): string[] {
    if (!this.columnSearchText.trim()) return this.allColumns;
    const searchLower = this.columnSearchText.toLowerCase();
    return this.allColumns.filter(column =>
      this.columnDisplayNames[column].toLowerCase().includes(searchLower)
    );
  }

  downloadCSV(): void {
    const dataToExport = this.dataSource.filteredData || this.dataSource.data;

    const exportData = dataToExport.map(item => ({
      'Requested On': item.timestamp,
      'LDAP': item.ldap,
      'Application Type': item.applicationType,
      'Leave Type': item.leaveType,
      'Leave Category': item.leaveCategory,
      'Duration': item.duration,
      'Start Date': item.startDate ? new Date(item.startDate).toLocaleDateString() : '',
      'Start Time': item.startTime,
      'End Date': item.endDate ? new Date(item.endDate).toLocaleDateString() : '',
      'End Time': item.endTime,
      'Shift': item.shiftCodeAtRequestTime,
      'Backup Info': item.backupInfo,
      'Reason': item.reason,
      'Status': item.status
    }));

    const worksheet = XLSX.utils.json_to_sheet(exportData);
    const workbook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, worksheet, 'Leave History');

    const excelBuffer: any = XLSX.write(workbook, { bookType: 'csv', type: 'array' });
    const blob = new Blob([excelBuffer], { type: 'text/csv;charset=utf-8;' });
    saveAs(blob, `leave_history_${new Date().toISOString().split('T')[0]}.csv`);
  }

  editRequest(entry: any): void {
    console.log("Entry ", entry);
    if (entry.status !== 'PENDING') return;

    const dialogRef = this.dialog.open(EditLeaveRequestComponent, {
      width: '700px',
      data: {
        id: entry.id,
        ldap: entry.ldap,
        approver: entry.approver,
        status: entry.status,
        applicationType: entry.applicationType,
        duration: entry.duration,
        leaveType: entry.leaveType,
        startDate: entry.startDate,
        endDate: entry.endDate,
        reason: entry.reason,
        oooProof: entry.oooProof,
        backupInfo: entry.backupInfo,
        documentPath: entry.documentPath // for WFH
      }
    });

    dialogRef.afterClosed().subscribe(updatedData => {
      if (updatedData) {
        const updatedRequest = { ...entry, ...updatedData };

        this.leaveService.updateLeaveRequest(updatedRequest).subscribe({
          next: () => {
            this.notificationService.showNotification({
              type: 'success',
              message: 'Request updated successfully.'
            });
            this.fetchLeaveHistory(this.ldap);
          },
          error: () => {
            this.notificationService.showNotification({
              type: 'error',
              message: 'Failed to update leave request.'
            });
          }
        });
      }
    });
  }

  deleteRequest(entry: any): void {
    if (entry.status !== 'PENDING') return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Confirm Deletion',
        message: `Are you sure you want to delete request from ${entry.startDate.toDateString()} to ${entry.endDate.toDateString()}?`,
        color: 'warn',
        confirmButtonText: 'Delete'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.leaveService.deleteLeaveRequestById(entry.id).subscribe({
          next: () => {
            this.notificationService.showNotification({
              type: 'success',
              message: 'Leave request deleted successfully.'
            });
            this.fetchLeaveHistory(this.ldap); // Refresh list
          },
          error: () => {
            this.notificationService.showNotification({
              type: 'error',
              message: 'Failed to delete leave request.'
            });
          }
        });
      }
    });
  }

  /**
   * Open audit history dialog for a leave request
   */
  viewAuditHistory(row: any): void {
    import('../requests/vunno-audit-dialog/vunno-audit-dialog.component').then(m => {
      this.dialog.open(m.VunnoAuditDialogComponent, {
        width: '700px',
        data: {
          vunnoResponseId: row.id,
          requestorName: row.ldap,
          leaveType: row.leaveType
        }
      });
    });
  }

  // Column filter methods (matching Requests tab)
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

  getUniqueColumnValues(columnKey: string): any[] {
    const data = this.dataSource.data;
    const values = Array.from(new Set(data.map(row => row[columnKey] || 'NA')));
    return values.filter(v =>
      v.toString().toLowerCase().includes(this.currentFilterMenuState.searchText.toLowerCase())
    );
  }

  toggleTempSelection(value: any, checked: boolean) {
    if (checked) this.currentFilterMenuState.tempSelected.add(value);
    else this.currentFilterMenuState.tempSelected.delete(value);
  }

  isTempSelected(value: any): boolean {
    return this.currentFilterMenuState.tempSelected.has(value);
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

  onFilterApplied() {
    if (this.currentFilterMenuState.columnKey) {
      this.appliedFilters[this.currentFilterMenuState.columnKey] =
        new Set(this.currentFilterMenuState.tempSelected);
    }
    this.applyFilters();
  }

  clearColumnFilter() {
    if (this.currentFilterMenuState.columnKey) {
      delete this.appliedFilters[this.currentFilterMenuState.columnKey];
    }
    this.applyFilters();
  }

  applyFilters() {
    const filterFn = (data: any) => {
      return Object.keys(this.appliedFilters).every(col => {
        const allowed = this.appliedFilters[col];
        return allowed.size === 0 || allowed.has(data[col] || 'NA');
      });
    };

    this.dataSource.filterPredicate = filterFn;
    this.dataSource.filter = Math.random().toString();
  }

  get allColumnsSelected(): boolean {
    return this.displayedColumns.length === this.allColumns.length;
  }
}
