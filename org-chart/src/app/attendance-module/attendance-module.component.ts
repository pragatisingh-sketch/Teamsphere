import { AfterViewInit, Component, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { saveAs } from 'file-saver';
import * as XLSX from 'xlsx';
import { SelectionModel } from '@angular/cdk/collections';
import { MatSort } from '@angular/material/sort';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from '../confirm-dialog/confirmation-dialog.component';
import { NotificationService } from '../shared/notification.service';
import { TimeSheetService, Attendance } from '../services/timesheet.service';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-attendance-module',
  templateUrl: './attendance-module.component.html',
  styleUrls: ['./attendance-module.component.css']
})
export class AttendanceModuleComponent implements OnInit, AfterViewInit {

  dataSource = new MatTableDataSource<Attendance>([]);
  totalRecords = 0;
  showFilters: boolean = false;
  isLoading: boolean = false;

  // For column selection dropdown
  dropdownOpen: boolean = false;
  searchText: string = '';
  allColumns: { key: string, displayName: string }[] = [
    { key: 'id', displayName: 'ID' },
    { key: 'ldap', displayName: 'LDAP' },
    { key: 'masked_orgid', displayName: 'Masked Org ID' },
    { key: 'subrole', displayName: 'Subrole' },
    { key: 'role', displayName: 'Role' },
    { key: 'date', displayName: 'Date' },
    { key: 'process', displayName: 'Process' },
    { key: 'billingCode', displayName: 'Billing Code' },
    { key: 'activity', displayName: 'Activity' },
    { key: 'status', displayName: 'Status' },
    { key: 'lead_ldap', displayName: 'Lead LDAP' },
    { key: 'vendor', displayName: 'Vendor' },
    { key: 'minutes', displayName: 'Minutes' },
    { key: 'project', displayName: 'Project' },
    { key: 'team', displayName: 'Team' },
    { key: 'comment', displayName: 'Comment' }
  ];
  filteredColumns = [...this.allColumns];
  selectedColumns: string[] = ['select', 'id', 'ldap', 'role', 'date', 'status', 'team', 'actions'];
  displayedColumns: string[] = [...this.selectedColumns];

  // For record selection
  selection = new SelectionModel<Attendance>(true, []);
  selectedRecords: Attendance[] = [];

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild('csvInfoDialog') csvInfoDialog!: TemplateRef<any>;

  csvFormatExamples = [
    { columnName: 'ID', description: 'Unique identifier for the attendance record' },
    { columnName: 'Ldap', description: 'LDAP of the user' },
    { columnName: 'masked_orgid', description: 'Masked organization ID' },
    { columnName: 'subrole', description: 'Sub-role of the user' },
    { columnName: 'role', description: 'Role of the user' },
    { columnName: 'Date', description: 'Date of the attendance record in YYYY-MM-DD format' },
    { columnName: 'Process', description: 'Associated process' },
    { columnName: 'Billing Code', description: 'Code used for billing' },
    { columnName: 'Activity', description: 'Activity performed' },
    { columnName: 'status', description: 'Current status of the record' },
    { columnName: 'lead_ldap', description: 'LDAP of the team lead' },
    { columnName: 'vendor', description: 'Vendor name, if applicable' },
    { columnName: 'minutes', description: 'Duration in minutes' },
    { columnName: 'Project', description: 'Project name' },
    { columnName: 'team', description: 'Team name' },
    { columnName: 'Comment', description: 'Additional comments' }
  ];

  constructor(
    private router: Router,
    private dialog: MatDialog,
    private notificationService: NotificationService,
    private timeSheetService: TimeSheetService
  ) {}

  ngOnInit(): void {
    this.loadAttendanceData();

    this.dataSource.filterPredicate = (data: Attendance, filter: string) => {
      return Object.values(data)
        .some(value => value?.toString().toLowerCase().includes(filter.toLowerCase()));
    };
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  hasEditAccess(): boolean {
    return !(localStorage.getItem("role") === 'ACCOUNT_MANAGER');
  }

  loadAttendanceData(): void {
    this.isLoading = true;
    this.timeSheetService.getAttendanceRecords()

      .pipe(finalize(() => this.isLoading = false))
      .subscribe({
        next: (data) => {
          this.dataSource.data = data;
          this.totalRecords = data.length;
        },
        error: (error) => {
          console.error('Error fetching attendance data:', error);
          this.notificationService.showNotification({
            type: 'error',
            message: 'Failed to load attendance data. Please try again later.'
          });
        }
      });
  }

  // Toggle dropdown for column selection
  toggleDropdown(): void {
    this.dropdownOpen = !this.dropdownOpen;
    if (this.dropdownOpen) {
      this.filteredColumns = [...this.allColumns];
    }
  }

  // Apply global filter
  applyGlobalFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  // Apply filter for specific column
  applyFilter(event: Event, column: string): void {
    const filterValue = (event.target as HTMLInputElement).value.trim().toLowerCase();

    this.dataSource.filterPredicate = (data: any, filter: string) => {
      const columnValue = data[column];
      if (columnValue === null || columnValue === undefined) {
        return false;
      }

      return columnValue.toString().toLowerCase().includes(filter);
    };

    this.dataSource.filter = filterValue;

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  // CSV operations
  onFileUpload(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.isLoading = true;
      this.timeSheetService.importFromCSV(file)
        .pipe(finalize(() => {
          this.isLoading = false;
          event.target.value = '';
        }))
        .subscribe({
          next: (importedRecords) => {
            this.dataSource.data = [...this.dataSource.data, ...importedRecords];
            this.notificationService.showSuccess(`Successfully imported ${importedRecords.length} records.`);
          },
          error: (error) => {
            console.error('Error importing CSV:', error);
            const errorMessage = error.error?.message || 'Failed to import CSV. Please check the file format and try again.';
            this.notificationService.showError(errorMessage);
          }
        });
    }
  }

  downloadCSV(): void {
    this.isLoading = true;
    this.timeSheetService.exportToCSV()
      .pipe(finalize(() => this.isLoading = false))
      .subscribe({
        next: (blob) => {
          saveAs(blob, `attendance_records_${new Date().toISOString().split('T')[0]}.csv`);
          this.notificationService.showNotification({
            type: 'success',
            message: 'CSV file downloaded successfully.'
          });
        },
        error: (error) => {
          console.error('Error downloading CSV:', error);
          this.notificationService.showNotification({
            type: 'error',
            message: 'Failed to download CSV. Please try again later.'
          });
        }
      });
  }

  openCsvInfoDialog(): void {
    this.dialog.open(this.csvInfoDialog, {
      width: '600px'
    });
  }

  // Selection methods
  isAllSelected(): boolean {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows && numRows > 0;
  }

  isIndeterminate(): boolean {
    return this.selection.selected.length > 0 && !this.isAllSelected();
  }

  masterToggle(): void {
    if (this.isAllSelected()) {
      this.selection.clear();
      this.selectedRecords = [];
    } else {
      this.dataSource.data.forEach(row => this.selection.select(row));
      this.selectedRecords = [...this.dataSource.data];
    }
  }

  toggleSelection(record: Attendance): void {
    this.selection.toggle(record);

    if (this.selection.isSelected(record)) {
      this.selectedRecords.push(record);
    } else {
      this.selectedRecords = this.selectedRecords.filter(r => r.id !== record.id);
    }
  }

  isSelected(record: Attendance): boolean {
    return this.selection.isSelected(record);
  }

  deleteSelectedRecords(): void {
    if (this.selectedRecords.length === 0) return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Confirm Deletion',
        message: `Are you sure you want to delete ${this.selectedRecords.length} selected record(s)? This action cannot be undone.`
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        const ids = this.selectedRecords.map(record => record.id);
        this.isLoading = true;
        this.timeSheetService.deleteMultipleRecords(ids)
          .pipe(finalize(() => this.isLoading = false))
          .subscribe({
            next: () => {
              this.dataSource.data = this.dataSource.data.filter(record => !ids.includes(record.id));
              this.selection.clear();
              this.selectedRecords = [];
              this.notificationService.showNotification({
                type: 'success',
                message: `Successfully deleted ${ids.length} record(s).`
              });
            },
            error: (error) => {
              console.error('Error deleting records:', error);
              this.notificationService.showNotification({
                type: 'error',
                message: 'Failed to delete records. Please try again later.'
              });
            }
          });
      }
    });
  }


  openAddTimeSheetForm()
  {
    // this.router.navigate(['/addtimesheet']);
  }

  // Column selection methods
  filterColumns(): void {
    const searchLower = this.searchText.toLowerCase();
    this.filteredColumns = this.allColumns.filter(col =>
      col.displayName.toLowerCase().includes(searchLower));
  }

  toggleSelectAll(): void {
    const mandatoryColumns = new Set(['select', 'id', 'ldap', 'role', 'date', 'status', 'team']);
    const actionColumn = 'actions';

    if (this.selectedColumns.length === this.allColumns.length) {
      this.selectedColumns = [...mandatoryColumns, actionColumn];
    } else {
      this.selectedColumns = [...new Set([...mandatoryColumns, ...this.allColumns.map(col => col.key), actionColumn])];
    }

    const updatedColumns = this.selectedColumns.filter(col => !mandatoryColumns.has(col) && col !== actionColumn);
    this.displayedColumns = [...mandatoryColumns, ...updatedColumns, actionColumn];
  }

  updateDisplayedColumns(event: Event, columnKey: string): void {
    const isChecked = (event.target as HTMLInputElement).checked;
    const mandatoryColumns = new Set(['select', 'id', 'ldap', 'role', 'date', 'status', 'team']);
    const actionColumn = 'actions';

    if (isChecked) {
      this.selectedColumns = [...new Set([...this.selectedColumns, columnKey])];
    } else {
      this.selectedColumns = this.selectedColumns.filter(col => col !== columnKey);
    }

    const updatedColumns = this.selectedColumns.filter(col => !mandatoryColumns.has(col) && col !== actionColumn);
    this.displayedColumns = [...mandatoryColumns, ...updatedColumns, actionColumn];
  }

  // Record actions
  viewRecord(record: Attendance): void {
    // In a real application, navigate to a details view
    console.log('View record:', record);
    // this.router.navigate(['/attendance', record.id]);
  }

  editRecord(record: Attendance): void {
    // In a real application, navigate to an edit form or open a dialog
    console.log('Edit record:', record);
    // this.router.navigate(['/attendance', record.id, 'edit']);
  }

  deleteRecord(record: Attendance): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Confirm Deletion',
        message: 'Are you sure you want to delete this record? This action cannot be undone.'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.isLoading = true;
        this.timeSheetService.deleteAttendanceRecord(record.id)
          .pipe(finalize(() => this.isLoading = false))
          .subscribe({
            next: () => {
              this.dataSource.data = this.dataSource.data.filter(item => item.id !== record.id);
              this.notificationService.showNotification({
                type: 'success',
                message: 'Record deleted successfully.'
              });
            },
            error: (error) => {
              console.error('Error deleting record:', error);
              this.notificationService.showNotification({
                type: 'error',
                message: 'Failed to delete record. Please try again later.'
              });
            }
          });
      }
    });
  }

  generateFromBitrix(): void {
    this.isLoading = true;
    this.notificationService.showNotification({
      type: 'info',
      message: 'Fetching data from Bitrix. This may take a moment...'
    });

    this.timeSheetService.fetchFromBitrix()
      .pipe(finalize(() => this.isLoading = false))
      .subscribe({
        next: (records) => {
          this.dataSource.data = [...this.dataSource.data, ...records];
          this.notificationService.showNotification({
            type: 'success',
            message: `Successfully imported ${records.length} records from Bitrix!`
          });
        },
        error: (error) => {
          console.error('Error fetching from Bitrix:', error);
          this.notificationService.showNotification({
            type: 'error',
            message: 'Failed to fetch data from Bitrix. Please try again later.'
          });
        }
      });
  }
}
