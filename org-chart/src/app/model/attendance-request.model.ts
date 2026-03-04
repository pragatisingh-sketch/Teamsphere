export interface AttendanceRequest {
    ldap: string;
    reason?: string;
    comment?: string;
    latitude: number;
    longitude: number;
    entryTimestamp: string;
    deviceType?: string;  // "Mobile", "Tablet", "Desktop", or "Unknown"
    accuracy?: number;    // GPS accuracy in meters
}
