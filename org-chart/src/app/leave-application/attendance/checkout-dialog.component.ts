import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

@Component({
  selector: 'app-checkout-dialog',
  template: `
    <h1 mat-dialog-title>{{ data.title }}</h1>

    <div mat-dialog-content>
      <p>{{ data.message }}</p>

      <!-- Reason Selection -->
      <mat-radio-group [(ngModel)]="selectedReason">
        <mat-radio-button *ngFor="let reason of reasons" [value]="reason">
          {{ reason }}
        </mat-radio-button>
      </mat-radio-group>

      <!-- Justification Section -->
      <div class="justification-section">
        <ng-container *ngIf="isNotesRequired()">
          <p class="justification-hint">
  <mat-icon class="info-icon">info</mat-icon>
  Please provide a suitable justification for the selected Reason - {{ selectedReason }}.
</p>

        </ng-container>

        <mat-form-field appearance="outline" class="notes-field" [class.active]="isNotesRequired()">
          <mat-label>Justification</mat-label>
          <textarea
            matInput
            [(ngModel)]="notes"
            [placeholder]="getPlaceholderText()"
          ></textarea>

          <mat-error *ngIf="isNotesRequired() && !notes.trim()">
            Justification is required for {{ selectedReason }}.
          </mat-error>
        </mat-form-field>
      </div>
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
      margin-top: 10px;
      transition: box-shadow 0.3s ease;
    }

    /* Subtle red glow when justification required */
    .notes-field.active ::ng-deep .mat-mdc-text-field-wrapper {
      box-shadow: 0 0 6px rgba(244, 67, 54, 0.4);
    }

    .justification-hint {
      display: flex;
      align-items: center;
      color: #e53935;
      font-size: 13px;
      margin: 4px 0 0 2px;
      font-weight: 500;
    }

    .info-icon {
      font-size: 16px;
      margin-right: 4px;
      color: #e57373;
    }
  `]
})
export class CheckoutDialogComponent {
  reasons: string[] = [];
  selectedReason: string = '';
  notes: string = '';

  constructor(
    public dialogRef: MatDialogRef<CheckoutDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) { }

  ngOnInit(): void {
    // Set reasons based on condition
    if (this.data.isEarlyCheckout) {
      this.reasons = [
        'Emergency/Personal Reason',
        'Medical Issue',
        'Second Half Leave',
        'Different Shift (Mismatch)',
        'Other'
      ];
    } else if (this.data.isLateCheckout) {
      this.reasons = [
        'Forgot to checkout',
        'Medical Issue',
        'First Half Leave',
        'Second Half Leave',
        'Different Shift (Mismatch)',
        'Other'
      ];
    } else {
      this.reasons = [
        'Forgot to checkout',
        'Medical Issue',
        'Emergency/Personal Reason',
        'Transport Issue',
        'Different Shift (Mismatch)',
        'OverTime',
        'Other'
      ];
    }
  }

  /** Determine if justification required */
  isNotesRequired(): boolean {
    return this.selectedReason === 'OverTime' || this.selectedReason === 'Other' ||
            this.selectedReason === 'Different Shift (Mismatch)';
  }

  /** Dynamic placeholder */
  getPlaceholderText(): string {
    return this.isNotesRequired()
      ? `Please provide a suitable note for ${this.selectedReason}.`
      : 'You can also provide a note for selected Reason';
  }

  /** Disable submit logic */
  isSubmitDisabled(): boolean {
    if (!this.selectedReason) return true;
    if (this.selectedReason === 'Different Shift (Mismatch)' || this.selectedReason === 'Other' || this.selectedReason === 'OverTime') {
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