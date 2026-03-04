export interface TimeEntryDetail {
  process: string;
  activity: string;
  timeInMins: number;
  comment: string;
  status: string;
}

export interface TimeEntryBreakdown {
  date: string;
  timeInMins: number;
  entries: number;
  details: TimeEntryDetail[];
}

export interface TimeEntryHierarchicalSummary {
  projectId: number;
  projectCode: string;
  projectName: string;
  userId: number;
  username: string;
  totalTimeInMins: number;
  totalEntries: number;
  timeUnit: string;
  startDate: string;
  endDate: string;
  breakdowns: TimeEntryBreakdown[];
}

export interface TimeSummaryFilters {
  userId?: number;
  projectId?: number;
  startDate?: Date;
  endDate?: Date;
} 