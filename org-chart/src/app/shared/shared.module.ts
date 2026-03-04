import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatMenuModule } from '@angular/material/menu';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatExpansionModule } from '@angular/material/expansion';
import { PageHeaderComponent } from '../page-header/page-header.component';
import { BreadcrumbComponent } from './breadcrumb/breadcrumb.component';
import { ReusableTableModule } from './components/reusable-table/reusable-table.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { SharedToggleComponent } from './shared-toggle/shared-toggle.component';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { InsightCardComponent } from './components/insight-card/insight-card.component';
import { UnifiedFilterComponent } from './components/unified-filter/unified-filter.component';
import { FAQDialogComponent } from './components/faq-dialog/faq-dialog.component';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule } from '@angular/material/dialog';

@NgModule({
  declarations: [
    PageHeaderComponent,
    BreadcrumbComponent,
    SharedToggleComponent,
    InsightCardComponent,
    UnifiedFilterComponent,
    FAQDialogComponent
  ],
  imports: [
    CommonModule,
    RouterModule,
    MatIconModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatCheckboxModule,
    MatMenuModule,
    MatFormFieldModule,
    MatInputModule,
    FormsModule,
    ReactiveFormsModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule,
    MatDatepickerModule,
    MatButtonModule,
    MatSelectModule,
    MatTooltipModule,
    MatDialogModule,
    MatChipsModule,
    MatExpansionModule,
    ReusableTableModule
  ],
  exports: [
    PageHeaderComponent,
    BreadcrumbComponent,
    ReusableTableModule,
    SharedToggleComponent,
    InsightCardComponent,
    UnifiedFilterComponent,
    FAQDialogComponent,
    MatSlideToggleModule,
    FormsModule,
    ReactiveFormsModule,
    MatProgressSpinnerModule,
    MatChipsModule
  ]
})
export class SharedModule { }

