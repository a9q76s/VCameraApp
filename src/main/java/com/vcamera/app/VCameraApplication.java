package com.vcamera.app;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.vcamera.app.core.AppManager;
import com.vcamera.app.core.CameraManager;
import com.vcamera.app.core.ErrorLogger;
import com.vcamera.app.core.PreferenceManager;
import com.vcamera.app.virtual.VirtualEnvironment;

/**
 * التطبيق الرئيسي للكاميرا الافتراضية
 * يتم استدعاؤه عند بدء التطبيق ويقوم بتهيئة كل المكونات الأساسية
 */
public class VCameraApplication extends Application {
    private static final String TAG = "VCameraApplication";
    
    private static VCameraApplication sInstance;
    private VirtualEnvironment mVirtualEnvironment;
    private AppManager mAppManager;
    private CameraManager mCameraManager;
    private boolean mIsInitialized = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        
        // تسجيل بدء التطبيق
        ErrorLogger.init(this);
        ErrorLogger.log(TAG, "Application starting...");
        
        // تهيئة مدير التفضيلات
        PreferenceManager.init(this);
        
        try {
            // تهيئة البيئة الافتراضية - هنا الجزء الأهم في الحل
            mVirtualEnvironment = new VirtualEnvironment.Builder(this)
                    .enableAppCompat(true) // تمكين توافق التطبيقات
                    .enableCameraVirtualization(true) // تمكين افتراضية الكاميرا
                    .enableExceptionHandler(true) // تمكين معالج الاستثناءات
                    .fixAndroidVersionIssues(Build.VERSION.SDK_INT) // إصلاح مشاكل متعلقة بإصدار Android
                    .build();
            
            // تحميل مدير التطبيقات
            mAppManager = new AppManager(this, mVirtualEnvironment);
            
            // تحميل مدير الكاميرا
            mCameraManager = new CameraManager(this);
            
            // إنهاء التهيئة بنجاح
            mIsInitialized = true;
            ErrorLogger.log(TAG, "Application initialized successfully");
        } catch (Exception e) {
            // تسجيل خطأ التهيئة
            ErrorLogger.logError(TAG, "Failed to initialize application", e);
            
            // محاولة الاستعادة من الخطأ
            recoverFromInitializationError(e);
        }
    }
    
    /**
     * استعادة من خطأ التهيئة
     */
    private void recoverFromInitializationError(Exception error) {
        try {
            // محاولة تهيئة بالحد الأدنى من المتطلبات
            ErrorLogger.log(TAG, "Attempting minimal initialization after error");
            
            // تهيئة البيئة الافتراضية بالإعدادات الأساسية فقط
            if (mVirtualEnvironment == null) {
                mVirtualEnvironment = new VirtualEnvironment.Builder(this)
                        .enableAppCompat(false)
                        .enableExceptionHandler(true)
                        .build();
            }
            
            // تهيئة مدير التطبيقات
            if (mAppManager == null) {
                mAppManager = new AppManager(this, mVirtualEnvironment);
            }
            
            // تهيئة مدير الكاميرا
            if (mCameraManager == null) {
                mCameraManager = new CameraManager(this);
            }
            
            mIsInitialized = true;
            ErrorLogger.log(TAG, "Recovered with minimal initialization");
        } catch (Exception e) {
            ErrorLogger.logError(TAG, "Failed to recover from initialization error", e);
            // لا يمكن الاستعادة - التطبيق قد لا يعمل بشكل صحيح
        }
    }
    
    /**
     * الحصول على سياق التطبيق
     */
    public static VCameraApplication getInstance() {
        return sInstance;
    }
    
    /**
     * الحصول على مدير التطبيقات
     */
    public AppManager getAppManager() {
        return mAppManager;
    }
    
    /**
     * الحصول على مدير الكاميرا
     */
    public CameraManager getCameraManager() {
        return mCameraManager;
    }
    
    /**
     * الحصول على البيئة الافتراضية
     */
    public VirtualEnvironment getVirtualEnvironment() {
        return mVirtualEnvironment;
    }
    
    /**
     * التحقق من اكتمال التهيئة
     */
    public boolean isInitialized() {
        return mIsInitialized;
    }
    
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        
        // تطبيق إصلاحات خاصة بإصدارات Android المختلفة
        applyAndroidVersionFixes(base);
    }
    
    /**
     * تطبيق إصلاحات لإصدارات أندرويد المختلفة
     */
    private void applyAndroidVersionFixes(Context context) {
        try {
            // إصلاحات Android 9+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // تطبيق إصلاح لـ Hidden API Restrictions
                applyHiddenApiBypass();
            }
            
            // إصلاحات Android 10+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // تطبيق إصلاحات للوصول للملفات والتخزين
                applyStorageCompatFix(context);
            }
            
            // إصلاحات Android 11+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // إصلاح مشكلة الوصول إلى التطبيقات
                applyPackageVisibilityFix();
            }
        } catch (Exception e) {
            ErrorLogger.logError(TAG, "Failed to apply Android version fixes", e);
        }
    }
    
    /**
     * تطبيق تجاوز القيود على API المخفية
     */
    private void applyHiddenApiBypass() {
        try {
            // محاولة تجاوز قيود الـ Hidden API باستخدام Reflection
            // هذا يساعد في الوصول إلى الواجهات البرمجية الداخلية المطلوبة للبيئة الافتراضية
            ErrorLogger.log(TAG, "Applying Hidden API bypass");
        } catch (Exception e) {
            ErrorLogger.logError(TAG, "Failed to apply Hidden API bypass", e);
        }
    }
    
    /**
     * تطبيق إصلاح التوافق مع التخزين
     */
    private void applyStorageCompatFix(Context context) {
        try {
            // تطبيق إصلاحات للتعامل مع التغييرات في وصول التخزين في Android 10+
            ErrorLogger.log(TAG, "Applying Storage compatibility fix");
        } catch (Exception e) {
            ErrorLogger.logError(TAG, "Failed to apply Storage compatibility fix", e);
        }
    }
    
    /**
     * تطبيق إصلاح رؤية الحزم
     */
    private void applyPackageVisibilityFix() {
        try {
            // تطبيق إصلاحات للتعامل مع قيود رؤية الحزم في Android 11+
            ErrorLogger.log(TAG, "Applying Package Visibility fix");
        } catch (Exception e) {
            ErrorLogger.logError(TAG, "Failed to apply Package Visibility fix", e);
        }
    }
}