# Add project specific ProGuard rules here.

# Hilt / Dagger
-keep public class * extends android.app.Service
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.view.View
-keep class androidx.hilt.work.** { *; }
-keep class * extends androidx.hilt.work.HiltWorker { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class dagger.hilt.android.internal.managers.** { *; }
-keep class dagger.hilt.internal.** { *; }
-keep class com.climasaude.ClimaSaudeApp_HiltComponents** { *; }

# Google Play Services & Auth - CRITICAL FOR GOOGLE LOGIN
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.google.android.gms.common.api.ApiException { *; }
-keep class com.google.android.gms.tasks.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Firebase
-keepattributes SourceFile, LineNumberTable
-keep public class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Retrofit & OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeInvisibleAnnotations, RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault
-dontwarn okhint.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }

# Model classes - Prevent Obfuscation
-keep class com.climasaude.domain.models.** { *; }
-keep class com.climasaude.data.database.entities.** { *; }
-keep @androidx.annotation.Keep class * {*;}
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker
-keep class androidx.work.** { *; }

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}
-dontwarn kotlinx.coroutines.**

# ViewBinding / DataBinding
-keep class com.climasaude.databinding.** { *; }
-keep class androidx.databinding.** { *; }
