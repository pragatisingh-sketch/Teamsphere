import { Component } from '@angular/core';

@Component({
  selector: 'app-request-form',
  templateUrl: './request-form.component.html',
  styleUrls: ['./request-form.component.css']
})
export class RequestFormComponent {
  request = {
    name: '',
    email: '',
    requestType: 'general',
    details: ''
  };

  onSubmit(form: any): void {
    if (form.valid) {
      console.log('Form Submitted:', this.request);
    }
  }
}
