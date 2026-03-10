import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DelegationHistoryDialogComponent } from './delegation-history-dialog.component';

describe('DelegationHistoryDialogComponent', () => {
  let component: DelegationHistoryDialogComponent;
  let fixture: ComponentFixture<DelegationHistoryDialogComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [DelegationHistoryDialogComponent]
    });
    fixture = TestBed.createComponent(DelegationHistoryDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
