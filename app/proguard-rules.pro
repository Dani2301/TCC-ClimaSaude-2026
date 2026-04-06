# Add project specific ProGuard rules here.

# Hilt rules - Comprehensive
-keep public class * extends android.app.Service
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.view.View
-keep class androidx.hilt.work.** { *; }
-keep class * extends androidx.hilt.work.HiltWorker { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Dagger / Hilt internals
-keep class dagger.hilt.android.internal.managers.** { *; }
-keep class dagger.hilt.internal.** { *; }
-keep class com.climasaude.ClimaSaudeApp_HiltComponents** { *; }
-keep class com.climasaude.databinding.** { *; }

# Room rules
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

# Firebase
-keepattributes SourceFile, LineNumberTable
-keep public class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Model classes - Garantir que modelos não sejam ofuscados. Modificado por: Daniel
-keep class com.climasaude.domain.models.** { *; }
-keep class com.climasaude.data.database.entities.** { *; }
-keep @androidx.annotation.Keep class * {*;}
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public class * extends com.bumptech.glide.GeneratedAppGlideModule { *; }
-keep public class com.bumptech.glide.GeneratedAppGlideModuleImpl { *; }
-dontwarn com.bumptech.glide.**

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

# Mantém os nomes dos ViewModels para o Hilt
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Kotlin Reflection (needed by some libraries)
-keep class kotlin.reflect.jvm.internal.** { *; }
-dontwarn kotlin.reflect.jvm.internal.**
