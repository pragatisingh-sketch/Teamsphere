import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

// Angular Material
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCheckboxModule } from '@angular/material/checkbox';

// Shared Module
import { SharedModule } from '../shared/shared.module';

// Components
import { ReleaseListComponent } from './release-list/release-list.component';
import { ReleaseFormDialogComponent } from './release-form-dialog/release-form-dialog.component';
import { SendNotificationDialogComponent } from './send-notification-dialog/send-notification-dialog.component';
import { ReleaseDetailsComponent } from './release-details/release-details.component';

// Guard
import { ReleaseGuard } from '../guards/release.guard';

const routes: Routes = [
    { path: '', component: ReleaseListComponent, canActivate: [ReleaseGuard] },
    { path: ':id', component: ReleaseDetailsComponent, canActivate: [ReleaseGuard] }
];

@NgModule({
    declarations: [
        ReleaseListComponent,
        ReleaseFormDialogComponent,
        SendNotificationDialogComponent,
        ReleaseDetailsComponent
    ],
    imports: [
        CommonModule,
        ReactiveFormsModule,
        FormsModule,
        RouterModule.forChild(routes),
        SharedModule,
        // Material Modules
        MatButtonModule,
        MatIconModule,
        MatDialogModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        MatDatepickerModule,
        MatNativeDateModule,
        MatTooltipModule,
        MatProgressSpinnerModule,
        MatCheckboxModule
    ]
})
export class ReleaseManagementModule { }
