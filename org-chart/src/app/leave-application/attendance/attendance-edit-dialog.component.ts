import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

export interface AttendanceEditDialogData {
  attendanceId: number;
  currentStatus?: string;
  currentCompliance?: boolean;
  editType: 'status' | 'compliance';
  employeeName: string;
  date: string;
}

@Component({
  selector: 'app-attendance-edit-dialog',
  template: `
    <h2 mat-dialog-title>
      {{ data.editType === 'status' ? 'Edit Attendance Status' : 'Edit Compliance Status' }}
    </h2>
    
    <mat-dialog-content>
      <div class="employee-info">
        <p><strong>Employee:</strong> {{ data.employeeName }}</p>
        <p><strong>Date:</strong> {{ data.date }}</p>
      </div>

      <form [formGroup]="editForm">
        <!-- Status Edit -->
        <mat-form-field *ngIf="data.editType === 'status'" appearance="outline" class="full-width">
          <mat-label>Attendance Status</mat-label>
          <mat-select formControlName="newStatus" required>
            <mat-option value="OnTime-CheckIn">OnTime CheckIn</mat-option>
            <mat-option value="Late-CheckIn">Late CheckIn</mat-option>
            <mat-option value="Early-CheckIn">Early CheckIn</mat-option>
            <mat-option value="OnTime Checkout">OnTime Checkout</mat-option>
            <mat-option value="Early-Checkout">Early Checkout</mat-option>
            <mat-option value="Late-Checkout">Late Checkout</mat-option>
            <mat-option value="OnTime-CheckIn, OnTime Checkout">OnTime CheckIn, OnTime Checkout</mat-option>
            <mat-option value="Late-CheckIn, OnTime Checkout">Late CheckIn, OnTime Checkout</mat-option>
            <mat-option value="OnTime-CheckIn, Late-Checkout">OnTime CheckIn, Late Checkout</mat-option>
            <mat-option value="Late-CheckIn, Late-Checkout">Late CheckIn, Late Checkout</mat-option>
          </mat-select>
          <mat-hint>Current: {{ data.currentStatus || 'Not set' }}</mat-hint>
        </mat-form-field>

        <!-- Compliance Edit -->
        <div *ngIf="data.editType === 'compliance'" class="compliance-section">
          <p class="current-value">Current Compliance: 
            <span [class.compliant]="!data.currentCompliance" [class.non-compliant]="data.currentCompliance">
              {{ data.currentCompliance ? 'Non-Compliant' : 'Compliant' }}
            </span>
          </p>
          <mat-radio-group formControlName="isDefaulter" required>
            <mat-radio-button [value]="false">Compliant</mat-radio-button>
            <mat-radio-button [value]="true">Non-Compliant</mat-radio-button>
          </mat-radio-group>
        </div>

        <!-- Reason (Required for both) -->
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Reason for Change *</mat-label>
          <textarea 
            matInput 
            formControlName="reason" 
            rows="4" 
            placeholder="Please provide a detailed reason for this modification"
            required
          ></textarea>
          <mat-error *ngIf="editForm.get('reason')?.hasError('required')">
            Reason is required
          </mat-error>
          <mat-error *ngIf="editForm.get('reason')?.hasError('minlength')">
            Reason must be at least 10 characters
          </mat-error>
        </mat-form-field>
      </form>
    </mat-dialog-content>

    <mat-dialog-actions class="dialog-actions-end">
      <button mat-button (click)="onCancel()">Cancel</button>
      <button 
        mat-raised-button 
        color="primary" 
        (click)="onSubmit()" 
        [disabled]="!editForm.valid"
      >
        Update
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .employee-info {
      background-color: #f5f5f5;
      padding: 12px;
      border-radius: 4px;
      margin-bottom: 20px;
    }

    .employee-info p {
      margin: 4px 0;
      font-size: 14px;
    }

    .full-width {
      width: 100%;
      margin-bottom: 16px;
    }

    .compliance-section {
      margin-bottom: 20px;
    }

    .current-value {
      margin-bottom: 12px;
      font-size: 14px;
    }

    .compliant {
      color: #4caf50;
      font-weight: 500;
    }

    .non-compliant {
      color: #f44336;
      font-weight: 500;
    }

    mat-radio-button {
      margin-right: 16px;
    }

    mat-dialog-content {
      min-width: 400px;
      max-width: 500px;
    }

    .dialog-actions-end {
      justify-content: flex-end;
      padding: 16px 24px;
    }
  `]
})
export class AttendanceEditDialogComponent {
  editForm: FormGroup;

  constructor(
    public dialogRef: MatDialogRef<AttendanceEditDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AttendanceEditDialogData,
    private fb: FormBuilder
  ) {
    this.editForm = this.fb.group({
      newStatus: [data.currentStatus || ''],
      isDefaulter: [data.currentCompliance !== undefined ? data.currentCompliance : null],
      reason: ['', [Validators.required, Validators.minLength(10)]]
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSubmit(): void {
    if (this.editForm.valid) {
      const result = {
        newStatus: this.editForm.value.newStatus,
        isDefaulter: this.editForm.value.isDefaulter,
        reason: this.editForm.value.reason
      };
      this.dialogRef.close(result);
    }
  }
}
