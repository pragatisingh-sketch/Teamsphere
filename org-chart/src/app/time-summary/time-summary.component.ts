import { Component, OnDestroy, OnInit, ViewChild, ElementRef } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatMenuTrigger } from '@angular/material/menu';
import { TimeSummaryService } from '../services/time-summary.service';
import { TimeEntryHierarchicalSummary } from '../model/time-summary.interface';
import { AuthService } from '../auth.service';
import { Router } from '@angular/router';
import { Project } from '../model/project.interface';
import { User } from '../model/user.interface';
import { FormBuilder, FormGroup, FormControl } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged, map } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { FilterDialogComponent } from './filter-dialog/filter-dialog.component';
import * as XLSX from 'xlsx';
import { saveAs } from 'file-saver';

@Component({
  selector: 'app-time-summary',
  templateUrl: './time-summary.component.html',
  styleUrls: ['./time-summary.component.css']
})
export class TimeSummaryComponent implements OnInit, OnDestroy {
  // Role-based visibility
  canAccessTimeSummary = false;
  isManagerOrAdmin = false;

  // Selection for bulk actions
  selection = new Set<number>();
  selectAll = false;

  displayedColumns: string[] = [
    'select',
    'projectName',
    'projectCode',
    'username',
    'totalTimeInMins',
    'totalEntries',
    'startDate',
    'endDate'
  ];

  dataSource: MatTableDataSource<TimeEntryHierarchicalSummary>;
  filterForm: FormGroup;
  timeUnits = ['DAY', 'WEEK', 'MONTH'];
  isLoading = false;

  // Dropdown data
  allUsers: User[] = [];
  allProjects: Project[] = [];
  filteredUsers: User[] = [];
  filteredProjects: Project[] = [];

  // Form controls for search
  userSearchControl = new FormControl('');
  projectSearchControl = new FormControl('');
  selectedUser: User | null = null;
  selectedProject: Project | null = null;

  // Add new properties for column filtering
  filterValues: { [key: string]: string[] } = {};
  columnUniqueValues: { [key: string]: string[] } = {};
  showColumnFilters: boolean = false;
  currentFilterMenuState = {
    columnKey: null as string | null,
    tempSelectedValues: [] as string[],
    searchText: ''
  };

  private destroy$ = new Subject<void>();

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild('globalSearchInput') globalSearchInput!: ElementRef<HTMLInputElement>;

  constructor(
    private timeSummaryService: TimeSummaryService,
    private fb: FormBuilder,
    private dialog: MatDialog,
    private authService: AuthService,
    private router: Router
  ) {
    this.dataSource = new MatTableDataSource<TimeEntryHierarchicalSummary>();
    this.filterForm = this.fb.group({
      userId: [''],
      projectId: [''],
      startDate: [''],
      endDate: ['']
    });
  }

  ngOnInit(): void {
    // Check role-based access
    this.checkAccess();
    if (!this.canAccessTimeSummary) {
      this.router.navigate(['/dashboard']);
      return;
    }

    this.loadUsers();
    this.loadProjects();
    this.loadData();
    this.setupUserSearch();
    this.setupProjectSearch();
    this.initializeTable();
    this.initializeColumnFilters();
  }

  checkAccess(): void {
    const role = localStorage.getItem('role');
    this.isManagerOrAdmin = role === 'ADMIN_OPS_MANAGER' || role === 'MANAGER';
    this.canAccessTimeSummary = this.isManagerOrAdmin;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  private setupUserSearch(): void {
    this.userSearchControl.valueChanges
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(300),
        distinctUntilChanged(),
        map(value => typeof value === 'string' ? value.toLowerCase() : '')
      )
      .subscribe(value => {
        if (value) {
          this.filteredUsers = this.allUsers.filter(user =>
            user.ldap.toLowerCase().includes(value)
          );
        } else {
          this.filteredUsers = this.allUsers;
        }
      });
  }

  private setupProjectSearch(): void {
    this.projectSearchControl.valueChanges
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(300),
        distinctUntilChanged(),
        map(value => typeof value === 'string' ? value.toLowerCase() : '')
      )
      .subscribe(value => {
        if (value) {
          this.filteredProjects = this.allProjects.filter(project =>
            project.projectName.toLowerCase().includes(value) ||
            project.projectCode.toLowerCase().includes(value)
          );
        } else {
          this.filteredProjects = this.allProjects;
        }
      });
  }

  onUserSelected(event: any): void {
    if (event.option && event.option.value) {
      const user = event.option.value as User;
      if (user) {
        this.selectedUser = user;
        this.filterForm.patchValue({
          userId: user.id
        });
        this.loadData();
      }
    }
  }

  onProjectSelected(event: any): void {
    if (event.option && event.option.value) {
      const project = event.option.value as Project;
      if (project) {
        this.selectedProject = project;
        this.filterForm.patchValue({
          projectId: project.id
        });
        this.loadData();
      }
    }
  }

  getUserDisplayName(user: User | null): string {
    if (!user) return '';
    return `${user.ldap}`;
  }

  getProjectDisplayName(project: Project | null): string {
    if (!project) return '';
    return `${project.projectName} (${project.projectCode})`;
  }

  loadData(): void {
    this.isLoading = true;
    const filters = {
      ...this.filterForm.value,
      ldap: this.selectedUser?.ldap,
      projectId: this.selectedProject?.id
    };
    console.log(filters);
    this.timeSummaryService.getHierarchicalSummary(filters).subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.initializeColumnFilters();

        // Ensure paginator is properly connected after data load
        if (this.paginator) {
          this.dataSource.paginator = this.paginator;
          this.paginator.firstPage();
        }

        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading time summary:', error);
        this.isLoading = false;
      }
    });
  }

  applyFilter(event: Event): void {
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

  onFilterSubmit(): void {
    this.loadData();
  }

  resetFilters(): void {
    this.filterForm.reset();
    this.userSearchControl.reset('');
    this.projectSearchControl.reset('');
    this.selectedUser = null;
    this.selectedProject = null;
    this.filteredUsers = this.allUsers;
    this.filteredProjects = this.allProjects;

    // Clear column filters and global search
    this.filterValues = {};
    this.dataSource.filter = '';

    // Clear global search input
    if (this.globalSearchInput) {
      this.globalSearchInput.nativeElement.value = '';
    }

    this.loadData();
  }

  formatTimeInMins(minutes: number): string {
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return `${hours}h ${mins}m`;
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString();
  }

  private loadUsers(): void {
    this.timeSummaryService.getAllUsers()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (users) => {
          this.allUsers = users;
          this.filteredUsers = users;
        },
        error: (error) => console.error('Error loading Users:', error)
      });
  }

  private loadProjects(): void {
    this.timeSummaryService.getAllProjects()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (projects) => {
          this.allProjects = projects;
          this.filteredProjects = projects;
        },
        error: (error) => console.error('Error loading Projects:', error)
      });
  }

  openFilterDialog(): void {
    const dialogRef = this.dialog.open(FilterDialogComponent, {
      width: '800px',
      data: {
        users: this.allUsers,
        projects: this.allProjects,
        filters: this.filterForm.value
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.filterForm.patchValue(result.filters);
        this.selectedUser = result.selectedUser;
        this.selectedProject = result.selectedProject;
        this.loadData();
      }
    });
  }

  downloadCSV(): void {
    // Transform the data to a more CSV-friendly format
    const exportData = this.dataSource.data.map(item => ({
      'Project Name': item.projectName,
      'Project Code': item.projectCode,
      'Username': item.username,
      'Total Time': this.formatTimeInMins(item.totalTimeInMins),
      'Total Entries': item.totalEntries,
      'Start Date': this.formatDate(item.startDate),
      'End Date': this.formatDate(item.endDate)
    }));

    const worksheet = XLSX.utils.json_to_sheet(exportData);
    const workbook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, worksheet, 'Time Summary');

    const excelBuffer: any = XLSX.write(workbook, { bookType: 'csv', type: 'array' });
    const blob = new Blob([excelBuffer], { type: 'text/csv;charset=utf-8;' });
    saveAs(blob, `time_summary_${new Date().toISOString().split('T')[0]}.csv`);
  }

  // Initialize the filter predicate in your initialization logic
  private initializeTable(): void {
    this.dataSource.filterPredicate = (data: any, filter: string): boolean => {
      try {
        // Try to parse as JSON for column filters
        const searchString = JSON.parse(filter);

        // Handle column filters (object)
        if (typeof searchString === 'object' && searchString !== null) {
          for (const key in searchString) {
            if (searchString.hasOwnProperty(key) && searchString[key]?.length > 0) {
              const columnValue = (data as any)[key]?.toString().toLowerCase() || '';
              if (!searchString[key].some((filterValue: string) =>
                columnValue.includes(filterValue.toLowerCase()))) {
                return false;
              }
            }
          }
          return true;
        }

        // Handle simple string filter (shouldn't reach here normally)
        return Object.values(data)
          .some(value => value?.toString().toLowerCase().includes(searchString.toLowerCase()));

      } catch (e) {
        // Handle global search (simple string filter)
        if (filter && filter.trim()) {
          return Object.values(data)
            .some(value => value?.toString().toLowerCase().includes(filter.toLowerCase()));
        }
        return true;
      }
    };
  }

  // Initialize column filters
  private initializeColumnFilters(): void {
    this.columnUniqueValues = {};

    // Loop through each column
    this.displayedColumns.forEach(columnKey => {
      if (columnKey !== 'actions') {
        // Get unique values for the column
        const values = Array.from(new Set(
          this.dataSource.data
            .map(item => String((item as any)[columnKey]))
            .filter(value => value !== null && value !== undefined && value !== '')
        )).sort();

        // Store the unique values
        this.columnUniqueValues[columnKey] = values;
        console.log(`Column ${columnKey} unique values:`, values); // Debug log
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

    // Clear global search input when column filters are applied
    if (this.globalSearchInput && Object.keys(activeFilters).length > 0) {
      this.globalSearchInput.nativeElement.value = '';
    }

    this.dataSource.filter = JSON.stringify(activeFilters);

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  toggleColumnFilters(): void {
    this.showColumnFilters = !this.showColumnFilters;
  }

  // Get row class for highlighting
  getRowClass(row: TimeEntryHierarchicalSummary): string {
    const weeklyThreshold = 2400; // 40 hours * 60 minutes  
    return row.totalTimeInMins < weeklyThreshold ? 'under-threshold' : '';
  }

  // Check if row is selected
  isSelected(row: TimeEntryHierarchicalSummary): boolean {
    const id = this.getRowId(row);
    return this.selection.has(id);
  }

  // Get unique ID for row
  private getRowId(row: TimeEntryHierarchicalSummary): number {
    return JSON.stringify({
      p: row.projectCode,
      u: row.username,
      s: row.startDate,
      e: row.endDate
    }).split('').reduce((a, b) => {
      a = ((a << 5) - a) + b.charCodeAt(0);
      return a & a;
    }, 0);
  }

  // Toggle row selection
  toggleSelection(row: TimeEntryHierarchicalSummary): void {
    const id = this.getRowId(row);
    if (this.selection.has(id)) {
      this.selection.delete(id);
    } else {
      this.selection.add(id);
    }
    this.updateSelectAllState();
  }

  // Toggle select all
  toggleSelectAll(): void {
    if (this.selectAll) {
      this.selection.clear();
    } else {
      this.dataSource.filteredData.forEach(row => {
        this.selection.add(this.getRowId(row));
      });
    }
    this.updateSelectAllState();
  }

  private updateSelectAllState(): void {
    const totalRows = this.dataSource.filteredData.length;
    const selectedRows = this.selection.size;
    this.selectAll = totalRows > 0 && selectedRows === totalRows;
  }

  getSelectedCount(): number {
    return this.selection.size;
  }

  // Open time entry detail  
  openTimeEntryDetail(row: TimeEntryHierarchicalSummary): void {
    console.log('Opening detail for:', row);
    alert(`Time Entry Details\n\nProject: ${row.projectName}\nUser: ${row.username}\nTotal Time: ${this.formatTimeInMins(row.totalTimeInMins)}\nEntries: ${row.totalEntries}`);
  }

  // Bulk approve
  bulkApprove(): void {
    if (this.selection.size === 0) {
      alert('Please select at least one entry to approve.');
      return;
    }
    if (confirm(`Approve ${this.selection.size} selected entries?`)) {
      console.log('Bulk approving:', Array.from(this.selection));
      alert('Bulk approve backend endpoint to be implemented.');
      this.selection.clear();
      this.updateSelectAllState();
    }
  }

  // Bulk reject
  bulkReject(): void {
    if (this.selection.size === 0) {
      alert('Please select at least one entry to reject.');
      return;
    }
    const reason = prompt(`Rejecting ${this.selection.size} entries.\n\nProvide reason:`);
    if (reason && reason.trim()) {
      console.log('Bulk rejecting:', Array.from(this.selection), 'Reason:', reason);
      alert('Bulk reject backend endpoint to be implemented.');
      this.selection.clear();
      this.updateSelectAllState();
    }
  }

  // Add property to track current trigger  
  private currentTrigger: MatMenuTrigger | null = null;
}
