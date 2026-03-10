import { Component, Input, OnInit, ViewChild, Output, EventEmitter } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatSort } from '@angular/material/sort';
import { MatPaginator } from '@angular/material/paginator';
import * as XLSX from 'xlsx';
import { saveAs } from 'file-saver';

@Component({
  selector: 'app-reusable-table',
  templateUrl: './reusable-table.component.html', 
  styleUrls: ['./reusable-table.component.css']
})
export class ReusableTableComponent implements OnInit {
  @Input() displayedColumns: string[] = [];
  @Input() dataSource: MatTableDataSource<any> = new MatTableDataSource<any>();
  @Input() columnFilters: boolean = false;
  @Input() columnUniqueValues: { [key: string]: string[] } = {};
  @Input() filterValues: { [key: string]: string[] } = {};
  @Output() filterChanged = new EventEmitter<any>();
  @Input() columnDisplayNames: { [key: string]: string } = {};

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  currentFilterColumn: string | null = null;
  searchText = '';
  tempSelectedValues: string[] = [];

  ngOnInit(): void {
    this.dataSource.sort = this.sort;
    this.dataSource.paginator = this.paginator;
  }

  openFilter(column: string): void {
    this.currentFilterColumn = column;
    this.searchText = '';
    this.tempSelectedValues = this.filterValues[column]?.slice() || [];
  }

  applyFilter(): void {
    if (this.currentFilterColumn) {
      this.filterValues[this.currentFilterColumn] = [...this.tempSelectedValues];
      this.filterChanged.emit(this.filterValues);
    }
  }

  clearFilter(): void {
    if (this.currentFilterColumn) {
      this.filterValues[this.currentFilterColumn] = [];
      this.tempSelectedValues = [];
      this.filterChanged.emit(this.filterValues);
    }
  }

  isSelected(value: string): boolean {
    return this.tempSelectedValues.includes(value);
  }

  toggleSelection(value: string, checked: boolean): void {
    if (checked && !this.isSelected(value)) {
      this.tempSelectedValues.push(value);
    } else if (!checked) {
      this.tempSelectedValues = this.tempSelectedValues.filter(v => v !== value);
    }
  }

  getFilteredValues(): string[] {
    const all = this.columnUniqueValues[this.currentFilterColumn!] || [];
    return all.filter(val => val.toLowerCase().includes(this.searchText.toLowerCase()));
  }

  downloadCSV(): void {
    const exportData = this.dataSource.filteredData.map(row => {
      const flatRow: any = {};
      this.displayedColumns.forEach(col => flatRow[col] = row[col]);
      return flatRow;
    });

    const worksheet = XLSX.utils.json_to_sheet(exportData);
    const workbook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, worksheet, 'Data');

    const excelBuffer: any = XLSX.write(workbook, { bookType: 'csv', type: 'array' });
    const blob = new Blob([excelBuffer], { type: 'text/csv;charset=utf-8;' });
    saveAs(blob, `table_export_${new Date().toISOString().split('T')[0]}.csv`);
  }
}