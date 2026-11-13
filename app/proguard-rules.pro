# =============================================================================
# Status Saver – R8 / ProGuard rules
# Most libraries (Hilt, Room, Media3, Coil 3, AdMob, UMP, WorkManager, Glance)
# ship consumer rules inside their AARs. Only app-specific rules live here.
# =============================================================================

# Keep line numbers for readable Play Console crash stack traces.
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# ---- kotlinx.serialization (type-safe Navigation routes & JSON) ----
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.msbytes.statussaver.**$$serializer { *; }
-keepclassmembers class com.msbytes.statussaver.** {
    *** Companion;
}
-keepclasseswithmembers class com.msbytes.statussaver.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepnames @kotlinx.serialization.Serializable class com.msbytes.statussaver.**

# ---- Room entities (field names map to columns) ----
-keep class com.msbytes.statussaver.data.local.db.** { *; }

# ---- WorkManager workers instantiated by reflection via HiltWorkerFactory ----
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ---- Glance widget receiver ----
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver

# ---- Media3: keep reflective extension lookups quiet ----
-dontwarn androidx.media3.**

# ---- Google Mobile Ads / UMP ----
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.**
-keep class com.google.android.ump.** { *; }

# ---- Coroutines ----
-dontwarn kotlinx.coroutines.debug.**

# ---- Misc. optional annotations referenced by libraries ----
-dontwarn org.jetbrains.annotations.**
-dontwarn javax.annotation.**
-dontwarn com.google.errorprone.annotations.**
