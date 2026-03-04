import { Component, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { MatMenuTrigger } from '@angular/material/menu';
import { ReportsService } from '../../../services/reports.service';
import { FormControl } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

@Component({
  selector: 'app-unified-filter',
  templateUrl: './unified-filter.component.html',
  styleUrls: ['./unified-filter.component.css']
})
export class UnifiedFilterComponent implements OnInit {
  @Input() userRole: string = 'USER';
  @Output() filterChange = new EventEmitter<{ type: string, value: string | null }>();

  @ViewChild('mainMenuTrigger') mainMenuTrigger!: MatMenuTrigger;

  filterOptions: { [key: string]: string[] } = {};
  activeFilters: { [key: string]: string } = {};

  // Search control
  searchControl = new FormControl('');
  filteredValues: string[] = [];
  currentCategory: string | null = null;

  // Categories definition
  categories = [
    { key: 'Team', label: 'Team', icon: 'group' },
    { key: 'Project', label: 'Project', icon: 'work' },
    { key: 'Program', label: 'Program', icon: 'category' },
    { key: 'Manager', label: 'Manager', icon: 'supervisor_account' },
    { key: 'Lead', label: 'Lead', icon: 'person' }
  ];

  constructor(private reportsService: ReportsService) { }

  ngOnInit(): void {
    this.loadFilterOptions();

    // Setup search debouncing
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(value => {
      this.filterValues(value || '');
    });
  }

  loadFilterOptions(): void {
    this.reportsService.getFilterOptions().subscribe(response => {
      if (response.data) {
        this.filterOptions = response.data;
      }
    });
  }

  isCategoryDisabled(category: string): boolean {
    if (this.userRole === 'ADMIN_OPS_MANAGER') return false;
    if (this.userRole === 'MANAGER') {
      return ['Manager'].includes(category);
    }
    if (this.userRole === 'LEAD') return true; // Leads can't filter
    return true;
  }

  onCategoryHover(category: string): void {
    if (this.isCategoryDisabled(category)) return;

    this.currentCategory = category;
    this.searchControl.setValue('');
    this.filterValues('');
  }

  filterValues(search: string): void {
    if (!this.currentCategory || !this.filterOptions[this.currentCategory]) {
      this.filteredValues = [];
      return;
    }

    const values = this.filterOptions[this.currentCategory];
    if (!search) {
      this.filteredValues = values;
    } else {
      const searchLower = search.toLowerCase();
      this.filteredValues = values.filter(v => v.toLowerCase().includes(searchLower));
    }
  }

  selectValue(value: string): void {
    if (this.currentCategory) {
      this.activeFilters[this.currentCategory] = value;
      this.filterChange.emit({ type: this.currentCategory.toLowerCase(), value: value });
      this.mainMenuTrigger.closeMenu();
    }
  }

  removeFilter(category: string): void {
    delete this.activeFilters[category];
    this.filterChange.emit({ type: category.toLowerCase(), value: null });
  }

  get activeFilterCount(): number {
    return Object.keys(this.activeFilters).length;
  }

  // Template helpers
  objectKeys = Object.keys;

  getCategoryIcon(category: string): string {
    const found = this.categories.find(c => c.key === category);
    return found ? found.icon : 'filter_list';
  }

  clearAllFilters(): void {
    this.activeFilters = {};
    // Emit clear for all categories
    this.categories.forEach(cat => {
      this.filterChange.emit({ type: cat.key.toLowerCase(), value: null });
    });
  }
}
