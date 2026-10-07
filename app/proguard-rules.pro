# ProGuard rules for WPMobile Hub

# Keep Room database and DAO classes
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Dao
-dontwarn androidx.room.limits.Limit

# Retrofit 2 rules
-keepattributes Signature, InnerClasses, EnclosingMethod
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclassmembers class * {
    @retrofit2.http.** <methods>;
}

# OkHttp 3 rules
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Moshi rules for JSON serialization
-keep class com.squareup.moshi.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
# Keep all generated JsonAdapters
-keep class *JsonAdapter { *; }
-keep class * { @com.squareup.moshi.JsonQualifier *; }
# Keep @JsonClass annotated classes
-keep @com.squareup.moshi.JsonClass class * { *; }

# Kotlin Coroutines rules
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}

# Data Models and Entities to prevent deserialization failures
-keep class com.example.data.local.** { *; }
-keep class com.example.data.remote.** { *; }
