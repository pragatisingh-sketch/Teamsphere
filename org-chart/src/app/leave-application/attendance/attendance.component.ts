import { Component, OnInit, ViewChild, OnDestroy, ChangeDetectorRef, AfterViewInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatMenuTrigger } from '@angular/material/menu';
import { LateCheckinDialogComponent } from './late-checkin-dialog.component';
import { CheckoutDialogComponent } from './checkout-dialog.component';
import { formatDate } from '@angular/common';
import { LeaveService } from 'src/app/services/leave.service';
import { NotificationService } from 'src/app/shared/notification.service';
import { AttendanceService } from 'src/app/services/attendance.service';
import { AttendanceRequest } from 'src/app/model/attendance-request.model';
import { filter, interval, Subject, take, takeUntil } from 'rxjs';
import { FormControl, FormGroup } from '@angular/forms';
import { saveAs } from 'file-saver';
import * as XLSX from 'xlsx';
import { CheckInStatusResponse } from 'src/app/model/check-in-status.model';
import { MatTableDataSource } from '@angular/material/table';
import { environment } from 'src/environments/environment';
import { HttpClient } from '@angular/common/http';
import * as moment from 'moment';
import { LocationCacheService } from 'src/app/services/location-cache.service';
import { DeviceDetectionService } from 'src/app/services/device-detection.service';

interface BaseResponse<T> {
  status: string;
  code: number;
  message: string;
  data: T;
}

interface RawAttendanceResponse {
  id: string;
  ldap: string;
  name: string;
  team: string;
  entryDate: string;
  entryTimestamp: string;
  exitDate?: string;
  exitTimestamp?: string;
  lateLoginReason: string;
  lateOrEarlyLogoutReason?: string;
  loginComment?: string;
  comment?: string;
  isOutsideOffice: boolean;
  isCheckOutOutsideOffice?: boolean;
  isDefaulter: boolean;
  lateOrEarlyCheckout?: string;
}

interface AttendanceRecord {
  id: string;
  ldap: string;
  name: string;
  team: string;
  date: string;
  checkInDate: string;
  checkOutDate?: string;
  checkIn: string;
  checkOut?: string;
  status: string;
  statusArray?: string[]; // Array of individual statuses parsed from lateOrEarlyCheckout
  reason?: string;
  checkoutReason?: string;
  comment?: string;
  isOutsideOffice: boolean;
  isCheckOutOutsideOffice?: boolean;
  isDefaulter: boolean;
  checkoutStatus?: string;
}
@Component({
  selector: 'app-attendance',
  templateUrl: './attendance.component.html',
  styleUrls: ['./attendance.component.css']
})
export class AttendanceComponent implements OnInit, OnDestroy, AfterViewInit {
  // Default columns - id is not available in UI
  displayedColumns: string[] = ['ldap', 'checkInDate', 'checkOutDate', 'checkIn', 'checkOut', 'status', 'isOutsideOffice', 'reason', 'checkoutReason', 'comment', 'isDefaulter', 'actions'];
  // All columns available for toggling (excluding 'actions' which is always visible, and 'id' which is hidden)
  allColumns: string[] = ['ldap', 'name', 'team', 'date', 'checkInDate', 'checkOutDate', 'checkIn', 'checkOut', 'status', 'reason', 'checkoutReason', 'comment', 'isOutsideOffice', 'isDefaulter'];
  showColumnFilters = false;
  baseUrl = environment.apiUrl;
  filterValues: any = {};
  private _attendanceRecords: AttendanceRecord[] = [];
  private destroy$ = new Subject<void>();
  checkInStatus: CheckInStatusResponse | null = null;

  dataSource = new MatTableDataSource<AttendanceRecord>([]);
  dateRange = new FormGroup({
    start: new FormControl<Date | null>(this.getLastWeekStartDate()),
    end: new FormControl<Date | null>(new Date())
  });

  showMyDirectReports = true;
  showMyAttendance = true;
  lastAppliedStartDate?: string;
  lastAppliedEndDate?: string;
  private readonly GRACE_MINUTES = 15;
  private readonly SHIFT_MISMATCH_HOURS = 2;
  // Column filtering properties
  columnUniqueValues: { [key: string]: string[] } = {};
  currentFilterMenuState = {
    columnKey: null as string | null,
    tempSelectedValues: [] as string[],
    searchText: ''
  };
  userRole: string | undefined;

  // Column toggle properties
  columnDisplayNames: { [key: string]: string } = {
    'id': 'ID',
    'ldap': 'LDAP',
    'name': 'Name',
    'team': 'Team',
    'date': 'Date',
    'checkInDate': 'Check-In Date',
    'checkOutDate': 'Check-Out Date',
    'checkIn': 'Check-In Time',
    'checkOut': 'Check-Out Time',
    'status': 'Status',
    'reason': 'Check-In Reason',
    'checkoutReason': 'Check-Out Reason',
    'comment': 'Additional Notes',
    'isOutsideOffice': 'Location',
    'isDefaulter': 'Compliance',
    'actions': 'Actions'
  };

  // Column toggle search
  columnSearchText: string = '';
  allColumnsSelected: boolean = false;
  showColumnToggle: boolean = false;
  userFullName: string | undefined;
  userTeam: string | undefined;
  isCheckingIn = false;
  isCheckingOut = false;
  hasCheckedIn = false;
  hasCheckedOut = false;
  status = '';
  checkInTime = '';
  checkOutTime = '';
  isLate = false;
  comment: string | undefined;

  // Resizable columns properties
  isResizing = false;
  currentResizingColumn: string | null = null;
  startX = 0;
  startWidth = 0;
  columnWidths: { [key: string]: number } = {};

  currentDate = formatDate(new Date(), 'EEEE, MMMM d, y', 'en-US');
  userInfo = {
    ldap: '',
    name: '',
    role: '',
    email: '',
    programAlignment: '',
    team: '',
    lead: '',
    manager: '',
    shift: ''
  };
  shiftDetails = {
    code: '',
    startTime: '',
    endTime: '',
    maxLoginTime: '',
  }
  latitude!: number;
  longitude!: number;
  checkedIn = false;
  checkedInTime = '';

  // Timer properties
  displayTimer: string = '00:00:00';
  private timerInterval: any;
  private checkInTimestamp: Date | null = null;
  private checkOutTimestamp: Date | null = null;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private http: HttpClient,
    public dialog: MatDialog,
    private leaveService: LeaveService,
    private notificationService: NotificationService,
    private attendanceService: AttendanceService,
    private cdr: ChangeDetectorRef,
    private locationCacheService: LocationCacheService,
    private deviceDetectionService: DeviceDetectionService,
    private cd: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.userRole = localStorage.getItem('role') || undefined;

    this.currentUser();

    // Set default view based on role
    if (this.userRole === 'ADMIN_OPS_MANAGER') {
      // ADMIN_OPS_MANAGER defaults to team view
      this.showMyAttendance = false;
      this.showMyDirectReports = true;
    } else if (this.userRole === 'ACCOUNT_MANAGER') {
      // ACCOUNT_MANAGER defaults to team view
      this.showMyAttendance = false;
      this.showMyDirectReports = true;
    } else if (this.userRole === 'USER') {
      // Regular users default to self view
      this.showMyAttendance = true;
      this.showMyDirectReports = false;
    } else if (this.userRole === 'LEAD' || this.userRole === 'MANAGER') {
      // LEADs and MANAGERs default to self view
      this.showMyAttendance = true;
      this.showMyDirectReports = false; // Start with pure self view
    } else {
      // Fallback for any other role (e.g., regular employee not explicitly 'USER')
      this.showMyAttendance = true;
      this.showMyDirectReports = false;
    }

    this.dataSource.filterPredicate = (data: AttendanceRecord, filter: string) => {
      try {
        const filterObject = JSON.parse(filter);
        const keys = Object.keys(filterObject);

        if (keys.length === 0) return true;

        return keys.every(key => {
          const filterValues = filterObject[key];
          if (!filterValues || (Array.isArray(filterValues) && filterValues.length === 0)) {
            return true;
          }

          if (Array.isArray(filterValues)) {
            const dataValue = String(data[key as keyof AttendanceRecord] || '').toLowerCase();
            return filterValues.some(value => dataValue.includes(value.toLowerCase()));
          }

          const dataValue = String(data[key as keyof AttendanceRecord] || '').toLowerCase();
          return dataValue.includes(filterValues.toLowerCase());
        });
      } catch (error) {
        console.error('Error parsing filter:', error);
        return true;
      }
    };
  }

  hasCheckedInToday(): boolean {
    // Check both the checkInStatus and local records
    if (this.checkInStatus?.checkedIn) {
      return true;
    }

    const today = new Date().toISOString().split('T')[0];
    return this._attendanceRecords.some(record =>
      record.date === today
    );
  }

  onTeamViewToggle(event: any): void {
    this.showMyDirectReports = event.checked;
    this.showMyAttendance = false
    console.log(`Toggle changed to: ${this.showMyDirectReports ? 'Direct Members' : 'Secondary Members'}`);

    this.loadAttendanceRecordsTemp();
  }

  getTodayCheckInTime(): string {
    // Check checkInStatus first for most up-to-date info
    if (this.checkInStatus?.checkedIn && this.checkInStatus.checkInTime) {
      return this.checkInStatus.checkInTime;
    }

    const today = new Date().toISOString().split('T')[0];
    const todayRecord = this._attendanceRecords.find(record =>
      record.date === today
    );
    return todayRecord ? todayRecord.checkIn : '--:--';
  }

  formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  // Helper method to parse status string into array
  // Example: "Late-Checkin, Early-Checkout" -> ["Late-Checkin", "Early-Checkout"]
  parseStatusString(statusString: string): string[] {
    if (!statusString || statusString === '-' || statusString.trim() === '') {
      return [];
    }

    // Split by comma and trim each part
    return statusString.split(',').map(s => s.trim()).filter(s => s.length > 0);
  }

  // Helper method to determine if a status is "on time" (green) or not (red)
  isOnTimeStatus(status: string): boolean {
    const upperStatus = status;
    return upperStatus.includes('OnTime') || upperStatus.includes('OnTime Checkout') || upperStatus.includes('OnTime CheckIn') || upperStatus.includes('Early-CheckIn');
  }

  // Check if checkout is on time
  isCheckoutOnTime(): boolean {
    if (!this.checkInStatus || !this.checkInStatus.earlyOrLateCheckOut) {
      return true; // Default to on-time if no status
    }
    const checkoutStatus = this.checkInStatus.earlyOrLateCheckOut;
    return checkoutStatus.includes('OnTime') || checkoutStatus.includes('OnTime Checkout') || checkoutStatus.includes('OnTime CheckIn');
  }

  // Start the timer with localStorage support
  startTimer(checkInTime: string): void {
    // Parse check-in time
    const today = new Date();
    const [hours, minutes] = checkInTime.split(':').map(Number);
    this.checkInTimestamp = new Date(today.getFullYear(), today.getMonth(), today.getDate(), hours, minutes, 0);

    // Clear any existing interval
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }

    // Set initial timer to 00:00:00
    this.displayTimer = '00:00:00';

    // Update timer every second
    this.timerInterval = setInterval(() => {
      this.updateTimer();
    }, 1000);
  }

  // Stop the timer
  stopTimer(checkOutTime: string): void {
    // Parse check-out time
    const today = new Date();
    const [hours, minutes] = checkOutTime.split(':').map(Number);
    this.checkOutTimestamp = new Date(today.getFullYear(), today.getMonth(), today.getDate(), hours, minutes, 0);

    // Clear interval
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }

    // Calculate final time
    this.updateTimer();
  }

  // Update timer display with localStorage fallback
  updateTimer(): void {
    if (!this.checkInStatus?.checkinDateTime) {
      this.displayTimer = '00:00:00';
      return;
    }

    const start = new Date(this.checkInStatus.checkinDateTime).getTime();
    const end = this.checkOutTimestamp
      ? new Date(this.checkOutTimestamp).getTime()
      : Date.now();

    const diff = end - start;

    if (diff < 0) {
      this.displayTimer = '00:00:00';
      return;
    }

    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((diff % (1000 * 60)) / 1000);

    this.displayTimer = `${this.padZero(hours)}:${this.padZero(minutes)}:${this.padZero(seconds)}`;
  }



  // Helper to pad numbers with zero
  padZero(num: number): string {
    return num.toString().padStart(2, '0');
  }

  isLeadOrManager(): boolean {
    return this.userRole === 'LEAD' || this.userRole === 'MANAGER';
  }
  isLead(): boolean {
    return this.userRole === 'LEAD';
  }

  canSeeToggle(): boolean {
    return (this.userRole === 'LEAD' || this.userRole === 'MANAGER')
      && !this.showMyAttendance;
  }

  loadAttendanceRecordsTemp() {
    const selectedStart = this.dateRange.get('start')?.value;
    const selectedEnd = this.dateRange.get('end')?.value;
    const today = new Date();

    let startDate: string;
    let endDate: string;

    if (selectedStart && selectedEnd) {
      // user has chosen range
      startDate = this.formatDate(selectedStart);
      endDate = this.formatDate(selectedEnd);
    } else if (this.userRole === 'LEAD' || this.userRole === 'MANAGER' || this.userRole === 'ADMIN_OPS_MANAGER') {
      // default → today
      startDate = this.formatDate(today);
      endDate = this.formatDate(today);
    } else {
      // default → last week
      endDate = this.formatDate(today);
      const pastWeek = new Date(today);
      pastWeek.setDate(today.getDate() - 6);
      startDate = this.formatDate(pastWeek);
    }

    const directOnly = this.showMyDirectReports;
    const self = this.showMyAttendance;

    const url = `${this.baseUrl}/api/atom?startDate=${startDate}&endDate=${endDate}&directOnly=${directOnly}&self=${self}`;

    this.http.get<BaseResponse<RawAttendanceResponse[]>>(url).subscribe({
      next: (response) => {
        const rawRecords = response.data;
        this._attendanceRecords = rawRecords.map((record) => {
          const attendanceRecord: AttendanceRecord = {
            id: record.id,
            ldap: record['ldap'] || '-',
            name: record['name'] || '-',
            team: record['team'] || '-',
            date: formatDate(record.entryDate, 'yyyy-MM-dd', 'en-US'),
            checkInDate: formatDate(record.entryDate, 'yyyy-MM-dd', 'en-US'),
            checkOutDate: record.exitDate ? formatDate(record.exitDate, 'yyyy-MM-dd', 'en-US') : '-',
            checkIn: formatDate(record.entryTimestamp, 'HH:mm', 'en-US'),
            checkOut: record.exitTimestamp ? formatDate(record.exitTimestamp, 'HH:mm', 'en-US') : '-',
            status: record.lateOrEarlyCheckout || '',
            statusArray: this.parseStatusString(record.lateOrEarlyCheckout || ''),
            reason: record.lateLoginReason || ' ',
            checkoutReason: record.lateOrEarlyLogoutReason || '-',
            comment: record.comment || 'NA',
            isOutsideOffice: record.isOutsideOffice,
            isCheckOutOutsideOffice: record.isCheckOutOutsideOffice,
            isDefaulter: record.isDefaulter, // Keep existing defaulter status from server
            checkoutStatus: record.lateOrEarlyCheckout || '-',
          };

          // DO NOT overwrite isDefaulter - it comes from the server and may have been manually modified
          // The server handles the compliance logic

          return attendanceRecord;
        });

        this._attendanceRecords.sort(
          (a, b) =>
            new Date(b.date + ' ' + b.checkIn).getTime() -
            new Date(a.date + ' ' + a.checkIn).getTime()
        );

        this.dataSource.data = this._attendanceRecords;
        this.collectUniqueColumnValues(this._attendanceRecords);

        // Load audit history status ONLY for supervisors (LEAD, MANAGER, ADMIN_OPS_MANAGER) viewing team records
        // This shows which records have been modified
        if (this.isLeadOrManager() && !this.showMyAttendance) {
          this.loadAuditHistoryStatus();
        }

        if (this.paginator) {
          this.paginator.firstPage();
        }
      },
      error: (err) => {
        console.error('Error loading attendance:', err);
        this.notificationService.showNotification({
          type: 'error',
          message: 'Failed to load attendance records',
        });
      },
    });
  }

  // Column filter methods
  openFilterMenu(columnKey: string, trigger: MatMenuTrigger): void {
    this.currentFilterMenuState.columnKey = columnKey;
    this.currentFilterMenuState.tempSelectedValues =
      this.filterValues[columnKey] ? [...this.filterValues[columnKey]] : [];
    this.currentFilterMenuState.searchText = '';
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
      filteredOptions.forEach(value => {
        if (!this.isTempSelected(value)) {
          this.currentFilterMenuState.tempSelectedValues.push(value);
        }
      });
    } else {
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
  }

  clearColumnFilter(): void {
    if (this.currentFilterMenuState.columnKey) {
      const key = this.currentFilterMenuState.columnKey;
      this.filterValues[key] = [];
      this.currentFilterMenuState.tempSelectedValues = [];
      this.applyColumnFilters();
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
    ); // Removed extra parenthesis
  }

  isFilterActive(columnKey: string): boolean {
    return this.filterValues[columnKey]?.length > 0;
  }

  collectUniqueColumnValues(data: AttendanceRecord[]): void {
    this.columnUniqueValues = {};

    this.displayedColumns.forEach(column => {
      const uniqueValues = new Set<string>();

      data.forEach(record => {
        const value = record[column as keyof AttendanceRecord];
        if (value !== undefined && value !== null) {
          uniqueValues.add(String(value));
        }
      });

      this.columnUniqueValues[column] = Array.from(uniqueValues).sort();
    });
  }

  // Rest of your existing methods (checkIn, getUserLocation, etc.) remain the same
  // Only the implementation needs to be copied over

  private isLateCheckIn(timestamp: string | Date): boolean {
    if (!this.shiftDetails.maxLoginTime) return false; // Fallback, can't determine

    const checkInTime = new Date(timestamp);
    const [maxHour, maxMin] = this.shiftDetails.maxLoginTime.split(':').map(Number);

    return (
      checkInTime.getHours() > maxHour ||
      (checkInTime.getHours() === maxHour && checkInTime.getMinutes() > maxMin)
    );
  }

  private normalizeShiftTimes(shift: any) {
    return {
      startMinutes: this.parseTimeToMinutes(shift.startTime || shift.start_time),
      endMinutes: this.parseTimeToMinutes(shift.endTime || shift.end_time),
      maxLoginMinutes: this.parseTimeToMinutes(shift.maxLoginTime || shift.max_login_time)
    };
  }

  private getShiftEndDateTime(
    shiftStart: string,
    shiftEnd: string,
    checkinDateTime: string
  ) {
    const start = moment(shiftStart, 'HH:mm');
    const end = moment(shiftEnd, 'HH:mm');

    const checkinDate = moment(checkinDateTime.split("T")[0], 'YYYY-MM-DD');

    const shiftStartDateTime = moment(checkinDate).set({
      hour: start.hour(),
      minute: start.minute(),
      second: 0
    });

    let shiftEndDateTime = moment(checkinDate).set({
      hour: end.hour(),
      minute: end.minute(),
      second: 0
    });

    // Overnight shift (end time next day)
    if (end.isBefore(start)) {
      shiftEndDateTime = shiftEndDateTime.add(1, 'day');
    }

    return { shiftStartDateTime, shiftEndDateTime };
  }



  async checkIn() {
    if (this.isCheckingIn) return;
    this.isCheckingIn = true;

    try {
      console.log('Starting check-in process...');

      const location = await this.locationCacheService.getLocation();
      this.latitude = location.latitude;
      this.longitude = location.longitude;
      const locationAccuracy = location.accuracy;

      console.log('Check-in location:', {
        lat: this.latitude,
        lng: this.longitude,
        accuracy: location.accuracy ? Math.round(location.accuracy) + 'm' : 'unknown'
      });

      const now = moment(); // current local time
      const formattedTime = now.format('HH:mm:ss');

      if (!this.shiftDetails) {
        this.notificationService.showNotification({
          type: 'error',
          message: 'Shift details not loaded. Please try again.'
        });
        this.isCheckingIn = false;
        return;
      }

      const { startTime, endTime, maxLoginTime } = (this.shiftDetails);

      // Convert shift times to moment
      let startMoment = moment(startTime, 'HH:mm:ss');
      let endMoment = moment(endTime, 'HH:mm:ss');
      let maxLoginMoment = moment(maxLoginTime, 'HH:mm:ss');

      // Handle cross-midnight shifts
      if (endMoment.isBefore(startMoment)) {
        endMoment.add(1, 'day');
        if (maxLoginMoment.isBefore(startMoment)) maxLoginMoment.add(1, 'day');
      }

      // Define time windows
      const twoHoursEarly = moment(startMoment).subtract(2, 'hours');   // earliest check-in window
      const fifteenMinBefore = moment(startMoment).subtract(15, 'minutes'); // grace before shift

      // Shift mismatch — too early or after shift end
      if (now.isBefore(twoHoursEarly) || now.isAfter(endMoment)) {
        this.openReasonDialog({
          title: 'Shift Mismatch Detected',
          message: `Your check-in time (${formattedTime}) is outside your scheduled shift (${startMoment.format('HH:mm')} to ${endMoment.format('HH:mm')}).`,
          isShiftMismatch: true
        }, location);
        return;
      }

      // Early Check-In — within 2h early but before 15-min grace window
      if (now.isBefore(fifteenMinBefore)) {
        this.openReasonDialog({
          title: 'Early Check-In',
          message: `You are checking-in way before your shift starts. Please provide a reason.`,
          isEarlyCheckIn: true
        }, location);
        return;
      }

      // Normal Check-In — from 15 min before start to max login time
      if (now.isSameOrAfter(fifteenMinBefore) && now.isSameOrBefore(maxLoginMoment)) {
        this.addRecord('-', '', location.accuracy); // Normal check-in
        return;
      }

      // Late Check-In — after max login but before shift end
      if (now.isAfter(maxLoginMoment) && now.isBefore(endMoment)) {
        this.openReasonDialog({
          title: 'Late Check-In Recorded',
          message: `You're checking in after ${maxLoginMoment.format('HH:mm')}. Please provide a reason.`,
          isEarlyCheckIn: false
        }, location);
        return;
      }

      // Fallback (should not occur)
      this.addRecord('-', '', location.accuracy);

    } catch (error) {
      this.handleCheckInError(error);
      this.isCheckingIn = false;
    }
  }

  // Helper for dialog handling
  openReasonDialog(data: any, location: any) {
    this.dialog.open(LateCheckinDialogComponent, {
      width: '500px',
      data
    }).afterClosed().subscribe(result => {
      if (result) {
        this.addRecord(result.reason, result.notes, location.accuracy);
      } else {
        this.isCheckingIn = false;
      }
    });
  }

  async checkOut() {
    if (this.isCheckingOut) return;
    this.isCheckingOut = true;

    try {

      const now = moment();
      const formattedTime = now.format("HH:mm:ss");

      // Basic validation
      if (!this.checkInStatus?.checkedIn) {
        this.notifyError("You must check in before checking out.");
        return this.stopCheckout();
      }

      if (this.checkInStatus?.checkedOut) {
        this.notifyInfo("You have already checked out.");
        return this.stopCheckout();
      }

      if (!this.checkInStatus?.checkinDateTime) {
        this.notifyError("Check-in timestamp missing. Unable to process checkout.");
        return this.stopCheckout();
      }

      if (!this.shiftDetails) {
        this.notifyError("Shift details not loaded. Please try again.");
        return this.stopCheckout();
      }

      const entryMoment = moment(this.checkInStatus.checkinDateTime);
      const entryDate = entryMoment.startOf("day");
      const todayDate = moment().startOf("day");

      // ---------- RULE 1: CHECKOUT FOR PREVIOUS DATE ----------
      if (!entryDate.isSame(todayDate, "day")) {
        return this.openLateCheckoutForPreviousDay(formattedTime, entryMoment);
      }

      // ---------- SAME-DAY CHECKOUT CALCULATION ----------
      const { shiftStartDateTime, shiftEndDateTime } = this.getShiftEndDateTime(
        this.shiftDetails.startTime,
        this.shiftDetails.endTime,
        this.checkInStatus.checkinDateTime
      );

      const graceBefore = moment(shiftEndDateTime).subtract(this.GRACE_MINUTES, "minutes");
      const graceAfter = moment(shiftEndDateTime).add(this.GRACE_MINUTES, "minutes");

      let category = "";
      let title = "";
      let message = "";

      if (now.isBefore(graceBefore)) {
        // EARLY
        category = "EARLY_CHECKOUT";
        title = "Early Check-Out";
        message = `You are checking out early. Shift ends at ${this.shiftDetails.endTime}. Please provide a reason.`;
      }

      else if (now.isBetween(graceBefore, graceAfter)) {
        // NORMAL
        category = "NORMAL";
      }

      else if (now.isAfter(graceAfter)) {
        // LATE
        category = "LATE_CHECKOUT";
        title = "Late Check-Out";
        message = `You are checking out late. Shift ended at ${this.shiftDetails.endTime}. Please provide a reason.`;
      }

      // ---------- OPEN APPROPRIATE DIALOG ----------
      if (category === "NORMAL") {
        this.processCheckout("On-Time Checkout", "");
        return this.stopCheckout();
      }

      this.dialog.open(CheckoutDialogComponent, {
        width: "500px",
        data: {
          checkOutTime: formattedTime,
          shiftEndTime: this.shiftDetails.endTime,
          title,
          message,
          isEarlyCheckout: category === "EARLY_CHECKOUT",
          isLateCheckout: category === "LATE_CHECKOUT",
          isShiftMismatch: false
        }
      }).afterClosed().subscribe(result => {
        if (result) {
          this.processCheckout(result.reason, result.notes);
        }
        this.stopCheckout();
      });

    } catch (err) {
      this.stopCheckout();
      this.handleCheckoutError(err);
    }
  }

  openLateCheckoutForPreviousDay(formattedTime: string, entryMoment: any) {
    const title = "Late Check-Out";
    const message = `You are checking out late for your shift on ${entryMoment.format('DD MMM')}. Please provide a reason.`;

    this.dialog.open(CheckoutDialogComponent, {
      width: "500px",
      data: {
        checkOutTime: formattedTime,
        shiftEndTime: this.shiftDetails.endTime,
        title,
        message,
        isEarlyCheckout: false,
        isLateCheckout: true,
        isShiftMismatch: false
      }
    }).afterClosed().subscribe(result => {
      if (result) this.processCheckout(result.reason, result.notes);
      this.stopCheckout();
    });
  }

  stopCheckout() {
    this.isCheckingOut = false;
    return;
  }

  notifyError(msg: string) {
    this.notificationService.showNotification({ type: 'error', message: msg });
  }

  notifyInfo(msg: string) {
    this.notificationService.showNotification({ type: 'info', message: msg });
  }

  private handleCheckInError(error: any) {
    console.error("Check-in failed:", error);
    this.notificationService.showNotification({
      type: 'error',
      message: 'Check-in failed: ' + (error instanceof Error ? error.message : 'Unknown error')
    });
    this.cdr.detectChanges();
  }

  private handleCheckoutError(error: any) {
    console.error("Check-out failed:", error);
    this.notificationService.showNotification({
      type: 'error',
      message: 'Check-out failed: ' + (error instanceof Error ? error.message : 'Unknown error')
    });
    this.isCheckingOut = false;
    this.cdr.detectChanges();
  }

  private async processCheckout(reason: string, notes: string) {
    try {
      console.log('Starting check-out process...');

      const location = await this.locationCacheService.getLocation();
      this.latitude = location.latitude;
      this.longitude = location.longitude;

      console.log('Check-out location:', {
        lat: this.latitude,
        lng: this.longitude,
        accuracy: location.accuracy ? Math.round(location.accuracy) + 'm' : 'unknown'
      });


      const checkoutPayload: AttendanceRequest = {
        ldap: this.userInfo.ldap,
        reason: reason || undefined,
        comment: notes || undefined,
        latitude: this.latitude,
        longitude: this.longitude,
        entryTimestamp: new Date().toISOString(), // This will be used as checkout timestamp
        deviceType: this.deviceDetectionService.getDeviceType(),
        accuracy: (location as any).accuracy || undefined
      };

      this.attendanceService.checkoutAttendance(checkoutPayload).subscribe({
        next: (response: any) => {
          console.log('Checkout response:', response);

          if (response && response.data) {
            this.notificationService.showNotification({
              type: 'success',
              message: 'Checkout recorded successfully'
            });

            // Update the local record with checkout information
            const today = new Date().toISOString().split('T')[0];
            const recordIndex = this._attendanceRecords.findIndex(record => record.date === today);

            if (recordIndex !== -1) {
              const checkoutData = response.data;
              this._attendanceRecords[recordIndex] = {
                ...this._attendanceRecords[recordIndex],
                checkOut: formatDate(checkoutData.exitTimestamp, 'HH:mm', 'en-US'),
                checkoutReason: checkoutData.lateOrEarlyLogoutReason || '-',
                status: checkoutData.lateOrEarlyCheckout || '',
                statusArray: this.parseStatusString(checkoutData.lateOrEarlyCheckout || ''),
                checkoutStatus: checkoutData.lateOrEarlyCheckout || '-',
                isCheckOutOutsideOffice: checkoutData.checkoutWhileOutsideOffice
              };

              this.dataSource.data = [...this._attendanceRecords];
              this.collectUniqueColumnValues(this._attendanceRecords);
            }

            // Update the server-side checkout status
            if (this.checkInStatus) {
              const checkoutData = response.data;
              const checkOutTime = formatDate(checkoutData.exitTimestamp, 'HH:mm', 'en-US');

              this.checkInStatus = {
                ...this.checkInStatus,
                checkedOut: true,
                checkedOutStatus: 'Checked out successfully',
                checkOutTime: checkOutTime,
                earlyOrLateCheckOut: checkoutData.lateOrEarlyCheckout || ''
              };

              // Stop timer
              this.stopTimer(checkOutTime);
            }
            this.isCheckingOut = false;
            this.cdr.detectChanges();

            // Refresh attendance status and records after successful checkout
            this.checkAttendanceStatus();
            this.loadAttendanceRecordsTemp();
          } else {
            console.error('Invalid checkout response from server:', response);
            throw new Error('Invalid response from server');
          }
        },
        error: (err) => {
          console.error('Checkout failed', err);
          this.notificationService.showNotification({
            type: 'error',
            message: 'Checkout Failed, Please Try Later'
          });
          this.isCheckingOut = false;
        }
      });
    } catch (error) {
      this.handleCheckoutError(error);
    }
  }

  getUserLocation(): Promise<{ latitude: number; longitude: number }> {
    return new Promise((resolve, reject) => {
      if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
          (position) => {
            this.latitude = position.coords.latitude;
            this.longitude = position.coords.longitude;
            console.log("Location fetched:", this.latitude, this.longitude);
            resolve({ latitude: this.latitude, longitude: this.longitude });
          },
          (error) => {
            console.error("Error getting location:", error.message);
            this.notificationService.showNotification({
              type: 'error',
              message: 'Please allow location access to mark attendance.'
            });
            reject(error);
          }
        );
      } else {
        const message = 'Geolocation is not supported by this browser.';
        this.notificationService.showNotification({
          type: 'error',
          message: message
        });
        reject(new Error(message));
      }
    });
  }

  private addRecord(reason: string, notes: string, locationAccuracy?: number) {
    const attendancePayload: AttendanceRequest = {
      ldap: this.userInfo.ldap,
      reason: reason || undefined,
      comment: notes || undefined,
      latitude: this.latitude,
      longitude: this.longitude,
      entryTimestamp: new Date().toISOString(), // UTC, handled as IST on backend
      deviceType: this.deviceDetectionService.getDeviceType(),
      accuracy: locationAccuracy
    };

    this.attendanceService.markAttendance(attendancePayload).subscribe({
      next: (response: any) => {
        console.log('Mark attendance response:', response);

        if (response && response.id) {
          this.notificationService.showNotification({
            type: 'success',
            message: 'Attendance marked successfully'
          });

          const formattedEntryTime = formatDate(response.entryTimestamp, 'HH:mm', 'en-US');

          const newRecord: AttendanceRecord = {
            id: response.id,
            ldap: this.userInfo.ldap || '-',
            name: this.userFullName || '-',
            team: this.userTeam || '-',
            date: formatDate(response.entryDate, 'yyyy-MM-dd', 'en-US'),
            checkInDate: formatDate(response.entryDate, 'yyyy-MM-dd', 'en-US'),
            checkOutDate: response.exitDate ? formatDate(response.exitDate, 'yyyy-MM-dd', 'en-US') : '-',
            checkIn: formattedEntryTime,
            status: response.lateOrEarlyCheckout || '',
            statusArray: this.parseStatusString(response.lateOrEarlyCheckout || ''),
            reason: response.lateLoginReason || '-',
            comment: response.comment || 'NA',
            isOutsideOffice: response.isOutsideOffice,
            isDefaulter: response.isDefaulter, // Keep existing defaulter status from server
            checkoutStatus: response.lateOrEarlyCheckout || '-'
          };

          // Apply enhanced compliance logic to new records
          newRecord.isDefaulter = this.isRecordNonCompliant(newRecord);

          console.log('New record created:', newRecord);

          // Update local records immediately
          this._attendanceRecords = [newRecord, ...this._attendanceRecords];
          this.dataSource.data = this._attendanceRecords;

          // Update column unique values for filtering
          this.collectUniqueColumnValues(this._attendanceRecords);

          // Use entryTimestamp as the real check-in time
          this.checkedIn = true;
          this.checkedInTime = formattedEntryTime;

          this.checkInStatus = {
            status: `Checked in at ${this.checkedInTime}`,
            checkedIn: true,
            checkInTime: this.checkedInTime,
            isLate: this.isLateCheckIn(response.entryTimestamp),
            checkedOutStatus: '',
            checkedOut: false,
            checkOutTime: null,
            earlyOrLateCheckOut: '',
            checkinDateTime: response.entryTimestamp,
            checkOutDateTime: response.exitTimestamp
          };

          console.log('Updated checkInStatus:', this.checkInStatus);
          console.log('isUserCheckedInToday:', this.isUserCheckedInToday);

          // Start timer
          this.startTimer(this.checkedInTime);

          this.cdr.detectChanges();
          this.checkAttendanceStatus();
        } else {
          console.error('Invalid response from server:', response);
          throw new Error('Invalid response from server');
        }
      },
      error: (err) => {
        console.error('Attendance marking failed', err);
        this.notificationService.showNotification({
          type: 'error',
          message: 'Attendance Marking Failed, Please Try Later'
        });
      }
    });
  }


  isRoleAllowed(): boolean {
    const allowedRoles = ['LEAD', 'MANAGER', 'ADMIN_OPS_MANAGER'];
    return this.userRole !== undefined && allowedRoles.includes(this.userRole);
  }

  canCheckIn(): boolean {
    return this.userRole === 'USER' || this.userRole === 'LEAD' || this.userRole === 'MANAGER';
  }

  currentUser() {
    this.leaveService.managerDetails$.pipe(
      filter(data => !!data && data.length > 0),
      take(1)
    ).subscribe({
      next: (data) => {
        this.userInfo = data[0];
        this.userTeam = this.userInfo.programAlignment;
        this.userFullName = this.userInfo.name;

        if (this.userInfo.ldap && (this.userRole === 'USER' || this.userRole === 'LEAD' || this.userRole === 'MANAGER')) {
          this.checkAttendanceStatus();
        }
        if (this.userInfo.shift) {
          this.attendanceService.getShiftDetails(this.userInfo.shift).subscribe({
            next: (shiftDetails: any) => {
              this.shiftDetails = {
                code: shiftDetails.code,
                startTime: shiftDetails.startTime,
                endTime: shiftDetails.endTime,
                maxLoginTime: shiftDetails.maxLoginTime
              };
            },
            error: (err) => {
              console.error('Failed to load shift details on init:', err);
            }
          });
        }

        this.loadAttendanceRecordsTemp();
      },
      error: (err) => {
        console.error('Error fetching user info:', err);
      }
    });
  }

  parseTimeToMinutes(time: string): number {
    const [hours, minutes] = time.split(':').map(Number);
    return hours * 60 + minutes;
  }


  private updateCheckInState(status: CheckInStatusResponse) {
    this.checkInStatus = status;

    // Update local state to match the server response
    this.checkedIn = status.checkedIn;
    if (status.checkedIn && status.checkInTime) {
      this.checkedInTime = status.checkInTime;

      // Start timer if checked in
      if (status.checkedOut && status.checkOutTime) {
        // User has checked out - show total time
        this.startTimer(status.checkInTime);
        this.stopTimer(status.checkOutTime);
      } else {
        // User is still checked in - show running timer
        this.startTimer(status.checkInTime);
      }
    } else {
      // User is not checked in - reset timer
      this.resetTimer();
    }

    this.cdr.detectChanges();
  }

  // Reset timer method and clear localStorage
  private resetTimer(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
    this.checkInTimestamp = null;
    this.checkOutTimestamp = null;
    this.displayTimer = '00:00:00';
  }

  refreshAttendance(): void {
    this.currentUser(); // Will re-fetch user info + attendance
  }

  checkAttendanceStatus(ldap?: string) {
    // No longer need to pass LDAP - the backend uses authenticated user automatically
    this.attendanceService.getCheckInStatus().pipe(
      takeUntil(this.destroy$) // Add ngOnDestroy handler
    ).subscribe({
      next: (response) => {
        this.updateCheckInState(response);

        // Force reload if records don't match status
        const today = formatDate(new Date(), 'yyyy-MM-dd', 'en-US');
        if (response.checkedIn && !this._attendanceRecords.some(r => r.date === today)) {
          this.loadAttendanceRecordsTemp();
        }
      },
      error: (err) => {
        console.error('Status check failed:', err);
        this.updateCheckInState({
          status: 'Error checking status',
          checkedIn: false,
          checkInTime: '',
          isLate: false,
          checkedOutStatus: '',
          checkedOut: false,
          checkOutTime: null,
          earlyOrLateCheckOut: '',
          checkinDateTime: null,
          checkOutDateTime: null
        });
      }
    });
  }

  getLastWeekStartDate(): Date {
    const date = new Date();
    date.setDate(date.getDate() - 7);
    return date;
  }

  applyDateFilter(): void {
    console.log("Applying date Filter");
    this.loadAttendanceRecordsTemp();
  }


  downloadCSV(): void {
    const exportData = this.dataSource.data.map(item => {
      // Format location to show both check-in and check-out simply
      let locationText = item.isOutsideOffice ? 'Outside' : 'Inside';

      // Add check-out location if available
      if (item.checkOut && item.checkOut !== '-') {
        const checkOutLocation = item.isCheckOutOutsideOffice ? 'Outside' : 'Inside';
        locationText += ', ' + checkOutLocation;
      }

      return {
        'LDAP': item.ldap,
        'Name': item.name,
        'Team': item.team,
        'Date': item.date,
        'Check-In Date': item.checkInDate,
        'Check-In Time': item.checkIn,
        'Check-Out Date': item.checkOutDate || '-',
        'Check-Out Time': item.checkOut || '-',
        'Check-In Reason': item.reason,
        'Check-In Notes': item.comment || '-',
        'Check-Out Reason': item.checkoutReason || '-',
        'Status': item.status,
        'Location': locationText,
        'Compliance': item.isDefaulter ? 'No' : 'Yes'
      };
    });

    const worksheet = XLSX.utils.json_to_sheet(exportData);
    const workbook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, worksheet, 'Attendance Records');

    const excelBuffer: any = XLSX.write(workbook, { bookType: 'csv', type: 'array' });
    const blob = new Blob([excelBuffer], { type: 'text/csv;charset=utf-8;' });
    saveAs(blob, `attendance_records_${new Date().toISOString().split('T')[0]}.csv`);
  }

  getStatusClass(record: AttendanceRecord): string {
    if (record.isDefaulter) return 'non-compliant';
    return record.status === 'Late' ? 'late' : 'on-time';
  }

  // Method to determine if compliance should be "Yes" (defaulter)
  // Compliance is "Yes" if either check-in or check-out status is not "OnTime"
  isRecordNonCompliant(record: AttendanceRecord): boolean {
    // Check if check-in status is not on time
    const checkInNotOnTime = record.statusArray && record.statusArray.some(status =>
      status && !this.isOnTimeStatus(status)
    );

    // Check if check-out status is not on time
    const checkOutNotOnTime = record.checkoutStatus && record.checkoutStatus !== '-' && !this.isOnTimeStatus(record.checkoutStatus);

    // Return true if either check-in or check-out is not on time
    return (checkInNotOnTime || checkOutNotOnTime) || record.isDefaulter;
  }

  // Getter method for template to check if user is checked in today
  get isUserCheckedInToday(): boolean {
    // Primary check: server status
    if (this.checkInStatus?.checkedIn) {
      return true;
    }

    // Secondary check: local records
    const today = new Date().toISOString().split('T')[0];
    const hasLocalRecord = this._attendanceRecords.some(record => record.date === today);

    return hasLocalRecord;
  }

  // Getter method for template to check if user is checked out today
  get isUserCheckedOutToday(): boolean {
    // Primary check: server status
    if (this.checkInStatus) {
      return this.checkInStatus.checkedOut;
    }

    // Fallback to local data if server status is not available
    const today = new Date().toISOString().split('T')[0];
    const todayRecord = this._attendanceRecords.find(record => record.date === today);
    return todayRecord ? (todayRecord.checkOut !== '-' && todayRecord.checkOut !== undefined) : false;
  }

  // Getter method for check-in status display
  get checkInStatusDisplay(): string {
    if (this.checkInStatus?.checkedIn && this.checkInStatus.status) {
      return this.checkInStatus.status;
    }

    // Fallback to local data if server status is not available
    const todayTime = this.getTodayCheckInTime();
    if (todayTime && todayTime !== '--:--') {
      return `Checked in at ${todayTime} `;
    }

    return '';
  }

  // Getter method for check-out status display
  get checkOutStatusDisplay(): string {
    // Primary check: server status
    if (this.checkInStatus?.checkedOut && this.checkInStatus.checkOutTime) {
      return `Checked out at ${this.checkInStatus.checkOutTime} `;
    }

    // Fallback to local data if server status is not available
    const today = new Date().toISOString().split('T')[0];
    const todayRecord = this._attendanceRecords.find(record => record.date === today);

    if (todayRecord && todayRecord.checkOut && todayRecord.checkOut !== '-') {
      return `Checked out at ${todayRecord.checkOut} `;
    }

    return '';
  }

  ngAfterViewInit(): void {
    if (this.paginator) { this.dataSource.paginator = this.paginator; }
    if (this.sort) { this.dataSource.sort = this.sort; }

    // Defer loading columns so we don't mutate bindings during CD
    setTimeout(() => {
      this.loadDisplayedColumns();
      // optional: ensure Angular updates immediately
      this.cd.detectChanges();
    }, 0);
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
    this.allColumnsSelected = checked; // Keep this for UI state

    if (checked) {
      // Show all columns (actions will be added separately as it's always visible)
      // Filter out 'actions' from allColumns if it somehow got there, then add it explicitly
      this.displayedColumns = [...this.allColumns.filter(col => col !== 'actions'), 'actions'];
    } else {
      // Hide all columns except required ones (keep at least ldap, date, and actions visible)
      this.displayedColumns = this.displayedColumns.filter(column =>
        column === 'ldap' || column === 'date' || column === 'actions'
      );
    }
    this.saveDisplayedColumns();
    this.updateAllColumnsSelectedState(); // Update state after changing displayedColumns
  }

  toggleColumn(column: string): void {
    // Prevent toggling the actions column - it should always be visible
    if (column === 'actions') {
      return;
    }

    const index = this.displayedColumns.indexOf(column);

    if (index === -1) {
      // Add column - find the correct position based on allColumns order
      const allColumnsIndex = this.allColumns.indexOf(column);
      if (allColumnsIndex !== -1) {
        // Find the position to insert (before actions column, or at the end if actions is not present)
        let insertIndex = this.displayedColumns.indexOf('actions');
        if (insertIndex === -1) insertIndex = this.displayedColumns.length;

        // Iterate through allColumns to find the correct insertion point
        // Insert before the first displayed column that appears after the current column in allColumns
        for (let i = allColumnsIndex + 1; i < this.allColumns.length; i++) {
          if (this.displayedColumns.includes(this.allColumns[i])) {
            insertIndex = this.displayedColumns.indexOf(this.allColumns[i]);
            break;
          }
        }
        this.displayedColumns.splice(insertIndex, 0, column);
      }
    } else {
      // Remove column
      this.displayedColumns.splice(index, 1);
    }

    this.saveDisplayedColumns();
    this.updateAllColumnsSelectedState();
  }

  isColumnDisplayed(column: string): boolean {
    return this.displayedColumns.includes(column);
  }

  updateAllColumnsSelectedState(): void {
    const filteredColumns = this.getFilteredColumns();

    const allFilteredColumnsSelected = filteredColumns.every(col =>
      this.displayedColumns.includes(col)
    );

    this.allColumnsSelected = allFilteredColumnsSelected;
  }

  saveDisplayedColumns(): void {
    localStorage.setItem('attendanceTableDisplayedColumns', JSON.stringify(this.displayedColumns));
  }

  loadDisplayedColumns(): void {
    const savedColumns = localStorage.getItem('attendanceTableDisplayedColumns');
    if (savedColumns) {
      try {
        const parsedColumns = JSON.parse(savedColumns);

        // Validate the saved columns to ensure they match our current template (id is excluded)
        const validColumns = ['ldap', 'name', 'team', 'date', 'checkInDate', 'checkOutDate', 'checkIn', 'checkOut', 'status', 'reason', 'checkoutReason', 'comment', 'isOutsideOffice', 'isDefaulter', 'actions'];
        const filteredColumns = parsedColumns.filter((col: string) => validColumns.includes(col));

        // If we have valid columns, use them; otherwise, use defaults
        if (filteredColumns.length > 0) {
          this.displayedColumns = filteredColumns;
        } else {
          // Set default columns
          this.displayedColumns = ['ldap', 'checkInDate', 'checkOutDate', 'checkIn', 'checkOut', 'status', 'isOutsideOffice', 'reason', 'checkoutReason', 'comment', 'isDefaulter', 'actions'];
        }
      } catch (error) {
        console.error('Error parsing saved columns:', error);
        // Set default columns if parsing fails
        this.displayedColumns = ['ldap', 'checkInDate', 'checkOutDate', 'checkIn', 'checkOut', 'status', 'isOutsideOffice', 'reason', 'checkoutReason', 'comment', 'isDefaulter', 'actions'];
      }
    } else {
      // Set default columns
      this.displayedColumns = ['ldap', 'checkInDate', 'checkOutDate', 'checkIn', 'checkOut', 'status', 'isOutsideOffice', 'reason', 'checkoutReason', 'comment', 'isDefaulter', 'actions'];
    }

    // Always ensure 'actions' column is present and at the end
    if (!this.displayedColumns.includes('actions')) {
      this.displayedColumns.push('actions');
    }

    // Update allColumnsSelected state
    this.updateAllColumnsSelectedState();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();

    // Clear timer interval
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
  }

  // Column resizing methods
  startResizing(event: MouseEvent, column: string): void {
    event.preventDefault();
    this.isResizing = true;
    this.currentResizingColumn = column;
    this.startX = event.clientX;

    // Get current width of the column
    const headerCell = event.target as HTMLElement;
    this.startWidth = headerCell.offsetWidth;

    // Add event listeners
    document.addEventListener('mousemove', this.onResizing);
    document.addEventListener('mouseup', this.stopResizing);
  }

  onResizing = (event: MouseEvent): void => {
    if (!this.isResizing || !this.currentResizingColumn) return;

    const deltaX = event.clientX - this.startX;
    const newWidth = Math.max(100, this.startWidth + deltaX); // Minimum width of 100px

    this.columnWidths[this.currentResizingColumn] = newWidth;

    // Apply the width to the column
    this.applyColumnWidth(this.currentResizingColumn, newWidth);
  };

  stopResizing = (): void => {
    if (!this.isResizing) return;

    this.isResizing = false;
    this.currentResizingColumn = null;

    // Remove event listeners
    document.removeEventListener('mousemove', this.onResizing);
    document.removeEventListener('mouseup', this.stopResizing);
  };

  applyColumnWidth(column: string, width: number): void {
    const table = document.querySelector('mat-table') as HTMLElement;
    if (!table) return;

    const columnIndex = this.displayedColumns.indexOf(column);
    if (columnIndex === -1) return;

    // Apply width to header cells
    const headerCells = table.querySelectorAll('mat-header-cell') as NodeListOf<HTMLElement>;
    if (headerCells[columnIndex]) {
      headerCells[columnIndex].style.width = `${width} px`;
    }

    // Apply width to data cells
    const rowCells = table.querySelectorAll('mat-cell') as NodeListOf<HTMLElement>;
    rowCells.forEach((cell, index) => {
      if (index % this.displayedColumns.length === columnIndex) {
        cell.style.width = `${width} px`;
      }
    });
  }
  viewSelfAttendance() {
    this.showMyAttendance = !this.showMyAttendance;
    this.loadAttendanceRecordsTemp();
  }


  getAttendanceHeaderText() {
    const today = new Date().toISOString().split("T")[0];
    const checkinDate = this.checkInStatus?.checkinDateTime
      ? this.checkInStatus.checkinDateTime.split("T")[0]
      : null;

    if (this.checkInStatus?.checkedIn && !this.checkInStatus?.checkedOut) {
      if (checkinDate && checkinDate !== today) {
        return {
          text: `Please checkout for ${this.formatDateForDisplay(checkinDate)}`,
          isWarning: true
        };
      }
    }

    return {
      text: "Today's Attendance",
      isWarning: false
    };
  }


  // Helper method to format date for display
  private formatDateForDisplay(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  /**
   * Check if user can edit attendance records
   * Users cannot edit their own records
   */
  canEditAttendance(record?: AttendanceRecord): boolean {
    const hasRole = this.userRole === 'LEAD' ||
      this.userRole === 'MANAGER' ||
      this.userRole === 'ADMIN_OPS_MANAGER';

    if (!hasRole) {
      return false;
    }

    // Must have both record and userInfo to make a decision
    if (!record || !this.userInfo) {
      return false;
    }

    // Only allow editing if it's NOT the user's own record
    return record.ldap !== this.userInfo.ldap;
  }

  /**
   * Open dialog to edit attendance status
   */
  openStatusEditDialog(record: AttendanceRecord): void {
    import('./attendance-edit-dialog.component').then(m => {
      const dialogRef = this.dialog.open(m.AttendanceEditDialogComponent, {
        width: '500px',
        data: {
          attendanceId: record.id,
          currentStatus: record.status,
          editType: 'status',
          employeeName: record.name,
          date: record.date
        }
      });

      dialogRef.afterClosed().subscribe(result => {
        if (result) {
          this.updateAttendanceStatus(record.id, result.newStatus, result.reason);
        }
      });
    });
  }

  /**
   * Open dialog to edit compliance status
   */
  openComplianceEditDialog(record: AttendanceRecord): void {
    import('./attendance-edit-dialog.component').then(m => {
      const dialogRef = this.dialog.open(m.AttendanceEditDialogComponent, {
        width: '500px',
        data: {
          attendanceId: record.id,
          currentCompliance: record.isDefaulter,
          editType: 'compliance',
          employeeName: record.name,
          date: record.date
        }
      });

      dialogRef.afterClosed().subscribe(result => {
        if (result) {
          this.updateComplianceStatus(record.id, result.isDefaulter, result.reason);
        }
      });
    });
  }

  /**
   * Update attendance status via API
   */
  updateAttendanceStatus(attendanceId: string, newStatus: string, reason: string): void {
    this.attendanceService.updateAttendanceStatus(Number(attendanceId), newStatus, reason).subscribe({
      next: (response) => {
        this.notificationService.showNotification({
          type: 'success',
          message: 'Attendance status updated successfully'
        });
        // Mark record as modified for highlighting
        this.markRecordAsModified(attendanceId);
        // Refresh the attendance records
        this.loadAttendanceRecordsTemp();
      },
      error: (error) => {
        console.error('Error updating attendance status:', error);
        this.notificationService.showNotification({
          type: 'error',
          message: error.message || 'Failed to update attendance status'
        });
      }
    });
  }

  /**
   * Update compliance status via API
   */
  updateComplianceStatus(attendanceId: string, isDefaulter: boolean, reason: string): void {
    console.log('Updating compliance:', { attendanceId, isDefaulter, reason });
    this.attendanceService.updateComplianceStatus(Number(attendanceId), isDefaulter, reason).subscribe({
      next: (response) => {
        console.log('Compliance update response:', response);
        this.notificationService.showNotification({
          type: 'success',
          message: 'Compliance status updated successfully'
        });
        // Mark record as modified for highlighting
        this.markRecordAsModified(attendanceId);
        // Refresh the attendance records
        this.loadAttendanceRecordsTemp();
      },
      error: (error) => {
        console.error('Error updating compliance status:', error);
        this.notificationService.showNotification({
          type: 'error',
          message: error.message || 'Failed to update compliance status'
        });
      }
    });
  }

  /**
   * View audit history for an attendance record
   */
  viewAuditHistory(record: AttendanceRecord): void {
    import('./attendance-audit-dialog.component').then(m => {
      this.dialog.open(m.AttendanceAuditDialogComponent, {
        width: '650px',
        data: {
          attendanceId: record.id,
          employeeName: record.name,
          date: record.date
        }
      });
    });
  }

  // Cache of record IDs that have audit history (for highlighting)
  private recordsWithAuditHistory = new Set<string>();

  /**
   * Check if a record has audit history (has been modified)
   */
  hasAuditHistory(record: AttendanceRecord): boolean {
    return this.recordsWithAuditHistory.has(record.id);
  }

  /**
   * Load audit history status for all visible records
   * This is called after records are loaded to check which ones have been modified
   */
  private loadAuditHistoryStatus(): void {
    console.log('loadAuditHistoryStatus called');
    console.log('Total records:', this._attendanceRecords.length);

    // Check each record for audit history - allow all users to see if their records were modified
    let checkedCount = 0;
    const totalRecords = this._attendanceRecords.length;

    this._attendanceRecords.forEach(record => {
      this.attendanceService.getAuditHistory(Number(record.id)).subscribe({
        next: (response) => {
          console.log(`Audit history for record ${record.id}:`, response);
          if (response && response.data && response.data.length > 0) {
            console.log(`Record ${record.id} has ${response.data.length} audit entries - marking as modified`);
            this.recordsWithAuditHistory.add(record.id);
          } else {
            console.log(`Record ${record.id} has no audit history`);
          }

          checkedCount++;
          if (checkedCount === totalRecords) {
            console.log('✅ Audit history check complete!');
            console.log('Modified records:', Array.from(this.recordsWithAuditHistory));
            console.log('Total modified:', this.recordsWithAuditHistory.size);
          }
        },
        error: (err) => {
          console.error(`Error loading audit history for record ${record.id}:`, err);
          checkedCount++;
        }
      });
    });
  }

  /**
   * Mark a record as modified after successful update
   */
  private markRecordAsModified(attendanceId: string): void {
    this.recordsWithAuditHistory.add(attendanceId);
  }
}