package com.vcamera.app.core;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * مدير الموقع
 * يوفر واجهة للتعامل مع موقع افتراضي للتطبيقات
 */
public class LocationManager {
    private static final String TAG = "LocationManager";
    private static final int PERMISSIONS_REQUEST_LOCATION = 1001;
    
    private final Context mContext;
    private final Handler mMainHandler;
    private final List<LocationChangeListener> mListeners;
    private final Map<String, AppLocation> mAppLocations;
    
    private boolean mMockLocationEnabled = false;
    private double mLastLatitude = 0;
    private double mLastLongitude = 0;
    private double mLastAltitude = 0;
    private float mLastAccuracy = 0;
    
    public LocationManager(Context context) {
        mContext = context;
        mMainHandler = new Handler(Looper.getMainLooper());
        mListeners = new ArrayList<>();
        mAppLocations = new HashMap<>();
        
        // تحميل الإعدادات المحفوظة
        loadSettings();
    }
    
    /**
     * تحميل إعدادات الموقع
     */
    private void loadSettings() {
        // في تطبيق حقيقي، سنقوم بتحميل الإعدادات من التخزين المحلي
        PreferenceManager prefManager = PreferenceManager.getInstance();
        
        mMockLocationEnabled = prefManager.getBoolean("mock_location_enabled", false);
        mLastLatitude = prefManager.getFloat("last_latitude", 0);
        mLastLongitude = prefManager.getFloat("last_longitude", 0);
        mLastAltitude = prefManager.getFloat("last_altitude", 0);
        mLastAccuracy = prefManager.getFloat("last_accuracy", 0);
        
        ErrorLogger.log(TAG, "Loaded location settings: mockEnabled=" + mMockLocationEnabled);
    }
    
    /**
     * حفظ إعدادات الموقع
     */
    private void saveSettings() {
        // في تطبيق حقيقي، سنقوم بحفظ الإعدادات في التخزين المحلي
        PreferenceManager prefManager = PreferenceManager.getInstance();
        
        prefManager.putBoolean("mock_location_enabled", mMockLocationEnabled);
        prefManager.putFloat("last_latitude", (float) mLastLatitude);
        prefManager.putFloat("last_longitude", (float) mLastLongitude);
        prefManager.putFloat("last_altitude", (float) mLastAltitude);
        prefManager.putFloat("last_accuracy", mLastAccuracy);
        
        ErrorLogger.log(TAG, "Saved location settings: mockEnabled=" + mMockLocationEnabled);
    }
    
    /**
     * تمكين أو تعطيل الموقع الوهمي
     */
    public void setMockLocationEnabled(boolean enabled) {
        if (mMockLocationEnabled != enabled) {
            ErrorLogger.log(TAG, "Setting mock location enabled: " + enabled);
            
            mMockLocationEnabled = enabled;
            saveSettings();
            
            // إشعار المستمعين
            notifyLocationEnabledChanged();
        }
    }
    
    /**
     * التحقق مما إذا كان الموقع الوهمي ممكنًا
     */
    public boolean isMockLocationEnabled() {
        return mMockLocationEnabled;
    }
    
    /**
     * تعيين موقع افتراضي للتطبيق
     */
    public void setAppLocation(String packageName, double latitude, double longitude, double altitude, float accuracy) {
        ErrorLogger.log(TAG, "Setting location for app: " + packageName);
        
        AppLocation appLocation = new AppLocation(packageName, latitude, longitude, altitude, accuracy);
        mAppLocations.put(packageName, appLocation);
        
        // حفظ في تطبيق حقيقي
        
        // تحديث آخر موقع تم تعيينه
        mLastLatitude = latitude;
        mLastLongitude = longitude;
        mLastAltitude = altitude;
        mLastAccuracy = accuracy;
        saveSettings();
    }
    
    /**
     * الحصول على موقع افتراضي للتطبيق
     */
    public AppLocation getAppLocation(String packageName) {
        // إذا كان التطبيق لديه موقع محدد، استخدمه
        if (mAppLocations.containsKey(packageName)) {
            return mAppLocations.get(packageName);
        }
        
        // وإلا، استخدم الموقع الافتراضي
        return new AppLocation(packageName, mLastLatitude, mLastLongitude, mLastAltitude, mLastAccuracy);
    }
    
    /**
     * التحقق من أذونات الموقع
     */
    public boolean checkLocationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
    
    /**
     * طلب أذونات الموقع
     */
    public void requestLocationPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_LOCATION);
        }
    }
    
    /**
     * معالجة نتيجة طلب الأذونات
     */
    public boolean handlePermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // تم منح الإذن
                return true;
            } else {
                // تم رفض الإذن
                return false;
            }
        }
        return false;
    }
    
    /**
     * إضافة مستمع لتغيير الموقع
     */
    public void addLocationChangeListener(LocationChangeListener listener) {
        if (listener != null && !mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }
    
    /**
     * إزالة مستمع لتغيير الموقع
     */
    public void removeLocationChangeListener(LocationChangeListener listener) {
        mListeners.remove(listener);
    }
    
    /**
     * إشعار المستمعين بتغيير تمكين الموقع
     */
    private void notifyLocationEnabledChanged() {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (LocationChangeListener listener : mListeners) {
                    listener.onMockLocationEnabledChanged(mMockLocationEnabled);
                }
            }
        });
    }
    
    /**
     * إشعار المستمعين بتغيير الموقع
     */
    private void notifyLocationChanged(final String packageName, final AppLocation location) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (LocationChangeListener listener : mListeners) {
                    listener.onAppLocationChanged(packageName, location);
                }
            }
        });
    }
    
    /**
     * واجهة للاستماع لأحداث تغيير الموقع
     */
    public interface LocationChangeListener {
        void onMockLocationEnabledChanged(boolean enabled);
        void onAppLocationChanged(String packageName, AppLocation location);
    }
    
    /**
     * فئة موقع التطبيق
     */
    public static class AppLocation {
        private final String packageName;
        private final double latitude;
        private final double longitude;
        private final double altitude;
        private final float accuracy;
        
        public AppLocation(String packageName, double latitude, double longitude, double altitude, float accuracy) {
            this.packageName = packageName;
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = altitude;
            this.accuracy = accuracy;
        }
        
        public String getPackageName() {
            return packageName;
        }
        
        public double getLatitude() {
            return latitude;
        }
        
        public double getLongitude() {
            return longitude;
        }
        
        public double getAltitude() {
            return altitude;
        }
        
        public float getAccuracy() {
            return accuracy;
        }
        
        /**
         * تحويل إلى كائن موقع أندرويد
         */
        public Location toAndroidLocation() {
            Location location = new Location(LocationProvider.NETWORK_PROVIDER);
            location.setLatitude(latitude);
            location.setLongitude(longitude);
            location.setAltitude(altitude);
            location.setAccuracy(accuracy);
            location.setTime(System.currentTimeMillis());
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                location.setElapsedRealtimeNanos(System.nanoTime());
            }
            
            return location;
        }
    }
}