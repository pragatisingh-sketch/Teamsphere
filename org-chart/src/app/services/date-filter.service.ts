import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface DateRange {
    start: Date;
    end: Date;
    preset?: string;
}

@Injectable({
    providedIn: 'root'
})
export class DateFilterService {

    // Default to last 7 days
    private defaultDateRange: DateRange = {
        start: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000),
        end: new Date(),
        preset: 'lastWeek'
    };

    private dateRangeSubject: BehaviorSubject<DateRange>;
    // Observable for components to subscribe to
    dateRange$: Observable<DateRange>;

    constructor() {
        // Initialize with cached range or last 7 days
        const cachedRange = this.getCachedDateRange();
        if (cachedRange) {
            console.log('DateFilterService initialized with cached range:', cachedRange);
            this.dateRangeSubject = new BehaviorSubject<DateRange>(cachedRange);
        } else {
            console.log('DateFilterService initialized with default range:', this.defaultDateRange);
            this.dateRangeSubject = new BehaviorSubject<DateRange>(this.defaultDateRange);
        }

        // Ensure dateRange$ uses the correct subject
        this.dateRange$ = this.dateRangeSubject.asObservable();
    }

    /**
     * Get current date range
     */
    getCurrentDateRange(): DateRange {
        return this.dateRangeSubject.value;
    }

    /**
     * Update the global date range filter
     * All subscribed components will receive the update
     */
    updateDateRange(start: Date, end: Date, preset?: string): void {
        const newRange: DateRange = { start, end, preset };
        console.log('DateFilterService: Updating date range to:', newRange);
        this.cacheDateRange(newRange);
        this.dateRangeSubject.next(newRange);
    }

    /**
     * Reset to default date range (last 7 days)
     */
    resetToDefault(): void {
        console.log('DateFilterService: Resetting to default date range');
        localStorage.removeItem('report_date_filter_cache');
        this.dateRangeSubject.next(this.defaultDateRange);
    }

    private cacheDateRange(range: DateRange): void {
        try {
            localStorage.setItem('report_date_filter_cache', JSON.stringify({
                start: range.start.toISOString(),
                end: range.end.toISOString(),
                preset: range.preset
            }));
        } catch (error) {
            console.error('Error caching date filter:', error);
        }
    }

    private getCachedDateRange(): DateRange | null {
        try {
            const cached = localStorage.getItem('report_date_filter_cache');
            if (cached) {
                const parsed = JSON.parse(cached);
                return {
                    start: new Date(parsed.start),
                    end: new Date(parsed.end),
                    preset: parsed.preset
                };
            }
        } catch (error) {
            console.error('Error parsing cached date filter:', error);
        }
        return null;
    }

    /**
     * Format date range for API calls (yyyy-MM-dd)
     */
    getFormattedDateRange(): { startDate: string; endDate: string } {
        const range = this.getCurrentDateRange();
        return {
            startDate: this.formatDate(range.start),
            endDate: this.formatDate(range.end)
        };
    }

    /**
     * Format a single date to yyyy-MM-dd
     */
    private formatDate(date: Date): string {
        return date.toISOString().split('T')[0];
    }
}
