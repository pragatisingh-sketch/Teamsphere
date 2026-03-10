import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { IssueService, Issue } from '../../services/issue.service';
import { NotificationService } from '../../shared/notification.service';
import { TableConfig, TableEvent } from '../../shared/components/reusable-table/table-config.interface';

@Component({
  selector: 'app-issue-list',
  templateUrl: './issue-list.component.html',
  styleUrls: ['./issue-list.component.css']
})
export class IssueListComponent implements OnInit {

  issues: Issue[] = [];
  isLoading = true;

  tableConfig: TableConfig = {
    title: '',
    columns: [
      { key: 'id', label: 'ID', sortable: true, filterable: true, width: '60px' },
      { key: 'title', label: 'Title', sortable: true, filterable: true, clickable: true },
      { key: 'type', label: 'Type', sortable: true, filterable: true, type: 'custom', template: 'badge' },
      { key: 'priority', label: 'Priority', sortable: true, filterable: true, type: 'custom', template: 'badge' },
      { key: 'status', label: 'Status', sortable: true, filterable: true, type: 'custom', template: 'badge' },
      { key: 'reporterEmail', label: 'Reporter', sortable: true, filterable: true },
      { key: 'createdAt', label: 'Created At', sortable: true, type: 'date', format: (v) => new Date(v).toLocaleDateString() }
    ],
    showGlobalSearch: true,
    showColumnFilters: true,
    showPagination: true,
    pageSize: 10,
    actions: [
      {
        id: 'view',
        label: 'View',
        icon: 'visibility',
        handler: (row) => this.onRowClick(row)
      }
    ],
    rowClick: (row) => this.onRowClick(row)
  };

  constructor(private issueService: IssueService, private notificationService: NotificationService, private router: Router) { }

  ngOnInit(): void {
    this.loadIssues();
  }

  loadIssues() {
    this.isLoading = true;
    this.issueService.getAllIssues().subscribe({
      next: (data) => {
        this.issues = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.notificationService.showNotification({
          type: 'error',
          message: err.error?.message || 'Error loading issues'
        });
        this.isLoading = false;
      }
    });
  }

  updateStatus(issue: Issue, newStatus: string) {
    if (issue.status === newStatus || !issue.id) return;

    this.issueService.updateIssue(issue.id, { ...issue, status: newStatus }).subscribe({
      next: (res) => {
        this.notificationService.showNotification({
          type: 'success',
          message: `Issue #${issue.id} marked as ${newStatus}`
        });
        this.loadIssues(); // Reload to reflect changes
      },
      error: (err) => {
        this.notificationService.showNotification({
          type: 'error',
          message: err.error?.message || 'Failed to update status'
        });
      }
    });
  }

  onRowClick(row: Issue) {
    if (row.id) {
      this.router.navigate(['/issues', row.id]);
    }
  }

  onTableEvent(event: TableEvent) {
    // Handle specific table events if needed
  }
}
