import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditLeaveRequestComponent } from './edit-leave-request.component';

describe('EditLeaveRequestComponent', () => {
  let component: EditLeaveRequestComponent;
  let fixture: ComponentFixture<EditLeaveRequestComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [EditLeaveRequestComponent]
    });
    fixture = TestBed.createComponent(EditLeaveRequestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
