package com.vcamera.app.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.vcamera.app.utils.BitmapUtils;
import com.vcamera.app.utils.FileUtils;
import com.vcamera.app.utils.MediaUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * مدير الكاميرا الافتراضية
 * يتحكم في مصادر الكاميرا ويوفر واجهة موحدة للتطبيقات
 */
public class CameraManager {
    private static final String TAG = "CameraManager";
    
    // أنواع مصدر الكاميرا
    public static final int SOURCE_REAL_CAMERA = 0;
    public static final int SOURCE_LOCAL_VIDEO = 1;
    public static final int SOURCE_NETWORK_VIDEO = 2;
    public static final int SOURCE_LOCAL_PICTURE = 3;
    
    // مصدر الكاميرا الحالي
    private int mCurrentSource = SOURCE_REAL_CAMERA;
    
    // الكاميرا الحقيقية
    private Camera mRealCamera;
    private Camera.Parameters mRealCameraParams;
    
    // مشغل الفيديو المحلي
    private MediaPlayer mVideoPlayer;
    
    // عرض الكاميرا الافتراضية
    private Surface mOutputSurface;
    
    // مسارات المصادر
    private String mLocalVideoPath;
    private String mNetworkVideoUrl;
    private String mLocalPicturePath;
    
    // حالة الكاميرا
    private boolean mIsInitialized = false;
    private boolean mIsCameraStarted = false;
    
    // استرجاع الإطارات (Frames)
    private static final int FRAME_RATE = 30;
    private VirtualCameraFrameProvider mFrameProvider;
    private Bitmap mCurrentFrameBitmap;
    
    // سياق التطبيق
    private final Context mContext;
    private final ErrorLogger mErrorLogger;
    private final SharedPreferences mPreferences;
    private final Executor mExecutor;
    private final Handler mMainHandler;
    
    // المثيل الوحيد (Singleton)
    private static CameraManager sInstance;
    
    /**
     * مصنع للمثيل الوحيد (Singleton Factory)
     */
    public static CameraManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (CameraManager.class) {
                if (sInstance == null) {
                    sInstance = new CameraManager(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }
    
    /**
     * المنشئ الخاص
     */
    private CameraManager(Context context) {
        mContext = context;
        mErrorLogger = new ErrorLogger(context);
        mPreferences = context.getSharedPreferences("camera_settings", Context.MODE_PRIVATE);
        mExecutor = Executors.newSingleThreadExecutor();
        mMainHandler = new Handler(Looper.getMainLooper());
        
        // تحميل الإعدادات المحفوظة
        loadSettings();
    }
    
    /**
     * تحميل إعدادات الكاميرا من التخزين
     */
    private void loadSettings() {
        mCurrentSource = mPreferences.getInt("current_source", SOURCE_REAL_CAMERA);
        mLocalVideoPath = mPreferences.getString("local_video_path", "");
        mNetworkVideoUrl = mPreferences.getString("network_video_url", "");
        mLocalPicturePath = mPreferences.getString("local_picture_path", "");
    }
    
    /**
     * حفظ إعدادات الكاميرا في التخزين
     */
    private void saveSettings() {
        mPreferences.edit()
                .putInt("current_source", mCurrentSource)
                .putString("local_video_path", mLocalVideoPath != null ? mLocalVideoPath : "")
                .putString("network_video_url", mNetworkVideoUrl != null ? mNetworkVideoUrl : "")
                .putString("local_picture_path", mLocalPicturePath != null ? mLocalPicturePath : "")
                .apply();
    }
    
    /**
     * تهيئة مدير الكاميرا
     */
    public boolean initialize() {
        if (mIsInitialized) {
            return true; // تم التهيئة بالفعل
        }
        
        try {
            Log.i(TAG, "تهيئة مدير الكاميرا...");
            
            // إنشاء مزود الإطارات الافتراضي
            mFrameProvider = new VirtualCameraFrameProvider();
            
            // إنشاء الدليل المؤقت للكاميرا إذا لم يكن موجوداً
            File vcamDir = new File(mContext.getFilesDir(), "vcam");
            if (!vcamDir.exists()) {
                vcamDir.mkdirs();
            }
            
            // تهيئة موفر الإطارات
            mFrameProvider.initialize();
            
            mIsInitialized = true;
            Log.i(TAG, "تم تهيئة مدير الكاميرا بنجاح");
            return true;
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء تهيئة مدير الكاميرا", e);
            return false;
        }
    }
    
    /**
     * بدء تشغيل الكاميرا
     */
    public boolean startCamera(Surface outputSurface) {
        if (!mIsInitialized) {
            mErrorLogger.logError(TAG, "محاولة بدء الكاميرا قبل التهيئة");
            return false;
        }
        
        if (mIsCameraStarted) {
            // الكاميرا قيد التشغيل بالفعل
            stopCamera(); // إيقاف الكاميرا الحالية أولاً
        }
        
        try {
            Log.i(TAG, "بدء تشغيل الكاميرا بالمصدر: " + getSourceName(mCurrentSource));
            
            mOutputSurface = outputSurface;
            
            switch (mCurrentSource) {
                case SOURCE_REAL_CAMERA:
                    startRealCamera();
                    break;
                case SOURCE_LOCAL_VIDEO:
                    startLocalVideo();
                    break;
                case SOURCE_NETWORK_VIDEO:
                    startNetworkVideo();
                    break;
                case SOURCE_LOCAL_PICTURE:
                    startLocalPicture();
                    break;
                default:
                    mErrorLogger.logError(TAG, "مصدر كاميرا غير معروف: " + mCurrentSource);
                    return false;
            }
            
            mIsCameraStarted = true;
            return true;
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء بدء تشغيل الكاميرا", e);
            return false;
        }
    }
    
    /**
     * إيقاف الكاميرا
     */
    public boolean stopCamera() {
        if (!mIsInitialized) {
            mErrorLogger.logError(TAG, "محاولة إيقاف الكاميرا قبل التهيئة");
            return false;
        }
        
        if (!mIsCameraStarted) {
            return true; // الكاميرا متوقفة بالفعل
        }
        
        try {
            Log.i(TAG, "إيقاف الكاميرا...");
            
            switch (mCurrentSource) {
                case SOURCE_REAL_CAMERA:
                    stopRealCamera();
                    break;
                case SOURCE_LOCAL_VIDEO:
                    stopLocalVideo();
                    break;
                case SOURCE_NETWORK_VIDEO:
                    stopNetworkVideo();
                    break;
                case SOURCE_LOCAL_PICTURE:
                    stopLocalPicture();
                    break;
            }
            
            mIsCameraStarted = false;
            mOutputSurface = null;
            
            return true;
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء إيقاف الكاميرا", e);
            return false;
        }
    }
    
    /**
     * تعيين مصدر الكاميرا
     */
    public boolean setSource(int source) {
        if (source < SOURCE_REAL_CAMERA || source > SOURCE_LOCAL_PICTURE) {
            mErrorLogger.logError(TAG, "مصدر كاميرا غير صالح: " + source);
            return false;
        }
        
        try {
            Log.i(TAG, "تغيير مصدر الكاميرا إلى: " + getSourceName(source));
            
            // إذا كانت الكاميرا قيد التشغيل، قم بإيقافها أولاً
            boolean wasStarted = mIsCameraStarted;
            Surface currentSurface = mOutputSurface;
            
            if (wasStarted) {
                stopCamera();
            }
            
            // تعيين المصدر الجديد
            mCurrentSource = source;
            saveSettings();
            
            // إعادة تشغيل الكاميرا إذا كانت قيد التشغيل
            if (wasStarted && currentSurface != null) {
                startCamera(currentSurface);
            }
            
            return true;
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء تعيين مصدر الكاميرا", e);
            return false;
        }
    }
    
    /**
     * تعيين مسار الفيديو المحلي
     */
    public boolean setLocalVideoPath(String path) {
        try {
            File videoFile = new File(path);
            if (!videoFile.exists() || !videoFile.canRead()) {
                mErrorLogger.logError(TAG, "ملف الفيديو غير موجود أو غير قابل للقراءة: " + path);
                return false;
            }
            
            Log.i(TAG, "تعيين مسار الفيديو المحلي: " + path);
            mLocalVideoPath = path;
            saveSettings();
            
            // إذا كان المصدر الحالي هو الفيديو المحلي والكاميرا قيد التشغيل، قم بإعادة تشغيلها
            if (mCurrentSource == SOURCE_LOCAL_VIDEO && mIsCameraStarted) {
                Surface currentSurface = mOutputSurface;
                stopCamera();
                startCamera(currentSurface);
            }
            
            return true;
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء تعيين مسار الفيديو المحلي", e);
            return false;
        }
    }
    
    /**
     * تعيين عنوان URL للفيديو الشبكي
     */
    public boolean setNetworkVideoUrl(String url) {
        try {
            if (url == null || url.isEmpty()) {
                mErrorLogger.logError(TAG, "عنوان URL غير صالح: " + url);
                return false;
            }
            
            Log.i(TAG, "تعيين عنوان URL للفيديو الشبكي: " + url);
            mNetworkVideoUrl = url;
            saveSettings();
            
            // إذا كان المصدر الحالي هو الفيديو الشبكي والكاميرا قيد التشغيل، قم بإعادة تشغيلها
            if (mCurrentSource == SOURCE_NETWORK_VIDEO && mIsCameraStarted) {
                Surface currentSurface = mOutputSurface;
                stopCamera();
                startCamera(currentSurface);
            }
            
            return true;
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء تعيين عنوان URL للفيديو الشبكي", e);
            return false;
        }
    }
    
    /**
     * تعيين مسار الصورة المحلية
     */
    public boolean setLocalPicturePath(String path) {
        try {
            File imageFile = new File(path);
            if (!imageFile.exists() || !imageFile.canRead()) {
                mErrorLogger.logError(TAG, "ملف الصورة غير موجود أو غير قابل للقراءة: " + path);
                return false;
            }
            
            Log.i(TAG, "تعيين مسار الصورة المحلية: " + path);
            mLocalPicturePath = path;
            saveSettings();
            
            // إذا كان المصدر الحالي هو الصورة المحلية والكاميرا قيد التشغيل، قم بإعادة تشغيلها
            if (mCurrentSource == SOURCE_LOCAL_PICTURE && mIsCameraStarted) {
                Surface currentSurface = mOutputSurface;
                stopCamera();
                startCamera(currentSurface);
            }
            
            // تحميل الصورة مسبقاً
            loadLocalPicture(path);
            
            return true;
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء تعيين مسار الصورة المحلية", e);
            return false;
        }
    }
    
    /**
     * الحصول على الإطار الحالي من الكاميرا
     */
    public Bitmap getCurrentFrame() {
        if (!mIsInitialized || !mIsCameraStarted) {
            return null;
        }
        
        return mFrameProvider.getCurrentFrame();
    }
    
    /**
     * بدء تشغيل الكاميرا الحقيقية
     */
    private void startRealCamera() {
        try {
            Log.i(TAG, "بدء تشغيل الكاميرا الحقيقية...");
            
            // فتح الكاميرا الخلفية (افتراضياً)
            mRealCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            
            // تكوين الكاميرا
            mRealCameraParams = mRealCamera.getParameters();
            mRealCameraParams.setPreviewSize(1280, 720);
            mRealCameraParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            mRealCamera.setParameters(mRealCameraParams);
            
            // تعيين معاينة الإخراج
            mRealCamera.setPreviewCallback((data, camera) -> {
                // تحويل بيانات معاينة الكاميرا إلى Bitmap
                Camera.Size size = camera.getParameters().getPreviewSize();
                Bitmap frameBitmap = BitmapUtils.convertYuvToBitmap(data, size.width, size.height);
                
                // تحديث الإطار الحالي
                mFrameProvider.setCurrentFrame(frameBitmap);
            });
            
            // بدء المعاينة
            mRealCamera.startPreview();
            
            Log.i(TAG, "تم بدء تشغيل الكاميرا الحقيقية بنجاح");
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء بدء تشغيل الكاميرا الحقيقية", e);
        }
    }
    
    /**
     * إيقاف الكاميرا الحقيقية
     */
    private void stopRealCamera() {
        try {
            if (mRealCamera != null) {
                mRealCamera.setPreviewCallback(null);
                mRealCamera.stopPreview();
                mRealCamera.release();
                mRealCamera = null;
            }
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء إيقاف الكاميرا الحقيقية", e);
        }
    }
    
    /**
     * بدء تشغيل الفيديو المحلي
     */
    private void startLocalVideo() {
        try {
            Log.i(TAG, "بدء تشغيل الفيديو المحلي: " + mLocalVideoPath);
            
            if (mLocalVideoPath == null || mLocalVideoPath.isEmpty()) {
                mErrorLogger.logError(TAG, "مسار الفيديو المحلي غير محدد");
                return;
            }
            
            File videoFile = new File(mLocalVideoPath);
            if (!videoFile.exists() || !videoFile.canRead()) {
                mErrorLogger.logError(TAG, "ملف الفيديو غير موجود أو غير قابل للقراءة: " + mLocalVideoPath);
                return;
            }
            
            // إنشاء مشغل الفيديو
            mVideoPlayer = new MediaPlayer();
            mVideoPlayer.setDataSource(mContext, Uri.fromFile(videoFile));
            mVideoPlayer.setLooping(true);
            mVideoPlayer.prepare();
            
            // إعداد استخراج الإطارات
            MediaUtils.extractFramesFromVideo(mContext, mVideoPlayer, mLocalVideoPath, (frames) -> {
                // بدء تحديث الإطارات
                mFrameProvider.startFrameSequence(frames, FRAME_RATE);
            });
            
            // بدء تشغيل الفيديو
            mVideoPlayer.start();
            
            Log.i(TAG, "تم بدء تشغيل الفيديو المحلي بنجاح");
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء بدء تشغيل الفيديو المحلي", e);
        }
    }
    
    /**
     * إيقاف الفيديو المحلي
     */
    private void stopLocalVideo() {
        try {
            if (mVideoPlayer != null) {
                if (mVideoPlayer.isPlaying()) {
                    mVideoPlayer.stop();
                }
                mVideoPlayer.release();
                mVideoPlayer = null;
            }
            
            // إيقاف تسلسل الإطارات
            mFrameProvider.stopFrameSequence();
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء إيقاف الفيديو المحلي", e);
        }
    }
    
    /**
     * بدء تشغيل الفيديو الشبكي
     */
    private void startNetworkVideo() {
        try {
            Log.i(TAG, "بدء تشغيل الفيديو الشبكي: " + mNetworkVideoUrl);
            
            if (mNetworkVideoUrl == null || mNetworkVideoUrl.isEmpty()) {
                mErrorLogger.logError(TAG, "عنوان URL للفيديو الشبكي غير محدد");
                return;
            }
            
            // إنشاء مشغل الفيديو
            mVideoPlayer = new MediaPlayer();
            mVideoPlayer.setDataSource(mContext, Uri.parse(mNetworkVideoUrl));
            mVideoPlayer.setLooping(true);
            
            // إعداد المشغل
            mVideoPlayer.setOnPreparedListener(mp -> {
                // بدء تشغيل الفيديو
                mp.start();
                
                // إعداد استخراج الإطارات
                MediaUtils.extractFramesFromNetworkVideo(mContext, mVideoPlayer, mNetworkVideoUrl, (frames) -> {
                    // بدء تحديث الإطارات
                    mFrameProvider.startFrameSequence(frames, FRAME_RATE);
                });
            });
            
            // بدء الإعداد غير المتزامن
            mVideoPlayer.prepareAsync();
            
            Log.i(TAG, "جاري إعداد الفيديو الشبكي...");
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء بدء تشغيل الفيديو الشبكي", e);
        }
    }
    
    /**
     * إيقاف الفيديو الشبكي
     */
    private void stopNetworkVideo() {
        try {
            if (mVideoPlayer != null) {
                if (mVideoPlayer.isPlaying()) {
                    mVideoPlayer.stop();
                }
                mVideoPlayer.release();
                mVideoPlayer = null;
            }
            
            // إيقاف تسلسل الإطارات
            mFrameProvider.stopFrameSequence();
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء إيقاف الفيديو الشبكي", e);
        }
    }
    
    /**
     * بدء تشغيل الصورة المحلية
     */
    private void startLocalPicture() {
        try {
            Log.i(TAG, "بدء تشغيل الصورة المحلية: " + mLocalPicturePath);
            
            if (mLocalPicturePath == null || mLocalPicturePath.isEmpty()) {
                mErrorLogger.logError(TAG, "مسار الصورة المحلية غير محدد");
                return;
            }
            
            // تحميل الصورة
            Bitmap imageBitmap = loadLocalPicture(mLocalPicturePath);
            if (imageBitmap == null) {
                mErrorLogger.logError(TAG, "فشل في تحميل الصورة: " + mLocalPicturePath);
                return;
            }
            
            // تعيين الصورة كإطار ثابت
            mFrameProvider.setStaticFrame(imageBitmap);
            
            Log.i(TAG, "تم بدء تشغيل الصورة المحلية بنجاح");
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء بدء تشغيل الصورة المحلية", e);
        }
    }
    
    /**
     * إيقاف الصورة المحلية
     */
    private void stopLocalPicture() {
        try {
            // تفريغ الإطار الثابت
            mFrameProvider.clearStaticFrame();
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء إيقاف الصورة المحلية", e);
        }
    }
    
    /**
     * تحميل الصورة المحلية
     */
    private Bitmap loadLocalPicture(String path) {
        try {
            File imageFile = new File(path);
            if (!imageFile.exists() || !imageFile.canRead()) {
                return null;
            }
            
            // فك تشفير ملف الصورة إلى Bitmap
            Bitmap originalBitmap = BitmapFactory.decodeFile(path);
            if (originalBitmap == null) {
                return null;
            }
            
            // ضبط حجم الصورة إلى الحجم المطلوب (720p)
            return BitmapUtils.resizeBitmap(originalBitmap, 1280, 720);
        } catch (Exception e) {
            mErrorLogger.logException(TAG, "خطأ أثناء تحميل الصورة المحلية", e);
            return null;
        }
    }
    
    /**
     * الحصول على اسم المصدر
     */
    private String getSourceName(int source) {
        switch (source) {
            case SOURCE_REAL_CAMERA:
                return "الكاميرا الحقيقية";
            case SOURCE_LOCAL_VIDEO:
                return "فيديو محلي";
            case SOURCE_NETWORK_VIDEO:
                return "فيديو شبكي";
            case SOURCE_LOCAL_PICTURE:
                return "صورة محلية";
            default:
                return "غير معروف";
        }
    }
    
    /**
     * فئة موفر إطارات الكاميرا الافتراضية
     */
    private class VirtualCameraFrameProvider {
        private Bitmap mCurrentFrameBitmap;
        private Bitmap mStaticFrameBitmap;
        private Bitmap[] mFrameSequence;
        private int mCurrentFrameIndex;
        private boolean mIsSequencePlaying;
        private final Handler mFrameHandler = new Handler(Looper.getMainLooper());
        private Runnable mFrameRunnable;
        
        /**
         * تهيئة موفر الإطارات
         */
        public void initialize() {
            mCurrentFrameBitmap = null;
            mStaticFrameBitmap = null;
            mFrameSequence = null;
            mCurrentFrameIndex = 0;
            mIsSequencePlaying = false;
        }
        
        /**
         * تعيين الإطار الحالي
         */
        public void setCurrentFrame(Bitmap frame) {
            mCurrentFrameBitmap = frame;
        }
        
        /**
         * تعيين إطار ثابت
         */
        public void setStaticFrame(Bitmap frame) {
            mStaticFrameBitmap = frame;
        }
        
        /**
         * مسح الإطار الثابت
         */
        public void clearStaticFrame() {
            mStaticFrameBitmap = null;
        }
        
        /**
         * بدء تسلسل الإطارات
         */
        public void startFrameSequence(Bitmap[] frames, int frameRate) {
            if (frames == null || frames.length == 0) {
                return;
            }
            
            mFrameSequence = frames;
            mCurrentFrameIndex = 0;
            mIsSequencePlaying = true;
            
            // إذا كان هناك تسلسل إطارات قيد التشغيل، قم بإيقافه أولاً
            if (mFrameRunnable != null) {
                mFrameHandler.removeCallbacks(mFrameRunnable);
            }
            
            // إنشاء مهمة جديدة لتشغيل الإطارات
            mFrameRunnable = new Runnable() {
                @Override
                public void run() {
                    if (!mIsSequencePlaying || mFrameSequence == null || mFrameSequence.length == 0) {
                        return;
                    }
                    
                    // تحديث الإطار الحالي
                    mCurrentFrameBitmap = mFrameSequence[mCurrentFrameIndex];
                    
                    // التقدم إلى الإطار التالي
                    mCurrentFrameIndex = (mCurrentFrameIndex + 1) % mFrameSequence.length;
                    
                    // جدولة الإطار التالي
                    mFrameHandler.postDelayed(this, 1000 / frameRate);
                }
            };
            
            // بدء تشغيل الإطارات
            mFrameHandler.post(mFrameRunnable);
        }
        
        /**
         * إيقاف تسلسل الإطارات
         */
        public void stopFrameSequence() {
            mIsSequencePlaying = false;
            
            if (mFrameRunnable != null) {
                mFrameHandler.removeCallbacks(mFrameRunnable);
                mFrameRunnable = null;
            }
            
            mFrameSequence = null;
            mCurrentFrameIndex = 0;
        }
        
        /**
         * الحصول على الإطار الحالي
         */
        public Bitmap getCurrentFrame() {
            // إذا كان هناك إطار ثابت، استخدمه بدلاً من الإطار الحالي
            if (mStaticFrameBitmap != null) {
                return mStaticFrameBitmap;
            }
            
            // إذا كان هناك تسلسل إطارات قيد التشغيل، استخدم الإطار الحالي
            if (mIsSequencePlaying && mFrameSequence != null && mFrameSequence.length > 0) {
                return mFrameSequence[mCurrentFrameIndex];
            }
            
            // استخدم الإطار الحالي
            return mCurrentFrameBitmap;
        }
    }
}