import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators, FormControl } from '@angular/forms';
import { formatDate } from '@angular/common';

@Component({
  selector: 'app-edit-leave-request',
  templateUrl: './edit-leave-request.component.html',
  styleUrls: ['./edit-leave-request.component.css']

})
export class EditLeaveRequestComponent {
  selectedFile: File | null = null;
  editForm: FormGroup;

  constructor(
    private dialogRef: MatDialogRef<EditLeaveRequestComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private fb: FormBuilder
  ) {
    this.editForm = this.fb.group({
      startDate: [data.startDate],
      endDate: [data.endDate],
      applicationType: [data.applicationType],
      duration: [data.duration],
      leaveType: [data.leaveType],
      oooProof: [data.oooProof],
      backupInfo: [data.backupInfo],
      reason: [data.reason],
      documentPath: [data.documentPath],
    });
  }

  ngOnInit(): void {
    this.setConditionalValidators(this.editForm.value.applicationType);

    this.editForm.get('applicationType')?.valueChanges.subscribe(value => {
      this.setConditionalValidators(value);
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  setConditionalValidators(applicationType: string): void {
    const reasonCtrl = this.editForm.get('reason');
    const backupInfoCtrl = this.editForm.get('backupInfo');
    const oooCtrl = this.editForm.get('oooProof');

    if (applicationType === 'Work From Home') {
      reasonCtrl?.setValidators([Validators.required]);
      reasonCtrl?.updateValueAndValidity();

      // Optional: Autofill leave-only fields with "NA(WFH)"
      backupInfoCtrl?.setValue('NA(WFH)');
      oooCtrl?.setValue('NA(WFH)');

      backupInfoCtrl?.clearValidators();
      oooCtrl?.clearValidators();

    } else if (applicationType === 'Leave') {
      reasonCtrl?.clearValidators();
      reasonCtrl?.updateValueAndValidity();

      // Reset the WFH-filled values back to empty
      if (backupInfoCtrl?.value === 'NA(WFH)') backupInfoCtrl.setValue('');
      if (oooCtrl?.value === 'NA(WFH)') oooCtrl.setValue('');

      backupInfoCtrl?.setValidators([Validators.required]);
      oooCtrl?.setValidators([Validators.required]);
    }

    ['reason', 'backupInfo', 'oooProof'].forEach(field => {
      this.editForm.get(field)?.updateValueAndValidity();
    });
  }


  onSave(): void {
    if (this.editForm.valid) {
      this.dialogRef.close({
        id: this.data.id,
        ldap: this.data.ldap,
        approvingLead: this.data.approver,
        applicationType: this.editForm.value.applicationType,
        lvWfhDuration: this.editForm.value.duration,
        leaveType: this.editForm.value.leaveType,
        startDate: this.editForm.value.startDate,
        endDate: this.editForm.value.endDate,
        oooProof: this.editForm.value.oooProof || "NA",
        backupInfo: this.editForm.value.backupInfo || "NA",
        reason: this.editForm.value.reason || "NA",
        documentPath: this.selectedFile || "NA",
        status: this.data.status
      });
    }
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      this.editForm.patchValue({ documentPath: file.name });
    }
  }
}

