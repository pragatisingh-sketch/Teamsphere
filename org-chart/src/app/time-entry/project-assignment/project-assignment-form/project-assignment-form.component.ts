import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, FormControl } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { HttpClient } from '@angular/common/http';
import { NotificationService } from '../../../shared/notification.service';
import { environment } from '../../../../environments/environment';
import { Observable } from 'rxjs';
import { map, startWith } from 'rxjs/operators';

// Interface for the backend BaseResponse structure
interface BaseResponse<T> {
  status: string;
  code: number;
  message: string;
  data: T;
}

interface User {
  id: number;
  ldap: string;
  firstName: string;
  lastName: string;
  level: string;
  email: string;
}

interface Project {
  id: number;
  projectName: string;
  projectCode: string;
}

interface ProjectAssignmentPayload {
  assignedDate: Date;
  role: string;
  status: string;
  userId?: number;
  projectId?: number;
  projectIds?: number[];
  userIds?: number[];
}

@Component({
  selector: 'app-project-assignment-form',
  templateUrl: './project-assignment-form.component.html',
  styleUrls: ['./project-assignment-form.component.css']
})
export class ProjectAssignmentFormComponent implements OnInit {
  assignmentForm!: FormGroup;
  isEditMode = false;
  baseUrl = environment.apiUrl;
  users: User[] = [];
  projects: Project[] = [];
  filteredUsers!: Observable<User[]>;
  filteredProjects!: Observable<Project[]>;
  userSearchControl = new FormControl('');
  projectSearchControl = new FormControl('');
  statusOptions = ['ACTIVE', 'COMPLETED', 'ON_HOLD'];
  roleOptions = ['DEVELOPER', 'TESTER', 'DESIGNER', 'ANALYST', 'MANAGER', 'OTHER'];
  assignmentType: 'user-to-projects' | 'project-to-users' = 'user-to-projects';

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private notificationService: NotificationService,
    public dialogRef: MatDialogRef<ProjectAssignmentFormComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    this.isEditMode = data?.isEditMode || false;
    this.assignmentType = data?.assignmentType || 'user-to-projects';
    this.initializeForm();
  }

  ngOnInit(): void {
    this.loadUsers();
    this.loadProjects();
    this.setupSearchFilters();
  }

  private initializeForm(): void {
    this.assignmentForm = this.fb.group({
      userId: ['', Validators.required],
      projectId: ['', Validators.required],
      projectIds: [[], Validators.required],
      userIds: [[], Validators.required],
      assignedDate: [new Date(), Validators.required],
      status: ['ACTIVE', Validators.required]
    });

    if (this.isEditMode && this.data.assignment) {
      this.assignmentForm.patchValue({
        userId: this.data.assignment.userId,
        projectId: this.data.assignment.projectId,
        projectIds: this.data.assignment.projectIds || [],
        userIds: this.data.assignment.userIds || [],
        assignedDate: new Date(this.data.assignment.assignedDate),
        status: this.data.assignment.status
      });
    }

    // Set initial validation based on assignment type
    this.updateFormValidation(this.assignmentType);
  }

  updateFormValidation(type: 'user-to-projects' | 'project-to-users'): void {
    const userIdControl = this.assignmentForm.get('userId');
    const projectIdControl = this.assignmentForm.get('projectId');
    const projectIdsControl = this.assignmentForm.get('projectIds');
    const userIdsControl = this.assignmentForm.get('userIds');

    if (type === 'user-to-projects') {
      userIdControl?.setValidators([Validators.required]);
      projectIdsControl?.setValidators([Validators.required, Validators.minLength(1)]);
      projectIdControl?.clearValidators();
      userIdsControl?.clearValidators();
    } else {
      projectIdControl?.setValidators([Validators.required]);
      userIdsControl?.setValidators([Validators.required, Validators.minLength(1)]);
      userIdControl?.clearValidators();
      projectIdsControl?.clearValidators();
    }

    // Update form validity
    userIdControl?.updateValueAndValidity();
    projectIdControl?.updateValueAndValidity();
    projectIdsControl?.updateValueAndValidity();
    userIdsControl?.updateValueAndValidity();
  }

  private setupSearchFilters(): void {
    // Set up user search filter
    this.filteredUsers = this.userSearchControl.valueChanges.pipe(
      startWith(''),
      map(value => this.filterUsers(value || ''))
    );

    // Set up project search filter
    this.filteredProjects = this.projectSearchControl.valueChanges.pipe(
      startWith(''),
      map(value => {
        console.log('Project search value changed:', value);
        return this.filterProjects(value || '');
      })
    );
  }

  private filterUsers(value: string): User[] {
    const filterValue = value.toLowerCase();
    const selectedUserIds = this.assignmentForm.get('userIds')?.value || [];

    return this.users.filter(user => {
      const matchesSearch =
        user.firstName.toLowerCase().includes(filterValue) ||
        user.lastName.toLowerCase().includes(filterValue) ||
        user.ldap.toLowerCase().includes(filterValue);

      // Include selected users even if they don't match the search
      return matchesSearch || selectedUserIds.includes(user.id);
    });
  }

  private filterProjects(value: string): Project[] {
    const filterValue = value.toLowerCase();
    const selectedProjectIds = this.assignmentForm.get('projectIds')?.value || [];

    console.log('Filtering projects with:', {
      filterValue,
      selectedProjectIds,
      totalProjects: this.projects.length
    });

    const filtered = this.projects.filter(project => {
      const matchesSearch =
        project.projectName.toLowerCase().includes(filterValue) ||
        project.projectCode.toLowerCase().includes(filterValue);

      // Include selected projects even if they don't match the search
      return matchesSearch || selectedProjectIds.includes(project.id);
    });

    console.log('Filtered projects result:', filtered);
    return filtered;
  }

  private loadUsers(): void {
    this.http.get<BaseResponse<User[]>>(`${this.baseUrl}/api/employees/my-team`).subscribe({
      next: (response) => {
        if (response.status === 'success') {
          this.users = response.data;
        } else {
          this.notificationService.showNotification({
            type: 'error',
            message: response.message || 'Failed to load team members'
          });
        }
      },
      error: (error) => {
        console.error('Error loading users:', error);
        // Try to extract message from backend error response
        let errorMessage = 'Failed to load team members';
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

  private loadProjects(): void {
    this.http.get<BaseResponse<Project[]>>(`${this.baseUrl}/api/projects`).subscribe({
      next: (response) => {
        if (response.status === 'success') {
          console.log('Projects loaded successfully:', response.data);
          this.projects = response.data;
          // Initialize filtered projects with all projects
          this.filteredProjects = this.projectSearchControl.valueChanges.pipe(
            startWith(''),
            map(value => {
              console.log('Filtering projects with value:', value);
              const filtered = this.filterProjects(value || '');
              console.log('Filtered projects:', filtered);
              return filtered;
            })
          );
        } else {
          this.notificationService.showNotification({
            type: 'error',
            message: response.message || 'Failed to load projects'
          });
        }
      },
      error: (error) => {
        console.error('Error loading projects:', error);
        // Try to extract message from backend error response
        let errorMessage = 'Failed to load projects';
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

  selectAllProjects(): void {
    const currentProjectIds = this.assignmentForm.get('projectIds')?.value || [];
    const allProjectIds = this.projects.map(project => project.id);

    // If all projects are selected, deselect all
    if (currentProjectIds.length === allProjectIds.length) {
      this.assignmentForm.patchValue({
        projectIds: []
      });
    } else {
      // Otherwise, select all projects
      this.assignmentForm.patchValue({
        projectIds: allProjectIds
      });
    }
  }

  selectAllUsers(): void {
    const currentUserIds = this.assignmentForm.get('userIds')?.value || [];
    const allUserIds = this.users.map(user => user.id);

    // If all users are selected, deselect all
    if (currentUserIds.length === allUserIds.length) {
      this.assignmentForm.patchValue({
        userIds: []
      });
    } else {
      // Otherwise, select all users
      this.assignmentForm.patchValue({
        userIds: allUserIds
      });
    }
  }

  onSubmit(): void {
    if (this.assignmentForm.valid) {
      const formData = this.assignmentForm.value;

      if (this.assignmentType === 'user-to-projects') {
        // Assign multiple projects to a single user
        const url = `${this.baseUrl}/api/projects/user/${formData.userId}/assign-projects`;
        const payload = {
          userId: formData.userId,
          projectIds: formData.projectIds,
          assignedDate: formData.assignedDate,
          status: formData.status
        };

        this.http.post<BaseResponse<any>>(url, payload).subscribe({
          next: (response) => {
            if (response.status === 'success') {
              this.notificationService.showNotification({
                type: 'success',
                message: response.message || 'Projects assigned successfully'
              });
              this.dialogRef.close(true);
            } else {
              this.notificationService.showNotification({
                type: 'error',
                message: response.message || 'Failed to assign projects'
              });
            }
          },
          error: (error) => {
            console.error('Error assigning projects:', error);
            // Try to extract message from backend error response
            let errorMessage = 'Failed to assign projects';
            if (error.error && error.error.message) {
              errorMessage = error.error.message;
            }

            this.notificationService.showNotification({
              type: 'error',
              message: errorMessage
            });
          }
        });
      } else {
        // Assign multiple users to a single project
        const url = `${this.baseUrl}/api/projects/${formData.projectId}/assign`;
        const payload = {
          userIds: formData.userIds,
          projectId: formData.projectId,
          assignedDate: formData.assignedDate,
          status: formData.status
        };

        this.http.post<BaseResponse<any>>(url, payload).subscribe({
          next: (response) => {
            if (response.status === 'success') {
              this.notificationService.showNotification({
                type: 'success',
                message: response.message || 'Users assigned successfully'
              });
              this.dialogRef.close(true);
            } else {
              this.notificationService.showNotification({
                type: 'error',
                message: response.message || 'Failed to assign users'
              });
            }
          },
          error: (error) => {
            console.error('Error assigning users:', error);
            // Try to extract message from backend error response
            let errorMessage = 'Failed to assign users';
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
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}
