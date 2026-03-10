import { NativeDateAdapter } from '@angular/material/core';
import { Injectable } from '@angular/core';

@Injectable()
export class CustomDateAdapter extends NativeDateAdapter {
  // Override the format method to add a class to weekend dates
  override format(date: Date, displayFormat: Object): string {
    // Get the formatted date from the parent class
    const formattedDate = super.format(date, displayFormat);

    // Check if the date is a weekend (Saturday or Sunday)
    const day = date.getDay();
    if (day === 0 || day === 6) { // 0 is Sunday, 6 is Saturday
      // Return the date with a span and a class for styling
      return `<span style="color: red;">${formattedDate}</span>`;
    }

    return formattedDate;
  }
}
