import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { HttpClient } from '@angular/common/http';
import { NotificationService } from '../../../shared/notification.service';
import { environment } from '../../../../environments/environment';

// Interface for the backend BaseResponse structure
interface BaseResponse<T> {
  status: string;
  code: number;
  message: string;
  data: T;
}

@Component({
  selector: 'app-project-form',
  templateUrl: './project-form.component.html',
  styleUrls: ['./project-form.component.css']
})
export class ProjectFormComponent implements OnInit {
  projectForm: FormGroup;
  isEditMode = false;
  baseUrl = environment.apiUrl;
  statusOptions = ['ACTIVE', 'COMPLETED', 'ON_HOLD'];

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private notificationService: NotificationService,
    public dialogRef: MatDialogRef<ProjectFormComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    this.isEditMode = data?.isEditMode || false;

    this.projectForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(100)]],
      code: ['', [Validators.required, Validators.maxLength(20), Validators.pattern('^[A-Z0-9-_]+$')]],
      description: ['', [Validators.required, Validators.maxLength(500)]],
      startDate: [new Date(), Validators.required],
      endDate: [null],
      status: ['ACTIVE', Validators.required],
      isOvertimeEligible: [false]
    });

    if (this.isEditMode && data.project) {
      this.projectForm.patchValue({
        name: data.project.name,
        code: data.project.code,
        description: data.project.description,
        startDate: new Date(data.project.startDate),
        endDate: data.project.endDate ? new Date(data.project.endDate) : null,
        status: data.project.status,
        isOvertimeEligible: data.project.isOvertimeEligible || false
      });

      // Disable code field in edit mode as it's usually not changed once set
      this.projectForm.get('code')?.disable();
    }
  }

  ngOnInit(): void {
    // Check if user has admin permissions
    if (!this.isAdminOpsManager()) {
      this.notificationService.showNotification({
        type: 'error',
        message: 'You do not have permission to add or edit projects.'
      });
      this.dialogRef.close(false);
    }
  }

  isAdminOpsManager(): boolean {
    const userRole = localStorage.getItem('role');
    return userRole === 'ADMIN_OPS_MANAGER';
  }

  formatDate(date: Date | null): string | null {
    if (!date) return null;
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  onSubmit(): void {
    // Double-check admin permissions before submitting
    if (!this.isAdminOpsManager()) {
      this.notificationService.showNotification({
        type: 'error',
        message: 'You do not have permission to add or edit projects.'
      });
      this.dialogRef.close(false);
      return;
    }

    if (this.projectForm.invalid) {
      console.log('Form is invalid:', this.projectForm.errors);
      return;
    }

    const formData = { ...this.projectForm.value };

    // Format dates for API
    formData.startDate = this.formatDate(formData.startDate);
    formData.endDate = this.formatDate(formData.endDate);

    // If in edit mode, we need to include the code even though it's disabled in the form
    if (this.isEditMode) {
      formData.code = this.data.project.code;
    }

    const createProjectDTO = {
      projectName: formData.name,
      projectCode: formData.code,
      description: formData.description,
      startDate: formData.startDate,
      endDate: formData.endDate,
      status: formData.status,
      isOvertimeEligible: formData.isOvertimeEligible
    };

    console.log('Submitting project data:', createProjectDTO);

    if (this.isEditMode) {
      this.http.put<BaseResponse<any>>(`${this.baseUrl}/api/projects/${this.data.project.id}`, createProjectDTO)
        .subscribe({
          next: (response) => {
            if (response.status === 'success') {
              this.notificationService.showNotification({
                type: 'success',
                message: response.message || 'Project updated successfully!'
              });
              this.dialogRef.close(true);
            } else {
              this.notificationService.showNotification({
                type: 'error',
                message: response.message || 'Failed to update project'
              });
            }
          },
          error: (error) => {
            console.error('Error updating project:', error);
            // Try to extract message from backend error response
            let errorMessage = 'Failed to update project. Please try again.';
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
      this.http.post<BaseResponse<any>>(`${this.baseUrl}/api/projects`, createProjectDTO)
        .subscribe({
          next: (response) => {
            if (response.status === 'success') {
              this.notificationService.showNotification({
                type: 'success',
                message: response.message || 'Project created successfully!'
              });
              this.dialogRef.close(true);
            } else {
              this.notificationService.showNotification({
                type: 'error',
                message: response.message || 'Failed to create project'
              });
            }
          },
          error: (error) => {
            console.error('Error creating project:', error);
            // Try to extract message from backend error response
            let errorMessage = 'Failed to create project. Please try again.';
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

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
