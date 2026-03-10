import { AfterViewInit, Component, OnInit, TemplateRef, ViewChild, HostListener, ElementRef } from '@angular/core';
import { Router } from '@angular/router';
import { UserService } from '../user.service';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { saveAs } from 'file-saver';
import * as XLSX from 'xlsx';
import { User } from '../model/user';
import { NotificationService } from '../shared/notification.service';
import { SelectionModel } from '@angular/cdk/collections';
import { MatSort } from '@angular/material/sort';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from '../confirm-dialog/confirmation-dialog.component';
import { EditLogsDialogComponent } from '../edit-logs-dialog/edit-logs-dialog.component';
import { DelegationDialogComponent } from '../delegation-dialog/delegation-dialog.component';
import { DelegationHistoryDialogComponent } from '../delegation-history-dialog/delegation-history-dialog.component';
import { DateFormatService } from '../services/date-format.service';
import { MatMenuTrigger } from '@angular/material/menu';
import { PermissionService, UserRole } from '../shared/permission.service';
import { DatabaseService } from '../services/database.service';


@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css']
})
export class AdminDashboardComponent implements OnInit, AfterViewInit {

  userRole: string | undefined;
  originalDataSourceData: User[] = []; // Store original data

  // Available roles for the dropdown
  availableRoles: UserRole[] = ['USER', 'LEAD', 'MANAGER', 'ADMIN_OPS_MANAGER'];

  // Use the permission service for secure role-based access control
  hasEditAccess(): boolean {
    // Only ACCOUNT_MANAGER cannot edit
    const role = localStorage.getItem("role");
    return role !== 'ACCOUNT_MANAGER';
  }

  hasAdminOpsManagerRole(): boolean {
    // Use the permission service to check for ADMIN_OPS_MANAGER role
    const role = localStorage.getItem("role");
    return role === 'ADMIN_OPS_MANAGER';
  }

  hasPasswordResetAccess(): boolean {
    // ADMIN_OPS_MANAGER, LEAD, and MANAGER can reset passwords
    const role = localStorage.getItem("role");
    return role === 'ADMIN_OPS_MANAGER' || role === 'LEAD' || role === 'MANAGER';
  }

  dataSource = new MatTableDataSource<User>([]);
  totalRecords = 0;
  showFilters: boolean = false;

  // --- New property for toggling column filter buttons ---
  showColumnFilters: boolean = false;
  // --- End new property ---

  // --- New properties for column filters ---
  filterValues: { [key: string]: string[] } = {};
  columnUniqueValues: { [key: string]: string[] } = {};
  // Define which columns are filterable with the new dropdown style
  // filterableColumns: string[] = ['team', 'level', 'location', 'status', 'programManager', 'lead', 'vendor']; // REMOVED - Will derive from allColumns
  // --- End new properties ---

  // --- State for the currently open filter menu ---
  currentFilterMenuState: {
    columnKey: string | null,
    tempSelectedValues: string[],
    searchText: string // Added search text
  } = {
      columnKey: null,
      tempSelectedValues: [],
      searchText: '' // Initialize search text
    };
  // --- End filter menu state ---

  user: User = {
    firstName: '',
    lastName: '',
    startDate: '',
    team: '',
    newLevel: '',
    lead: '',
    programManager: '',
    vendor: '',
    email: '',
    status: '',
    profilePic: '',
    lwdMlStartDate: '',
    process: '',
    resignationDate: '',
    roleChangeEffectiveDate: '',
    levelBeforeChange: '',
    levelAfterChange: '',
    lastBillingDate: '',
    backfillLdap: '',
    billingStartDate: '',
    language: '',
    tenureTillDate: '',
    // addedInGoVfsWhoMain: '',
    //addedInGoVfsWhoInactive: '',
    id: undefined,
    parent: undefined,
    ldap: '',
    level: '',
    inactiveReason: '',
    pnseProgram: '',
    location: '',
    shift: '',
    inactive: false
  };

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;


  @ViewChild('csvInfoDialog') csvInfoDialog!: TemplateRef<any>;

  csvFormatExamples = [
    { columnName: 'First Name', description: 'User\'s first name' },
    { columnName: 'Last Name', description: 'User\'s last name' },
    { columnName: 'Start Date', description: 'Start date in YYYY-MM-DD format' },
    { columnName: 'Ldap', description: 'LDAP of the user must be unique' },
    { columnName: 'Tenure', description: 'User\'s tenure is automated from backend' },
    { columnName: 'Manager', description: 'Ldap Assigned program manager' },
    { columnName: 'Lead', description: 'Assigned team lead' },
    { columnName: 'Team', description: 'Team name the user belongs to' },
    { columnName: 'PS&E Program', description: 'Program Enrolled' },
    { columnName: 'New Level effective', description: 'New effective level after promotion' },
    { columnName: 'Vendor', description: 'Vendor name, if applicable' },
    { columnName: 'Email Address', description: 'User\'s email address' },
    { columnName: 'Status', description: 'Current employment status (e.g., Active, Inactive)' },
    { columnName: 'LWD / ML Start Date', description: 'Last Working Date or Maternity Leave Start Date' },
    { columnName: 'Process', description: 'Associated process or workflow' },
    { columnName: 'Resignation / ML(Inactive Reason)', description: 'Reason for inactive status, whether it is Maternity Leave or Resignation' },
    { columnName: 'Role change effective date', description: 'Effective date for role change' },
    { columnName: 'Level (Before Change)', description: 'Level before role change' },
    { columnName: 'Level (After Change)', description: 'Level after role change' },
    { columnName: 'Last Billing Date', description: 'Date of last billing cycle' },
    { columnName: 'Backfill Ldap', description: 'LDAP of the backfill resource' },
    { columnName: 'Billing Start Date', description: 'Billing start date in YYYY-MM-DD format' },
    { columnName: 'Language', description: 'Primary language used' },
    { columnName: 'Level', description: 'Current designation must be Program Manager, Team Lead, Tech I, Tech II, Tech III or i18n ' },
    { columnName: 'Location', description: 'Location of the user' }
  ];


  constructor(
    private userService: UserService,
    private router: Router,
    private notificationService: NotificationService,
    private dialog: MatDialog,
    private dateFormatService: DateFormatService,
    private permissionService: PermissionService,
    private databaseService: DatabaseService,
    private elementRef: ElementRef
  ) { }

  ngOnInit(): void {
    this.getUserData();
    this.initializeTable();
  }

  initializeTable() {
    // Define the filter predicate for multi-column filtering
    this.dataSource.filterPredicate = (data: User, filter: string): boolean => {
      const searchString = JSON.parse(filter); // We'll pass the filterValues object as a JSON string

      // Check global search first (if applicable, can be combined later)
      // For now, focus on column filters

      // Iterate over active column filters
      for (const key in searchString) {
        if (searchString.hasOwnProperty(key) && searchString[key]?.length > 0) {
          const columnValue = (data as any)[key]?.toString().toLowerCase() || '';

          // Special handling for combined 'firstName' + 'lastName' if needed later
          // if (key === 'firstName') { ... }

          if (!searchString[key].some((filterValue: string) => columnValue.includes(filterValue.toLowerCase()))) {
            // If the column value doesn't match ANY of the selected filter values for this column, exclude the row
            return false;
          }
        }
      }
      return true; // Include the row if it passes all active column filters
    };

    // Custom sorting for 'firstName' to include 'lastName'
    this.dataSource.sortingDataAccessor = (item, property) => {
      switch (property) {
        case 'firstName': return `${item.firstName} ${item.lastName}`;
        // Add other custom sorting logic if needed
        default: return (item as any)[property];
      }
    };
  }

  ngAfterViewInit(): void {
    console.log("Logging the sort ", this.sort);
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;

  }

  openCsvInfoDialog() {
    this.dialog.open(this.csvInfoDialog);
  }

  getUserData(): void {
    this.userService.getUsers().subscribe(
      (response) => {
        // Transform the employee data to match the User type
        const transformedData: User[] = response.data.map((employee) => ({
          parent: employee.parent || '',
          id: employee.id || '',
          firstName: employee.firstName || '',
          lastName: employee.lastName || '',
          ldap: employee.ldap || '',
          startDate: this.dateFormatService.formatDate(employee.startDate),
          team: employee.team || '',
          newLevel: employee.newLevel || '',
          programManager: employee.programManager || '',
          lead: employee.lead || '',
          vendor: employee.vendor || '',
          email: employee.email || '',
          status: employee.status || '',
          lwdMlStartDate: this.dateFormatService.formatDate(employee.lwdMlStartDate),
          process: employee.process || '',
          resignationDate: this.dateFormatService.formatDate(employee.resignationDate),
          roleChangeEffectiveDate: this.dateFormatService.formatDate(employee.roleChangeEffectiveDate),
          levelBeforeChange: employee.levelBeforeChange || '',
          levelAfterChange: employee.levelAfterChange || '',
          lastBillingDate: this.dateFormatService.formatDate(employee.lastBillingDate),
          backfillLdap: employee.backfillLdap || '',
          billingStartDate: this.dateFormatService.formatDate(employee.billingStartDate),
          language: employee.language || '',
          tenureTillDate: employee.tenureTillDate || '',
          profilePic: employee.profilePic || '',
          level: employee.level || '',
          inactiveReason: employee.inactiveReason || '',
          pnseProgram: employee.pnseProgram || '',
          location: employee.location || '',
          shift: employee.shift || '',
          inactive: employee.inactive || false
        }));

        console.log("Logging the transformed data ", transformedData);
        // Set the transformed data to the dataSource
        this.dataSource.data = transformedData;
        this.originalDataSourceData = [...transformedData]; // Store a copy for resetting filters
        this.totalRecords = transformedData.length; // Set the total number of records

        // Initialize unique values for filters after data is loaded
        this.initializeColumnFilters();
      },
      (error) => {
        console.error('Error fetching employee data:', error);
        this.notificationService.showNotification({
          type: 'error',
          message: error.error?.message || 'Failed to fetch employee data'
        });
      }
    );
  }

  // --- New methods for column filters ---

  initializeColumnFilters(): void {
    this.columnUniqueValues = {}; // Reset
    // Iterate over all defined columns, excluding special ones
    this.allColumns.forEach(column => {
      const columnKey = column.key;
      if (columnKey !== 'select' && columnKey !== 'actions' && columnKey !== 'profilePic') { // Exclude non-data/non-filterable columns
        const values = this.originalDataSourceData
          .map(user => (user as any)[columnKey])
          .filter((value, index, self) => value !== null && value !== undefined && value !== '' && self.indexOf(value) === index); // Get unique, non-empty values

        // Convert numbers/dates to string for consistent filtering if needed, although default toString should work
        this.columnUniqueValues[columnKey] = values.map(v => v.toString()).sort();

        // Initialize filterValues for this column
        this.filterValues[columnKey] = [];
      }
    });
  }

  applyColumnFilters(): void {
    // We apply the filters by setting the 'filter' property of the dataSource.
    // The filterPredicate defined in initializeTable will then use this.filterValues.
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

  // Called from the filter menu's Apply button
  onFilterApplied(): void { // No arguments needed, uses component state
    if (this.currentFilterMenuState.columnKey) {
      this.filterValues[this.currentFilterMenuState.columnKey] = [...this.currentFilterMenuState.tempSelectedValues]; // Apply from temp state
      this.applyColumnFilters();
    }
    this.resetCurrentFilterMenuState(); // Reset after applying
  }

  // Called from the filter menu's Clear button
  clearColumnFilter(): void { // No arguments needed
    if (this.currentFilterMenuState.columnKey) {
      const key = this.currentFilterMenuState.columnKey;
      if (this.filterValues[key]?.length > 0) {
        this.filterValues[key] = [];
        this.applyColumnFilters();
      }
      this.resetCurrentFilterMenuState(); // Reset after clearing
    }
  }

  // Helper to get unique values for the template
  getUniqueColumnValues(columnKey: string): string[] {
    return this.columnUniqueValues[columnKey] || [];
  }

  // Helper to check if a filter is active for UI indication
  isFilterActive(columnKey: string): boolean {
    return this.filterValues[columnKey]?.length > 0;
  }

  // Reset all column filters
  resetAllFilters(): void {
    this.filterValues = {};
    // Use allColumns to reset filters for initialized columns
    this.allColumns.forEach((col: { key: string }) => {
      if (this.columnUniqueValues.hasOwnProperty(col.key)) { // Only reset if it was initialized
        this.filterValues[col.key] = [];
      }
    });
    this.applyColumnFilters(); // Apply the empty filters
  }

  // --- End new methods ---

  openAddUserForm() {
    this.router.navigate(['/admin/add-user']);
  }

  viewUser(user: any) {
    console.log("User ", user);
    if (!user || !user.id) {
      this.notificationService.showError('Invalid user data. Cannot view user details.');
      return;
    }
    // Check if the user is inactive based on inactive field
    const isInactive = user.inactive === true;
    this.router.navigate(['/user-details', user.id], { queryParams: { isInactive } });
  }

  editUser(user: { id: any; }) {
    this.router.navigate(['/admin/edit-user', user.id]);
  }

  deleteUser(user: { id: any; firstName?: string; lastName?: string; }) {
    // Open confirmation dialog before deleting
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Confirm Deletion',
        message: `Are you sure you want to delete ${user.firstName || ''} ${user.lastName || ''}? This action cannot be undone.`,
        color: 'warn',
        confirmButtonText: 'Delete'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        // User confirmed deletion, proceed with delete operation
        this.userService.deleteUser(user.id).subscribe({
          next: (response) => {
            this.notificationService.showNotification({
              type: 'success',
              message: response.message
            });
            this.getUserData();
          },
          error: (error) => {
            if (error.status === 409) {
              this.notificationService.showNotification({
                type: 'error',
                message: error.error?.message || 'A request has already been made for this user. Please contact Admin Ops Manager for details.',
              });
            } else {
              this.notificationService.showNotification({
                type: 'error',
                message: error.error?.message || 'Failed to delete the user. Please try again later.'
              });
            }
          }
        });
      }
      // If result is false, user cancelled the deletion, so do nothing
    });
  }

  changeUserRole(user: User, newRole: UserRole) {
    // Use the permission service to check if the user can edit roles
    this.permissionService.canEditRoles().subscribe(canEdit => {
      if (!canEdit) {
        this.notificationService.showNotification({
          type: 'error',
          message: 'Only Admin Ops Manager can change user roles.'
        });
        return;
      }

      if (!user.ldap) {
        this.notificationService.showNotification({
          type: 'error',
          message: 'User LDAP is required to change role.'
        });
        return;
      }

      const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
        width: '400px',
        data: {
          title: 'Confirm Role Change',
          message: `Are you sure you want to change ${user.firstName} ${user.lastName}'s role to ${newRole}?`
        }
      });

      dialogRef.afterClosed().subscribe(result => {
        if (result) {
          this.userService.changeUserRole(user.ldap, newRole).subscribe({
            next: (response) => {
              this.notificationService.showNotification({
                type: 'success',
                message: response.message
              });
              this.getUserData(); // Refresh the data
            },
            error: (err) => {
              console.error('Error changing role:', err);
              this.notificationService.showNotification({
                type: 'error',
                message: err.error?.message || 'Failed to change role. Please try again later.'
              });
            }
          });
        }
      });
    });
  }

  resetUserPassword(user: User) {
    // Use the permission service to check if the user can reset passwords
    this.permissionService.canResetPasswords().subscribe(canReset => {
      if (!canReset) {
        this.notificationService.showNotification({
          type: 'error',
          message: 'Only Admin Ops Manager, Leads, and Managers can reset user passwords.'
        });
        return;
      }

      if (!user.ldap) {
        this.notificationService.showNotification({
          type: 'error',
          message: 'User LDAP is required to reset password.'
        });
        return;
      }

      const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
        width: '400px',
        data: {
          title: 'Confirm Password Reset',
          message: `Are you sure you want to reset ${user.firstName} ${user.lastName}'s password to the default value?`,
          color: 'primary',
          confirmButtonText: 'Reset Password'
        }
      });

      dialogRef.afterClosed().subscribe(result => {
        if (result) {
          this.userService.resetUserPassword(user.ldap).subscribe({
            next: () => {
              this.notificationService.showNotification({
                type: 'success',
                message: 'Password reset successfully to default value.'
              });
            },
            error: (err) => {
              console.error('Error resetting password:', err);
              this.notificationService.showNotification({
                type: 'error',
                message: 'Failed to reset password. Please try again later.'
              });
            }
          });
        }
      });
    });
  }

  /**
   * Open dialog to view edit logs for a user
   * @param user The user to view edit logs for
   */
  viewEditLogs(user: User): void {
    if (!user.ldap) {
      this.notificationService.showNotification({
        type: 'error',
        message: 'User LDAP is required to view edit logs.'
      });
      return;
    }

    const dialogRef = this.dialog.open(EditLogsDialogComponent, {
      width: '900px',
      data: { user: user }
    });
  }

  /**
   * Open dialog to view delegation history for a user
   * @param user The user to view delegation history for
   */
  viewDelegationHistory(user: User): void {
    if (!user.ldap) {
      this.notificationService.showNotification({
        type: 'error',
        message: 'User LDAP is required to view delegation history.'
      });
      return;
    }

    const dialogRef = this.dialog.open(DelegationHistoryDialogComponent, {
      width: '800px',
      data: { user: user }
    });
  }

  /**
   * Trigger a database backup
   * Only available to users with ADMIN_OPS_MANAGER role
   */
  triggerDatabaseBackup() {
    // Check if user has ADMIN_OPS_MANAGER role
    if (!this.hasAdminOpsManagerRole()) {
      this.notificationService.showNotification({
        type: 'error',
        message: 'Only Admin Ops Manager can trigger database backups.'
      });
      return;
    }

    // Show confirmation dialog
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Confirm Database Backup',
        message: 'Are you sure you want to create a database backup? This will backup the current database state and upload it to Google Drive.',
        color: 'primary',
        confirmButtonText: 'Backup'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        // Show loading notification
        this.notificationService.showInfo('Database backup in progress. This may take a few minutes...');

        // Trigger the backup
        this.databaseService.triggerBackup().subscribe({
          next: () => {
            this.notificationService.showSuccess('Database backup completed successfully.');
          },
          error: (err) => {
            console.error('Error during database backup:', err);
            // The service now handles error transformation, so err.message should contain the user-friendly message
            const errorMessage = err.message || err.error?.message || 'Failed to backup database. Please try again later.';
            this.notificationService.showError(errorMessage);
          }
        });
      }
    });
  }

  /**
   * Import data from a backup file
   * Only available to users with ADMIN_OPS_MANAGER role
   */
  importFromBackup() {
    // Check if user has ADMIN_OPS_MANAGER role
    if (!this.hasAdminOpsManagerRole()) {
      this.notificationService.showNotification({
        type: 'error',
        message: 'Only Admin Ops Manager can import data from backups.'
      });
      return;
    }

    // Show confirmation dialog
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Confirm Database Import',
        message: 'Are you sure you want to import data from a backup? This will replace the current database with data from the most recent backup.',
        color: 'warn',
        confirmButtonText: 'Import'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        // Show loading notification
        this.notificationService.showInfo('Database import in progress. This may take a few minutes...');

        // Trigger the import
        this.databaseService.importFromBackup().subscribe({
          next: () => {
            this.notificationService.showSuccess('Database import completed successfully.');
            // Refresh the data after import
            this.getUserData();
          },
          error: (err) => {
            console.error('Error during database import:', err);
            // The service now handles error transformation, so err.message should contain the user-friendly message
            const errorMessage = err.message || err.error?.message || 'Failed to import database. Please try again later.';
            this.notificationService.showError(errorMessage);
          }
        });
      }
    });
  }

  /**
   * Export users with their edit logs as CSV
   */
  exportUsersWithLogs(): void {
    this.userService.exportUsersWithLogs().subscribe({
      next: (response) => {
        saveAs(response.data, 'users_with_logs.csv');
        this.notificationService.showNotification({
          type: 'success',
          message: response.message
        });
      },
      error: (err) => {
        console.error('Error exporting users with logs:', err);
        this.notificationService.showNotification({
          type: 'error',
          message: err.error?.message || 'Failed to export users with logs. Please try again later.'
        });
      }
    });
  }

  canDelegate(): boolean {
    const role = localStorage.getItem("role");
    return role === 'LEAD' || role === 'MANAGER' || role === 'ADMIN_OPS_MANAGER';
  }

  delegateRoleToUser(user: User) {
    const role = localStorage.getItem("role");
    const delegatorLdap = localStorage.getItem("username"); // username stores the LDAP

    if (!delegatorLdap) {
      this.notificationService.showError('Could not identify current user.');
      return;
    }

    this.dialog.open(DelegationDialogComponent, {
      width: '500px',
      data: {
        delegatorLdap: delegatorLdap,
        currentRole: role,
        delegateeLdap: user.ldap
      }
    });
  }



  paginate(event: any): void {
    const startIndex = event.pageIndex * event.pageSize;
    const endIndex = startIndex + event.pageSize;
    this.dataSource.data = this.dataSource.data.slice(startIndex, endIndex);
  }

  applyGlobalFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value.trim().toLowerCase();

    // Option 1: Keep global filter separate (simpler for now)
    // This will overwrite column filters. Needs refinement if both should work together.
    // For now, let's assume global filter clears column filters or vice-versa.
    // Or, modify the predicate to handle both.

    // Let's make global filter work independently for now.
    // We need a separate filter predicate or adjust the existing one.
    // Easiest approach: Reset column filters when global search is used.
    this.resetAllFilters(); // Reset column filters when global search starts

    // Use a simple global filter predicate when global search is active
    this.dataSource.filterPredicate = (data: User, filter: string): boolean => {
      const searchTerm = filter;
      const dataStr = Object.keys(data as any).reduce((currentTerm, key) => {
        // Combine relevant fields for global search
        return currentTerm + (data as any)[key] + '◬'; // Use a unique separator
      }, '').toLowerCase();
      return dataStr.indexOf(searchTerm) !== -1;
    };

    this.dataSource.filter = filterValue;

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }

    // !!! Important: Reset to column filter predicate if global filter is cleared
    if (!filterValue) {
      this.initializeTable(); // Re-initialize with the column filter predicate
    }
  }

  onFileUpload(event: Event): void {
    const target = event.target as HTMLInputElement;
    if (target.files && target.files.length > 0) {
      const file = target.files[0];
      const reader = new FileReader();
      reader.onload = (e: any) => {
        const data = e.target.result;
        const workbook = XLSX.read(data, { type: 'binary' });
        const sheetName = workbook.SheetNames[0];
        const sheet = workbook.Sheets[sheetName];
        const rawData = XLSX.utils.sheet_to_json(sheet);

        const columnMapping: { [key: string]: string } = {
          "First Name": "firstName",
          "Last Name": "lastName",
          "Ldap": "ldap",
          "Start Date": "startDate",
          "Team": "team",
          "New Level effective": "newLevel",
          "Manager": "programManager",
          "Lead": "lead",
          "Vendor": "vendor",
          "Email Address": "email",
          "Status": "status",
          "LWD / ML Start Date": "lwdMlStartDate",
          "Process": "process",
          "Resignation Date": "resignationDate",
          "Role change effective date": "roleChangeEffectiveDate",
          "Level (Before Change)": "levelBeforeChange",
          "Level (After Change)": "levelAfterChange",
          "Last Billing Date": "lastBillingDate",
          "Backfill Ldap": "backfillLdap",
          "Billing Start Date": "billingStartDate",
          "Language Supported": "language",
          "Tenure Till Date": "tenureTillDate",
          "Shift": "shift",
          "Level": "level",
          "Comments": "inactiveReason",
          "PS&E Program": "pnseProgram",
          "Location": "location"
        };

        const jsonData: User[] = rawData.map((row: any) => {
          const mappedRow: any = {};
          for (const [csvColumn, variableName] of Object.entries(columnMapping)) {
            mappedRow[variableName] = row[csvColumn] || '';
          }
          return mappedRow as User;
        });

        this.dataSource.data = jsonData;
        this.userService.addCSV(this.dataSource.data).subscribe({
          next: (response) => {
            this.router.navigate(['/admin/dashboard']);
            this.notificationService.showNotification({
              type: 'success',
              message: response.message
            });
          },
          error: (error) => {
            this.notificationService.showNotification({
              type: 'error',
              message: error.error?.message || 'Failed to upload data. Please try again.'
            });
          }
        });
      };
      reader.readAsBinaryString(file);
    }
  }

  // isDuplicateLdap(ldap: string): boolean {
  //   const count = this.dataSource.data.filter(user => user.ldap === ldap).length;
  //   return count > 1;
  // }


  // onFileUpload(event: Event): void {
  //   const target = event.target as HTMLInputElement;
  //   if (target.files && target.files.length > 0) {
  //     const file = target.files[0];
  //     const reader = new FileReader();
  //     reader.onload = (e: any) => {
  //       const data = e.target.result;
  //       const workbook = XLSX.read(data, { type: 'binary' });
  //       const sheetName = workbook.SheetNames[0];
  //       const sheet = workbook.Sheets[sheetName];
  //       const rawData = XLSX.utils.sheet_to_json(sheet);

  //       const columnMapping: { [key: string]: string } = {
  //         "First Name": "firstName",
  //         "Last Name": "lastName",
  //         "Ldap": "ldap",
  //         "Start Date": "startDate",
  //         "Team": "team",
  //         "New Level": "newLevel",
  //         "Manager": "programManager",
  //         "Lead": "lead",
  //         "Vendor": "vendor",
  //         "Email Address": "email",
  //         "Status": "status",
  //         "LWD / ML Start Date": "lwdMlStartDate",
  //         "Process": "process",
  //      //   "Resignation Date": "resignationDate",
  //         "Role change effective date": "roleChangeEffectiveDate",
  //         "Level (Before Change)": "levelBeforeChange",
  //         "Level (After Change)": "levelAfterChange",
  //         "Last Billing Date": "lastBillingDate",
  //         "Backfill Ldap": "backfillLdap",
  //         "Billing Start Date": "billingStartDate",
  //         "Language": "language",
  //         "Tenure Till Date": "tenureTillDate",
  //  //       "Added in go/vfs-who (Main)": "addedInGoVfsWhoMain",
  //  //       "Added in go/vfs-who (Inactive)": "addedInGoVfsWhoInactive",
  //         "Level": "level",
  //         "Resignation / ML(Inactive Reason)" : "inactiveReason",
  //       };
  //       console.log("Column mapping ",columnMapping)
  //       const jsonData: User[] = rawData.map((row: any) => {
  //         const mappedRow: any = {};
  //         for (const [csvColumn, variableName] of Object.entries(columnMapping)) {
  //           mappedRow[variableName] = row[csvColumn] || '';
  //         }
  //         return mappedRow as User;
  //       });

  //       console.log('Uploaded Data:', jsonData);
  //       this.dataSource.data = jsonData;
  //       this.userService.addCSV(this.dataSource.data).subscribe(() => {
  //         this.router.navigate(['/admin/dashboard']);
  //         this.notificationService.showNotification({
  //                type: 'success',
  //                  message: 'Data Uploaded Successfully'
  //                });
  //       });
  //     };
  //     reader.readAsBinaryString(file);
  //   }
  // }



  downloadCSV(): void {
    const data = this.dataSource.data;
    const worksheet = XLSX.utils.json_to_sheet(data);
    const workbook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, worksheet, 'Users');

    //cons
    const excelBuffer: any = XLSX.write(workbook, { bookType: 'csv', type: 'array' });
    const blob = new Blob([excelBuffer], { type: 'text/csv;charset=utf-8;' });
    saveAs(blob, 'users.csv');
  }

  selection = new SelectionModel<any>(true, []);
  get selectedUsers() {
    return this.selection.selected;
  }

  isAllSelected() {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows;
  }

  isIndeterminate() {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected > 0 && numSelected < numRows;
  }

  // Select or deselect all rows
  masterToggle() {
    this.isAllSelected()
      ? this.selection.clear()
      : this.dataSource.data.forEach(row => this.selection.select(row));
  }

  // Toggle selection for a single row
  toggleSelection(user: any) {
    this.selection.toggle(user);
  }

  // Check if a user is selected
  isSelected(user: any) {
    return this.selection.isSelected(user);
  }

  // Delete selected users
  // deleteSelectedUsers() {
  //   const selectedUserIds = this.selection.selected.map(user => user.id);
  //   if (selectedUserIds.length === 0) {
  //     console.warn('No users selected for deletion.');
  //     this.notificationService.showNotification({
  //       type: 'warning',
  //       message: 'No users selected for deletion.'
  //     });
  //     return;
  //   }

  //   console.log('Attempting to delete users with IDs:', selectedUserIds);
  //   this.userService.deleteUsers(selectedUserIds).subscribe({
  //     next: () => {
  //       this.dataSource.data = this.dataSource.data.filter(user => !selectedUserIds.includes(user.id));
  //       this.selection.clear();
  //       this.notificationService.showNotification({
  //         type: 'success',
  //         message: 'Users deleted successfully.'
  //       });
  //     },
  //     error: (err: any) => {
  //       this.notificationService.showNotification({
  //         type: 'error',
  //         message: 'Failed to delete the users'
  //       }); },
  //   });
  // }




  //}



  // deleteSelectedUsers() {
  //   const selectedUserIds = this.selection.selected.map(user => user.id);

  //   if (selectedUserIds.length === 0) {
  //     console.warn('No users selected for deletion.');
  //     this.notificationService.showNotification({
  //       type: 'warning',selection
  //       message: 'No users selected for deletion.'
  //     });
  //     return;
  //   }

  //   if (!confirm('Are you sure you want to delete the selected users? This action cannot be undone.')) {
  //     return;
  //   }

  //   this.userService.deleteUsers(selectedUserIds).subscribe({
  //     next: () => {
  //       this.dataSource.data = this.dataSource.data.filter(user => !selectedUserIds.includes(user.id));
  //       this.selection.clear();
  //       this.notificationService.showNotification({
  //         type: 'success',
  //         message: 'Users deleted successfully.'
  //       });
  //     },
  //     error: (err: any) => {
  //       console.error('Error deleting users:', err);
  //       this.notificationService.showNotification({
  //         type: 'error',
  //         message: 'Failed to delete the users. Please try again later.'
  //       });
  //     },
  //   });
  // }



  deleteSelectedUsers() {
    const selectedUserIds = this.selection.selected
      .map(user => user.id)
      .filter(id => id !== undefined && id !== "" && id !== null);
    console.log("Selected user ids ", selectedUserIds);


    if (selectedUserIds.length === 0) {
      this.notificationService.showNotification({
        type: 'warning',
        message: 'No users selected for deletion.'
      });
      return;
    }

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Confirm Deletion',
        message: `Are you sure you want to delete the selected ${selectedUserIds.length} user(s)? This action cannot be undone.`
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.userService.deleteUsers(selectedUserIds).subscribe({
          next: (response) => {
            this.dataSource.data = this.dataSource.data.filter(user => !selectedUserIds.includes(user.id));
            this.selection.clear();
            this.notificationService.showNotification({
              type: 'success',
              message: response.message
            });
          },
          error: (err: any) => {
            if (err.status === 409) {
              const errorMessage = err.error?.message || 'A request has already been made for one or more of these users. Please contact Admin Ops Manager for details.';
              this.notificationService.showError(errorMessage);
            } else {
              console.error('Error deleting users:', err);
              const errorMessage = err.error?.message || 'Failed to delete the users. Please try again later.';
              this.notificationService.showError(errorMessage);
            }
          },
        });
      }
    });
  }

  selectedColumns: string[] = ['select', 'profilePic', 'firstName', 'ldap', 'team', 'programManager', 'lead', 'level', 'location', 'actions'];

  allColumns = [
    { key: 'select', displayName: 'Check box' },
    { key: 'firstName', displayName: 'Name' },
    { key: 'ldap', displayName: 'Ldap' },
    { key: 'team', displayName: 'Team' },
    { key: 'actions', displayName: 'Actions' },
    { key: 'level', displayName: 'Level' },
    { key: 'newLevel', displayName: 'New Level' },
    { key: 'lead', displayName: 'Lead' },
    { key: 'programManager', displayName: 'Program Manager' },
    { key: 'location', displayName: 'Location' },
    { key: 'vendor', displayName: 'Vendor' },
    { key: 'email', displayName: 'Email' },
    { key: 'status', displayName: 'Status' },
    { key: 'process', displayName: 'Process' },
    { key: 'startDate', displayName: 'Start Date' },
    { key: 'lwdMlStartDate', displayName: 'LWD/ML Start Date' },
    { key: 'resignationDate', displayName: 'Resignation Date' },
    { key: 'roleChangeEffectiveDate', displayName: 'Role Change Effective Date' },
    { key: 'lastBillingDate', displayName: 'Last Billing Date' },
    { key: 'backfillLdap', displayName: 'Backfill Ldap' },
    { key: 'billingStartDate', displayName: 'Billing Start Date' },
    { key: 'language', displayName: 'Language' },
    { key: 'tenureTillDate', displayName: 'Tenure Till Date' },
    { key: 'pnseProgram', displayName: 'PS&E Program' },
    { key: 'profilePic', displayName: 'Profile Pic' },
    { key: 'parent', displayName: 'Parent' },
    { key: 'shift', displayName: 'Shift' }
  ];

  displayedColumns: string[] = [...this.selectedColumns];
  filteredColumns = [...this.allColumns];
  dropdownOpen: boolean = false;
  searchText: string = '';

  toggleDropdown() {
    this.dropdownOpen = !this.dropdownOpen;
  }

  /**
   * Close dropdown when clicking outside of it
   */
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event): void {
    const target = event.target as HTMLElement;
    const dropdownElement = this.elementRef.nativeElement.querySelector('.dropdown');

    // Check if the click is outside the dropdown
    if (this.dropdownOpen && dropdownElement && !dropdownElement.contains(target)) {
      this.dropdownOpen = false;
    }
  }

  /**
   * Close dropdown when pressing Escape key
   */
  @HostListener('document:keydown.escape', ['$event'])
  onEscapeKey(event: KeyboardEvent): void {
    if (this.dropdownOpen) {
      this.dropdownOpen = false;
      event.preventDefault();
    }
  }

  /**
   * Close dropdown programmatically
   */
  closeDropdown(): void {
    this.dropdownOpen = false;
  }

  filterColumns() {
    const searchLower = this.searchText.toLowerCase();
    this.filteredColumns = this.allColumns.filter(col => col.displayName.toLowerCase().includes(searchLower));
  }

  toggleSelectAll() {
    const mandatoryColumns = new Set(['select', 'profilePic', 'firstName', 'ldap', 'programManager', 'lead', 'level', 'newLevel', 'tenureTillDate', 'team']);
    const actionColumn = 'actions';

    if (this.selectedColumns.length === this.allColumns.length) {
      this.selectedColumns = [...mandatoryColumns, actionColumn];
    } else {
      this.selectedColumns = [...new Set([...mandatoryColumns, ...this.allColumns.map(col => col.key)])];
    }

    this.updateDisplayedColumns();
  }

  updateDisplayedColumns(event?: Event, columnKey?: string) {
    const mandatoryColumns = new Set(['select', 'profilePic', 'firstName', 'ldap', 'programManager', 'lead', 'level', 'newLevel', 'tenureTillDate', 'team']);
    const actionColumn = 'actions';

    if (event && columnKey) {
      const isChecked = (event.target as HTMLInputElement).checked;
      if (isChecked) {
        this.selectedColumns = [...new Set([...this.selectedColumns, columnKey])];
      } else {
        this.selectedColumns = this.selectedColumns.filter(col => col !== columnKey);
      }
    }

    const updatedColumns = this.selectedColumns.filter(col => !mandatoryColumns.has(col) && col !== actionColumn);
    this.displayedColumns = [...mandatoryColumns, ...updatedColumns, actionColumn];

    // Ensure all columns in displayedColumns exist in the data
    this.displayedColumns = this.displayedColumns.filter(col =>
      this.allColumns.some(c => c.key === col) || col === 'select' || col === 'actions'
    );
  }

  applyFilter(event: Event, column: string) {
    const filterValue = (event.target as HTMLInputElement).value.trim().toLowerCase();

    // Update the filter predicate to only search in the specified column
    this.dataSource.filterPredicate = (data: any, filter: string) => {
      const columnValue = data[column];
      if (columnValue === null || columnValue === undefined) {
        return false;
      }

      // Handle special case for firstName column which includes lastName
      if (column === 'firstName') {
        const fullName = `${data.firstName} ${data.lastName}`.toLowerCase();
        return fullName.includes(filter);
      }

      return columnValue.toString().toLowerCase().includes(filter);
    };

    this.dataSource.filter = filterValue;

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  // --- New helper methods for template interaction with currentFilterMenuState ---

  openFilterMenu(columnKey: string, trigger: MatMenuTrigger): void {
    this.currentFilterMenuState.columnKey = columnKey;
    this.currentFilterMenuState.tempSelectedValues = this.filterValues[columnKey] ? [...this.filterValues[columnKey]] : [];
    this.currentFilterMenuState.searchText = ''; // Reset search text when opening
    // trigger.openMenu();
  }

  resetCurrentFilterMenuState(): void {
    this.currentFilterMenuState.columnKey = null;
    this.currentFilterMenuState.tempSelectedValues = [];
    this.currentFilterMenuState.searchText = ''; // Reset search text when closing
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
      if (checked) {
        // Get unique values for the *current* column and copy them
        this.currentFilterMenuState.tempSelectedValues = this.getUniqueColumnValues(this.currentFilterMenuState.columnKey).slice();
      } else {
        this.currentFilterMenuState.tempSelectedValues = [];
      }
    }
  }

  isAllTempSelected(): boolean {
    if (!this.currentFilterMenuState.columnKey) return false;
    const uniqueValues = this.getUniqueColumnValues(this.currentFilterMenuState.columnKey);
    return uniqueValues.length > 0 && this.currentFilterMenuState.tempSelectedValues.length === uniqueValues.length;
  }

  isSomeTempSelected(): boolean {
    if (!this.currentFilterMenuState.columnKey) return false;
    const uniqueValueCount = this.getUniqueColumnValues(this.currentFilterMenuState.columnKey).length;
    const tempSelectedCount = this.currentFilterMenuState.tempSelectedValues.length;
    return tempSelectedCount > 0 && tempSelectedCount < uniqueValueCount;
  }

  get currentColumnKeyForMenu(): string | null {
    return this.currentFilterMenuState.columnKey;
  }

  // New getter for filtered options based on search text
  get filteredMenuOptions(): string[] {
    if (!this.currentFilterMenuState.columnKey) {
      return [];
    }
    const uniqueValues = this.getUniqueColumnValues(this.currentFilterMenuState.columnKey);
    const searchTextLower = this.currentFilterMenuState.searchText.trim().toLowerCase();

    if (!searchTextLower) {
      return uniqueValues; // No search text, return all
    }

    return uniqueValues.filter(value =>
      value.toLowerCase().includes(searchTextLower)
    );
  }

  // --- End new helper methods ---

  // --- New method to toggle column filter visibility ---
  toggleColumnFilters(): void {
    this.showColumnFilters = !this.showColumnFilters;
  }
  // --- End new method ---

  /**
   * Upload employee relations CSV file
   */
  uploadEmployeeRelationCsv(): void {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.csv';

    input.onchange = (event) => {
      const file = (event.target as HTMLInputElement).files?.[0];
      if (file) {
        const formData = new FormData();
        formData.append('file', file);

        // Use the database service for making HTTP requests
        this.databaseService.uploadEmployeeRelationCsv(formData).subscribe({
          next: (response: string) => {
            this.notificationService.showNotification({
              type: 'success',
              message: response || 'CSV uploaded successfully'
            });
          },
          error: (err: any) => {
            console.error('Error uploading CSV:', err);
            const errorMessage = err.error?.message || 'Failed to upload CSV. Please try again later.';
            this.notificationService.showError(errorMessage);
          }
        });
      }
    };

    input.click();
  }
}

