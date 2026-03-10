import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReportIssueDialogComponent } from './report-issue-dialog.component';

describe('ReportIssueDialogComponent', () => {
  let component: ReportIssueDialogComponent;
  let fixture: ComponentFixture<ReportIssueDialogComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ReportIssueDialogComponent]
    });
    fixture = TestBed.createComponent(ReportIssueDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
