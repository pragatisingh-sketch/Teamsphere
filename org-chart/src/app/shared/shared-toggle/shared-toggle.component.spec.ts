import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SharedToggleComponent } from './shared-toggle.component';

describe('SharedToggleComponent', () => {
  let component: SharedToggleComponent;
  let fixture: ComponentFixture<SharedToggleComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [SharedToggleComponent]
    });
    fixture = TestBed.createComponent(SharedToggleComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
