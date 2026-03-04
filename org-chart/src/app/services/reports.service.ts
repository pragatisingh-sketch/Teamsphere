import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { InsightData } from '../shared/components/insight-card/insight-card.component';
import { environment } from '../../environments/environment';

export interface DashboardInsightsRequest {
  department?: string;
  project?: string;
  startDate?: string;
  endDate?: string;
  timeRange?: string;
  level?: string;
}
export interface BaseResponse<T> {
  status: string;
  code: number;
  message: string;
  data: T;
}

@Injectable({
  providedIn: 'root'
})
export class ReportsService {
  private apiUrl = `${environment.apiUrl}/api/reports`;

  constructor(private http: HttpClient) { }

  /**
   * Get dashboard insight cards from the API
   */
  getDashboardInsights(params?: DashboardInsightsRequest): Observable<BaseResponse<InsightData[]>> {
    let httpParams = new HttpParams();

    if (params) {
      Object.keys(params).forEach(key => {
        const value = params[key as keyof DashboardInsightsRequest];
        if (value !== undefined && value !== null) {
          httpParams = httpParams.set(key, value.toString());
        }
      });
    }

    return this.http.get<BaseResponse<InsightData[]>>(`${this.apiUrl}/insights`, { params: httpParams });
  }

  /**
   * Convert backend DTO to frontend InsightData interface
   */
  convertInsightCardDTO(dto: any): InsightData {
    console.log('Converting DTO:', dto);

    // Handle value conversion - ensure it's the right type
    let value: string | number = dto.value;
    if (typeof dto.value === 'string' && !isNaN(Number(dto.value))) {
      value = Number(dto.value);
    }

    const insightData: InsightData = {
      title: dto.title,
      value: value,
      subtitle: dto.subtitle,
      trend: dto.trend ? {
        value: typeof dto.trend.value === 'number' ? dto.trend.value : parseFloat(dto.trend.value) || 0,
        direction: dto.trend.direction || 'neutral',
        label: dto.trend.label
      } : undefined,
      icon: dto.icon || 'insights',
      color: this.mapColor(dto.color),
      type: dto.type || 'number',
      progress: dto.progress || 0,
      secondaryValue: dto.secondaryValue || '',
      secondaryLabel: dto.secondaryLabel || '',
      // New compact table properties
      layoutType: dto.layoutType || 'default',
      summaryText: dto.summaryText,
      tableRows: dto.tableRows ? dto.tableRows.map((row: any) => ({
        category: row.category,
        count: row.count,
        icon: row.icon,
        color: row.color
      })) : undefined
    };

    console.log('Converted to InsightData:', insightData);
    return insightData;
  }

  /**
   * Map backend colors to frontend color scheme
   */
  private mapColor(backendColor: string): 'primary' | 'accent' | 'warn' | 'success' {
    switch (backendColor?.toLowerCase()) {
      case 'blue':
      case 'primary':
        return 'primary';
      case 'green':
      case 'success':
        return 'success';
      case 'orange':
      case 'warn':
        return 'warn';
      case 'purple':
      case 'indigo':
      case 'accent':
        return 'accent';
      default:
        return 'primary';
    }
  }

  /**
   * Get time entry summary
   */
  getTimeEntrySummary(startDate: string, endDate: string): Observable<BaseResponse<any>> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);

    return this.http.get<BaseResponse<any>>(`${this.apiUrl}/time-entry-summary`, { params });
  }

  /**
   * Get project allocation
   */
  getProjectAllocation(startDate: string, endDate: string): Observable<BaseResponse<any>> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);

    return this.http.get<BaseResponse<any>>(`${this.apiUrl}/project-allocation`, { params });
  }

  /**
   * Get employee productivity
   */
  getEmployeeProductivity(startDate: string, endDate: string): Observable<BaseResponse<any>> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);

    return this.http.get<BaseResponse<any>>(`${this.apiUrl}/employee-productivity`, { params });
  }

  /**
   * Get attendance report
   */
  getAttendanceReport(startDate: string, endDate: string): Observable<BaseResponse<any>> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);

    return this.http.get<BaseResponse<any>>(`${this.apiUrl}/attendance`, { params });
  }

  /**
   * Get real-time dashboard insights with enhanced error handling
   */
  getRealTimeDashboardInsights(params?: DashboardInsightsRequest): Observable<BaseResponse<InsightData[]>> {
    let httpParams = new HttpParams();

    if (params) {
      Object.keys(params).forEach(key => {
        const value = params[key as keyof DashboardInsightsRequest];
        if (value !== undefined && value !== null && value !== '') {
          httpParams = httpParams.set(key, value.toString());
        }
      });
    }

    // Add timeout and retry logic for real-time data
    return this.http.get<BaseResponse<InsightData[]>>(`${this.apiUrl}/insights`, {
      params: httpParams
    });
  }

  /**
   * Get dashboard insights with fallback mechanism
   */
  getDashboardInsightsWithFallback(params?: DashboardInsightsRequest): Observable<BaseResponse<InsightData[]>> {
    return this.getRealTimeDashboardInsights(params);
  }

  /**
   * Get top 3 defaulters
   */
  getTopDefaulters(type: string, startDate: string, endDate: string, filters?: any): Observable<BaseResponse<any[]>> {
    let params = new HttpParams()
      .set('type', type)
      .set('startDate', startDate)
      .set('endDate', endDate);

    if (filters) {
      if (filters.team) params = params.set('team', filters.team);
      if (filters.project) params = params.set('project', filters.project);
      if (filters.program) params = params.set('program', filters.program);
      if (filters.manager) params = params.set('manager', filters.manager);
    }

    return this.http.get<BaseResponse<any[]>>(`${this.apiUrl}/top-defaulters`, { params });
  }

  /**
   * Get all defaulters detailed list
   */
  getAllDefaulters(type: string, startDate: string, endDate: string, filters?: any): Observable<BaseResponse<any[]>> {
    let params = new HttpParams()
      .set('type', type)
      .set('startDate', startDate)
      .set('endDate', endDate);

    if (filters) {
      if (filters.team) params = params.set('team', filters.team);
      if (filters.project) params = params.set('project', filters.project);
      if (filters.program) params = params.set('program', filters.program);
      if (filters.manager) params = params.set('manager', filters.manager);
    }

    return this.http.get<BaseResponse<any[]>>(`${this.apiUrl}/all-defaulters`, { params });
  }

  /**
   * Get utilization details
   */
  getUtilizationDetails(startDate: string, endDate: string, filters?: any): Observable<BaseResponse<any[]>> {
    let params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);

    if (filters) {
      if (filters.team) params = params.set('team', filters.team);
      if (filters.project) params = params.set('project', filters.project);
      if (filters.program) params = params.set('program', filters.program);
      if (filters.manager) params = params.set('manager', filters.manager);
    }

    return this.http.get<BaseResponse<any[]>>(`${this.apiUrl}/utilization-details`, { params });
  }

  /**
   * Get top 3 low utilization users
   */
  getTopLowUtilization(startDate: string, endDate: string, filters?: any): Observable<BaseResponse<any[]>> {
    let params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);

    if (filters) {
      if (filters.team) params = params.set('team', filters.team);
      if (filters.project) params = params.set('project', filters.project);
      if (filters.program) params = params.set('program', filters.program);
      if (filters.manager) params = params.set('manager', filters.manager);
    }

    return this.http.get<BaseResponse<any[]>>(`${this.apiUrl}/top-low-utilization`, { params });
  }

  /**
   * Get filter options
   */
  getFilterOptions(): Observable<BaseResponse<{ [key: string]: string[] }>> {
    return this.http.get<BaseResponse<{ [key: string]: string[] }>>(`${this.apiUrl}/filter-options`);
  }

  /**
   * Get detailed issues for a specific user by type 
   * Used for the issue details modal when clicking on issue counts
   */
  getUserIssues(type: string, employeeLdap: string, startDate: string, endDate: string): Observable<BaseResponse<IssueDetail[]>> {
    const params = new HttpParams()
      .set('type', type)
      .set('employeeLdap', employeeLdap)
      .set('startDate', startDate)
      .set('endDate', endDate);

    return this.http.get<BaseResponse<IssueDetail[]>>(`${this.apiUrl}/user-issues`, { params });
  }

  /**
   * Get weekly time-entry defaulters report
   * Returns list of employees who haven't filled time entries, grouped by weeks
   */
  getWeeklyTimeEntryDefaulters(
    startDate: string,
    endDate: string,
    filters?: { team?: string; manager?: string }
  ): Observable<BaseResponse<WeeklyTimeEntryDefaulter[]>> {
    let params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);

    if (filters) {
      if (filters.team) params = params.set('team', filters.team);
      if (filters.manager) params = params.set('manager', filters.manager);
    }

    return this.http.get<BaseResponse<WeeklyTimeEntryDefaulter[]>>(
      `${this.apiUrl}/weekly-time-entry-defaulters`,
      { params }
    );
  }

  /**
   * Send time-entry reminder emails
   * Supports both individual and bulk reminder sending
   */
  sendTimeEntryReminder(request: TimeEntryReminderRequest): Observable<BaseResponse<{ success: number; failed: number }>> {
    return this.http.post<BaseResponse<{ success: number; failed: number }>>(
      `${this.apiUrl}/send-time-entry-reminder`,
      request
    );
  }

  /**
   * Get daily attendance defaulters for a specific date
   */
  getDailyAttendanceDefaulters(
    date: Date,
    filters?: { team?: string; manager?: string }
  ): Observable<BaseResponse<DailyAttendanceDefaulter[]>> {
    let params = new HttpParams()
      .set('date', date.toISOString().split('T')[0]);

    if (filters) {
      if (filters.team) params = params.set('team', filters.team);
      if (filters.manager) params = params.set('manager', filters.manager);
    }

    return this.http.get<BaseResponse<DailyAttendanceDefaulter[]>>(
      `${this.apiUrl}/daily-attendance-defaulters`,
      { params }
    );
  }

  /**
   * Send attendance reminder emails
   */
  sendAttendanceReminder(request: AttendanceReminderRequest): Observable<BaseResponse<{ success: number; failed: number }>> {
    return this.http.post<BaseResponse<{ success: number; failed: number }>>(
      `${this.apiUrl}/send-attendance-reminder`,
      request
    );
  }

  /**
   * Get long weekend leave patterns
   */
  getLongWeekendLeavePatterns(
    startDate: Date,
    endDate: Date,
    filters?: { team?: string; manager?: string }
  ): Observable<BaseResponse<LongWeekendLeavePattern[]>> {
    let params = new HttpParams()
      .set('startDate', startDate.toISOString().split('T')[0])
      .set('endDate', endDate.toISOString().split('T')[0]);

    if (filters) {
      if (filters.team) params = params.set('team', filters.team);
      if (filters.manager) params = params.set('manager', filters.manager);
    }

    return this.http.get<BaseResponse<LongWeekendLeavePattern[]>>(
      `${this.apiUrl}/long-weekend-leave-patterns`,
      { params }
    );
  }
}


// Interface for issue details
export interface IssueDetail {
  id: number;
  type: string;
  date: string;
  description: string;
  status: string;
  createdAt?: string;
  updatedAt?: string;
  // TimeEntry specific
  project?: string;
  activity?: string;
  timeInMins?: number;
  process?: string;
  comment?: string;
  // Attendance specific
  entryTimestamp?: string;
  exitTimestamp?: string;
  lateLoginReason?: string;
  lateOrEarlyLogoutReason?: string;
  isOutsideOffice?: boolean;
  // Leave specific
  fromDate?: string;
  toDate?: string;
  leaveType?: string;
  leaveCategory?: string;
  duration?: string;
  applicationType?: string;
  startTime?: string;
  endTime?: string;
}

// Interface for weekly breakdown of missing entries
export interface WeeklyBreakdown {
  weekStartDate: string;
  weekEndDate: string;
  weekLabel: string;
  wholeWeekMissing: boolean;
  missingDays: string[];
}

// Interface for weekly time entry defaulter
export interface WeeklyTimeEntryDefaulter {
  employeeId: number;
  ldap: string;
  employeeName: string;
  email: string;
  department: string;
  manager: string;
  missingWeeksCount: number;
  weeklyBreakdowns: WeeklyBreakdown[];
}

// Interface for time entry reminder request
export interface TimeEntryReminderRequest {
  recipientLdaps: string[];
  customMessage?: string;
  bulk: boolean;
  missingPeriods?: string[];
  missingDays?: string[];
  wholeWeekMissing?: boolean;
}

// Interface for daily attendance defaulter
export interface DailyAttendanceDefaulter {
  employeeId: number;
  employeeName: string;
  ldap: string;
  email: string;
  department: string;
  manager: string;
  lastAttendanceDate: string | null;
}

// Interface for attendance reminder request
export interface AttendanceReminderRequest {
  recipientLdaps: string[];
  customMessage?: string;
  isBulk: boolean;
}

// Interface for long weekend leave pattern
export interface LongWeekendLeavePattern {
  employeeId: number;
  employeeName: string;
  ldap: string;
  email: string;
  department: string;
  manager: string;
  occurrenceCount: number;
  instances: LongWeekendInstance[];
}

// Interface for long weekend instance
export interface LongWeekendInstance {
  startDate: string;
  endDate: string;
  leaveType: string;
  totalDays: number;
  pattern: string;
}