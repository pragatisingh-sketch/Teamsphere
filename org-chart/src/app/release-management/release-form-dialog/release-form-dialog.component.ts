import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ReleaseService, Release, ReleaseItem, ReleaseStep } from '../../services/release.service';
import { NotificationService } from '../../shared/notification.service';

@Component({
    selector: 'app-release-form-dialog',
    templateUrl: './release-form-dialog.component.html',
    styleUrls: ['./release-form-dialog.component.css']
})
export class ReleaseFormDialogComponent implements OnInit {
    releaseForm: FormGroup;
    isEdit = false;
    isSubmitting = false;
    releaseTypes = [
        { value: 'BUG_FIX', label: 'Bug Fix', icon: '🐛' },
        { value: 'ENHANCEMENT', label: 'Enhancement', icon: '✨' },
        { value: 'FEATURE', label: 'New Feature', icon: '🚀' },
        { value: 'HOTFIX', label: 'Hotfix', icon: '🔥' }
    ];

    constructor(
        private fb: FormBuilder,
        private releaseService: ReleaseService,
        private notificationService: NotificationService,
        public dialogRef: MatDialogRef<ReleaseFormDialogComponent>,
        @Inject(MAT_DIALOG_DATA) public data: { isEdit: boolean; release?: Release }
    ) {
        this.isEdit = data.isEdit;
        this.releaseForm = this.fb.group({
            version: ['', [Validators.required, Validators.pattern(/^\d+\.\d+\.\d+$/)]],
            title: ['', Validators.required],
            releaseDate: [new Date(), Validators.required],
            releaseItems: this.fb.array([])
        });
    }

    ngOnInit(): void {
        if (this.isEdit && this.data.release) {
            this.patchForm(this.data.release);
        } else {
            // Add one empty item for new releases
            this.addItem();
        }
    }

    get releaseItems(): FormArray {
        return this.releaseForm.get('releaseItems') as FormArray;
    }

    getSteps(itemIndex: number): FormArray {
        return this.releaseItems.at(itemIndex).get('steps') as FormArray;
    }

    patchForm(release: Release): void {
        this.releaseForm.patchValue({
            version: release.version,
            title: release.title,
            releaseDate: new Date(release.releaseDate)
        });

        // Clear and rebuild items
        this.releaseItems.clear();
        if (release.releaseItems) {
            for (const item of release.releaseItems) {
                const itemGroup = this.createItemGroup(item);
                this.releaseItems.push(itemGroup);
            }
        }
    }

    createItemGroup(item?: ReleaseItem): FormGroup {
        const group = this.fb.group({
            id: [item?.id || null],
            type: [item?.type || 'BUG_FIX', Validators.required],
            title: [item?.title || '', Validators.required],
            description: [item?.description || ''],
            steps: this.fb.array([])
        });

        // Add steps
        if (item?.steps && item.steps.length > 0) {
            const stepsArray = group.get('steps') as FormArray;
            for (const step of item.steps) {
                stepsArray.push(this.createStepGroup(step));
            }
        }

        return group;
    }

    createStepGroup(step?: ReleaseStep): FormGroup {
        return this.fb.group({
            id: [step?.id || null],
            stepOrder: [step?.stepOrder || 1],
            explanation: [step?.explanation || '', Validators.required],
            screenshotUrl: [step?.screenshotUrl || '']
        });
    }

    addItem(): void {
        this.releaseItems.push(this.createItemGroup());
    }

    removeItem(index: number): void {
        this.releaseItems.removeAt(index);
    }

    addStep(itemIndex: number): void {
        const steps = this.getSteps(itemIndex);
        const newStep = this.createStepGroup({ stepOrder: steps.length + 1, explanation: '' });
        steps.push(newStep);
    }

    removeStep(itemIndex: number, stepIndex: number): void {
        const steps = this.getSteps(itemIndex);
        steps.removeAt(stepIndex);
        // Reorder remaining steps
        for (let i = 0; i < steps.length; i++) {
            steps.at(i).patchValue({ stepOrder: i + 1 });
        }
    }

    formatDate(date: Date): string {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    onSubmit(): void {
        if (this.releaseForm.invalid) {
            this.notificationService.showWarning('Please fill all required fields');
            return;
        }

        this.isSubmitting = true;
        const formValue = this.releaseForm.value;

        const release: Release = {
            ...formValue,
            releaseDate: this.formatDate(new Date(formValue.releaseDate))
        };

        const request = this.isEdit && this.data.release?.id
            ? this.releaseService.updateRelease(this.data.release.id, release)
            : this.releaseService.createRelease(release);

        request.subscribe({
            next: (response) => {
                this.notificationService.showSuccess(
                    this.isEdit ? 'Release updated successfully' : 'Release created successfully'
                );
                this.dialogRef.close(true);
            },
            error: (err) => {
                this.notificationService.showError(err.error?.message || 'Failed to save release');
                this.isSubmitting = false;
            }
        });
    }

    onCancel(): void {
        this.dialogRef.close(false);
    }
}
