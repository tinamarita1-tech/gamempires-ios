# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keep class retrofit2.Response { *; }
-keep interface retrofit2.Call { *; }
-keep interface retrofit2.Callback { *; }

# Moshi
-keepattributes *Annotation*
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# DTO and Model classes
-keep class com.gamempires.messenger.data.nextcloud.dto.** { *; }
-keepclassmembers class com.gamempires.messenger.data.nextcloud.dto.** { *; }
-keep class com.gamempires.messenger.model.** { *; }
-keepclassmembers class com.gamempires.messenger.model.** { *; }

# Hilt / Dagger
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ---- Added for the full-app release build (whole app is now shrunk by R8) ----

# Moshi reflection (KotlinJsonAdapterFactory) reads Kotlin metadata at runtime
-keep class kotlin.Metadata { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}

# WebRTC: native code calls back into these Java classes by name (JNI)
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# App call engine implements WebRTC observer interfaces called from native code
-keep class com.gamempires.messenger.calls.** { *; }

# Retrofit suspend functions
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep interface com.gamempires.messenger.data.nextcloud.api.** { *; }

# Enums used in JSON / DataStore
-keepclassmembers enum com.gamempires.messenger.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep line numbers for readable crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Push notifications (Firebase ships its own consumer rules; keep our push classes readable in crash reports)
-keep class com.gamempires.messenger.push.** { *; }

# WebRTC's native library looks these classes up by name when it loads (JniZero). Without them the app
# aborts with "JNI DETECTED ERROR ... java_class == null" the moment a call starts (release builds only).
-keep class org.jni_zero.** { *; }
-dontwarn org.jni_zero.**
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
-keepclasseswithmembers class * {
    @org.jni_zero.CalledByNative <methods>;
}
-keepclasseswithmembers class * {
    @org.jni_zero.AccessedByNative <fields>;
}
