import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { InsightData } from '../shared/components/insight-card/insight-card.component';
import { ReportsService, DashboardInsightsRequest } from '../services/reports.service';
import { DateFilterService } from '../services/date-filter.service';
import { ExportService } from '../services/export.service';
import { PermissionService } from '../shared/permission.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-reports',
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.css']
})
export class ReportsComponent implements OnInit, OnDestroy {

  // Insight cards data
  insightCards: InsightData[] = [];
  loading = true;

  // Service subscription
  private insightsSubscription?: Subscription;

  // Filter panel state
  filterPanelOpen = false;

  // Filter data

  // Date filters
  startDate = new Date();
  endDate = new Date();

  // Selected preset for visual indication
  selectedPreset = 'lastWeek';

  // Active filters
  activeFilters = {
    dateRange: { start: null as Date | null, end: null as Date | null },
    level: ''
  };

  constructor(
    private reportsService: ReportsService,
    private router: Router,
    private dateFilterService: DateFilterService,
    private exportService: ExportService,
    public permissionService: PermissionService
  ) { }

  ngOnInit(): void {
    // Get persisted date range from service
    const currentRange = this.dateFilterService.getCurrentDateRange();
    this.startDate = currentRange.start;
    this.endDate = currentRange.end;

    // Set active filters
    this.activeFilters.dateRange = {
      start: this.startDate,
      end: this.endDate
    };

    // Restore selected preset if available, otherwise default
    if (currentRange.preset) {
      this.selectedPreset = currentRange.preset;
    }

    console.log('Initial filters loaded from service:', this.activeFilters);
    this.loadDashboardInsights();
  }

  ngOnDestroy(): void {
    // Unsubscribe from any active subscriptions
    if (this.insightsSubscription) {
      this.insightsSubscription.unsubscribe();
    }
  }

  initializeDates(): void {
    // Reset date filter service to default
    this.dateFilterService.resetToDefault();

    // Get the default range back
    const defaultRange = this.dateFilterService.getCurrentDateRange();
    this.startDate = defaultRange.start;
    this.endDate = defaultRange.end;

    // Reset preset
    this.selectedPreset = defaultRange.preset || 'lastWeek';

    // Set the active filters with the default range
    this.activeFilters.dateRange = {
      start: this.startDate,
      end: this.endDate
    };
  }

  /**
   * Handle preset date range selection
   */
  selectDatePreset(preset: string): void {
    const today = new Date();

    switch (preset) {

      case 'lastWeek': {
        // Find Monday of the current week (local)
        const day = today.getDay(); // Sunday=0
        const currentWeekMonday = new Date(today);
        currentWeekMonday.setDate(today.getDate() - ((day + 6) % 7));

        // Last week's Monday (start) and Sunday (end)
        const start = new Date(currentWeekMonday);
        start.setDate(start.getDate() - 7);
        start.setHours(0, 0, 0, 0);              // normalize to local start of day

        const end = new Date(start);
        end.setDate(start.getDate() + 6);
        end.setHours(23, 59, 59, 999);          // normalize to local end of day

        this.startDate = start;
        this.endDate = end;
        this.selectedPreset = 'lastWeek';
        break;
      }

      case 'lastMonth': {
        const year = today.getFullYear();
        const month = today.getMonth(); // 0-indexed

        const start = new Date(year, month - 1, 1);
        start.setHours(0, 0, 0, 0);

        const end = new Date(year, month, 0); // last day of previous month
        end.setHours(23, 59, 59, 999);

        this.startDate = start;
        this.endDate = end;
        this.selectedPreset = 'lastMonth';
        break;
      }

      case 'lastQuarter': {
        const currentMonth = today.getMonth(); // 0–11
        const currentQuarter = Math.floor(currentMonth / 3); // 0–3
        let lastQuarter = currentQuarter - 1;
        let year = today.getFullYear();

        if (lastQuarter < 0) {
          lastQuarter = 3;
          year = year - 1;
        }

        const startMonth = lastQuarter * 3;
        const start = new Date(year, startMonth, 1);
        start.setHours(0, 0, 0, 0);

        const end = new Date(year, startMonth + 3, 0); // last day of quarter
        end.setHours(23, 59, 59, 999);

        this.startDate = start;
        this.endDate = end;
        this.selectedPreset = 'lastQuarter';
        break;
      }

      case 'last7Days': {
        const start = new Date(today);
        start.setDate(start.getDate() - 6);
        start.setHours(0, 0, 0, 0);

        const end = new Date(today);
        end.setHours(23, 59, 59, 999);

        this.startDate = start;
        this.endDate = end;
        this.selectedPreset = 'last7Days';
        break;
      }

      default:
        // leave as-is or handle invalid preset
        break;
    }

    // Update active filters
    this.activeFilters.dateRange = {
      start: this.startDate,
      end: this.endDate
    };

    // Update the global date filter service
    this.dateFilterService.updateDateRange(this.startDate, this.endDate, this.selectedPreset);

    // Apply filters automatically when preset is selected
    this.applyFilters();
  }



  loadDashboardInsights(): void {
    this.loading = true;

    // Build request parameters
    const params: DashboardInsightsRequest = {};

    if (this.activeFilters.dateRange.start && this.activeFilters.dateRange.end) {
      params.startDate = this.activeFilters.dateRange.start.toISOString().split('T')[0];
      params.endDate = this.activeFilters.dateRange.end.toISOString().split('T')[0];
    }

    if (this.activeFilters.level) {
      params.level = this.activeFilters.level;
    }

    // Cancel previous subscription if exists
    if (this.insightsSubscription) {
      this.insightsSubscription.unsubscribe();
    }

    // Use the enhanced real-time service method
    this.insightsSubscription = this.reportsService.getDashboardInsightsWithFallback(params).subscribe({
      next: (response) => {
        console.log('Real-time API Response:', response);
        if (response.status === 'success' && response.data) {
          console.log('Converting multithreaded DTOs:', response.data);
          // Convert backend DTOs to frontend InsightData interface
          this.insightCards = response.data.map(dto => this.reportsService.convertInsightCardDTO(dto));
          console.log('Converted Real-time Insight Cards:', this.insightCards);

          // Log performance metrics
          console.timeEnd('Dashboard Insights Load Time');
        } else {
          console.error('Failed to load real-time insights:', response.message);
          this.insightCards = this.getCachedInsightCards() || this.getDefaultInsightCards();
        }

        // Cache successful responses for offline access
        if (response.status === 'success' && response.data) {
          this.cacheInsightCards(response.data.map(dto => this.reportsService.convertInsightCardDTO(dto)));
        }

        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading real-time insights:', error);
        console.log('Falling back to cached/default data');

        // Fallback to cached data or default hardcoded cards
        const cachedCards = this.getCachedInsightCards();
        this.insightCards = cachedCards || this.getDefaultInsightCards();
        this.loading = false;
      }
    });

    // Start timing for performance monitoring
    console.time('Dashboard Insights Load Time');
  }

  /**
   * Fallback method to return default insight cards when API fails
   */
  private getDefaultInsightCards(): InsightData[] {
    return [
      // Compact Table Card
      {
        title: 'Overall Compliance',
        subtitle: 'Time Attendance & Leave Compliance',
        value: 735,
        trend: { value: 15, direction: 'up', label: 'from last period' },
        icon: 'table_chart',
        color: 'primary',
        type: 'number',
        layoutType: 'compact-table',
        summaryText: 'Summary Overview',
        tableRows: [
          {
            category: 'Time Entry',
            count: 642,
            icon: 'access_time',
            color: 'red'
          },
          {
            category: 'Attendance',
            count: 305,
            icon: 'people',
            color: 'red'
          },
          {
            category: 'Leaves',
            count: 70,
            icon: 'calendar_today',
            color: 'orange'
          }
        ]
      },
      // Original card
      {
        title: 'Overall Defaulters',
        value: 42,
        subtitle: 'Time, Attendance & Leave Defaulters',
        trend: { value: 4.5, direction: 'up', label: 'from last month' },
        icon: 'warning',
        color: 'warn',
        type: 'number',
        progress: 18, // percentage of total employees who are defaulters
        secondaryValue: 'TE: 120, ATD: 180, LV: 100',
        secondaryLabel: 'Defaulter Breakdown'
      }
      ,
      {
        title: 'Overall Organization Utilization',
        value: '99',
        subtitle: 'Based on activity-based working & non-working days',
        trend: { value: 1.2, direction: 'up', label: 'from last month' },
        icon: 'trending_up',
        color: 'primary',
        type: 'percentage',
        progress: 99,
        secondaryValue: 'Working: 266, Non-Working: 4',
        secondaryLabel: 'Activity-Based Overview'
      },

      {
        title: 'Activity-Based Utilization',
        value: '85',
        subtitle: 'Working vs Non-Working Activities',
        trend: { value: 1.5, direction: 'up', label: 'from last month' },
        icon: 'analytics',
        color: 'primary',
        type: 'percentage',
        progress: 85,
        secondaryValue: 'Working: 85%, Non-Working: 15% | Absenteeism: 8, CompOff: 4',
        secondaryLabel: 'Detailed Activity Analysis'
      },

      {
        title: 'Leaves / WFH Report (Month)',
        value: '100',
        subtitle: 'Total entries for this month',
        trend: { value: 4.1, direction: 'up', label: 'from last month' },
        icon: 'calendar_month',
        color: 'accent',
        type: 'number',
        progress: 60, // e.g., WFH ratio or any metric you choose
        secondaryValue: 'Leaves: 38, WFH: 62',
        secondaryLabel: 'Monthly Breakdown'
      },
      {
        title: 'Leave & Absence',
        value: 23,
        subtitle: 'Days taken this month',
        trend: { value: 8.7, direction: 'down', label: 'from last month' },
        icon: 'event_busy',
        color: 'warn',
        type: 'number',
        progress: 35,
        secondaryValue: '15',
        secondaryLabel: 'Remaining'
      },
      {
        title: 'Budget vs Actual',
        value: 92,
        subtitle: 'Budget utilization',
        trend: { value: 3.4, direction: 'up', label: 'over budget' },
        icon: 'account_balance',
        color: 'accent',
        type: 'percentage',
        progress: 92,
        secondaryValue: '$124K',
        secondaryLabel: 'Spent'
      },
      {
        title: 'Time Tracking',
        value: 6.8,
        subtitle: 'Average hours/day',
        trend: { value: 0.5, direction: 'up', label: 'from last week' },
        icon: 'schedule',
        color: 'primary',
        type: 'number',
        progress: 85,
        secondaryValue: '95%',
        secondaryLabel: 'Compliance'
      }
    ];
  }

  /**
   * Get cached insight cards from localStorage or sessionStorage
   */
  private getCachedInsightCards(): InsightData[] | null {
    try {
      const cachedData = localStorage.getItem('dashboard_insights_cache');
      if (cachedData) {
        const parsed = JSON.parse(cachedData);
        // Validate the cached data structure
        if (parsed && Array.isArray(parsed) && parsed.length > 0 && parsed[0].title) {
          console.log('Using cached insight cards');
          return parsed;
        }
      }
    } catch (error) {
      console.error('Error parsing cached insight cards:', error);
    }
    return null;
  }

  /**
   * Cache insight cards for offline access
   */
  private cacheInsightCards(cards: InsightData[]): void {
    try {
      localStorage.setItem('dashboard_insights_cache', JSON.stringify(cards));
    } catch (error) {
      console.error('Error caching insight cards:', error);
    }
  }

  // Filter panel methods
  toggleFilterPanel(): void {
    this.filterPanelOpen = !this.filterPanelOpen;
  }

  closeFilterPanel(): void {
    this.filterPanelOpen = false;
  }

  // Filter change methods
  onDateChange(): void {
    // Update active filters when dates change
    this.activeFilters.dateRange = {
      start: this.startDate,
      end: this.endDate
    };
  }

  onFilterChange(): void {
    // This will be called when any filter selection changes
    console.log('Filters changed:', this.activeFilters);
  }

  clearFilters(): void {
    // Reset all filters
    this.activeFilters = {
      dateRange: { start: null, end: null },
      level: ''
    };

    // Reset date pickers and preset selection
    this.selectedPreset = 'lastWeek';
    this.initializeDates();

    console.log('Filters cleared');
  }

  applyFilters(): void {
    // Apply filters and refresh data
    this.loading = true;

    console.log('Applying filters:', this.activeFilters);

    // Update global date filter service with custom range
    if (this.startDate && this.endDate) {
      this.dateFilterService.updateDateRange(this.startDate, this.endDate, this.selectedPreset);
    }

    // Load insights with new filters
    this.loadDashboardInsights();

    this.filterPanelOpen = false; // Close panel after applying
  }

  updateInsightCardsWithFilters(): void {
    // This method is no longer needed as we're using the API directly
    // The filtering logic is now handled by the backend
  }

  onCardClick(card: InsightData): void {
    console.log('Card clicked:', card.title);
    this.openDetailedView(card);
  }

  openDetailedView(card: InsightData): void {
    console.log(`Opening detailed view for: ${card.title}`);

    // Navigate based on card type
    const titleLower = card.title.toLowerCase();
    const subtitleLower = card.subtitle ? card.subtitle.toLowerCase() : '';

    // Check for specific patterns in title/subtitle to determine routing
    if (titleLower.includes('non-compliance') || subtitleLower.includes('non-compliant')) {
      this.router.navigate(['/compliance-details']);
    } else if (titleLower.includes('utilization') ||
      (titleLower.includes('over') && (titleLower.includes('allocated') || titleLower.includes('utilized')))) {
      this.router.navigate(['/utilization-details']);
    } else if (titleLower.includes('time entry') && titleLower.includes('pending')) {
      this.router.navigate(['/time-entry-pending']);
    } else if (titleLower.includes('attendance') && titleLower.includes('not marked')) {
      this.router.navigate(['/attendance-not-marked']);
    } else if (titleLower.includes('long weekend') || titleLower.includes('leave pattern')) {
      this.router.navigate(['/long-weekend-leave']);
    } else {
      console.log('No specific route found for:', card.title);
    }
  }

  goToDashboard(): void {
    this.router.navigate(['/admin-dashboard']);
  }

  /**
   * Download attendance tracker Excel with Instructions and Structure tabs
   */
  downloadAttendanceTracker(): void {
    const startDate = this.activeFilters.dateRange.start || this.startDate;
    const endDate = this.activeFilters.dateRange.end || this.endDate;

    const startDateStr = startDate.toISOString().split('T')[0];
    const endDateStr = endDate.toISOString().split('T')[0];

    console.log('Downloading attendance tracker for:', startDateStr, 'to', endDateStr);

    this.exportService.exportExcel('ATTENDANCE_TRACKER', startDateStr, endDateStr).subscribe({
      next: (blob) => {
        // Create download link
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `attendance-tracker_${startDateStr}_to_${endDateStr}.xlsx`;
        link.click();
        window.URL.revokeObjectURL(url);
        console.log('Attendance tracker downloaded successfully');
      },
      error: (error) => {
        console.error('Error downloading attendance tracker:', error);
        alert('Failed to download attendance tracker. Please try again.');
      }
    });
  }

  formatDate(date: Date): string {
    return date ? date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' }) : '';
  }
}