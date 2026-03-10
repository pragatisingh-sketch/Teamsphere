import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { SharedModule } from '../shared/shared.module';

// Angular Material Imports
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDialogModule } from '@angular/material/dialog';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';

// Component Imports
import { TimeEntryComponent } from './time-entry.component';
import { TimeEntryFormComponent } from './time-entry-form/time-entry-form.component';
import { MultipleTimeEntryFormComponent } from './multiple-time-entry-form/multiple-time-entry-form.component';
import { ProjectsComponent } from './projects/projects.component';
import { ProjectFormComponent } from './projects/project-form/project-form.component';
import { RequestsComponent } from './requests/requests.component';
import { ProjectAssignmentComponent } from './project-assignment/project-assignment.component';
import { ProjectAssignmentFormComponent } from './project-assignment/project-assignment-form/project-assignment-form.component';
import { WeekCopyDialogComponent } from './week-copy-dialog/week-copy-dialog.component';
import { HolidaysComponent } from './holidays/holidays.component';

@NgModule({
  declarations: [
    TimeEntryComponent,
    TimeEntryFormComponent,
    MultipleTimeEntryFormComponent,
    ProjectsComponent,
    ProjectFormComponent,
    RequestsComponent,
    ProjectAssignmentComponent,
    ProjectAssignmentFormComponent,
    WeekCopyDialogComponent,
    HolidaysComponent,
  ],
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    HttpClientModule,
    SharedModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatDialogModule,
    MatCheckboxModule,
    MatTooltipModule,
    MatDividerModule,
    MatSlideToggleModule,
    MatMenuModule,
    MatProgressSpinnerModule,
    MatCardModule,
    MatChipsModule
  ],
  exports: [
    TimeEntryComponent
  ]
})
export class TimeEntryModule { }
