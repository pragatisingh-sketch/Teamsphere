import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';

export interface InsightDataRow {
  category: string;
  count: number | string;
  icon?: string;
  color?: 'red' | 'orange' | 'green' | 'blue' | 'purple';
}

export interface InsightData {
  title: string;
  value: string | number;
  subtitle?: string;
  trend?: {
    value: number;
    direction: 'up' | 'down' | 'neutral';
    label?: string;
  };
  icon?: string;
  color?: 'primary' | 'accent' | 'warn' | 'success';
  type?: 'percentage' | 'number' | 'currency' | 'score';
  progress?: number; // 0-100 for progress bars
  secondaryValue?: string | number;
  secondaryLabel?: string;
  // New compact table properties
  layoutType?: 'default' | 'compact-table';
  summaryText?: string;
  tableRows?: InsightDataRow[];
}

@Component({
  selector: 'app-insight-card',
  templateUrl: './insight-card.component.html',
  styleUrls: ['./insight-card.component.css']
})
export class InsightCardComponent implements OnInit {
  @Input() data: InsightData = {
    title: '',
    value: 0,
    subtitle: '',
    trend: { value: 0, direction: 'neutral' },
    icon: '',
    color: 'primary',
    type: 'number',
    progress: 0,
    secondaryValue: 0,
    secondaryLabel: ''
  };

  @Input() layout: 'default' | 'compact' | 'horizontal' = 'default';
  @Input() showProgress: boolean = false;
  @Input() showSecondary: boolean = false;
  @Input() clickable: boolean = true;
  @Input() loading: boolean = false;

  @Output() cardClick = new EventEmitter<void>();

  get trendIcon(): string {
    switch (this.data.trend?.direction) {
      case 'up': return 'trending_up';
      case 'down': return 'trending_down';
      default: return 'remove';
    }
  }

  get trendColor(): string {
    switch (this.data.trend?.direction) {
      case 'up': return 'success';
      case 'down': return 'warn';
      default: return 'primary';
    }
  }

  get formattedValue(): string {
    if (this.loading) return '--';

    const value = this.data.value;
    if (this.data.type === 'percentage') {
      return `${value}%`;
    } else if (this.data.type === 'currency') {
      return `$${value.toLocaleString()}`;
    } else if (this.data.type === 'score') {
      return `${value}/100`;
    } else {
      return typeof value === 'number' ? value.toLocaleString() : value;
    }
  }

  get formattedTrendValue(): string {
    if (this.data.trend && this.data.trend.value !== undefined) {
      // Ensure value is a number before calling toFixed
      const value = typeof this.data.trend.value === 'number'
        ? this.data.trend.value
        : parseFloat(String(this.data.trend.value));

      if (!isNaN(value)) {
        return value.toFixed(1);
      }
    }
    return '0.0';
  }

  ngOnInit(): void {
    // Default icon based on title if not provided
    if (!this.data.icon) {
      this.data.icon = this.getDefaultIcon();
    }
  }

  getDefaultIcon(): string {
    const title = this.data.title.toLowerCase();
    if (title.includes('utilization')) return 'pie_chart';
    if (title.includes('performance')) return 'speed';
    if (title.includes('project')) return 'assignment';
    if (title.includes('leave') || title.includes('absence')) return 'event_busy';
    if (title.includes('budget') || title.includes('cost')) return 'account_balance';
    if (title.includes('time') || title.includes('hour')) return 'schedule';
    return 'insights';
  }

  onCardClick(): void {
    if (this.clickable && !this.loading) {
      this.cardClick.emit();
    }
  }
}