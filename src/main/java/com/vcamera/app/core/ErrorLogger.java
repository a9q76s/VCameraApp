package com.vcamera.app.core;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * مسجل الأخطاء والأحداث في التطبيق
 * يقوم بتسجيل الأحداث والأخطاء في سجل الأندرويد وفي ملف منفصل للتشخيص
 */
public class ErrorLogger {
    private static final String TAG = "VCameraErrorLogger";
    private static final String LOG_FOLDER = "VCamera/logs";
    private static final String LOG_FILE_PREFIX = "vcamera_log_";
    
    private static Context sAppContext;
    private static boolean sIsInitialized = false;
    private static boolean sLogToFile = false;
    private static String sCurrentLogFile;
    
    /**
     * تهيئة مسجل الأخطاء
     */
    public static void init(Context context) {
        sAppContext = context.getApplicationContext();
        sIsInitialized = true;
        
        // تهيئة ملف السجل
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());
        sCurrentLogFile = LOG_FILE_PREFIX + timestamp + ".log";
        
        // إنشاء مجلد السجلات إذا لم يكن موجودًا
        try {
            File logDir = new File(context.getExternalFilesDir(null), LOG_FOLDER);
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            sLogToFile = logDir.exists();
            
            // تسجيل بدء دورة حياة جديدة للتطبيق
            log(TAG, "===========================");
            log(TAG, "VCamera Application Started");
            log(TAG, "===========================");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize log files", e);
            sLogToFile = false;
        }
    }
    
    /**
     * تسجيل رسالة معلومات
     */
    public static void log(String tag, String message) {
        // تسجيل في سجل الأندرويد
        Log.d(tag, message);
        
        // تسجيل في ملف السجل
        if (sIsInitialized && sLogToFile) {
            writeToLogFile(tag, message, null);
        }
    }
    
    /**
     * تسجيل رسالة خطأ مع استثناء
     */
    public static void logError(String tag, String message, Throwable exception) {
        // تسجيل في سجل الأندرويد
        Log.e(tag, message, exception);
        
        // تسجيل في ملف السجل
        if (sIsInitialized && sLogToFile) {
            writeToLogFile(tag, message, exception);
        }
    }
    
    /**
     * تسجيل خطأ إطلاق التطبيق
     */
    public static void logLaunchError(String packageName, int userId, String error) {
        String message = "Launch failed for app: " + packageName + ", userId: " + userId + ", error: " + error;
        Log.e(TAG, message);
        
        // تسجيل في ملف خاص بأخطاء الإطلاق
        if (sIsInitialized && sLogToFile) {
            try {
                File logDir = new File(sAppContext.getExternalFilesDir(null), LOG_FOLDER);
                File launchErrorsFile = new File(logDir, "launch_errors.log");
                
                FileWriter writer = new FileWriter(launchErrorsFile, true);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                String timestamp = dateFormat.format(new Date());
                
                writer.append(timestamp)
                      .append(" - ")
                      .append(message)
                      .append("\n");
                writer.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to write launch error to log file", e);
            }
        }
    }
    
    /**
     * كتابة إلى ملف السجل
     */
    private static void writeToLogFile(String tag, String message, Throwable exception) {
        try {
            File logDir = new File(sAppContext.getExternalFilesDir(null), LOG_FOLDER);
            File logFile = new File(logDir, sCurrentLogFile);
            
            FileWriter writer = new FileWriter(logFile, true);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
            String timestamp = dateFormat.format(new Date());
            
            writer.append(timestamp)
                  .append(" | ")
                  .append(tag)
                  .append(" | ")
                  .append(message)
                  .append("\n");
            
            if (exception != null) {
                writer.append(timestamp)
                      .append(" | ")
                      .append(tag)
                      .append(" | Exception: ")
                      .append(exception.toString())
                      .append("\n");
                
                // كتابة تفاصيل الاستثناء
                StackTraceElement[] stackTrace = exception.getStackTrace();
                for (StackTraceElement element : stackTrace) {
                    writer.append("\t at ")
                          .append(element.toString())
                          .append("\n");
                }
            }
            
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to log file", e);
        }
    }
    
    /**
     * تمكين أو تعطيل التسجيل إلى الملف
     */
    public static void setLogToFile(boolean enable) {
        sLogToFile = enable;
    }
    
    /**
     * الحصول على مسار ملف السجل الحالي
     */
    public static String getCurrentLogFilePath() {
        if (!sIsInitialized || !sLogToFile) {
            return null;
        }
        
        File logDir = new File(sAppContext.getExternalFilesDir(null), LOG_FOLDER);
        File logFile = new File(logDir, sCurrentLogFile);
        return logFile.getAbsolutePath();
    }
    
    /**
     * مسح كل ملفات السجل القديمة
     */
    public static void clearOldLogs() {
        if (!sIsInitialized) {
            return;
        }
        
        try {
            File logDir = new File(sAppContext.getExternalFilesDir(null), LOG_FOLDER);
            if (logDir.exists()) {
                File[] logFiles = logDir.listFiles();
                if (logFiles != null) {
                    long now = System.currentTimeMillis();
                    long maxAge = 7 * 24 * 60 * 60 * 1000L; // 7 أيام
                    
                    for (File file : logFiles) {
                        if (file.isFile() && file.getName().startsWith(LOG_FILE_PREFIX)) {
                            if (now - file.lastModified() > maxAge) {
                                file.delete();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear old logs", e);
        }
    }
}