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

# ---- Room Database & Entities ----
-keep class * extends androidx.room.RoomDatabase
-keep class com.neuroflow.app.data.local.entity.** { *; }
-keep class com.neuroflow.app.data.local.dao.** { *; }
-keep class com.neuroflow.app.data.local.** { *; }
-keep class com.neuroflow.app.domain.model.** { *; }
-dontwarn androidx.room.paging.**

