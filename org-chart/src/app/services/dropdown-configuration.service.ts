import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
 DropdownConfiguration,
 CreateDropdownConfiguration,
 UpdateDropdownConfiguration,
 BaseResponse
} from '../model/dropdown-configuration.interface';


@Injectable({
 providedIn: 'root'
})
export class DropdownConfigurationService {
 private baseUrl = `${environment.apiUrl}/api/dropdown-configurations`;


 constructor(private http: HttpClient) {}


 /**
  * Create a new dropdown configuration option
  */
 createDropdownOption(createData: CreateDropdownConfiguration): Observable<DropdownConfiguration> {
   return this.http.post<BaseResponse<DropdownConfiguration>>(this.baseUrl, createData).pipe(
     map(response => response.data)
   );
 }


 /**
  * Get all active dropdown options for a specific type
  */
 getActiveDropdownOptions(dropdownType: string): Observable<DropdownConfiguration[]> {
   return this.http.get<BaseResponse<DropdownConfiguration[]>>(`${this.baseUrl}/active/${dropdownType}`).pipe(
     map(response => response.data)
   );
 }


 /**
  * Get all dropdown options for a specific type (including inactive)
  */
 getAllDropdownOptions(dropdownType: string): Observable<DropdownConfiguration[]> {
   return this.http.get<BaseResponse<DropdownConfiguration[]>>(`${this.baseUrl}/all/${dropdownType}`).pipe(
     map(response => response.data)
   );
 }


 /**
  * Update an existing dropdown configuration option
  */
 updateDropdownOption(id: number, updateData: UpdateDropdownConfiguration): Observable<DropdownConfiguration> {
   return this.http.put<BaseResponse<DropdownConfiguration>>(`${this.baseUrl}/${id}`, updateData).pipe(
     map(response => response.data)
   );
 }


 /**
  * Delete a dropdown configuration option
  */
 deleteDropdownOption(id: number): Observable<void> {
   return this.http.delete<BaseResponse<void>>(`${this.baseUrl}/${id}`).pipe(
     map(() => void 0)
   );
 }


 /**
  * Get a specific dropdown option by ID
  */
 getDropdownOptionById(id: number): Observable<DropdownConfiguration> {
   return this.http.get<BaseResponse<DropdownConfiguration>>(`${this.baseUrl}/${id}`).pipe(
     map(response => response.data)
   );
 }


 /**
  * Get all dropdown types
  */
 getAllDropdownTypes(): Observable<string[]> {
   return this.http.get<BaseResponse<string[]>>(`${this.baseUrl}/types`).pipe(
     map(response => response.data)
   );
 }


 /**
  * Reorder dropdown options for a specific type
  */
 reorderDropdownOptions(dropdownType: string, orderedIds: number[]): Observable<DropdownConfiguration[]> {
   return this.http.put<BaseResponse<DropdownConfiguration[]>>(`${this.baseUrl}/reorder/${dropdownType}`, orderedIds).pipe(
     map(response => response.data)
   );
 }


 /**
  * Check if user has admin/ops manager role
  */
 canManageDropdowns(): boolean {
   const role = localStorage.getItem('role');
   return role === 'ADMIN_OPS_MANAGER';
 }
}



