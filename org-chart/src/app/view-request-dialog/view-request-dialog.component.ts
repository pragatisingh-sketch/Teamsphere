import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { HttpClient } from '@angular/common/http';
import { environment } from 'src/environments/environment';

interface ColumnChange {
  column: string;
  oldValue: any;
  newValue: any;
}

@Component({
  selector: 'app-view-request-dialog',
  templateUrl: './view-request-dialog.component.html',
  styleUrls: ['./view-request-dialog.component.css']
})
export class ViewRequestDialogComponent {
  employeeData: any[] = []; // Ensure it's always an array
  displayedColumns: string[] = [];
  isEditRequest: boolean = false;
  originalEmployee: any = null;
  changedColumns: ColumnChange[] = [];
  hasOriginalData: boolean = false;

  excludedColumns: string[] = ['id', 'parent'];

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    private http: HttpClient
  ) {
    if (data && data.employeeData) {
      try {
        // Parse the employee data JSON string
        let parsedData = typeof data.employeeData === 'string' ? JSON.parse(data.employeeData) : data.employeeData;

        // Ensure it's always an array
        this.employeeData = Array.isArray(parsedData) ? parsedData : [parsedData];

        if (this.employeeData.length > 0) {
          this.displayedColumns = Object.keys(this.employeeData[0]).filter(
            column => !this.excludedColumns.includes(column)
          );

          // Check if this is an edit request
          this.isEditRequest = data.requestType === 'EDIT EXISTING USER';

          // Check if we have original data from the backend
          if (data.originalData) {
            try {
              this.originalEmployee = typeof data.originalData === 'string' ? JSON.parse(data.originalData) : data.originalData;
              this.hasOriginalData = true;
              this.identifyChanges();
            } catch (error) {
              console.error('Error parsing original data:', error);
              this.hasOriginalData = false;
            }
          } else if (this.isEditRequest && this.employeeData.length > 0) {
            // Fallback to fetching original data via API (for backward compatibility)
            this.fetchOriginalEmployee(this.employeeData[0].ldap);
          }
        }
      } catch (error) {
        console.error('Error parsing employee data:', error);
        this.employeeData = [];
        this.displayedColumns = [];
      }
    } else {
      console.warn('No employee data provided to dialog');
      this.employeeData = [];
      this.displayedColumns = [];
    }
  }

  fetchOriginalEmployee(ldap: string) {
    if (!ldap) return;

    this.http.get<{status: string, code: number, message: string, data: any}>(`${environment.apiUrl}/api/employees/by-ldap/${ldap}`).subscribe({
      next: (response: any) => {
        console.log('Original employee data:', response.data);
        this.originalEmployee = response.data;
        this.identifyChanges();
      },
      error: (error) => {
        console.error('Error fetching original employee data:', error);
        // Try to get employee by ID as fallback
        if (this.employeeData[0].id) {
          this.http.get<{status: string, code: number, message: string, data: any}>(`${environment.apiUrl}/api/employees/${this.employeeData[0].id}`).subscribe({
            next: (response: any) => {
              console.log('Fetched by ID instead:', response.data);
              this.originalEmployee = response.data;
              this.identifyChanges();
            },
            error: (err) => {
              console.error('Error fetching by ID:', err);
            }
          });
        }
      }
    });
  }

  identifyChanges() {
    if (!this.originalEmployee || this.employeeData.length === 0) return;

    const editedEmployee = this.employeeData[0];
    this.changedColumns = [];

    // Compare each property to find differences
    for (const column of this.displayedColumns) {
      // Skip profilePic comparison as it's binary data
      if (column === 'profilePic') continue;

      const originalValue = this.originalEmployee[column];
      const newValue = editedEmployee[column];

      // Handle different data types and null/undefined values
      const areEqual = this.compareValues(originalValue, newValue);

      if (!areEqual) {
        this.changedColumns.push({
          column,
          oldValue: originalValue,
          newValue: newValue
        });
      }
    }

    console.log('Changed columns:', this.changedColumns);
  }

  compareValues(val1: any, val2: any): boolean {
    // If both are null or undefined, they're equal
    if (val1 == null && val2 == null) return true;

    // If only one is null/undefined, they're not equal
    if (val1 == null || val2 == null) return false;

    // Convert to strings for comparison (handles dates and numbers)
    return String(val1).trim() === String(val2).trim();
  }

  isColumnChanged(column: string): boolean {
    return this.changedColumns.some(change => change.column === column);
  }

  getOriginalValue(column: string): any {
    const change = this.changedColumns.find(c => c.column === column);
    return change ? change.oldValue : null;
  }
}
