import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class LocationCacheService {

  private CACHE_KEY = "cached_location";
  private MAX_AGE = 3 * 60 * 1000; // 3 minutes (reduced from 10 minutes)

  async getLocation(forceRefresh: boolean = false): Promise<{ latitude: number; longitude: number; accuracy?: number }> {

    const cached = localStorage.getItem(this.CACHE_KEY);

    if (!forceRefresh && cached) {
      const parsed = JSON.parse(cached);
      const age = Date.now() - parsed.timestamp;

      // If cache is fresh → return cached
      if (age < this.MAX_AGE) {
        console.log(`📍 Using cached location (age: ${Math.round(age / 1000)}s, accuracy: ${parsed.accuracy || 'unknown'}m)`);
        return {
          latitude: parsed.latitude,
          longitude: parsed.longitude,
          accuracy: parsed.accuracy
        };
      } else {
        console.log(`⏰ Cache expired (age: ${Math.round(age / 1000)}s > ${this.MAX_AGE / 1000}s)`);
      }
    }

    // Else fetch new location
    console.log('🔄 Fetching fresh location...');
    const fresh = await this.fetchCurrentLocation();

    // Store in localstorage
    localStorage.setItem(this.CACHE_KEY, JSON.stringify({
      latitude: fresh.latitude,
      longitude: fresh.longitude,
      accuracy: fresh.accuracy,
      timestamp: Date.now()
    }));

    console.log(`✅ Fresh location cached (accuracy: ${fresh.accuracy}m)`);

    return fresh;
  }

  private fetchCurrentLocation(): Promise<any> {
    return new Promise((resolve, reject) => {

      if (!navigator.geolocation) {
        console.error('❌ Geolocation not supported by browser');
        return reject("Geolocation not supported");
      }

      const options: PositionOptions = {
        enableHighAccuracy: true,    // Use GPS for better accuracy (5-10m vs 50-100m)
        timeout: 10000,               // 10 second timeout - fail fast instead of hanging
        maximumAge: 0                 // Never accept browser's cached position
      };

      console.log('📡 Requesting geolocation with options:', options);

      navigator.geolocation.getCurrentPosition(
        position => {
          console.log('✅ Location obtained:', {
            lat: position.coords.latitude,
            lng: position.coords.longitude,
            accuracy: Math.round(position.coords.accuracy) + 'm'
          });

          resolve({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
            accuracy: position.coords.accuracy
          });
        },
        error => {
          console.error('❌ Geolocation error:', {
            code: error.code,
            message: error.message,
            details: this.getErrorMessage(error.code)
          });
          reject(error);
        },
        options
      );
    });
  }

  private getErrorMessage(code: number): string {
    switch (code) {
      case 1: return 'Permission denied - Please allow location access';
      case 2: return 'Position unavailable - GPS signal not available';
      case 3: return 'Timeout - Location request took too long';
      default: return 'Unknown error';
    }
  }
}
