package com.vcamera.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.vcamera.app.R;
import com.vcamera.app.core.AppManager;
import com.vcamera.app.core.CameraManager;
import com.vcamera.app.core.ErrorLogger;
import com.vcamera.app.core.LocationManager;
import com.vcamera.app.core.PreferenceManager;
import com.vcamera.app.virtual.VirtualAppEnvironment;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    
    // كود طلبات النشاط
    private static final int REQUEST_SELECT_APP = 1001;
    private static final int REQUEST_INSTALL_APP = 1002;
    private static final int REQUEST_VIDEO_CAPTURE = 1003;
    private static final int REQUEST_IMAGE_CAPTURE = 1004;
    private static final int REQUEST_VIDEO_PICK = 1005;
    private static final int REQUEST_IMAGE_PICK = 1006;
    
    // مديرو المكونات
    private VirtualAppEnvironment mVirtualAppEnvironment;
    private AppManager mAppManager;
    private CameraManager mCameraManager;
    private LocationManager mLocationManager;
    private PreferenceManager mPreferenceManager;
    private ErrorLogger mErrorLogger;
    
    // عناصر واجهة المستخدم
    private Button mLaunchAppButton;
    private Button mInstallAppButton;
    private Button mCameraSettingsButton;
    private Button mLocationSettingsButton;
    private Button mAdvancedSettingsButton;
    private ImageButton mRealCameraButton;
    private ImageButton mLocalVideoButton;
    private ImageButton mNetworkVideoButton;
    private ImageButton mLocalPictureButton;
    private ImageView mPreviewImageView;
    private TextView mStatusTextView;
    
    // المعالج الرئيسي
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // تهيئة المديرين
        initializeManagers();
        
        // تهيئة عناصر واجهة المستخدم
        initializeUI();
        
        // محاولة تهيئة البيئة الافتراضية
        initializeVirtualEnvironment();
    }
    
    /**
     * تهيئة مديري المكونات
     */
    private void initializeManagers() {
        mErrorLogger = new ErrorLogger(this);
        mPreferenceManager = PreferenceManager.getInstance(this);
        mCameraManager = CameraManager.getInstance(this);
        mLocationManager = LocationManager.getInstance(this);
        mAppManager = AppManager.getInstance(this);
        mVirtualAppEnvironment = VirtualAppEnvironment.getInstance(this);
    }
    
    /**
     * تهيئة عناصر واجهة المستخدم
     */
    private void initializeUI() {
        try {
            // أزرار الإدارة
            mLaunchAppButton = findViewById(R.id.btn_launch_app);
            mInstallAppButton = findViewById(R.id.btn_install_app);
            mCameraSettingsButton = findViewById(R.id.btn_camera_settings);
            mLocationSettingsButton = findViewById(R.id.btn_location_settings);
            mAdvancedSettingsButton = findViewById(R.id.btn_advanced_settings);
            
            // أزرار مصدر الكاميرا
            mRealCameraButton = findViewById(R.id.btn_real_camera);
            mLocalVideoButton = findViewById(R.id.btn_local_video);
            mNetworkVideoButton = findViewById(R.id.btn_network_video);
            mLocalPictureButton = findViewById(R.id.btn_local_picture);
            
            // عناصر أخرى
            mPreviewImageView = findViewById(R.id.img_preview);
            mStatusTextView = findViewById(R.id.txt_status);
            
            // ضبط أحداث النقر
            mLaunchAppButton.setOnClickListener(v -> openAppSelectDialog());
            mInstallAppButton.setOnClickListener(v -> selectAppToInstall());
            mCameraSettingsButton.setOnClickListener(v -> openCameraSettings());
            mLocationSettingsButton.setOnClickListener(v -> openLocationSettings());
            mAdvancedSettingsButton.setOnClickListener(v -> openAdvancedSettings());
            
            // أحداث مصدر الكاميرا
            mRealCameraButton.setOnClickListener(v -> switchToRealCamera());
            mLocalVideoButton.setOnClickListener(v -> selectLocalVideo());
            mNetworkVideoButton.setOnClickListener(v -> enterNetworkVideoUrl());
            mLocalPictureButton.setOnClickListener(v -> selectLocalPicture());
            
            // ضبط مصدر الكاميرا الحالي استنادًا إلى الإعدادات
            updateCameraSourceUI(mPreferenceManager.getInt("current_camera_source", CameraManager.SOURCE_REAL_CAMERA));
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء تهيئة واجهة المستخدم", e);
        }
    }
    
    /**
     * تهيئة البيئة الافتراضية
     */
    private void initializeVirtualEnvironment() {
        try {
            // تعيين رسالة الحالة
            updateStatus("جارٍ تهيئة البيئة الافتراضية...");
            
            // تشغيل المهمة في خلفية منفصلة
            new Thread(() -> {
                try {
                    // تهيئة البيئة الافتراضية
                    boolean initialized = mVirtualAppEnvironment.initialize();
                    
                    // تحديث واجهة المستخدم على الخيط الرئيسي
                    mMainHandler.post(() -> {
                        if (initialized) {
                            updateStatus("تم تهيئة البيئة الافتراضية بنجاح");
                            
                            // تحميل قائمة التطبيقات المثبتة
                            loadInstalledApps();
                            
                            // تهيئة مدير الكاميرا
                            mCameraManager.initialize();
                        } else {
                            updateStatus("فشل في تهيئة البيئة الافتراضية");
                            showErrorDialog("فشل في تهيئة البيئة الافتراضية", "تحقق من سجلات الأخطاء لمزيد من المعلومات.");
                        }
                    });
                } catch (Exception e) {
                    mMainHandler.post(() -> {
                        mErrorLogger.logException(TAG, "خطأ أثناء تهيئة البيئة الافتراضية", e);
                        updateStatus("خطأ: " + e.getMessage());
                        showErrorDialog("خطأ أثناء تهيئة البيئة الافتراضية", e.getMessage());
                    });
                }
            }).start();
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء تهيئة البيئة الافتراضية", e);
            updateStatus("خطأ: " + e.getMessage());
        }
    }
    
    /**
     * تحميل التطبيقات المثبتة
     */
    private void loadInstalledApps() {
        try {
            updateStatus("جارٍ تحميل التطبيقات المثبتة...");
            
            // الحصول على التطبيقات المثبتة
            Map<String, VirtualAppEnvironment.VirtualAppInfo> installedApps = mVirtualAppEnvironment.getInstalledApps();
            
            // تحديث القائمة في مدير التطبيقات
            mAppManager.setInstalledApps(installedApps);
            
            updateStatus("تم تحميل " + installedApps.size() + " تطبيقات");
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء تحميل التطبيقات المثبتة", e);
            updateStatus("خطأ: " + e.getMessage());
        }
    }
    
    /**
     * فتح مربع حوار تحديد التطبيق
     */
    private void openAppSelectDialog() {
        try {
            // الحصول على التطبيقات المثبتة
            Map<String, VirtualAppEnvironment.VirtualAppInfo> installedApps = mVirtualAppEnvironment.getInstalledApps();
            
            if (installedApps.isEmpty()) {
                Toast.makeText(this, "لا توجد تطبيقات مثبتة.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // إنشاء قائمة بأسماء التطبيقات
            String[] appNames = new String[installedApps.size()];
            String[] packageNames = new String[installedApps.size()];
            
            int i = 0;
            for (Map.Entry<String, VirtualAppEnvironment.VirtualAppInfo> entry : installedApps.entrySet()) {
                String packageName = entry.getKey();
                VirtualAppEnvironment.VirtualAppInfo appInfo = entry.getValue();
                
                appNames[i] = appInfo.appName != null ? appInfo.appName : packageName;
                packageNames[i] = packageName;
                
                i++;
            }
            
            // عرض مربع حوار التحديد
            new AlertDialog.Builder(this)
                    .setTitle("اختر تطبيقًا لتشغيله")
                    .setItems(appNames, (dialog, which) -> {
                        String selectedPackage = packageNames[which];
                        launchApp(selectedPackage);
                    })
                    .setNegativeButton("إلغاء", null)
                    .show();
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء فتح مربع حوار تحديد التطبيق", e);
            Toast.makeText(this, "خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * تشغيل تطبيق
     */
    private void launchApp(String packageName) {
        try {
            updateStatus("جارٍ تشغيل التطبيق: " + packageName);
            
            // تشغيل التطبيق في خلفية منفصلة
            new Thread(() -> {
                try {
                    // تشغيل التطبيق
                    boolean launched = mVirtualAppEnvironment.launchApp(packageName);
                    
                    // تحديث واجهة المستخدم على الخيط الرئيسي
                    mMainHandler.post(() -> {
                        if (launched) {
                            updateStatus("تم تشغيل التطبيق بنجاح: " + packageName);
                        } else {
                            updateStatus("فشل في تشغيل التطبيق: " + packageName);
                            showErrorDialog("فشل في تشغيل التطبيق", "تحقق من سجلات الأخطاء لمزيد من المعلومات.");
                        }
                    });
                } catch (Exception e) {
                    mMainHandler.post(() -> {
                        mErrorLogger.logException(TAG, "خطأ أثناء تشغيل التطبيق", e);
                        updateStatus("خطأ: " + e.getMessage());
                        showErrorDialog("خطأ أثناء تشغيل التطبيق", e.getMessage());
                    });
                }
            }).start();
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء تشغيل التطبيق", e);
            updateStatus("خطأ: " + e.getMessage());
        }
    }
    
    /**
     * تحديد تطبيق للتثبيت
     */
    private void selectAppToInstall() {
        try {
            // إنشاء نية لاختيار ملف APK
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/vnd.android.package-archive");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            
            // بدء النشاط للحصول على نتيجة
            startActivityForResult(Intent.createChooser(intent, "اختر ملف APK"), REQUEST_INSTALL_APP);
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء تحديد تطبيق للتثبيت", e);
            Toast.makeText(this, "خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * فتح إعدادات الكاميرا
     */
    private void openCameraSettings() {
        try {
            // بدء نشاط إعدادات الكاميرا
            Intent intent = new Intent(this, CameraSettingsActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء فتح إعدادات الكاميرا", e);
            Toast.makeText(this, "خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * فتح إعدادات الموقع
     */
    private void openLocationSettings() {
        try {
            // بدء نشاط إعدادات الموقع
            Intent intent = new Intent(this, LocationSettingsActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء فتح إعدادات الموقع", e);
            Toast.makeText(this, "خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * فتح الإعدادات المتقدمة
     */
    private void openAdvancedSettings() {
        try {
            // بدء نشاط الإعدادات المتقدمة
            Intent intent = new Intent(this, AdvancedSettingsActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء فتح الإعدادات المتقدمة", e);
            Toast.makeText(this, "خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * التبديل إلى الكاميرا الحقيقية
     */
    private void switchToRealCamera() {
        try {
            // تعيين مصدر الكاميرا إلى الكاميرا الحقيقية
            mCameraManager.setSource(CameraManager.SOURCE_REAL_CAMERA);
            
            // تحديث واجهة المستخدم
            updateCameraSourceUI(CameraManager.SOURCE_REAL_CAMERA);
            
            // تحديث الإعدادات
            mPreferenceManager.setInt("current_camera_source", CameraManager.SOURCE_REAL_CAMERA);
            
            updateStatus("تم التبديل إلى الكاميرا الحقيقية");
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء التبديل إلى الكاميرا الحقيقية", e);
            Toast.makeText(this, "خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * تحديد فيديو محلي
     */
    private void selectLocalVideo() {
        try {
            // عرض مربع حوار الخيارات
            new AlertDialog.Builder(this)
                    .setTitle("مصدر الفيديو المحلي")
                    .setItems(new String[]{"التقاط فيديو جديد", "اختيار فيديو من المعرض"}, (dialog, which) -> {
                        if (which == 0) {
                            // التقاط فيديو جديد
                            Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                            if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
                                startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
                            } else {
                                Toast.makeText(this, "لا يوجد تطبيق لالتقاط الفيديو.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // اختيار فيديو من المعرض
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("video/*");
                            startActivityForResult(Intent.createChooser(intent, "اختر فيديو"), REQUEST_VIDEO_PICK);
                        }
                    })
                    .setNegativeButton("إلغاء", null)
                    .show();
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء تحديد فيديو محلي", e);
            Toast.makeText(this, "خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * إدخال عنوان URL للفيديو الشبكي
     */
    private void enterNetworkVideoUrl() {
        try {
            // تطبيق حقيقي سيعرض مربع حوار لإدخال URL
            // هنا نستخدم URL افتراضي للتوضيح
            String defaultUrl = "https://example.com/video.mp4";
            
            // تعيين مصدر الكاميرا إلى الفيديو الشبكي
            mCameraManager.setNetworkVideoUrl(defaultUrl);
            mCameraManager.setSource(CameraManager.SOURCE_NETWORK_VIDEO);
            
            // تحديث واجهة المستخدم
            updateCameraSourceUI(CameraManager.SOURCE_NETWORK_VIDEO);
            
            // تحديث الإعدادات
            mPreferenceManager.setInt("current_camera_source", CameraManager.SOURCE_NETWORK_VIDEO);
            mPreferenceManager.setString("network_video_url", defaultUrl);
            
            updateStatus("تم التبديل إلى فيديو شبكي: " + defaultUrl);
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء إدخال عنوان URL للفيديو الشبكي", e);
            Toast.makeText(this, "خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * تحديد صورة محلية
     */
    private void selectLocalPicture() {
        try {
            // عرض مربع حوار الخيارات
            new AlertDialog.Builder(this)
                    .setTitle("مصدر الصورة المحلية")
                    .setItems(new String[]{"التقاط صورة جديدة", "اختيار صورة من المعرض"}, (dialog, which) -> {
                        if (which == 0) {
                            // التقاط صورة جديدة
                            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                            } else {
                                Toast.makeText(this, "لا يوجد تطبيق لالتقاط الصور.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // اختيار صورة من المعرض
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            startActivityForResult(Intent.createChooser(intent, "اختر صورة"), REQUEST_IMAGE_PICK);
                        }
                    })
                    .setNegativeButton("إلغاء", null)
                    .show();
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء تحديد صورة محلية", e);
            Toast.makeText(this, "خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * تحديث واجهة مستخدم مصدر الكاميرا
     */
    private void updateCameraSourceUI(int source) {
        // إعادة تعيين جميع الأزرار
        mRealCameraButton.setSelected(false);
        mLocalVideoButton.setSelected(false);
        mNetworkVideoButton.setSelected(false);
        mLocalPictureButton.setSelected(false);
        
        // تحديد الزر الصحيح
        switch (source) {
            case CameraManager.SOURCE_REAL_CAMERA:
                mRealCameraButton.setSelected(true);
                break;
            case CameraManager.SOURCE_LOCAL_VIDEO:
                mLocalVideoButton.setSelected(true);
                break;
            case CameraManager.SOURCE_NETWORK_VIDEO:
                mNetworkVideoButton.setSelected(true);
                break;
            case CameraManager.SOURCE_LOCAL_PICTURE:
                mLocalPictureButton.setSelected(true);
                break;
        }
    }
    
    /**
     * تحديث نص الحالة
     */
    private void updateStatus(String status) {
        if (mStatusTextView != null) {
            mStatusTextView.setText(status);
        }
        Log.d(TAG, "الحالة: " + status);
    }
    
    /**
     * عرض مربع حوار الخطأ
     */
    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("موافق", null)
                .show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        
        try {
            switch (requestCode) {
                case REQUEST_INSTALL_APP:
                    // تثبيت التطبيق المحدد
                    handleAppInstallResult(data);
                    break;
                case REQUEST_VIDEO_CAPTURE:
                case REQUEST_VIDEO_PICK:
                    // تعيين الفيديو المحلي
                    handleVideoResult(data);
                    break;
                case REQUEST_IMAGE_CAPTURE:
                case REQUEST_IMAGE_PICK:
                    // تعيين الصورة المحلية
                    handleImageResult(data);
                    break;
            }
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء معالجة نتيجة النشاط", e);
            Toast.makeText(this, "خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * معالجة نتيجة تثبيت التطبيق
     */
    private void handleAppInstallResult(Intent data) {
        try {
            Uri apkUri = data.getData();
            if (apkUri == null) {
                Toast.makeText(this, "لم يتم تحديد ملف APK.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            updateStatus("جارٍ تثبيت التطبيق...");
            
            // تشغيل التثبيت في خلفية منفصلة
            new Thread(() -> {
                try {
                    // نسخ الملف إلى التخزين المؤقت
                    File tempApk = copyUriToTempFile(apkUri);
                    if (tempApk == null) {
                        mMainHandler.post(() -> {
                            updateStatus("فشل في نسخ ملف APK");
                            Toast.makeText(this, "فشل في نسخ ملف APK.", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                    
                    // تثبيت التطبيق
                    boolean installed = mVirtualAppEnvironment.installApp(tempApk.getAbsolutePath());
                    
                    // تنظيف الملف المؤقت
                    tempApk.delete();
                    
                    // تحديث واجهة المستخدم على الخيط الرئيسي
                    mMainHandler.post(() -> {
                        if (installed) {
                            updateStatus("تم تثبيت التطبيق بنجاح");
                            Toast.makeText(this, "تم تثبيت التطبيق بنجاح.", Toast.LENGTH_SHORT).show();
                            
                            // إعادة تحميل التطبيقات المثبتة
                            loadInstalledApps();
                        } else {
                            updateStatus("فشل في تثبيت التطبيق");
                            Toast.makeText(this, "فشل في تثبيت التطبيق.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    mMainHandler.post(() -> {
                        mErrorLogger.logException(TAG, "خطأ أثناء تثبيت التطبيق", e);
                        updateStatus("خطأ: " + e.getMessage());
                        Toast.makeText(this, "خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء معالجة نتيجة تثبيت التطبيق", e);
            updateStatus("خطأ: " + e.getMessage());
        }
    }
    
    /**
     * معالجة نتيجة تحديد الفيديو
     */
    private void handleVideoResult(Intent data) {
        try {
            Uri videoUri = data.getData();
            if (videoUri == null) {
                Toast.makeText(this, "لم يتم تحديد فيديو.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // نسخ الفيديو إلى التخزين المؤقت
            File tempVideo = copyUriToTempFile(videoUri);
            if (tempVideo == null) {
                Toast.makeText(this, "فشل في نسخ الفيديو.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // تعيين مصدر الكاميرا إلى الفيديو المحلي
            mCameraManager.setLocalVideoPath(tempVideo.getAbsolutePath());
            mCameraManager.setSource(CameraManager.SOURCE_LOCAL_VIDEO);
            
            // تحديث واجهة المستخدم
            updateCameraSourceUI(CameraManager.SOURCE_LOCAL_VIDEO);
            
            // تحديث الإعدادات
            mPreferenceManager.setInt("current_camera_source", CameraManager.SOURCE_LOCAL_VIDEO);
            mPreferenceManager.setString("local_video_path", tempVideo.getAbsolutePath());
            
            updateStatus("تم التبديل إلى فيديو محلي: " + tempVideo.getName());
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء معالجة نتيجة تحديد الفيديو", e);
            Toast.makeText(this, "خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * معالجة نتيجة تحديد الصورة
     */
    private void handleImageResult(Intent data) {
        try {
            Bitmap bitmap = null;
            
            // الحصول على الصورة من البيانات
            if (data.hasExtra("data")) {
                // صورة من التقاط الكاميرا
                bitmap = (Bitmap) data.getExtras().get("data");
            } else {
                // صورة من المعرض
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                }
            }
            
            if (bitmap == null) {
                Toast.makeText(this, "لم يتم تحديد صورة.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // حفظ الصورة إلى ملف مؤقت
            File tempImageFile = new File(getCacheDir(), "temp_image_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(tempImageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            
            // تعيين مصدر الكاميرا إلى الصورة المحلية
            mCameraManager.setLocalPicturePath(tempImageFile.getAbsolutePath());
            mCameraManager.setSource(CameraManager.SOURCE_LOCAL_PICTURE);
            
            // تحديث واجهة المستخدم
            updateCameraSourceUI(CameraManager.SOURCE_LOCAL_PICTURE);
            
            // تحديث الإعدادات
            mPreferenceManager.setInt("current_camera_source", CameraManager.SOURCE_LOCAL_PICTURE);
            mPreferenceManager.setString("local_picture_path", tempImageFile.getAbsolutePath());
            
            // عرض معاينة الصورة
            mPreviewImageView.setImageBitmap(bitmap);
            mPreviewImageView.setVisibility(View.VISIBLE);
            
            updateStatus("تم التبديل إلى صورة محلية: " + tempImageFile.getName());
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء معالجة نتيجة تحديد الصورة", e);
            Toast.makeText(this, "خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * نسخ URI إلى ملف مؤقت
     */
    private File copyUriToTempFile(Uri uri) {
        try {
            // إنشاء ملف مؤقت
            String fileName = "temp_" + System.currentTimeMillis();
            File tempFile = new File(getCacheDir(), fileName);
            
            // نسخ المحتوى
            java.io.InputStream input = getContentResolver().openInputStream(uri);
            java.io.OutputStream output = new FileOutputStream(tempFile);
            
            byte[] buffer = new byte[4 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            
            output.flush();
            output.close();
            input.close();
            
            return tempFile;
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء نسخ URI إلى ملف مؤقت", e);
            return null;
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // تحديث واجهة المستخدم لمصدر الكاميرا
        updateCameraSourceUI(mPreferenceManager.getInt("current_camera_source", CameraManager.SOURCE_REAL_CAMERA));
    }
}