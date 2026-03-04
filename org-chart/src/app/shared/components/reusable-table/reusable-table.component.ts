import { Component, Input, Output, EventEmitter, OnInit, AfterViewInit, OnDestroy, ViewChild, ElementRef, TemplateRef, ViewChildren, QueryList, OnChanges, SimpleChanges } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatMenuTrigger } from '@angular/material/menu';
import { SelectionModel } from '@angular/cdk/collections';
import { MatCheckboxChange } from '@angular/material/checkbox';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { TableConfig, TableColumn, TableAction, BulkAction, FilterConfig, TableState, TableEvent } from './table-config.interface';
import { ExportService } from '../../../services/export.service';
import { DateFilterService } from '../../../services/date-filter.service';

@Component({
  selector: 'app-reusable-table',
  templateUrl: './reusable-table.component.html',
  styleUrls: ['./reusable-table.component.css']
})
export class ReusableTableComponent implements OnInit, AfterViewInit, OnDestroy, OnChanges {
  @Input() tableId: string = 'default-table';
  @Input() config: TableConfig = {
    columns: [],
    showGlobalSearch: true,
    showColumnToggle: true,
    showColumnFilters: true,
    showPagination: true,
    showSelection: false,
    pageSize: 10,
    pageSizeOptions: [5, 10, 25, 50],
    showExport: false
  };

  @Input() data: any[] = [];
  @Output() tableEvent = new EventEmitter<TableEvent>();
  @Output() dataChange = new EventEmitter<any[]>();

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;
  @ViewChildren('columnFilterTrigger') columnFilterTriggers!: QueryList<MatMenuTrigger>;

  dataSource = new MatTableDataSource<any>([]);
  selection = new SelectionModel<any>(true, []);

  // Column resizing properties
  private startX = 0;
  private startWidth = 0;
  private resizingColumn: string | null = null;
  private documentListeners: (() => void)[] = [];

  // State management
  state: TableState = {
    filters: {},
    globalSearch: '',
    sortBy: '',
    sortDirection: 'asc',
    pageIndex: 0,
    pageSize: 10,
    selectedRows: [],
    displayedColumns: [],
    columnFilters: {},
    columnWidths: {}
  };

  // Column filtering properties
  columnUniqueValues: { [key: string]: string[] } = {};
  showColumnFilters: boolean = false;
  currentFilterMenuState = {
    columnKey: null as string | null,
    tempSelectedValues: [] as string[],
    searchText: ''
  };
  private currentTrigger: MatMenuTrigger | null = null;

  // Column toggle properties
  columnSearchText: string = '';
  allColumnsSelected: boolean = true;
  columnDisplayNames: { [key: string]: string } = {};

  // Search debouncing
  private searchSubject = new Subject<string>();

  // Filter menu options for search
  get filteredMenuOptions(): string[] {
    if (!this.currentFilterMenuState.columnKey) return [];
    const uniqueValues = this.getUniqueColumnValues(this.currentFilterMenuState.columnKey);
    const searchTextLower = this.currentFilterMenuState.searchText.trim().toLowerCase();
    return searchTextLower ?
      uniqueValues.filter(value => value.toLowerCase().includes(searchTextLower)) :
      uniqueValues;
  }

  constructor(
    private exportService: ExportService,
    private dateFilterService: DateFilterService
  ) { }

  ngOnInit(): void {
    this.initializeTable();
    this.setupSearchDebouncing();
    this.loadState();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['data']) {
      this.dataSource.data = this.data || [];
      this.initializeColumnFilters();
      if (this.dataSource.paginator) {
        this.dataSource.paginator.firstPage();
      }
    }
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
    this.detectTableScroll();

    // Add resize listener
    window.addEventListener('resize', () => this.detectTableScroll());
  }

  ngOnDestroy(): void {
    window.removeEventListener('resize', () => this.detectTableScroll());
    this.searchSubject.complete();
  }

  private initializeTable(): void {
    // Build column display names
    this.config.columns.forEach(column => {
      this.columnDisplayNames[column.key] = column.label;
      // Set default resizable to true if not specified
      if (column.resizable === undefined) {
        column.resizable = true;
      }
    });

    // Set default displayed columns
    this.state.displayedColumns = this.config.columns
      .filter(col => !col.hidden)
      .map(col => col.key);

    // Add selection column if enabled
    if (this.config.showSelection) {
      this.state.displayedColumns.unshift('select');
    }

    // Add actions column if actions are defined
    if (this.config.actions && this.config.actions.length > 0) {
      this.state.displayedColumns.push('actions');
    }

    // Initialize data source
    this.dataSource.data = this.data;
    this.initializeColumnFilters();

    // Set up sorting
    this.dataSource.sortingDataAccessor = (item, header) => {
      return this.getNestedValue(item, header);
    };

    // Set up filter predicate
    this.dataSource.filterPredicate = this.createFilterPredicate();

    // Load column widths from state or apply defaults
    this.applyColumnWidths();
  }

  private setupSearchDebouncing(): void {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(searchValue => {
      this.applyGlobalSearchInternal(searchValue);
    });
  }

  private loadState(): void {
    // Load saved state from localStorage or use defaults
    const savedState = localStorage.getItem(`reusableTableState_${this.tableId}`);
    if (savedState) {
      try {
        const parsedState = JSON.parse(savedState);
        this.state = { ...this.state, ...parsedState };
        this.showColumnFilters = this.state.filters['showColumnFilters'] || false;
      } catch (e) {
        console.warn('Failed to parse saved table state:', e);
      }
    }

    // Update displayed columns based on state
    if (this.state.displayedColumns.length > 0) {
      this.state.displayedColumns = this.state.displayedColumns.filter(col =>
        this.config.columns.find(c => c.key === col) ||
        (col === 'select' && this.config.showSelection) ||
        (col === 'actions' && this.config.actions && this.config.actions.length > 0)
      );
    }
  }

  private saveState(): void {
    localStorage.setItem(`reusableTableState_${this.tableId}`, JSON.stringify(this.state));
  }

  private detectTableScroll(): void {
    setTimeout(() => {
      const tableContainer = document.querySelector('.table-responsive') as HTMLElement;
      if (tableContainer) {
        const isScrollable = tableContainer.scrollWidth > tableContainer.clientWidth;
        if (isScrollable) {
          tableContainer.classList.add('scrollable');
        } else {
          tableContainer.classList.remove('scrollable');
        }
      }

      // Also check if individual columns need resizing
      this.adjustColumnWidthsIfNeeded();
    }, 100);
  }

  private adjustColumnWidthsIfNeeded(): void {
    const headerRow = document.querySelector('.reusable-table mat-header-row') as HTMLElement;
    if (!headerRow) return;

    const cells = headerRow.querySelectorAll('mat-header-cell');
    const containerWidth = document.querySelector('.table-responsive')?.clientWidth || 0;
    const totalColumnWidth = Array.from(cells).reduce((sum, cell) => sum + (cell as HTMLElement).offsetWidth, 0);

    // If columns are too narrow, distribute available space
    if (containerWidth > totalColumnWidth) {
      const availableSpace = containerWidth - totalColumnWidth;
      const resizableColumns = this.config.columns.filter(col => col.resizable && this.isColumnDisplayed(col.key));

      if (resizableColumns.length > 0) {
        const extraWidthPerColumn = availableSpace / resizableColumns.length;

        resizableColumns.forEach(column => {
          const columnIndex = this.state.displayedColumns.indexOf(column.key);
          const cell = cells[columnIndex] as HTMLElement;

          if (cell) {
            const currentWidth = cell.offsetWidth;
            const newWidth = Math.max(currentWidth + extraWidthPerColumn,
              this.getColumnMinWidth(column.key));
            cell.style.width = `${newWidth}px`;
            this.updateColumnWidth(column.key, newWidth);
          }
        });
      }
    }
  }

  // Column filtering methods
  private initializeColumnFilters(): void {
    this.columnUniqueValues = {};

    this.config.columns.forEach(column => {
      if (column.filterable && column.key !== 'actions' && column.key !== 'select') {
        const values = Array.from(new Set(
          this.dataSource.data
            .map(item => String(this.getNestedValue(item, column.key)))
            .filter(value => value !== null && value !== undefined && value !== '')
        )).sort();

        this.columnUniqueValues[column.key] = values;
      }
    });
  }

  openFilterMenu(columnKey: string, trigger: MatMenuTrigger): void {
    this.currentTrigger = trigger;
    this.currentFilterMenuState.columnKey = columnKey;
    this.currentFilterMenuState.tempSelectedValues =
      this.state.columnFilters[columnKey] ? [...this.state.columnFilters[columnKey]] : [];
    this.currentFilterMenuState.searchText = '';
  }

  isTempSelected(value: string): boolean {
    return this.currentFilterMenuState.tempSelectedValues.includes(value);
  }

  toggleTempSelection(value: string, checked: boolean): void {
    const index = this.currentFilterMenuState.tempSelectedValues.indexOf(value);
    if (checked && index === -1) {
      this.currentFilterMenuState.tempSelectedValues.push(value);
    } else if (!checked && index !== -1) {
      this.currentFilterMenuState.tempSelectedValues.splice(index, 1);
    }
  }

  toggleSelectAllTemp(checked: boolean): void {
    if (this.currentFilterMenuState.columnKey) {
      this.currentFilterMenuState.tempSelectedValues = checked ?
        [...this.getUniqueColumnValues(this.currentFilterMenuState.columnKey)] : [];
    }
  }

  isAllTempSelected(): boolean {
    return this.currentFilterMenuState.columnKey ?
      this.currentFilterMenuState.tempSelectedValues.length ===
      this.getUniqueColumnValues(this.currentFilterMenuState.columnKey).length : false;
  }

  isSomeTempSelected(): boolean {
    return this.currentFilterMenuState.tempSelectedValues.length > 0 &&
      !this.isAllTempSelected();
  }

  getUniqueColumnValues(columnKey: string): string[] {
    return this.columnUniqueValues[columnKey] || [];
  }

  isFilterActive(columnKey: string): boolean {
    return this.state.columnFilters[columnKey]?.length > 0;
  }

  onFilterApplied(): void {
    if (this.currentFilterMenuState.columnKey) {
      const key = this.currentFilterMenuState.columnKey;
      this.state.columnFilters[key] = [...this.currentFilterMenuState.tempSelectedValues];
      this.applyColumnFilters();
      this.saveState();

      // Keep the menu open
      const trigger = this.currentTrigger;
      if (trigger) {
        setTimeout(() => {
          trigger.openMenu();
          this.currentFilterMenuState.tempSelectedValues = this.state.columnFilters[key] || [];
        });
      }
    }
  }

  clearColumnFilter(): void {
    if (this.currentFilterMenuState.columnKey) {
      const key = this.currentFilterMenuState.columnKey;
      this.state.columnFilters[key] = [];
      this.currentFilterMenuState.tempSelectedValues = [];
      this.applyColumnFilters();
      this.saveState();

      // Keep the menu open after clearing
      const trigger = this.currentTrigger;
      if (trigger) {
        setTimeout(() => {
          trigger.openMenu();
        });
      }
    }
  }

  private applyColumnFilters(): void {
    this.dataSource.filter = this.buildFilterString();
    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  // Column toggle methods
  getFilteredColumns(): string[] {
    if (!this.columnSearchText) {
      return this.config.columns.map(col => col.key);
    }

    return this.config.columns
      .filter(column => this.columnDisplayNames[column.key].toLowerCase()
        .includes(this.columnSearchText.toLowerCase()))
      .map(col => col.key);
  }

  toggleAllColumns(checked: boolean): void {
    this.allColumnsSelected = checked;
    const filteredColumns = this.getFilteredColumns();

    if (checked) {
      // Add all filtered columns
      filteredColumns.forEach(column => {
        if (!this.state.displayedColumns.includes(column) &&
          !this.config.columns.find(col => col.key === column)?.disableColumnToggle) {
          this.state.displayedColumns.push(column);
        }
      });
    } else {
      // Remove all filtered columns except select and actions
      this.state.displayedColumns = this.state.displayedColumns.filter(column =>
        !filteredColumns.includes(column) ||
        column === 'select' ||
        column === 'actions' ||
        this.config.columns.find(col => col.key === column)?.disableColumnToggle
      );
    }

    this.updateAllColumnsSelectedState();
    this.saveState();
    this.detectTableScroll();
  }

  toggleColumn(column: string): void {
    const index = this.state.displayedColumns.indexOf(column);

    if (index === -1) {
      // Add the column
      const configColumn = this.config.columns.find(col => col.key === column);
      if (configColumn && !configColumn.disableColumnToggle) {
        // Find where to insert the column (maintain original order)
        let insertIndex = this.config.showSelection ? 1 : 0; // After select column
        for (let i = insertIndex; i < this.state.displayedColumns.length - (this.config.actions ? 1 : 0); i++) {
          const colKey = this.state.displayedColumns[i];
          const currentColumn = this.config.columns.find(col => col.key === colKey);
          if (currentColumn && this.config.columns.indexOf(currentColumn) >
            this.config.columns.indexOf(configColumn)) {
            break;
          }
          insertIndex = i + 1;
        }
        this.state.displayedColumns.splice(insertIndex, 0, column);
      }
    } else {
      // Remove the column
      this.state.displayedColumns.splice(index, 1);
    }

    this.updateAllColumnsSelectedState();
    this.saveState();
    this.detectTableScroll();
  }

  isColumnDisplayed(column: string): boolean {
    return this.state.displayedColumns.includes(column);
  }

  isColumnDisabled(column: string): boolean {
    if (column === 'select' || column === 'actions') {
      return true;
    }
    const configColumn = this.config.columns.find(c => c.key === column);
    return configColumn?.disableColumnToggle || false;
  }

  updateAllColumnsSelectedState(): void {
    const filteredColumns = this.getFilteredColumns();
    const selectableFilteredColumns = filteredColumns.filter(col =>
      col !== 'select' &&
      col !== 'actions' &&
      !this.config.columns.find(c => c.key === col)?.disableColumnToggle
    );

    const allFilteredColumnsSelected = selectableFilteredColumns.every(col =>
      this.state.displayedColumns.includes(col)
    );

    this.allColumnsSelected = allFilteredColumnsSelected;
  }

  // Global search
  applyGlobalSearch(event: Event): void {
    const searchValue = (event.target as HTMLInputElement).value;
    this.searchSubject.next(searchValue);
  }

  private applyGlobalSearchInternal(searchValue: string): void {
    this.state.globalSearch = searchValue.trim().toLowerCase();
    this.dataSource.filter = this.buildFilterString();

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }

    this.emitTableEvent('searchChange', { searchValue: this.state.globalSearch });
  }

  // Selection methods
  toggleRow(row: any, event: MatCheckboxChange): void {
    this.selection.toggle(row);
    this.updateSelectedRows();
  }

  toggleAllRows(event: MatCheckboxChange): void {
    if (event.checked) {
      this.selection.select(...this.dataSource.data);
    } else {
      this.selection.clear();
    }
    this.updateSelectedRows();
  }

  private updateSelectedRows(): void {
    this.state.selectedRows = this.selection.selected;
    this.emitTableEvent('selectionChange', { selectedRows: this.state.selectedRows });
  }

  isAllSelected(): boolean {
    return this.selection.selected.length === this.dataSource.data.length;
  }

  isSomeSelected(): boolean {
    return this.selection.selected.length > 0 && !this.isAllSelected();
  }

  // Action handlers
  onRowClick(row: any): void {
    if (this.config.rowClick) {
      this.config.rowClick(row);
    } else {
      this.emitTableEvent('rowClick', row);
    }
  }

  onCellClick(event: Event, column: TableColumn, row: any): void {
    // Stop propagation to prevent row click from firing
    event.stopPropagation();

    const value = this.getNestedValue(row, column.key);

    if (column.cellClick) {
      column.cellClick(value, row);
    }

    this.emitTableEvent('cellClick', { column: column.key, value, row });
  }

  onAction(action: TableAction, row: any): void {
    if (action.confirm) {
      this.showConfirmationDialog(action, row);
    } else {
      action.handler(row);
      this.emitTableEvent('action', { action: action.id, row });
    }
  }

  onBulkAction(action: BulkAction): void {
    if (action.confirm) {
      this.showBulkConfirmationDialog(action);
    } else {
      action.handler(this.state.selectedRows);
      this.emitTableEvent('bulkAction', { action: action.id, rows: this.state.selectedRows });
    }
  }

  private showConfirmationDialog(action: TableAction, row: any): void {
    // This would typically use a dialog service
    // For now, we'll use a simple confirm dialog
    const message = action.confirm!.message || `Are you sure you want to ${action.label.toLowerCase()}?`;
    if (confirm(message)) {
      action.handler(row);
      this.emitTableEvent('action', { action: action.id, row });
    }
  }

  private showBulkConfirmationDialog(action: BulkAction): void {
    const message = action.confirm!.message || `Are you sure you want to ${action.label.toLowerCase()}?`;
    if (confirm(message)) {
      action.handler(this.state.selectedRows);
      this.emitTableEvent('bulkAction', { action: action.id, rows: this.state.selectedRows });
    }
  }

  // Export functionality
  supportsFormat(format: 'csv' | 'excel'): boolean {
    if (this.config.exportConfig?.enabled) {
      return this.config.exportConfig.formats.includes(format);
    }
    // Fallback to old behavior - if showExport is true, support CSV
    return this.config.showExport === true && format === 'csv';
  }

  exportCsv(): void {
    const exportData = this.dataSource.data.map(item => {
      const exportRow: any = {};
      this.config.columns.forEach(column => {
        if (column.key !== 'actions' && column.key !== 'select') {
          exportRow[column.label] = this.getNestedValue(item, column.key);
        }
      });
      return exportRow;
    });

    // Simple CSV export
    const csvContent = this.convertToCSV(exportData);
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });

    const fileName = this.config.exportConfig?.csvFilename ||
      this.config.exportFileName ||
      'export';
    this.downloadFile(blob, `${fileName.replace('.csv', '')}_${new Date().toISOString().split('T')[0]}.csv`);
  }

  exportExcel(): void {
    if (!this.config.exportConfig?.excelExportType) {
      console.error('Excel export type not configured');
      return;
    }

    // Get date range from DateFilterService or use config overrides
    this.dateFilterService.dateRange$.subscribe(dateRange => {
      const startDate = this.config.exportConfig?.startDate ||
        this.formatDateForApi(dateRange.start);
      const endDate = this.config.exportConfig?.endDate ||
        this.formatDateForApi(dateRange.end);
      const filters = this.config.exportConfig?.filters || {};

      this.exportService.exportExcel(
        this.config.exportConfig!.excelExportType!,
        startDate,
        endDate,
        filters
      ).subscribe({
        next: (blob) => {
          const fileName = this.config.exportConfig?.excelFilename || 'export.xlsx';
          this.downloadFile(blob, fileName);
        },
        error: (error) => {
          console.error('Error exporting Excel:', error);
          alert('Failed to export Excel file. Please try again.');
        }
      });
    }).unsubscribe(); // Unsubscribe immediately after getting value
  }

  private downloadFile(blob: Blob, filename: string): void {
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = filename;
    link.click();
    URL.revokeObjectURL(link.href);
  }

  private formatDateForApi(date: Date): string {
    const year = date.getFullYear();
    const month = ('0' + (date.getMonth() + 1)).slice(-2);
    const day = ('0' + date.getDate()).slice(-2);
    return `${year}-${month}-${day}`;
  }

  // Legacy export method for backward compatibility
  exportData(): void {
    this.exportCsv();
  }

  private convertToCSV(objArray: any[]): string {
    const array = typeof objArray !== 'object' ? JSON.parse(objArray) : objArray;
    let str = '';

    if (array.length > 0) {
      const headers = Object.keys(array[0]);
      str += headers.join(',') + '\r\n';

      array.forEach((item: any) => {
        const values = headers.map(header => {
          const value = item[header];
          return value === null || value === undefined ? '' : value.toString().replace(/"/g, '""');
        });
        str += '"' + values.join('","') + '"\r\n';
      });
    }

    return str;
  }

  // Utility methods
  private getNestedValue(obj: any, path: string): any {
    return path.split('.').reduce((current, key) => current?.[key], obj);
  }

  private buildFilterString(): string {
    const filters: any = {};

    // Global search
    if (this.state.globalSearch) {
      filters.globalSearch = this.state.globalSearch;
    }

    // Column filters
    Object.keys(this.state.columnFilters).forEach(key => {
      if (this.state.columnFilters[key].length > 0) {
        filters[key] = this.state.columnFilters[key];
      }
    });

    return Object.keys(filters).length > 0 ? JSON.stringify(filters) : '';
  }

  private createFilterPredicate(): (data: any, filter: string) => boolean {
    return (data: any, filter: string): boolean => {
      if (!filter) return true;

      try {
        const filterObj = JSON.parse(filter);

        if (typeof filterObj === 'string') {
          // Global search
          return Object.values(data).some(value =>
            value?.toString().toLowerCase().includes(filterObj.toLowerCase())
          );
        }

        // Column-specific filters
        for (const key in filterObj) {
          if (filterObj.hasOwnProperty(key)) {
            const value = this.getNestedValue(data, key);

            if (Array.isArray(filterObj[key])) {
              // Multi-select filter
              if (!filterObj[key].some((filterValue: string) =>
                value?.toString().toLowerCase().includes(filterValue.toLowerCase())
              )) {
                return false;
              }
            } else if (typeof filterObj[key] === 'string' && filterObj[key].trim() !== '') {
              // Text filter
              if (!value?.toString().toLowerCase().includes(filterObj[key].toLowerCase())) {
                return false;
              }
            }
          }
        }
        return true;
      } catch (e) {
        // Fallback to simple string search
        const simpleFilter = filter.toLowerCase();
        return Object.values(data).some(value =>
          value?.toString().toLowerCase().includes(simpleFilter)
        );
      }
    };
  }

  private emitTableEvent(type: TableEvent['type'], data: any, row?: any, rows?: any[]): void {
    const tableEvent: TableEvent = { type, data, row, rows };
    this.tableEvent.emit(tableEvent);
  }

  // Pagination and sorting
  onPageChange(event: any): void {
    this.state.pageIndex = event.pageIndex;
    this.state.pageSize = event.pageSize;
    this.saveState();
    this.emitTableEvent('pageChange', event);
  }

  onSortChange(event: any): void {
    this.state.sortBy = event.active;
    this.state.sortDirection = event.direction;
    this.saveState();
    this.emitTableEvent('sortChange', event);
  }

  // Toggle column filters
  toggleColumnFilters(): void {
    this.showColumnFilters = !this.showColumnFilters;
    this.state.filters['showColumnFilters'] = this.showColumnFilters;
    this.saveState();
  }

  // Get row class
  getRowClass(row: any): string | string[] | undefined {
    return this.config.rowClass ? this.config.rowClass(row) : undefined;
  }

  // Get cell value with formatting
  getCellValue(row: any, column: TableColumn): string {
    const value = this.getNestedValue(row, column.key);

    if (column.format && typeof column.format === 'function') {
      return column.format(value, row);
    }

    return value || '';
  }

  // Column resizing methods
  onResizeMouseDown(event: MouseEvent, columnKey: string): void {
    event.preventDefault();

    const headerCell = this.getHeaderCell(columnKey);
    if (!headerCell) return;

    this.resizingColumn = columnKey;
    this.startX = event.clientX;
    this.startWidth = headerCell.offsetWidth;

    // Add global event listeners
    const mouseMoveListener = (e: MouseEvent) => this.onResizeMouseMove(e);
    const mouseUpListener = () => this.onResizeMouseUp();

    document.addEventListener('mousemove', mouseMoveListener);
    document.addEventListener('mouseup', mouseUpListener);

    // Store cleanup functions
    this.documentListeners.push(() => document.removeEventListener('mousemove', mouseMoveListener));
    this.documentListeners.push(() => document.removeEventListener('mouseup', mouseUpListener));

    // Add resizing class
    headerCell.classList.add('resizing');
  }

  private onResizeMouseMove(event: MouseEvent): void {
    if (!this.resizingColumn) return;

    const deltaX = event.clientX - this.startX;
    const newWidth = Math.max(
      this.getColumnMinWidth(this.resizingColumn),
      this.startWidth + deltaX
    );

    // Update ALL cells in this column (header + data cells)
    this.setColumnWidth(this.resizingColumn, newWidth);
    this.updateColumnWidth(this.resizingColumn, newWidth);
  }

  private onResizeMouseUp(): void {
    if (this.resizingColumn) {
      const headerCell = this.getHeaderCell(this.resizingColumn);
      if (headerCell) {
        headerCell.classList.remove('resizing');
      }

      this.saveColumnWidths();
      this.resizingColumn = null;
    }

    // Clean up document listeners
    this.documentListeners.forEach(cleanup => cleanup());
    this.documentListeners = [];
  }

  private getHeaderCell(columnKey: string): HTMLElement | null {
    const index = this.state.displayedColumns.indexOf(columnKey);
    if (index === -1) return null;

    const headerRow = document.querySelector('.reusable-table mat-header-row') as HTMLElement;
    if (!headerRow) return null;

    const cells = headerRow.querySelectorAll('mat-header-cell');
    return cells[index] as HTMLElement;
  }

  /**
   * Get all data cells for a specific column
   */
  private getDataCells(columnKey: string): HTMLElement[] {
    const index = this.state.displayedColumns.indexOf(columnKey);
    if (index === -1) return [];

    const rows = document.querySelectorAll('.reusable-table mat-row');
    const dataCells: HTMLElement[] = [];

    rows.forEach(row => {
      const cells = row.querySelectorAll('mat-cell');
      if (cells[index]) {
        dataCells.push(cells[index] as HTMLElement);
      }
    });

    return dataCells;
  }

  /**
   * Set width for all cells in a column (header + data cells)
   */
  private setColumnWidth(columnKey: string, width: number): void {
    const widthPx = `${width}px`;

    // Update header cell
    const headerCell = this.getHeaderCell(columnKey);
    if (headerCell) {
      headerCell.style.width = widthPx;
      headerCell.style.minWidth = widthPx;
      headerCell.style.maxWidth = widthPx;
    }

    // Update all data cells
    const dataCells = this.getDataCells(columnKey);
    dataCells.forEach(cell => {
      cell.style.width = widthPx;
      cell.style.minWidth = widthPx;
      cell.style.maxWidth = widthPx;
    });
  }

  private getColumnMinWidth(columnKey: string): number {
    const column = this.config.columns.find(col => col.key === columnKey);
    if (!column || !column.minWidth) return 50;

    // Parse minWidth (e.g., "100px" -> 100)
    const match = column.minWidth.match(/(\d+)px/);
    return match ? parseInt(match[1], 10) : 50;
  }

  private updateColumnWidth(columnKey: string, width: number): void {
    if (!this.state.columnWidths) {
      this.state.columnWidths = {};
    }
    this.state.columnWidths[columnKey] = width;
  }

  private applyColumnWidths(): void {
    if (!this.state.columnWidths) return;

    this.state.displayedColumns.forEach(columnKey => {
      if (columnKey === 'select' || columnKey === 'actions') return;

      const width = this.state.columnWidths?.[columnKey];
      if (width) {
        this.setColumnWidth(columnKey, width);
      }
    });
  }

  private saveColumnWidths(): void {
    const savedState = localStorage.getItem('reusableTableState');
    let state = savedState ? JSON.parse(savedState) : {};

    state.columnWidths = this.state.columnWidths;
    localStorage.setItem('reusableTableState', JSON.stringify(state));
  }
}