import { AfterViewInit, Component, OnInit, ViewChild, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatDialog } from '@angular/material/dialog';
import { NotificationService } from '../shared/notification.service';
import { TimeEntryFormComponent } from './time-entry-form/time-entry-form.component';
import { MultipleTimeEntryFormComponent } from './multiple-time-entry-form/multiple-time-entry-form.component';
import { ConfirmationDialogComponent } from '../confirm-dialog/confirmation-dialog.component';
import { WeekCopyDialogComponent } from './week-copy-dialog/week-copy-dialog.component';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup } from '@angular/forms';
import { environment } from '../../environments/environment';
import { MatMenuTrigger } from '@angular/material/menu';
import { saveAs } from 'file-saver';
import * as XLSX from 'xlsx';

// Interface for the backend BaseResponse structure
interface BaseResponse<T> {
  status: string;
  code: number;
  message: string;
  data: T;
}

// Updated interface to match the API response format
export interface TimeEntry {
  id: number;
  userId: number;
  username?: string;
  userName?: string; // For backward compatibility
  projectId: number;
  projectCode?: string;
  projectName: string;
  entryDate?: string; // From API
  date?: string;      // For UI display
  timeInMins?: number; // From API
  hours?: number;      // For UI display
  activity?: string;   // From API
  description?: string; // For UI display
  status: string;
  comment?: string;    // From API
  comments?: string;   // For UI display
  rejectionComment?: string;
  createdAt?: string;
  updatedAt?: string;
  ldap?: string;
  leadId?: number;
  leadUsername?: string;
  process?: string;
  attendanceType?: string;
  isOvertime?: boolean; // Flag to indicate if this is an overtime entry
  isLocked?: boolean; // Flag to indicate if entry is locked (auto-generated from leave)
  source?: string; // Source of entry: 'USER', 'AUTO_LEAVE', etc.
}

@Component({
  selector: 'app-time-entry',
  templateUrl: './time-entry.component.html',
  styleUrls: ['./time-entry.component.css']
})
export class TimeEntryComponent implements OnInit, AfterViewInit, OnDestroy {
  // Store the resize handler for cleanup
  private resizeHandler!: () => void;
  baseUrl = environment.apiUrl;
  dataSource = new MatTableDataSource<TimeEntry>([]);
  displayedColumns: string[] = ['date', 'ldap', 'projectName', 'process', 'activity', 'timeInMins', 'attendanceType', 'comment', 'status', 'createdAt', 'updatedAt', 'actions'];
  //  displayedColumns: string[] = ['date', 'ldap', 'leadUsername', 'projectName', 'process', 'activity', 'timeInMins', 'attendanceType', 'isOvertime', 'comment', 'status', 'actions'];
  allColumns: string[] = ['date', 'ldap', 'leadUsername', 'projectName', 'process', 'activity', 'timeInMins', 'attendanceType', 'isOvertime', 'comment', 'status', 'createdAt', 'updatedAt', 'actions'];
  totalRecords = 0;
  showFilters = false;
  showColumnFilters = false;
  filterValues: any = {};

  // Column toggle properties
  columnDisplayNames: { [key: string]: string } = {
    'date': 'Date',
    'ldap': 'LDAP',
    'leadUsername': 'Lead',
    'projectName': 'Project',
    'process': 'Process',
    'activity': 'Activity',
    'timeInMins': 'Time (mins)',
    'attendanceType': 'Attendance Type',
    'isOvertime': 'Overtime',
    'comment': 'Comment',
    'status': 'Status',
    'createdAt': 'Created At',
    'updatedAt': 'Updated At',
    'actions': 'Actions'
  };

  // Column toggle search
  columnSearchText: string = '';
  allColumnsSelected: boolean = false;
  dateRange = new FormGroup({
    start: new FormControl<Date | null>(this.getLastWeekStartDate()),
    end: new FormControl<Date | null>(new Date())
  });

  userRole: string | undefined;

  // Column filtering properties
  columnUniqueValues: { [key: string]: string[] } = {};
  currentFilterMenuState = {
    columnKey: null as string | null,
    tempSelectedValues: [] as string[],
    searchText: ''
  };
  currentTrigger: MatMenuTrigger | null = null;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private http: HttpClient,
    private router: Router,
    private dialog: MatDialog,
    private notificationService: NotificationService
  ) { }

  ngOnInit(): void {
    this.userRole = localStorage.getItem('role') || undefined;
    this.loadTimeEntries();

    // Set up filter predicate for both global and column filters
    this.dataSource.filterPredicate = (data: TimeEntry, filter: string) => {
      // For global filter (simple string)
      if (!filter.includes('{')) {
        return Object.values(data)
          .some(value => value?.toString().toLowerCase().includes(filter.toLowerCase()));
      }

      // For column filters (JSON object)
      try {
        const filterObject = JSON.parse(filter);
        const keys = Object.keys(filterObject);

        if (keys.length === 0) return true;

        return keys.every(key => {
          const filterValues = filterObject[key];

          // Skip empty filters
          if (!filterValues || (Array.isArray(filterValues) && filterValues.length === 0)) {
            return true;
          }

          // Handle array of filter values (for column filter menu)
          if (Array.isArray(filterValues)) {
            const dataValue = String(data[key as keyof TimeEntry] || '').toLowerCase();
            return filterValues.some(value => dataValue.includes(value.toLowerCase()));
          }

          // Handle string filter values (for text input filters)
          const dataValue = String(data[key as keyof TimeEntry] || '').toLowerCase();
          return dataValue.includes(filterValues.toLowerCase());
        });
      } catch (error) {
        console.error('Error parsing filter:', error);
        return true;
      }
    };
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
        // Add a small buffer (20px) to ensure action buttons aren't cut off
        const isScrollable = tableContainer.scrollWidth > (tableContainer.clientWidth - 20);

        // Add or remove the 'scrollable' class based on whether the table is scrollable
        if (isScrollable) {
          tableContainer.classList.add('scrollable');
        } else {
          tableContainer.classList.remove('scrollable');
        }

        // Ensure the action buttons are visible by checking if they're in view
        const actionCells = document.querySelectorAll('.actions-cell') as NodeListOf<HTMLElement>;
        if (actionCells && actionCells.length > 0) {
          // Check if any action cell is partially out of view
          Array.from(actionCells).forEach(cell => {
            const cellRect = cell.getBoundingClientRect();
            const containerRect = tableContainer.getBoundingClientRect();

            // If the cell is partially out of view, ensure the table is scrollable
            if (cellRect.right > containerRect.right) {
              tableContainer.classList.add('scrollable');
            }
          });
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

  getLastWeekStartDate(): Date {
    const date = new Date();
    date.setDate(date.getDate() - 7);
    return date;
  }

  loadTimeEntries(): void {
    const startDate = this.formatDate(this.dateRange.get('start')?.value || this.getLastWeekStartDate());
    const endDate = this.formatDate(this.dateRange.get('end')?.value || new Date());

    this.http.get<BaseResponse<TimeEntry[]>>(`${this.baseUrl}/api/time-entries?startDate=${startDate}&endDate=${endDate}`)
      .subscribe({
        next: (response) => {
          console.log('Received time entries response:', response);
          if (response.status === 'success') {
            const mappedData = response.data.map(entry => this.mapTimeEntry(entry));
            this.dataSource.data = mappedData;
            this.totalRecords = mappedData.length;

            // Collect unique values for column filters
            this.collectUniqueColumnValues(mappedData);

            // Update scroll detection after data loads
            setTimeout(() => {
              this.detectTableScroll();
            }, 100);
          } else {
            // Handle error response from backend
            this.notificationService.showNotification({
              type: 'error',
              message: response.message || 'Failed to load time entries'
            });
          }
        },
        error: (error) => {
          console.error('Error fetching time entries:', error);
          // Extract error message and let the service transform it
          const errorMessage = error.error?.message || 'Failed to load time entries. Please try again.';
          this.notificationService.showError(errorMessage);

          if (error.status === 0) {
            this.addSampleData();
          }
        }
      });
  }

  // Collect unique values for each column to use in filters
  collectUniqueColumnValues(data: TimeEntry[]): void {
    // Define columns that should have filters
    const columnsToFilter = ['date', 'ldap', 'leadUsername', 'projectName', 'process',
      'activity', 'timeInMins', 'attendanceType', 'comment', 'status'];

    // Reset the unique values
    this.columnUniqueValues = {};

    // For each column, collect unique values
    columnsToFilter.forEach(column => {
      const uniqueValues = new Set<string>();

      data.forEach(entry => {
        const value = entry[column as keyof TimeEntry];
        if (value !== undefined && value !== null) {
          uniqueValues.add(String(value));
        }
      });

      this.columnUniqueValues[column] = Array.from(uniqueValues).sort();
    });
  }

  // Map API response to UI format
  mapTimeEntry(entry: TimeEntry): TimeEntry {
    return {
      ...entry,
      date: entry.entryDate || entry.date, // Use entryDate from API or fallback to date
      hours: entry.timeInMins ? entry.timeInMins / 60 : 0, // Convert minutes to hours
      description: entry.activity || entry.description, // Use activity from API or fallback to description
      comments: entry.comment || entry.comments, // Use comment from API or fallback to comments
      userName: entry.username || entry.userName || entry.ldap, // Use username from API or fallback
      leadUsername: entry.leadUsername,
      process: entry.process,
      ldap: entry.ldap,
      comment: entry.comment,
      attendanceType: entry.attendanceType,
      isOvertime: entry.isOvertime || false, // Default to false if isOvertime is not provided
      isLocked: entry.isLocked || entry.source === 'AUTO_LEAVE' || false, // Lock if isLocked=true or source is AUTO_LEAVE
      source: entry.source, // Preserve source field
    };
  }

  // Add sample data for testing
  addSampleData(): void {
    const sampleData: TimeEntry[] = [
      {
        id: 1,
        userId: 1606,
        username: "akhilbhatnagar",
        projectId: 1,
        projectCode: "PROJ001",
        projectName: "Customer Portal Development",
        entryDate: "2025-04-02",
        ldap: "akhilbhatnagar",
        leadId: 1606,
        leadUsername: "akhilbhatnagar",
        process: "Development",
        activity: "DEVELOPMENT",
        timeInMins: 240,
        comment: "Working on user authentication module",
        status: "APPROVED",
        rejectionComment: undefined
      },
      {
        id: 3,
        userId: 1606,
        username: "akhilbhatnagar",
        projectId: 1,
        projectCode: "PROJ001",
        projectName: "Customer Portal Development",
        entryDate: "2025-04-02",
        ldap: "akhilbhatnagar",
        leadId: 1606,
        leadUsername: "akhilbhatnagar",
        process: "OPS EXCELLENCE",
        activity: "DEVELOPMENT",
        timeInMins: 100,
        comment: "Working on user authentication module",
        status: "PENDING",
        rejectionComment: undefined
      }
    ];

    // Map the sample data
    const mappedData = sampleData.map(entry => this.mapTimeEntry(entry));
    this.dataSource.data = mappedData;
    this.totalRecords = mappedData.length;

    // Collect unique values for column filters
    this.collectUniqueColumnValues(mappedData);
  }

  formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  applyDateFilter(): void {
    this.loadTimeEntries();
  }

  applyFilter(event: Event, column: string): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.filterValues[column] = filterValue.trim().toLowerCase();
    this.applyFilterValues();
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

      // Set up global filter predicate
      this.dataSource.filterPredicate = (data: TimeEntry, filter: string): boolean => {
        const searchTerm = filter.toLowerCase();
        const dataStr = Object.keys(data as any).reduce((currentTerm, key) => {
          // Combine relevant fields for global search
          return currentTerm + (data as any)[key] + '◬'; // Use a unique separator
        }, '').toLowerCase();
        return dataStr.indexOf(searchTerm) !== -1;
      };
    } else {
      // Reset to column filter predicate when global filter is cleared
      this.dataSource.filterPredicate = (data: TimeEntry, filter: string) => {
        // For global filter (simple string)
        if (!filter.includes('{')) {
          return Object.values(data)
            .some(value => value?.toString().toLowerCase().includes(filter.toLowerCase()));
        }

        // For column filters (JSON object)
        try {
          const filterObject = JSON.parse(filter);
          const keys = Object.keys(filterObject);

          if (keys.length === 0) return true;

          return keys.every(key => {
            const filterValues = filterObject[key];

            // Skip empty filters
            if (!filterValues || (Array.isArray(filterValues) && filterValues.length === 0)) {
              return true;
            }

            // Handle array of filter values (for column filter menu)
            if (Array.isArray(filterValues)) {
              const dataValue = String(data[key as keyof TimeEntry] || '').toLowerCase();
              return filterValues.some(value => dataValue.includes(value.toLowerCase()));
            }

            // Handle string filter values (for text input filters)
            const dataValue = String(data[key as keyof TimeEntry] || '').toLowerCase();
            return dataValue.includes(filterValues.toLowerCase());
          });
        } catch (error) {
          console.error('Error parsing filter:', error);
          return true;
        }
      };
    }

    this.dataSource.filter = filterValue;
    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  toggleColumnFilters(): void {
    this.showColumnFilters = !this.showColumnFilters;
  }

  // Column filter menu methods
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
    if (checked) {
      if (!this.isTempSelected(value)) {
        this.currentFilterMenuState.tempSelectedValues.push(value);
      }
    } else {
      const index = this.currentFilterMenuState.tempSelectedValues.indexOf(value);
      if (index >= 0) {
        this.currentFilterMenuState.tempSelectedValues.splice(index, 1);
      }
    }
  }

  isAllTempSelected(): boolean {
    const filteredOptions = this.getFilteredMenuOptions();
    return filteredOptions.length > 0 &&
      filteredOptions.every(value => this.isTempSelected(value));
  }

  isSomeTempSelected(): boolean {
    const filteredOptions = this.getFilteredMenuOptions();
    return filteredOptions.some(value => this.isTempSelected(value)) &&
      !this.isAllTempSelected();
  }

  toggleSelectAllTemp(checked: boolean): void {
    const filteredOptions = this.getFilteredMenuOptions();

    if (checked) {
      // Add all filtered options that aren't already selected
      filteredOptions.forEach(value => {
        if (!this.isTempSelected(value)) {
          this.currentFilterMenuState.tempSelectedValues.push(value);
        }
      });
    } else {
      // Remove all filtered options
      this.currentFilterMenuState.tempSelectedValues =
        this.currentFilterMenuState.tempSelectedValues.filter(
          value => !filteredOptions.includes(value)
        );
    }
  }

  onFilterApplied(): void {
    if (this.currentFilterMenuState.columnKey) {
      const key = this.currentFilterMenuState.columnKey;
      this.filterValues[key] = [...this.currentFilterMenuState.tempSelectedValues];
      this.applyColumnFilters();
    }

    // Close the menu
    if (this.currentTrigger) {
      this.currentTrigger.closeMenu();
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

  applyColumnFilters(): void {
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

  getUniqueColumnValues(columnKey: string): string[] {
    return this.columnUniqueValues[columnKey] || [];
  }

  get filteredMenuOptions(): string[] {
    return this.getFilteredMenuOptions();
  }

  getFilteredMenuOptions(): string[] {
    if (!this.currentFilterMenuState.columnKey) return [];

    const columnKey = this.currentFilterMenuState.columnKey;
    const searchText = this.currentFilterMenuState.searchText.toLowerCase();
    const options = this.getUniqueColumnValues(columnKey);

    if (!searchText) return options;

    return options.filter(option =>
      option.toLowerCase().includes(searchText)
    );
  }

  isFilterActive(columnKey: string): boolean {
    return this.filterValues[columnKey]?.length > 0;
  }

  // Column toggle methods
  getFilteredColumns(): string[] {
    if (!this.columnSearchText) {
      return this.allColumns;
    }

    return this.allColumns.filter(column =>
      this.columnDisplayNames[column].toLowerCase().includes(this.columnSearchText.toLowerCase())
    );
  }

  toggleAllColumns(checked: boolean): void {
    this.allColumnsSelected = checked;

    const filteredColumns = this.getFilteredColumns();

    if (checked) {
      // Add all filtered columns
      this.displayedColumns = [...this.allColumns];
    } else {
      // Remove all filtered columns (except actions which should always be present)
      this.displayedColumns = this.displayedColumns.filter(column =>
        !filteredColumns.includes(column) || column === 'actions'
      );
    }

    // Save the displayed columns
    this.saveDisplayedColumns();

    // Update scroll detection after columns change
    setTimeout(() => {
      this.detectTableScroll();
    }, 100);
  }

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

  isColumnDisplayed(column: string): boolean {
    return this.displayedColumns.includes(column);
  }

  updateAllColumnsSelectedState(): void {
    const filteredColumns = this.getFilteredColumns();
    const selectableFilteredColumns = filteredColumns.filter(col => col !== 'actions');

    const allFilteredColumnsSelected = selectableFilteredColumns.every(col =>
      this.displayedColumns.includes(col)
    );

    this.allColumnsSelected = allFilteredColumnsSelected;
  }

  saveDisplayedColumns(): void {
    localStorage.setItem('timeEntryTableDisplayedColumns', JSON.stringify(this.displayedColumns));
  }

  loadDisplayedColumns(): void {
    // Clear any saved column preferences to force new defaults
    localStorage.removeItem('timeEntryTableDisplayedColumns');

    // Set default columns (excluding leadUsername and isOvertime - they should be toggled on)
    this.displayedColumns = ['date', 'ldap', 'projectName', 'process', 'activity', 'timeInMins', 'attendanceType', 'comment', 'status', 'actions'];

    // Ensure 'actions' column is always present
    if (!this.displayedColumns.includes('actions')) {
      this.displayedColumns.push('actions');
    }

    // Update allColumnsSelected state
    this.updateAllColumnsSelectedState();
  }

  openAddTimeEntryForm(): void {
    const dialogRef = this.dialog.open(TimeEntryFormComponent, {
      width: '800px',
      data: {
        isEditMode: false,
        existingEntries: this.dataSource.data  // Pass current time entries
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadTimeEntries();
      }
    });
  }

  openMultipleTimeEntryForm(): void {
    const dialogRef = this.dialog.open(MultipleTimeEntryFormComponent, {
      width: '900px',
      maxHeight: '90vh',
      data: {
        existingEntries: this.dataSource.data
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadTimeEntries();
      }
    });
  }


  editTimeEntry(timeEntry: TimeEntry): void {
    const dialogRef = this.dialog.open(TimeEntryFormComponent, {
      width: '800px',
      data: {
        isEditMode: true,
        timeEntry: timeEntry,
        existingEntries: this.dataSource.data  // Pass current time entries
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadTimeEntries();
      }
    });
  }

  deleteTimeEntry(timeEntry: TimeEntry): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: { title: 'Confirm Delete', message: 'Are you sure you want to delete this time entry?' }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.http.delete<BaseResponse<any>>(`${this.baseUrl}/api/time-entries/${timeEntry.id}`)
          .subscribe({
            next: (response) => {
              if (response.status === 'success') {
                this.notificationService.showNotification({
                  type: 'success',
                  message: response.message || 'Time entry deleted successfully!'
                });
                this.loadTimeEntries();
              } else {
                this.notificationService.showNotification({
                  type: 'error',
                  message: response.message || 'Failed to delete time entry'
                });
              }
            },
            error: (error) => {
              console.error('Error deleting time entry:', error);
              // Extract error message and let the service transform it
              const errorMessage = error.error?.message || 'Failed to delete time entry. Please try again.';
              this.notificationService.showError(errorMessage);
            }
          });
      }
    });
  }

  viewProjects(): void {
    this.router.navigate(['/time-entry/projects']);
  }

  viewRequests(): void {
    this.router.navigate(['/time-entry/requests']);
  }

  assignProjects(): void {
    this.router.navigate(['/time-entry/project-assignment']);
  }

  isLeadOrManager(): boolean {
    return this.userRole === 'LEAD' || this.userRole === 'MANAGER' || this.userRole === 'ADMIN_OPS_MANAGER';
  }

  /**
   * Check if a time entry can be edited
   * Regular users cannot edit locked entries (auto-generated from leave)
   * Lead/Manager can edit all entries including locked ones
   */
  isEntryEditable(entry: TimeEntry): boolean {
    // If entry is locked/auto-generated AND not rejected, only Lead/Manager can edit
    if ((entry.isLocked || entry.source === 'AUTO_LEAVE') && entry.status !== 'REJECTED') {
      return this.isLeadOrManager();
    }
    // REJECTED entries are editable by the user
    // Regular pending entries are also editable
    return entry.status === 'PENDING' || entry.status === 'REJECTED';
  }

  /**
   * Check if a time entry can be deleted  
   * Regular users cannot delete locked entries (auto-generated from leave)
   * Lead/Manager can delete all entries including locked ones
   */
  isEntryDeletable(entry: TimeEntry): boolean {
    // If entry is locked/auto-generated AND not rejected, only Lead/Manager can delete
    if ((entry.isLocked || entry.source === 'AUTO_LEAVE') && entry.status !== 'REJECTED') {
      return this.isLeadOrManager();
    }
    // REJECTED entries are deletable by the user
    // Regular pending entries are also deletable
    return entry.status === 'PENDING' || entry.status === 'REJECTED';
  }

  /**
   * Get tooltip text for locked entries
   */
  getLockedEntryTooltip(entry: TimeEntry): string {
    if ((entry.isLocked || entry.source === 'AUTO_LEAVE') && entry.status !== 'REJECTED') {
      return 'Auto-generated from leave request - only Lead/Manager can modify';
    }
    if (entry.status === 'REJECTED') {
      return 'This entry was rejected - you can edit and resubmit';
    }
    return '';
  }

  viewRejectionComment(entry: TimeEntry): void {
    const comment = entry.rejectionComment;
    if (comment) {
      this.dialog.open(ConfirmationDialogComponent, {
        width: '400px',
        data: {
          title: 'Rejection Comment',
          message: comment,
          showCancel: false,
          confirmText: 'OK',
          showConfirm: true
        }
      });
    }
  }

  cloneTimeEntry(entry: TimeEntry): void {
    const clonedEntry = {
      ...entry,
      id: undefined,
      status: 'PENDING',
      date: new Date(),
      entryDate: new Date(),
      timeInMins: entry.timeInMins || (entry.hours ? entry.hours * 60 : 0),
      comment: entry.comment || '',
    };

    const dialogRef = this.dialog.open(TimeEntryFormComponent, {
      width: '800px',
      data: {
        isEditMode: false,
        timeEntry: clonedEntry,
        isCloning: true,
        existingEntries: this.dataSource.data  // Pass current time entries
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadTimeEntries();
      }
    });
  }

  /**
   * Opens the week copy dialog to copy a time entry to multiple days in a week
   * @param entry The time entry to copy
   */
  copyToWeek(entry: TimeEntry): void {
    const dialogRef = this.dialog.open(WeekCopyDialogComponent, {
      width: '600px',
      data: {
        timeEntry: entry
      }
    });

    dialogRef.afterClosed().subscribe(async result => {
      if (result) {
        // Check for holidays in the selected dates and create separate holiday entries
        await this.createHolidayEntriesForDates(result.selectedDates, entry);

        // Filter out holiday dates from regular batch request
        const regularDates = await this.filterOutHolidayDates(result.selectedDates);

        // Create regular batch request only for non-holiday dates
        if (regularDates.length > 0) {
          const batchRequest = {
            sourceEntry: {
              projectId: entry.projectId,
              entryDate: entry.entryDate || entry.date,
              process: entry.process,
              activity: entry.activity,
              timeInMins: entry.timeInMins || 0,
              attendanceType: entry.attendanceType,
              leadId: entry.leadId,
              comment: entry.comment || '',
              ldap: entry.ldap || '',
              isOvertime: entry.isOvertime || false
            },
            targetDates: regularDates
          };

          // Send the batch request to the server
          this.http.post<BaseResponse<any>>(`${this.baseUrl}/api/time-entries/batch`, batchRequest)
            .subscribe({
              next: (response) => {
                if (response.status === 'success') {
                  const totalDays = result.selectedDates.length;
                  let message = `Successfully copied time entry to ${totalDays} day${totalDays !== 1 ? 's' : ''}!`;

                  this.notificationService.showNotification({
                    type: 'success',
                    message: message
                  });
                  this.loadTimeEntries();
                } else {
                  this.notificationService.showNotification({
                    type: 'error',
                    message: response.message || 'Failed to copy time entries'
                  });
                }
              },
              error: (error) => {
                console.error('Error creating batch time entries:', error);

                // Try to extract message from backend error response
                let errorMessage = 'Failed to copy time entries. Please try again.';
                if (error.error && error.error.message) {
                  errorMessage = error.error.message;
                }

                // Check if the error is related to time limit
                if (errorMessage.includes('Total time for') && errorMessage.includes('would exceed 8 hours')) {
                  // Extract the date from the error message
                  const dateMatch = errorMessage.match(/Total time for (\d{4}-\d{2}-\d{2})/);
                  const dateStr = dateMatch ? dateMatch[1] : 'a specific date';

                  this.notificationService.showNotification({
                    type: 'warning',
                    message: `Cannot copy to ${dateStr} as it already has 8 hours filled without overtime. Please use the overtime toggle if needed.`
                  });
                } else {
                  this.notificationService.showNotification({
                    type: 'error',
                    message: errorMessage
                  });
                }
              }
            });
        } else {
          // All dates were holidays, just reload to show the holiday entries
          this.loadTimeEntries();
        }
      }
    });
  }

  /**
   * Download time entries as CSV file
   */
  downloadCSV(): void {
    // Transform the data to a more CSV-friendly format
    const exportData = this.dataSource.data.map(item => {
      // Format date safely
      let formattedDate = '';
      if (item.date || item.entryDate) {
        const dateStr = item.date || item.entryDate;
        if (dateStr) {
          formattedDate = new Date(dateStr).toISOString().split('T')[0];
        }
      }

      return {
        'ID': item.id,
        'Date': formattedDate,
        'LDAP': item.ldap || '',
        'Lead': item.leadUsername || '',
        'Project Name': item.projectName || '',
        'Process': item.process || '',
        'Activity': item.activity || '',
        'Time (mins)': item.timeInMins || 0,
        'Attendance Type': item.attendanceType || '',
        'Overtime': item.isOvertime ? 'Yes' : 'No',
        'Comment': item.comment || '',
        'Status': item.status || ''
      };
    });

    const worksheet = XLSX.utils.json_to_sheet(exportData);
    const workbook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, worksheet, 'Time Entries');

    const excelBuffer: any = XLSX.write(workbook, { bookType: 'csv', type: 'array' });
    const blob = new Blob([excelBuffer], { type: 'text/csv;charset=utf-8;' });
    saveAs(blob, `time_entries_${new Date().toISOString().split('T')[0]}.csv`);

    this.notificationService.showNotification({
      type: 'success',
      message: 'Time entries exported successfully.'
    });
  }

  /**
   * Creates holiday entries for dates that are Google holidays
   * @param dates Array of date strings to check for holidays
   * @param sourceEntry The source time entry to use as template
   */
  private async createHolidayEntriesForDates(dates: string[], sourceEntry: TimeEntry): Promise<void> {
    const holidayEntries: any[] = [];

    for (const dateStr of dates) {
      try {
        // Check if this date is a Google holiday
        const holidayCheck = await this.http.get<BaseResponse<boolean>>(`${this.baseUrl}/api/holidays/check?date=${dateStr}`).toPromise();

        if (holidayCheck && holidayCheck.status === 'success' && holidayCheck.data === true) {
          // Get holiday details
          const holidayDetails = await this.http.get<BaseResponse<any>>(`${this.baseUrl}/api/holidays/date/${dateStr}`).toPromise();

          // Create holiday entry
          const holidayEntry = {
            projectId: sourceEntry.projectId,
            entryDate: dateStr,
            process: 'Holiday',
            activity: 'HOLIDAY',
            timeInMins: 480, // 8 hours for full day
            attendanceType: 'S1/GO',
            leadId: sourceEntry.leadId,
            comment: holidayDetails && holidayDetails.status === 'success'
              ? `Google Holiday - ${holidayDetails.data.holidayName}`
              : 'Google Holiday',
            ldap: sourceEntry.ldap || '',
            isOvertime: false
          };

          holidayEntries.push(holidayEntry);
        }
      } catch (error) {
        console.error(`Error checking holiday for date ${dateStr}:`, error);
        // Continue with other dates even if one fails
      }
    }

    // Create holiday entries in batch
    if (holidayEntries.length > 0) {
      try {
        const holidayBatchRequest = {
          entries: holidayEntries
        };

        await this.http.post<BaseResponse<any>>(`${this.baseUrl}/api/time-entries/batch-holidays`, holidayBatchRequest).toPromise();
        console.log(`Created ${holidayEntries.length} holiday entries successfully`);
      } catch (error) {
        console.error('Error creating holiday entries:', error);
        // Don't fail the entire operation if holiday creation fails
      }
    }
  }

  /**
   * Filters out holiday dates from the given array of dates
   * @param dates Array of date strings to filter
   * @returns Promise<string[]> Array of non-holiday dates
   */
  private async filterOutHolidayDates(dates: string[]): Promise<string[]> {
    const regularDates: string[] = [];

    for (const dateStr of dates) {
      try {
        // Check if this date is a Google holiday
        const holidayCheck = await this.http.get<BaseResponse<boolean>>(`${this.baseUrl}/api/holidays/check?date=${dateStr}`).toPromise();

        // If it's not a holiday, add it to regular dates
        if (!holidayCheck || holidayCheck.status !== 'success' || holidayCheck.data !== true) {
          regularDates.push(dateStr);
        }
      } catch (error) {
        console.error(`Error checking holiday for date ${dateStr}:`, error);
        // If there's an error checking, treat it as a regular date
        regularDates.push(dateStr);
      }
    }

    return regularDates;
  }

  /**
   * Formats a date for the API (YYYY-MM-DD)
   * @param date Date object to format
   * @returns Formatted date string
   */
  private formatDateForApi(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
