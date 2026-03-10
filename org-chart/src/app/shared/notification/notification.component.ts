import { Component, OnInit, OnDestroy } from '@angular/core';
import { NotificationService, Notification } from '../notification.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-notification',
  templateUrl: './notification.component.html',
  styleUrls: ['./notification.component.css']
})
export class NotificationComponent implements OnInit, OnDestroy {
  notification: Notification | null = null;
  private notificationSubscription: Subscription | undefined;

  constructor(private notificationService: NotificationService) {}

  ngOnInit(): void {
    this.notificationSubscription = this.notificationService.notification$.subscribe(
      (notification) => {
        this.notification = notification;
        setTimeout(() => {
          this.notification = null;
        }, 3000); // Hide notification after 3 seconds
      }
    );
  }

  

  ngOnDestroy(): void {
    if (this.notificationSubscription) {
      this.notificationSubscription.unsubscribe();
    }
  }
}
