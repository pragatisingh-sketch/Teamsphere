import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';

import { TableConfig, TableColumn, TableAction, BulkAction } from '../reusable-table/table-config.interface';

// Sample data interface
interface Employee {
  id: number;
  name: string;
  email: string;
  department: string;
  position: string;
  status: 'Active' | 'Inactive' | 'Pending';
  hireDate: string;
  salary: number;
  manager: string;
  location: string;
}

@Component({
  selector: 'app-example-usage',
  templateUrl: './example-usage.component.html',
  styleUrls: ['./example-usage.component.css']
})
export class ExampleUsageComponent implements OnInit {

  // Sample data
  employees: Employee[] = [
    {
      id: 1,
      name: 'John Doe',
      email: 'john.doe@company.com',
      department: 'Engineering',
      position: 'Senior Developer',
      status: 'Active',
      hireDate: '2020-03-15',
      salary: 75000,
      manager: 'Jane Smith',
      location: 'New York'
    },
    {
      id: 7,
      name: 'Christopher Lee',
      email: 'christopher.lee@company.com',
      department: 'Sales',
      position: 'Senior Sales Manager',
      status: 'Active',
      hireDate: '2016-05-18',
      salary: 95000,
      manager: 'David Lee',
      location: 'Chicago'
    },
    {
      id: 8,
      name: 'Amanda Taylor',
      email: 'amanda.taylor@company.com',
      department: 'Marketing',
      position: 'Marketing Director',
      status: 'Active',
      hireDate: '2014-03-25',
      salary: 88000,
      manager: 'Sarah Wilson',
      location: 'Boston'
    },
    {
      id: 9,
      name: 'David Lee',
      email: 'david.lee@company.com',
      department: 'Executive',
      position: 'VP of Operations',
      status: 'Active',
      hireDate: '2012-11-08',
      salary: 120000,
      manager: 'CEO',
      location: 'New York'
    },
    {
      id: 10,
      name: 'Jennifer Kim',
      email: 'jennifer.kim@company.com',
      department: 'Finance',
      position: 'Senior Financial Analyst',
      status: 'Active',
      hireDate: '2021-07-14',
      salary: 72000,
      manager: 'Michael Brown',
      location: 'Remote'
    },
    {
      id: 11,
      name: 'Mark Rodriguez',
      email: 'mark.rodriguez@company.com',
      department: 'HR',
      position: 'HR Business Partner',
      status: 'Active',
      hireDate: '2019-09-30',
      salary: 68000,
      manager: 'Sarah Wilson',
      location: 'San Francisco'
    },
    {
      id: 12,
      name: 'Emily Chen',
      email: 'emily.chen@company.com',
      department: 'Customer Success',
      position: 'Customer Success Manager',
      status: 'Active',
      hireDate: '2022-01-15',
      salary: 65000,
      manager: 'Jane Smith',
      location: 'Remote'
    },
    {
      id: 2,
      name: 'Jane Smith',
      email: 'jane.smith@company.com',
      department: 'Engineering',
      position: 'Tech Lead',
      status: 'Active',
      hireDate: '2018-07-22',
      salary: 85000,
      manager: 'Robert Johnson',
      location: 'San Francisco'
    },
    {
      id: 3,
      name: 'Robert Johnson',
      email: 'robert.johnson@company.com',
      department: 'Engineering',
      position: 'Engineering Manager',
      status: 'Active',
      hireDate: '2015-11-05',
      salary: 100000,
      manager: 'Sarah Wilson',
      location: 'New York'
    },
    {
      id: 4,
      name: 'Sarah Wilson',
      email: 'sarah.wilson@company.com',
      department: 'Product',
      position: 'Product Manager',
      status: 'Active',
      hireDate: '2019-01-30',
      salary: 80000,
      manager: 'Michael Brown',
      location: 'Boston'
    },
    {
      id: 5,
      name: 'Michael Brown',
      email: 'michael.brown@company.com',
      department: 'Product',
      position: 'Senior Product Manager',
      status: 'Inactive',
      hireDate: '2017-09-12',
      salary: 90000,
      manager: 'David Lee',
      location: 'Remote'
    },
    {
      id: 6,
      name: 'Lisa Garcia',
      email: 'lisa.garcia@company.com',
      department: 'Design',
      position: 'UX Designer',
      status: 'Pending',
      hireDate: '2023-02-20',
      salary: 65000,
      manager: 'Sarah Wilson',
      location: 'Chicago'
    }
  ];

  // Table configuration
  tableConfig: TableConfig = {
    title: 'Employee Directory',
    subtitle: 'Manage and view employee information across the organization',
    columns: [
      {
        key: 'id',
        label: 'Employee ID',
        sortable: true,
        filterable: true,
        searchable: true,
        width: '120px',
        minWidth: '100px',
        resizable: true,
        clickable: true, // Make this column clickable
        cellClick: (value: number, row: Employee) => this.onEmployeeIdClick(value, row)
      },
      {
        key: 'name',
        label: 'Name',
        sortable: true,
        filterable: true,
        searchable: true,
        type: 'text',
        width: '200px',
        minWidth: '150px',
        resizable: true
      },
      {
        key: 'email',
        label: 'Email',
        sortable: true,
        filterable: true,
        searchable: true,
        type: 'text',
        width: '250px',
        minWidth: '200px',
        resizable: true
      },
      {
        key: 'department',
        label: 'Department',
        sortable: true,
        filterable: true,
        searchable: true,
        type: 'text',
        width: '150px',
        minWidth: '120px',
        resizable: true,
        clickable: true, // Make department clickable
        cellClick: (value: string, row: Employee) => this.onDepartmentClick(value, row)
      },
      {
        key: 'position',
        label: 'Position',
        sortable: true,
        filterable: true,
        searchable: true,
        type: 'text',
        width: '180px',
        minWidth: '150px',
        resizable: true
      },
      {
        key: 'status',
        label: 'Status',
        sortable: true,
        filterable: true,
        searchable: true,
        type: 'text',
        width: '130px',
        minWidth: '100px',
        resizable: true,
        format: (value: string) => {
          const statusColors: { [key: string]: string } = {
            'Active': '<span class="status-active">●</span> Active',
            'Inactive': '<span class="status-inactive">●</span> Inactive',
            'Pending': '<span class="status-pending">●</span> Pending'
          };
          return statusColors[value] || value;
        }
      },
      {
        key: 'hireDate',
        label: 'Hire Date',
        sortable: true,
        filterable: true,
        searchable: false,
        type: 'date',
        width: '140px',
        minWidth: '120px',
        resizable: true,
        format: (value: string) => {
          const date = new Date(value);
          return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
        }
      },
      {
        key: 'salary',
        label: 'Salary',
        sortable: true,
        filterable: true,
        searchable: false,
        type: 'number',
        width: '140px',
        minWidth: '120px',
        resizable: true,
        format: (value: number) => `$${value.toLocaleString()}`
      },
      {
        key: 'manager',
        label: 'Manager',
        sortable: true,
        filterable: true,
        searchable: true,
        type: 'text',
        width: '200px',
        minWidth: '150px',
        resizable: true
      },
      {
        key: 'location',
        label: 'Location',
        sortable: true,
        filterable: true,
        searchable: true,
        type: 'text',
        width: '150px',
        minWidth: '120px',
        resizable: true
      }
    ],
    actions: [
      {
        id: 'view',
        label: 'View Details',
        icon: 'visibility',
        color: 'primary',
        handler: (row: Employee) => this.viewEmployee(row),
        disabled: (row: Employee) => false
      },
      {
        id: 'edit',
        label: 'Edit Employee',
        icon: 'edit',
        color: 'primary',
        handler: (row: Employee) => this.editEmployee(row),
        disabled: (row: Employee) => row.status === 'Inactive'
      },
      {
        id: 'deactivate',
        label: 'Deactivate',
        icon: 'person_remove',
        color: 'warn',
        handler: (row: Employee) => this.deactivateEmployee(row),
        disabled: (row: Employee) => row.status === 'Inactive',
        hidden: (row: Employee) => row.status === 'Pending'
      },
      {
        id: 'activate',
        label: 'Activate',
        icon: 'person_add',
        color: 'primary',
        handler: (row: Employee) => this.activateEmployee(row),
        disabled: (row: Employee) => row.status === 'Active',
        hidden: (row: Employee) => row.status !== 'Inactive'
      }
    ],
    bulkActions: [
      {
        id: 'activate_selected',
        label: 'Activate Selected',
        icon: 'person_add',
        color: 'primary',
        handler: (selectedRows: Employee[]) => this.bulkActivate(selectedRows),
        disabled: (selectedRows: Employee[]) => selectedRows.length === 0 || selectedRows.some(row => row.status === 'Active')
      },
      {
        id: 'deactivate_selected',
        label: 'Deactivate Selected',
        icon: 'person_remove',
        color: 'warn',
        handler: (selectedRows: Employee[]) => this.bulkDeactivate(selectedRows),
        disabled: (selectedRows: Employee[]) => selectedRows.length === 0 || selectedRows.some(row => row.status === 'Inactive')
      },
      {
        id: 'export_selected',
        label: 'Export Selected',
        icon: 'download',
        color: 'primary',
        handler: (selectedRows: Employee[]) => this.exportEmployees(selectedRows),
        disabled: (selectedRows: Employee[]) => selectedRows.length === 0
      }
    ],
    showGlobalSearch: true,
    showColumnToggle: true,
    showColumnFilters: true,
    showPagination: true,
    showSelection: true,
    pageSize: 5,
    pageSizeOptions: [5, 10, 25, 50],
    showExport: true,
    exportFileName: 'employees',
    sortable: true,
    filterable: true,
    resizable: true,
    scrollable: true,
    emptyState: {
      message: 'No employees found. Add a new employee to get started.',
      icon: 'person_add',
      action: {
        label: 'Add Employee',
        handler: () => this.addEmployee()
      }
    },
    rowClass: (row: Employee) => {
      const classes = [];
      if (row.status === 'Inactive') classes.push('status-inactive-row');
      if (row.status === 'Pending') classes.push('status-pending-row');
      return classes;
    }
    // Note: rowClick removed - using clickable columns instead
    // Individual columns (ID, Department) are clickable with cellClick handlers
  };

  constructor(
    private dialog: MatDialog,
    // Uncomment below to enable navigation:
    // private router: Router
  ) { }

  ngOnInit(): void {
    // You can also conditionally set rowClick based on permissions or other criteria
    // Example: this.tableConfig.rowClick = this.canViewDetails ? (row) => this.onEmployeeRowClick(row) : undefined;
  }

  // ========================================
  // Cell Click Handlers - Clickable Column Examples
  // ========================================

  /**
   * Handle click on Employee ID column
   * PATTERN 1: Navigate to employee detail page by ID
   */
  onEmployeeIdClick(employeeId: number, employee: Employee): void {
    console.log('Employee ID clicked:', employeeId, employee);

    // PATTERN 1: Navigate to detail page using Router
    // this.router.navigate(['/employees', employeeId]);

    // For demonstration, show alert
    alert(`Navigate to Employee Detail\n\nEmployee ID: ${employeeId}\nName: ${employee.name}\n\nIn a real app, this would navigate to:\n/employees/${employeeId}`);
  }

  /**
   * Handle click on Department column
   * PATTERN 2: Navigate to filtered list or department dashboard
   */
  onDepartmentClick(department: string, employee: Employee): void {
    console.log('Department clicked:', department, employee);

    // PATTERN 2: Navigate to department dashboard or filtered list
    // this.router.navigate(['/departments', department]);
    // OR
    // this.router.navigate(['/employees'], { queryParams: { department } });

    // For demonstration, show alert
    alert(`View ${department} Department\n\nClicked from employee: ${employee.name}\n\nIn a real app, this would:\n- Navigate to /departments/${department}\n- Or show filtered employee list\n- Or open department dashboard`);
  }

  // ========================================
  // Action Handlers (for action buttons in the table)
  // ========================================

  /**
   * View employee details via action button
   * This is different from row click - this is triggered by the "View" action button
   */
  viewEmployee(employee: Employee): void {
    console.log('View action button clicked:', employee);
    // In a real app, you might open a dialog or navigate to a detail page
    alert(`Viewing details for ${employee.name}`);
  }

  editEmployee(employee: Employee): void {
    console.log('Edit employee:', employee);
    alert(`Editing ${employee.name}`);
  }

  deactivateEmployee(employee: Employee): void {
    console.log('Deactivate employee:', employee);
    alert(`Deactivating ${employee.name}`);
    // Update the employee status
    employee.status = 'Inactive';
  }

  activateEmployee(employee: Employee): void {
    console.log('Activate employee:', employee);
    alert(`Activating ${employee.name}`);
    // Update the employee status
    employee.status = 'Active';
  }

  addEmployee(): void {
    console.log('Add new employee');
    alert('Add new employee functionality would open here');
  }

  refreshData(): void {
    console.log('Refresh data');
    alert('Data refreshed successfully');
  }

  resetTable(): void {
    console.log('Reset table');
    alert('Table reset to default state');
  }

  bulkActivate(employees: Employee[]): void {
    console.log('Bulk activate employees:', employees);
    alert(`Activating ${employees.length} employees`);
    employees.forEach(emp => emp.status = 'Active');
  }

  bulkDeactivate(employees: Employee[]): void {
    console.log('Bulk deactivate employees:', employees);
    alert(`Deactivating ${employees.length} employees`);
    employees.forEach(emp => emp.status = 'Inactive');
  }

  exportEmployees(employees: Employee[]): void {
    console.log('Export employees:', employees);
    alert(`Exporting ${employees.length} employees`);
  }

  // ========================================
  // Table Event Handler (optional)
  // ========================================

  /**
   * Central event handler for table events
   * Note: You can handle events either here OR in the config callbacks
   * The config callbacks (like rowClick, action.handler) are usually more convenient
   */
  onTableEvent(event: any): void {
    console.log('Table event:', event);

    switch (event.type) {
      case 'rowClick':
        // RowClick is already handled in config.rowClick
        // This event is emitted for logging/tracking purposes
        console.log('Row clicked (event):', event.row);
        break;
      case 'action':
        // Actions are handled directly in the config
        break;
      case 'bulkAction':
        // Bulk actions are handled directly in the config
        break;
      case 'selectionChange':
        console.log('Selected rows:', event.selectedRows);
        break;
      case 'searchChange':
        console.log('Search value:', event.searchValue);
        break;
    }
  }
}