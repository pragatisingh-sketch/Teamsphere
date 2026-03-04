import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { TableConfig } from '../shared/components/reusable-table/table-config.interface';
import { DateFilterService, DateRange } from '../services/date-filter.service';

interface LeavesWFHRecord {
  employeeId: number;
  employeeName: string;
  department: string;
  leavesCount: number;
  wfhCount: number;
  totalDays: number;
}

@Component({
  selector: 'app-leaves-wfh-details',
  templateUrl: './leaves-wfh-details.component.html',
  styleUrls: ['./leaves-wfh-details.component.css']
})
export class LeavesWfhDetailsComponent implements OnInit, OnDestroy {

  leavesWFHData: LeavesWFHRecord[] = [];
  tableConfig: TableConfig = {} as TableConfig;
  loading = false;
  dateRange: DateRange | null = null;

  private dateSubscription?: Subscription;

  constructor(
    private router: Router,
    private dateFilterService: DateFilterService
  ) { }

  ngOnInit(): void {
    this.dateSubscription = this.dateFilterService.dateRange$.subscribe(range => {
      this.dateRange = range;
      console.log('Leaves & WFH Details - Date range updated:', range);
      this.loadLeavesWFHData();
    });

    this.initializeTableConfig();
  }

  ngOnDestroy(): void {
    if (this.dateSubscription) {
      this.dateSubscription.unsubscribe();
    }
  }

  initializeTableConfig(): void {
    this.tableConfig = {
      columns: [
        {
          key: 'employeeName',
          label: 'Employee Name',
          sortable: true,
          filterable: true,
          searchable: true,
          width: '200px',
          minWidth: '150px',
          resizable: true
        },
        {
          key: 'department',
          label: 'Team',
          sortable: true,
          filterable: true,
          searchable: true,
          width: '180px',
          minWidth: '140px',
          resizable: true,
          clickable: true,
          cellClick: (value: string, row: LeavesWFHRecord) => this.onDepartmentClick(value, row)
        },
        {
          key: 'leavesCount',
          label: 'Leaves',
          sortable: true,
          filterable: true,
          type: 'number',
          width: '140px',
          minWidth: '120px',
          resizable: true,
          format: (value: number) => {
            const color = value > 5 ? '#ff9800' : '#4caf50';
            return `<span style="color: ${color}; font-weight: 500;">${value}</span>`;
          }
        },
        {
          key: 'wfhCount',
          label: 'Work From Home',
          sortable: true,
          filterable: true,
          type: 'number',
          width: '170px',
          minWidth: '140px',
          resizable: true,
          format: (value: number) => `<span style="color: #2196f3; font-weight: 500;">${value}</span>`
        },
        {
          key: 'totalDays',
          label: 'Total Days',
          sortable: true,
          filterable: true,
          type: 'number',
          width: '140px',
          minWidth: '120px',
          resizable: true,
          format: (value: number) => `<span style="font-weight: 600; font-size: 16px;">${value}</span>`
        }
      ],
      showGlobalSearch: true,
      showColumnToggle: true,
      showColumnFilters: true,
      showPagination: true,
      showSelection: true,
      pageSize: 10,
      pageSizeOptions: [10, 25, 50, 100],
      showExport: true,
      exportFileName: 'leaves-wfh-details',
      sortable: true,
      filterable: true,
      resizable: true,
      actions: [
        {
          id: 'view',
          label: 'View Details',
          icon: 'visibility',
          color: 'primary',
          handler: (row: LeavesWFHRecord) => this.viewEmployeeDetails(row)
        },
        {
          id: 'approve',
          label: 'Manage Leave',
          icon: 'event_available',
          color: 'accent',
          handler: (row: LeavesWFHRecord) => this.manageLeave(row)
        }
      ]
    };
  }

  loadLeavesWFHData(): void {
    this.loading = true;

    // TODO: Replace with actual API call
    this.leavesWFHData = this.getDummyLeavesWFHData();

    this.loading = false;
  }

  getDummyLeavesWFHData(): LeavesWFHRecord[] {
    return [
      { employeeId: 1001, employeeName: 'John Doe', department: 'Engineering', leavesCount: 3, wfhCount: 5, totalDays: 8 },
      { employeeId: 1002, employeeName: 'Jane Smith', department: 'Engineering', leavesCount: 2, wfhCount: 4, totalDays: 6 },
      { employeeId: 1003, employeeName: 'Robert Johnson', department: 'Product', leavesCount: 5, wfhCount: 3, totalDays: 8 },
      { employeeId: 1004, employeeName: 'Sarah Wilson', department: 'Product', leavesCount: 1, wfhCount: 6, totalDays: 7 },
      { employeeId: 1005, employeeName: 'Michael Brown', department: 'Sales', leavesCount: 4, wfhCount: 2, totalDays: 6 },
      { employeeId: 1006, employeeName: 'Lisa Garcia', department: 'Design', leavesCount: 2, wfhCount: 7, totalDays: 9 },
      { employeeId: 1007, employeeName: 'Christopher Lee', department: 'Sales', leavesCount: 6, wfhCount: 1, totalDays: 7 },
      { employeeId: 1008, employeeName: 'Amanda Taylor', department: 'Marketing', leavesCount: 3, wfhCount: 4, totalDays: 7 },
      { employeeId: 1009, employeeName: 'David Lee', department: 'Executive', leavesCount: 2, wfhCount: 3, totalDays: 5 },
      { employeeId: 1010, employeeName: 'Jennifer Kim', department: 'Finance', leavesCount: 4, wfhCount: 5, totalDays: 9 },
      { employeeId: 1011, employeeName: 'Mark Rodriguez', department: 'HR', leavesCount: 1, wfhCount: 3, totalDays: 4 },
      { employeeId: 1012, employeeName: 'Emily Chen', department: 'Customer Success', leavesCount: 3, wfhCount: 6, totalDays: 9 }
    ];
  }

  onEmployeeIdClick(employeeId: number, record: LeavesWFHRecord): void {
    console.log('Employee ID clicked:', employeeId, record);
    alert(`Navigate to Employee Detail\n\nEmployee ID: ${employeeId}\nName: ${record.employeeName}\nLeaves: ${record.leavesCount} | WFH: ${record.wfhCount}`);
  }

  onDepartmentClick(department: string, record: LeavesWFHRecord): void {
    console.log('Department clicked:', department, record);
    alert(`View ${department} Department\n\nEmployee: ${record.employeeName}\n\nFiltered leaves/WFH report for ${department}`);
  }

  goBack(): void {
    this.router.navigate(['/reports']);
  }

  formatDate(date: Date): string {
    return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
  }

  viewEmployeeDetails(record: LeavesWFHRecord): void {
    console.log('View details for:', record);
    alert(`View Details\n\nEmployee: ${record.employeeName}\nDepartment: ${record.department}\nLeaves: ${record.leavesCount} | WFH: ${record.wfhCount}`);
  }

  manageLeave(record: LeavesWFHRecord): void {
    console.log('Manage leave for:', record);
    alert(`Manage Leave\n\nEmployee: ${record.employeeName}\nLeaves Taken: ${record.leavesCount}\nWFH Days: ${record.wfhCount}\n\nLeave management options will be displayed here.`);
  }
}
