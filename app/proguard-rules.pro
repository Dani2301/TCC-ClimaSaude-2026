# Regras específicas para evitar crash em aparelhos Samsung/Android 14

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

# Retrofit 2
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations, RuntimeInvisibleParameterAnnotations
-keep class retrofit2.** { *; }
-keep @retrofit2.http.** class * { *; }
-dontwarn retrofit2.**

# OkHttp3
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson
-keep class com.google.gson.** { *; }
-keep class com.climasaude.data.remote.responses.** { *; }
-keep class com.climasaude.domain.models.** { *; }

# Google Play Services & Auth
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

# WorkManager (Crítico para Android 14)
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker
-keep class androidx.work.** { *; }
-keep class com.climasaude.data.receivers.** { *; }

# Prevenir ofuscação de entidades de banco e modelos
-keep class com.climasaude.data.database.entities.** { *; }
-keep @androidx.annotation.Keep class * {*;}

# Glide
-keep class com.github.bumptech.glide.** { *; }

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**
