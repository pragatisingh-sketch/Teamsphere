import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { HttpClient } from '@angular/common/http';
import { NotificationService } from '../../shared/notification.service';
import { environment } from '../../../environments/environment';
import { BaseResponse } from '../../model/base-response.model';
import { ConfirmationDialogComponent } from '../../confirm-dialog/confirmation-dialog.component';
import { MatDialog } from '@angular/material/dialog';

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

interface Employee {
    id: number;
    ldap: string;
    shift: string;
    lead: string;
    firstName: string;
    lastName: string;
}

@Component({
    selector: 'app-multiple-time-entry-form',
    templateUrl: './multiple-time-entry-form.component.html',
    styleUrls: ['./multiple-time-entry-form.component.css']
})
export class MultipleTimeEntryFormComponent implements OnInit {
    mainForm: FormGroup;
    baseUrl = environment.apiUrl;
    projects: Project[] = [];
    activities: Activity[] = [];
    currentUserLdap: string = '';
    currentUserLeadId: number | null = null;
    currentUserLeadName: string = '';
    attendanceType: string = '';
    isOvertimeEntry: boolean = false;
    maxTimeInMins = 480;

    leads: any[] = [];
    filteredLeads: any[] = []; // Added filteredLeads

    // New properties for features
    isLeadOrManager: boolean = false;
    isOnBehalfMode: boolean = false;
    teamMembers: User[] = [];
    filteredTeamMembers: User[] = [];
    showHolidayWarning: boolean = false;
    holidayMessage: string = '';

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
        { name: 'MS2/SLH', value: 'MS2/SLH' },
        { name: 'S1/PAT', value: 'S1/PAT' },
        { name: 'S2/PAT', value: 'S2/PAT' },
        { name: 'S3/PAT', value: 'S3/PAT' },
        { name: 'MS/PAT', value: 'MS/PAT' },
        { name: 'MS2/PAT', value: 'MS2/PAT' },
        { name: 'Maternity', value: 'Maternity' }
    ];

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

    constructor(
        private fb: FormBuilder,
        private http: HttpClient,
        private notificationService: NotificationService,
        public dialogRef: MatDialogRef<MultipleTimeEntryFormComponent>,
        private dialog: MatDialog,
        @Inject(MAT_DIALOG_DATA) public data: any
    ) {
        this.mainForm = this.fb.group({
            entryDate: [new Date(), Validators.required],
            ldap: ['', Validators.required],
            leadId: ['', Validators.required],
            attendanceType: ['', Validators.required],
            projectBlocks: this.fb.array([])
        });
    }

    ngOnInit(): void {
        this.loadCurrentUser();
        this.loadActivities();
        this.loadProjects();

        // Listen for date changes to check for holidays
        this.mainForm.get('entryDate')?.valueChanges.subscribe(date => {
            if (date) {
                this.checkForHoliday(date);
            }
        });

        // Listen for ldap changes in on-behalf mode
        this.mainForm.get('ldap')?.valueChanges.subscribe(ldap => {
            if (this.isOnBehalfMode && this.isLeadOrManager && ldap) {
                this.fetchLeadForTeamMember(ldap);
            }
        });
    }

    get projectBlocks(): FormArray {
        return this.mainForm.get('projectBlocks') as FormArray;
    }

    createProjectBlock(): FormGroup {
        return this.fb.group({
            projectId: ['', Validators.required],
            process: ['', Validators.required],
            activity: ['PRODUCTION', Validators.required],
            timeInMins: [0, [Validators.required, Validators.min(1)]],
            comment: ['']
        });
    }

    addProjectBlock(): void {
        const newBlock = this.createProjectBlock();

        // Auto-fill the first project if available
        if (this.projects.length > 0) {
            const firstProjectId = this.projects[0].projectId || this.projects[0].id;
            newBlock.patchValue({
                projectId: firstProjectId
            });
        }

        // Subscribe to activity changes in this block to update attendance types
        newBlock.get('activity')?.valueChanges.subscribe(() => {
            this.updateAttendanceTypes();
        });

        this.projectBlocks.push(newBlock);
    }

    /**
     * Updates the filtered attendance types based on activities selected in project blocks.
     * - If any block has ABSENTEEISM activity → shows leave attendance types
     * - If any block has COMPOFF activity → shows compoff attendance types
     * - Otherwise → shows normal attendance types
     */
    private updateAttendanceTypes(): void {
        let hasAbsenteeism = false;
        let hasCompoff = false;

        // Check all project blocks for special activities
        for (let i = 0; i < this.projectBlocks.length; i++) {
            const activity = this.projectBlocks.at(i).get('activity')?.value;
            if (activity === 'ABSENTEEISM') {
                hasAbsenteeism = true;
                break;
            } else if (activity === 'COMPOFF') {
                hasCompoff = true;
            }
        }

        const currentType = this.mainForm.get('attendanceType')?.value;

        if (hasAbsenteeism) {
            this.filteredAttendanceTypes = this.leaveAttendanceTypes;
            // Reset attendance type if current value is not in leave options
            if (currentType && !this.leaveAttendanceTypes.some(t => t.value === currentType)) {
                this.mainForm.get('attendanceType')?.setValue('');
            }
        } else if (hasCompoff) {
            this.filteredAttendanceTypes = this.comoffAttendanceTypes;
            // Reset attendance type if current value is not in compoff options
            if (currentType && !this.comoffAttendanceTypes.some(t => t.value === currentType)) {
                this.mainForm.get('attendanceType')?.setValue('');
            }
        } else {
            this.filteredAttendanceTypes = this.attendanceTypes;
        }
    }

    removeProjectBlock(index: number): void {
        this.projectBlocks.removeAt(index);
        // Recalculate attendance types since the activities may have changed
        this.updateAttendanceTypes();
    }

    loadCurrentUser(): void {
        this.http.get<BaseResponse<User>>(`${this.baseUrl}/api/projects/current-user`)
            .subscribe({
                next: (response) => {
                    if (response.status === 'success') {
                        const user = response.data;
                        this.currentUserLdap = user.ldap;
                        this.isLeadOrManager = ['LEAD', 'MANAGER', 'ADMIN_OPS_MANAGER'].includes(user.role);

                        this.mainForm.patchValue({
                            ldap: user.ldap
                        });

                        // Fetch employee details to get shift and lead
                        this.fetchEmployeeDetails(user.ldap);

                        // If user is a lead, manager, or admin_ops_manager, load team members
                        if (this.isLeadOrManager) {
                            this.loadTeamMembers();
                        }
                    }
                },
                error: (error) => {
                    const errorMessage = error.error?.message || 'Failed to load user information';
                    this.notificationService.showError(errorMessage);
                }
            });
    }

    fetchEmployeeDetails(ldap: string): void {
        this.http.get<BaseResponse<Employee>>(`${this.baseUrl}/api/employees/by-ldap/${ldap}`)
            .subscribe({
                next: (response) => {
                    if (response.status === 'success') {
                        const employee = response.data;

                        // Map shift to attendance type
                        this.attendanceType = this.mapShiftToAttendanceType(employee.shift);
                        this.mainForm.patchValue({
                            attendanceType: this.attendanceType
                        });


                        // Get lead ID
                        this.fetchLeadId(employee.lead, ldap);

                        // Load projects for this user
                        this.loadProjects(ldap);
                    }
                },
                error: (error) => {
                    console.error('Failed to fetch employee details:', error);
                }
            });
    }

    mapShiftToAttendanceType(shift: string): string {
        // Map shift codes to attendance types
        const shiftMapping: { [key: string]: string } = {
            'S1': 'S1/F',
            'S2': 'S2/F',
            'S3': 'S3/F',
            'MS': 'MS/F',
            'MS2': 'MS2/F'
        };

        return shiftMapping[shift] || 'S1/F'; // Default to S1/F
    }

    fetchLeadId(leadLdap: string, employeeLdap: string): void {
        this.http.get<BaseResponse<User[]>>(`${this.baseUrl}/api/employees/my-team-leads/${employeeLdap}`)
            .subscribe({
                next: (response) => {
                    if (response.status === 'success') {
                        this.leads = response.data;
                        this.filteredLeads = response.data; // Update filteredLeads
                        const lead = response.data.find(l => l.ldap === leadLdap);
                        if (lead) {
                            this.currentUserLeadId = lead.id;
                            this.currentUserLeadName = lead.ldap;
                            this.mainForm.patchValue({
                                leadId: lead.id
                            });
                        }
                    }
                },
                error: (error) => {
                    console.error('Failed to fetch lead:', error);
                }
            });
    }

    filterLeads(event: any): void {
        const searchText = (event.target.value || '').toLowerCase();
        this.filteredLeads = this.leads.filter(lead =>
            lead.ldap.toLowerCase().includes(searchText) ||
            (lead.role && lead.role.toLowerCase().includes(searchText))
        );
    }

    getLeadName(leadId: number): string {
        const lead = this.leads.find(l => l.id === leadId);
        return lead ? lead.ldap : '';
    }

    loadProjects(ldap?: string): void {
        let url = `${this.baseUrl}/api/time-entries/my-projects`;
        if (ldap) {
            url += `?ldap=${ldap}`;
        }

        this.http.get<BaseResponse<Project[]>>(url)
            .subscribe({
                next: (response) => {
                    if (response.status === 'success') {
                        this.projects = response.data;

                        // Add first project block by default ONLY if no blocks exist yet
                        // (Usually on initial load or reset)
                        if (this.projects.length > 0 && this.projectBlocks.length === 0) {
                            this.addProjectBlock();
                        }

                        // If blocks exist, we might want to reset their project selection or leave it
                        // Since we just loaded new projects, the existing selection in blocks might be invalid
                        // For now, let's just update the options. If the previously selected project ID 
                        // isn't in the new list, the select will show empty or invalid state, which is expected behavior
                    }
                },
                error: (error) => {
                    const errorMessage = error.error?.message || 'Failed to load projects';
                    this.notificationService.showError(errorMessage);
                }
            });
    }

    loadActivities(): void {
        this.http.get<BaseResponse<string[]>>(`${this.baseUrl}/api/projects/activities`)
            .subscribe({
                next: (response) => {
                    if (response.status === 'success') {
                        this.activities = response.data.map(name => ({
                            name: name,
                            value: name.toUpperCase().replace(/\s+/g, '_')
                        }));
                    }
                },
                error: (error) => {
                    const errorMessage = error.error?.message || 'Failed to load activities';
                    this.notificationService.showError(errorMessage);
                }
            });
    }

    getTotalTime(): number {
        let total = 0;
        this.projectBlocks.controls.forEach(block => {
            const time = block.get('timeInMins')?.value || 0;
            total += parseInt(time.toString(), 10);
        });
        return total;
    }

    showTimeWarning(): boolean {
        const total = this.getTotalTime();
        if (this.isOvertimeEntry) {
            return false; // No warning for overtime entries
        }
        return total !== this.maxTimeInMins;
    }

    getTimeWarningMessage(): string {
        const total = this.getTotalTime();
        if (total < this.maxTimeInMins) {
            return `Total time is ${total} minutes. You still have ${this.maxTimeInMins - total} minutes remaining for the day.`;
        } else if (total > this.maxTimeInMins) {
            return `Total time is ${total} minutes, which exceeds the daily limit of ${this.maxTimeInMins} minutes. Please enable overtime or adjust times.`;
        }
        return '';
    }

    toggleOvertimeMode(): void {
        // Check if any selected project is overtime eligible
        if (!this.hasOvertimeEligibleProject() && !this.isOvertimeEntry) {
            this.notificationService.showWarning('None of the selected projects are overtime eligible. Please select an overtime-eligible project first.');
            return;
        }
        this.isOvertimeEntry = !this.isOvertimeEntry;
    }

    getProjectName(projectId: number): string {
        const project = this.projects.find(p => (p.projectId || p.id) === projectId);
        return project?.projectName || project?.name || '';
    }

    onProjectSelected(index: number, projectId: number): void {
        // You can add logic here if needed when a project is selected
        console.log(`Project ${projectId} selected for block ${index}`);
    }

    hasOvertimeEligibleProject(): boolean {
        // Check if any selected project in the form blocks is overtime eligible
        for (let i = 0; i < this.projectBlocks.length; i++) {
            const projectId = this.projectBlocks.at(i).get('projectId')?.value;
            if (projectId) {
                const project = this.projects.find(p => (p.projectId || p.id) === projectId);
                if (project?.isOvertimeEligible === true) {
                    return true;
                }
            }
        }
        return false;
    }

    formatDate(date: Date): string {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    isAddBlockDisabled(): boolean {
        if (this.isOvertimeEntry) {
            return false; // If overtime is enabled, allow adding more entries
        }
        const total = this.getTotalTime();
        return total >= this.maxTimeInMins;
    }

    getAddBlockTooltip(): string {
        if (this.isAddBlockDisabled()) {
            return 'You have already filled 480 minutes for the day. Please enable overtime toggle to fill extra time for the day.';
        }
        return 'Add another time entry for a different project/process';
    }

    onSubmit(): void {
        if (this.mainForm.invalid || this.projectBlocks.length === 0) {
            this.notificationService.showNotification({
                type: 'warning',
                message: 'Please fill all required fields and add at least one project entry.'
            });
            return;
        }

        const total = this.getTotalTime();
        if (!this.isOvertimeEntry && total > this.maxTimeInMins) {
            this.notificationService.showNotification({
                type: 'error',
                message: `Total time (${total} minutes) exceeds daily limit of ${this.maxTimeInMins} minutes. Please enable overtime or adjust times.`
            });
            return;
        }

        // Prepare time entries for bulk creation
        const formData = this.mainForm.value;
        const entryDate = this.formatDate(formData.entryDate);

        const timeEntries = this.projectBlocks.controls.map(block => ({
            entryDate: entryDate,
            ldap: formData.ldap,
            leadId: formData.leadId,
            projectId: block.get('projectId')?.value,
            process: block.get('process')?.value,
            activity: block.get('activity')?.value,
            timeInMins: block.get('timeInMins')?.value,
            attendanceType: formData.attendanceType,
            comment: block.get('comment')?.value,
            isOvertime: this.isOvertimeEntry
        }));

        // Send bulk request
        this.http.post<BaseResponse<any>>(`${this.baseUrl}/api/time-entries/bulk`, timeEntries)
            .subscribe({
                next: (response) => {
                    if (response.status === 'success') {
                        this.notificationService.showNotification({
                            type: 'success',
                            message: `Successfully created ${timeEntries.length} time entries!`
                        });
                        this.dialogRef.close(true);
                    } else {
                        this.notificationService.showNotification({
                            type: 'error',
                            message: response.message || 'Failed to create time entries'
                        });
                    }
                },
                error: (error) => {
                    const errorMessage = error.error?.message || 'Failed to create time entries. Please try again.';
                    this.notificationService.showError(errorMessage);
                }
            });
    }

    onCancel(): void {
        this.dialogRef.close(false);
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
                    const errorMessage = error.error?.message || 'Failed to load team members. Please try again.';
                    this.notificationService.showError(errorMessage);
                }
            });
    }

    filterTeamMembers(event: any): void {
        const searchText = (event.target.value || '').toLowerCase();
        this.filteredTeamMembers = this.teamMembers.filter(member =>
            member.ldap.toLowerCase().includes(searchText)
        );
    }

    toggleOnBehalfMode(): void {
        this.isOnBehalfMode = !this.isOnBehalfMode;

        if (this.isOnBehalfMode) {
            // Switch to on behalf mode - clear fields
            this.mainForm.patchValue({
                ldap: '',
                leadId: null,
                attendanceType: ''
            });
        } else {
            // Switch back to personal mode
            this.mainForm.patchValue({
                ldap: this.currentUserLdap
            });
            // Re-fetch details for current user
            this.fetchEmployeeDetails(this.currentUserLdap);
        }
    }

    fetchLeadForTeamMember(ldap: string): void {
        this.fetchEmployeeDetails(ldap);
    }

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
                    this.showHolidayWarning = false;
                }
            });
    }

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
                this.dialogRef.close(false);
            } else if (result === true) {
                this.autofillHolidayForm(holidayName);
            }
        });
    }

    private autofillHolidayForm(holidayName?: string): void {
        // Clear all existing project blocks
        while (this.projectBlocks.length !== 0) {
            this.projectBlocks.removeAt(0);
        }

        // Add one block for holiday
        this.addProjectBlock();

        // Get the newly added block
        const block = this.projectBlocks.at(0);

        // Use the first available project (or user can change it)
        const projectId = this.projects.length > 0 ? (this.projects[0].projectId || this.projects[0].id) : null;

        block.patchValue({
            projectId: projectId,
            process: 'Holiday',
            activity: 'HOLIDAY',
            timeInMins: 480,
            comment: holidayName ? `Google Holiday - ${holidayName}` : 'Google Holiday'
        });

        this.mainForm.patchValue({
            attendanceType: 'S1/GO'
        });

        // Mark as dirty
        this.mainForm.markAsDirty();
    }
}
