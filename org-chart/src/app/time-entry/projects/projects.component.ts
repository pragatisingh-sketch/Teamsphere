import { Component, OnInit, ViewChild } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatDialog } from '@angular/material/dialog';
import { HttpClient } from '@angular/common/http';
import { NotificationService } from '../../shared/notification.service';
import { ConfirmationDialogComponent } from '../../confirm-dialog/confirmation-dialog.component';
import { ProjectFormComponent } from './project-form/project-form.component';
import { environment } from '../../../environments/environment';

// Interface for the backend BaseResponse structure
interface BaseResponse<T> {
  status: string;
  code: number;
  message: string;
  data: T;
}

// Updated interface to match the API response format
export interface Project {
  id: number;
  userId?: number;
  username?: string;
  projectId?: number;
  projectCode?: string;
  code?: string;        // For backward compatibility
  projectName?: string;
  name?: string;        // For backward compatibility
  description?: string;
  startDate?: string;
  endDate?: string;
  status?: string;
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
  isOvertimeEligible?: boolean;
}

@Component({
  selector: 'app-projects',
  templateUrl: './projects.component.html',
  styleUrls: ['./projects.component.css']
})
export class ProjectsComponent implements OnInit {
  baseUrl = environment.apiUrl;
  dataSource = new MatTableDataSource<Project>([]);
  displayedColumns: string[] = ['name', 'code', 'description', 'status', 'overtimeEligible', 'actions'];
  totalRecords = 0;
  showFilters = false;
  filterValues: any = {};
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
    console.log('User role in projects component:', this.userRole);
    this.loadProjects();

    this.dataSource.filterPredicate = (data: Project, filter: string) => {
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

  loadProjects(): void {
    // Use the my-projects endpoint for regular users, and the projects endpoint for managers/admins
    const endpoint = this.isLeadOrManager() ? 'api/projects' : 'api/projects/my-projects';

    this.http.get<BaseResponse<Project[]>>(`${this.baseUrl}/${endpoint}`)
      .subscribe({
        next: (response) => {
          console.log('Received projects response:', response);
          if (response.status === 'success') {
            // Map the API response to match the UI expectations
            const mappedData = response.data.map(project => this.mapProject(project));
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
              message: response.message || 'Failed to load projects'
            });
          }
        },
        error: (error) => {
          console.error('Error fetching projects:', error);
          // Try to extract message from backend error response
          let errorMessage = 'Failed to load projects. Please try again.';
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
  mapProject(project: Project): Project {
    return {
      ...project,
      id: project.id || project.projectId || 0,
      name: project.name || project.projectName || '',
      code: project.code || project.projectCode || ''
    };
  }

  // Add sample data for testing
  addSampleData(): void {
    const sampleData: Project[] = [
      {
        id: 1,
        userId: 1606,
        username: "akhilbhatnagar",
        projectId: 1,
        projectCode: "PROJ001",
        projectName: "Customer Portal Development",
        description: "Development of customer-facing portal with authentication and dashboard",
        status: "ACTIVE",
        startDate: "2025-01-01",
        endDate: "2025-12-31"
      },
      {
        id: 2,
        userId: 1606,
        username: "akhilbhatnagar",
        projectId: 2,
        projectCode: "PROJ002",
        projectName: "Internal Admin Dashboard",
        description: "Admin dashboard for internal operations",
        status: "ACTIVE",
        startDate: "2025-02-15",
        endDate: "2025-08-30"
      }
    ];

    // Map the sample data
    const mappedData = sampleData.map(project => this.mapProject(project));
    this.dataSource.data = mappedData;
    this.totalRecords = mappedData.length;
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
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();
    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  openAddProjectForm(): void {
    const dialogRef = this.dialog.open(ProjectFormComponent, {
      width: '800px',
      data: { isEditMode: false }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadProjects();
      }
    });
  }

  editProject(project: Project): void {
    const dialogRef = this.dialog.open(ProjectFormComponent, {
      width: '800px',
      data: { isEditMode: true, project: project }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadProjects();
      }
    });
  }

  deleteProject(project: Project): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: { title: 'Confirm Delete', message: 'Are you sure you want to delete this project?' }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.http.delete<BaseResponse<any>>(`${this.baseUrl}/api/projects/${project.id || project.projectId}`)
          .subscribe({
            next: (response) => {
              if (response.status === 'success') {
                this.notificationService.showNotification({
                  type: 'success',
                  message: response.message || 'Project deleted successfully!'
                });
                this.loadProjects();
              } else {
                this.notificationService.showNotification({
                  type: 'error',
                  message: response.message || 'Failed to delete project'
                });
              }
            },
            error: (error) => {
              console.error('Error deleting project:', error);
              // Try to extract message from backend error response
              let errorMessage = 'Failed to delete project. Please try again.';
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
    console.log('User role in projects isLeadOrManager:', this.userRole);
    // Only ADMIN_OPS_MANAGER should have full access to projects
    return this.userRole === 'ADMIN_OPS_MANAGER' || this.userRole === 'MANAGER' || this.userRole === 'LEAD';
  }
}
