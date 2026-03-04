import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { UserFormComponent } from './user-form.component';
import { UserService } from '../user.service';
import { NotificationService } from '../shared/notification.service';
import { DropdownConfigurationService } from '../services/dropdown-configuration.service';
import { DropdownConfigurationModalComponent } from '../components/dropdown-configuration-modal/dropdown-configuration-modal.component';

describe('UserFormComponent', () => {
  let component: UserFormComponent;
  let fixture: ComponentFixture<UserFormComponent>;
  let httpMock: HttpTestingController;
  let mockUserService: jasmine.SpyObj<UserService>;
  let mockNotificationService: jasmine.SpyObj<NotificationService>;
  let mockDropdownConfigService: jasmine.SpyObj<DropdownConfigurationService>;
  let mockDialog: jasmine.SpyObj<MatDialog>;
  let mockRouter: jasmine.SpyObj<Router>;

  const mockDropdownOptions = [
    { id: 1, dropdownType: 'PROJECT', optionValue: 'Test Project', displayName: 'Test Project', isActive: true, sortOrder: 1, createdBy: 'admin', createdAt: '2024-01-01', updatedAt: '2024-01-01' },
    { id: 2, dropdownType: 'LEVEL', optionValue: 'Senior', displayName: 'Senior', isActive: true, sortOrder: 1, createdBy: 'admin', createdAt: '2024-01-01', updatedAt: '2024-01-01' }
  ];

  beforeEach(async () => {
    const userServiceSpy = jasmine.createSpyObj('UserService', ['getUserById', 'getLeadsOnly', 'getManagersOnly', 'addUser', 'updateUser']);
    const notificationServiceSpy = jasmine.createSpyObj('NotificationService', ['showNotification', 'showSuccess', 'showError']);
    const dropdownConfigServiceSpy = jasmine.createSpyObj('DropdownConfigurationService', ['getActiveDropdownOptions', 'canManageDropdowns']);
    const dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      declarations: [UserFormComponent],
      imports: [
        HttpClientTestingModule,
        MatDialogModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        MatAutocompleteModule,
        MatDatepickerModule,
        MatNativeDateModule,
        MatProgressSpinnerModule,
        FormsModule,
        ReactiveFormsModule,
        BrowserAnimationsModule
      ],
      providers: [
        { provide: UserService, useValue: userServiceSpy },
        { provide: NotificationService, useValue: notificationServiceSpy },
        { provide: DropdownConfigurationService, useValue: dropdownConfigServiceSpy },
        { provide: MatDialog, useValue: dialogSpy },
        { provide: Router, useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: () => null
              }
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UserFormComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    mockUserService = TestBed.inject(UserService) as jasmine.SpyObj<UserService>;
    mockNotificationService = TestBed.inject(NotificationService) as jasmine.SpyObj<NotificationService>;
    mockDropdownConfigService = TestBed.inject(DropdownConfigurationService) as jasmine.SpyObj<DropdownConfigurationService>;
    mockDialog = TestBed.inject(MatDialog) as jasmine.SpyObj<MatDialog>;
    mockRouter = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    // Setup default mock responses
    mockUserService.getLeadsOnly.and.returnValue(of({ data: [] }));
    mockUserService.getManagersOnly.and.returnValue(of({ data: [] }));
    mockDropdownConfigService.getActiveDropdownOptions.and.returnValue(of(mockDropdownOptions));
    mockDropdownConfigService.canManageDropdowns.and.returnValue(true);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Dropdown Configuration', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should load all dropdown values on initialization', () => {
      expect(mockDropdownConfigService.getActiveDropdownOptions).toHaveBeenCalledWith('PROJECT');
      expect(mockDropdownConfigService.getActiveDropdownOptions).toHaveBeenCalledWith('PSE_PROGRAM');
      expect(mockDropdownConfigService.getActiveDropdownOptions).toHaveBeenCalledWith('PROCESS');
      expect(mockDropdownConfigService.getActiveDropdownOptions).toHaveBeenCalledWith('LEVEL');
      expect(mockDropdownConfigService.getActiveDropdownOptions).toHaveBeenCalledWith('LANGUAGE');
      expect(mockDropdownConfigService.getActiveDropdownOptions).toHaveBeenCalledWith('LOCATION');
      expect(mockDropdownConfigService.getActiveDropdownOptions).toHaveBeenCalledWith('VENDOR');
    });



    it('should open dropdown configuration modal when admin clicks configure button', () => {
      const mockDialogRef = {
        afterClosed: () => of(true)
      };
      mockDialog.open.and.returnValue(mockDialogRef as any);

      component.openDropdownConfigModal('PROJECT', 'Project');

      expect(mockDialog.open).toHaveBeenCalledWith(DropdownConfigurationModalComponent, {
        width: '800px',
        maxWidth: '90vw',
        data: {
          dropdownType: 'PROJECT',
          title: 'Project'
        }
      });
    });

    it('should reload dropdown values after modal closes with changes', () => {
      const mockDialogRef = {
        afterClosed: () => of(true) // Simulate changes made
      };
      mockDialog.open.and.returnValue(mockDialogRef as any);
      spyOn(component, 'loadDynamicProjects');

      component.openDropdownConfigModal('PROJECT', 'Project');

      expect(component.loadDynamicProjects).toHaveBeenCalled();
      expect(mockNotificationService.showNotification).toHaveBeenCalledWith({
        type: 'success',
        message: 'Project options have been updated and are now available in the dropdown.'
      });
    });

    it('should not reload dropdown values if no changes were made', () => {
      const mockDialogRef = {
        afterClosed: () => of(false) // No changes made
      };
      mockDialog.open.and.returnValue(mockDialogRef as any);
      spyOn(component, 'loadDynamicProjects');

      component.openDropdownConfigModal('PROJECT', 'Project');

      expect(component.loadDynamicProjects).not.toHaveBeenCalled();
      expect(mockNotificationService.showNotification).not.toHaveBeenCalled();
    });

    it('should handle dropdown loading errors gracefully', () => {
      mockDropdownConfigService.getActiveDropdownOptions.and.returnValue(throwError('API Error'));

      component.loadDynamicProjects();

      // Should fallback to hardcoded values
      expect(component.teams.length).toBeGreaterThan(0);
    });

    it('should check if user can manage dropdowns', () => {
      mockDropdownConfigService.canManageDropdowns.and.returnValue(true);
      expect(component.canManageDropdowns()).toBe(true);

      mockDropdownConfigService.canManageDropdowns.and.returnValue(false);
      expect(component.canManageDropdowns()).toBe(false);
    });
  });

  describe('Dynamic Dropdown Updates', () => {
    it('should update teams array when projects are loaded', () => {
      const projectOptions = [
        { id: 1, dropdownType: 'PROJECT', optionValue: 'New Project', displayName: 'New Project', isActive: true, sortOrder: 1, createdBy: 'admin', createdAt: '2024-01-01', updatedAt: '2024-01-01' }
      ];
      mockDropdownConfigService.getActiveDropdownOptions.and.returnValue(of(projectOptions));

      component.loadDynamicProjects();

      expect(component.teams).toContain('New Project');
      expect(component.filteredTeams).toContain('New Project');
    });

    it('should update levels arrays when levels are loaded', () => {
      const levelOptions = [
        { id: 1, dropdownType: 'LEVEL', optionValue: 'Expert', displayName: 'Expert', isActive: true, sortOrder: 1, createdBy: 'admin', createdAt: '2024-01-01', updatedAt: '2024-01-01' }
      ];
      mockDropdownConfigService.getActiveDropdownOptions.and.returnValue(of(levelOptions));

      component.loadLevels();

      expect(component.levels).toContain('Expert');
      expect(component.filteredLevels).toContain('Expert');
      expect(component.filteredNewLevels).toContain('Expert');
      expect(component.filteredLevelsBeforeChange).toContain('Expert');
      expect(component.filteredLevelsAfterChange).toContain('Expert');
    });
  });
});
