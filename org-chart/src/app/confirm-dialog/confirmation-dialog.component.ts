import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

@Component({
  selector: 'app-confirmation-dialog',
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <p>{{ data.message }}</p>
      <mat-form-field *ngIf="data.showCommentField" appearance="outline" class="full-width">
        <mat-label>{{ data.commentLabel }}</mat-label>
        <textarea matInput [(ngModel)]="comment" rows="3"></textarea>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Cancel</button>
      <button mat-raised-button [color]="data.color || 'warn'" (click)="onConfirm()">
        {{ data.confirmButtonText || 'Confirm' }}
      </button>
    </mat-dialog-actions>
  `
})
export class ConfirmationDialogComponent {
  comment: string = '';

  constructor(
    public dialogRef: MatDialogRef<ConfirmationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {}

  onCancel(): void {
    this.dialogRef.close(false);
  }

  onConfirm(): void {
    if (this.data.showCommentField) {
      this.dialogRef.close({ confirmed: true, comment: this.comment });
    } else {
      this.dialogRef.close(true);
    }
  }
}
