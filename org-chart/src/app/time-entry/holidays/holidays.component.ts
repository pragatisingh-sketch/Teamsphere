import { Component, OnInit, ViewChild } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { NotificationService } from '../../shared/notification.service';
import { environment } from '../../../environments/environment';

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
  selector: 'app-holidays',
  templateUrl: './holidays.component.html',
  styleUrls: ['./holidays.component.css']
})
export class HolidaysComponent implements OnInit {
  displayedColumns: string[] = ['holidayDate', 'holidayName', 'description', 'holidayType', 'uploadedBy', 'actions'];
  dataSource = new MatTableDataSource<Holiday>();
  
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  baseUrl = environment.apiUrl;
  userRole: string | undefined;
  selectedFile: File | null = null;
  isUploading = false;

  constructor(
    private http: HttpClient,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.userRole = localStorage.getItem('role') || undefined;
    console.log('User role in holidays component:', this.userRole);
    this.loadHolidays();

    this.dataSource.filterPredicate = (data: Holiday, filter: string) => {
      return Object.values(data)
        .some(value => value?.toString().toLowerCase().includes(filter.toLowerCase()));
    };
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  isAdminOpsManager(): boolean {
    return this.userRole === 'ADMIN_OPS_MANAGER';
  }

  loadHolidays(): void {
    this.http.get<BaseResponse<Holiday[]>>(`${this.baseUrl}/api/holidays`)
      .subscribe({
        next: (response) => {
          if (response.status === 'success') {
            this.dataSource.data = response.data;
            console.log('Loaded holidays:', response.data);
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

  applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file && file.type === 'text/csv') {
      this.selectedFile = file;
    } else {
      this.notificationService.showNotification({
        type: 'error',
        message: 'Please select a valid CSV file'
      });
      this.selectedFile = null;
    }
  }

  uploadCSV(): void {
    if (!this.selectedFile) {
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

    this.isUploading = true;
    const formData = new FormData();
    formData.append('file', this.selectedFile);

    this.http.post<BaseResponse<Holiday[]>>(`${this.baseUrl}/api/holidays/upload-csv`, formData)
      .subscribe({
        next: (response) => {
          this.isUploading = false;
          if (response.status === 'success') {
            this.notificationService.showNotification({
              type: 'success',
              message: response.message || 'Holidays uploaded successfully!'
            });
            this.loadHolidays(); // Reload the holidays list
            this.selectedFile = null;
            // Reset file input
            const fileInput = document.getElementById('csvFileInput') as HTMLInputElement;
            if (fileInput) {
              fileInput.value = '';
            }
          } else {
            this.notificationService.showNotification({
              type: 'error',
              message: response.message || 'Failed to upload holidays'
            });
          }
        },
        error: (error) => {
          this.isUploading = false;
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
              this.loadHolidays(); // Reload the holidays list
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

  triggerFileInput(): void {
    const fileInput = document.getElementById('csvFileInput') as HTMLInputElement;
    if (fileInput) {
      fileInput.click();
    }
  }

  downloadSampleCSV(): void {
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
