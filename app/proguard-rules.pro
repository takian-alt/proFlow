# Add project specific ProGuard rules here.

# ---- Firebase / Google Play Services ----
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Keep data classes used for Firestore deserialization
-keepclassmembers class com.neuroflow.app.data.sync.** {
    *;
}

# ---- Room v22 Database, DAOs & Entities ----
-keep class * extends androidx.room.RoomDatabase
-keep class com.neuroflow.app.data.local.entity.** { *; }
-keep class com.neuroflow.app.data.local.dao.** { *; }
-keep class com.neuroflow.app.data.local.** { *; }
-keep class com.neuroflow.app.domain.model.** { *; }
-keep class androidx.room.migration.Migration { *; }
-keepclassmembers class * extends androidx.room.migration.Migration { *; }
-keepclassmembers class com.neuroflow.app.data.local.AppDatabaseKt { *; }
-keepclassmembers class com.neuroflow.app.data.local.Converters { *; }
-dontwarn androidx.room.paging.**

# ---- Kotlin Coroutines & Flow ----
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}
-keep class kotlinx.coroutines.flow.** { *; }
-dontwarn kotlinx.coroutines.**

# ---- Foreground Service, WorkManager & Background Workers ----
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.neuroflow.app.worker.** { *; }
-keep class com.neuroflow.app.service.** { *; }

# ---- Hilt / Dagger Injection ----
-keep class com.neuroflow.app.di.** { *; }
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }

# ---- Modern Domain Services & ADR-002 Contracts ----
-keep class com.neuroflow.app.domain.service.** { *; }
-keep class com.neuroflow.app.data.calendar.** { *; }
