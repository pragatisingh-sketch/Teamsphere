import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TimeSheetService, Attendance } from '../services/timesheet.service';
import { NgForm } from '@angular/forms';
import { NotificationService } from '../shared/notification.service';

@Component({
  selector: 'app-timesheet-details',
  templateUrl: './timesheet-details.component.html',
  styleUrls: ['./timesheet-details.component.css']
})
export class TimesheetDetailsComponent implements OnInit {
  timesheet: Attendance = {
    id: '',
    ldap: '',
    masked_orgid: '',
    subrole: '',
    role: '',
    date: '',
    process: '',
    billingCode: '',
    activity: '',
    status: '',
    lead_ldap: '',
    vendor: '',
    minutes: '',
    project: '',
    team: '',
    comment: ''
  };
  
  isEditMode = false;

  // Dropdown options
  levels: string[] = [
    'Tech I', 
    'Tech II', 
    'Tech III', 
    'i18n', 
    'Team Lead', 
    'Account Manager', 
    'Program Manager'
  ];

  processes: string[] = [
    'KG_VF', 
    'SEI', 
    'HUME', 
    'MAGI Feedback'
  ];

  teams: string[] = [
    'Accounts',
    'AIO UX Writers',
    'Automation',
    'DaaS Monitoring Team',
    'Quality',
    'Team Ontology',
    'VF Data GUR',
    'VF WW Lang GUR',
    'SEI',
    'ICO Team 0',
    'ICO Team 1',
    'ICO Team 2',
    'ICO Team 3',
    'ICO Team 4',
    'Team Bengali',
    'Team Gujarati',
    'Team Hindi',
    'Team Malayalam',
    'Team Marathi',
    'Team Nepali',
    'Team Punjabi',
    'Team Telugu',
    'Team Urdu',
    'MAGI Feedback Team 1',
    'MAGI Feedback Team 2',
    'MAGI Feedback Team 3'
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private timesheetService: TimeSheetService,
    private notificationService: NotificationService
  ) { }

  ngOnInit(): void {
    const timesheetId = this.route.snapshot.paramMap.get('id');
    if (timesheetId) {
      this.isEditMode = true;
      this.timesheetService.getTimesheetById(timesheetId).subscribe({
        next: (data) => {
          this.timesheet = data;
        },
        error: (error) => {
          console.error('Error fetching timesheet:', error);
          this.notificationService.showNotification({
            type: 'error',
            message: 'Failed to load timesheet data'
          });
        }
      });
    }
  }

  onSubmit(form: NgForm): void {
    if (form.valid) {
      const role = localStorage.getItem("role");
      const successMessage = role === 'ADMIN_OPS_MANAGER' ? 
        `Timesheet ${this.isEditMode ? 'updated' : 'added'} successfully!` : 
        `Timesheet ${this.isEditMode ? 'edit' : 'creation'} request has been sent for approval`;

      if (this.isEditMode) {
        this.timesheetService.updateAttendanceRecord(this.timesheet).subscribe({
          next: () => {
            this.notificationService.showNotification({
              type: 'success',
              message: successMessage
            });
            this.router.navigate(['/attendance']);
          },
          error: (error) => {
            console.error('Error updating timesheet:', error);
            this.notificationService.showNotification({
              type: 'error',
              message: 'Failed to update timesheet'
            });
          }
        });
      } else {
        this.timesheetService.addAttendanceRecord(this.timesheet).subscribe({
          next: () => {
            this.notificationService.showNotification({
              type: 'success',
              message: successMessage
            });
            this.router.navigate(['/attendance']);
          },
          error: (error) => {
            console.error('Error adding timesheet:', error);
            this.notificationService.showNotification({
              type: 'error',
              message: 'Failed to add timesheet'
            });
          }
        });
      }
    } else {
      this.notificationService.showNotification({
        type: 'error',
        message: 'Please fill all required fields correctly'
      });
    }
  }

  hasEditAccess(): boolean {
    return !(localStorage.getItem("role") === 'ACCOUNT_MANAGER');
  }

  cancel(): void {
    this.router.navigate(['/attendance']);
  }
}