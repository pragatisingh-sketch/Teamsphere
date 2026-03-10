import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ExtDashboardComponent } from './ext-dashboard.component';

describe('ExtDashboardComponent', () => {
  let component: ExtDashboardComponent;
  let fixture: ComponentFixture<ExtDashboardComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ExtDashboardComponent]
    });
    fixture = TestBed.createComponent(ExtDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
