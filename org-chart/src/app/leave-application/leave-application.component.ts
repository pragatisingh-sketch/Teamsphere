import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { NotificationService } from 'src/app/shared/notification.service';
import { distinctUntilChanged } from 'rxjs/operators';
import { FormBuilder, FormGroup, Validators, FormControl } from '@angular/forms';
import { LeaveService } from '../services/leave.service';
import { formatDate } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { ChangeDetectorRef } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AttendanceComponent } from './attendance/attendance.component';
import { NewRequestComponent } from '../leave-application/components/new-request/new-request.component';
import { HistoryComponent } from '../leave-application/components/history/history.component';
import { EmployeeService } from '../employee.service';
import { HttpClient } from '@angular/common/http';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { environment } from 'src/environments/environment';
import { ConfirmationDialogComponent } from 'src/app/confirm-dialog/confirmation-dialog.component';

interface BaseResponse<T> {
  status: string;
  code: number;
  message: string;
  data: T;
}


interface SelectOption {
  value: string;
  label: string;
}

export interface VunnoMgmtDto {
  ldap: string;
  name: string;
  role: string;
  email: string;
  programAlignment: string;
  team: string;
  lead: string;
}

interface LeaveRecord {
  status: string;  // Changed from union type to string
  applicationType: string,
  type: string;
  startDate: Date;
  endDate: Date;
  duration: string;
  approver: string;
}

interface Holiday {
  id: number;
  holidayDate: string;
  holidayName: string;
  description?: string;
  holidayType: string;
  isActive: boolean;
  createdAt: string;
  updatedAt?: string;
  uploadedBy?: string;
}

@Component({
  selector: 'app-leave-application',
  templateUrl: './leave-application.component.html',
  styleUrls: [
    './leave-application.component.css',
    './leave-sections.css',
    './bulk-heading.css'
  ]
})

export class LeaveApplicationComponent implements OnInit {

  activeTab: string = 'attendance'; // Default active tab
  ldapProvided: boolean = false;
  loading = false;
  loadingMessage = "";
  submissionMessage: string = '';
  showSuccess: boolean = false;
  leaveForm!: FormGroup;
  submitted = false;
  leaveBalance = { sick: 0, casual: 0, earned: 0, total: 0, totalwfh: 0.0, qtrwfh: 0.0, };
  leaveHistory: LeaveRecord[] = [];
  userRole: string | undefined;
  status = "PENDING";
  selectedFile: File | null = null;
  isDragOver: boolean = false;

  // Cached available leave types to prevent infinite change detection
  availableLeaveTypes: { value: string; label: string; disabled: boolean }[] = [];

  // Flag to prevent infinite loop when updating form fields
  private isUpdatingFormFields = false;

  // Flag to prevent double submission
  isSubmitting = false;

  // Holiday management properties
  baseUrl = environment.apiUrl;
  holidayDataSource = new MatTableDataSource<Holiday>();
  holidayDisplayedColumns: string[] = ['holidayDate', 'holidayName', 'description', 'holidayType', 'uploadedBy', 'actions'];
  selectedHolidayFile: File | null = null;
  isUploadingHolidays = false;
  canUploadHolidays = false;


  showApproverDropdown = false;
  approverOptions: string[] = [];

  // WFH disclaimer properties
  showWFHDisclaimer = false;
  wfhDisclaimerMessage = '';

  // Leave balance validation properties
  showLeaveBalanceWarning = false;
  leaveBalanceWarningMessage = '';
  hasSufficientBalance = true;
  pendingLeaveRequests: any[] = [];

  // Date overlap validation properties
  showDateOverlapWarning = false;
  dateOverlapWarningMessage = '';
  overlappingRequests: any[] = [];
  hasOverlappingRequests = false; // Flag to disable submit button

  // End date control property
  isEndDateDisabled = false;

  // Time entry preview for auto-generated entries
  timeEntryPreview: any[] = [];

  // Day-by-day configuration for leave requests
  dayConfigurations: Array<{
    date: Date;
    dayName: string;
    leaveType: string;
    duration: number; // 0.5 for half day, 1 for full day
    durationLabel?: string; // 'AM Half', 'PM Half', 'Full Day'
    durationValue?: string; // 'AM_HALF', 'PM_HALF', 'FULL'
    balance: number; // Running balance after this day
    isHoliday?: boolean; // True if this is a Google holiday
    holidayName?: string; // Name of the holiday if applicable
  }> = [];
  showDayConfiguration = false;
  // View toggle and bulk mode properties
  configView: 'bulk' | 'compact' = 'bulk';
  bulkLeaveType: string = '';
  firstDayHalf: boolean = false;
  lastDayHalf: boolean = false;
  // Half-day selection (shift-agnostic: First Half or Second Half)
  firstDayHalfType: 'FIRST' | 'SECOND' = 'FIRST';  // Default to First Half
  lastDayHalfType: 'FIRST' | 'SECOND' = 'SECOND';  // Default to Second Half
  // WFH/CompOff half-day tracking (separate from Leave)
  isFirstDayHalf: boolean = false;
  isLastDayHalf: boolean = false;
  // WFH/CompOff half-day types (matching Leave implementation)
  firstDayHalfTypeWFH: 'FIRST' | 'SECOND' = 'FIRST';  // Default to First Half
  lastDayHalfTypeWFH: 'FIRST' | 'SECOND' = 'SECOND';  // Default to Second Half

  // Holidays list for checking Google holidays
  holidays: Holiday[] = [];

  // Leave balance validation
  hasBalanceErrors: boolean = false;
  balanceErrorMessages: string[] = [];
  requestedDaysByLeaveType: Map<string, number> = new Map();



  @ViewChild('attendanceComp') attendanceComponent?: AttendanceComponent;
  @ViewChild(NewRequestComponent) newRequestComponent?: NewRequestComponent;
  @ViewChild(HistoryComponent) historyComponent?: HistoryComponent;
  @ViewChild(MatPaginator) holidayPaginator!: MatPaginator;
  @ViewChild(MatSort) holidaySort!: MatSort;
  @ViewChild('holidayFileInput') holidayFileInput!: ElementRef;

  constructor(private fb: FormBuilder, private leaveService: LeaveService, private dialog: MatDialog, private cdRef: ChangeDetectorRef, private snackbar: MatSnackBar,
    private notificationService: NotificationService, private employeeService: EmployeeService, private http: HttpClient
  ) { }

  private markFormGroupTouched(formGroup: FormGroup | FormControl): void {
    if (formGroup instanceof FormControl) {
      formGroup.markAsTouched();
      return;
    }

    Object.values(formGroup.controls).forEach(control => {
      if (control instanceof FormGroup || control instanceof FormControl) {
        this.markFormGroupTouched(control);
      }
    });
  }

  applyHolidayFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.holidayDataSource.filter = filterValue.trim().toLowerCase();

    if (this.holidayDataSource.paginator) {
      this.holidayDataSource.paginator.firstPage();
    }
  }

  onHolidayFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file && file.type === 'text/csv') {
      this.selectedHolidayFile = file;
    } else {
      this.notificationService.showNotification({
        type: 'error',
        message: 'Please select a valid CSV file'
      });
      this.selectedHolidayFile = null;
    }
  }

  onWFHFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      this.leaveForm.patchValue({
        document: file.name
      });
    }
  }

  uploadHolidayCSV(): void {
    if (!this.selectedHolidayFile) {
      this.notificationService.showNotification({
        type: 'error',
        message: 'Please select a CSV file first'
      });
      return;
    }

    if (!this.isAdminOpsManager()) {
      this.notificationService.showNotification({
        type: 'error',
        message: 'You do not have permission to upload holidays'
      });
      return;
    }

    this.isUploadingHolidays = true;
    const formData = new FormData();
    formData.append('file', this.selectedHolidayFile);

    this.http.post<BaseResponse<Holiday[]>>(`${this.baseUrl}/api/holidays/upload-csv`, formData)
      .subscribe({
        next: (response) => {
          this.isUploadingHolidays = false;
          if (response.status === 'success') {
            this.notificationService.showNotification({
              type: 'success',
              message: response.message || 'Holidays uploaded successfully!'
            });
            this.loadHolidays();
            this.selectedHolidayFile = null;
            // Reset file input
            if (this.holidayFileInput) {
              this.holidayFileInput.nativeElement.value = '';
            }
          } else {
            this.notificationService.showNotification({
              type: 'error',
              message: response.message || 'Failed to upload holidays'
            });
          }
        },
        error: (error) => {
          this.isUploadingHolidays = false;
          console.error('Error uploading CSV:', error);
          let errorMessage = 'Failed to upload holidays. Please try again.';
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

  deleteHoliday(holiday: Holiday): void {
    if (!this.isAdminOpsManager()) {
      this.notificationService.showNotification({
        type: 'error',
        message: 'You do not have permission to delete holidays'
      });
      return;
    }

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete Holiday',
        message: `Are you sure you want to delete the holiday "${holiday.holidayName}"?`,
        confirmButtonText: 'Delete',
        color: 'warn'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result === true) {
        this.http.delete<BaseResponse<string>>(`${this.baseUrl}/api/holidays/${holiday.id}`)
          .subscribe({
            next: (response) => {
              if (response.status === 'success') {
                this.notificationService.showNotification({
                  type: 'success',
                  message: 'Holiday deleted successfully!'
                });
                this.loadHolidays();
              } else {
                this.notificationService.showNotification({
                  type: 'error',
                  message: response.message || 'Failed to delete holiday'
                });
              }
            },
            error: (error) => {
              console.error('Error deleting holiday:', error);
              this.notificationService.showNotification({
                type: 'error',
                message: 'Failed to delete holiday. Please try again.'
              });
            }
          });
      }
    });
  }

  downloadSampleHolidayCSV(): void {
    const csvContent = `Date,Holiday,Description
1/1/2025,New Year's Day,New Year celebration
8/15/2025,Independence Day,Indian Independence Day
12/25/2025,Christmas Day,Christmas celebration`;

    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'holidays_sample.csv';
    a.click();
    window.URL.revokeObjectURL(url);
  }

  isRoleAllowed(): boolean {
    const allowedRoles = ['LEAD', 'MANAGER', 'ADMIN_OPS_MANAGER'];
    return this.userRole !== undefined && allowedRoles.includes(this.userRole);
  }

  // Update your setActiveTab method
  setActiveTab(tab: string): void {
    this.activeTab = tab;

    // Load data for specific tabs
    if (tab === 'history') {
      // this.fetchLeaveHistory();
    }
    else if (tab === 'balance') { }
    else if (tab === 'attendance') {
      setTimeout(() => {
        this.attendanceComponent?.refreshAttendance();
      }, 0);
    }
    else if (tab === 'holidays') {
      setTimeout(() => {
        this.loadHolidays();
      }, 0);
    }
  }

  // Holiday Management Methods
  isAdminOpsManager(): boolean {
    return this.userRole === 'ADMIN_OPS_MANAGER';
  }

  loadHolidays(): void {
    this.http.get<BaseResponse<Holiday[]>>(`${this.baseUrl}/api/holidays`)
      .subscribe({
        next: (response) => {
          if (response.status === 'success') {
            this.holidayDataSource.data = response.data;
            this.holidays = response.data; // Store for day configuration checks
          } else {
            this.notificationService.showNotification({
              type: 'error',
              message: response.message || 'Failed to load holidays'
            });
          }
        },
        error: (error) => {
          console.error('Error loading holidays:', error);
          this.notificationService.showNotification({
            type: 'error',
            message: 'Failed to load holidays. Please try again.'
          });
        }
      });
  }

  // Options for select fields
  requestTypeOptions: SelectOption[] = [
    { value: 'Leave', label: 'Leave' },
    { value: 'Work From Home', label: 'Work From Home' }
  ];

  leaveTypeOptions: SelectOption[] = [];

  durationTypeOptions: SelectOption[] = [
    { value: 'Full Day', label: 'Full Day' },
    { value: 'First Half', label: 'First Half' },
    { value: 'Second Half', label: 'Second Half' },
    { value: 'Multiple Days', label: 'Multiple Days' },
  ];

  ngOnInit(): void {
    this.userRole = localStorage.getItem('role') || undefined;
    this.canUploadHolidays = this.isAdminOpsManager();
    this.initForm();

    this.leaveService.getCurrentUserLdap().subscribe({
      next: (response) => {
        const ldap = response.data.ldap;
        this.leaveForm.get('ldap')?.setValue(ldap);
        this.fetchManager();
        // Load pending requests for balance validation
        this.loadPendingRequests(ldap);
        // Fetch leave balance for the user
        this.fetchLeaveBalance(ldap);
      },
      error: (err) => console.error('Error fetching current user:', err)
    });

    this.setupFormSubscriptions();

    // Load holidays
    this.loadHolidays();

    // Set up holiday table filter
    this.holidayDataSource.filterPredicate = (data: Holiday, filter: string) => {
      return Object.values(data)
        .some(value => value?.toString().toLowerCase().includes(filter.toLowerCase()));
    };
  }


  ngAfterViewInit(): void {
    if (this.holidayPaginator && this.holidaySort) {
      this.holidayDataSource.paginator = this.holidayPaginator;
      this.holidayDataSource.sort = this.holidaySort;
    }
  }

  formatDate(date: Date): string {
    const options: Intl.DateTimeFormatOptions = { year: 'numeric', month: 'short', day: 'numeric' };
    return date.toLocaleDateString('en-US', options);
  }

  get f() {
    return this.leaveForm.controls;
  }


  private setupFormSubscriptions(): void {
    // LDAP changes
    this.leaveForm.get('ldap')?.valueChanges.subscribe(ldap => {
      this.ldapProvided = !!ldap && ldap.trim() !== '';
      this.updateApproverFieldState(ldap);
    });

    // Duration type changes - use distinctUntilChanged to prevent multiple calls
    this.leaveForm.get('durationType')?.valueChanges.pipe(
      distinctUntilChanged()
    ).subscribe(value => {
      console.log("Duration type changed to:", value);
      this.updateTimes(value);
      this.updateEndDateRequirement(value);
    });

    // Request type changes
    this.leaveForm.get('requestType')?.valueChanges.pipe(
      distinctUntilChanged()
    ).subscribe(requestType => {
      this.updateLeaveTypes(requestType);
      this.updateUIBasedOnRequestType(requestType);
      // Regenerate day configurations because visibility depends on requestType
      this.generateDayConfigurations();
      // Dynamically set validator for reason
      const reasonControl = this.leaveForm.get('reason');
      if (requestType === 'Work From Home') {
        reasonControl?.setValidators([Validators.required]);
      } else {
        reasonControl?.clearValidators();
      }
      reasonControl?.updateValueAndValidity();

      // Check for WFH disclaimer
      this.checkWFHDisclaimer();
    });

    // Start date changes
    this.leaveForm.get('startDate')?.valueChanges.pipe(
      distinctUntilChanged()
    ).subscribe(startDate => {
      const durationTypeControl = this.leaveForm.get('durationType');
      const endDate = this.leaveForm.get('endDate')?.value;

      // Auto-switch to Multiple Days if date range > 1 day
      if (startDate && endDate) {
        const start = new Date(startDate);
        const end = new Date(endDate);
        if (start < end) {
          durationTypeControl?.setValue('Multiple Days');
        }
      }

      const durationType = durationTypeControl?.value;
      if (startDate && durationType) {
        // Update end date requirement based on current duration type
        this.updateEndDateRequirement(durationType);
      }
      // Check for WFH disclaimer when dates change
      this.checkWFHDisclaimer();
      // Check leave balance when dates change
      this.checkLeaveBalance();
      // Check for date overlap with pending requests
      this.checkDateOverlap();
      // Generate day-by-day configuration
      this.generateDayConfigurations();
    });

    // End date changes
    this.leaveForm.get('endDate')?.valueChanges.pipe(
      distinctUntilChanged()
    ).subscribe(endDate => {
      const startDate = this.leaveForm.get('startDate')?.value;

      // Auto-switch to Multiple Days if date range > 1 day
      if (startDate && endDate) {
        const start = new Date(startDate);
        const end = new Date(endDate);
        if (start < end) {
          this.leaveForm.get('durationType')?.setValue('Multiple Days');
        }
      }

      // Check for WFH disclaimer when dates change
      this.checkWFHDisclaimer();
      // Check leave balance when dates change
      this.checkLeaveBalance();
      // Check for date overlap with pending requests
      this.checkDateOverlap();
      // Generate day-by-day configuration
      this.generateDayConfigurations();
    });

    // Leave type changes
    this.leaveForm.get('leaveType')?.valueChanges.pipe(
      distinctUntilChanged()
    ).subscribe(value => {
      if (value === 'Long Leave') {
        this.leaveForm.get('durationType')?.setValue('Multiple Days');
        this.leaveForm.get('durationType')?.disable();
        // For Long Leave, enable end date field and require it
        this.isEndDateDisabled = false;
        this.leaveForm.get('endDate')?.enable();
        this.leaveForm.get('endDate')?.setValidators([Validators.required]);
      } else {
        this.leaveForm.get('durationType')?.enable();
        this.leaveForm.get('endDate')?.clearValidators();
        // Reset end date disabled state when not Long Leave
        const durationType = this.leaveForm.get('durationType')?.value;
        if (durationType) {
          this.updateEndDateRequirement(durationType);
        }
      }
      this.leaveForm.get('endDate')?.updateValueAndValidity();
      // Check leave balance when leave type changes
      this.checkLeaveBalance();
      // Generate day configurations when leave type changes (for bulk view updates)
      this.generateDayConfigurations();
    });

    // Duration type changes - though usually handled by other methods, ensuring reactivity
    this.leaveForm.get('durationType')?.valueChanges.pipe(
      distinctUntilChanged()
    ).subscribe(value => {
      // Regenerate day configurations to reflect duration changes (like switching to Full Day/Half Day)
      this.generateDayConfigurations();
    });
  }


  private initForm(): void {
    this.leaveForm = this.fb.group({
      ldap: ['', Validators.required], // Make disabled
      approver: ['', Validators.required],
      requestType: ['', Validators.required],
      shift: ['S1', Validators.required], // Added for new UI
      leaveType: ['', Validators.required],
      durationType: ['', Validators.required],
      startDate: ['', Validators.required],
      endDate: [''],
      backupInfo: ['', Validators.required],
      reason: ['',],
      document: [null], // optional, handled in method
      oooProof: ['']
    }, { validators: this.dateRangeValidator });

    // Special handling for Long Leave
    this.leaveForm.get('leaveType')?.valueChanges.subscribe(value => {
      if (value === 'Long Leave') {
        this.leaveForm.get('durationType')?.setValue('Multiple Days');
        this.leaveForm.get('durationType')?.disable();
        // For Long Leave, enable end date field and require it
        this.isEndDateDisabled = false;
        this.leaveForm.get('endDate')?.enable();
        this.leaveForm.get('endDate')?.setValidators([Validators.required]);
      } else {
        this.leaveForm.get('durationType')?.enable();
        this.leaveForm.get('endDate')?.clearValidators();
        this.leaveForm.get('endDate')?.setValidators([Validators.required]);
      }
      this.leaveForm.get('endDate')?.updateValueAndValidity();
    });

    // Listen to shift changes to update time entry preview
    this.leaveForm.get('shift')?.valueChanges.pipe(
      distinctUntilChanged()
    ).subscribe(value => {
      console.log('Shift changed to:', value);
      // Regenerate time entry preview with new shift
      this.generateTimeEntryPreview();
    });

    this.setupFormSubscriptions();
  }

  dateRangeValidator(formGroup: FormGroup) {
    const startDate = formGroup.get('startDate')?.value;
    const endDate = formGroup.get('endDate')?.value;
    const durationType = formGroup.get('durationType')?.value;

    if (!startDate || !endDate) {
      return null;
    }

    const start = new Date(startDate);
    const end = new Date(endDate);

    // Start date cannot be greater than end date
    if (start > end) {
      return { dateRange: 'Start date cannot be after end date' };
    }

    // For Multiple Days duration, dates cannot be equal
    if (durationType === 'Multiple Days' && start.getTime() === end.getTime()) {
      return { dateRange: 'For Multiple Days duration, start and end dates cannot be the same' };
    }

    return null;
  }

  updateLeaveTypes(requestType: string): void {
    if (requestType === 'Work From Home') {
      this.leaveForm.get('leaveType')?.disable();
      this.leaveForm.get('leaveType')?.setValue('');
    }
    else {
      // Base leave types - always include LWP as an option
      this.leaveTypeOptions = [
        { value: 'Sick Leave', label: 'Sick Leave' },
        { value: 'Earned Leave', label: 'Earned Leave' },
        { value: 'Casual Leave', label: 'Casual Leave' },
        { value: 'LWP', label: 'LWP (Leave Without Pay)' }
      ];

      // Debug: Log current leave balances
      console.log('Current leave balances:', {
        casual: this.leaveBalance.casual,
        earned: this.leaveBalance.earned,
        sick: this.leaveBalance.sick
      });

      // Check if all leave balances are zero
      const allBalancesZero =
        (this.leaveBalance.casual || 0) === 0 &&
        (this.leaveBalance.earned || 0) === 0 &&
        (this.leaveBalance.sick || 0) === 0;

      console.log('All balances zero?', allBalancesZero);

      this.leaveForm.get('leaveType')?.enable();

      if (allBalancesZero) {
        // Auto-select LWP when all balances are zero
        this.leaveForm.get('leaveType')?.setValue('LWP');
        console.log('All leave balances are zero. Auto-selected LWP.');
      } else {
        // Clear selection for normal case
        this.leaveForm.get('leaveType')?.setValue('');
        console.log('User has leave balance. Not auto-selecting LWP.');
      }
    }
  }

  updateApproverFieldState(ldap: string): void {
    const approverControl = this.leaveForm.get('approver');

    if (!ldap || ldap.trim() === '') {
      approverControl?.disable();
      approverControl?.reset(); // Clear the value when disabled
    } else {
      approverControl?.enable();
    }
  }

  fetchManager(): void {
    const ldap = this.leaveForm.get('ldap')?.value;
    if (ldap) {
      this.leaveService.getManagerByLdap(ldap).subscribe({
        next: (managerDetails) => {
          if (managerDetails) {
            const user = managerDetails[0];


            this.leaveForm.get('approver')?.setValue(user.lead);
            this.approverOptions = [user.lead];

            this.leaveService.setManagerDetails(user);

          }
        },
        error: (err) => {
          console.error('Error fetching manager:', err);
          this.loading = false;
        }
      });
    }
  }

  /**
   * Fetch leave balance for the user
   */
  fetchLeaveBalance(ldap: string): void {
    this.leaveService.getLeaveDetails(ldap).subscribe({
      next: (data) => {
        // Data array format: [sick, casual, earned, total, totalwfh, qtrwfh]
        this.leaveBalance.sick = data[0] || 0;
        this.leaveBalance.casual = data[1] || 0;
        this.leaveBalance.earned = data[2] || 0;
        this.leaveBalance.total = data[3] || 0;
        this.leaveBalance.totalwfh = data[4] || 0;
        this.leaveBalance.qtrwfh = data[5] || 0;

        console.log('Leave balance loaded:', this.leaveBalance);

        // Update cached available leave types
        this.updateAvailableLeaveTypes();
      },
      error: (error) => {
        console.error('Error fetching leave balance:', error);
        // Set defaults to 0 if fetch fails
        this.leaveBalance = { sick: 0, casual: 0, earned: 0, total: 0, totalwfh: 0, qtrwfh: 0 };
        this.updateAvailableLeaveTypes();
      }
    });
  }

  updateUIBasedOnRequestType(requestType: string): void {
    const isWFH = requestType === 'Work From Home';
    const isCompOff = requestType === 'CompOff';
    const isLeave = requestType === 'Leave';

    // Toggle Leave Type
    const leaveTypeControl = this.leaveForm.get('leaveType');
    if (isWFH || isCompOff) {
      leaveTypeControl?.disable();
      leaveTypeControl?.setValue('');
    } else {
      leaveTypeControl?.enable();
      // Auto-select leave type when switching to Leave
      this.autoSelectLeaveType();
    }

    // Handle Duration Type - Only required for Leave
    const durationTypeControl = this.leaveForm.get('durationType');
    if (isWFH || isCompOff) {
      // For WFH and CompOff, make duration optional (configured via half-day checkboxes)
      durationTypeControl?.clearValidators();
    } else if (isLeave) {
      // For Leave, keep it required
      durationTypeControl?.setValidators([Validators.required]);

      // Removed default to 'Full Day' to allow natural date range selection
      // if (!durationTypeControl?.value) {
      //   durationTypeControl?.setValue('Full Day');
      // }
    }
    durationTypeControl?.updateValueAndValidity();

    // Handle oooProof field - Optional for ALL types (Leave, CompOff, WFH)
    const oooProofControl = this.leaveForm.get('oooProof');
    const backupInfoControl = this.leaveForm.get('backupInfo');

    if (isLeave || isCompOff) {
      // For Leave and CompOff: OOO optional, Backup required
      oooProofControl?.enable();
      oooProofControl?.clearValidators();
      backupInfoControl?.enable();
      backupInfoControl?.setValidators([Validators.required]);
    } else if (isWFH) {
      // For WFH: OOO optional, Backup disabled
      oooProofControl?.enable();
      oooProofControl?.clearValidators();
      backupInfoControl?.disable();
      backupInfoControl?.setValue('');
      backupInfoControl?.clearValidators();
    }
    oooProofControl?.updateValueAndValidity();
    backupInfoControl?.updateValueAndValidity();

    // Handle reason field (required for WFH)
    const reasonControl = this.leaveForm.get('reason');
    if (isWFH) {
      reasonControl?.enable();
      reasonControl?.setValidators([Validators.required]);
    } else {
      reasonControl?.disable();
      reasonControl?.setValue('');
      reasonControl?.clearValidators();
    }
    reasonControl?.updateValueAndValidity();

    // Handle document field - Disabled for all types (not used)
    const documentControl = this.leaveForm.get('document');
    documentControl?.disable();
    documentControl?.setValue('');
    documentControl?.clearValidators();
    documentControl?.updateValueAndValidity();
  }

  get isWFH(): boolean {
    return this.leaveForm.get('requestType')?.value === 'Work From Home';
  }

  onRequestTypeChange(): void {
    const requestType = this.leaveForm.get('requestType')?.value;

    if (requestType === 'Work From Home') {
      this.leaveForm.get('leaveType')?.setValue('');
      this.leaveForm.get('leaveType')?.disable(); // Disable Leave Type
    } else {
      this.leaveForm.get('leaveType')?.enable(); // Enable Leave Type if not WFH
    }
    this.updateUIBasedOnRequestType(requestType);

    // Regenerate day configurations when switching request types
    // This ensures Section 3 appears when switching to Leave with dates already selected
    this.generateDayConfigurations();
  }

  updateTimes(durationType: string): void {
    // Time entries will be auto-generated, no need to set times manually
    // This method is kept for backward compatibility but does nothing
  }

  updateEndDateRequirement(durationType: string): void {
    const endDateControl = this.leaveForm.get('endDate');
    const startDate = this.leaveForm.get('startDate')?.value;

    if (!startDate) return;

    if (durationType === 'Multiple Days' ||
      (this.leaveForm.get('leaveType')?.value === 'Long Leave' &&
        this.leaveForm.get('requestType')?.value === 'Leave')) {
      // For multiple days, enable end date field and require it
      this.isEndDateDisabled = false;
      endDateControl?.enable();
      endDateControl?.setValidators([Validators.required]);
    }
    else if (['Full Day', 'First Half', 'Second Half'].includes(durationType)) {
      // Keep end date enabled and optional for single day durations
      // User can select same date or different date for WFH/CompOff
      endDateControl?.setValue(startDate);
      endDateControl?.clearValidators();
      this.isEndDateDisabled = false;
      endDateControl?.enable();
    }
    else {
      // Default case - keep enabled
      this.isEndDateDisabled = false;
      endDateControl?.enable();
      endDateControl?.clearValidators();
    }

    endDateControl?.updateValueAndValidity();
  }

  onDurationTypeChange(): void {
    console.log("onDurationTypeChange function started");
    const durationType = this.leaveForm.get('durationType')?.value;
    this.updateTimes(durationType);
    this.updateEndDateRequirement(durationType);

    // Check for WFH disclaimer
    this.checkWFHDisclaimer();

    console.log("onDurationTypeChange function ended");
  }

  /**
   * Calculate the number of days for a WFH request
   */
  calculateWFHDays(): number {
    const durationType = this.leaveForm.get('durationType')?.value;
    const startDate = this.leaveForm.get('startDate')?.value;
    const endDate = this.leaveForm.get('endDate')?.value;

    if (!durationType) return 0;

    switch (durationType) {
      case 'Full Day':
        return 1;
      case 'First Half':
      case 'Second Half':
        return 0.5;
      case 'Multiple Days':
        if (startDate && endDate) {
          // Use business days calculation (excluding weekends)
          // This matches the backend WFH logic
          return this.countBusinessDays(new Date(startDate), new Date(endDate));
        }
        return 0;
      default:
        return 0;
    }
  }

  /**
   * Check if WFH disclaimer should be shown
   */
  checkWFHDisclaimer(): void {
    const requestType = this.leaveForm.get('requestType')?.value;

    if (requestType === 'Work From Home') {
      const wfhDays = this.calculateWFHDays();

      if (wfhDays >= 3) {
        this.showWFHDisclaimer = true;
        this.wfhDisclaimerMessage = `This WFH request is for ${wfhDays} days (>=3 days). It will be redirected to the Account Manager for approval.`;
      } else {
        this.showWFHDisclaimer = false;
        this.wfhDisclaimerMessage = '';
      }
    } else {
      this.showWFHDisclaimer = false;
      this.wfhDisclaimerMessage = '';
    }
  }

  /**
   * Load pending leave requests for the user
   */
  loadPendingRequests(ldap: string): void {
    this.leaveService.getUserLeaveHistory(ldap).subscribe({
      next: (data: any[]) => {
        // Filter only PENDING requests
        this.pendingLeaveRequests = data.filter(request => request.status === 'PENDING');
        console.log('Pending leave requests loaded:', this.pendingLeaveRequests);
        // Trigger balance check after loading pending requests
        this.checkLeaveBalance();
      },
      error: (error) => {
        console.error('Error loading pending requests:', error);
        this.pendingLeaveRequests = [];
      }
    });
  }

  /**
   * Calculate the number of days requested based on duration type and dates
   */
  calculateRequestedDays(): number {
    const durationType = this.leaveForm.get('durationType')?.value;
    const startDate = this.leaveForm.get('startDate')?.value;
    const endDate = this.leaveForm.get('endDate')?.value;

    if (!durationType) return 0;

    switch (durationType) {
      case 'Full Day':
        return 1;
      case 'First Half':
      case 'Second Half':
        return 0.5;
      case 'Multiple Days':
        if (startDate && endDate) {
          // Calculate business days (excluding weekends)
          // This matches the backend logic
          return this.countBusinessDays(new Date(startDate), new Date(endDate));
        }
        return 0;
      default:
        return 0;
    }
  }


  /**
   * Generate day-by-day configuration for leave request
   */
  private generateDayConfigurations(): void {
    const startDate = this.leaveForm.get('startDate')?.value;
    const endDate = this.leaveForm.get('endDate')?.value;
    const leaveType = this.leaveForm.get('leaveType')?.value;
    const requestType = this.leaveForm.get('requestType')?.value;

    console.log('=== generateDayConfigurations DEBUG ===');
    console.log('startDate:', startDate);
    console.log('endDate:', endDate);
    console.log('leaveType:', leaveType);
    console.log('requestType:', requestType);
    console.log('showDayConfiguration before:', this.showDayConfiguration);

    // Show configuration for Leave, WFH, and CompOff
    if (!requestType || (requestType !== 'Leave' && requestType !== 'Work From Home' && requestType !== 'CompOff')) {
      console.log('Not a valid request type for configuration');
      this.showDayConfiguration = false;
      this.dayConfigurations = [];

      // Clear stale validation errors 
      this.validateLeaveBalances();

      // Re-run overlap check (persists warning if dates still overlap)
      this.checkDateOverlap();

      return;
    }

    // For WFH/CompOff, just need dates to show half-day checkboxes
    if (requestType === 'Work From Home' || requestType === 'CompOff') {
      if (!startDate || !endDate) {
        console.log('Missing dates for WFH/CompOff');
        this.showDayConfiguration = false;
        return;
      }

      const start = new Date(startDate);
      const end = new Date(endDate);

      if (end < start) {
        console.log('End date before start date');
        this.showDayConfiguration = false;
        return;
      }

      // Show configuration section for WFH/CompOff
      this.showDayConfiguration = true;
      this.dayConfigurations = []; // No day-by-day table needed
      return;
    }

    // Only need dates - leave type will be chosen in Bulk/Compact view
    if (!startDate || !endDate) {
      console.log('Missing dates', { startDate, endDate });
      this.showDayConfiguration = false;
      this.dayConfigurations = [];
      return;
    }

    const start = new Date(startDate);
    const end = new Date(endDate);

    if (end < start) {
      console.log('End date before start date');
      this.showDayConfiguration = false;
      this.dayConfigurations = [];
      return;
    }


    // Generate configuration for each business day
    const configurations: any[] = [];
    let currentDate = new Date(start);

    // Use bulkLeaveType if set, otherwise use first available leave type
    const defaultLeaveType = this.bulkLeaveType ||
      this.availableLeaveTypes.find(type => !type.disabled)?.value ||
      'Casual Leave';

    while (currentDate <= end) {
      const dayOfWeek = currentDate.getDay();
      const isWeekend = dayOfWeek === 0 || dayOfWeek === 6;
      const holiday = this.isHoliday(currentDate);

      // Only include business days that are NOT Google holidays
      // Google holidays will still get time entries but won't appear in day configuration table
      if (!isWeekend && !holiday) {
        const dayNames = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

        let duration = 1.0;
        let durationLabel = 'Full Day';
        let durationValue = 'FULL';

        // Check if this is the first day and first day half is checked
        // Use normalized date comparison to be safe
        const isFirstDay = currentDate.getTime() === start.getTime();
        const isLastDay = currentDate.getTime() === end.getTime();

        if (this.configView === 'bulk') {
          if (isFirstDay && this.firstDayHalf) {
            duration = 0.5;
            durationValue = this.firstDayHalfType === 'FIRST' ? 'FIRST_HALF' : 'SECOND_HALF';
            durationLabel = this.firstDayHalfType === 'FIRST' ? 'First Half' : 'Second Half';
          }
          else if (isLastDay && this.lastDayHalf) {
            duration = 0.5;
            durationValue = this.lastDayHalfType === 'FIRST' ? 'FIRST_HALF' : 'SECOND_HALF';
            durationLabel = this.lastDayHalfType === 'FIRST' ? 'First Half' : 'Second Half';
          }
        }

        configurations.push({
          date: new Date(currentDate),
          dayName: dayNames[currentDate.getDay()],
          leaveType: defaultLeaveType,
          duration: duration,
          durationLabel: durationLabel,
          durationValue: durationValue,
          balance: 0, // Will be updated when leave type is selected
          isHoliday: false, // Never a holiday since we filter them out
          holidayName: null
        });
      }

      currentDate.setDate(currentDate.getDate() + 1);
    }

    console.log('Generated configurations:', configurations);
    this.dayConfigurations = configurations;
    this.showDayConfiguration = configurations.length > 0;
    console.log('showDayConfiguration after:', this.showDayConfiguration);
    console.log('=== Day configurations generated:', this.dayConfigurations.length, '===');

    // Validate leave balances after generating configurations
    this.validateLeaveBalances();

    // Check for overlapping requests
    this.checkDateOverlap();

    // Update form-level fields for validation
    this.updateFormLeaveTypeAndDuration();

    // Calculate initial balances
    if (this.dayConfigurations.length > 0) {
      this.recalculateAllBalances();
    }

    // Generate time entry preview
    this.generateTimeEntryPreview();
  }

  /**
   * Generate time entry preview showing what time entries will be created
   * Includes both regular working days and Google holidays
   * Fetches actual project from user's recent time entries via backend API
   */
  private generateTimeEntryPreview(): void {
    const startDate = this.leaveForm.get('startDate')?.value;
    const endDate = this.leaveForm.get('endDate')?.value;
    const requestType = this.leaveForm.get('requestType')?.value;
    const shift = this.leaveForm.get('shift')?.value || 'S1';

    // Only generate for Leave requests
    if (requestType !== 'Leave' || !startDate || !endDate) {
      this.timeEntryPreview = [];
      return;
    }

    // Fetch user's actual project from backend
    this.fetchUserProjectAndGeneratePreview(startDate, endDate, shift);
  }

  /**
   * Fetch user's recent project from time entries and generate preview
   */
  private fetchUserProjectAndGeneratePreview(startDate: Date, endDate: Date, shift: string): void {
    const ldap = this.leaveForm.get('ldap')?.value;
    if (!ldap) {
      this.buildTimeEntryPreview(startDate, endDate, shift, 'N/A');
      return;
    }

    // Call backend API to get user's recent project
    // The backend will check last 15 days of time entries, then project_mapping table
    this.http.get<any>(`${environment.apiUrl}/api/time-entries/recent-project/${ldap}`).subscribe({
      next: (response) => {
        const project = response.data?.projectName || response.projectName || 'N/A';
        console.log('Fetched recent project:', project);
        this.buildTimeEntryPreview(startDate, endDate, shift, project);
      },
      error: (error) => {
        console.warn('Could not fetch recent project, using N/A:', error);
        // Fallback to N/A if API fails
        this.buildTimeEntryPreview(startDate, endDate, shift, 'N/A');
      }
    });
  }

  /**
   * Public method to manually trigger preview regeneration
   * Called when day configurations change (leave type or duration changes)
   */
  public refreshTimeEntryPreview(): void {
    this.generateTimeEntryPreview();
  }

  /**
   * Build time entry preview with actual project information
   */
  private buildTimeEntryPreview(startDate: Date, endDate: Date, shift: string, project: string): void {
    const preview: any[] = [];
    const start = new Date(startDate);
    const end = new Date(endDate);
    let currentDate = new Date(start);

    // Generate preview for all business days including Google holidays
    while (currentDate <= end) {
      const dayOfWeek = currentDate.getDay();
      const isWeekend = dayOfWeek === 0 || dayOfWeek === 6;
      const holiday = this.isHoliday(currentDate);

      // Include all business days (even if holiday)
      if (!isWeekend) {
        // Get corresponding day configuration to determine leave type and duration
        const dayConfig = this.findDayConfiguration(currentDate);
        const leaveType = dayConfig?.leaveType || 'Casual Leave';
        const duration = dayConfig?.durationValue || 'FULL';

        // Calculate attendance type based on shift, leave type, and whether it's a holiday
        const attendanceType = this.calculateAttendanceType(shift, leaveType, duration, !!holiday);

        // Generate comment based on leave type and holiday
        let comment = '';
        if (holiday) {
          comment = `Google Holiday: ${holiday.holidayName}`;
        } else if (leaveType === 'LWP') {
          comment = 'Auto-Generated - LWP';
        } else {
          const leaveTypeShort = this.getLeaveTypeShortForm(leaveType);
          const durationText = duration === 'FULL' ? '1 day' : '0.5 day';
          comment = `Auto-generated: ${leaveTypeShort} (${durationText})`;
        }

        preview.push({
          date: new Date(currentDate),
          project: project,
          process: 'Absenteeism',
          activity: 'ABSENTEEISM',
          time: duration === 'FULL' ? '480' : '240',  // 480 mins (8 hours) for full day, 240 mins (4 hours) for half day
          attendance: attendanceType,
          comment: comment,
          status: 'PENDING'
        });
      }

      currentDate.setDate(currentDate.getDate() + 1);
    }

    this.timeEntryPreview = preview;
    console.log('Generated time entry preview:', this.timeEntryPreview);
  }

  /**
   * Calculate attendance type based on shift, leave type, duration, and holiday status
   * Examples:
   * - S1 + Sick Leave (Full) = S1/SL
   * - MS1 + Sick Leave (Full) = MS1/SL  
   * - S1 + Sick Leave (Half) = S1/SLH
   * - S1 + Casual/Earned (Full) = S1/Leave
   * - S1 + Casual/Earned (Half) = S1/H
   * - S1 + Google Holiday = S1/GO
   */
  private calculateAttendanceType(shift: string, leaveType: string, duration: string, isHoliday: boolean): string {
    if (isHoliday) {
      return `${shift}/GO`;
    }

    if (leaveType === 'LWP') {
      return 'LWP';
    }

    const isHalfDay = duration !== 'FULL';

    // Sick Leave
    if (leaveType === 'Sick Leave') {
      return isHalfDay ? `${shift}/SLH` : `${shift}/SL`;
    }

    // Casual or Earned Leave
    if (leaveType === 'Casual Leave' || leaveType === 'Earned Leave') {
      return isHalfDay ? `${shift}/H` : `${shift}/Leave`;
    }

    // Default fallback (LWP or other leave types)
    return `${shift}/Leave`;
  }

  /**
   * Get short form of leave type for comment
   */
  private getLeaveTypeShortForm(leaveType: string): string {
    const shortForms: { [key: string]: string } = {
      'Sick Leave': 'sick leave',
      'Casual Leave': 'casual',
      'Earned Leave': 'earned',
      'Work From Home': 'WFH'
    };
    return shortForms[leaveType] || leaveType.toLowerCase();
  }

  /**
   * Find day configuration for a specific date
   */
  private findDayConfiguration(date: Date): any {
    if (!this.dayConfigurations || this.dayConfigurations.length === 0) {
      return null;
    }

    return this.dayConfigurations.find(config => {
      const configDate = new Date(config.date);
      return configDate.getDate() === date.getDate() &&
        configDate.getMonth() === date.getMonth() &&
        configDate.getFullYear() === date.getFullYear();
    });
  }

  /**
   * Check if a date is a holiday
   */
  private isHoliday(date: Date): Holiday | null {
    const dateStr = this.formatDateForComparison(date);
    return this.holidays.find(h => this.formatDateForComparison(new Date(h.holidayDate)) === dateStr) || null;
  }

  /**
   * Format date for comparison (YYYY-MM-DD)
   */
  private formatDateForComparison(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  /**
   * Update duration for a specific day in configuration
   */
  updateDayDuration(index: number, value: string): void {
    if (index >= 0 && index < this.dayConfigurations.length) {
      // Parse duration value
      let duration = 1.0;
      let label = 'Full Day';

      if (value === 'FIRST_HALF') {
        duration = 0.5;
        label = 'First Half';
      } else if (value === 'SECOND_HALF') {
        duration = 0.5;
        label = 'Second Half';
      } else if (value === 'FULL') {
        duration = 1.0;
        label = 'Full Day';
      }

      this.dayConfigurations[index].duration = duration;
      this.dayConfigurations[index].durationLabel = label;
      this.dayConfigurations[index].durationValue = value;

      // Recalculate balances for all days
      this.recalculateAllBalances();

      // Refresh time entry preview when duration changes
      this.refreshTimeEntryPreview();
    }
  }

  /**
   * Update leave type for a specific day (Compact mode)
   */
  updateDayLeaveType(index: number, leaveType: string): void {
    if (this.dayConfigurations[index]) {
      this.dayConfigurations[index].leaveType = leaveType;

      // Recalculate balances for all days
      this.recalculateAllBalances();

      // Refresh time entry preview
      this.refreshTimeEntryPreview();

      // Validate leave balances
      this.validateLeaveBalances();
    }
  }

  /**
   * Recalculate balances for all days based on cumulative usage
   */
  recalculateAllBalances(): void {
    // Track cumulative usage for each leave type
    const cumulativeUsage: { [key: string]: number } = {
      'Casual Leave': 0,
      'Earned Leave': 0,
      'Sick Leave': 0
    };

    // Calculate balance for each day
    for (let i = 0; i < this.dayConfigurations.length; i++) {
      const day = this.dayConfigurations[i];
      const leaveType = day.leaveType;

      // Only add to cumulative usage if NOT a holiday
      // Holidays are company-provided and don't count against leave balance
      if (!day.isHoliday) {
        cumulativeUsage[leaveType] = (cumulativeUsage[leaveType] || 0) + day.duration;
      }

      // Calculate remaining balance
      const totalBalance = this.getAvailableBalance(leaveType);
      day.balance = totalBalance - cumulativeUsage[leaveType];
    }
  }

  /**
   * Check if leave balance is valid for submission
   */
  isLeaveBalanceValid(): boolean {
    if (!this.dayConfigurations || this.dayConfigurations.length === 0) {
      return true; // No validation needed if no days configured
    }

    // Check if any day has negative balance
    return !this.dayConfigurations.some(day => day.balance < 0);
  }

  /**
   * Handle bulk leave type change
   * Applies selected leave type to all days and refreshes preview
   */
  onBulkLeaveTypeChange(): void {
    // Apply the new leave type to all day configurations
    this.dayConfigurations = this.dayConfigurations.map(day => {
      if (!day.isHoliday) {  // Don't override Google holidays
        return {
          ...day,
          leaveType: this.bulkLeaveType
        };
      }
      return day;
    });

    // Recalculate balances
    this.recalculateAllBalances();

    // Refresh time entry preview
    this.refreshTimeEntryPreview();

    // Validate leave balances
    this.validateLeaveBalances();
  }

  /**
   * Update form-level leaveType and durationType based on day configurations
   * This is needed for form validation to pass
   */
  updateFormLeaveTypeAndDuration(): void {
    // Prevent infinite loop
    if (this.isUpdatingFormFields) {
      return;
    }

    if (this.dayConfigurations.length === 0) {
      return;
    }

    this.isUpdatingFormFields = true;

    // Determine leaveType for form
    let formLeaveType: string;

    if (this.configView === 'bulk') {
      // Bulk mode: use the bulk leave type
      formLeaveType = this.bulkLeaveType || 'Casual Leave';
    } else {
      // Compact mode: check if all days have the same leave type
      const leaveTypes = new Set(this.dayConfigurations.map(day => day.leaveType));

      if (leaveTypes.size === 1) {
        // All days have the same leave type
        formLeaveType = this.dayConfigurations[0].leaveType;
      } else {
        // Multiple leave types - use "Mixed"
        formLeaveType = 'Mixed';
      }
    }

    // Set form leaveType
    this.leaveForm.patchValue({ leaveType: formLeaveType }, { emitEvent: false });

    // Determine durationType based on total days and half-day settings
    let durationType: string;
    const totalDays = this.dayConfigurations.length;
    const hasHalfDay = this.dayConfigurations.some(day => day.duration === 0.5);

    if (totalDays === 1) {
      if (hasHalfDay) {
        const firstDay = this.dayConfigurations[0];
        durationType = firstDay.durationValue === 'FIRST_HALF' ? 'First Half' : 'Second Half';
      } else {
        durationType = 'Full Day';
      }
    } else {
      durationType = 'Multiple Days';
    }

    // Set form durationType
    this.leaveForm.patchValue({ durationType: durationType }, { emitEvent: false });

    this.isUpdatingFormFields = false;
  }

  /**
   * Handle first day half checkbox change
   * Updates first day duration and refreshes preview
   */
  onFirstDayHalfChange(): void {
    if (this.dayConfigurations.length > 0) {
      if (this.firstDayHalf) {
        // Use selected AM/PM type (not hardcoded)
        this.applyHalfDayDuration(0, this.firstDayHalfType);
      } else {
        // Set first day to full day
        this.dayConfigurations[0].duration = 1.0;
        this.dayConfigurations[0].durationValue = 'FULL';
        this.dayConfigurations[0].durationLabel = 'Full Day';
      }
      this.recalculateAndValidate();
    }
  }

  /**
   * Handle first day AM/PM type change
   * Updates duration when user switches between AM and PM
   */
  onFirstDayHalfTypeChange(): void {
    if (this.firstDayHalf && this.dayConfigurations.length > 0) {
      this.applyHalfDayDuration(0, this.firstDayHalfType);
      this.recalculateAndValidate();
    }
  }

  /**
   * Handle last day half checkbox change
   * Updates last day duration and refreshes preview
   */
  onLastDayHalfChange(): void {
    if (this.dayConfigurations.length > 0) {
      const lastIndex = this.dayConfigurations.length - 1;
      if (this.lastDayHalf) {
        // Use selected AM/PM type (not hardcoded)
        this.applyHalfDayDuration(lastIndex, this.lastDayHalfType);
      } else {
        // Set last day to full day
        this.dayConfigurations[lastIndex].duration = 1.0;
        this.dayConfigurations[lastIndex].durationValue = 'FULL';
        this.dayConfigurations[lastIndex].durationLabel = 'Full Day';
      }
      this.recalculateAndValidate();
    }
  }

  /**
   * Handle last day AM/PM type change
   * Updates duration when user switches between AM and PM
   */
  onLastDayHalfTypeChange(): void {
    if (this.lastDayHalf && this.dayConfigurations.length > 0) {
      const lastIndex = this.dayConfigurations.length - 1;
      this.applyHalfDayDuration(lastIndex, this.lastDayHalfType);
      this.recalculateAndValidate();
    }
  }

  /**
   * Helper method to apply half-day duration (First Half or Second Half)
   * Maps UI terminology to backend values: FIRST → AM_HALF, SECOND → PM_HALF
   */
  private applyHalfDayDuration(index: number, type: 'FIRST' | 'SECOND'): void {
    this.dayConfigurations[index].duration = 0.5;
    this.dayConfigurations[index].durationValue = type === 'FIRST' ? 'FIRST_HALF' : 'SECOND_HALF';
    this.dayConfigurations[index].durationLabel = type === 'FIRST' ? 'First Half' : 'Second Half';
  }

  /**
   * Helper method to recalculate and validate after changes
   * Centralizes the refresh steps
   */
  private recalculateAndValidate(): void {
    this.recalculateAllBalances();
    this.refreshTimeEntryPreview();
    this.validateLeaveBalances();
  }


  /**
   * Get available balance for a specific leave type
   */
  getAvailableBalance(leaveType: string): number {
    switch (leaveType) {
      case 'Casual Leave':
        return this.leaveBalance.casual || 0;
      case 'Earned Leave':
        return this.leaveBalance.earned || 0;
      case 'Sick Leave':
        return this.leaveBalance.sick || 0;
      case 'LWP':
        // LWP (Leave Without Pay) has unlimited balance
        return Number.MAX_SAFE_INTEGER;
      default:
        return 0;
    }
  }

  /**
   * Calculate how much balance is reserved by pending requests
   */
  getReservedBalance(leaveType: string): number {
    if (!this.pendingLeaveRequests || this.pendingLeaveRequests.length === 0) {
      return 0;
    }

    // Filter pending requests for the same leave type
    const relevantRequests = this.pendingLeaveRequests.filter(
      request => request.leaveType === leaveType
    );

    // Sum up the duration of all pending requests
    let totalReserved = 0;
    relevantRequests.forEach(request => {
      const duration = parseFloat(request.duration) || 0;
      totalReserved += duration;
    });

    return totalReserved;
  }

  /**
   * Check if user has sufficient leave balance
   */
  checkLeaveBalance(): void {
    const requestType = this.leaveForm.get('requestType')?.value;
    const leaveType = this.leaveForm.get('leaveType')?.value;

    // Only check for Leave requests, not WFH
    if (requestType !== 'Leave' || !leaveType) {
      this.showLeaveBalanceWarning = false;
      this.leaveBalanceWarningMessage = '';
      this.hasSufficientBalance = true;
      return;
    }

    // Skip balance check for Mixed type (handled by per-day validation)
    if (leaveType === 'Mixed') {
      this.hasSufficientBalance = true;
      this.showLeaveBalanceWarning = false;
      this.leaveBalanceWarningMessage = '';
      return;
    }

    const requestedDays = this.calculateRequestedDays();
    if (requestedDays === 0) {
      this.showLeaveBalanceWarning = false;
      this.leaveBalanceWarningMessage = '';
      this.hasSufficientBalance = true;
      return;
    }

    const availableBalance = this.getAvailableBalance(leaveType);
    const reservedBalance = this.getReservedBalance(leaveType);
    const actualAvailable = availableBalance - reservedBalance;

    console.log('Balance check:', {
      leaveType,
      requestedDays,
      availableBalance,
      reservedBalance,
      actualAvailable
    });

    // Check if sufficient balance
    if (actualAvailable < requestedDays) {
      this.hasSufficientBalance = false;
      this.showLeaveBalanceWarning = true;

      // Build warning message
      if (availableBalance === 0) {
        this.leaveBalanceWarningMessage = `You have 0 days of ${leaveType} available. Please select another leave type.`;
      } else if (reservedBalance > 0) {
        this.leaveBalanceWarningMessage = `You have requested ${requestedDays} ${requestedDays === 1 ? 'day' : 'days'} of ${leaveType}, but only ${actualAvailable} ${actualAvailable === 1 ? 'day' : 'days'} ${actualAvailable === 1 ? 'is' : 'are'} available.\n\nTotal balance: ${availableBalance} ${availableBalance === 1 ? 'day' : 'days'}\nReserved by pending requests: ${reservedBalance} ${reservedBalance === 1 ? 'day' : 'days'}\n\nPlease ask your lead to reject the pending request(s) or select another leave type.`;
      } else {
        this.leaveBalanceWarningMessage = `You have requested ${requestedDays} ${requestedDays === 1 ? 'day' : 'days'} of ${leaveType}, but only ${availableBalance} ${availableBalance === 1 ? 'day' : 'days'} ${availableBalance === 1 ? 'is' : 'are'} available.\n\nPlease select another leave type or reduce the number of days.`;
      }
    } else {
      this.hasSufficientBalance = true;
      this.showLeaveBalanceWarning = false;
      this.leaveBalanceWarningMessage = '';
    }
  }

  /**
   * Validate all leave balances for the current configuration
   * Updates hasBalanceErrors and balanceErrorMessages
   */
  validateLeaveBalances(): void {
    this.hasBalanceErrors = false;
    this.balanceErrorMessages = [];
    this.requestedDaysByLeaveType.clear();

    if (this.configView === 'bulk') {
      // Bulk mode: all days use the same leave type
      if (this.bulkLeaveType) {
        const requestedDays = this.calculateRequestedDaysByLeaveType(this.bulkLeaveType);
        const availableBalance = this.getAvailableBalance(this.bulkLeaveType);

        if (requestedDays > availableBalance) {
          this.hasBalanceErrors = true;
          this.balanceErrorMessages.push(
            `${this.bulkLeaveType}: Requested ${requestedDays} days but only ${availableBalance} available`
          );
        }
        this.requestedDaysByLeaveType.set(this.bulkLeaveType, requestedDays);
      }
    } else {
      // Compact mode: check each day individually
      const leaveTypeCounts = new Map<string, number>();

      this.dayConfigurations.forEach(day => {
        const leaveType = day.leaveType;
        const current = leaveTypeCounts.get(leaveType) || 0;
        leaveTypeCounts.set(leaveType, current + day.duration);
      });

      leaveTypeCounts.forEach((requestedDays, leaveType) => {
        const availableBalance = this.getAvailableBalance(leaveType);

        if (requestedDays > availableBalance) {
          this.hasBalanceErrors = true;
          this.balanceErrorMessages.push(
            `${leaveType}: Requested ${requestedDays} days but only ${availableBalance} available`
          );
        }
        this.requestedDaysByLeaveType.set(leaveType, requestedDays);
      });
    }
  }

  /**
   * Calculate requested days for a specific leave type
   */
  calculateRequestedDaysByLeaveType(leaveType: string): number {
    let totalDays = 0;

    this.dayConfigurations.forEach(day => {
      if (day.leaveType === leaveType) {
        totalDays += day.duration;
      }
    });

    return totalDays;
  }

  /**
   * Update cached available leave types with balance information
   * Call this whenever leave balance changes to update the cached list
   */
  updateAvailableLeaveTypes(): void {
    const leaveTypes = [
      { value: 'Casual Leave', balance: this.leaveBalance.casual || 0 },
      { value: 'Earned Leave', balance: this.leaveBalance.earned || 0 },
      { value: 'Sick Leave', balance: this.leaveBalance.sick || 0 },
      { value: 'LWP', balance: Number.MAX_SAFE_INTEGER } // LWP is always available
    ];

    this.availableLeaveTypes = leaveTypes.map(type => {
      const available = type.balance > 0;
      const label = type.balance === Number.MAX_SAFE_INTEGER
        ? type.value
        : `${type.value}    ${type.balance} left`;

      return {
        value: type.value,
        label: label,
        disabled: !available
      };
    });

    // Auto-select first available leave type
    this.autoSelectLeaveType();
  }

  /**
   * Auto-select a leave type that has balance available
   * Priority order: Casual Leave → Earned Leave → Sick Leave → LWP
   */
  autoSelectLeaveType(): void {
    const requestType = this.leaveForm.get('requestType')?.value;

    // Only auto-select for Leave requests, not WFH or CompOff
    if (requestType !== 'Leave') {
      return;
    }

    // Find first non-disabled leave type
    const firstAvailable = this.availableLeaveTypes.find(type => !type.disabled);

    if (!firstAvailable) {
      return; // Should never happen as LWP is always available
    }

    // For single leave requests (compact view - one leave type for all days)
    const leaveTypeControl = this.leaveForm.get('leaveType');
    if (this.configView === 'compact') {
      // Only auto-select if not already set
      if (!leaveTypeControl?.value) {
        leaveTypeControl?.setValue(firstAvailable.value);
      }
    }

    // For mixed leave requests (bulk config)
    if (this.configView === 'bulk') {
      // Only auto-select if not already set
      if (!this.bulkLeaveType) {
        this.bulkLeaveType = firstAvailable.value;
      }
    }
  }

  /**
   * Get available leave types - returns cached version
   * @deprecated Use availableLeaveTypes property directly in template
   */
  getAvailableLeaveTypes(): { value: string; label: string; disabled: boolean }[] {
    return this.availableLeaveTypes;
  }

  /**
   * Get warning badge data for bulk leave type
   * Returns null if no warning needed
   */
  getBulkLeaveTypeWarningBadge(): { requested: number, available: number } | null {
    if (!this.bulkLeaveType || this.configView !== 'bulk') {
      return null;
    }

    const requested = this.calculateRequestedDaysByLeaveType(this.bulkLeaveType);
    const available = this.getAvailableBalance(this.bulkLeaveType);

    if (requested > available) {
      return { requested, available };
    }

    return null;
  }

  /**
   * Check if new request dates overlap with any pending requests
   */
  checkDateOverlap(): void {
    const startDate = this.leaveForm.get('startDate')?.value;
    const endDate = this.leaveForm.get('endDate')?.value;

    // Reset overlap warning
    this.showDateOverlapWarning = false;
    this.dateOverlapWarningMessage = '';
    this.overlappingRequests = [];

    // Only check if we have both dates
    if (!startDate || !endDate) {
      return;
    }

    // No pending requests to check against
    if (!this.pendingLeaveRequests || this.pendingLeaveRequests.length === 0) {
      return;
    }

    const newStart = new Date(startDate);
    const newEnd = new Date(endDate);

    // Normalize times to midnight for date-only comparison
    newStart.setHours(0, 0, 0, 0);
    newEnd.setHours(0, 0, 0, 0);

    // Find all overlapping pending requests
    this.overlappingRequests = this.pendingLeaveRequests.filter(request => {
      const existingStart = new Date(request.startDate);
      const existingEnd = new Date(request.endDate);

      existingStart.setHours(0, 0, 0, 0);
      existingEnd.setHours(0, 0, 0, 0);

      // Check for overlap: requests overlap if they don't NOT overlap
      // No overlap if: new ends before existing starts OR new starts after existing ends
      const noOverlap = newEnd < existingStart || newStart > existingEnd;
      return !noOverlap; // Return true if there IS overlap
    });

    // Set flag for submit button validation
    this.hasOverlappingRequests = this.overlappingRequests.length > 0;

    // If overlaps found, build warning message
    if (this.overlappingRequests.length > 0) {
      this.showDateOverlapWarning = true;

      const formatDate = (date: Date) => {
        return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
      };

      if (this.overlappingRequests.length === 1) {
        const overlap = this.overlappingRequests[0];
        const existingStart = new Date(overlap.startDate);
        const existingEnd = new Date(overlap.endDate);
        existingStart.setHours(0, 0, 0, 0);
        existingEnd.setHours(0, 0, 0, 0);

        // Check for exact match
        if (newStart.getTime() === existingStart.getTime() && newEnd.getTime() === existingEnd.getTime()) {
          this.dateOverlapWarningMessage = `You already have a pending ${overlap.applicationType} request for these exact dates (${formatDate(existingStart)} to ${formatDate(existingEnd)}).\n\nTo apply for a new request, please contact your lead to reject or revoke the existing pending request first.`;
        } else {
          // Partial overlap or encapsulation
          this.dateOverlapWarningMessage = `Your requested dates overlap with an existing pending ${overlap.applicationType} request:\n\nPending Request: ${formatDate(existingStart)} to ${formatDate(existingEnd)}\nNew Request: ${formatDate(newStart)} to ${formatDate(newEnd)}\n\nPlease contact your lead to reject or revoke the pending request before submitting this new request.`;
        }
      } else {
        // Multiple overlaps
        const overlapDetails = this.overlappingRequests.map(req => {
          const start = new Date(req.startDate);
          const end = new Date(req.endDate);
          return `  • ${req.applicationType}: ${formatDate(start)} to ${formatDate(end)}`;
        }).join('\n');

        this.dateOverlapWarningMessage = `Your requested dates overlap with ${this.overlappingRequests.length} pending requests:\n\n${overlapDetails}\n\nPlease contact your lead to reject or revoke the conflicting pending requests before submitting this new request.`;
      }
    }
  }


  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.length) {
      this.selectedFile = input.files[0];
    }
  }

  removeFile(): void {
    this.selectedFile = null;
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;
  }

  onFileDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;

    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.selectedFile = files[0];
    }
  }

  formatDateForBackend(dateValue: any): string {
    if (!dateValue) {
      return '';
    }

    let date: Date;

    // If it's already a Date object
    if (dateValue instanceof Date) {
      date = dateValue;
    }
    // If it's a string, try to parse it
    else if (typeof dateValue === 'string') {
      // If it's already in YYYY-MM-DD format, return as is
      if (dateValue.match(/^\d{4}-\d{2}-\d{2}$/)) {
        return dateValue;
      }
      date = new Date(dateValue);
    }
    // If it's something else, try to convert to Date
    else {
      date = new Date(dateValue);
    }

    // Check if the date is valid
    if (isNaN(date.getTime())) {
      return '';
    }

    // Format as YYYY-MM-DD using local date components to avoid timezone issues
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }

  onSubmit(): void {
    this.submitted = true;

    // Prevent double submission
    if (this.isSubmitting) {
      return;
    }

    // Update form-level fields based on day configurations BEFORE validation
    this.updateFormLeaveTypeAndDuration();

    this.markFormGroupTouched(this.leaveForm);

    // Debug authentication
    const token = localStorage.getItem('authToken');
    const role = localStorage.getItem('role');
    const username = localStorage.getItem('username');

    console.log('Authentication Debug:', {
      hasToken: !!token,
      tokenLength: token?.length,
      role: role,
      username: username
    });

    if (!token) {
      this.notificationService.showNotification({
        type: 'error',
        message: 'You are not logged in. Please login first.'
      });
      return;
    }

    const requestType = this.leaveForm.get('requestType')?.value;
    const durationValue = this.leaveForm.get('durationType')?.value;
    console.log("App ", this.leaveForm.get('applicationType')?.value, " requestType ", requestType)

    this.updateUIBasedOnRequestType(requestType);
    this.updateEndDateRequirement(durationValue);
    this.leaveForm.updateValueAndValidity();

    Object.keys(this.leaveForm.controls).forEach(controlName => {
      const control = this.leaveForm.get(controlName);
      console.log(`${controlName}: valid=${control?.valid}, value=${control?.value}`);
    });


    if (this.leaveForm.invalid) {
      console.warn('Form is invalid. Details:', this.leaveForm.value, this.leaveForm.errors);
      return;
    }

    // If end date is undefined/empty, set it to start date
    const endDate = this.leaveForm.value.endDate;
    const startDate = this.leaveForm.value.startDate;
    const finalEndDate = (!endDate || endDate === '') ? startDate : endDate;
    console.log('Final end date:', finalEndDate);

    // Calculate correct duration type based on day configurations
    let calculatedDurationType = this.leaveForm.value.durationType;
    if (requestType === 'Leave' && this.dayConfigurations && this.dayConfigurations.length > 0) {
      // Check if any day has half duration
      const hasHalfDay = this.dayConfigurations.some(dc => dc.duration === 0.5);
      const hasFullDay = this.dayConfigurations.some(dc => dc.duration === 1.0);

      if (this.dayConfigurations.length > 1) {
        calculatedDurationType = 'Multiple Days';
      } else if (hasHalfDay && hasFullDay) {
        calculatedDurationType = 'Multiple Days'; // Mixed durations
      } else if (hasHalfDay && !hasFullDay) {
        // All half days - use the first one's type
        const firstHalfDay = this.dayConfigurations.find(dc => dc.duration === 0.5);
        if (firstHalfDay) {
          calculatedDurationType = firstHalfDay.durationValue === 'FIRST_HALF' ? 'First Half' : 'Second Half';
        }
      } else {
        calculatedDurationType = 'Full Day';
      }
    }

    const payload = {
      ldap: this.leaveForm.value.ldap,
      approvingLead: this.leaveForm.value.approver,
      applicationType: this.leaveForm.value.requestType,
      leaveType: this.leaveForm.value.leaveType || '',
      lvWfhDuration: calculatedDurationType,
      startDate: startDate ? formatDate(startDate, 'yyyy-MM-dd', 'en-US') : '',
      endDate: finalEndDate ? formatDate(finalEndDate, 'yyyy-MM-dd', 'en-US') : '',
      oooProof: this.getProofValue(this.leaveForm.value.oooProof),
      backupInfo: this.getProofValue(this.leaveForm.value.backupInfo),
      reason: this.leaveForm.value.reason || 'NA',
      status: this.status,
      role: this.userRole,
      dayConfigurations: this.dayConfigurations.map(dc => ({
        date: this.formatDateForBackend(dc.date),
        leaveType: dc.leaveType,
        duration: dc.durationLabel || 'Full Day', // String: "Full Day", "First Half", "Second Half"
        durationValue: dc.duration // Double: 1.0 or 0.5
      }))
    };

    const formData = new FormData();
    formData.append('leaveRequest', new Blob([JSON.stringify(payload)], { type: 'application/json' }));

    if (requestType === 'Work From Home' && this.selectedFile) {
      formData.append('document', this.selectedFile);
    }

    // Set submitting flag to true
    this.isSubmitting = true;

    this.leaveService.submitLeaveRequest(formData).subscribe({
      next: (response) => {
        if (response?.message?.includes('Requested leave successfully saved into database')) {
          this.notificationService.showNotification({ type: 'success', message: 'Request updated successfully.' });

          const currentLdap = this.leaveForm.get('ldap')?.value;
          if (currentLdap) {
            this.fetchLeaveBalance(currentLdap);
            this.loadPendingRequests(currentLdap);
          }

          this.resetForm();
        }
        else {
          this.notificationService.showNotification({ type: 'warning', message: 'Leave requested. Mail not sent — please contact your lead.' });
        }

        // Re-enable submit button on success
        this.isSubmitting = false;
      },
      error: (error) => {
        console.error('Submission error:', error);
        this.notificationService.showNotification({ type: 'error', message: 'Leave request not successful, Please try again!' });

        // Re-enable submit button on error
        this.isSubmitting = false;
      }
    });
  }

  triggerHolidayFileInput(): void {
    this.holidayFileInput.nativeElement.click();
  }

  private getProofValue(value: any): string {
    if (value === null || value === undefined || value === '') {
      return "NA,(WFH)";
    }
    return value; // Corrected from 'reserved' to 'value' based on typical usage
  }

  resetForm(): void {
    // Clear only the fields that should be reset
    this.leaveForm.patchValue({
      requestType: '',
      leaveType: '',
      durationType: '',
      startDate: '',
      endDate: '',
      backupInfo: '',
      oooProof: ''
    });

    // Clear specific fields that shouldn't keep values
    this.leaveForm.get('requestType')?.setValue('');
    this.leaveForm.get('leaveType')?.setValue('');
    this.leaveForm.get('durationType')?.setValue('');
    this.leaveForm.get('startDate')?.setValue('');
    this.leaveForm.get('endDate')?.setValue('');

    // Reset end date disabled state
    this.isEndDateDisabled = false;
    this.leaveForm.get('endDate')?.enable();

    // Reset WFH disclaimer
    this.showWFHDisclaimer = false;
    this.wfhDisclaimerMessage = '';

    // Reset leave balance warning
    this.showLeaveBalanceWarning = false;
    this.leaveBalanceWarningMessage = '';
    this.hasSufficientBalance = true;

    // Reset date overlap warning
    this.showDateOverlapWarning = false;
    this.dateOverlapWarningMessage = '';
    this.overlappingRequests = [];

    // Reset form state
    this.leaveForm.markAsPristine();
    this.leaveForm.markAsUntouched();
    this.submitted = false;
    this.loading = false;
    this.leaveTypeOptions = []
    this.submitted = false;
    this.cdRef.detectChanges();
  }

  /**
   * Calculate total days for the request (for new UI)
   */
  calculateTotalDays(): number {
    const requestType = this.leaveForm.get('requestType')?.value;

    // For WFH and CompOff, use the new calculation method
    if (requestType === 'Work From Home' || requestType === 'CompOff') {
      return this.calculateTotalDaysForWFHCompOff();
    }

    // For Leave, use day configurations
    if (this.dayConfigurations && this.dayConfigurations.length > 0) {
      // Only count non-holiday days
      return this.dayConfigurations
        .filter(day => !day.isHoliday)
        .reduce((sum, day) => sum + day.duration, 0);
    }
    return 0;
  }

  /**
   * Get count of holidays in the selected date range
   */
  getHolidayCount(): number {
    if (this.dayConfigurations && this.dayConfigurations.length > 0) {
      return this.dayConfigurations.filter(day => day.isHoliday).length;
    }
    return 0;
  }

  /**
   * Get total days requested for a specific leave type (for new UI)
   */
  getTotalByLeaveType(leaveType: string): number {
    if (!this.dayConfigurations || this.dayConfigurations.length === 0) {
      return 0;
    }
    return this.dayConfigurations
      .filter(day => day.leaveType === leaveType && !day.isHoliday) // Exclude holidays
      .reduce((sum, day) => sum + day.duration, 0);
  }

  /**
   * WFH/CompOff Configuration Helper Methods
   */
  getConfigurationTitle(): string {
    const requestType = this.leaveForm.get('requestType')?.value;
    if (requestType === 'Leave') {
      return 'Leave Configuration';
    } else if (requestType === 'Work From Home') {
      return 'WFH Configuration';
    } else if (requestType === 'CompOff') {
      return 'CompOff Configuration';
    }
    return 'Configuration';
  }

  calculateTotalDaysForWFHCompOff(): number {
    const startDate = this.leaveForm.get('startDate')?.value;
    const endDate = this.leaveForm.get('endDate')?.value;

    if (!startDate || !endDate) return 0;

    // Count business days between dates (excluding weekends and holidays)
    let totalDays = this.countBusinessDays(new Date(startDate), new Date(endDate));

    // Subtract half days
    if (this.isFirstDayHalf) totalDays -= 0.5;
    if (this.isLastDayHalf) totalDays -= 0.5;

    return Math.max(0, totalDays); // Ensure non-negative
  }

  onWFHCompOffHalfDayChange(): void {
    // Trigger recalculation when half-day checkboxes change
    this.calculateTotalDaysForWFHCompOff();
  }

  /**
   * Handle first day half day checkbox change for WFH/CompOff
   */
  onFirstDayHalfChangeWFH(): void {
    if (!this.isFirstDayHalf) {
      // Reset to default when unchecked
      this.firstDayHalfTypeWFH = 'FIRST';
    }
    this.calculateTotalDaysForWFHCompOff();
  }

  /**
   * Handle last day half day checkbox change for WFH/CompOff
   */
  onLastDayHalfChangeWFH(): void {
    if (!this.isLastDayHalf) {
      // Reset to default when unchecked
      this.lastDayHalfTypeWFH = 'SECOND';
    }
    this.calculateTotalDaysForWFHCompOff();
  }

  /**
   * Handle first day half type dropdown change for WFH/CompOff
   */
  onFirstDayHalfTypeChangeWFH(): void {
    this.calculateTotalDaysForWFHCompOff();
  }

  /**
   * Handle last day half type dropdown change for WFH/CompOff
   */
  onLastDayHalfTypeChangeWFH(): void {
    this.calculateTotalDaysForWFHCompOff();
  }

  isSingleDay(): boolean {
    const startDate = this.leaveForm.get('startDate')?.value;
    const endDate = this.leaveForm.get('endDate')?.value;

    if (!startDate || !endDate) return true;

    const start = new Date(startDate);
    const end = new Date(endDate);
    return start.getTime() === end.getTime();
  }

  private countBusinessDays(start: Date, end: Date): number {
    let count = 0;
    const current = new Date(start);

    while (current <= end) {
      const dayOfWeek = current.getDay();
      const isWeekend = (dayOfWeek === 0 || dayOfWeek === 6);
      const isHoliday = this.isGoogleHoliday(current);

      if (!isWeekend && !isHoliday) {
        count++;
      }

      current.setDate(current.getDate() + 1);
    }

    return count;
  }

  private isGoogleHoliday(date: Date): boolean {
    const dateStr = this.formatDateForHolidayCheck(date);
    return this.holidays.some(h => {
      const holidayDate = new Date(h.holidayDate);
      return this.formatDateForHolidayCheck(holidayDate) === dateStr;
    });
  }

  private formatDateForHolidayCheck(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
