import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AdminDashboardComponent } from './admin-dashboard/admin-dashboard.component';
import { UserDetailsComponent } from './user-details/user-details.component';
import { UserFormComponent } from './user-form/user-form.component';
import { LoginComponent } from './login/login.component';
import { OrgChartComponent } from './org-chart/org-chart.component';
import { ExtDashboardComponent } from './ext-dashboard/ext-dashboard.component';
import { RoleGuard } from './role.guard';
import { AdminGuard } from './admin.guard';
import { RequestFormComponent } from './request-form/request-form.component';
import { SignupComponent } from './signup/signup.component';
import { ApprovalTableComponent } from './approval-table/approval-table.component';
import { PasswordResetComponent } from './password-reset/password-reset.component';
import { AttendanceModuleComponent } from './attendance-module/attendance-module.component';
import { TimesheetDetailsComponent } from './timesheet-details/timesheet-details.component';
import { TimeEntryComponent } from './time-entry/time-entry.component';
import { ProjectsComponent } from './time-entry/projects/projects.component';
import { RequestsComponent } from './time-entry/requests/requests.component';
import { ProjectAssignmentComponent } from './time-entry/project-assignment/project-assignment.component';
import { TimeSummaryComponent } from './time-summary/time-summary.component';
import { LeaveApplicationComponent } from './leave-application/leave-application.component';
import { ReportsComponent } from './reports/reports.component';
import { ForgotPasswordComponent } from './forgot-password/forgot-password.component';
import { OtpVerificationComponent } from './otp-verification/otp-verification.component';
import { MaintenanceComponent } from './maintenance/maintenance.component';
import { ExampleUsageComponent } from './shared/components/example-usage/example-usage.component';
import { ComplianceDetailsComponent } from './compliance-details/compliance-details.component';
import { UtilizationDetailsComponent } from './utilization-details/utilization-details.component';
import { LeavesWfhDetailsComponent } from './leaves-wfh-details/leaves-wfh-details.component';
import { IssueListComponent } from './issue-tracker/issue-list/issue-list.component';
import { IssueDetailComponent } from './issue-tracker/issue-detail/issue-detail.component';
import { TimeEntryPendingComponent } from './time-entry-pending/time-entry-pending.component';
import { AttendanceNotMarkedComponent } from './attendance-not-marked/attendance-not-marked.component';
import { LongWeekendLeaveComponent } from './long-weekend-leave/long-weekend-leave.component';

const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'password-reset', component: PasswordResetComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'otp-verification', component: OtpVerificationComponent },
  { path: 'maintenance', component: MaintenanceComponent },

  // Admin routes with role-based access control
  {
    path: 'admin/dashboard',
    component: AdminDashboardComponent,
    canActivate: [AdminGuard],
    data: { roles: ['ADMIN_OPS_MANAGER', 'LEAD', 'MANAGER', 'ACCOUNT_MANAGER'] }
  },
  {
    path: 'admin/extdashboard',
    component: ExtDashboardComponent,
    canActivate: [AdminGuard],
    data: { roles: ['ADMIN_OPS_MANAGER', 'LEAD', 'MANAGER', 'ACCOUNT_MANAGER'] }
  },
  {
    path: 'admin/add-user',
    component: UserFormComponent,
    canActivate: [AdminGuard],
    data: { roles: ['ADMIN_OPS_MANAGER', 'LEAD', 'MANAGER'] }
  },
  {
    path: 'admin/edit-user/:id',
    component: UserFormComponent,
    canActivate: [AdminGuard],
    data: { roles: ['ADMIN_OPS_MANAGER', 'LEAD', 'MANAGER'] }
  },

  // User routes with basic authentication check
  { path: 'user-details/:id', component: UserDetailsComponent, canActivate: [RoleGuard] },
  { path: 'org-chart', component: OrgChartComponent, canActivate: [RoleGuard] },
  { path: 'request-form', component: RequestFormComponent, canActivate: [RoleGuard] },
  { path: 'signup', component: SignupComponent },
  { path: 'attendance', component: AttendanceModuleComponent, canActivate: [RoleGuard] },
  { path: 'approve', component: ApprovalTableComponent, canActivate: [RoleGuard] },
  { path: 'addtimesheet', component: TimesheetDetailsComponent, canActivate: [RoleGuard] },
  { path: 'time-entry', component: TimeEntryComponent, canActivate: [RoleGuard] },
  { path: 'time-entry/projects', component: ProjectsComponent, canActivate: [RoleGuard] },
  { path: 'time-entry/requests', component: RequestsComponent, canActivate: [RoleGuard] },
  { path: 'time-entry/project-assignment', component: ProjectAssignmentComponent, canActivate: [RoleGuard] },
  { path: 'time-summary', component: TimeSummaryComponent, canActivate: [RoleGuard] },
  { path: 'vunno', component: LeaveApplicationComponent, canActivate: [RoleGuard] },
  { path: 'reports', component: ReportsComponent, canActivate: [RoleGuard] },
  { path: 'compliance-details', component: ComplianceDetailsComponent, canActivate: [RoleGuard] },
  { path: 'utilization-details', component: UtilizationDetailsComponent, canActivate: [RoleGuard] },
  { path: 'leaves-wfh-details', component: LeavesWfhDetailsComponent, canActivate: [RoleGuard] },
  { path: 'time-entry-pending', component: TimeEntryPendingComponent, canActivate: [RoleGuard] },
  { path: 'attendance-not-marked', component: AttendanceNotMarkedComponent, canActivate: [RoleGuard] },
  { path: 'long-weekend-leave', component: LongWeekendLeaveComponent, canActivate: [RoleGuard] },
  { path: 'example-table', component: ExampleUsageComponent, canActivate: [RoleGuard] },
  { path: 'issues', component: IssueListComponent, canActivate: [RoleGuard] },
  { path: 'issues/:id', component: IssueDetailComponent, canActivate: [RoleGuard] },

  // Release Management (lazy-loaded, LDAP-restricted in guard)
  {
    path: 'releases',
    loadChildren: () => import('./release-management/release-management.module').then(m => m.ReleaseManagementModule),
    canActivate: [RoleGuard]
  },

];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
