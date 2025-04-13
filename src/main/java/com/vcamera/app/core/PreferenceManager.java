package com.vcamera.app.core;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * مدير التفضيلات
 * يوفر واجهة للتعامل مع تخزين واسترجاع الإعدادات والتفضيلات
 */
public class PreferenceManager {
    private static final String PREF_NAME = "vcamera_preferences";
    private static final String TAG = "PreferenceManager";
    
    private static PreferenceManager sInstance;
    private final SharedPreferences mPreferences;
    
    private PreferenceManager(Context context) {
        mPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * تهيئة مدير التفضيلات
     */
    public static void init(Context context) {
        if (sInstance == null) {
            sInstance = new PreferenceManager(context.getApplicationContext());
        }
    }
    
    /**
     * الحصول على نسخة مدير التفضيلات
     */
    public static PreferenceManager getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("PreferenceManager not initialized");
        }
        return sInstance;
    }
    
    /**
     * تخزين قيمة نصية
     */
    public void putString(String key, String value) {
        mPreferences.edit().putString(key, value).apply();
    }
    
    /**
     * الحصول على قيمة نصية
     */
    public String getString(String key, String defaultValue) {
        return mPreferences.getString(key, defaultValue);
    }
    
    /**
     * تخزين قيمة منطقية
     */
    public void putBoolean(String key, boolean value) {
        mPreferences.edit().putBoolean(key, value).apply();
    }
    
    /**
     * الحصول على قيمة منطقية
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return mPreferences.getBoolean(key, defaultValue);
    }
    
    /**
     * تخزين قيمة عددية صحيحة
     */
    public void putInt(String key, int value) {
        mPreferences.edit().putInt(key, value).apply();
    }
    
    /**
     * الحصول على قيمة عددية صحيحة
     */
    public int getInt(String key, int defaultValue) {
        return mPreferences.getInt(key, defaultValue);
    }
    
    /**
     * تخزين قيمة عددية طويلة
     */
    public void putLong(String key, long value) {
        mPreferences.edit().putLong(key, value).apply();
    }
    
    /**
     * الحصول على قيمة عددية طويلة
     */
    public long getLong(String key, long defaultValue) {
        return mPreferences.getLong(key, defaultValue);
    }
    
    /**
     * تخزين قيمة عائمة
     */
    public void putFloat(String key, float value) {
        mPreferences.edit().putFloat(key, value).apply();
    }
    
    /**
     * الحصول على قيمة عائمة
     */
    public float getFloat(String key, float defaultValue) {
        return mPreferences.getFloat(key, defaultValue);
    }
    
    /**
     * حذف قيمة
     */
    public void remove(String key) {
        mPreferences.edit().remove(key).apply();
    }
    
    /**
     * مسح كل القيم
     */
    public void clear() {
        mPreferences.edit().clear().apply();
    }
    
    /**
     * التحقق من وجود قيمة
     */
    public boolean contains(String key) {
        return mPreferences.contains(key);
    }
}