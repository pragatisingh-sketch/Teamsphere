import { Component, OnInit, NgZone, ApplicationRef } from '@angular/core';
import { Router, NavigationEnd, Event } from '@angular/router';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  showSidebar = true; // Default to showing sidebar

  // List of routes where sidebar should be hidden
  private noSidebarRoutes = [
    '/login',
    '/',
    '/signup',
    '/password-reset',
    '/forgot-password',
    '/otp-verification',
  ];

  constructor(
    private router: Router,
    private ngZone: NgZone,
    private appRef: ApplicationRef
  ) {}

  ngOnInit() {
    // Set initial sidebar visibility based on current URL
    const currentUrl = this.router.url;
    console.log('Initial URL:', currentUrl);

    // Default to showing sidebar for dashboard and other authenticated pages
    if (currentUrl && !this.noSidebarRoutes.includes(currentUrl)) {
      this.showSidebar = true;
    } else {
      this.updateSidebarVisibility(currentUrl);
    }

    // Subscribe to NavigationEnd events (more reliable than NavigationStart)
    this.router.events.pipe(
      filter((event: Event): event is NavigationEnd => event instanceof NavigationEnd)
    ).subscribe((event: NavigationEnd) => {
      this.ngZone.run(() => {
        console.log('Navigation completed to:', event.url);
        this.updateSidebarVisibility(event.url);

      });
    });
  }

  private updateSidebarVisibility(url: string): void {
    if (!url) {
      // If URL is undefined or empty, default to showing sidebar
      this.showSidebar = true;
      return;
    }

    // Extract the base URL without query parameters
    const baseUrl = url.split('?')[0];

    // Check if current URL base path matches any route in the noSidebarRoutes list
    const shouldHideSidebar = this.noSidebarRoutes.some(route => {
      // Base path match
      if (baseUrl === route) {
        return true;
      }

      // Special case for root path
      if (route === '/' && (baseUrl === '' || baseUrl === '/')) {
        return true;
      }

      return false;
    });

    console.log(`URL: ${url}, Base URL: ${baseUrl}, Sidebar visible: ${!shouldHideSidebar}`);
    this.showSidebar = !shouldHideSidebar;
  }
}
