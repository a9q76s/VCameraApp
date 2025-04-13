package com.vcamera.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vcamera.app.R;
import com.vcamera.app.VCameraApplication;
import com.vcamera.app.core.AppManager;
import com.vcamera.app.core.ErrorLogger;

import java.util.List;

/**
 * نشاط إدارة التطبيقات
 * يسمح للمستخدم بتثبيت وإدارة التطبيقات في البيئة الافتراضية
 */
public class AppManagerActivity extends AppCompatActivity implements 
        AppAdapter.AppClickListener, 
        AppManager.AppEventListener {
    
    private static final String TAG = "AppManagerActivity";
    private static final int REQUEST_PICK_APK = 1001;
    
    private RecyclerView mAppsRecyclerView;
    private AppAdapter mAppAdapter;
    private View mEmptyView;
    private ProgressBar mProgressBar;
    private EditText mSearchEditText;
    private Button mSearchButton;
    private View mInstallSection;
    private Button mInstallButton;
    private TextView mTitleText;
    
    private AppManager mAppManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_manager);
        
        ErrorLogger.log(TAG, "AppManagerActivity onCreate");
        
        // تهيئة مدير التطبيقات
        mAppManager = ((VCameraApplication) getApplication()).getAppManager();
        
        // تهيئة عناصر واجهة المستخدم
        initViews();
        
        // التسجيل كمستمع لأحداث التطبيقات
        mAppManager.addAppEventListener(this);
        
        // تحميل قائمة التطبيقات
        loadApps();
    }
    
    @Override
    protected void onDestroy() {
        // إلغاء التسجيل من مستمع أحداث التطبيقات
        if (mAppManager != null) {
            mAppManager.removeAppEventListener(this);
        }
        
        super.onDestroy();
    }
    
    /**
     * تهيئة عناصر واجهة المستخدم
     */
    private void initViews() {
        // عنوان النشاط
        mTitleText = findViewById(R.id.text_title);
        mTitleText.setText("Clone App");
        
        // تهيئة RecyclerView
        mAppsRecyclerView = findViewById(R.id.recycler_apps);
        mAppsRecyclerView.setLayoutManager(new GridLayoutManager(this, 1)); // عرض قائمة
        
        // تهيئة Adapter
        mAppAdapter = new AppAdapter(this);
        mAppAdapter.setAppClickListener(this);
        mAppsRecyclerView.setAdapter(mAppAdapter);
        
        // تهيئة عناصر أخرى
        mEmptyView = findViewById(R.id.empty_view);
        mProgressBar = findViewById(R.id.progress_bar);
        
        // قسم البحث
        mSearchEditText = findViewById(R.id.edit_search);
        mSearchButton = findViewById(R.id.button_search);
        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchApps();
            }
        });
        
        // قسم التثبيت
        mInstallSection = findViewById(R.id.layout_install);
        mInstallButton = findViewById(R.id.button_install);
        mInstallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectApkFile();
            }
        });
    }
    
    /**
     * تحميل قائمة التطبيقات
     */
    private void loadApps() {
        showProgress(true);
        
        // في خلفية منفصلة
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<AppManager.AppInfo> apps = mAppManager.getInstalledApps();
                
                // تحديث واجهة المستخدم في الموجه الرئيسي
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAppAdapter.setApps(apps);
                        updateEmptyViewVisibility();
                        showProgress(false);
                    }
                });
            }
        }).start();
    }
    
    /**
     * تحديث حالة رؤية العرض الفارغ
     */
    private void updateEmptyViewVisibility() {
        if (mAppAdapter.getItemCount() == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
            mAppsRecyclerView.setVisibility(View.GONE);
        } else {
            mEmptyView.setVisibility(View.GONE);
            mAppsRecyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * إظهار/إخفاء مؤشر التقدم
     */
    private void showProgress(boolean show) {
        mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    
    /**
     * البحث عن تطبيقات
     */
    private void searchApps() {
        String query = mSearchEditText.getText().toString().trim();
        
        if (query.isEmpty()) {
            // إذا كان الاستعلام فارغًا، أعد تحميل كل التطبيقات
            loadApps();
            return;
        }
        
        showProgress(true);
        
        // في خلفية منفصلة
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<AppManager.AppInfo> allApps = mAppManager.getInstalledApps();
                
                // تصفية التطبيقات حسب الاستعلام
                // في هذا المثال، سنقوم بتصفية يدوية
                // في تطبيق حقيقي، قد نوفر وظيفة بحث في AppManager
                
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAppAdapter.setApps(allApps);
                        updateEmptyViewVisibility();
                        showProgress(false);
                    }
                });
            }
        }).start();
    }
    
    /**
     * اختيار ملف APK
     */
    private void selectApkFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.android.package-archive");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        try {
            startActivityForResult(Intent.createChooser(intent, "Select APK"), REQUEST_PICK_APK);
        } catch (Exception e) {
            ErrorLogger.logError(TAG, "Failed to show file picker", e);
            Toast.makeText(this, "Cannot open file picker", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_PICK_APK && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                
                // تثبيت التطبيق
                installApp(uri.toString());
            }
        }
    }
    
    /**
     * تثبيت تطبيق من مسار ملف
     */
    private void installApp(String filePath) {
        showProgress(true);
        
        // استخدام مدير التطبيقات لتثبيت التطبيق
        mAppManager.installApp(filePath, 0, new AppManager.AppInstallCallback() {
            @Override
            public void onInstallResult(boolean success, String message) {
                showProgress(false);
                
                if (success) {
                    Toast.makeText(AppManagerActivity.this, "App installed successfully", Toast.LENGTH_SHORT).show();
                    // تحديث القائمة
                    loadApps();
                } else {
                    Toast.makeText(AppManagerActivity.this, "Installation failed: " + message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    
    /**
     * إزالة تطبيق
     */
    private void uninstallApp(String packageName, int userId) {
        showProgress(true);
        
        // استخدام مدير التطبيقات لإزالة التطبيق
        mAppManager.uninstallApp(packageName, userId, new AppManager.AppUninstallCallback() {
            @Override
            public void onUninstallResult(boolean success, String message) {
                showProgress(false);
                
                if (success) {
                    Toast.makeText(AppManagerActivity.this, "App uninstalled successfully", Toast.LENGTH_SHORT).show();
                    // تحديث القائمة
                    loadApps();
                } else {
                    Toast.makeText(AppManagerActivity.this, "Uninstallation failed: " + message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    
    @Override
    public void onAppClick(String packageName, int userId) {
        // إطلاق التطبيق
        mAppManager.launchApp(this, packageName, userId);
    }
    
    @Override
    public void onAppLongClick(String packageName, int userId) {
        // عرض خيارات التطبيق
        showAppOptions(packageName, userId);
    }
    
    /**
     * عرض خيارات التطبيق
     */
    private void showAppOptions(final String packageName, final int userId) {
        // في تطبيق حقيقي، سنقوم بعرض مربع حوار أو قائمة خيارات
        
        // لبساطة المثال، سنقوم بإزالة التطبيق مباشرة
        Toast.makeText(this, "Uninstalling " + packageName, Toast.LENGTH_SHORT).show();
        uninstallApp(packageName, userId);
    }
    
    @Override
    public void onAppListChanged() {
        // تحديث القائمة عند تغيير قائمة التطبيقات
        loadApps();
    }
}