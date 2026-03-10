import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { HttpClient } from '@angular/common/http';
import { NotificationService } from '../../shared/notification.service';
import { environment } from '../../../environments/environment';
import { TimeEntry } from '../time-entry.component';
import {BaseResponse} from '../../model/base-response.model';
import { ConfirmationDialogComponent } from '../../confirm-dialog/confirmation-dialog.component';

interface Project {
  projectId?: number;
  id?: number;
  projectName?: string;
  name?: string;
  projectCode?: string;
  code?: string;
  ldap?: string;
  isOvertimeEligible?: boolean;
}

interface User {
  id: number;
  ldap: string;
  role: string;
}

interface Activity {
  name: string;
  value: string;
}

interface AttendanceType {
  name: string;
  value: string;
}

@Component({
  selector: 'app-time-entry-form',
  templateUrl: './time-entry-form.component.html',
  styleUrls: ['./time-entry-form.component.css']
})
export class TimeEntryFormComponent implements OnInit {
  timeEntryForm: FormGroup;
  isEditMode = false;
  projects: Project[] = [];
  filteredProjects: Project[] = [];
  leads: User[] = [];
  filteredLeads: User[] = [];
  teamMembers: User[] = [];
  filteredTeamMembers: User[] = [];
  currentUserRole: string = '';
  isLeadOrManager: boolean = false;
  isOnBehalfMode: boolean = false; // Toggle state for on behalf mode
  isOvertimeEntry: boolean = false; // Toggle state for overtime entries
  showOvertimeToggle: boolean = false; // Flag to control overtime toggle visibility
  currentUserLdap: string = ''; // Store current user's LDAP
  currentUserLeadId: number | null = null; // Store current user's lead ID
  baseUrl = environment.apiUrl;
  activities: Activity[] = [];
  maxTimeInMins = 480; // 8 hours in minutes
  showHolidayWarning: boolean = false; // Flag to show holiday warning
  holidayMessage: string = ''; // Holiday warning message
  attendanceTypes: AttendanceType[] = [
    { name: 'S1/F', value: 'S1/F' },
    { name: 'S2/F', value: 'S2/F' },
    { name: 'S3/F', value: 'S3/F' },
    { name: 'MS/F', value: 'MS/F' },
    { name: 'MS2/F', value: 'MS2/F' },
    { name: 'S1/H', value: 'S1/H' },
    { name: 'S2/H', value: 'S2/H' },
    { name: 'S3/H', value: 'S3/H' },
    { name: 'MS/H', value: 'MS/H' },
    { name: 'MS2/H', value: 'MS2/H' },
    { name: 'S1/WO', value: 'S1/WO' },
    { name: 'S2/WO', value: 'S2/WO' },
    { name: 'S3/WO', value: 'S3/WO' },
    { name: 'MS/WO', value: 'MS/WO' },
    { name: 'MS2/WO', value: 'MS2/WO' },
    { name: 'S1/GO', value: 'S1/GO' },
    { name: 'S2/GO', value: 'S2/GO' },
    { name: 'S3/GO', value: 'S3/GO' },
    { name: 'MS/GO', value: 'MS/GO' },
    { name: 'MS2/GO', value: 'MS2/GO' },
    { name: 'S1/SLH', value: 'S1/SLH' },
    { name: 'S2/SLH', value: 'S2/SLH' },
    { name: 'S3/SLH', value: 'S3/SLH' },
    { name: 'MS/SLH', value: 'MS/SLH' },
    { name: 'MS2/SLH', value: 'MS2/SLH' }

  ];

  // Attendance types specifically for comoff activity
  comoffAttendanceTypes: AttendanceType[] = [
    { name: 'S1/C', value: 'S1/C' },
    { name: 'S2/C', value: 'S2/C' },
    { name: 'S3/C', value: 'S3/C' },
    { name: 'MS/C', value: 'MS/C' },
    { name: 'MS2/C', value: 'MS2/C' }
  ];

  leaveAttendanceTypes: AttendanceType[] = [
    { name: 'S1/Leave', value: 'S1/Leave' },
    { name: 'S2/Leave', value: 'S2/Leave' },
    { name: 'S3/Leave', value: 'S3/Leave' },
    { name: 'MS/Leave', value: 'MS/Leave' },
    { name: 'MS2/Leave', value: 'MS2/Leave' },
    { name: 'S1/H', value: 'S1/H' },
    { name: 'S2/H', value: 'S2/H' },
    { name: 'S3/H', value: 'S3/H' },
    { name: 'MS/H', value: 'MS/H' },
    { name: 'MS2/H', value: 'MS2/H' },
    { name: 'S1/A', value: 'S1/A' },
    { name: 'S2/A', value: 'S2/A' },
    { name: 'S3/A', value: 'S3/A' },
    { name: 'MS/A', value: 'MS/A' },
    { name: 'MS2/A', value: 'MS2/A' },
    { name: 'S1/C', value: 'S1/C' },
    { name: 'S2/C', value: 'S2/C' },
    { name: 'S3/C', value: 'S3/C' },
    { name: 'MS/C', value: 'MS/C' },
    { name: 'MS2/C', value: 'MS2/C' },
    { name: 'S1/BL', value: 'S1/BL' },
    { name: 'S2/BL', value: 'S2/BL' },
    { name: 'S3/BL', value: 'S3/BL' },
    { name: 'MS/BL', value: 'MS/BL' },
    { name: 'MS2/BL', value: 'MS2/BL' },
    { name: 'S1/SL', value: 'S1/SL' },
    { name: 'S2/SL', value: 'S2/SL' },
    { name: 'S3/SL', value: 'S3/SL' },
    { name: 'MS/SL', value: 'MS/SL' },
    { name: 'MS2/SL', value: 'MS2/SL' },
    { name: 'S1/SLH', value: 'S1/SLH' },
    { name: 'S2/SLH', value: 'S2/SLH' },
    { name: 'S3/SLH', value: 'S3/SLH' },
    { name: 'MS/SLH', value: 'MS/SLH' },
    { name: 'MS2/SLH', value: 'MS2/SLH' },
    { name: 'S1/PAT', value: 'S1/PAT' },
    { name: 'S2/PAT', value: 'S2/PAT' },
    { name: 'S3/PAT', value: 'S3/PAT' },
    { name: 'MS/PAT', value: 'MS/PAT' },
    { name: 'MS2/PAT', value: 'MS2/PAT' },
    { name: 'Maternity', value: 'Maternity' }
  ];

  filteredAttendanceTypes: AttendanceType[] = this.attendanceTypes;

  private existingEntries: TimeEntry[] = [];

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private notificationService: NotificationService,
    public dialogRef: MatDialogRef<TimeEntryFormComponent>,
    private dialog: MatDialog,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    this.isEditMode = data?.isEditMode || false;
    this.existingEntries = data?.existingEntries || [];

    this.timeEntryForm = this.fb.group({
      entryDate: [new Date(), Validators.required],
      ldap: ['', Validators.required],
      leadId: ['', Validators.required],
      projectId: ['', Validators.required],
      process: ['', Validators.required],
      activity: ['', Validators.required],
      timeInMins: ['', [Validators.required, Validators.min(1), Validators.max(this.maxTimeInMins)]],
      attendanceType: ['', Validators.required],
      comment: ['']
    });

    // Listen for changes to the projectId form control to update comment validation
    this.timeEntryForm.get('projectId')?.valueChanges.subscribe(projectId => {
      this.updateCommentValidation(projectId);
    });

    // Handle both edit mode and cloning
    if ((this.isEditMode || data?.isCloning) && data.timeEntry) {
      console.log(data.timeEntry);
      this.timeEntryForm.patchValue({
        entryDate: data.isCloning ? new Date() : new Date(data.timeEntry.entryDate || data.timeEntry.date),
        ldap: data.timeEntry.ldap,
        leadId: data.timeEntry.leadId,
        projectId: data.timeEntry.projectId,
        process: data.timeEntry.process,
        activity: data.timeEntry.activity,
        timeInMins: data.timeEntry.timeInMins || data.timeEntry.timeInMinutes,
        attendanceType: data.timeEntry.attendanceType,
        comment: data.timeEntry.comment
      });
    }
  }

  ngOnInit(): void {
    this.loadProjects();
    this.loadActivities();

    // First load leads, then load current user to ensure leads are available
    this.loadLeads()
      .then(() => this.loadCurrentUser())
      .catch(error => console.error('Error loading leads:', error));

    // Listen for changes to the projectId form control
    this.timeEntryForm.get('projectId')?.valueChanges.subscribe(projectId => {
      this.checkProjectForOvertimeToggle(projectId);
      this.updateCommentValidation(projectId);
    });

    // Listen for changes to the entryDate form control to check for holidays
    this.timeEntryForm.get('entryDate')?.valueChanges.subscribe(date => {
      this.checkForHoliday(date);
    });

    // Update attendance types based on activity selection
    this.timeEntryForm.get('activity')?.valueChanges.subscribe(() => {
      this.updateAttendanceTypes();
    });

    // Initialize attendance types
    this.updateAttendanceTypes();

    // Add listener for ldap changes in on-behalf mode
    this.timeEntryForm.get('ldap')?.valueChanges.subscribe(ldap => {
      if (this.isOnBehalfMode && this.isLeadOrManager && ldap) {
        this.fetchLeadForTeamMember(ldap);
      }
    });
  }

  fetchLeadForTeamMember(ldap: string): void {
    // Get team member details and their available leads
    this.http.get<BaseResponse<any>>(`${this.baseUrl}/api/employees/by-ldap/${ldap}`)
      .subscribe({
        next: (empResponse) => {
          if (empResponse.status === 'success') {
            const employee = empResponse.data;
            const leadLdap = employee.lead;

            // Get leads available for this team member
            this.http.get<BaseResponse<User[]>>(`${this.baseUrl}/api/employees/my-team-leads/${ldap}`)
              .subscribe({
                next: (leadsResponse) => {
                  if (leadsResponse.status === 'success') {
                    // Update leads dropdown with team member's available leads
                    this.filteredLeads = leadsResponse.data;

                    // Find and set the team member's actual lead
                    const lead = leadsResponse.data.find(l => l.ldap === leadLdap);
                    if (lead) {
                      this.timeEntryForm.patchValue({ leadId: lead.id });
                    }
                  }
                },
                error: (error) => {
                  console.error('Failed to get team member leads:', error);
                }
              });
          }
        },
        error: (error) => {
          console.error('Failed to get employee by ldap:', error);
        }
      });
  }

  private updateAttendanceTypes(): void {
    const activityValue = this.timeEntryForm.get('activity')?.value;
    if (activityValue === 'ABSENTEEISM') {
      this.filteredAttendanceTypes = this.leaveAttendanceTypes;

      // Reset attendance type if current value is not in leave options
      const currentType = this.timeEntryForm.get('attendanceType')?.value;
      if (currentType && !this.leaveAttendanceTypes.some(t => t.value === currentType)) {
        this.timeEntryForm.get('attendanceType')?.setValue('');
      }
    } else if (activityValue === 'COMPOFF') {
      this.filteredAttendanceTypes = this.comoffAttendanceTypes;

      // Reset attendance type if current value is not in comoff options
      const currentType = this.timeEntryForm.get('attendanceType')?.value;
      if (currentType && !this.comoffAttendanceTypes.some(t => t.value === currentType)) {
        this.timeEntryForm.get('attendanceType')?.setValue('');
      }
    } else {
      this.filteredAttendanceTypes = this.attendanceTypes;
    }
  }

  /**
   * Checks if the selected project is overtime eligible to determine if the overtime toggle should be shown
   * @param projectId The ID of the selected project
   */
  checkProjectForOvertimeToggle(projectId: number | undefined): void {
    console.log('Checking overtime toggle for project ID:', projectId);

    if (!projectId) {
      this.showOvertimeToggle = false;
      console.log('No project ID provided, hiding overtime toggle');
      return;
    }

    // Find the selected project in the projects array
    const selectedProject = this.projects.find(project =>
      (project.projectId || project.id) === projectId
    );

    console.log('Selected project:', selectedProject);
    console.log('Project isOvertimeEligible:', selectedProject?.isOvertimeEligible);

    // Show overtime toggle only if the project is marked as overtime eligible
    if (selectedProject) {
      // Check if the project has the isOvertimeEligible property set to true
      this.showOvertimeToggle = selectedProject.isOvertimeEligible === true;
      console.log('Show overtime toggle:', this.showOvertimeToggle);

      // If we're hiding the toggle and overtime is enabled, disable it
      if (!this.showOvertimeToggle && this.isOvertimeEntry) {
        this.isOvertimeEntry = false;
        this.toggleOvertimeMode();
      }
    } else {
      this.showOvertimeToggle = false;
      console.log('Project not found, hiding overtime toggle');
    }
  }

  /**
   * Updates comment field validation based on selected project
   * @param projectId The ID of the selected project
   */
  updateCommentValidation(projectId: number | undefined): void {
    const commentControl = this.timeEntryForm.get('comment');
    if (!commentControl) return;

    if (projectId) {
      const selectedProject = this.projects.find(project =>
        (project.projectId || project.id) === projectId
      );

      // Make comment required if project is overtime eligible
      if (selectedProject?.isOvertimeEligible) {
        commentControl.setValidators([Validators.required]);
        commentControl.markAsDirty();
        commentControl.updateValueAndValidity();
      } else {
        commentControl.clearValidators();
        commentControl.markAsDirty();
        commentControl.updateValueAndValidity();
      }
    } else {
      commentControl.clearValidators();
      commentControl.markAsDirty();
      commentControl.updateValueAndValidity();
    }
  }

  /**
   * Checks if the selected date is a Google holiday and shows a confirmation dialog
   * @param date The selected date
   */
  checkForHoliday(date: Date | null): void {
    if (!date) {
      this.showHolidayWarning = false;
      return;
    }

    const formattedDate = this.formatDate(date);

    this.http.get<BaseResponse<boolean>>(`${this.baseUrl}/api/holidays/check?date=${formattedDate}`)
      .subscribe({
        next: (response) => {
          if (response.status === 'success' && response.data === true) {
            // Get holiday details to show in the confirmation dialog
            this.http.get<BaseResponse<any>>(`${this.baseUrl}/api/holidays/date/${formattedDate}`)
              .subscribe({
                next: (holidayResponse) => {
                  if (holidayResponse.status === 'success') {
                    this.showHolidayDialog(`This day is a Google holiday (${holidayResponse.data.holidayName}). Do you still want to proceed?`, holidayResponse.data.holidayName);
                  }
                },
                error: () => {
                  this.showHolidayDialog('This day is a Google holiday. Do you still want to proceed?');
                }
              });
          } else {
            this.showHolidayWarning = false;
          }
        },
        error: () => {
          // If there's an error checking holidays, don't show warning
          this.showHolidayWarning = false;
        }
      });
  }

  /**
   * Shows a confirmation dialog for Google holidays
   * @param message The message to display in the dialog
   */
  showHolidayDialog(message: string, holidayName?: string): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Google Holiday',
        message: message,
        confirmButtonText: 'Proceed',
        color: 'primary',
        holidayName: holidayName
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result === false) {
        // User clicked cancel or closed the dialog, close the time-entry form
        this.dialogRef.close(false);
      } else if (result === true) {
        // User clicked proceed, autofill the form with holiday details
        this.autofillHolidayForm(holidayName);
      }
    });
  }

  /**
   * Autofills the form with holiday details when user proceeds
   * @param holidayName The name of the holiday
   */
  private autofillHolidayForm(holidayName?: string): void {
    // Set holiday-specific values including 480 minutes (8 hours) for full day
    this.timeEntryForm.patchValue({
      process: 'Holiday',
      activity: 'HOLIDAY',
      attendanceType: 'S1/GO',
      timeInMins: 480, // 8 hours in minutes for full day holiday
      comment: holidayName ? `Google Holiday - ${holidayName}` : 'Google Holiday'
    });

    // Mark the form as dirty to trigger validation
    Object.keys(this.timeEntryForm.controls).forEach(key => {
      this.timeEntryForm.get(key)?.markAsDirty();
    });

    // Update comment validation since we're setting a value
    const projectId = this.timeEntryForm.get('projectId')?.value;
    this.updateCommentValidation(projectId);
  }

  loadCurrentUser(): void {
    this.http.get<BaseResponse<User>>(`${this.baseUrl}/api/projects/current-user`)
      .subscribe({
        next: (response) => {
          if (response.status === 'success') {
            const user = response.data;
            this.currentUserRole = user.role;
            this.isLeadOrManager = ['LEAD', 'MANAGER', 'ADMIN_OPS_MANAGER'].includes(user.role);
            this.currentUserLdap = user.ldap;

            // Set the current user's LDAP as default ONLY if not in edit mode
            // This ensures we don't override the LDAP of the user being edited
            if (!this.isEditMode) {
              this.timeEntryForm.patchValue({
                ldap: user.ldap
              });
            }

            // Get the current user's lead from employee data
            this.http.get<BaseResponse<any>>(`${this.baseUrl}/api/employees/by-ldap/${user.ldap}`)
              .subscribe({
                next: (empResponse) => {
                  if (empResponse.status === 'success') {
                    console.log("Data-> ", empResponse.data)
                    const employee = empResponse.data;
                    const leadLdap = employee.lead;

                    // Find the lead in the leads array
                    let lead = this.leads.find(l => l.ldap === leadLdap);

                    // If lead not found, check if it's a manager's lead
                    if (!lead && this.currentUserRole === 'ADMIN_OPS_MANAGER') {
                      lead = this.leads.find(l => l.role === 'ADMIN_OPS_MANAGER');
                    }

                    if (lead && !this.isEditMode) {
                      this.currentUserLeadId = lead.id; // Store for mode switching
                      this.timeEntryForm.patchValue({
                        leadId: lead.id
                      });
                    }
                  }
                },
                error: (error) => {
                  console.error('Failed to get employee by ldap:', error);
                }
              });

            // If user is a lead, manager, or admin_ops_manager, load team members
            if (this.isLeadOrManager) {
              this.loadTeamMembers();
            }
          } else {
            this.notificationService.showNotification({
              type: 'error',
              message: response.message || 'Failed to load user information'
            });
          }
        },
        error: (error) => {
          // Extract error message and let the service transform it
          const errorMessage = error.error?.message || 'Failed to load user information. Please try again.';
          this.notificationService.showError(errorMessage);
        }
      });
  }

  // Toggle between personal entry and on behalf mode
  toggleOnBehalfMode(): void {
    this.isOnBehalfMode = !this.isOnBehalfMode;

    // If in edit mode, don't change the fields to preserve the user being edited
    if (this.isEditMode) {
      return;
    }

    if (this.isOnBehalfMode) {
      // Switch to on behalf mode
      this.timeEntryForm.patchValue({
        ldap: '',
        leadId: null
      });
    } else {
      // Switch back to personal mode
      this.timeEntryForm.patchValue({
        ldap: this.currentUserLdap,
        leadId: this.currentUserLeadId
      });
      // Reset leads dropdown to current user's leads
      this.filteredLeads = this.leads;
    }
  }

  // Toggle overtime mode
  toggleOvertimeMode(): void {
    this.isOvertimeEntry = !this.isOvertimeEntry;

    // Update the validator for timeInMins based on overtime mode
    const timeInMinsControl = this.timeEntryForm.get('timeInMins');
    if (timeInMinsControl) {
      if (this.isOvertimeEntry) {
        // For overtime entries, only require a minimum of 1 minute with no maximum
        timeInMinsControl.setValidators([Validators.required, Validators.min(1)]);
      } else {
        // For regular entries, require between 1 and maxTimeInMins (480) minutes
        timeInMinsControl.setValidators([
          Validators.required,
          Validators.min(1),
          Validators.max(this.maxTimeInMins)
        ]);
      }
      // Update the validators
      timeInMinsControl.updateValueAndValidity();
    }
  }

  loadTeamMembers(): void {
    this.http.get<BaseResponse<User[]>>(`${this.baseUrl}/api/time-entries/team-members`)
      .subscribe({
        next: (response) => {
          if (response.status === 'success') {
            this.teamMembers = response.data;
            this.filteredTeamMembers = response.data;
          } else {
            this.notificationService.showNotification({
              type: 'error',
              message: response.message || 'Failed to load team members'
            });
          }
        },
        error: (error) => {
          // Extract error message and let the service transform it
          const errorMessage = error.error?.message || 'Failed to load team members. Please try again.';
          this.notificationService.showError(errorMessage);
        }
      });
  }

  filterTeamMembers(event: any): void {
    const searchText = event.target.value.toLowerCase();
    this.filteredTeamMembers = this.teamMembers.filter(member =>
      member.ldap.toLowerCase().includes(searchText)
    );
  }

  loadProjects(): void {
    // Determine which endpoint to use based on whether we're in the requests table edit mode
    const endpoint = this.data?.fromRequestsTable ? 'api/projects' : 'api/time-entries/my-projects';

    this.http.get<BaseResponse<Project[]>>(`${this.baseUrl}/${endpoint}`)
      .subscribe({
        next: (response) => {
          if (response.status === 'success') {
            this.projects = response.data;
            this.filteredProjects = response.data;
            console.log('Loaded projects:', this.projects);

            if (this.projects.length > 0 && !this.isEditMode) {
              const defaultProjectId = this.projects[0].projectId || this.projects[0].id;
              this.timeEntryForm.patchValue({
                projectId: defaultProjectId
              });

              // Check if the default project is overtime eligible to show/hide overtime toggle
              this.checkProjectForOvertimeToggle(defaultProjectId);
            } else if (this.isEditMode && this.data.timeEntry) {
              // For edit mode, check the selected project
              const editProjectId = this.data.timeEntry.projectId;
              this.checkProjectForOvertimeToggle(editProjectId);
            }
          } else {
            this.notificationService.showNotification({
              type: 'error',
              message: response.message || 'Failed to load projects'
            });
          }
        },
        error: (error) => {
          // Try to extract message from backend error response
          let errorMessage = 'Failed to load projects. Please try again.';
          if (error.error && error.error.message) {
            errorMessage = error.error.message;
          }

          this.notificationService.showNotification({
            type: 'error',
            message: errorMessage
          });
        }
      });
  }

  loadLeads(): Promise<void> {
    return new Promise((resolve, reject) => {
      const ldapParam = this.currentUserLdap || 'null';
      this.http.get<BaseResponse<User[]>>(`${this.baseUrl}/api/employees/my-team-leads/${ldapParam}`)
        .subscribe({
          next: (response) => {
            if (response.status === 'success') {
              this.leads = response.data;
              this.filteredLeads = response.data;
              resolve();
            } else {
              this.notificationService.showError(response.message || 'Failed to load leads');
              reject(response.message);
            }
          },
          error: (error) => {
            // Extract error message and let the service transform it
            const errorMessage = error.error?.message || 'Failed to load leads. Please try again.';
            this.notificationService.showError(errorMessage);
            reject(errorMessage);
          }
        });
    });
  }

  filterLeads(event: any): void {
    const searchText = event.target.value.toLowerCase();
    this.filteredLeads = this.leads.filter(lead =>
      lead.ldap.toLowerCase().includes(searchText) ||
      lead.role.toLowerCase().includes(searchText)
    );
  }

  loadActivities(): void {
    this.http.get<BaseResponse<string[]>>(`${this.baseUrl}/api/projects/activities`)
      .subscribe({
        next: (response) => {
          if (response.status === 'success') {
            // Map the activity names to Activity objects with name and value
            this.activities = response.data.map(name => ({
              name: name,
              value: name.toUpperCase().replace(/\s+/g, '_') // Convert "Production" to "PRODUCTION"
            }));
          } else {
            this.notificationService.showNotification({
              type: 'error',
              message: response.message || 'Failed to load activities'
            });
          }
        },
        error: (error) => {
          // Extract error message and let the service transform it
          const errorMessage = error.error?.message || 'Failed to load activities. Please try again.';
          this.notificationService.showError(errorMessage);
        }
      });
  }

  private isDuplicateEntry(formData: any): boolean {
    console.log(this.existingEntries);
    const currentEntryId = this.isEditMode ? this.data.timeEntry.id : null;

    return this.existingEntries.some(entry => {
      // Skip comparing with the current entry being edited
      if (this.isEditMode && entry.id === currentEntryId) {
        return false;
      }

      const dateValue = entry.entryDate || entry.date;
      const entryDate = this.formatDate(new Date(dateValue || Date.now()));
      const formEntryDate = this.formatDate(new Date(formData.entryDate));

      return (
        entryDate === formEntryDate &&
        entry.ldap === formData.ldap &&
        entry.projectId === formData.projectId &&
        entry.process === formData.process &&
        entry.activity === formData.activity &&
        entry.timeInMins === formData.timeInMins &&
        entry.attendanceType === formData.attendanceType
      );
    });
  }

  onSubmit(): void {
    if (this.timeEntryForm.invalid) {
      return;
    }

    const formData = {
      ...this.timeEntryForm.value,
      isOvertime: this.isOvertimeEntry // Add the overtime flag to the form data
    };
    const entryDate = formData.entryDate;

    if (entryDate) {
      formData.entryDate = this.formatDate(entryDate);
    }

    // Only check remaining time for new entries, not for edits, and only if not overtime
    if (!this.isEditMode) {
      // Check for duplicate entry regardless of overtime status
      if (this.isDuplicateEntry(formData)) {
        this.notificationService.showNotification({
          type: 'warning',
          message: 'A time entry with identical details already exists for this date. Please modify your entry.'
        });
        return;
      }

      // If it's an overtime entry, skip the remaining time check
      if (this.isOvertimeEntry) {
        this.createTimeEntry(formData);
        return;
      }

      // For regular entries, check remaining time for the day
      // Build the URL with query parameters
      let remainingTimeUrl = `${this.baseUrl}/api/time-entries/remaining-time?date=${formData.entryDate}`;

      // Always use the LDAP from the form to check remaining time
      // This ensures we validate against the correct user's time entries
      // whether it's the current user or someone else (in edit mode or on behalf mode)
      if (formData.ldap && formData.ldap !== this.currentUserLdap) {
        remainingTimeUrl += `&ldap=${formData.ldap}`;
      }

      this.http.get<BaseResponse<number>>(remainingTimeUrl)
        .subscribe({
          next: (response) => {
            if (response.status === 'success') {
              const remainingTime = response.data;
              if (formData.timeInMins > remainingTime) {
                this.notificationService.showNotification({
                  type: 'error',
                  message: `Cannot add more than ${remainingTime} minutes for this day.`
                });
                return;
              }

              this.createTimeEntry(formData);
            } else {
              this.notificationService.showNotification({
                type: 'error',
                message: response.message || 'Failed to check remaining time'
              });
            }
          },
          error: (error) => {
            // Try to extract message from backend error response
            let errorMessage = 'Failed to check remaining time. Please try again.';
            if (error.error && error.error.message) {
              errorMessage = error.error.message;
            }

            this.notificationService.showNotification({
              type: 'error',
              message: errorMessage
            });
          }
        });
    } else {
      // For edit mode, directly update the time entry
      this.updateTimeEntry(formData);
    }
  }

  private createTimeEntry(formData: any): void {
    this.http.post<BaseResponse<any>>(`${this.baseUrl}/api/time-entries`, formData)
      .subscribe({
        next: (response) => {
          if (response.status === 'success') {
            this.notificationService.showSuccess(response.message || 'Time entry created successfully!');
            this.dialogRef.close(true);
          } else {
            this.notificationService.showError(response.message || 'Failed to create time entry');
          }
        },
        error: (error) => {
          // Extract error message and let the service transform it
          const errorMessage = error.error?.message || 'Failed to create time entry. Please try again.';
          this.notificationService.showError(errorMessage);
        }
      });
  }

  private updateTimeEntry(formData: any): void {
    this.http.put<BaseResponse<any>>(`${this.baseUrl}/api/time-entries/${this.data.timeEntry.id}`, formData)
      .subscribe({
        next: (response) => {
          if (response.status === 'success') {
            this.notificationService.showNotification({
              type: 'success',
              message: response.message || 'Time entry updated successfully!'
            });
            this.dialogRef.close(true);
          } else {
            this.notificationService.showNotification({
              type: 'error',
              message: response.message || 'Failed to update time entry'
            });
          }
        },
        error: (error) => {
          // Try to extract message from backend error response
          let errorMessage = 'Failed to update time entry. Please try again.';
          if (error.error && error.error.message) {
            errorMessage = error.error.message;
          }

          this.notificationService.showNotification({
            type: 'error',
            message: errorMessage
          });
        }
      });
  }

  formatDate(date: Date | string | null): string {
    if (!date) {
      return '';
    }

    const d = date instanceof Date ? date : new Date(date);

    // Check if date is valid
    if (isNaN(d.getTime())) {
      return '';
    }

    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
