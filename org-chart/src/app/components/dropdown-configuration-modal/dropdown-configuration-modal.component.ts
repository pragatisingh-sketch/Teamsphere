import { Component, Inject, OnInit, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatTableDataSource } from '@angular/material/table';
import { DropdownConfigurationService } from '../../services/dropdown-configuration.service';
import { NotificationService } from '../../shared/notification.service';
import { DropdownConfiguration, CreateDropdownConfiguration, UpdateDropdownConfiguration } from '../../model/dropdown-configuration.interface';
import { ConfirmationDialogComponent } from '../../confirm-dialog/confirmation-dialog.component';
import { MatDialog } from '@angular/material/dialog';


export interface DropdownConfigModalData {
 dropdownType: string;
 title: string;
}


@Component({
 selector: 'app-dropdown-configuration-modal',
 templateUrl: './dropdown-configuration-modal.component.html',
 styleUrls: ['./dropdown-configuration-modal.component.css']
})
export class DropdownConfigurationModalComponent implements OnInit, AfterViewInit {
 dropdownForm!: FormGroup;
 dataSource = new MatTableDataSource<DropdownConfiguration>();
 displayedColumns: string[] = ['optionValue', 'isActive', 'sortOrder', 'actions'];
 isLoading = false;
 isEditMode = false;
 editingItem: DropdownConfiguration | null = null;
 hasChanges = false;
 isSubmitting = false;

 @ViewChild('optionValueInput') optionValueInput!: ElementRef<HTMLInputElement>;


 constructor(
   private fb: FormBuilder,
   private dropdownService: DropdownConfigurationService,
   private notificationService: NotificationService,
   public dialogRef: MatDialogRef<DropdownConfigurationModalComponent>,
   private dialog: MatDialog,
   @Inject(MAT_DIALOG_DATA) public data: DropdownConfigModalData
 ) {
   this.initializeForm();
 }


 ngOnInit(): void {
   this.loadDropdownOptions();
 }

 ngAfterViewInit(): void {
   // Prevent auto-focus on the input field when modal opens
   // This ensures the dropdown doesn't automatically open
   setTimeout(() => {
     if (this.optionValueInput && this.optionValueInput.nativeElement) {
       this.optionValueInput.nativeElement.blur();
     }
     // Also blur any other focused elements
     const activeElement = document.activeElement as HTMLElement;
     if (activeElement && activeElement.blur && activeElement !== document.body) {
       activeElement.blur();
     }
   }, 0);
 }


 private initializeForm(): void {
   this.dropdownForm = this.fb.group({
     optionValue: ['', [Validators.required, Validators.maxLength(100)]],
     isActive: [true, Validators.required],
     sortOrder: [0, [Validators.min(0)]]
   });
 }


 private loadDropdownOptions(): void {
   this.isLoading = true;
   this.dropdownService.getAllDropdownOptions(this.data.dropdownType).subscribe({
     next: (options) => {
       this.dataSource.data = options;
       this.isLoading = false;
     },
     error: (error) => {
       console.error('Error loading dropdown options:', error);
       this.notificationService.showNotification({
         type: 'error',
         message: 'Failed to load dropdown options'
       });
       this.isLoading = false;
     }
   });
 }


 onSubmit(): void {
   if (this.dropdownForm.valid && !this.isSubmitting) {
     this.isSubmitting = true;
     const formData = this.dropdownForm.value;

     if (this.isEditMode && this.editingItem) {
       this.updateDropdownOption(formData);
     } else {
       this.createDropdownOption(formData);
     }
   }
 }


 private createDropdownOption(formData: any): void {
   // Validate form data before processing
   if (!formData.optionValue || typeof formData.optionValue !== 'string') {
     this.notificationService.showNotification({
       type: 'error',
       message: 'Option value is required and must be a valid string'
     });
     return;
   }

   const createData: CreateDropdownConfiguration = {
     dropdownType: this.data.dropdownType,
     optionValue: formData.optionValue.trim(),
     displayName: formData.optionValue.trim(), // Use optionValue as displayName
     isActive: formData.isActive,
     sortOrder: formData.sortOrder || 0
   };


   this.dropdownService.createDropdownOption(createData).subscribe({
     next: () => {
       this.notificationService.showNotification({
         type: 'success',
         message: 'Dropdown option created successfully'
       });
       this.hasChanges = true;
       this.resetForm();
       this.loadDropdownOptions();
       this.isSubmitting = false;
     },
     error: (error) => {
       console.error('Error creating dropdown option:', error);

       let errorMessage = 'Failed to create dropdown option';

       if (error.status === 403) {
         errorMessage = 'Access denied. You need ADMIN_OPS_MANAGER role to manage dropdown configurations.';
       } else if (error.status === 401) {
         errorMessage = 'Authentication failed. Please log in again.';
       } else if (error.error?.message) {
         errorMessage = error.error.message;
       }

       this.notificationService.showNotification({
         type: 'error',
         message: errorMessage
       });
       this.isSubmitting = false;
     }
   });
 }


 private updateDropdownOption(formData: any): void {
   if (!this.editingItem) return;

   // Validate form data before processing
   if (!formData.optionValue || typeof formData.optionValue !== 'string') {
     this.notificationService.showNotification({
       type: 'error',
       message: 'Option value is required and must be a valid string'
     });
     return;
   }

   const updateData: UpdateDropdownConfiguration = {
     optionValue: formData.optionValue.trim(),
     displayName: formData.optionValue.trim(), // Use optionValue as displayName
     isActive: formData.isActive,
     sortOrder: formData.sortOrder
   };


   this.dropdownService.updateDropdownOption(this.editingItem.id, updateData).subscribe({
     next: () => {
       this.notificationService.showNotification({
         type: 'success',
         message: 'Dropdown option updated successfully'
       });
       this.hasChanges = true;
       this.resetForm();
       this.loadDropdownOptions();
       this.isSubmitting = false;
     },
     error: (error) => {
       console.error('Error updating dropdown option:', error);
       this.notificationService.showNotification({
         type: 'error',
         message: error.error?.message || 'Failed to update dropdown option'
       });
       this.isSubmitting = false;
     }
   });
 }


 editItem(item: DropdownConfiguration): void {
   this.isEditMode = true;
   this.editingItem = item;
   this.dropdownForm.patchValue({
     optionValue: item.optionValue,
     isActive: item.isActive,
     sortOrder: item.sortOrder
   });
   // Keep optionValue field enabled in edit mode to allow editing
   this.dropdownForm.get('optionValue')?.enable();
 }


 deleteItem(item: DropdownConfiguration): void {
  // Open confirmation dialog before deleting
  const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
    width: '400px',
    data: {
      title: 'Confirm Deletion',
      message: `Are you sure you want to delete "${item.optionValue}"? This action cannot be undone.`,
      color: 'warn',
      confirmButtonText: 'Delete'
    }
  });

  dialogRef.afterClosed().subscribe(result => {
    if (result) {
      // User confirmed deletion, proceed with delete operation
      this.dropdownService.deleteDropdownOption(item.id).subscribe({
        next: () => {
          this.notificationService.showNotification({
            type: 'success',
            message: 'Dropdown option deleted successfully'
          });
          this.hasChanges = true;
          this.loadDropdownOptions();
        },
        error: (error) => {
          console.error('Error deleting dropdown option:', error);
          if (error.status === 409) {
            this.notificationService.showNotification({
              type: 'error',
              message: error.error?.message || 
                       'A request has already been made for this dropdown. Please contact Admin Ops Manager for details.',
            });
          } else {
            this.notificationService.showNotification({
              type: 'error',
              message: error.error?.message || 'Failed to delete dropdown option'
            });
          }
        }
      });
    }
    // If result is false, user cancelled the deletion, so do nothing
  });
}



 resetForm(): void {
   this.isEditMode = false;
   this.editingItem = null;
   this.dropdownForm.reset({
     optionValue: '',
     isActive: true,
     sortOrder: 0
   });
   this.dropdownForm.get('optionValue')?.enable();
 }


 onCancel(): void {
   this.resetForm();
 }


 onClose(): void {
   this.dialogRef.close(this.hasChanges);
 }


 getStatusText(isActive: boolean): string {
   return isActive ? 'Active' : 'Inactive';
 }


 getStatusClass(isActive: boolean): string {
   return isActive ? 'status-active' : 'status-inactive';
 }



}





