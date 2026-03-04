import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { AttendanceService } from 'src/app/services/attendance.service';

export interface AuditHistoryDialogData {
  attendanceId: number;
  employeeName: string;
  date: string;
}

export interface AuditRecord {
  id: number;
  actionType: string;
  previousStatus?: string;
  newStatus?: string;
  previousCompliance?: boolean;
  newCompliance?: boolean;
  changedBy: string;
  changedByRole: string;
  changedAt: string;
  changeReason: string;
  changeDescription: string;
}

@Component({
  selector: 'app-attendance-audit-dialog',
  template: `
    <h2 mat-dialog-title>Attendance Modification History</h2>
    
    <mat-dialog-content>
      <div class="employee-info">
        <p><strong>Employee:</strong> {{ data.employeeName }}</p>
        <p><strong>Date:</strong> {{ data.date }}</p>
      </div>

      <div *ngIf="loading" class="loading-container">
        <mat-spinner diameter="40"></mat-spinner>
        <p>Loading audit history...</p>
      </div>

      <div *ngIf="!loading && auditRecords.length === 0" class="no-records">
        <mat-icon>info</mat-icon>
        <p>No modification history found for this attendance record.</p>
      </div>

      <div *ngIf="!loading && auditRecords.length > 0" class="audit-timeline">
        <div *ngFor="let record of auditRecords" class="audit-item">
          <div class="audit-icon">
            <mat-icon [class.status-change]="record.actionType === 'STATUS_UPDATE'"
                      [class.compliance-change]="record.actionType === 'COMPLIANCE_UPDATE'">
              {{ record.actionType === 'STATUS_UPDATE' ? 'edit' : 'verified_user' }}
            </mat-icon>
          </div>
          
          <div class="audit-content">
            <div class="audit-header">
              <span class="action-type">{{ getActionTypeLabel(record.actionType) }}</span>
              <span class="timestamp">{{ formatDate(record.changedAt) }}</span>
            </div>
            
            <div class="audit-details">
              <p class="changed-by">
                <mat-icon>person</mat-icon>
                {{ record.changedBy }} ({{ record.changedByRole }})
              </p>
              
              <div class="change-info" *ngIf="record.actionType === 'STATUS_UPDATE'">
                <div class="change-row">
                  <span class="label">Previous:</span>
                  <span class="value old">{{ record.previousStatus || 'Not set' }}</span>
                </div>
                <div class="change-row">
                  <span class="label">New:</span>
                  <span class="value new">{{ record.newStatus }}</span>
                </div>
              </div>

              <div class="change-info" *ngIf="record.actionType === 'COMPLIANCE_UPDATE'">
                <div class="change-row">
                  <span class="label">Previous:</span>
                  <span class="value" [class.compliant]="!record.previousCompliance" 
                        [class.non-compliant]="record.previousCompliance">
                    {{ record.previousCompliance ? 'Non-Compliant' : 'Compliant' }}
                  </span>
                </div>
                <div class="change-row">
                  <span class="label">New:</span>
                  <span class="value" [class.compliant]="!record.newCompliance" 
                        [class.non-compliant]="record.newCompliance">
                    {{ record.newCompliance ? 'Non-Compliant' : 'Compliant' }}
                  </span>
                </div>
              </div>

              <div class="reason-box">
                <strong>Reason:</strong>
                <p>{{ record.changeReason }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </mat-dialog-content>

    <mat-dialog-actions class="dialog-actions-end">
      <button mat-raised-button color="primary" (click)="onClose()">Close</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .employee-info {
      background-color: #f5f5f5;
      padding: 12px;
      border-radius: 4px;
      margin-bottom: 20px;
    }

    .employee-info p {
      margin: 4px 0;
      font-size: 14px;
    }

    .loading-container {
      text-align: center;
      padding: 40px 20px;
    }

    .loading-container p {
      margin-top: 16px;
      color: #666;
    }

    .no-records {
      text-align: center;
      padding: 40px 20px;
      color: #666;
    }

    .no-records mat-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: #999;
    }

    .audit-timeline {
      position: relative;
      padding-left: 40px;
    }

    .audit-timeline::before {
      content: '';
      position: absolute;
      left: 19px;
      top: 0;
      bottom: 0;
      width: 2px;
      background: #e0e0e0;
    }

    .audit-item {
      position: relative;
      margin-bottom: 24px;
      display: flex;
      gap: 16px;
    }

    .audit-icon {
      position: absolute;
      left: -40px;
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background: white;
      border: 2px solid #e0e0e0;
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1;
    }

    .audit-icon mat-icon {
      font-size: 20px;
      width: 20px;
      height: 20px;
    }

    .audit-icon mat-icon.status-change {
      color: #2196f3;
    }

    .audit-icon mat-icon.compliance-change {
      color: #ff9800;
    }

    .audit-content {
      flex: 1;
      background: #fafafa;
      border-radius: 8px;
      padding: 12px 16px;
      border-left: 3px solid #2196f3;
    }

    .audit-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
    }

    .action-type {
      font-weight: 600;
      color: #333;
      font-size: 15px;
    }

    .timestamp {
      font-size: 12px;
      color: #666;
    }

    .changed-by {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      color: #555;
      margin-bottom: 12px;
    }

    .changed-by mat-icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
    }

    .change-info {
      background: white;
      padding: 10px;
      border-radius: 4px;
      margin-bottom: 12px;
    }

    .change-row {
      display: flex;
      gap: 12px;
      margin-bottom: 6px;
      font-size: 13px;
    }

    .change-row:last-child {
      margin-bottom: 0;
    }

    .change-row .label {
      font-weight: 500;
      min-width: 70px;
      color: #666;
    }

    .change-row .value {
      flex: 1;
    }

    .change-row .value.old {
      color: #f44336;
      text-decoration: line-through;
    }

    .change-row .value.new {
      color: #4caf50;
      font-weight: 500;
    }

    .change-row .value.compliant {
      color: #4caf50;
      font-weight: 500;
    }

    .change-row .value.non-compliant {
      color: #f44336;
      font-weight: 500;
    }

    .reason-box {
      background: #fff3cd;
      padding: 10px;
      border-radius: 4px;
      border-left: 3px solid #ffc107;
    }

    .reason-box strong {
      font-size: 13px;
      color: #856404;
    }

    .reason-box p {
      margin: 6px 0 0 0;
      font-size: 13px;
      color: #856404;
      line-height: 1.5;
    }

    mat-dialog-content {
      min-width: 500px;
      max-width: 600px;
      max-height: 70vh;
    }

    .dialog-actions-end {
      justify-content: flex-end;
      padding: 16px 24px;
    }
  `]
})
export class AttendanceAuditDialogComponent implements OnInit {
  auditRecords: AuditRecord[] = [];
  loading = true;

  constructor(
    public dialogRef: MatDialogRef<AttendanceAuditDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AuditHistoryDialogData,
    private attendanceService: AttendanceService
  ) { }

  ngOnInit(): void {
    this.loadAuditHistory();
  }

  loadAuditHistory(): void {
    this.loading = true;
    this.attendanceService.getAuditHistory(this.data.attendanceId).subscribe({
      next: (response) => {
        this.auditRecords = response.data || [];
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading audit history:', error);
        this.loading = false;
      }
    });
  }

  getActionTypeLabel(actionType: string): string {
    switch (actionType) {
      case 'STATUS_UPDATE':
        return 'Status Modified';
      case 'COMPLIANCE_UPDATE':
        return 'Compliance Modified';
      default:
        return actionType;
    }
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  onClose(): void {
    this.dialogRef.close();
  }
}
