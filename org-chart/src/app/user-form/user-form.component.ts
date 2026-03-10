import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { UserService } from '../user.service';
import { User } from '../model/user'
import { NgForm, FormControl } from '@angular/forms';
import { NotificationService } from '../shared/notification.service';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged, map } from 'rxjs/operators';
import { LANGUAGES, SHIFTS, LEVELS } from '../shared/constants';
import { MatDialog } from '@angular/material/dialog';
import { DropdownConfigurationService } from '../services/dropdown-configuration.service';
import { DropdownConfigurationModalComponent } from '../components/dropdown-configuration-modal/dropdown-configuration-modal.component';


@Component({
  selector: 'app-user-form',
  templateUrl: './user-form.component.html',
  styleUrls: ['./user-form.component.css']
})
export class UserFormComponent implements OnInit, OnDestroy {
profileImageUrl: any;
fileName: any;
uploadFileName: string | null = null;
uploadFileError: string | null = null;

levels = LEVELS;

psEPrograms: string[] = [];

teams: string[] = [];

  user: User = {
    firstName: '',
    lastName: '',
    startDate: '',
    team: '',
    newLevel: '',
    lead: '',
    programManager: '',
    vendor: '',
    email: '',
    status: '',
    profilePic: '',
    lwdMlStartDate: '',
    process: '',
    resignationDate: '',
    roleChangeEffectiveDate: '',
    levelBeforeChange: '',
    levelAfterChange: '',
    lastBillingDate: '',
    backfillLdap: '',
    billingStartDate: '',
    language: '',
    tenureTillDate: '',
    location: '',
  //  addedInGoVfsWhoMain: '',
   // addedInGoVfsWhoInactive: '',
    id: undefined,
    parent: undefined,
    ldap: '',
    level: '',
    inactiveReason:'',
    pnseProgram:'',
    shift:'',
    inactive: false
  };

  // Store original user data for comparison in edit mode
  originalUser: User | null = null;

  isEditMode = false;
  fileInput: any;

  processes: string[] = [];

  leads: any[] = [];
  managers: any[] = [];
  filteredLeads: any[] = [];
  filteredManagers: any[] = [];
  leadSearchText = '';
  managerSearchText = '';

  // Form controls for search
  leadSearchControl = new FormControl('');
  managerSearchControl = new FormControl('');
  teamSearchControl = new FormControl('');
  levelSearchControl = new FormControl('');
  newLevelSearchControl = new FormControl('');
  processSearchControl = new FormControl('');
  pseProgramSearchControl = new FormControl('');
  levelBeforeChangeSearchControl = new FormControl('');
  levelAfterChangeSearchControl = new FormControl('');
  private destroy$ = new Subject<void>();

  filteredTeams: string[] = [];
  filteredLevels: string[] = [];
  filteredNewLevels: string[] = [];
  filteredProcesses: string[] = [];
  filteredPsePrograms: string[] = [];
  filteredLevelsBeforeChange: string[] = [];
  filteredLevelsAfterChange: string[] = [];

  shifts = SHIFTS;
  languages = LANGUAGES;
  filteredLanguages: string[] = [];
  languageSearchControl = new FormControl('');

  // Location options
  locations: string[] = [];
  filteredLocations: string[] = [];
  locationSearchControl = new FormControl('');

  // Vendor options
  vendors: string[] = ['VBS'];

  dynamicProjects: string[] = [];



  constructor(private route: ActivatedRoute, private userService: UserService,
     private router: Router, private notificationService:NotificationService,
     private dialog: MatDialog, private dropdownConfigService: DropdownConfigurationService
   ) {}

  ngOnInit(): void {
    const userId = this.route.snapshot.paramMap.get('id');
    if (userId) {
      this.isEditMode = true;
      this.userService.getUserById(userId,false).subscribe(response => {
        this.user = { ...response.data };
        // Store original user data for comparison
        this.originalUser = { ...response.data };
        console.log("user to be edited",this.user);
        // Set initial values for search controls
        this.leadSearchControl.setValue(this.user.lead || '');
        this.managerSearchControl.setValue(this.user.programManager || '');
        this.teamSearchControl.setValue(this.user.team || '');
        this.levelSearchControl.setValue(this.user.level || '');
        this.newLevelSearchControl.setValue(this.user.newLevel || '');
        this.processSearchControl.setValue(this.user.process || '');
        this.pseProgramSearchControl.setValue(this.user.pnseProgram || '');
        this.levelBeforeChangeSearchControl.setValue(this.user.levelBeforeChange || '');
        this.levelAfterChangeSearchControl.setValue(this.user.levelAfterChange || '');
        this.languageSearchControl.setValue(this.user.language || '');
        this.locationSearchControl.setValue(this.user.location || '');
      });
    } else {
      // Set default vendor value for new users
      this.user.vendor = 'VBS';
    }

    // Load dynamic dropdown values from database first
    this.loadAllDropdownValues();

    // Setup search functionality
    this.setupLanguageSearch();
    this.setupLocationSearch();

    this.loadLeads();
    this.loadManagers();
    this.setupLeadSearch();
    this.setupManagerSearch();
    this.setupTeamSearch();
    this.setupLevelSearch();
    this.setupNewLevelSearch();
    this.setupProcessSearch();
    this.setupPseProgramSearch();
    this.setupLevelBeforeChangeSearch();
    this.setupLevelAfterChangeSearch();

    // Subscribe to form control value changes
    this.leadSearchControl.valueChanges.subscribe(value => {
      if (value !== null) {
        this.user.lead = value;
      }
    });

    this.managerSearchControl.valueChanges.subscribe(value => {
      if (value !== null) {
        this.user.programManager = value;
      }
    });

    this.teamSearchControl.valueChanges.subscribe(value => {
      if (value !== null) {
        this.user.team = value;
      }
    });

    this.levelSearchControl.valueChanges.subscribe(value => {
      if (value !== null) {
        this.user.level = value;
      }
    });

    this.newLevelSearchControl.valueChanges.subscribe(value => {
      if (value !== null) {
        this.user.newLevel = value;
      }
    });

    this.processSearchControl.valueChanges.subscribe(value => {
      if (value !== null) {
        this.user.process = value;
      }
    });

    this.pseProgramSearchControl.valueChanges.subscribe(value => {
      if (value !== null) {
        this.user.pnseProgram = value;
      }
    });

    this.levelBeforeChangeSearchControl.valueChanges.subscribe(value => {
      if (value !== null) {
        this.user.levelBeforeChange = value;
      }
    });

    this.levelAfterChangeSearchControl.valueChanges.subscribe(value => {
      if (value !== null) {
        this.user.levelAfterChange = value;
      }
    });

    // Add language control value changes subscription
    this.languageSearchControl.valueChanges.subscribe(value => {
      if (value !== null) {
        this.user.language = value;
      }
    });

    // Add location control value changes subscription
    this.locationSearchControl.valueChanges.subscribe(value => {
      if (value !== null) {
        this.user.location = value;
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private setupLeadSearch(): void {
    this.leadSearchControl.valueChanges
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(300),
        distinctUntilChanged(),
        map(value => typeof value === 'string' ? value.toLowerCase() : '')
      )
      .subscribe(value => {
        if (value) {
          this.filteredLeads = this.leads.filter(lead =>
            lead.ldap.toLowerCase().includes(value)
          );
        } else {
          this.filteredLeads = this.leads;
        }
      });
  }

  private setupManagerSearch(): void {
    this.managerSearchControl.valueChanges
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(300),
        distinctUntilChanged(),
        map(value => typeof value === 'string' ? value.toLowerCase() : '')
      )
      .subscribe(value => {
        if (value) {
          this.filteredManagers = this.managers.filter(manager =>
            manager.ldap.toLowerCase().includes(value)
          );
        } else {
          this.filteredManagers = this.managers;
        }
      });
  }

  private setupTeamSearch(): void {
    this.teamSearchControl.valueChanges
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(300),
        distinctUntilChanged(),
        map(value => typeof value === 'string' ? value.toLowerCase() : '')
      )
      .subscribe(value => {
        if (value) {
          this.filteredTeams = this.teams.filter(team =>
            team.toLowerCase().includes(value)
          );
        } else {
          this.filteredTeams = this.teams;
        }
      });
  }

  private setupLevelSearch(): void {
    this.levelSearchControl.valueChanges
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(300),
        distinctUntilChanged(),
        map(value => typeof value === 'string' ? value.toLowerCase() : '')
      )
      .subscribe(value => {
        if (value) {
          this.filteredLevels = this.levels.filter(level =>
            level.toLowerCase().includes(value)
          );
        } else {
          this.filteredLevels = this.levels;
        }
      });
  }

  private setupNewLevelSearch(): void {
    this.newLevelSearchControl.valueChanges
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(300),
        distinctUntilChanged(),
        map(value => typeof value === 'string' ? value.toLowerCase() : '')
      )
      .subscribe(value => {
        if (value) {
          this.filteredNewLevels = this.levels.filter(level =>
            level.toLowerCase().includes(value)
          );
        } else {
          this.filteredNewLevels = this.levels;
        }
      });
  }

  private setupProcessSearch(): void {
    this.processSearchControl.valueChanges
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(300),
        distinctUntilChanged(),
        map(value => typeof value === 'string' ? value.toLowerCase() : '')
      )
      .subscribe(value => {
        if (value) {
          this.filteredProcesses = this.processes.filter(process =>
            process.toLowerCase().includes(value)
          );
        } else {
          this.filteredProcesses = this.processes;
        }
      });
  }

  private setupPseProgramSearch(): void {
    this.pseProgramSearchControl.valueChanges
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(300),
        distinctUntilChanged(),
        map(value => typeof value === 'string' ? value.toLowerCase() : '')
      )
      .subscribe(value => {
        if (value) {
          this.filteredPsePrograms = this.psEPrograms.filter(program =>
            program.toLowerCase().includes(value)
          );
        } else {
          this.filteredPsePrograms = this.psEPrograms;
        }
      });
  }

  private setupLevelBeforeChangeSearch(): void {
    this.levelBeforeChangeSearchControl.valueChanges
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(300),
        distinctUntilChanged(),
        map(value => typeof value === 'string' ? value.toLowerCase() : '')
      )
      .subscribe(value => {
        if (value) {
          this.filteredLevelsBeforeChange = this.levels.filter(level =>
            level.toLowerCase().includes(value)
          );
        } else {
          this.filteredLevelsBeforeChange = this.levels;
        }
      });
  }

  private setupLevelAfterChangeSearch(): void {
    this.levelAfterChangeSearchControl.valueChanges
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(300),
        distinctUntilChanged(),
        map(value => typeof value === 'string' ? value.toLowerCase() : '')
      )
      .subscribe(value => {
        if (value) {
          this.filteredLevelsAfterChange = this.levels.filter(level =>
            level.toLowerCase().includes(value)
          );
        } else {
          this.filteredLevelsAfterChange = this.levels;
        }
      });
  }

  private setupLanguageSearch(): void {
    this.languageSearchControl.valueChanges
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(300),
        distinctUntilChanged(),
        map(value => typeof value === 'string' ? value.toLowerCase() : '')
      )
      .subscribe(value => {
        if (value) {
          this.filteredLanguages = this.languages.filter(language =>
            language.toLowerCase().includes(value)
          );
        } else {
          this.filteredLanguages = this.languages;
        }
      });
  }

  private setupLocationSearch(): void {
    this.locationSearchControl.valueChanges
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(300),
        distinctUntilChanged(),
        map(value => typeof value === 'string' ? value.toLowerCase() : '')
      )
      .subscribe(value => {
        if (value) {
          this.filteredLocations = this.locations.filter(location =>
            location.toLowerCase().includes(value)
          );
        } else {
          this.filteredLocations = this.locations;
        }
      });
  }

  getTeamDisplayName(team: string): string {
    return team || '';
  }

  getLevelDisplayName(level: string): string {
    return level || '';
  }

  getProcessDisplayName(process: string): string {
    return process || '';
  }

  getPseProgramDisplayName(program: string): string {
    return program || '';
  }

  getLeadDisplayName(ldap: string): string {
    return ldap || '';
  }

  getManagerDisplayName(ldap: string): string {
    return ldap || '';
  }

  getLanguageDisplayName(language: string): string {
    return language || '';
  }

  getLocationDisplayName(location: string): string {
    return location || '';
  }

  onPseProgramSelected(event: any): void {
    const selectedValue = event.option.value;
    if (selectedValue) {
      this.user.pnseProgram = selectedValue;
      this.pseProgramSearchControl.setValue(selectedValue);
    }
  }

  onLevelBeforeChangeSelected(event: any): void {
    const selectedValue = event.option.value;
    if (selectedValue) {
      this.user.levelBeforeChange = selectedValue;
      this.levelBeforeChangeSearchControl.setValue(selectedValue);
    }
  }

  onLevelAfterChangeSelected(event: any): void {
    const selectedValue = event.option.value;
    if (selectedValue) {
      this.user.levelAfterChange = selectedValue;
      this.levelAfterChangeSearchControl.setValue(selectedValue);
    }
  }

  onLeadSelected(event: any): void {
    const selectedValue = event.option.value;
    if (selectedValue) {
      this.user.lead = selectedValue;
      this.leadSearchControl.setValue(selectedValue);
    }
  }

  onManagerSelected(event: any): void {
    const selectedValue = event.option.value;
    if (selectedValue) {
      this.user.programManager = selectedValue;
      this.managerSearchControl.setValue(selectedValue);
    }
  }

  loadLeads(): void {
    this.userService.getLeadsOnly().subscribe({
      next: (response) => {
        this.leads = response.data;
        this.filteredLeads = response.data;
      },
      error: (error) => {
        console.error('Error loading leads:', error);
        this.notificationService.showNotification({
          type: 'error',
          message: error.error?.message || 'Failed to load leads'
        });
      }
    });
  }

  loadManagers(): void {
    this.userService.getManagersOnly().subscribe({
      next: (response) => {
        this.managers = response.data;
        this.filteredManagers = response.data;
      },
      error: (error) => {
        console.error('Error loading managers:', error);
        this.notificationService.showNotification({
          type: 'error',
          message: error.error?.message || 'Failed to load managers'
        });
      }
    });
  }

  filterLeads(): void {
    if (!this.leadSearchText) {
      this.filteredLeads = this.leads;
      return;
    }
    this.filteredLeads = this.leads.filter(lead =>
      lead.ldap.toLowerCase().includes(this.leadSearchText.toLowerCase())
    );
  }

  filterManagers(): void {
    if (!this.managerSearchText) {
      this.filteredManagers = this.managers;
      return;
    }
    this.filteredManagers = this.managers.filter(manager =>
      manager.ldap.toLowerCase().includes(this.managerSearchText.toLowerCase())
    );
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;

    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      this.fileName = file.name;
      const reader = new FileReader();
      reader.onload = () => {
        this.profileImageUrl = reader.result;
      };
      reader.readAsDataURL(file);

      console.log('Selected file:', file);
    }
  }

  async onSubmit(form: NgForm) {
    console.log('On submit', form);
    console.log('Form Submitted!');
    console.log('Form Values:', form.value);
    console.log('Form Validity:', form.valid);

    if (!form.valid) {
        this.showError();
        return;
    }

    // Validate dropdown values
    const validationErrors = this.validateDropdownValues();
    if (validationErrors.length > 0) {
        this.notificationService.showNotification({
            type: 'error',
            message: validationErrors[0]
        });
        return;
    }

    // Check if any data has changed in edit mode
    if (this.isEditMode && !this.hasUserDataChanged()) {
        this.notificationService.showNotification({
            type: 'info',
            message: 'No changes detected. Update request not sent.'
        });
        this.router.navigate(['/admin/dashboard']);
        return;
    }    // Create a copy of user data with properly formatted dates

    const userDataToSend = { ...this.user };



    // Format all date fields to YYYY-MM-DD format to avoid timezone issues

    userDataToSend.startDate = this.formatDateForBackend(this.user.startDate);

    userDataToSend.lwdMlStartDate = this.formatDateForBackend(this.user.lwdMlStartDate);

    userDataToSend.resignationDate = this.formatDateForBackend(this.user.resignationDate);

    userDataToSend.roleChangeEffectiveDate = this.formatDateForBackend(this.user.roleChangeEffectiveDate);

    userDataToSend.lastBillingDate = this.formatDateForBackend(this.user.lastBillingDate);

    userDataToSend.billingStartDate = this.formatDateForBackend(this.user.billingStartDate);

    userDataToSend.tenureTillDate = this.formatDateForBackend(this.user.tenureTillDate);



    const formData = new FormData();

    console.log('User data to be sent:', userDataToSend);

    formData.append('employee', new Blob([JSON.stringify(userDataToSend)], { type: 'application/json' }));

    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    if (fileInput && fileInput.files && fileInput.files[0]) {
        const file = fileInput.files[0];

        // Check file size before appending it
        if (file.size > 1048576) {
            this.notificationService.showNotification({
                type: 'warning',
                message: 'Profile picture size should be less than 1 MB',
            });
            return;
        }

        formData.append('profilePic', file);
    } else {
        console.warn('No file selected or file input not found.');
    }

    try {
        if (this.isEditMode) {
            const updateObservable = await this.userService.updateUser(formData);
            updateObservable.subscribe({
                next: (response) => {
                    this.notificationService.showNotification({
                        type: 'success',
                        message: response.message
                    });
                    this.isEditMode = false;
                    this.router.navigate(['/admin/dashboard']);
                },
                error: (error: HttpErrorResponse) => {
                    if (error.status === 409) {
                        this.notificationService.showNotification({
                            type: 'error',
                            message: error.error?.message || 'A request for this user is already pending approval.'
                        });
                    } else {
                        this.notificationService.showNotification({
                            type: 'error',
                            message: error.error?.message || 'Failed to update user. Please try again.'
                        });
                    }
                }
            });
        } else {
            this.userService.addUser(formData).subscribe({
                next: (response) => {
                    this.notificationService.showSuccess(response.message);
                    this.router.navigate(['/admin/dashboard']);
                },
                error: (error: HttpErrorResponse) => {
                    if (error.status === 409) {
                        const errorMessage = error.error?.message || 'An employee with this LDAP already exists.';
                        this.notificationService.showError(errorMessage);
                    } else {
                        const errorMessage = error.error?.message || 'Failed to add user or user with this LDAP already exists. Please try again.';
                        this.notificationService.showError(errorMessage);
                    }
                }
            });
        }
    } catch (error) {
        console.error('Error processing request:', error);
        this.notificationService.showNotification({
            type: 'error',
            message: 'An unexpected error occurred. Please try again.'
        });
    }
}

  showSuccess() {
    const role = localStorage.getItem("role");
    const message = role === 'ADMIN_OPS_MANAGER' ?
        'User Added successfully!' :
        'User deletion request has been sent for approval'
    this.notificationService.showSuccess(message);
  }

  showError() {
    console.log("Error is shown")
    this.notificationService.showError('Please fill all the required fields!');
  }


  cancel() {
    this.router.navigate(['/admin/dashboard']);
  }

    /**
   * Formats a date value for backend consumption, avoiding timezone issues
   * @param dateValue - The date value to format (can be Date, string, or null/undefined)
   * @returns Formatted date string in YYYY-MM-DD format or empty string if invalid/null
   */
    formatDateForBackend(dateValue: any): string {
      if (!dateValue) {
        return '';
      }
  
      let date: Date;
  
      // If it's already a Date object
      if (dateValue instanceof Date) {
        date = dateValue;
      }
      // If it's a string, try to parse it
      else if (typeof dateValue === 'string') {
        // If it's already in YYYY-MM-DD format, return as is
        if (dateValue.match(/^\d{4}-\d{2}-\d{2}$/)) {
          return dateValue;
        }
        date = new Date(dateValue);
      }
      // If it's something else, try to convert to Date
      else {
        date = new Date(dateValue);
      }
  
      // Check if the date is valid
      if (isNaN(date.getTime())) {
        return '';
      }
  
      // Format as YYYY-MM-DD using local date components to avoid timezone issues
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
  
      return `${year}-${month}-${day}`;
    }

  onTeamSelected(event: any): void {
    const selectedValue = event.option.value;
    if (selectedValue) {
      this.user.team = selectedValue;
      this.teamSearchControl.setValue(selectedValue);
    }
  }

  onLevelSelected(event: any): void {
    const selectedValue = event.option.value;
    if (selectedValue) {
      this.user.level = selectedValue;
      this.levelSearchControl.setValue(selectedValue);
    }
  }

  onNewLevelSelected(event: any): void {
    const selectedValue = event.option.value;
    if (selectedValue) {
      this.user.newLevel = selectedValue;
      this.newLevelSearchControl.setValue(selectedValue);
    }
  }

  onProcessSelected(event: any): void {
    const selectedValue = event.option.value;
    if (selectedValue) {
      this.user.process = selectedValue;
      this.processSearchControl.setValue(selectedValue);
    }
  }

  onLanguageSelected(event: any): void {
    const selectedValue = event.option.value;
    if (selectedValue) {
      this.user.language = selectedValue;
      this.languageSearchControl.setValue(selectedValue);
    }
  }

  onLocationSelected(event: any): void {
    const selectedValue = event.option.value;
    if (selectedValue) {
      this.user.location = selectedValue;
      this.locationSearchControl.setValue(selectedValue);
    }
  }

  // Validation methods for autocomplete fields
  validateManagerInput(): void {
    const currentValue = this.managerSearchControl.value;
    if (currentValue && this.managers.length > 0) {
      const managerExists = this.managers.some(manager => manager.ldap === currentValue);
      if (!managerExists) {
        // Reset to previous valid value or empty
        this.managerSearchControl.setValue(this.user.programManager || '');
        this.notificationService.showNotification({
          type: 'warning',
          message: 'Please select a Program Manager from the dropdown list.'
        });
      }
    }
  }

  validateLeadInput(): void {
    const currentValue = this.leadSearchControl.value;
    if (currentValue && this.leads.length > 0) {
      const leadExists = this.leads.some(lead => lead.ldap === currentValue);
      if (!leadExists) {
        // Reset to previous valid value or empty
        this.leadSearchControl.setValue(this.user.lead || '');
        this.notificationService.showNotification({
          type: 'warning',
          message: 'Please select a Lead from the dropdown list.'
        });
      }
    }
  }

  validateTeamInput(): void {
    const currentValue = this.teamSearchControl.value;
    if (currentValue && !this.teams.includes(currentValue)) {
      // Reset to previous valid value or empty
      this.teamSearchControl.setValue(this.user.team || '');
      this.notificationService.showNotification({
        type: 'warning',
        message: 'Please select a Project from the dropdown list.'
      });
    }
  }

  validateLevelInput(): void {
    const currentValue = this.levelSearchControl.value;
    if (currentValue && !this.levels.includes(currentValue)) {
      // Reset to previous valid value or empty
      this.levelSearchControl.setValue(this.user.level || '');
      this.notificationService.showNotification({
        type: 'warning',
        message: 'Please select a Level from the dropdown list.'
      });
    }
  }

  validateNewLevelInput(): void {
    const currentValue = this.newLevelSearchControl.value;
    if (currentValue && !this.levels.includes(currentValue)) {
      // Reset to previous valid value or empty
      this.newLevelSearchControl.setValue(this.user.newLevel || '');
      this.notificationService.showNotification({
        type: 'warning',
        message: 'Please select a New Level from the dropdown list.'
      });
    }
  }

  validateProcessInput(): void {
    const currentValue = this.processSearchControl.value;
    if (currentValue && !this.processes.includes(currentValue)) {
      // Reset to previous valid value or empty
      this.processSearchControl.setValue(this.user.process || '');
      this.notificationService.showNotification({
        type: 'warning',
        message: 'Please select a Process from the dropdown list.'
      });
    }
  }

  validatePseProgramInput(): void {
    const currentValue = this.pseProgramSearchControl.value;
    if (currentValue && !this.psEPrograms.includes(currentValue)) {
      // Reset to previous valid value or empty
      this.pseProgramSearchControl.setValue(this.user.pnseProgram || '');
      this.notificationService.showNotification({
        type: 'warning',
        message: 'Please select a PS&E Program from the dropdown list.'
      });
    }
  }

  validateLevelBeforeChangeInput(): void {
    const currentValue = this.levelBeforeChangeSearchControl.value;
    if (currentValue && !this.levels.includes(currentValue)) {
      // Reset to previous valid value or empty
      this.levelBeforeChangeSearchControl.setValue(this.user.levelBeforeChange || '');
      this.notificationService.showNotification({
        type: 'warning',
        message: 'Please select a Level (Before Change) from the dropdown list.'
      });
    }
  }

  validateLevelAfterChangeInput(): void {
    const currentValue = this.levelAfterChangeSearchControl.value;
    if (currentValue && !this.levels.includes(currentValue)) {
      // Reset to previous valid value or empty
      this.levelAfterChangeSearchControl.setValue(this.user.levelAfterChange || '');
      this.notificationService.showNotification({
        type: 'warning',
        message: 'Please select a Level (After Change) from the dropdown list.'
      });
    }
  }

  validateLanguageInput(): void {
    const currentValue = this.languageSearchControl.value;
    if (currentValue && !this.languages.includes(currentValue)) {
      // Reset to previous valid value or empty
      this.languageSearchControl.setValue(this.user.language || '');
      this.notificationService.showNotification({
        type: 'warning',
        message: 'Please select a Language from the dropdown list.'
      });
    }
  }

  validateLocationInput(): void {
    const currentValue = this.locationSearchControl.value;
    if (currentValue && !this.locations.includes(currentValue)) {
      // Reset to previous valid value or empty
      this.locationSearchControl.setValue(this.user.location || '');
      this.notificationService.showNotification({
        type: 'warning',
        message: 'Please select a Location from the dropdown list.'
      });
    }
  }

  /**
   * Checks if the current user data has changed compared to the original data
   * @returns boolean indicating whether any changes were made
   */
  hasUserDataChanged(): boolean {
    if (!this.originalUser) {
      return true; // If no original data, assume changes were made (new user)
    }

    // Compare all properties of the user object
    for (const key in this.user) {
      if (Object.prototype.hasOwnProperty.call(this.user, key)) {
        // Skip profilePic comparison as it's handled separately
        if (key === 'profilePic') {
          continue;
        }

        // @ts-ignore: Property may not exist on type 'User'
        if (this.user[key] !== this.originalUser[key]) {
          return true;
        }
      }
    }

    // Check if a new profile picture was selected
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    if (fileInput && fileInput.files && fileInput.files.length > 0) {
      return true;
    }

    return false;
  }

  /**
   * Validates that all dropdown values match the available options
   * @returns Array of error messages, empty if all values are valid
   */
  validateDropdownValues(): string[] {
    const errors: string[] = [];

    // Validate team
    if (this.user.team  && !this.teams.includes(this.user.team) && this.originalUser && this.user.team !== this.originalUser.team) {
      errors.push(`Invalid Project value: "${this.user.team}". Please select from the available options.`);
    } 

    // Validate level
    if (this.user.level && !this.levels.includes(this.user.level ) && this.originalUser && this.user.level !== this.originalUser.level) {
      errors.push(`Invalid Level value: "${this.user.level}". Please select from the available options.`);
    }

    // Validate new level
    if (this.user.newLevel && !this.levels.includes(this.user.newLevel) && this.originalUser && this.user.newLevel !== this.originalUser.newLevel) {
      errors.push(`Invalid New Level value: "${this.user.newLevel}". Please select from the available options.`);
    }

    // Validate process
    if (this.user.process && !this.processes.includes(this.user.process) && this.originalUser && this.user.process !== this.originalUser.process) {
      errors.push(`Invalid Process value: "${this.user.process}". Please select from the available options.`);
    }

    // Validate PS&E Program
    if (this.user.pnseProgram && !this.psEPrograms.includes(this.user.pnseProgram) && this.originalUser && this.user.pnseProgram !== this.originalUser.pnseProgram) {
      errors.push(`Invalid PS&E Program value: "${this.user.pnseProgram}". Please select from the available options.`);
    }

    // Validate level before change
    if (this.user.levelBeforeChange && !this.levels.includes(this.user.levelBeforeChange) && this.originalUser && this.user.levelBeforeChange !== this.originalUser.levelBeforeChange) {
      errors.push(`Invalid Level (Before Change) value: "${this.user.levelBeforeChange}". Please select from the available options.`);
    }

    // Validate level after change
    if (this.user.levelAfterChange && !this.levels.includes(this.user.levelAfterChange) && this.originalUser && this.user.levelAfterChange !== this.originalUser.levelAfterChange) {
      errors.push(`Invalid Level (After Change) value: "${this.user.levelAfterChange}". Please select from the available options.`);
    }

    // Validate language
    if (this.user.language && !this.languages.includes(this.user.language) && this.originalUser && this.user.language !== this.originalUser.language) {
      errors.push(`Invalid Language value: "${this.user.language}". Please select from the available options.`);
    }

    // Validate location
    if (this.user.location && !this.locations.includes(this.user.location) && this.originalUser && this.user.location !== this.originalUser.location) {
      errors.push(`Invalid Location value: "${this.user.location}". Please select from the available options.`);
    }

    // Validate shift
    if (this.user.shift && !this.shifts.includes(this.user.shift)) {
      errors.push(`Invalid Shift value: "${this.user.shift}". Please select from the available options.`);
    }

    // Validate vendor
    if (this.user.vendor && !this.vendors.includes(this.user.vendor)) {
      errors.push(`Invalid Vendor value: "${this.user.vendor}". Please select from the available options.`);
    }

    // Validate lead and manager (these are more complex as they're objects with ldap properties)
    if (this.user.lead && this.leads.length > 0) {
      const leadExists = this.leads.some(lead => lead.ldap === this.user.lead);
      if (!leadExists) {
        errors.push(`Invalid Lead value: "${this.user.lead}". Please select from the available options.`);
      }
    }

    if (this.user.programManager && this.managers.length > 0) {
      const managerExists = this.managers.some(manager => manager.ldap === this.user.programManager);
      if (!managerExists && this.originalUser && this.user.programManager !== this.originalUser.programManager) {
        errors.push(`Invalid Program Manager value: "${this.user.programManager}". Please select from the available options.`);
      }
    }

    return errors;
  }

  // Load all dropdown values from database
  loadAllDropdownValues(): void {
    // Load all dropdown values
    this.loadDynamicProjects();
    this.loadPsePrograms();
    this.loadProcesses();
    this.loadLevels();
    this.loadLanguages();
    this.loadLocations();
    this.loadVendors();
  }

  // Load dynamic projects from backend
 loadDynamicProjects(): void {
   this.dropdownConfigService.getActiveDropdownOptions('PROJECT').subscribe({
     next: (projects) => {
       this.dynamicProjects = projects.map(p => p.optionValue);
       // Update the teams array to use dynamic projects
       this.teams = [...this.dynamicProjects];
       this.filteredTeams = [...this.teams];
     },
     error: (error) => {
       console.error('Error loading dynamic projects:', error);
       // Fallback to hardcoded teams if API fails
       this.teams = [
         'CG Ratings',
         'CG Data',
         'VDM',
         'UFR VF',
         'CQMT',
         'E&V',
         'AIO UX Writers',
         'CQ&P Hume',
         'UFR Hume',
         'GenAi GOVO',
         'GenAi VOVO',
         'VO&E'
       ];
       this.filteredTeams = [...this.teams];
     }
   });
 }


 // Load PS&E Programs from backend
 loadPsePrograms(): void {
   this.dropdownConfigService.getActiveDropdownOptions('PSE_PROGRAM').subscribe({
     next: (programs) => {
       this.psEPrograms = programs.map(p => p.optionValue);
       this.filteredPsePrograms = [...this.psEPrograms];
     },
     error: (error) => {
       console.error('Error loading PS&E Programs:', error);
       // Fallback to hardcoded programs if API fails
       this.psEPrograms = [
         'Content Growth',
         'Vertical Development & Maintenance',
         'User Feedback & Reporting',
         'Content Quality & Protections',
         'Experimentation & Velocity',
         'GenAi Feedback and Factuality',
         'Vendor Operations & Excellance'
       ];
       this.filteredPsePrograms = [...this.psEPrograms];
     }
   });
 }


 // Load Processes from backend
 loadProcesses(): void {
   this.dropdownConfigService.getActiveDropdownOptions('PROCESS').subscribe({
     next: (processes) => {
       this.processes = processes.map(p => p.optionValue);
       this.filteredProcesses = [...this.processes];
     },
     error: (error) => {
       console.error('Error loading Processes:', error);
       // Fallback to hardcoded processes if API fails
       this.processes = ['KG_VF', 'SEI', 'HUME', 'MAGI Feedback'];
       this.filteredProcesses = [...this.processes];
     }
   });
 }


 // Load Languages from backend
 loadLanguages(): void {
   this.dropdownConfigService.getActiveDropdownOptions('LANGUAGE').subscribe({
     next: (languages) => {
       this.languages = languages.map(p => p.optionValue);
       this.filteredLanguages = [...this.languages];
     },
     error: (error) => {
       console.error('Error loading Languages:', error);
       // Fallback to hardcoded languages if API fails
       this.languages = LANGUAGES;
       this.filteredLanguages = [...this.languages];
     }
   });
 }


 // Load Locations from backend
 loadLocations(): void {
   this.dropdownConfigService.getActiveDropdownOptions('LOCATION').subscribe({
     next: (locations) => {
       this.locations = locations.map(p => p.optionValue);
       this.filteredLocations = [...this.locations];
     },
     error: (error) => {
       console.error('Error loading Locations:', error);
       // Fallback to hardcoded locations if API fails
       this.locations = [
         'GOVO, Gurugram, India',
         'VOVO, Gurugram, India'
       ];
       this.filteredLocations = [...this.locations];
     }
   });
 }


 // Load Levels from backend
 loadLevels(): void {
   this.dropdownConfigService.getActiveDropdownOptions('LEVEL').subscribe({
     next: (levels) => {
       this.levels = levels.map(p => p.optionValue);
       this.filteredLevels = [...this.levels];
       this.filteredNewLevels = [...this.levels];
       this.filteredLevelsBeforeChange = [...this.levels];
       this.filteredLevelsAfterChange = [...this.levels];
     },
     error: (error) => {
       console.error('Error loading Levels:', error);
       // Fallback to hardcoded levels if API fails
       this.levels = LEVELS;
       this.filteredLevels = [...this.levels];
       this.filteredNewLevels = [...this.levels];
       this.filteredLevelsBeforeChange = [...this.levels];
       this.filteredLevelsAfterChange = [...this.levels];
     }
   });
 }


 // Load Vendors from backend
 loadVendors(): void {
   this.dropdownConfigService.getActiveDropdownOptions('VENDOR').subscribe({
     next: (vendors) => {
       this.vendors = vendors.map(p => p.optionValue);
     },
     error: (error) => {
       console.error('Error loading Vendors:', error);
       // Fallback to hardcoded vendors if API fails
       this.vendors = ['VBS'];
     }
   });
 }


 // Open dropdown configuration modal
 openDropdownConfigModal(dropdownType: string, title: string): void {
   if (!this.canManageDropdowns()) {
     this.notificationService.showNotification({
       type: 'error',
       message: 'Access denied. You need ADMIN_OPS_MANAGER role to manage dropdown configurations.'
     });
     return;
   }


   const dialogRef = this.dialog.open(DropdownConfigurationModalComponent, {
     width: '800px',
     maxWidth: '90vw',
     data: {
       dropdownType: dropdownType,
       title: title
     }
   });


   dialogRef.afterClosed().subscribe((hasChanges: boolean) => {
     // Reload the dropdown options only if changes were made
     if (hasChanges) {
       switch (dropdownType) {
         case 'PROJECT':
           this.loadDynamicProjects();
           break;
         case 'PSE_PROGRAM':
           this.loadPsePrograms();
           break;
         case 'PROCESS':
           this.loadProcesses();
           break;
         case 'LEVEL':
           this.loadLevels();
           break;
         case 'LANGUAGE':
           this.loadLanguages();
           break;
         case 'LOCATION':
           this.loadLocations();
           break;
         case 'VENDOR':
           this.loadVendors();
           break;
       }

       // Show notification that dropdown values have been updated
       this.notificationService.showNotification({
         type: 'success',
         message: `${title} options have been updated and are now available in the dropdown.`
       });
     }
   });
 }


 // Check if user can manage dropdowns
 canManageDropdowns(): boolean {
   return this.dropdownConfigService.canManageDropdowns();
 }

}





