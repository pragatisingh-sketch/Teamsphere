import { Component } from '@angular/core';

@Component({
  selector: 'app-maintenance',
  templateUrl: './maintenance.component.html',
  styleUrls: ['./maintenance.component.css']
})
export class MaintenanceComponent {
  
  constructor() { }

  refreshPage() {
    window.location.reload();
  }

  goToLogin() {
    window.location.href = '/login';
  }
}
