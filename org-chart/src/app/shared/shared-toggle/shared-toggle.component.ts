import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatSlideToggleChange } from '@angular/material/slide-toggle';

@Component({
  selector: 'app-shared-toggle',
  templateUrl: './shared-toggle.component.html',
  styleUrls: ['./shared-toggle.component.css']
})
export class SharedToggleComponent {
  // Whether toggle is on or off
  @Input() checked = true;

  // Labels for ON and OFF states
  @Input() label = 'Direct Members';

  // Emit when toggle changes
  @Output() toggled = new EventEmitter<boolean>();

  onToggle(event: MatSlideToggleChange) {
    this.toggled.emit(event.checked);
  }

}
