import { Component, OnInit, ViewChild } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatDialog } from '@angular/material/dialog';
import { HttpClient } from '@angular/common/http';
import { NotificationService } from '../../shared/notification.service';
import { environment } from '../../../environments/environment';
import { ProjectAssignmentFormComponent } from './project-assignment-form/project-assignment-form.component';
import { ConfirmationDialogComponent } from '../../confirm-dialog/confirmation-dialog.component';
import { MatSelectChange } from '@angular/material/select';

// Interface for the backend BaseResponse structure
interface BaseResponse<T> {
  status: string;
  code: number;
  message: string;
  data: T;
}

export interface ProjectAssignment {
  id: number;
  userId?: number;
  userName?: string;
  username?: string;
  projectId?: number;
  projectName?: string;
  projectCode?: string;
  assignedDate?: string;
  status?: string;
  ldap?: string;
  leadId?: number;
  leadUsername?: string;
}

@Component({
  selector: 'app-project-assignment',
  templateUrl: './project-assignment.component.html',
  styleUrls: ['./project-assignment.component.css']
})
export class ProjectAssignmentComponent implements OnInit {
  baseUrl = environment.apiUrl;
  dataSource = new MatTableDataSource<ProjectAssignment>([]);
  displayedColumns: string[] = ['userName', 'projectName', 'assignedDate', 'status', 'actions'];
  totalRecords = 0;
  showFilters = false;
  filterValues: any = {};
  assignmentStatuses = ['ACTIVE', 'COMPLETED', 'ON_HOLD'];
  userRole: string | undefined;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private http: HttpClient,
    private dialog: MatDialog,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.userRole = localStorage.getItem('role') || undefined;
    console.log('User role in project assignment component:', this.userRole);
    this.loadProjectAssignments();

    this.dataSource.filterPredicate = (data: ProjectAssignment, filter: string) => {
      return Object.values(data)
        .some(value => value?.toString().toLowerCase().includes(filter.toLowerCase()));
    };
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  loadProjectAssignments(): void {
    this.http.get<BaseResponse<ProjectAssignment[]>>(`${this.baseUrl}/api/projects/team-assignments`)
      .subscribe({
        next: (response) => {
          console.log('Received project assignments response:', response);
          if (response.status === 'success') {
            // Map the API response to match the UI expectations
            const mappedData = response.data.map(assignment => this.mapProjectAssignment(assignment));
            this.dataSource.data = mappedData;
            this.totalRecords = mappedData.length;

            // Show success message from backend if needed
            if (response.message) {
              this.notificationService.showNotification({
                type: 'success',
                message: response.message
              });
            }
          } else {
            // Handle error response from backend
            this.notificationService.showNotification({
              type: 'error',
              message: response.message || 'Failed to load project assignments'
            });
          }
        },
        error: (error) => {
          console.error('Error fetching project assignments:', error);
          // Try to extract message from backend error response
          let errorMessage = 'Failed to load project assignments. Please try again.';
          if (error.error && error.error.message) {
            errorMessage = error.error.message;
          }

          this.notificationService.showNotification({
            type: 'error',
            message: errorMessage
          });

          // For testing - add sample data if API fails
          if (error.status === 0) {
            this.addSampleData();
          }
        }
      });
  }

  // Map API response to UI format
  mapProjectAssignment(assignment: ProjectAssignment): ProjectAssignment {
    return {
      ...assignment,
      userName: assignment.username || assignment.userName || assignment.ldap || ''
    };
  }

  // Add sample data for testing
  addSampleData(): void {
    const sampleData: ProjectAssignment[] = [
      {
        id: 1,
        userId: 1606,
        username: "akhilbhatnagar",
        projectId: 1,
        projectName: "Customer Portal Development",
        projectCode: "PROJ001",
        assignedDate: "2025-01-15",
        status: "ACTIVE"
      },
      {
        id: 2,
        userId: 1607,
        username: "johndoe",
        projectId: 1,
        projectName: "Customer Portal Development",
        projectCode: "PROJ001",
        assignedDate: "2025-01-20",
        status: "ACTIVE"
      }
    ];

    // Map the sample data
    const mappedData = sampleData.map(assignment => this.mapProjectAssignment(assignment));
    this.dataSource.data = mappedData;
    this.totalRecords = mappedData.length;
  }

  applyFilter(event: Event | MatSelectChange, column: string): void {
    let filterValue: string;

    if (event instanceof MatSelectChange) {
      filterValue = event.value;
    } else {
      filterValue = (event.target as HTMLInputElement).value;
    }

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
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();
    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  openAddAssignmentForm(): void {
    const dialogRef = this.dialog.open(ProjectAssignmentFormComponent, {
      width: '800px',
      data: { isEditMode: false }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadProjectAssignments();
      }
    });
  }

  editAssignment(assignment: ProjectAssignment): void {
    const dialogRef = this.dialog.open(ProjectAssignmentFormComponent, {
      width: '800px',
      data: { isEditMode: true, assignment: assignment }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadProjectAssignments();
      }
    });
  }

  removeAssignment(assignment: ProjectAssignment): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Confirm Removal',
        message: 'Are you sure you want to remove this project assignment?',
        showCancel: false,
        confirmText: 'OK',
        showConfirm: true
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.http.delete<BaseResponse<any>>(`${this.baseUrl}/api/projects/${assignment.projectId}/users/${assignment.userId}`)
          .subscribe({
            next: (response) => {
              if (response.status === 'success') {
                this.notificationService.showNotification({
                  type: 'success',
                  message: response.message || 'Project assignment removed successfully!'
                });
                this.loadProjectAssignments();
              } else {
                this.notificationService.showNotification({
                  type: 'error',
                  message: response.message || 'Failed to remove project assignment'
                });
              }
            },
            error: (error) => {
              console.error('Error removing project assignment:', error);
              // Try to extract message from backend error response
              let errorMessage = 'Failed to remove project assignment. Please try again.';
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

  isLeadOrManager(): boolean {
    console.log('User role in project assignment isLeadOrManager:', this.userRole);
    // For testing, temporarily return true to show all buttons
//    Uncomment the line below when testing is complete
    return this.userRole === 'LEAD' || this.userRole === 'MANAGER' || this.userRole === 'ADMIN_OPS_MANAGER';
  }
}
