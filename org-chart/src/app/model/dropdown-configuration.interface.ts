export interface DropdownConfiguration {
 id: number;
 dropdownType: string;
 optionValue: string;
 displayName: string;
 isActive: boolean;
 sortOrder: number;
 createdBy: string;
 createdAt: string;
 updatedAt: string;
}


export interface CreateDropdownConfiguration {
 dropdownType: string;
 optionValue: string;
 displayName: string;
 sortOrder?: number;
 isActive?: boolean;
}


export interface UpdateDropdownConfiguration {
 optionValue: string;
 displayName: string;
 sortOrder?: number;
 isActive: boolean;
}


export interface BaseResponse<T> {
 status: string;
 code: number;
 message: string;
 data: T;
}





