export interface CheckInStatusResponse {
    status: string;
    checkedIn: boolean;
    checkInTime: string | null;
    isLate: boolean;
    checkedOutStatus: string;
    checkedOut: boolean;
    checkOutTime: string | null;
    earlyOrLateCheckOut: string;
    checkinDateTime: string | null;
    checkOutDateTime: string | null;
    isCheckInOutsideOffice?: boolean;
    isCheckOutOutsideOffice?: boolean;
}