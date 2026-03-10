import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { ErrorMessageService } from './error-message.service';

export interface Notification {
  type: string;  // 'success', 'error', 'info', 'warning'
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private notificationSubject = new Subject<Notification>();
  notification$ = this.notificationSubject.asObservable();

  constructor(private errorMessageService: ErrorMessageService) {}

  showNotification(notification: Notification) {
    // Transform error messages to be more user-friendly
    if (notification.type === 'error') {
      notification.message = this.errorMessageService.transformErrorMessage(notification.message);
    }

    this.notificationSubject.next(notification);
  }

  /**
   * Show a user-friendly error notification
   * @param message The error message (will be transformed if technical)
   */
  showError(message: string) {
    this.showNotification({
      type: 'error',
      message: message
    });
  }

  /**
   * Show a success notification
   * @param message The success message
   */
  showSuccess(message: string) {
    this.showNotification({
      type: 'success',
      message: message
    });
  }

  /**
   * Show an info notification
   * @param message The info message
   */
  showInfo(message: string) {
    this.showNotification({
      type: 'info',
      message: message
    });
  }

  /**
   * Show a warning notification
   * @param message The warning message
   */
  showWarning(message: string) {
    this.showNotification({
      type: 'warning',
      message: message
    });
  }
}
