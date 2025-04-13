# تطبيق VCamera الجديد

## نظرة عامة

تطبيق VCamera هو تطبيق بيئة افتراضية متقدم يسمح للمستخدمين بتثبيت وتشغيل تطبيقات متعددة داخل بيئة معزولة. الميزة الرئيسية للتطبيق هي القدرة على استبدال الكاميرا الحقيقية بمصادر بديلة مثل الفيديو المحلي أو الصور الثابتة، مما يوفر خيارات أكثر للخصوصية أثناء مكالمات الفيديو.

## الميزات الرئيسية

- **بيئة افتراضية متكاملة**: تشغيل العديد من التطبيقات داخل البيئة الافتراضية.
- **الكاميرا الافتراضية**: استبدال الكاميرا الحقيقية بمصادر بديلة:
  - الكاميرا الحقيقية (الافتراضي)
  - فيديو محلي
  - بث فيديو من الإنترنت
  - صورة محلية
- **الموقع الوهمي**: تحديد مواقع مخصصة للتطبيقات.
- **دعم Google Framework**: تثبيت خدمات Google حسب الحاجة.
- **تعدد المستخدمين**: دعم متعدد المستخدمين لاستخدام حسابات متعددة.

## حل مشكلة "Launched Failed"

تم تطوير النسخة الجديدة من VCamera خصيصًا لحل مشكلة "Launched Failed" التي كانت تظهر في الإصدارات السابقة. تم إعادة تصميم آلية إطلاق التطبيقات بالكامل مع توفير دعم محسن لإصدارات Android الحديثة (10 وما فوق).

تفاصيل الحل موجودة في ملف [LaunchFailedSolutionDetails.md](/docs/LaunchFailedSolutionDetails.md).

## الهيكل

الهيكل العام للتطبيق:

```
com.vcamera.app/
├── VCameraApplication.java        # تطبيق Android الرئيسي
├── core/                          # المكونات الأساسية
│   ├── AppManager.java            # إدارة التطبيقات
│   ├── CameraManager.java         # إدارة الكاميرا الافتراضية
│   ├── ErrorLogger.java           # تسجيل الأخطاء
│   ├── LocationManager.java       # إدارة الموقع الوهمي
│   └── PreferenceManager.java     # إدارة الإعدادات
├── ui/                            # واجهة المستخدم
│   ├── MainActivity.java          # النشاط الرئيسي
│   ├── AppAdapter.java            # محول قائمة التطبيقات
│   ├── AppManagerActivity.java    # نشاط إدارة التطبيقات
│   ├── CameraSettingsActivity.java # إعدادات الكاميرا
│   ├── LocationSettingsActivity.java # إعدادات الموقع
│   └── AdvancedFeaturesActivity.java # الميزات المتقدمة
└── virtual/                       # البيئة الافتراضية
    ├── AppLauncher.java           # مطلق التطبيقات المحسن
    ├── GoogleFrameworkManager.java # إدارة إطار عمل Google
    └── VirtualEnvironment.java    # البيئة الافتراضية
```

## بناء التطبيق

### المتطلبات

- Android Studio 4.2+
- Java 8+
- Android SDK 30+
- Gradle 7.0+

### خطوات البناء

1. استنساخ المستودع:
   ```bash
   git clone https://github.com/your-repo/vcamera-app.git
   ```

2. فتح المشروع في Android Studio

3. مزامنة Gradle:
   ```bash
   ./gradlew build
   ```

4. بناء APK:
   ```bash
   ./gradlew assembleDebug   # للإصدار التجريبي
   ./gradlew assembleRelease # للإصدار النهائي
   ```

## التثبيت

راجع [دليل التثبيت](/docs/InstallationGuide.md) للحصول على تعليمات تفصيلية حول كيفية التثبيت والإعداد.

## المساهمة

نرحب بالمساهمين! إذا كنت ترغب في المساهمة في تطوير VCamera:

1. Fork المستودع
2. إنشاء فرع للميزة الجديدة
3. إرسال طلب سحب مع وصف مفصل للتغييرات

## الترخيص

هذا المشروع مرخص بموجب رخصة Apache License 2.0.