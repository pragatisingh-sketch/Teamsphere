import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UserService } from '../user.service';
import { DateFormatService } from '../services/date-format.service';

@Component({
  selector: 'app-delegation-history-dialog',
  templateUrl: './delegation-history-dialog.component.html',
  styleUrls: ['./delegation-history-dialog.component.css']
})
export class DelegationHistoryDialogComponent implements OnInit {
  history: any[] = [];
  loading = true;
  displayedColumns: string[] = ['delegatorLdap', 'delegateeLdap', 'startDate', 'endDate', 'status', 'originalRole'];

  constructor(
    public dialogRef: MatDialogRef<DelegationHistoryDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { user: any },
    private userService: UserService,
    private dateFormatService: DateFormatService
  ) { }

  ngOnInit(): void {
    console.log('DelegationHistoryDialog - User data:', this.data.user);
    if (this.data.user && this.data.user.ldap) {
      console.log('Fetching delegation history for LDAP:', this.data.user.ldap);
      this.userService.getDelegationHistory(this.data.user.ldap).subscribe(
        (response) => {
          console.log('Delegation history response:', response);
          this.history = response.data || [];
          console.log('History array:', this.history);
          this.loading = false;
        },
        (error) => {
          console.error('Error fetching delegation history:', error);
          console.error('Error details:', error.error);
          this.loading = false;
        }
      );
    } else {
      console.warn('No user or ldap found in data');
      this.loading = false;
    }
  }

  formatDate(date: string): string {
    return this.dateFormatService.formatDate(date);
  }

  close(): void {
    this.dialogRef.close();
  }
}
