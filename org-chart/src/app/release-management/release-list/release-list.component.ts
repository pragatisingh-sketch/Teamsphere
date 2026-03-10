import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { ReleaseService, Release } from '../../services/release.service';
import { NotificationService } from '../../shared/notification.service';
import { ReleaseFormDialogComponent } from '../release-form-dialog/release-form-dialog.component';
import { SendNotificationDialogComponent } from '../send-notification-dialog/send-notification-dialog.component';
import { ConfirmationDialogComponent } from '../../confirm-dialog/confirmation-dialog.component';
import { TableConfig } from '../../shared/components/reusable-table/table-config.interface';

@Component({
    selector: 'app-release-list',
    templateUrl: './release-list.component.html',
    styleUrls: ['./release-list.component.css']
})
export class ReleaseListComponent implements OnInit {
    releases: Release[] = [];
    isLoading = true;

    tableConfig: TableConfig = {
        title: '',
        columns: [
            { key: 'version', label: 'Version', sortable: true, filterable: true, width: '100px' },
            { key: 'title', label: 'Title', sortable: true, filterable: true, clickable: true, cellClick: (value, row) => this.viewDetails(row) },
            { key: 'releaseDate', label: 'Release Date', sortable: true, type: 'date', format: (v) => v ? new Date(v).toLocaleDateString() : '' },
            { key: 'itemCount', label: 'Items', sortable: true, width: '80px', format: (v) => String(v || 0) },
            { key: 'notificationSent', label: 'Notified', sortable: true, width: '100px', format: (v) => v ? 'Yes' : 'No' },
            { key: 'createdBy', label: 'Created By', sortable: true, filterable: true }
        ],
        showGlobalSearch: true,
        showColumnFilters: true,
        showPagination: true,
        pageSize: 10,
        actions: [
            {
                id: 'edit',
                label: 'Edit',
                icon: 'edit',
                handler: (row) => this.openEditDialog(row)
            },
            {
                id: 'notify',
                label: 'Send Notification',
                icon: 'send',
                handler: (row) => this.openNotifyDialog(row)
            },
            {
                id: 'delete',
                label: 'Delete',
                icon: 'delete',
                color: 'warn',
                handler: (row) => this.confirmDelete(row)
            }
        ]
    };

    constructor(
        private releaseService: ReleaseService,
        private notificationService: NotificationService,
        private dialog: MatDialog,
        private router: Router
    ) { }

    ngOnInit(): void {
        this.loadReleases();
    }

    loadReleases(): void {
        this.isLoading = true;
        this.releaseService.getAllReleases().subscribe({
            next: (response) => {
                if (response.status === 'success') {
                    this.releases = response.data.map(r => ({
                        ...r,
                        itemCount: r.releaseItems?.length || 0
                    }));
                }
                this.isLoading = false;
            },
            error: (err) => {
                if (err.status === 403) {
                    this.notificationService.showError('Access denied. You are not authorized to view releases.');
                    this.router.navigate(['/dashboard']);
                } else {
                    this.notificationService.showError(err.error?.message || 'Failed to load releases');
                }
                this.isLoading = false;
            }
        });
    }

    openCreateDialog(): void {
        const dialogRef = this.dialog.open(ReleaseFormDialogComponent, {
            width: '900px',
            maxHeight: '90vh',
            data: { isEdit: false }
        });

        dialogRef.afterClosed().subscribe(result => {
            if (result) {
                this.loadReleases();
            }
        });
    }

    openEditDialog(release: Release): void {
        const dialogRef = this.dialog.open(ReleaseFormDialogComponent, {
            width: '900px',
            maxHeight: '90vh',
            data: { isEdit: true, release }
        });

        dialogRef.afterClosed().subscribe(result => {
            if (result) {
                this.loadReleases();
            }
        });
    }

    openNotifyDialog(release: Release): void {
        const dialogRef = this.dialog.open(SendNotificationDialogComponent, {
            width: '600px',
            maxHeight: '90vh',
            data: { release }
        });

        dialogRef.afterClosed().subscribe(result => {
            if (result) {
                this.loadReleases();
            }
        });
    }

    confirmDelete(release: Release): void {
        const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
            width: '400px',
            data: {
                title: 'Delete Release',
                message: `Are you sure you want to delete version ${release.version}?`,
                confirmButtonText: 'Delete',
                color: 'warn'
            }
        });

        dialogRef.afterClosed().subscribe(result => {
            if (result && release.id) {
                this.releaseService.deleteRelease(release.id).subscribe({
                    next: () => {
                        this.notificationService.showSuccess('Release deleted successfully');
                        this.loadReleases();
                    },
                    error: (err) => {
                        this.notificationService.showError(err.error?.message || 'Failed to delete release');
                    }
                });
            }
        });
    }

    viewDetails(release: Release): void {
        this.router.navigate(['/releases', release.id]);
    }
}
