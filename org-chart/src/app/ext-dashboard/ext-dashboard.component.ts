import { Component, OnInit, HostListener, ElementRef } from '@angular/core';
import * as Highcharts from 'highcharts';
import { UserService } from '../user.service';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { FormControl } from '@angular/forms';
import { Observable } from 'rxjs';
import { map, startWith } from 'rxjs/operators';

interface SeriesData {
  name: string;
  data: number[];
}

@Component({
  selector: 'app-ext-dashboard',
  templateUrl: './ext-dashboard.component.html',
  styleUrls: ['./ext-dashboard.component.css'],
})
export class ExtDashboardComponent implements OnInit {
  Highcharts: typeof Highcharts = Highcharts;

  // Chart configuration objects
  columnChartOptions: Highcharts.Options = {};
  pieChartOptions: Highcharts.Options = {};

  // Flags to force chart updates
  updateFlagColumn: boolean = false;
  updateFlagPie: boolean = false;
  pieData: any;
  series: SeriesData[] = [];
  categories: any;
  rowTotals: any;
  columnTotals: any;

  // Column visibility and collapse state
  columnVisibility: { [key: string]: boolean } = {};
  collapsedColumns: { [key: string]: boolean } = {};
  draggedColumn: string | null = null;

  // Column search functionality
  columnSearchControl = new FormControl('');
  filteredColumns!: Observable<SeriesData[]>;

  // Column resizing
  columnWidths: { [key: string]: string } = {};
  isResizing = false;
  currentResizeColumn = '';
  startX = 0;
  startWidth = 0;
  resizingElement: HTMLElement | null = null;

  // Fullscreen states
  isPieFullscreen = false;
  isColumnFullscreen = false;

  constructor(
    private userService: UserService,
    private elementRef: ElementRef
  ) {}

  ngOnInit(): void {
    this.loadChartData();
  }

  // Fetch data from the backend
  loadChartData() {
    this.userService.getChartData().subscribe({
      next: (response: any) => {
        // Validate received data
        this.pieData = response.data.pieData;
        this.series = response.data.series;
        this.categories = response.data.categories;

        if (!response.data || !response.data.categories || !response.data.series || !response.data.pieData) {
          console.error('Invalid chart data received:', response.data);
          return;
        }

        // Initialize column states after data is loaded
        this.initializeColumnStates();
        this.setupColumnSearch();

        // Configure the column chart
        this.columnChartOptions = {
          chart: {
            type: 'column',
          },
          credits: {
            enabled: false
          },
          title: {
            text: 'Employee Distribution',
          },
          xAxis: {
            categories: response.data.categories,
          },
          yAxis: {
            min: 0,
            title: {
              text: 'Number of Employees',
            },
            stackLabels: {
              enabled: true,
            },
          },
          legend: {
            layout: 'horizontal',
            align: 'center',
            verticalAlign: 'bottom',
            floating: false,
            backgroundColor: Highcharts.defaultOptions.legend?.backgroundColor || 'white',
            borderColor: '#CCC',
            borderWidth: 1,
            shadow: false,
          },
          tooltip: {
            headerFormat: '<b>{point.x}</b><br/>',
            pointFormat: '{series.name}: {point.y}<br/>Total: {point.stackTotal}',
          },
          plotOptions: {
            column: {
              stacking: 'normal',
              dataLabels: {
                enabled: true,
              },
            },
          },
          series: response.data.series,
        };
        this.updateFlagColumn = true;

        // Configure the pie chart
        this.pieChartOptions = {
          chart: {
            type: 'pie',
          },
          credits: {
            enabled: false
          },
          title: {
            text: 'Employee Distribution by Program',
          },
          series: [
            {
              type: 'pie',
              name: 'Employees',
              data: response.data.pieData,
              showInLegend: true,
              dataLabels: {
                enabled: true,
                format: '{point.name}: {point.y}',
              },
              tooltip: {
                pointFormat: '{series.name}: <b>{point.percentage:.1f}%</b>',
              },
            },
          ],
        };
        this.updateFlagPie = true;
      },
      error: (error) => {
        console.error('Error loading chart data:', error);
      }
    });
  }

  // Initialize column states
  private initializeColumnStates(): void {
    this.columnVisibility = {};
    this.collapsedColumns = {};

    this.series.forEach((serie: SeriesData) => {
      this.columnVisibility[serie.name] = false;
      this.collapsedColumns[serie.name] = false;
    });
  }

  private setupColumnSearch(): void {
    this.filteredColumns = this.columnSearchControl.valueChanges.pipe(
      startWith(''),
      map(value => this.filterColumns(value || ''))
    );
  }

  private filterColumns(value: string): SeriesData[] {
    const filterValue = value.toLowerCase();
    return this.series.filter(serie =>
      serie.name.toLowerCase().includes(filterValue)
    );
  }

  // Select all columns
  selectAllColumns(selected: boolean): void {
    this.series.forEach(serie => {
      this.columnVisibility[serie.name] = selected;
    });
  }

  // Check if all columns are selected
  areAllColumnsSelected(): boolean {
    return this.series.every(serie => this.columnVisibility[serie.name]);
  }

  // Check if some columns are selected
  areSomeColumnsSelected(): boolean {
    return this.series.some(serie => this.columnVisibility[serie.name]) &&
           !this.areAllColumnsSelected();
  }

  calculateRowTotal(data: number[]): number {
    return data.reduce((sum, current) => sum + current, 0);
  }

  calculateColumnTotals(): number[] {
    if (!this.series || !this.series.length) return [];
    const totals: number[] = [];
    const dataLength = this.series[0].data.length;

    for (let i = 0; i < dataLength; i++) {
      const columnSum = this.series.reduce((sum: number, serie: any) => sum + serie.data[i], 0);
      totals.push(columnSum);
    }
    return totals;
  }

  calculateGrandTotal(): number {
    return this.series?.reduce((sum: number, serie: any) =>
      sum + serie.data.reduce((rowSum: number, value: number) => rowSum + value, 0), 0) || 0;
  }

  calculateTeamTotal(data: number[]): number {
    return data.reduce((sum, current) => sum + current, 0);
  }

  calculateProgramTotal(index: number): number {
    return this.series.reduce((sum: number, serie: any) => sum + serie.data[index], 0);
  }


  sortColumn: string = '';
  sortAscending: boolean = true;

  sortTable(column: string) {
    if (this.sortColumn === column) {
      this.sortAscending = !this.sortAscending;
    } else {
      this.sortColumn = column;
      this.sortAscending = true;
    }

    // Combine all row data into a single array for sorting
    const combined = this.categories.map((category: any, index: number) => ({
      category,
      data: this.series.map((serie: { data: any[]; }) => serie.data[index]),
      total: this.calculateProgramTotal(index) // Store row total for consistency
    }));

    // Determine sorting logic
    combined.sort((a: { category: any; total: any; data: { [x: string]: any; }; }, b: { category: any; total: any; data: { [x: string]: any; }; }) => {
      let valueA, valueB;

      if (column === 'program') {
        valueA = a.category;
        valueB = b.category;
        return this.sortAscending ? valueA.localeCompare(valueB) : valueB.localeCompare(valueA);
      } else if (column === 'total') {
        valueA = a.total;
        valueB = b.total;
      } else {
        // Find the correct column index in series
        const colIndex = this.series.findIndex((serie: { name: string; }) => serie.name === column);
        if (colIndex === -1) return 0; // Prevent errors if column name is incorrect
        valueA = a.data[colIndex];
        valueB = b.data[colIndex];
      }

      return this.sortAscending ? valueA - valueB : valueB - valueA;
    });

    // Apply the sorted data back to the component state
    this.categories = combined.map((item: { category: any; }) => item.category);
    this.series.forEach((serie: { data: any; }, index: string | number) => {
      serie.data = combined.map((item: { data: { [x: string]: any; }; }) => item.data[index]);
    });
  }

  // Toggle column visibility
  toggleColumn(columnName: string): void {
    this.columnVisibility[columnName] = !this.columnVisibility[columnName];
  }

  // Toggle column collapse
  toggleCollapse(columnName: string): void {
    this.collapsedColumns[columnName] = !this.collapsedColumns[columnName];
  }

  // Handle column drag and drop
  onColumnDrop(event: CdkDragDrop<string[]>): void {
    if (this.series) {
      moveItemInArray(this.series, event.previousIndex, event.currentIndex);
    }
  }

  // Get visible columns
  getVisibleColumns(): SeriesData[] {
    return this.series?.filter((serie: SeriesData) => this.columnVisibility[serie.name]) || [];
  }

  // Column resizing methods
  startResize(event: MouseEvent, columnName: string): void {
    if (this.isResizing) return;

    this.isResizing = true;
    this.currentResizeColumn = columnName;
    this.startX = event.pageX;

    const thElement = (event.target as HTMLElement).closest('th');
    if (thElement) {
      this.resizingElement = thElement;
      this.startWidth = thElement.offsetWidth;
      document.body.style.cursor = 'col-resize';
      document.body.style.userSelect = 'none';
    }

    event.preventDefault();
    event.stopPropagation();
  }

  @HostListener('document:mousemove', ['$event'])
  onMouseMove(event: MouseEvent): void {
    if (!this.isResizing || !this.resizingElement) return;

    const width = this.startWidth + (event.pageX - this.startX);
    if (width >= 50) { // Minimum width of 50px
      this.columnWidths[this.currentResizeColumn] = `${width}px`;
      this.resizingElement.style.width = `${width}px`;
    }
  }

  @HostListener('document:mouseup')
  onMouseUp(): void {
    if (this.isResizing) {
      this.isResizing = false;
      this.resizingElement = null;
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    }
  }

  // Fullscreen methods
  toggleFullscreen(chartType: 'pie' | 'column') {
    if (chartType === 'pie') {
      this.isPieFullscreen = !this.isPieFullscreen;
      if (this.isPieFullscreen) {
        this.isColumnFullscreen = false;
      }
    } else {
      this.isColumnFullscreen = !this.isColumnFullscreen;
      if (this.isColumnFullscreen) {
        this.isPieFullscreen = false;
      }
    }

    // Trigger chart updates
    this.updateFlagPie = true;
    this.updateFlagColumn = true;
  }

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    if (this.isPieFullscreen || this.isColumnFullscreen) {
      this.isPieFullscreen = false;
      this.isColumnFullscreen = false;
      this.updateFlagPie = true;
      this.updateFlagColumn = true;
    }
  }
}