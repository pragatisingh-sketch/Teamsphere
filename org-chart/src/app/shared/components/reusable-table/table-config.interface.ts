import { MatIcon } from '@angular/material/icon';

// Interface for table column configuration
export interface TableColumn {
  key: string;
  label: string;
  sortable?: boolean;
  filterable?: boolean;
  searchable?: boolean;
  width?: string;
  minWidth?: string;
  maxWidth?: string;
  resizable?: boolean;
  type?: 'text' | 'number' | 'date' | 'boolean' | 'custom';
  format?: (value: any, row: any) => string;
  hidden?: boolean;
  disableColumnToggle?: boolean; // If true, column cannot be toggled
  template?: string; // Custom template reference
  clickable?: boolean; // If true, column cells will be clickable/linkable
  cellClick?: (value: any, row: any) => void; // Callback when cell is clicked
}

// Interface for table actions configuration
export interface TableAction {
  id: string;
  label: string;
  icon?: string;
  color?: 'primary' | 'accent' | 'warn';
  disabled?: (row: any) => boolean;
  hidden?: (row: any) => boolean;
  handler: (row: any) => void;
  confirm?: {
    title: string;
    message: string;
    confirmButtonText?: string;
    cancelButtonText?: string;
    showCommentField?: boolean;
    commentLabel?: string;
  };
}

// Interface for bulk actions configuration
export interface BulkAction {
  id: string;
  label: string;
  icon?: string;
  color?: 'primary' | 'accent' | 'warn';
  disabled?: (selectedRows: any[]) => boolean;
  handler: (selectedRows: any[]) => void;
  confirm?: {
    title: string;
    message?: string;
    confirmButtonText?: string;
    cancelButtonText?: string;
    showCommentField?: boolean;
    commentLabel?: string;
  };
}

// Interface for filter configuration
export interface FilterConfig {
  type: 'text' | 'select' | 'date' | 'multi-select' | 'custom';
  label: string;
  key: string;
  options?: Array<{ value: any; label: string }>; // For select and multi-select filters
  placeholder?: string;
  defaultValue?: any;
  customTemplate?: string;
}

// Interface for table configuration
export interface TableConfig {
  title?: string;
  subtitle?: string;
  columns: TableColumn[];
  actions?: TableAction[];
  bulkActions?: BulkAction[];
  filters?: FilterConfig[];
  showGlobalSearch?: boolean;
  showColumnToggle?: boolean;
  showColumnFilters?: boolean;
  showPagination?: boolean;
  showSelection?: boolean;
  pageSize?: number;
  pageSizeOptions?: number[];
  showExport?: boolean;
  exportFileName?: string;
  exportConfig?: {
    enabled: boolean;
    formats: ('csv' | 'excel')[];
    excelExportType?: string; // Strategy type for backend (e.g., TIME_ENTRY_DEFAULTER)
    csvFilename?: string;
    excelFilename?: string;
    startDate?: string; // Override date range if needed
    endDate?: string;
    filters?: any; // Override filters if needed
  };
  sortable?: boolean;
  filterable?: boolean;
  resizable?: boolean;
  scrollable?: boolean;
  stickyHeader?: boolean;
  height?: string;
  customClasses?: string[];
  emptyState?: {
    message: string;
    icon?: string;
    action?: {
      label: string;
      handler: () => void;
    };
  };
  rowClass?: (row: any) => string | string[];
  /**
   * Callback function triggered when a table row is clicked.
   * Common use cases:
   * 1. Navigate to detail page: (row) => this.router.navigate(['/detail', row.id])
   * 2. Open dialog/modal: (row) => this.dialog.open(DetailComponent, { data: row })
   * 3. Custom action: (row) => this.performAction(row)
   * 
   * Note: Row click events will not trigger when clicking on action buttons or checkboxes.
   */
  rowClick?: (row: any) => void;
  contextMenu?: TableAction[];
}

// Interface for table state
export interface TableState {
  filters: { [key: string]: any };
  globalSearch: string;
  sortBy: string;
  sortDirection: 'asc' | 'desc';
  pageIndex: number;
  pageSize: number;
  selectedRows: any[];
  displayedColumns: string[];
  columnFilters: { [key: string]: string[] };
  columnWidths: { [key: string]: number };
}

// Interface for table event
export interface TableEvent {
  type: 'rowClick' | 'cellClick' | 'action' | 'bulkAction' | 'filterChange' | 'searchChange' | 'sortChange' | 'pageChange' | 'selectionChange';
  data: any;
  row?: any;
  rows?: any[];
}