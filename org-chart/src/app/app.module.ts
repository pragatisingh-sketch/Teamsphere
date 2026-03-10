import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { AppComponent } from './app.component';
import { LoginComponent } from './login/login.component';
import { AdminDashboardComponent } from './admin-dashboard/admin-dashboard.component';
import { UserDetailsComponent } from './user-details/user-details.component';
import { UserFormComponent } from './user-form/user-form.component';
import { AppRoutingModule } from './app-routing.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';


// Angular Material modules
import { MatChipsModule } from '@angular/material/chips';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatDialogModule } from '@angular/material/dialog';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MatCardModule } from '@angular/material/card';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatIconModule } from '@angular/material/icon';
import { MatNativeDateModule, MatOptionModule } from '@angular/material/core';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSortModule } from '@angular/material/sort';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatRadioModule } from '@angular/material/radio';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonToggleModule } from '@angular/material/button-toggle';

// Other modules
import { HighchartsChartModule } from 'highcharts-angular';

// Application components
import { HeaderComponent } from './header/header.component';
import { FooterComponent } from './footer/footer.component';
import { SidebarComponent } from './sidebar/sidebar.component';
import { SharedModule } from './shared/shared.module';
import { ExtDashboardComponent } from './ext-dashboard/ext-dashboard.component';
import { AuthService } from './auth.service';
import { ReportsService } from './services/reports.service';
import { AuthInterceptor } from './auth.interceptor';
// Removed HttpInterceptorService import - using only AuthInterceptor
import { LoaderComponent } from './shared/loader/loader.component';
import { NotificationComponent } from './shared/notification/notification.component';
import { MaintenanceComponent } from './maintenance/maintenance.component';
import { RequestFormComponent } from './request-form/request-form.component';
import { SignupComponent } from './signup/signup.component';
import { ConfirmationDialogComponent } from './confirm-dialog/confirmation-dialog.component';
import { OrgChartComponent } from './org-chart/org-chart.component';
import { ApprovalTableComponent } from './approval-table/approval-table.component';
import { ViewRequestDialogComponent } from './view-request-dialog/view-request-dialog.component';
import { PasswordResetComponent } from './password-reset/password-reset.component';
import { AttendanceModuleComponent } from './attendance-module/attendance-module.component';
import { TimesheetDetailsComponent } from './timesheet-details/timesheet-details.component';
import { TimeEntryModule } from './time-entry/time-entry.module';
import { TimeSummaryComponent } from './time-summary/time-summary.component';
import { FilterDialogModule } from './time-summary/filter-dialog/filter-dialog.module';
import { IssueTrackerModule } from './issue-tracker/issue-tracker.module';
import { EditLogsDialogComponent } from './edit-logs-dialog/edit-logs-dialog.component';
import { LeaveApplicationComponent } from './leave-application/leave-application.component';
import { AlertDialogComponent } from './leave-application/alert-dialog.component';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { ForgotPasswordComponent } from './forgot-password/forgot-password.component';
import { OtpVerificationComponent } from './otp-verification/otp-verification.component';
import { LateCheckinDialogComponent } from './leave-application/attendance/late-checkin-dialog.component';
import { CheckoutDialogComponent } from './leave-application/attendance/checkout-dialog.component';
import { AttendanceComponent } from './leave-application/attendance/attendance.component';
import { AttendanceModule } from './leave-application/attendance/attendance.module';
import { BalanceComponent } from './leave-application/components/balance/balance.component';
import { HistoryComponent } from './leave-application/components/history/history.component';
import { NewRequestComponent } from './leave-application/components/new-request/new-request.component';
import { RequestsComponent } from './leave-application/components/requests/requests.component';
import { EditLeaveRequestComponent } from './leave-application/components/history/edit-leave-request/edit-leave-request.component';
import { DocumentPreviewDialogComponent } from './leave-application/components/requests/document-preview-dialog/document-preview-dialog.component';
import { DropdownConfigurationModalComponent } from './components/dropdown-configuration-modal/dropdown-configuration-modal.component';
import { ReportsComponent } from './reports/reports.component';
import { ExampleUsageComponent } from './shared/components/example-usage/example-usage.component';
import { ComplianceDetailsComponent } from './compliance-details/compliance-details.component';
import { UtilizationDetailsComponent } from './utilization-details/utilization-details.component';
import { LeavesWfhDetailsComponent } from './leaves-wfh-details/leaves-wfh-details.component';
import { DelegationDialogComponent } from './delegation-dialog/delegation-dialog.component';
import { DelegationHistoryDialogComponent } from './delegation-history-dialog/delegation-history-dialog.component';
import { CategoryUpdateDialogComponent } from './leave-application/components/requests/category-update-dialog/category-update-dialog.component';
import { IssueDetailsDialogComponent } from './compliance-details/issue-details-dialog/issue-details-dialog.component';
import { TimeEntryPendingComponent } from './time-entry-pending/time-entry-pending.component';
import { TimeEntryReminderDialogComponent } from './time-entry-pending/time-entry-reminder-dialog/time-entry-reminder-dialog.component';
import { AttendanceNotMarkedComponent } from './attendance-not-marked/attendance-not-marked.component';
import { AttendanceReminderDialogComponent } from './attendance-not-marked/attendance-reminder-dialog/attendance-reminder-dialog.component';
import { LongWeekendLeaveComponent } from './long-weekend-leave/long-weekend-leave.component';


@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    AdminDashboardComponent,
    UserDetailsComponent,
    UserFormComponent,
    HeaderComponent,
    FooterComponent,
    SidebarComponent,
    ExtDashboardComponent,
    LoaderComponent,
    NotificationComponent,
    MaintenanceComponent,
    RequestFormComponent,
    SignupComponent,
    ConfirmationDialogComponent,
    OrgChartComponent,
    ApprovalTableComponent,
    ViewRequestDialogComponent,
    PasswordResetComponent,
    AttendanceModuleComponent,
    TimesheetDetailsComponent,
    TimeSummaryComponent,
    EditLogsDialogComponent,
    LeaveApplicationComponent,
    AlertDialogComponent,
    ForgotPasswordComponent,
    OtpVerificationComponent,
    LateCheckinDialogComponent,
    CheckoutDialogComponent,
    BalanceComponent,
    HistoryComponent,
    NewRequestComponent,
    RequestsComponent,
    EditLeaveRequestComponent,
    DocumentPreviewDialogComponent,
    DropdownConfigurationModalComponent,
    ReportsComponent,
    ExampleUsageComponent,
    ComplianceDetailsComponent,
    UtilizationDetailsComponent,
    UtilizationDetailsComponent,
    LeavesWfhDetailsComponent,
    DelegationDialogComponent,
    DelegationHistoryDialogComponent,
    CategoryUpdateDialogComponent,
    IssueDetailsDialogComponent,
    TimeEntryPendingComponent,
    TimeEntryReminderDialogComponent,
    AttendanceNotMarkedComponent,
    AttendanceReminderDialogComponent,
    LongWeekendLeaveComponent
  ],
  imports: [
    MatSnackBarModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatExpansionModule,
    MatIconModule,
    MatButtonModule,
    ReactiveFormsModule,
    BrowserModule,
    AppRoutingModule,
    FormsModule,
    ReactiveFormsModule,
    HttpClientModule,
    BrowserAnimationsModule,
    HighchartsChartModule,
    TimeEntryModule,
    FilterDialogModule,
    SharedModule,
    IssueTrackerModule,
    AttendanceModule,
    // Angular Material modules
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatFormFieldModule,
    MatTableModule,
    MatPaginatorModule,
    MatDialogModule,
    MatCardModule,
    MatNativeDateModule,
    MatDatepickerModule,
    MatProgressBarModule,
    MatCheckboxModule,
    MatSortModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatOptionModule,
    MatAutocompleteModule,
    DragDropModule,
    MatMenuModule,
    MatDividerModule,
    MatRadioModule,
    MatTabsModule,
    MatChipsModule,
    MatButtonToggleModule,
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    // Removed HttpInterceptorService to avoid conflicts with AuthInterceptor
    AuthService,
    ReportsService,
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
