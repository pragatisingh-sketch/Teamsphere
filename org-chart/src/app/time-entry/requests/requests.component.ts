import { Component, OnInit, ViewChild, AfterViewInit, ElementRef, HostListener, OnDestroy } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatDialog } from '@angular/material/dialog';
import { MatMenuTrigger } from '@angular/material/menu';
import { HttpClient } from '@angular/common/http';
import { NotificationService } from '../../shared/notification.service';
import { ConfirmationDialogComponent } from '../../confirm-dialog/confirmation-dialog.component';
import { environment } from '../../../environments/environment';
import { MatSelectChange } from '@angular/material/select';
import { Router } from '@angular/router';
import { SelectionModel } from '@angular/cdk/collections';
import { saveAs } from 'file-saver';
import * as XLSX from 'xlsx';
import { TimeEntryFormComponent } from '../time-entry-form/time-entry-form.component';
import { CdkDragStart, CdkDropList, CdkDragMove, CdkDrag } from '@angular/cdk/drag-drop';

// Interface for the backend BaseResponse structure
interface BaseResponse<T> {
  status: string;
  code: number;
  message: string;
  data: T;
}

export interface TimeEntryRequest {
  id: number;
  userId: number;
  userName?: string;
  username?: string;
  projectId: number;
  projectCode?: string;
  projectName: string;
  date?: string;
  entryDate?: string;
  hours?: number;
  timeInMins?: number;
  description?: string;
  activity?: string;
  status: string;
  comments?: string;
  comment?: string;
  rejectionComment?: string;
  createdAt?: string;
  updatedAt?: string;
  ldap?: string;
  leadId?: number;
  leadUsername?: string;
  process?: string;
  attendanceType?: string;
  shift?: string;
  isOvertime?: boolean; // Flag to indicate if this is an overtime entry
  // Flag to indicate if this entry is part of a day with insufficient total time
  isInsufficientDayTime?: boolean;
  // Total time for the day this entry belongs to
  dayTotalTimeInMins?: number;
}

@Component({
  selector: 'app-requests',
  templateUrl: './requests.component.html',
  styleUrls: ['./requests.component.css']
})
export class RequestsComponent implements OnInit, AfterViewInit, OnDestroy {
  baseUrl = environment.apiUrl;
  dataSource = new MatTableDataSource<TimeEntryRequest>([]);
  displayedColumns: string[] = ['select', 'id', 'userName', 'projectName', 'date', 'hours', 'description', 'process', 'shift', 'isOvertime', 'status', 'comments', 'createdAt', 'updatedAt', 'actions'];
  allColumns: string[] = ['select', 'id', 'userName', 'projectName', 'date', 'hours', 'description', 'process', 'shift', 'isOvertime', 'status', 'comments', 'createdAt', 'updatedAt', 'actions'];
  totalRecords = 0;
  showFilters = false;
  filterValues: any = {};
  requestStatuses = ['PENDING', 'APPROVED', 'REJECTED', 'SUBMITTED'];
  startDate: Date | null = null;
  endDate: Date | null = null;

  // Compact table card counts
  pendingRequestsCount = 0;
  approvedRequestsCount = 0;
  rejectedRequestsCount = 0;

  showDirectOnly: boolean = true;

  // Column filtering properties
  columnUniqueValues: { [key: string]: string[] } = {};
  showColumnFilters: boolean = false;
  currentFilterMenuState = {
    columnKey: null as string | null,
    tempSelectedValues: [] as string[],
    searchText: ''
  };
  private currentTrigger: MatMenuTrigger | null = null;

  // Column toggle properties
  columnDisplayNames: { [key: string]: string } = {
    'select': 'Select',
    'id': 'ID',
    'userName': 'User',
    'projectName': 'Project',
    'date': 'Date',
    'hours': 'Hours',
    'description': 'Description',
    'process': 'Process',
    'shift': 'Shift',
    'isOvertime': 'Overtime',
    'status': 'Status',
    'comments': 'Comments',
    'createdAt': 'Created At',
    'updatedAt': 'Updated At',
    'actions': 'Actions'
  };

  // Column toggle search
  columnSearchText: string = '';
  allColumnsSelected: boolean = true;

  // Store the resize handler for cleanup
  private resizeHandler!: () => void;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  selection = new SelectionModel<TimeEntryRequest>(true, []);

  constructor(
    private http: HttpClient, // Removed @Inject
    private dialog: MatDialog, // Removed @Inject
    private notificationService: NotificationService,
    private router: Router // Removed @Inject
  ) { }

  ngOnInit(): void {
    this.calculateRequestCounts();
    // Set default date range to the last week
    const today = new Date();
    const oneWeekAgo = new Date();
    oneWeekAgo.setDate(today.getDate() - 7);

    this.endDate = today;
    this.startDate = oneWeekAgo;

    this.loadRequests(); // Load initial data with the default range
    this.initializeTable();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;

    // Load saved displayed columns
    this.loadDisplayedColumns();

    // Add scroll detection for the table
    this.detectTableScroll();

    // Create and store the resize handler
    this.resizeHandler = () => {
      this.detectTableScroll();
    };

    // Listen for window resize to update scroll detection
    window.addEventListener('resize', this.resizeHandler);
  }

  /**
   * Detects if the table is scrollable horizontally and adds a class accordingly
   */
  detectTableScroll(): void {
    setTimeout(() => {
      const tableContainer = document.querySelector('.table-responsive') as HTMLElement;
      if (tableContainer) {
        // Check if the content width is greater than the container width
        const isScrollable = tableContainer.scrollWidth > tableContainer.clientWidth;

        // Add or remove the 'scrollable' class based on whether the table is scrollable
        if (isScrollable) {
          tableContainer.classList.add('scrollable');
        } else {
          tableContainer.classList.remove('scrollable');
        }
      }
    }, 500); // Delay to ensure the table has rendered
  }

  /**
   * Cleanup resources when component is destroyed
   */
  ngOnDestroy(): void {
    // Remove the resize event listener
    if (this.resizeHandler) {
      window.removeEventListener('resize', this.resizeHandler);
    }
  }

  // Get filtered columns based on search text
  getFilteredColumns(): string[] {
    if (!this.columnSearchText) {
      return this.allColumns;
    }

    return this.allColumns.filter(column =>
      this.columnDisplayNames[column].toLowerCase().includes(this.columnSearchText.toLowerCase())
    );
  }

  // Toggle all columns
  toggleAllColumns(checked: boolean): void {
    this.allColumnsSelected = checked;

    const filteredColumns = this.getFilteredColumns();

    if (checked) {
      // Add all filtered columns (except those that should always be present)
      this.displayedColumns = [...this.allColumns];
    } else {
      // Remove all filtered columns (except those that should always be present)
      this.displayedColumns = this.displayedColumns.filter(column =>
        !filteredColumns.includes(column) || column === 'select' || column === 'actions'
      );
    }

    // Save the displayed columns
    this.saveDisplayedColumns();

    // Update scroll detection after columns change
    setTimeout(() => {
      this.detectTableScroll();
    }, 100);
  }

  // Column toggling methods
  toggleColumn(column: string): void {
    const index = this.displayedColumns.indexOf(column);

    if (index === -1) {
      // Add the column
      const allColumnsIndex = this.allColumns.indexOf(column);
      if (allColumnsIndex > -1) {
        // Find where to insert the column (maintain original order)
        let insertIndex = 0;
        for (let i = 0; i < allColumnsIndex; i++) {
          if (this.displayedColumns.includes(this.allColumns[i])) {
            insertIndex = this.displayedColumns.indexOf(this.allColumns[i]) + 1;
          }
        }
        this.displayedColumns.splice(insertIndex, 0, column);
      }
    } else {
      // Remove the column
      this.displayedColumns.splice(index, 1);
    }

    // Update allColumnsSelected state
    this.updateAllColumnsSelectedState();

    // Save the displayed columns
    this.saveDisplayedColumns();

    // Update scroll detection after columns change
    setTimeout(() => {
      this.detectTableScroll();
    }, 100);
  }

  // Check if a column is currently displayed
  isColumnDisplayed(column: string): boolean {
    return this.displayedColumns.includes(column);
  }

  // Update the allColumnsSelected state based on current selection
  updateAllColumnsSelectedState(): void {
    const filteredColumns = this.getFilteredColumns();
    const selectableFilteredColumns = filteredColumns.filter(col => col !== 'select' && col !== 'actions');

    const allFilteredColumnsSelected = selectableFilteredColumns.every(col =>
      this.displayedColumns.includes(col)
    );

    this.allColumnsSelected = allFilteredColumnsSelected;
  }

  // Save displayed columns to localStorage
  saveDisplayedColumns(): void {
    localStorage.setItem('requestsTableDisplayedColumns', JSON.stringify(this.displayedColumns));
  }

  // Load displayed columns from localStorage
  loadDisplayedColumns(): void {
    const savedColumns = localStorage.getItem('requestsTableDisplayedColumns');
    if (savedColumns) {
      this.displayedColumns = JSON.parse(savedColumns);

      // Ensure 'select' and 'actions' columns are always present
      if (!this.displayedColumns.includes('select')) {
        this.displayedColumns.unshift('select');
      }
      if (!this.displayedColumns.includes('actions')) {
        this.displayedColumns.push('actions');
      }

      // Update allColumnsSelected state
      this.updateAllColumnsSelectedState();
    }
  }

  loadRequests(): void {
    // Ensure dates are set before proceeding
    if (!this.startDate || !this.endDate) {
      console.error('Start date or end date is not set.');
      // Optionally set default dates here again or handle the error
      // For now, let's return to prevent API call with null dates
      return;
    }

    const startDate = this.formatDate(this.startDate);
    const endDate = this.formatDate(this.endDate);
    const status = this.filterValues['status'];

    let url = `${this.baseUrl}/api/time-entries/team?startDate=${startDate}&endDate=${endDate}&directOnly=${this.showDirectOnly}`;
    if (status) {
      url += `&status=${status}`;
    }

    this.http.get<BaseResponse<TimeEntryRequest[]>>(url)
      .subscribe({
        next: (response) => {
          console.log('Received time entry requests response:', response);
          if (response.status === 'success') {
            // Map the API response to match the UI expectations
            const mappedData = response.data.map(entry => this.mapTimeEntryRequest(entry));

            // Check for days with insufficient total time
            this.checkInsufficientDayTime(mappedData);

            this.dataSource.data = mappedData;
            this.totalRecords = mappedData.length;
            this.initializeColumnFilters();

            // Recalculate counts after data is loaded
            this.calculateRequestCounts();
          } else {
            // Handle error response from backend
            this.notificationService.showError(response.message || 'Failed to load time entry requests');
          }
        },
        error: (error) => {
          console.error('Error fetching time entry requests:', error);
          // Try to extract message from backend error response
          let errorMessage = 'Failed to load time entry requests. Please try again.';
          if (error.error && error.error.message) {
            errorMessage = error.error.message;
          }

          this.notificationService.showNotification({
            type: 'error',
            message: errorMessage
          });
        }
      });
  }

  /**
   * Checks if the total time for each day is equal to 480 minutes (8 hours)
   * If not, marks all entries for that day with isInsufficientDayTime = true
   * Does not mark days with overtime entries as insufficient
   */
  private checkInsufficientDayTime(entries: TimeEntryRequest[]): void {
    // Group entries by date and user
    const entriesByDateAndUser = new Map<string, TimeEntryRequest[]>();

    entries.forEach(entry => {
      const date = entry.date || entry.entryDate || '';
      const userId = entry.userId;
      const key = `${date}_${userId}`;

      if (!entriesByDateAndUser.has(key)) {
        entriesByDateAndUser.set(key, []);
      }

      entriesByDateAndUser.get(key)?.push(entry);
    });

    // Check each day's total time
    entriesByDateAndUser.forEach((dayEntries) => {
      const totalTimeInMins = dayEntries.reduce((total, entry) => {
        return total + (entry.timeInMins || 0);
      }, 0);

      // Store the total time for each entry in the day
      dayEntries.forEach(entry => {
        entry.dayTotalTimeInMins = totalTimeInMins;
      });

      // Check if any entry for this day is marked as overtime
      const hasOvertimeEntry = dayEntries.some(entry => entry.isOvertime);

      // If total time is not 480 minutes (8 hours) and there are no overtime entries, mark all entries
      if (totalTimeInMins !== 480 && !hasOvertimeEntry) {
        dayEntries.forEach(entry => {
          entry.isInsufficientDayTime = true;
        });
      }
    });
  }

  // Map API response to UI format
  mapTimeEntryRequest(entry: TimeEntryRequest): TimeEntryRequest {
    return {
      ...entry,
      date: entry.entryDate || entry.date,
      timeInMins: entry.timeInMins || (entry.hours ? entry.hours * 60 : 0),
      description: entry.activity || entry.description,
      comments: entry.comment,
      userName: entry.username || entry.userName || entry.ldap,
      attendanceType: entry.attendanceType || 'N/A', // Default to 'N/A' if attendanceType is not provided
      shift: entry.shift || 'N/A', // Default to 'N/A' if shift is not provided
      isOvertime: entry.isOvertime || false // Default to false if isOvertime is not provided
    };
  }

  applyFilter(event: any, column: string): void {
    if (column === 'status') {
      const matSelect = event as MatSelectChange;
      this.filterValues[column] = matSelect.value;
    } else {
      const filterValue = (event.target as HTMLInputElement).value;
      this.filterValues[column] = filterValue;
    }

    // Reload data with filters
    this.loadRequests();
  }

  applyFilterValues(): void {
    this.dataSource.filter = JSON.stringify(this.filterValues);
    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  applyGlobalFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value.trim().toLowerCase();

    // Clear column filters when global search is used
    if (filterValue) {
      this.filterValues = {};
    }

    this.dataSource.filter = filterValue;
    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  // Initialize the filter predicate in your initialization logic
  private initializeTable(): void {
    this.dataSource.filterPredicate = (data: any, filter: string): boolean => {
      try {
        const searchString = JSON.parse(filter);

        // Handle simple string filter
        if (typeof searchString === 'string') {
          return Object.values(data)
            .some(value => value?.toString().toLowerCase().includes(searchString.toLowerCase()));
        }

        // Handle complex object filter
        for (const key in searchString) {
          if (searchString.hasOwnProperty(key) && searchString[key]) {
            if (Array.isArray(searchString[key]) && searchString[key].length > 0) {
              // For array filters (column filters)
              const columnValue = (data as any)[key]?.toString().toLowerCase() || '';
              if (!searchString[key].some((filterValue: string) =>
                columnValue.includes(filterValue.toLowerCase()))) {
                return false;
              }
            } else if (typeof searchString[key] === 'string' && searchString[key].trim() !== '') {
              // For string filters (text inputs)
              const columnValue = (data as any)[key]?.toString().toLowerCase() || '';
              if (!columnValue.includes(searchString[key].toLowerCase())) {
                return false;
              }
            }
          }
        }
        return true;
      } catch (e) {
        // If filter is not a valid JSON, treat it as a simple string
        const simpleFilter = filter.toLowerCase();
        return Object.values(data)
          .some(value => value?.toString().toLowerCase().includes(simpleFilter));
      }
    };
  }

  // Initialize column filters
  private initializeColumnFilters(): void {
    this.columnUniqueValues = {};

    // Loop through each column
    this.displayedColumns.forEach(columnKey => {
      if (columnKey !== 'actions' && columnKey !== 'select') {
        // Get unique values for the column
        const values = Array.from(new Set(
          this.dataSource.data
            .map(item => String((item as any)[columnKey]))
            .filter(value => value !== null && value !== undefined && value !== '')
        )).sort();

        // Store the unique values
        this.columnUniqueValues[columnKey] = values;
      }
    });
  }

  // Filter menu methods
  openFilterMenu(columnKey: string, trigger: MatMenuTrigger): void {
    this.currentTrigger = trigger;
    this.currentFilterMenuState.columnKey = columnKey;
    this.currentFilterMenuState.tempSelectedValues =
      this.filterValues[columnKey] ? [...this.filterValues[columnKey]] : [];
    this.currentFilterMenuState.searchText = '';
  }

  resetCurrentFilterMenuState(): void {
    this.currentFilterMenuState.columnKey = null;
    this.currentFilterMenuState.tempSelectedValues = [];
    this.currentFilterMenuState.searchText = '';
    this.currentTrigger = null;
  }

  isTempSelected(value: string): boolean {
    return this.currentFilterMenuState.tempSelectedValues.includes(value);
  }

  toggleTempSelection(value: string, checked: boolean): void {
    const index = this.currentFilterMenuState.tempSelectedValues.indexOf(value);
    if (checked && index === -1) {
      this.currentFilterMenuState.tempSelectedValues.push(value);
    } else if (!checked && index !== -1) {
      this.currentFilterMenuState.tempSelectedValues.splice(index, 1);
    }
  }

  toggleSelectAllTemp(checked: boolean): void {
    if (this.currentFilterMenuState.columnKey) {
      this.currentFilterMenuState.tempSelectedValues = checked ?
        [...this.getUniqueColumnValues(this.currentFilterMenuState.columnKey)] : [];
    }
  }

  isAllTempSelected(): boolean {
    return this.currentFilterMenuState.columnKey ?
      this.currentFilterMenuState.tempSelectedValues.length ===
      this.getUniqueColumnValues(this.currentFilterMenuState.columnKey).length : false;
  }

  isSomeTempSelected(): boolean {
    return this.currentFilterMenuState.tempSelectedValues.length > 0 &&
      !this.isAllTempSelected();
  }

  getUniqueColumnValues(columnKey: string): string[] {
    return this.columnUniqueValues[columnKey] || [];
  }

  isFilterActive(columnKey: string): boolean {
    return this.filterValues[columnKey]?.length > 0;
  }

  get filteredMenuOptions(): string[] {
    if (!this.currentFilterMenuState.columnKey) return [];
    const uniqueValues = this.getUniqueColumnValues(this.currentFilterMenuState.columnKey);
    const searchTextLower = this.currentFilterMenuState.searchText.trim().toLowerCase();
    return searchTextLower ?
      uniqueValues.filter(value => value.toLowerCase().includes(searchTextLower)) :
      uniqueValues;
  }

  onFilterApplied(): void {
    if (this.currentFilterMenuState.columnKey) {
      const key = this.currentFilterMenuState.columnKey;
      this.filterValues[key] = [...this.currentFilterMenuState.tempSelectedValues];
      this.applyColumnFilters();

      // Keep the menu open and maintain the selected values
      const trigger = this.currentTrigger;
      if (trigger) {
        setTimeout(() => {
          trigger.openMenu();
          this.currentFilterMenuState.tempSelectedValues = this.filterValues[key] || [];
        });
      }
    }
  }

  clearColumnFilter(): void {
    if (this.currentFilterMenuState.columnKey) {
      const key = this.currentFilterMenuState.columnKey;
      this.filterValues[key] = [];
      this.currentFilterMenuState.tempSelectedValues = [];
      this.applyColumnFilters();

      // Keep the menu open after clearing
      const trigger = this.currentTrigger;
      if (trigger) {
        setTimeout(() => {
          trigger.openMenu();
        });
      }
    }
  }

  private applyColumnFilters(): void {
    const activeFilters = Object.keys(this.filterValues).reduce((acc, key) => {
      if (this.filterValues[key]?.length > 0) {
        acc[key] = this.filterValues[key];
      }
      return acc;
    }, {} as { [key: string]: string[] });

    this.dataSource.filter = JSON.stringify(activeFilters);

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  toggleColumnFilters(): void {
    this.showColumnFilters = !this.showColumnFilters;
  }

  approveRequest(request: TimeEntryRequest): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Confirm Approval',
        message: 'Are you sure you want to approve this time entry request?',
        color: 'primary',
        confirmButtonText: 'Approve'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        const approvalData = { timeEntryId: request.id };
        this.http.post<BaseResponse<any>>(`${this.baseUrl}/api/time-entries/${request.id}/approve`, approvalData)
          .subscribe({
            next: (response) => {
              if (response.status === 'success') {
                this.notificationService.showNotification({
                  type: 'success',
                  message: response.message || 'Time entry request approved successfully!'
                });
                this.loadRequests();
              } else {
                this.notificationService.showNotification({
                  type: 'error',
                  message: response.message || 'Failed to approve time entry request'
                });
              }
            },
            error: (error) => {
              console.error('Error approving time entry request:', error);
              // Try to extract message from backend error response
              let errorMessage = 'Failed to approve time entry request. Please try again.';
              if (error.error && error.error.message) {
                errorMessage = error.error.message;
              }

              this.notificationService.showNotification({
                type: 'error',
                message: errorMessage
              });
            }
          });
      }
    });
  }

  rejectRequest(request: TimeEntryRequest): void {
    // Check if the entry is already approved
    if (request.status === 'APPROVED') {
      this.notificationService.showNotification({
        type: 'warning',
        message: 'Approved entries cannot be rejected.'
      });
      return;
    }

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Confirm Rejection',
        message: 'Are you sure you want to reject this time entry request?',
        showCommentField: true,
        commentLabel: 'Rejection Reason (Required)',
        color: 'warn',
        confirmButtonText: 'Reject'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result?.confirmed) {
        const rejectionData = {
          timeEntryId: request.id,
          rejectionComment: result.comment || ''
        };
        this.http.post<BaseResponse<any>>(`${this.baseUrl}/api/time-entries/${request.id}/reject`, rejectionData)
          .subscribe({
            next: (response) => {
              if (response.status === 'success') {
                this.notificationService.showNotification({
                  type: 'success',
                  message: response.message || 'Time entry request rejected successfully!'
                });
                this.loadRequests();
              } else {
                this.notificationService.showNotification({
                  type: 'error',
                  message: response.message || 'Failed to reject time entry request'
                });
              }
            },
            error: (error) => {
              console.error('Error rejecting time entry request:', error);
              // Try to extract message from backend error response
              let errorMessage = 'Failed to reject time entry request. Please try again.';
              if (error.error && error.error.message) {
                errorMessage = error.error.message;
              }

              this.notificationService.showNotification({
                type: 'error',
                message: errorMessage
              });
            }
          });
      }
    });
  }

  applyDateRange(): void {
    if (this.startDate && this.endDate) {
      this.loadRequests(); // This will now use the selected date range
    }
  }

  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  backToTimeEntries() {
    this.router.navigate(['/time-entry']);
  }

  /** Whether the number of selected elements matches the total number of rows. */
  isAllSelected() {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows;
  }

  /** Selects all rows if they are not all selected; otherwise clear selection. */
  toggleAllRows() {
    if (this.isAllSelected()) {
      this.selection.clear();
      return;
    }
    this.selection.select(...this.dataSource.data);
  }

  /** The label for the checkbox on the passed row */
  checkboxLabel(row?: TimeEntryRequest): string {
    if (!row) {
      return `${this.isAllSelected() ? 'deselect' : 'select'} all`;
    }
    return `${this.selection.isSelected(row) ? 'deselect' : 'select'} row`;
  }

  approveAllSelected(): void {
    const selectedEntries = this.selection.selected;
    if (selectedEntries.length === 0) {
      this.notificationService.showNotification({
        type: 'warning',
        message: 'Please select at least one time entry to approve.'
      });
      return;
    }

    // Check if all selected entries are already approved
    const allApproved = selectedEntries.every(entry => entry.status === 'APPROVED');
    if (allApproved) {
      this.notificationService.showNotification({
        type: 'warning',
        message: 'All selected entries are already approved.'
      });
      return;
    }

    // Check if some entries are already approved
    const someApproved = selectedEntries.some(entry => entry.status === 'APPROVED');
    if (someApproved) {
      this.notificationService.showNotification({
        type: 'warning',
        message: 'Some selected entries are already approved. Please review your selection carefully.'
      });
    }

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Confirm Approval',
        message: `Are you sure you want to approve selected time entry requests?`,
        color: 'primary',
        confirmButtonText: 'Approve All'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        const approvalData = {
          timeEntryId: selectedEntries.map(entry => entry.id)
        };

        this.http.post<BaseResponse<any>>(`${this.baseUrl}/api/time-entries/approveAll`, approvalData)
          .subscribe({
            next: (response) => {
              if (response.status === 'success') {
                this.notificationService.showNotification({
                  type: 'success',
                  message: response.message || 'Selected time entries approved successfully!'
                });
                this.selection.clear();
                this.loadRequests();
              } else {
                this.notificationService.showNotification({
                  type: 'error',
                  message: response.message || 'Failed to approve time entries'
                });
              }
            },
            error: (error) => {
              console.error('Error approving time entries:', error);
              // Try to extract message from backend error response
              let errorMessage = 'Failed to approve time entries. Please try again.';
              if (error.error && error.error.message) {
                errorMessage = error.error.message;
              }

              this.notificationService.showNotification({
                type: 'error',
                message: errorMessage
              });
            }
          });
      }
    });
  }

  rejectAllSelected(): void {
    const selectedEntries = this.selection.selected;
    if (selectedEntries.length === 0) {
      this.notificationService.showWarning('Please select at least one time entry to reject.');
      return;
    }

    // Check if all selected entries are already rejected
    const allRejected = selectedEntries.every(entry => entry.status === 'REJECTED');
    if (allRejected) {
      this.notificationService.showWarning('All selected entries are already rejected.');
      return;
    }

    // // Check if any selected entries are approved
    // const hasApprovedEntries = selectedEntries.some(entry => entry.status === 'APPROVED');
    // if (hasApprovedEntries) {
    //   this.notificationService.showNotification({
    //     type: 'warning',
    //     message: 'Some selected entries are already rejected and cannot be rejected. Please review your selection carefully.'
    //   });
    //   return;
    // }    // // Check if any selected entries are approved
    // const hasApprovedEntries = selectedEntries.some(entry => entry.status === 'APPROVED');
    // if (hasApprovedEntries) {
    //   this.notificationService.showNotification({
    //     type: 'warning',
    //     message: 'Some selected entries are already rejected and cannot be rejected. Please review your selection carefully.'
    //   });
    //   return;
    // }

    // // Check if some entries are already rejected
    // const someRejected = selectedEntries.some(entry => entry.status === 'REJECTED');
    // if (someRejected) {
    //   this.notificationService.showNotification({
    //     type: 'warning',
    //     message: 'Some selected entries are already rejected. Please review your selection carefully.'
    //   });
    // }

    // // Check if some entries are already rejected
    // const someRejected = selectedEntries.some(entry => entry.status === 'REJECTED');
    // if (someRejected) {
    //   this.notificationService.showNotification({
    //     type: 'warning',
    //     message: 'Some selected entries are already rejected. Please review your selection carefully.'
    //   });
    // }

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Confirm Rejection',
        message: `Are you sure you want to reject selected time entry requests?`,
        showCommentField: true,
        commentLabel: 'Rejection Reason (Required)',
        color: 'warn',
        confirmButtonText: 'Reject All'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result?.confirmed) {
        const rejectionData = {
          timeEntryId: selectedEntries.map(entry => entry.id),
          rejectionComment: result.comment || ''
        };

        this.http.post<BaseResponse<any>>(`${this.baseUrl}/api/time-entries/rejectAll`, rejectionData)
          .subscribe({
            next: (response) => {
              if (response.status === 'success') {
                this.notificationService.showNotification({
                  type: 'success',
                  message: response.message || 'Selected time entries rejected successfully!'
                });
                this.selection.clear();
                this.loadRequests();
              } else {
                this.notificationService.showNotification({
                  type: 'error',
                  message: response.message || 'Failed to reject time entries'
                });
              }
            },
            error: (error) => {
              console.error('Error rejecting time entries:', error);
              // Try to extract message from backend error response
              let errorMessage = 'Failed to reject time entries. Please try again.';
              if (error.error && error.error.message) {
                errorMessage = error.error.message;
              }

              this.notificationService.showNotification({
                type: 'error',
                message: errorMessage
              });
            }
          });
      }
    });
  }

  downloadCSV(): void {
    // Transform the data to a more CSV-friendly format
    console.log(this.dataSource.data)
    const exportData = this.dataSource.data.map(item => ({
      'ID': item.id,
      'User Name': item.username || item.userName || item.ldap,
      'Project Name': item.projectName,
      'Project Code': item.projectCode,
      'Date': item.entryDate || item.date,
      'Minutes': item.timeInMins ? item.timeInMins : 0,
      'Description': item.activity || item.description,
      'Process': item.process,
      'Shift': item.attendanceType || 'N/A',
      'Overtime': item.isOvertime ? 'Yes' : 'No',
      'Status': item.status,
      'Comments': item.comment || item.comments || item.rejectionComment || '',
      'Lead': item.leadUsername
    }));

    const worksheet = XLSX.utils.json_to_sheet(exportData);
    const workbook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, worksheet, 'Time Entry Requests');

    const excelBuffer: any = XLSX.write(workbook, { bookType: 'csv', type: 'array' });
    const blob = new Blob([excelBuffer], { type: 'text/csv;charset=utf-8;' });
    saveAs(blob, `time_entry_requests_${new Date().toISOString().split('T')[0]}.csv`);
  }

  /**
   * Opens the time entry form to edit an existing time entry
   * @param request The time entry request to edit
   */
  editTimeEntry(request: TimeEntryRequest): void {
    const dialogRef = this.dialog.open(TimeEntryFormComponent, {
      width: '800px',
      data: {
        isEditMode: true,
        timeEntry: request,
        existingEntries: this.dataSource.data,
        fromRequestsTable: true // Flag to indicate we're editing from the requests table
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadRequests();
      }
    });
  }

  /**
   * Deletes a time entry after confirmation
   * @param request The time entry request to delete
   */
  deleteTimeEntry(request: TimeEntryRequest): void {
    // Check if the entry is already approved or pending
    // if (request.status === 'APPROVED') {
    //   this.notificationService.showNotification({
    //     type: 'warning',
    //     message: 'Approved entries cannot be deleted.'
    //   });
    //   return;
    // }

    // if (request.status === 'PENDING') {
    //   this.notificationService.showNotification({
    //     type: 'warning',
    //     message: 'Pending entries cannot be deleted. Please reject them first.'
    //   });
    //   return;
    // }

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Confirm Delete',
        message: 'Are you sure you want to delete this time entry?',
        color: 'warn',
        confirmButtonText: 'Delete'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.http.delete<BaseResponse<any>>(`${this.baseUrl}/api/time-entries/${request.id}`)
          .subscribe({
            next: (response) => {
              if (response.status === 'success') {
                this.notificationService.showNotification({
                  type: 'success',
                  message: response.message || 'Time entry deleted successfully!'
                });
                this.loadRequests();
              } else {
                this.notificationService.showNotification({
                  type: 'error',
                  message: response.message || 'Failed to delete time entry'
                });
              }
            },
            error: (error) => {
              console.error('Error deleting time entry:', error);
              // Try to extract message from backend error response
              let errorMessage = 'Failed to delete time entry. Please try again.';
              if (error.error && error.error.message) {
                errorMessage = error.error.message;
              }

              this.notificationService.showNotification({
                type: 'error',
                message: errorMessage
              });
            }
          });
      }
    });
  }

  /**
   * Calculate request counts for compact table cards
   */
  calculateRequestCounts(): void {
    const data = this.dataSource.data;

    this.pendingRequestsCount = data.filter(req => req.status === 'PENDING').length;
    this.approvedRequestsCount = data.filter(req => req.status === 'APPROVED').length;
    this.rejectedRequestsCount = data.filter(req => req.status === 'REJECTED').length;
  }

  /**
   * Handle card click events
   */
  onCardClick(): void {
    console.log('Card clicked - you can implement navigation or filtering here');
    // You can implement navigation to filtered views or detailed analytics
  }

  toggleDirectOnly(): void {
    this.loadRequests();
  }

}
