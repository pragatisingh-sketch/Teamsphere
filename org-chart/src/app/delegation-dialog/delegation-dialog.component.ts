import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialog } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { UserService } from '../user.service';
import { User } from '../model/user';
import { NotificationService } from '../shared/notification.service';
import { AuthService } from '../auth.service';
import { ConfirmationDialogComponent } from '../confirm-dialog/confirmation-dialog.component';

@Component({
    selector: 'app-delegation-dialog',
    templateUrl: './delegation-dialog.component.html',
    styleUrls: ['./delegation-dialog.component.css']
})
export class DelegationDialogComponent implements OnInit {
    delegationForm: FormGroup;
    users: User[] = [];
    loading = false;

    constructor(
        public dialogRef: MatDialogRef<DelegationDialogComponent>,
        @Inject(MAT_DIALOG_DATA) public data: { delegatorLdap: string, currentRole: string, delegateeLdap?: string },
        private fb: FormBuilder,
        private userService: UserService,
        private notificationService: NotificationService,
        private authService: AuthService,
        private dialog: MatDialog
    ) {
        this.delegationForm = this.fb.group({
            delegateeLdap: ['', Validators.required],
            startDate: [new Date(), Validators.required],
            endDate: ['', Validators.required]
        });
    }

    ngOnInit(): void {
        this.loadUsers();
        if (this.data.delegateeLdap) {
            this.delegationForm.patchValue({ delegateeLdap: this.data.delegateeLdap });
            this.delegationForm.get('delegateeLdap')?.disable();
        }
    }

    loadUsers() {
        this.loading = true;
        this.userService.getUsers().subscribe(
            (response) => {
                // Filter out the current user (delegator)
                this.users = response.data.filter(u => u.ldap !== this.data.delegatorLdap && !u.inactive);
                this.loading = false;
            },
            (error) => {
                console.error('Error loading users', error);
                this.notificationService.showError('Failed to load users');
                this.loading = false;
            }
        );
    }

    onSubmit() {
        if (this.delegationForm.valid) {
            const formValue = this.delegationForm.getRawValue();
            const delegateeName = this.users.find(u => u.ldap === formValue.delegateeLdap);

            // Show confirmation dialog
            const confirmDialogRef = this.dialog.open(ConfirmationDialogComponent, {
                width: '450px',
                data: {
                    title: 'Confirm Role Delegation',
                    message: `Are you sure you want to delegate your ${this.data.currentRole} role to ${delegateeName?.firstName} ${delegateeName?.lastName} (${formValue.delegateeLdap})?\n\nYou will be logged out immediately after delegation.`,
                    confirmButtonText: 'Delegate Role',
                    color: 'primary'
                }
            });

            confirmDialogRef.afterClosed().subscribe(confirmed => {
                if (confirmed) {
                    this.performDelegation(formValue);
                }
            });
        }
    }

    performDelegation(formValue: any) {
        const request = {
            delegatorLdap: this.data.delegatorLdap,
            delegateeLdap: formValue.delegateeLdap,
            startDate: formValue.startDate,
            endDate: formValue.endDate
        };

        this.loading = true;
        this.userService.delegateRole(request).subscribe(
            (response) => {
                this.notificationService.showSuccess(response.message);
                this.dialogRef.close(true);
                this.loading = false;
                // Logout the user as they have delegated their role
                this.authService.logout();
            },
            (error) => {
                console.error('Error delegating role', error);
                this.notificationService.showError(error.error?.message || 'Failed to delegate role');
                this.loading = false;
            }
        );
    }

    onCancel() {
        this.dialogRef.close();
    }
}
