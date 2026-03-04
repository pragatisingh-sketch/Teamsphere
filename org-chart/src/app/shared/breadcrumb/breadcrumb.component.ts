import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { Title } from '@angular/platform-browser';

interface Breadcrumb {
  label: string;
  url: string;
}

@Component({
  selector: 'app-breadcrumb',
  templateUrl: './breadcrumb.component.html',
  styleUrls: ['./breadcrumb.component.css']
})
export class BreadcrumbComponent implements OnInit {
  breadcrumbs: Breadcrumb[] = [];
  
  // Map route paths to display names
  routeLabels: { [key: string]: string } = {
    '': 'Home',
    'admin': 'Admin',
    'dashboard': 'Dashboard',
    'extdashboard': 'Dashboard',
    'time-entry': 'Time Entries',
    'time-summary': 'Time Summary',
    'approve': 'Requests',
    'vunno': 'Leave/WFH',
    'attendance': 'Attendance',
    'user-details': 'User Details',
    'add-user': 'Add User',
    'edit-user': 'Edit User',
    'projects': 'Projects',
    'requests': 'Requests',
    'project-assignment': 'Project Assignment'
  };

  constructor(
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private titleService: Title
  ) {}

  ngOnInit() {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      this.breadcrumbs = this.createBreadcrumbs(this.activatedRoute.root);
      
      // Set page title based on the last breadcrumb
      if (this.breadcrumbs.length > 0) {
        this.titleService.setTitle(`VACO - ${this.breadcrumbs[this.breadcrumbs.length - 1].label}`);
      }
    });
  }

  private createBreadcrumbs(route: ActivatedRoute, url: string = '', breadcrumbs: Breadcrumb[] = []): Breadcrumb[] {
    const children: ActivatedRoute[] = route.children;

    if (children.length === 0) {
      return breadcrumbs;
    }

    for (const child of children) {
      const routeURL: string = child.snapshot.url.map(segment => segment.path).join('/');
      
      if (routeURL !== '') {
        url += `/${routeURL}`;
        
        // Get the route segments
        const segments = routeURL.split('/');
        const lastSegment = segments[segments.length - 1];
        
        // Check if it's a dynamic segment (has a parameter)
        if (lastSegment.startsWith(':')) {
          // For dynamic segments, try to get a meaningful name
          const paramName = lastSegment.substring(1);
          const paramValue = child.snapshot.params[paramName];
          
          if (paramValue) {
            breadcrumbs.push({
              label: this.getParameterLabel(paramName, paramValue),
              url: url
            });
          }
        } else {
          // For static segments, use the route label mapping
          const label = this.getRouteLabel(lastSegment);
          breadcrumbs.push({
            label: label,
            url: url
          });
        }
      }

      return this.createBreadcrumbs(child, url, breadcrumbs);
    }

    return breadcrumbs;
  }

  private getRouteLabel(segment: string): string {
    // Return the mapped label or the segment itself if no mapping exists
    return this.routeLabels[segment] || this.formatLabel(segment);
  }

  private getParameterLabel(paramName: string, paramValue: string): string {
    // For parameters like IDs, we can try to make them more readable
    if (paramName === 'id') {
      return `ID: ${paramValue}`;
    }
    return paramValue;
  }

  private formatLabel(text: string): string {
    // Convert camelCase or kebab-case to Title Case with spaces
    return text
      .replace(/-/g, ' ')
      .replace(/([A-Z])/g, ' $1')
      .replace(/^\w/, c => c.toUpperCase());
  }
}
