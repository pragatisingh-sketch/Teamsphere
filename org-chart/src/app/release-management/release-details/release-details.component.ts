import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ReleaseService, Release } from '../../services/release.service';
import { NotificationService } from '../../shared/notification.service';

@Component({
    selector: 'app-release-details',
    templateUrl: './release-details.component.html',
    styleUrls: ['./release-details.component.css']
})
export class ReleaseDetailsComponent implements OnInit {
    release: Release | null = null;
    isLoading = true;

    // Group items by type for display
    bugFixes: any[] = [];
    enhancements: any[] = [];
    features: any[] = [];

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private releaseService: ReleaseService,
        private notificationService: NotificationService
    ) { }

    ngOnInit(): void {
        const id = this.route.snapshot.paramMap.get('id');
        if (id) {
            this.loadRelease(+id);
        } else {
            this.router.navigate(['/releases']);
        }
    }

    loadRelease(id: number): void {
        this.isLoading = true;
        this.releaseService.getReleaseById(id).subscribe({
            next: (response) => {
                if (response.status === 'success') {
                    this.release = response.data;
                    this.groupItemsByType();
                }
                this.isLoading = false;
            },
            error: (err) => {
                this.notificationService.showError(err.error?.message || 'Failed to load release');
                this.router.navigate(['/releases']);
                this.isLoading = false;
            }
        });
    }

    groupItemsByType(): void {
        if (!this.release?.releaseItems) return;

        this.bugFixes = this.release.releaseItems.filter(
            item => item.type === 'BUG_FIX' || item.type === 'HOTFIX'
        );
        this.enhancements = this.release.releaseItems.filter(
            item => item.type === 'ENHANCEMENT'
        );
        this.features = this.release.releaseItems.filter(
            item => item.type === 'FEATURE'
        );
    }

    goBack(): void {
        this.router.navigate(['/releases']);
    }

    formatDate(dateString: string): string {
        return new Date(dateString).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });
    }
}
