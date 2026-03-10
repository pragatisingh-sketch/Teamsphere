import { Injectable } from '@angular/core';

@Injectable({
    providedIn: 'root'
})
export class DeviceDetectionService {

    /**
     * Detects the device type based on user agent and screen size
     * @returns "Mobile", "Tablet", "Desktop", or "Unknown"
     */
    getDeviceType(): string {
        const userAgent = navigator.userAgent.toLowerCase();
        const screenWidth = window.screen.width;

        // Check for mobile devices
        if (/android|webos|iphone|ipod|blackberry|iemobile|opera mini/i.test(userAgent)) {
            return 'Mobile';
        }

        // Check for tablets
        if (/ipad|android(?!.*mobile)|tablet/i.test(userAgent)) {
            return 'Tablet';
        }

        // Check by screen size as fallback
        if (screenWidth < 768) {
            return 'Mobile';
        } else if (screenWidth >= 768 && screenWidth < 1024) {
            return 'Tablet';
        }

        // Default to desktop
        return 'Desktop';
    }

    /**
     * Gets detailed device information for logging
     */
    getDeviceInfo(): {
        type: string;
        userAgent: string;
        screenWidth: number;
        screenHeight: number;
    } {
        return {
            type: this.getDeviceType(),
            userAgent: navigator.userAgent,
            screenWidth: window.screen.width,
            screenHeight: window.screen.height
        };
    }
}
