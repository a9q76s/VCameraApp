package com.vcamera.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.vcamera.app.R;
import com.vcamera.app.VCameraApplication;
import com.vcamera.app.core.CameraManager;
import com.vcamera.app.core.ErrorLogger;

/**
 * نشاط إعدادات الكاميرا الافتراضية
 * يسمح للمستخدم باختيار مصدر الكاميرا والإعدادات ذات الصلة
 */
public class CameraSettingsActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "CameraSettingsActivity";
    
    private static final int REQUEST_PICK_VIDEO = 1001;
    private static final int REQUEST_PICK_PICTURE = 1002;
    
    private CameraManager mCameraManager;
    
    private View mRealCameraOption;
    private View mLocalVideoOption;
    private View mNetworkVideoOption;
    private View mLocalPictureOption;
    private TextView mCurrentSourceText;
    private TextView mLocalVideoPathText;
    private TextView mNetworkVideoUrlText;
    private TextView mLocalPicturePathText;
    private Button mSaveButton;
    private Switch mEnableAudioSwitch;
    
    private int mSelectedSource;
    private String mLocalVideoPath;
    private String mNetworkVideoUrl;
    private String mLocalPicturePath;
    private boolean mEnableAudio;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_settings);
        
        ErrorLogger.log(TAG, "CameraSettingsActivity onCreate");
        
        // تهيئة مدير الكاميرا
        mCameraManager = ((VCameraApplication) getApplication()).getCameraManager();
        
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
        titleText.setText("Virtual Camera Setting");
        
        // نص الحماية
        TextView protectionText = findViewById(R.id.text_protection);
        protectionText.setText("Protect Your Camera Privacy.");
        
        // خيارات مصدر الكاميرا
        mRealCameraOption = findViewById(R.id.option_real_camera);
        mLocalVideoOption = findViewById(R.id.option_local_video);
        mNetworkVideoOption = findViewById(R.id.option_network_video);
        mLocalPictureOption = findViewById(R.id.option_local_picture);
        
        // نص المصدر الحالي
        mCurrentSourceText = findViewById(R.id.text_current_source);
        
        // نصوص المسارات
        mLocalVideoPathText = findViewById(R.id.text_local_video_path);
        mNetworkVideoUrlText = findViewById(R.id.text_network_video_url);
        mLocalPicturePathText = findViewById(R.id.text_local_picture_path);
        
        // تبديل الصوت
        mEnableAudioSwitch = findViewById(R.id.switch_enable_audio);
        mEnableAudioSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mEnableAudio = isChecked;
            }
        });
        
        // زر الحفظ
        mSaveButton = findViewById(R.id.button_save);
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });
        
        // إعداد مستمعي النقر
        mRealCameraOption.setOnClickListener(this);
        mLocalVideoOption.setOnClickListener(this);
        mNetworkVideoOption.setOnClickListener(this);
        mLocalPictureOption.setOnClickListener(this);
        
        // أزرار تحديد الملفات
        findViewById(R.id.button_select_video).setOnClickListener(this);
        findViewById(R.id.button_select_picture).setOnClickListener(this);
    }
    
    /**
     * تحميل الإعدادات الحالية
     */
    private void loadCurrentSettings() {
        mSelectedSource = mCameraManager.getCurrentSource();
        mLocalVideoPath = mCameraManager.getLocalVideoPath();
        mNetworkVideoUrl = mCameraManager.getNetworkVideoUrl();
        mLocalPicturePath = mCameraManager.getLocalPicturePath();
        mEnableAudio = true; // افتراضيًا
        
        updateUI();
    }
    
    /**
     * تحديث واجهة المستخدم
     */
    private void updateUI() {
        // تحديث المصدر المحدد
        updateSelectedSource();
        
        // تحديث نصوص المسارات
        mLocalVideoPathText.setText(mLocalVideoPath != null ? mLocalVideoPath : "No video selected");
        mNetworkVideoUrlText.setText(mNetworkVideoUrl != null ? mNetworkVideoUrl : "No URL entered");
        mLocalPicturePathText.setText(mLocalPicturePath != null ? mLocalPicturePath : "No picture selected");
        
        // تحديث مفتاح الصوت
        mEnableAudioSwitch.setChecked(mEnableAudio);
    }
    
    /**
     * تحديث المصدر المحدد
     */
    private void updateSelectedSource() {
        // إعادة تعيين كل الخيارات
        mRealCameraOption.setSelected(false);
        mLocalVideoOption.setSelected(false);
        mNetworkVideoOption.setSelected(false);
        mLocalPictureOption.setSelected(false);
        
        // تعيين المصدر المحدد
        switch (mSelectedSource) {
            case CameraManager.SOURCE_REAL_CAMERA:
                mRealCameraOption.setSelected(true);
                mCurrentSourceText.setText("1. Disable,use real camera device");
                break;
            case CameraManager.SOURCE_LOCAL_VIDEO:
                mLocalVideoOption.setSelected(true);
                mCurrentSourceText.setText("2. Use Local Video");
                break;
            case CameraManager.SOURCE_NETWORK_VIDEO:
                mNetworkVideoOption.setSelected(true);
                mCurrentSourceText.setText("3. Use Network Video Stream");
                break;
            case CameraManager.SOURCE_LOCAL_PICTURE:
                mLocalPictureOption.setSelected(true);
                mCurrentSourceText.setText("4. Use Local Picture");
                break;
        }
    }
    
    /**
     * حفظ الإعدادات
     */
    private void saveSettings() {
        ErrorLogger.log(TAG, "Saving camera settings: source=" + mSelectedSource);
        
        // التحقق من المسار/العنوان المطلوب بناءً على المصدر المحدد
        boolean isValid = true;
        
        switch (mSelectedSource) {
            case CameraManager.SOURCE_LOCAL_VIDEO:
                if (mLocalVideoPath == null) {
                    Toast.makeText(this, "Please select a local video", Toast.LENGTH_SHORT).show();
                    isValid = false;
                }
                break;
            case CameraManager.SOURCE_NETWORK_VIDEO:
                if (mNetworkVideoUrl == null || mNetworkVideoUrl.isEmpty()) {
                    Toast.makeText(this, "Please enter a network video URL", Toast.LENGTH_SHORT).show();
                    isValid = false;
                }
                break;
            case CameraManager.SOURCE_LOCAL_PICTURE:
                if (mLocalPicturePath == null) {
                    Toast.makeText(this, "Please select a local picture", Toast.LENGTH_SHORT).show();
                    isValid = false;
                }
                break;
        }
        
        if (!isValid) {
            return;
        }
        
        // حفظ الإعدادات
        mCameraManager.setCameraSource(mSelectedSource);
        
        if (mLocalVideoPath != null) {
            mCameraManager.setLocalVideoPath(mLocalVideoPath);
        }
        
        if (mNetworkVideoUrl != null) {
            mCameraManager.setNetworkVideoUrl(mNetworkVideoUrl);
        }
        
        if (mLocalPicturePath != null) {
            mCameraManager.setLocalPicturePath(mLocalPicturePath);
        }
        
        // حفظ إعدادات الصوت (في تطبيق حقيقي)
        
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    /**
     * تحديد فيديو محلي
     */
    private void selectLocalVideo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        try {
            startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_PICK_VIDEO);
        } catch (Exception e) {
            ErrorLogger.logError(TAG, "Failed to show video picker", e);
            Toast.makeText(this, "Cannot open video picker", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * تحديد صورة محلية
     */
    private void selectLocalPicture() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        try {
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_PICK_PICTURE);
        } catch (Exception e) {
            ErrorLogger.logError(TAG, "Failed to show picture picker", e);
            Toast.makeText(this, "Cannot open picture picker", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            
            if (requestCode == REQUEST_PICK_VIDEO) {
                mLocalVideoPath = uri.toString();
                mLocalVideoPathText.setText(mLocalVideoPath);
            } else if (requestCode == REQUEST_PICK_PICTURE) {
                mLocalPicturePath = uri.toString();
                mLocalPicturePathText.setText(mLocalPicturePath);
            }
        }
    }
    
    @Override
    public void onClick(View v) {
        int id = v.getId();
        
        if (id == R.id.option_real_camera) {
            mSelectedSource = CameraManager.SOURCE_REAL_CAMERA;
            updateSelectedSource();
        } else if (id == R.id.option_local_video) {
            mSelectedSource = CameraManager.SOURCE_LOCAL_VIDEO;
            updateSelectedSource();
        } else if (id == R.id.option_network_video) {
            mSelectedSource = CameraManager.SOURCE_NETWORK_VIDEO;
            updateSelectedSource();
        } else if (id == R.id.option_local_picture) {
            mSelectedSource = CameraManager.SOURCE_LOCAL_PICTURE;
            updateSelectedSource();
        } else if (id == R.id.button_select_video) {
            selectLocalVideo();
        } else if (id == R.id.button_select_picture) {
            selectLocalPicture();
        }
    }
}