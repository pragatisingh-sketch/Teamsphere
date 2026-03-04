import { Component, ElementRef, Input, ViewChild } from '@angular/core';
import { NotificationService } from 'src/app/shared/notification.service';
import { FormBuilder, FormGroup, Validators, FormControl } from '@angular/forms';
import { LeaveService } from 'src/app/services/leave.service';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from 'src/app/confirm-dialog/confirmation-dialog.component';
import { HttpClient } from '@angular/common/http';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { environment } from '../../../../environments/environment';

interface BaseResponse<T> {
  status: string;
  code: number;
  message: string;
  data: T;
}

export interface Holiday {
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
  selector: 'app-balance',
  templateUrl: './balance.component.html',
  styleUrls: ['./balance.component.css']
})

export class BalanceComponent {
  @Input() leaveBalance: any;
  @Input() canUpload: boolean = false;
  @Input() userRole?: string;
  @ViewChild('fileInput') fileInput!: ElementRef;
  @ViewChild('holidayFileInput') holidayFileInput!: ElementRef;
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  userName: string | undefined;
  selectedFile: File | null = null;
  googleSheetId: string = '';

  // Holiday management properties
  baseUrl = environment.apiUrl;
  holidayDataSource = new MatTableDataSource<Holiday>();
  holidayDisplayedColumns: string[] = ['holidayDate', 'holidayName', 'description', 'holidayType', 'uploadedBy', 'actions'];
  selectedHolidayFile: File | null = null;
  isUploadingHolidays = false;

  constructor(
    private fb: FormBuilder,
    private notificationService: NotificationService,
    private leaveService: LeaveService,
    private dialog: MatDialog,
    private http: HttpClient
  ) { }

  ngOnInit() {
    this.fetchLeaveBalance();
    this.loadHolidays();
    this.userRole = localStorage.getItem('role') || undefined;

    // Set up holiday table filter
    this.holidayDataSource.filterPredicate = (data: Holiday, filter: string) => {
      return Object.values(data)
        .some(value => value?.toString().toLowerCase().includes(filter.toLowerCase()));
    };
  }

  ngAfterViewInit(): void {
    if (this.paginator && this.sort) {
      this.holidayDataSource.paginator = this.paginator;
      this.holidayDataSource.sort = this.sort;
    }
  }

  fetchLeaveBalance(): void {
    this.userName = localStorage.getItem('username') || '';

    this.leaveService.getLeaveDetails(this.userName).subscribe({
      next: (data) => {
        this.leaveBalance.sick = data[0];
        this.leaveBalance.casual = data[1];
        this.leaveBalance.earned = data[2];
        this.leaveBalance.total = data[3];
        this.leaveBalance.totalwfh = data[4];
        this.leaveBalance.qtrwfh = data[5];
      },
      error: (error) => {
        console.error('Error fetching leave details:', error);
        this.notificationService.showNotification({
          type: 'error',
          message: 'Leave Balance not available for now.Please try later.'
        })
      }
    });
  }


  onFileSelected(event: any): void {
    const file: File = event.target.files[0];

    if (!file) return;

    const fileName = file.name.toLowerCase();
    if (!fileName.endsWith('.csv')) {
      this.notificationService.showNotification({
        type: 'error',
        message: 'Only CSV files are supported.'
      });
      return;
    }

    this.selectedFile = file;
  }

  uploadLeaveBalance(): void {
    if (!this.selectedFile) {
      this.notificationService.showNotification({
        type: 'error',
        message: 'Please select a file before uploading.'
      });
      return;
    }

    const formData = new FormData();
    formData.append('file', this.selectedFile);

    this.leaveService.uploadLeaveBalance(formData).subscribe({
      next: (result: any) => {
        const successCount = result?.successCount ?? 0;
        const skippedCount = result?.skippedCount ?? 0;
        const msg = skippedCount > 0
          ? `Upload complete: ${successCount} updated, ${skippedCount} skipped. Downloading skipped report...`
          : `Upload complete: ${successCount} employee(s) updated successfully.`;

        this.notificationService.showNotification({ type: 'success', message: msg });

        if (skippedCount > 0 && result?.skippedEmployees?.length > 0) {
          this.downloadSkippedReport(result.skippedEmployees);
        }

        this.resetFileInput();
        this.fetchLeaveBalance();
      },
      error: (err) => {
        if (err.status === 409) {
          // The message is in err.error.message (from LeaveBalanceUploadResultDto)
          const rawMsg: string = err.error?.message || '';
          const match = rawMsg.match(/by\s+([\w.@]+)/i);
          const uploader = match ? match[1] : 'someone else';

          const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
            data: {
              title: 'Already Uploaded',
              message: `Leave balances are already uploaded for this month by ${uploader}. Do you want to re-upload?`,
              confirmButtonText: 'Re-upload',
              color: 'warn'
            }
          });

          dialogRef.afterClosed().subscribe(result => {
            if (result === true) {
              this.forceUploadLeaveBalance();
            }
          });
        } else {
          this.notificationService.showNotification({
            type: 'error',
            message: err.error?.message || 'Failed to upload leave balances.'
          });
        }
      }
    });
  }

  forceUploadLeaveBalance(): void {
    if (!this.selectedFile) return;

    const formData = new FormData();
    formData.append('file', this.selectedFile);

    this.leaveService.uploadLeaveBalance(formData, true).subscribe({
      next: (result: any) => {
        const successCount = result?.successCount ?? 0;
        const skippedCount = result?.skippedCount ?? 0;
        const msg = skippedCount > 0
          ? `Re-upload complete: ${successCount} updated, ${skippedCount} skipped. Downloading skipped report...`
          : `Re-upload complete: ${successCount} employee(s) updated successfully.`;

        this.notificationService.showNotification({ type: 'success', message: msg });

        if (skippedCount > 0 && result?.skippedEmployees?.length > 0) {
          this.downloadSkippedReport(result.skippedEmployees);
        }

        this.resetFileInput();
        this.fetchLeaveBalance();
      },
      error: () => this.notificationService.showNotification({
        type: 'error',
        message: 'Failed to re-upload leave balances.'
      })
    });
  }

  /**
   * Generates and auto-downloads a CSV report of skipped employees.
   */
  private downloadSkippedReport(skippedEmployees: { ldap: string; reason: string }[]): void {
    let csv = 'LDAP/Email,Reason\n';
    for (const emp of skippedEmployees) {
      const ldap = emp.ldap.replace(/"/g, '""');
      const reason = emp.reason.replace(/"/g, '""');
      csv += `"${ldap}","${reason}"\n`;
    }

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const today = new Date().toISOString().slice(0, 10);
    const a = document.createElement('a');
    a.href = url;
    a.download = `leave_balance_skipped_${today}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  private resetFileInput(): void {
    this.selectedFile = null;
    if (this.fileInput && this.fileInput.nativeElement) {
      (this.fileInput.nativeElement as HTMLInputElement).value = '';
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

    if (confirm(`Are you sure you want to delete the holiday "${holiday.holidayName}"?`)) {
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
}