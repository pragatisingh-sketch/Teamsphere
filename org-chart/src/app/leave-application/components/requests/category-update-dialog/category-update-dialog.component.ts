import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

export interface RequestModel {
  id: number | string;
  requestorName?: string;
  leaveType?: string;
  startDate?: string | Date;
  endDate?: string | Date;
  leaveCategory?: string;
  document?: string;
  status?: string;
  [key: string]: any;
}

export interface CategoryDialogData {
  mode?: 'single'; 
  rows: RequestModel[];
  userRole?: string;
}

@Component({
  selector: 'app-category-update-dialog',
  templateUrl: './category-update-dialog.component.html',
  styleUrls: ['./category-update-dialog.component.css']
})
export class CategoryUpdateDialogComponent implements OnInit {
  form!: FormGroup;
  mode: 'single' = 'single';
  rows: RequestModel[] = [];
  categoryOptions = ['PLANNED', 'UNPLANNED'];

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<CategoryUpdateDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CategoryDialogData
  ) { }

  ngOnInit(): void {
    this.mode = this.data?.mode || 'single';
    this.rows = Array.isArray(this.data?.rows) ? this.data.rows : [];

    // build form
    this.form = this.fb.group({
      newCategory: [this.rows[0].leaveCategory || null, Validators.required],
      reason: ['', [Validators.required, Validators.maxLength(500)]]
    });

  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload = {
      category: this.form.value.newCategory,
      reason: this.form.value.reason,
    };

    // Close with payload; parent will call service for single/bulk endpoints
    this.dialogRef.close({ confirmed: true, payload });
  }

  cancel() {
    this.dialogRef.close({ confirmed: false });
  }
}
