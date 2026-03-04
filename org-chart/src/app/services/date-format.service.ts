import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class DateFormatService {
  formatDate(value: any): string {
    if (!value) return '-';

    // Try parsing as a regular date
    const parsed = new Date(value);
    if (!isNaN(parsed.getTime())) {
      return parsed.toLocaleDateString('en-GB'); // Format: dd/MM/yyyy
    }

    // Try parsing as Excel serial number
    if (!isNaN(value) && value > 20000) {
      const excelEpoch = new Date(1899, 11, 30);
      excelEpoch.setDate(excelEpoch.getDate() + Math.floor(value));
      return excelEpoch.toLocaleDateString('en-GB');
    }

    return '-';
  }

  parseDate(value: any): Date | null {
    if (!value) return null;

    // Try parsing as a regular date
    const parsed = new Date(value);
    if (!isNaN(parsed.getTime())) {
      return parsed;
    }

    // Try parsing as Excel serial number
    if (!isNaN(value) && value > 20000) {
      const excelEpoch = new Date(1899, 11, 30);
      excelEpoch.setDate(excelEpoch.getDate() + Math.floor(value));
      return excelEpoch;
    }

    return null;
  }
} 