import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, timer } from 'rxjs';
import { switchMap, catchError } from 'rxjs/operators';
import { UserService } from '../user.service';

export interface RequestCounts {
  pending: number;
  approved: number;
  rejected: number;
  total: number;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationCountService {
  private requestCountsSubject = new BehaviorSubject<RequestCounts>({
    pending: 0,
    approved: 0,
    rejected: 0,
    total: 0
  });

  public requestCounts$ = this.requestCountsSubject.asObservable();

  // Auto-refresh interval (in milliseconds) - refresh every 30 seconds
  private readonly REFRESH_INTERVAL = 30000;

  constructor(private userService: UserService) {
    this.startAutoRefresh();
  }

  /**
   * Manually refresh request counts
   */
  refreshCounts(): void {
    this.userService.getRequestCounts().subscribe({
      next: (response) => {
        this.requestCountsSubject.next(response.data);
      },
      error: (error) => {
        console.error('Error fetching request counts:', error);
        // Don't update counts on error to avoid showing incorrect data
      }
    });
  }

  /**
   * Get current request counts without subscribing
   */
  getCurrentCounts(): RequestCounts {
    return this.requestCountsSubject.value;
  }

  /**
   * Start auto-refresh timer
   */
  private startAutoRefresh(): void {
    // Initial load
    this.refreshCounts();

    // Set up periodic refresh
    timer(this.REFRESH_INTERVAL, this.REFRESH_INTERVAL)
      .pipe(
        switchMap(() => this.userService.getRequestCounts()),
        catchError((error) => {
          console.error('Auto-refresh error:', error);
          // Return empty observable to continue the timer
          return [];
        })
      )
      .subscribe({
        next: (response: any) => {
          if (response && response.data) {
            this.requestCountsSubject.next(response.data);
          }
        }
      });
  }

  /**
   * Get pending count as observable
   */
  getPendingCount(): Observable<number> {
    return this.requestCounts$.pipe(
      switchMap(counts => [counts.pending])
    );
  }

  /**
   * Update counts after a request is processed
   * This allows for immediate UI updates without waiting for the next refresh
   */
  updateCountsAfterAction(action: 'approve' | 'reject'): void {
    const currentCounts = this.getCurrentCounts();
    if (currentCounts.pending > 0) {
      const updatedCounts = {
        ...currentCounts,
        pending: currentCounts.pending - 1,
        approved: action === 'approve' ? currentCounts.approved + 1 : currentCounts.approved,
        rejected: action === 'reject' ? currentCounts.rejected + 1 : currentCounts.rejected
      };
      this.requestCountsSubject.next(updatedCounts);
    }
    
    // Refresh from server after a short delay to ensure accuracy
    setTimeout(() => this.refreshCounts(), 1000);
  }
}
