import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormControl } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { User } from '../../model/user.interface';
import { Project } from '../../model/project.interface';
import { Subject } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged, map } from 'rxjs/operators';

@Component({
  selector: 'app-filter-dialog',
  templateUrl: './filter-dialog.component.html',
  styleUrls: ['./filter-dialog.component.css']
})
export class FilterDialogComponent implements OnInit {
  filterForm: FormGroup;
  
  // Search controls
  userSearchControl = new FormControl('');
  projectSearchControl = new FormControl('');
  
  // Data
  allUsers: User[] = [];
  allProjects: Project[] = [];
  filteredUsers: User[] = [];
  filteredProjects: Project[] = [];
  selectedUser: User | null = null;
  selectedProject: Project | null = null;

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    public dialogRef: MatDialogRef<FilterDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    this.filterForm = this.fb.group({
      userId: [''],
      projectId: [''],
      startDate: [''],
      endDate: ['']
    });
  }

  ngOnInit(): void {
    this.allUsers = this.data.users;
    this.allProjects = this.data.projects;
    this.filteredUsers = this.allUsers;
    this.filteredProjects = this.allProjects;
    
    if (this.data.filters) {
      this.filterForm.patchValue(this.data.filters);
      this.selectedUser = this.allUsers.find(u => u.id === this.data.filters.userId) || null;
      this.selectedProject = this.allProjects.find(p => p.id === this.data.filters.projectId) || null;
    }

    this.setupUserSearch();
    this.setupProjectSearch();
  }

  private setupUserSearch(): void {
    this.userSearchControl.valueChanges
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(300),
        distinctUntilChanged(),
        map(value => typeof value === 'string' ? value.toLowerCase() : '')
      )
      .subscribe(value => {
        if (value) {
          this.filteredUsers = this.allUsers.filter(user =>
            user.ldap.toLowerCase().includes(value)
          );
        } else {
          this.filteredUsers = this.allUsers;
        }
      });
  }

  private setupProjectSearch(): void {
    this.projectSearchControl.valueChanges
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(300),
        distinctUntilChanged(),
        map(value => typeof value === 'string' ? value.toLowerCase() : '')
      )
      .subscribe(value => {
        if (value) {
          this.filteredProjects = this.allProjects.filter(project =>
            project.projectName.toLowerCase().includes(value) ||
            project.projectCode.toLowerCase().includes(value)
          );
        } else {
          this.filteredProjects = this.allProjects;
        }
      });
  }

  onUserSelected(event: any): void {
    if (event.option && event.option.value) {
      const user = event.option.value as User;
      if (user) {
        this.selectedUser = user;
        this.filterForm.patchValue({
          userId: user.id
        });
      }
    }
  }

  onProjectSelected(event: any): void {
    if (event.option && event.option.value) {
      const project = event.option.value as Project;
      if (project) {
        this.selectedProject = project;
        this.filterForm.patchValue({
          projectId: project.id
        });
      }
    }
  }

  getUserDisplayName(user: User | null): string {
    if (!user) return '';
    return `${user.ldap}`;
  }

  getProjectDisplayName(project: Project | null): string {
    if (!project) return '';
    return `${project.projectName} (${project.projectCode})`;
  }

  onApply(): void {
    this.dialogRef.close({
      filters: this.filterForm.value,
      selectedUser: this.selectedUser,
      selectedProject: this.selectedProject
    });
  }

  onReset(): void {
    this.filterForm.reset();
    this.userSearchControl.reset('');
    this.projectSearchControl.reset('');
    this.selectedUser = null;
    this.selectedProject = null;
    this.filteredUsers = this.allUsers;
    this.filteredProjects = this.allProjects;
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
} 