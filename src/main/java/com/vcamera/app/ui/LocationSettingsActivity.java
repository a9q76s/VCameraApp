package com.vcamera.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.vcamera.app.R;
import com.vcamera.app.VCameraApplication;
import com.vcamera.app.core.ErrorLogger;
import com.vcamera.app.core.LocationManager;

/**
 * نشاط إعدادات الموقع
 * يسمح للمستخدم بتمكين وإدارة الموقع الوهمي
 */
public class LocationSettingsActivity extends AppCompatActivity {
    private static final String TAG = "LocationSettingsActivity";
    
    private LocationManager mLocationManager;
    
    private Switch mEnableMockLocationSwitch;
    private LinearLayout mLocationManagerLayout;
    private Button mOpenLocationManagerButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_settings);
        
        ErrorLogger.log(TAG, "LocationSettingsActivity onCreate");
        
        // تهيئة مدير الموقع
        mLocationManager = ((VCameraApplication) getApplication()).getLocationManager();
        
        // تهيئة عناصر واجهة المستخدم
        initViews();
        
        // تحميل الإعدادات الحالية
        loadCurrentSettings();
    }
    
    /**
     * تهيئة عناصر واجهة المستخدم
     */
    private void initViews() {
        // عنوان النشاط
        TextView titleText = findViewById(R.id.text_title);
        titleText.setText("Location Setting");
        
        // تبديل تمكين الموقع الوهمي
        mEnableMockLocationSwitch = findViewById(R.id.switch_enable_mock_location);
        mEnableMockLocationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onMockLocationToggled(isChecked);
            }
        });
        
        // تخطيط مدير الموقع
        mLocationManagerLayout = findViewById(R.id.layout_location_manager);
        
        // زر فتح مدير الموقع
        mOpenLocationManagerButton = findViewById(R.id.button_open_location_manager);
        mOpenLocationManagerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLocationManager();
            }
        });
    }
    
    /**
     * تحميل الإعدادات الحالية
     */
    private void loadCurrentSettings() {
        boolean isMockLocationEnabled = mLocationManager.isMockLocationEnabled();
        mEnableMockLocationSwitch.setChecked(isMockLocationEnabled);
        
        // تحديث واجهة المستخدم
        updateUI(isMockLocationEnabled);
    }
    
    /**
     * تحديث واجهة المستخدم
     */
    private void updateUI(boolean isMockLocationEnabled) {
        if (isMockLocationEnabled) {
            mLocationManagerLayout.setVisibility(View.VISIBLE);
        } else {
            mLocationManagerLayout.setVisibility(View.GONE);
        }
    }
    
    /**
     * معالجة تبديل الموقع الوهمي
     */
    private void onMockLocationToggled(boolean isChecked) {
        ErrorLogger.log(TAG, "Mock location toggled: " + isChecked);
        
        // تحديث الإعداد في مدير الموقع
        mLocationManager.setMockLocationEnabled(isChecked);
        
        // تحديث واجهة المستخدم
        updateUI(isChecked);
        
        // عرض رسالة
        String message = isChecked ? "Mock location enabled" : "Mock location disabled";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        
        if (isChecked) {
            // التحقق من الأذونات إذا تم تمكين الموقع الوهمي
            checkLocationPermissions();
        }
    }
    
    /**
     * التحقق من أذونات الموقع
     */
    private void checkLocationPermissions() {
        if (!mLocationManager.checkLocationPermissions()) {
            Toast.makeText(this, "Location permissions required", Toast.LENGTH_LONG).show();
            // طلب الأذونات
            mLocationManager.requestLocationPermissions(this);
        }
    }
    
    /**
     * فتح مدير الموقع
     */
    private void openLocationManager() {
        // في تطبيق حقيقي، نفتح نشاط مدير الموقع
        // هنا نفتح LocationManagerActivity الذي سيتم إنشاؤه
        
        if (mLocationManager.isMockLocationEnabled()) {
            ErrorLogger.log(TAG, "Opening location manager");
            
            // افتح نشاط مدير الموقع
            // startActivity(new Intent(this, LocationManagerActivity.class));
            
            // لأغراض المثال، نعرض رسالة فقط
            Toast.makeText(this, "Location Manager will be opened here", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Mock location must be enabled first", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (mLocationManager.handlePermissionsResult(requestCode, permissions, grantResults)) {
            // إذا تمت الموافقة على الأذونات
            Toast.makeText(this, "Location permissions granted", Toast.LENGTH_SHORT).show();
        } else {
            // إذا تم رفض الأذونات
            Toast.makeText(this, "Location permissions denied", Toast.LENGTH_LONG).show();
            
            // تعطيل الموقع الوهمي
            mEnableMockLocationSwitch.setChecked(false);
            mLocationManager.setMockLocationEnabled(false);
            updateUI(false);
        }
    }
}