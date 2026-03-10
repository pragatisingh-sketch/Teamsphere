import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ViewRequestDialogComponent } from './view-request-dialog.component';

describe('ViewRequestDialogComponent', () => {
  let component: ViewRequestDialogComponent;
  let fixture: ComponentFixture<ViewRequestDialogComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ViewRequestDialogComponent]
    });
    fixture = TestBed.createComponent(ViewRequestDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
