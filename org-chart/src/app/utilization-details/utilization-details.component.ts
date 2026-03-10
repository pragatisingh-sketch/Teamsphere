import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { TableConfig } from '../shared/components/reusable-table/table-config.interface';
import { DateFilterService, DateRange } from '../services/date-filter.service';
import { ReportsService } from '../services/reports.service';
import { AuthService } from '../auth.service';

interface UtilizationRecord {
  employeeId: number;
  employeeName: string;
  department: string;
  manager: string;
  project: string;
  program: string;
  fullyUtilizedDays: number;
  partiallyUtilizedDays: number;
  zeroUtilizationDays: number;
  utilizationPercentage: number;
}

@Component({
  selector: 'app-utilization-details',
  templateUrl: './utilization-details.component.html',
  styleUrls: ['./utilization-details.component.css']
})
export class UtilizationDetailsComponent implements OnInit, OnDestroy {

  // Top 3 Low Utilization
  topLowUtilizationUsers: UtilizationRecord[] = [];

  // Categorized Lists
  fullyUtilizedUsers: UtilizationRecord[] = [];
  partiallyUtilizedUsers: UtilizationRecord[] = [];
  zeroUtilizationUsers: UtilizationRecord[] = [];

  // Filtered Lists
  filteredFullyUtilized: UtilizationRecord[] = [];
  filteredPartiallyUtilized: UtilizationRecord[] = [];
  filteredZeroUtilization: UtilizationRecord[] = [];

  tableConfig: TableConfig = {} as TableConfig;
  loading = false;
  dateRange: DateRange | null = null;
  startDate: string = '';
  endDate: string = '';

  // Accordion State
  isZeroOpen = true;
  isPartialOpen = false;
  isFullyOpen = false;

  // Filters
  filters: { [key: string]: string } = {};
  userRole: string = 'USER';

  private dateSubscription?: Subscription;

  constructor(
    private router: Router,
    private dateFilterService: DateFilterService,
    private reportsService: ReportsService,
    private authService: AuthService
  ) { }

  ngOnInit(): void {
    // Get user role
    this.authService.role$.subscribe(role => {
      if (role) {
        this.userRole = role;
      }
    });

    this.dateSubscription = this.dateFilterService.dateRange$.subscribe(range => {
      this.dateRange = range;
      this.startDate = this.formatDateForApi(range.start);
      this.endDate = this.formatDateForApi(range.end);
      console.log('Utilization Details - Date range updated:', range);
      this.loadUtilizationData();
    });

    this.initializeTableConfig();
  }

  handleUnifiedFilterChange(event: { type: string, value: string | null }): void {
    const key = event.type.toLowerCase();
    if (event.value) {
      this.filters[key] = event.value;
    } else {
      delete this.filters[key];
    }
    this.loadUtilizationData();
  }

  ngOnDestroy(): void {
    if (this.dateSubscription) {
      this.dateSubscription.unsubscribe();
    }
  }

  initializeTableConfig(): void {
    this.tableConfig = {
      columns: [
        { key: 'employeeName', label: 'Name', sortable: true, filterable: true, searchable: true, width: '150px' },
        { key: 'department', label: 'Team', sortable: true, filterable: true, searchable: true, width: '120px' },
        { key: 'manager', label: 'Manager', sortable: true, filterable: true, searchable: true, width: '120px' },
        { key: 'project', label: 'Process', sortable: true, filterable: true, searchable: true, width: '120px' },
        { key: 'program', label: 'Program', sortable: true, filterable: true, searchable: true, width: '120px' },
        {
          key: 'fullyUtilizedDays',
          label: 'Fully Utilized',
          sortable: true,
          type: 'number',
          width: '100px',
          format: (value: number) => `<span style="color: #4caf50; font-weight: bold;">${value}</span>`
        },
        {
          key: 'partiallyUtilizedDays',
          label: 'Partially Utilized',
          sortable: true,
          type: 'number',
          width: '100px',
          format: (value: number) => `<span style="color: #ff9800; font-weight: bold;">${value}</span>`
        },
        {
          key: 'zeroUtilizationDays',
          label: 'Zero Utilization',
          sortable: true,
          type: 'number',
          width: '100px',
          format: (value: number) => `<span style="color: #f44336; font-weight: bold;">${value}</span>`
        },
        {
          key: 'utilizationPercentage',
          label: 'Utilization %',
          sortable: true,
          type: 'number',
          width: '120px',
          format: (value: number) => {
            const color = value >= 80 ? '#4caf50' : value >= 50 ? '#ff9800' : '#f44336';
            return `<span style="color: ${color}; font-weight: bold;">${value}%</span>`;
          }
        }
      ],
      showGlobalSearch: true,
      showColumnFilters: true,
      showColumnToggle: true,
      showPagination: true,
      pageSize: 10,
      showExport: true,
      exportFileName: 'utilization_report'
    };
  }

  loadUtilizationData(): void {
    if (!this.dateRange) return;
    this.loading = true;

    const start = this.startDate;
    const end = this.endDate;

    console.log('Loading utilization data for:', { start, end, filters: this.filters, role: this.userRole });

    // Load Top 3 Low Utilization
    this.reportsService.getTopLowUtilization(start, end, this.filters).subscribe({
      next: (res) => {
        this.topLowUtilizationUsers = res.data || [];
        console.log('Top Low Utilization Users:', this.topLowUtilizationUsers);
      },
      error: (err) => console.error('Error loading Top Low Utilization Users:', err)
    });

    // Load All Data and Categorize
    this.reportsService.getUtilizationDetails(start, end, this.filters).subscribe({
      next: (res) => {
        const allData = res.data || [];

        // Categorize
        this.fullyUtilizedUsers = allData.filter((u: UtilizationRecord) => u.utilizationPercentage >= 80);
        this.partiallyUtilizedUsers = allData.filter((u: UtilizationRecord) => u.utilizationPercentage >= 50 && u.utilizationPercentage < 80);
        this.zeroUtilizationUsers = allData.filter((u: UtilizationRecord) => u.utilizationPercentage < 50);

        this.filteredFullyUtilized = this.fullyUtilizedUsers;
        this.filteredPartiallyUtilized = this.partiallyUtilizedUsers;
        this.filteredZeroUtilization = this.zeroUtilizationUsers;

        console.log('Utilization Data Categorized:', {
          fully: this.fullyUtilizedUsers.length,
          partial: this.partiallyUtilizedUsers.length,
          zero: this.zeroUtilizationUsers.length
        });

        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading Utilization Details:', err);
        this.loading = false;
      }
    });
  }

  toggleAccordion(section: 'zero' | 'partial' | 'fully'): void {
    if (section === 'zero') this.isZeroOpen = !this.isZeroOpen;
    if (section === 'partial') this.isPartialOpen = !this.isPartialOpen;
    if (section === 'fully') this.isFullyOpen = !this.isFullyOpen;
  }

  formatDateForApi(date: Date): string {
    const year = date.getFullYear();
    const month = ('0' + (date.getMonth() + 1)).slice(-2);
    const day = ('0' + date.getDate()).slice(-2);
    return `${year}-${month}-${day}`;
  }

  onEmployeeIdClick(employeeId: number, record: UtilizationRecord): void {
    console.log('Employee ID clicked:', employeeId, record);
    alert(`Navigate to Employee Detail\n\nEmployee ID: ${employeeId}\nName: ${record.employeeName}\nUtilization: ${record.utilizationPercentage.toFixed(1)}%`);
  }

  onDepartmentClick(department: string, record: UtilizationRecord): void {
    console.log('Department clicked:', department, record);
    alert(`View ${department} Department\n\nEmployee: ${record.employeeName}\n\nFiltered utilization report for ${department}`);
  }

  goBack(): void {
    this.router.navigate(['/reports']);
  }

  formatDate(date: Date): string {
    return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
  }

  viewEmployeeDetails(record: UtilizationRecord): void {
    console.log('View details for:', record);
    alert(`View Details\n\nEmployee: ${record.employeeName}\nDepartment: ${record.department}\nUtilization: ${record.utilizationPercentage.toFixed(1)}%`);
  }

  optimizeUtilization(record: UtilizationRecord): void {
    console.log('Optimize utilization for:', record);
    alert(`Optimize Utilization\n\nEmployee: ${record.employeeName}\nCurrent Utilization: ${record.utilizationPercentage.toFixed(1)}%\n\nSuggested actions to improve utilization will be displayed here.`);
  }
}
