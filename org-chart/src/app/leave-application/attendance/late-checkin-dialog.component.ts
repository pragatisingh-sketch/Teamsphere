import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

@Component({
  selector: 'app-late-checkin-dialog',
  template: `
    <h1 mat-dialog-title>{{ messageTitle }}</h1>

    <div mat-dialog-content>
      <p>{{ message }}</p>

      <!-- Reason selection -->
      <mat-radio-group [(ngModel)]="selectedReason">
        <mat-radio-button *ngFor="let reason of reasons" [value]="reason">
          {{ reason }}
        </mat-radio-button>
      </mat-radio-group>

      <!-- Info message shown dynamically for reasons requiring justification -->
      <ng-container *ngIf="selectedReason === 'Different Shift (Mismatch)' || selectedReason === 'Other'">
        <p class="justification-hint">
          <mat-icon class="info-icon">info</mat-icon>
          Please provide a suitable justification for the selected Reason</p>
      </ng-container>

      <!-- Notes box -->
      <mat-form-field appearance="outline" class="notes-field">
        <mat-label>Justification</mat-label>
        <textarea
          matInput
          [(ngModel)]="notes"
          [placeholder]="getPlaceholderText()"
        ></textarea>

        <!-- Error for reasons requiring justification -->
        <mat-error *ngIf="(selectedReason === 'Different Shift (Mismatch)' || selectedReason === 'Other') && !notes.trim()">
          Justification is required for {{ selectedReason }}.
        </mat-error>
      </mat-form-field>
    </div>

    <div mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Cancel</button>
      <button
        mat-raised-button
        color="primary"
        (click)="onSubmit()"
        [disabled]="isSubmitDisabled()"
      >
        Submit
      </button>
    </div>
  `,
  styles: [`
    mat-radio-group {
      display: flex;
      flex-direction: column;
      margin: 15px 0;
    }

    mat-radio-button {
      margin: 5px 0;
    }

    .notes-field {
      width: 100%;
      margin-top: 15px;
    }

    .justification-hint {
      display: flex;
      align-items: center;
      color: #d32f2f;
      font-size: 13px;
      margin: 8px 0;
    }

    .info-icon {
      font-size: 16px;
      margin-right: 4px;
      color: #d32f2f;
    }
  `]
})
export class LateCheckinDialogComponent {
  reasons: string[] = [];
  selectedReason: string = '';
  notes: string = '';
  message: string = '';
  messageTitle: string = 'Check-In Reason Required';

  constructor(
    public dialogRef: MatDialogRef<LateCheckinDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {}

  ngOnInit(): void {
    // Set reasons based on condition
    if (this.data.isShiftMismatch) {
      this.messageTitle = 'Shift Mismatch Detected';
      this.message = this.data.message || `Your check-in time is outside your scheduled shift. Please provide a reason.`;
      this.reasons = [
        'Early Check-In',
        'Missed to fill attendance form (Present on floor)',
        'Different Shift (Mismatch)',
        'Other'
      ];
    } else if (this.data.isEarlyCheckIn) {
      this.messageTitle = 'Early Check-In';
      this.message = this.data.message || `You are checking in early. Please provide a reason.`;
      this.reasons = [
        'Early Check-In',
        'Different Shift (Mismatch)',
        'Transport (Cab arrived Early)',
        'Other'
      ];
    } else {
      this.messageTitle = 'Late Check-In';
      this.message = this.data.message || `You are checking in late. Please provide a reason.`;
      this.reasons = [
        'Transport Issue (Cab got late)',
        'Medical Issue',
        'First Half Leave',
        'Second Half Leave',
        'Missed to fill attendance form (Present on floor)',
        'Different Shift (Mismatch)',
        'Other'
      ];
    }
  }

  /** Dynamic placeholder */
  getPlaceholderText(): string {
    if (this.selectedReason === 'Different Shift (Mismatch)' || this.selectedReason === 'Other') {
      return `Please provide a suitable justification for ${this.selectedReason}.`;
    }
    return 'You can also provide an additional comment for the selected reason.';
  }

  /** Disable submit based on validation */
  isSubmitDisabled(): boolean {
    if (!this.selectedReason) return true;
    
    // Notes required for "Different Shift (Mismatch)", "Other", and "OverTime"
    if (this.selectedReason === 'Different Shift (Mismatch)' || this.selectedReason === 'Other') {
      if (!this.notes.trim()) return true;
    }
    return false;
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSubmit(): void {
    const result = {
      reason: this.selectedReason,
      notes: this.notes
    };
    this.selectedReason = '';
    this.notes = '';
    this.dialogRef.close(result);
  }
}