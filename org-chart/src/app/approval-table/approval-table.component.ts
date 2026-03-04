import { Component, OnInit, ViewChild } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { UserService } from '../user.service';
import { NotificationService } from '../shared/notification.service';
import { ViewRequestDialogComponent } from '../view-request-dialog/view-request-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { NotificationCountService } from '../shared/notification-count.service';

@Component({
  selector: 'app-approval-table',
  templateUrl: './approval-table.component.html',
  styleUrls: ['./approval-table.component.css'],
})
export class ApprovalTableComponent implements OnInit {
  dataSource = new MatTableDataSource<any>();
  displayedColumns: string[] = [];
  role: any;
  currentUserLdap: string = '';
  showMyRequestsOnly: boolean = false;
  allRequests: any[] = [];
  isLoadingEmployeeData: boolean = false;

  // Filter properties
  statusFilter: string = '';
  startDate: Date | null = null;
  endDate: Date | null = null;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;
  showFilters: boolean = false;

  constructor(
    private userService: UserService,
    private notificationService: NotificationService,
    private dialog: MatDialog,
    private notificationCountService: NotificationCountService
  ) {}

  ngOnInit() {
    this.role = localStorage.getItem("role");
    this.currentUserLdap = localStorage.getItem("username") || '';

    if (this.role === 'ADMIN_OPS_MANAGER') {
      this.displayedColumns = ['name', 'requestType', 'ldap', 'createdAt', 'status', 'actions'];
    } else if (this.role === 'LEAD' || this.role === 'MANAGER') {
      this.displayedColumns = ['name', 'requestType', 'ldap', 'createdAt', 'status', 'actions'];
    }

    this.fetchLeadsRequests();
  }

  fetchLeadsRequests() {
    // Create filter parameters
    const filters: any = {};

    if (this.statusFilter) {
      filters.status = this.statusFilter;
    }

    if (this.startDate) {
      filters.startDate = this.formatDate(this.startDate);
    }

    if (this.endDate) {
      filters.endDate = this.formatDate(this.endDate);
    }

    this.userService.getAllLeadsRequests(filters).subscribe({
      next: (response) => {
        console.log('Raw data from backend:', response.data);
        this.allRequests = response.data.map(request => ({
          name: request.requestedBy || request.name,
          requestType: request.requestType,
          ldap: request.ldap,
          createdAt: new Date(request.requestedAt || request.createdAt).toLocaleString(),
          status: request.status,
          employeeDataKey: request.employeeDataKey, // Store the key instead of data
          id: request.id
        }));

        this.filterRequests();
        console.log('Transformed data:', this.dataSource.data);
        this.dataSource.paginator = this.paginator;
        this.dataSource.sort = this.sort;
      },
      error: (error) => {
        console.error('Error fetching leads requests:', error);
        const errorMessage = error.error?.message || 'Failed to fetch requests';
        this.notificationService.showError(errorMessage);
      }
    });
  }

  /**
   * Format date to YYYY-MM-DD format
   */
  formatDate(date: Date): string {
    if (!date) return '';
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  /**
   * Apply all filters and reload data
   */
  applyFilters(): void {
    this.fetchLeadsRequests();
  }

  /**
   * Reset all filters to default values
   */
  resetFilters(): void {
    this.statusFilter = '';
    this.startDate = null;
    this.endDate = null;
    this.fetchLeadsRequests();
  }

  filterRequests() {
    if (this.showMyRequestsOnly && this.currentUserLdap) {
      this.dataSource.data = this.allRequests.filter(request => request.ldap === this.currentUserLdap);
    } else {
      this.dataSource.data = [...this.allRequests];
    }
  }

  toggleMyRequestsOnly() {
    this.showMyRequestsOnly = !this.showMyRequestsOnly;
    this.filterRequests();
  }

  applyGlobalFilter(event: Event) {
    const filterValue = (event.target as HTMLInputElement).value.trim().toLowerCase();
    this.dataSource.filter = filterValue;
  }

  applyFilter(event: Event, column: string) {
    const filterValue = (event.target as HTMLInputElement).value.trim().toLowerCase();
    this.dataSource.filterPredicate = (data, filter) => {
      return data[column]?.toString().toLowerCase().includes(filter);
    };
    this.dataSource.filter = filterValue;
  }

  approveRequest(request: any) {
    this.userService.processLeadsRequest(request.id, 'APPROVE').subscribe({
      next: (response) => {
        request.status = 'APPROVED';
        this.fetchLeadsRequests();
        this.notificationService.showNotification({ type: 'success', message: response.message });
        // Update notification counts
        this.notificationCountService.updateCountsAfterAction('approve');
      },
      error: (error) => {
        console.error('Error approving request:', error);
        this.notificationService.showNotification({
          type: 'error',
          message: error.error?.message || 'Failed to approve request'
        });
      }
    });
  }

  rejectRequest(request: any) {
    this.userService.processLeadsRequest(request.id, 'REJECT').subscribe({
      next: (response) => {
        request.status = 'REJECTED';
        this.fetchLeadsRequests();
        this.notificationService.showNotification({ type: 'success', message: response.message });
        // Update notification counts
        this.notificationCountService.updateCountsAfterAction('reject');
      },
      error: (error) => {
        console.error('Error rejecting request:', error);
        this.notificationService.showNotification({
          type: 'error',
          message: error.error?.message || 'Failed to reject request'
        });
      }
    });
  }

  commentOnRequest(request: any) {
    console.log('Adding comment on request:', request);
  }

  viewRequest(request: any): void {
    console.log("View ", request);

    // Show loading state
    this.isLoadingEmployeeData = true;

    // Fetch employee data with original data for comparison
    this.userService.getEmployeeDataWithOriginal(request.id).subscribe({
      next: (response) => {
        this.isLoadingEmployeeData = false;
        this.dialog.open(ViewRequestDialogComponent, {
          width: '800px',
          data: {
            employeeData: response.employeeData, // Current employee data JSON string
            originalData: response.originalData, // Original employee data JSON string (for comparison)
            requestType: response.requestType,
            requestId: request.id
          }
        });
      },
      error: (error) => {
        this.isLoadingEmployeeData = false;
        console.error('Error fetching employee data:', error);
        const errorMessage = error.error?.message || 'Failed to fetch employee data for this request';
        this.notificationService.showError(errorMessage);
      }
    });
  }
}
