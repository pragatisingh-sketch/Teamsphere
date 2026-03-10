import { Component, Inject, OnInit, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { UserService } from '../user.service';
import { NotificationService } from '../shared/notification.service';

export interface UserEditLog {
  id: number;
  userLdap: string;
  fieldName: string;
  oldValue: string;
  newValue: string;
  changedBy: string;
  changedAt: Date;
}

@Component({
  selector: 'app-edit-logs-dialog',
  templateUrl: './edit-logs-dialog.component.html',
  styleUrls: ['./edit-logs-dialog.component.css']
})
export class EditLogsDialogComponent implements OnInit {
  displayedColumns: string[] = ['fieldName', 'oldValue', 'newValue', 'changedBy', 'changedAt'];
  dataSource = new MatTableDataSource<UserEditLog>([]);
  isLoading = true;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    public dialogRef: MatDialogRef<EditLogsDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { user: any },
    private userService: UserService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadEditLogs();
  }

  ngAfterViewInit() {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  loadEditLogs(): void {
    this.isLoading = true;
    this.userService.getUserEditLogs(this.data.user.ldap).subscribe({
      next: (response) => {
        this.dataSource.data = response.data;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading edit logs:', error);
        this.notificationService.showNotification({
          type: 'error',
          message: error.error?.message || 'Failed to load edit logs. Please try again later.'
        });
        this.isLoading = false;
      }
    });
  }

  applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  close(): void {
    this.dialogRef.close();
  }
}
