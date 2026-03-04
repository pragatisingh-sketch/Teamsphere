import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TimeEntry } from '../time-entry.component';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

// Interface for the backend BaseResponse structure
interface BaseResponse<T> {
  status: string;
  code: number;
  message: string;
  data: T;
}

@Component({
  selector: 'app-week-copy-dialog',
  templateUrl: './week-copy-dialog.component.html',
  styleUrls: ['./week-copy-dialog.component.css']
})
export class WeekCopyDialogComponent implements OnInit {
  weekCopyForm: FormGroup;
  sourceEntry: TimeEntry;
  weekDays: {
    date: Date,
    selected: boolean,
    disabled: boolean,
    disabledReason?: string
  }[] = [];
  baseUrl = environment.apiUrl;
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    public dialogRef: MatDialogRef<WeekCopyDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    this.sourceEntry = data.timeEntry;

    this.weekCopyForm = this.fb.group({
      startDate: [this.getMonday(new Date()), Validators.required],
      endDate: [this.getFriday(new Date()), Validators.required]
    });
  }

  ngOnInit(): void {
    this.updateWeekDays();

    // Listen for date changes to update the week days
    this.weekCopyForm.get('startDate')?.valueChanges.subscribe(() => {
      this.updateWeekDays();
    });
  }

  /**
   * Updates the list of weekdays based on the selected start date
   * and checks for days that already have 480 minutes filled
   */
  updateWeekDays(): void {
    const startDate = this.weekCopyForm.get('startDate')?.value;
    if (!startDate) return;

    this.isLoading = true;

    // Clear existing days
    this.weekDays = [];

    // Get the Monday of the week containing the selected date
    const monday = this.getMonday(new Date(startDate));

    // Create temporary weekdays array
    const tempWeekDays: { date: Date, selected: boolean, disabled: boolean, disabledReason?: string }[] = [];

    // Create entries for Monday through Friday
    for (let i = 0; i < 5; i++) {
      const date = new Date(monday);
      date.setDate(monday.getDate() + i);

      // Check if this is the same date as the source entry
      const sourceDate = new Date(this.sourceEntry.entryDate || this.sourceEntry.date || '');
      const isSameDate = date.getFullYear() === sourceDate.getFullYear() &&
                         date.getMonth() === sourceDate.getMonth() &&
                         date.getDate() === sourceDate.getDate();

      tempWeekDays.push({
        date: date,
        selected: !isSameDate, // Select all days except the source day
        disabled: isSameDate,  // Disable the source day
        disabledReason: isSameDate ? 'Source entry date' : undefined
      });
    }

    // Update the end date to be Friday of the same week
    this.weekCopyForm.get('endDate')?.setValue(this.getFriday(monday));

    // Check remaining time for each day
    const remainingTimeRequests = tempWeekDays.map(day => {
      if (day.disabled) {
        return of(null); // Skip already disabled days
      }

      const formattedDate = this.formatDateForApi(day.date);
      const ldap = this.sourceEntry.ldap || '';

      return this.http.get<BaseResponse<number>>(`${this.baseUrl}/api/time-entries/remaining-time?date=${formattedDate}&ldap=${ldap}`)
        .pipe(
          map(response => ({
            date: day.date,
            remainingTime: response.status === 'success' ? response.data : 480
          })),
          catchError(() => of({ date: day.date, remainingTime: 480 })) // Default to 480 if error
        );
    });

    // Process all requests
    forkJoin(remainingTimeRequests).subscribe(results => {
      // Update weekDays with remaining time information
      tempWeekDays.forEach(day => {
        const result = results.find(r => r && this.isSameDay(r.date, day.date));

        // Get the time in minutes from the source entry, defaulting to 0 if undefined
        const entryTimeInMins = this.sourceEntry.timeInMins || 0;
        // Check if the entry is overtime, defaulting to false if undefined
        const isOvertime = this.sourceEntry.isOvertime || false;

        // If day is not already disabled and has no remaining time (0) and entry is not overtime
        if (!day.disabled && result && result.remainingTime === 0 && !isOvertime) {
          day.disabled = true;
          day.selected = false;
          day.disabledReason = 'This day already has 8 hours filled without overtime';
        }
        // If day is not already disabled and remaining time is less than entry time
        else if (!day.disabled && result && result.remainingTime < entryTimeInMins && !isOvertime) {
          day.disabled = true;
          day.selected = false;
          day.disabledReason = `Only ${result.remainingTime} minutes remaining for this day`;
        }
      });

      // Check for Google holidays and disable those days
      this.checkForHolidays(tempWeekDays).then(() => {
        this.weekDays = tempWeekDays;
        this.isLoading = false;
      });
    });
  }

  /**
   * Checks if two dates represent the same day
   */
  isSameDay(date1: Date, date2: Date): boolean {
    return date1.getFullYear() === date2.getFullYear() &&
           date1.getMonth() === date2.getMonth() &&
           date1.getDate() === date2.getDate();
  }

  /**
   * Gets the Monday of the week containing the given date
   */
  getMonday(date: Date): Date {
    const day = date.getDay();
    const diff = date.getDate() - day + (day === 0 ? -6 : 1); // Adjust for Sunday
    return new Date(date.setDate(diff));
  }

  /**
   * Gets the Friday of the week containing the given date
   */
  getFriday(date: Date): Date {
    const monday = this.getMonday(new Date(date));
    const friday = new Date(monday);
    friday.setDate(monday.getDate() + 4);
    return friday;
  }

  /**
   * Toggles the selection of a specific day
   */
  toggleDay(index: number): void {
    if (!this.weekDays[index].disabled) {
      this.weekDays[index].selected = !this.weekDays[index].selected;
    }
  }

  /**
   * Gets the formatted date string for display
   */
  formatDate(date: Date): string {
    return date.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
  }

  /**
   * Checks if any days are selected
   */
  hasSelectedDays(): boolean {
    return this.weekDays.some(day => day.selected);
  }

  /**
   * Submits the form and returns the selected days
   */
  onSubmit(): void {
    if (this.weekCopyForm.invalid || !this.hasSelectedDays()) {
      return;
    }

    // Get the selected days
    const selectedDates = this.weekDays
      .filter(day => day.selected)
      .map(day => this.formatDateForApi(day.date));

    this.dialogRef.close({
      selectedDates: selectedDates,
      sourceEntry: this.sourceEntry
    });
  }

  /**
   * Formats a date for the API (YYYY-MM-DD)
   */
  formatDateForApi(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  /**
   * Cancels the dialog
   */
  onCancel(): void {
    this.dialogRef.close(null);
  }

  /**
   * Checks for Google holidays in the given week days and disables them
   * @param weekDays Array of week days to check
   * @returns Promise that resolves when holiday checking is complete
   */
  private async checkForHolidays(weekDays: { date: Date, selected: boolean, disabled: boolean, disabledReason?: string }[]): Promise<void> {
    const holidayRequests = weekDays.map(day => {
      if (day.disabled) {
        return Promise.resolve(null); // Skip already disabled days
      }

      const formattedDate = this.formatDateForApi(day.date);
      return this.http.get<BaseResponse<boolean>>(`${this.baseUrl}/api/holidays/check?date=${formattedDate}`)
        .toPromise()
        .then(response => {
          if (response && response.status === 'success' && response.data === true) {
            // Get holiday details
            return this.http.get<BaseResponse<any>>(`${this.baseUrl}/api/holidays/date/${formattedDate}`)
              .toPromise()
              .then(holidayResponse => {
                if (holidayResponse && holidayResponse.status === 'success') {
                  day.disabled = true;
                  day.selected = true; // Keep holiday dates selected for batch processing
                  day.disabledReason = `Google Holiday: ${holidayResponse.data.holidayName}`;
                }
              })
              .catch(() => {
                // If we can't get the holiday name, just mark it as a holiday
                day.disabled = true;
                day.selected = true; // Keep holiday dates selected for batch processing
                day.disabledReason = 'Google Holiday';
              });
          }
          return null;
        })
        .catch(() => {
          // If there's an error checking holidays, continue without disabling
          return null;
        });
    });

    // Wait for all holiday checks to complete
    await Promise.all(holidayRequests);
  }
}
