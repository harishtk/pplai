# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Model classes
-keep class com.aiavatar.app**model.**{ *; }
# Keep Dto classes
-keep class com.aiavatar.app**dto.**{ *; }

# Firebase
# -keep class com.google.firebase.** { *; }

# Retrofit
-dontwarn retrofit.**
-keep class retrofit.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# OkHttp3
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class sun.misc.Unsafe { *; }
-dontwarn java.nio.file.*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Gson
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }

# CircularProgressButton
-keepclassmembers class com.dd.StrokeGradientDrawable {
    public void setStrokeColor(int);
}

# Code obfuscation
-keepclassmembers class com.aiavatar.app.view.model_class.** { <fields>; }
-keep class com.aiavatar.app.view.model_class.** { *; }

# For logs
-assumenosideeffects class android.util.Log {
   public static boolean isLoggable(java.lang.String, int);
    # public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# From https://github.com/lolevsky/Android-Clean-Architecture/blob/master/presentation/proguard-rules.pro

-keepclassmembers class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator CREATOR;
}

# Adjust
-keep class com.adjust.sdk.** { *; }
-keep class com.google.android.gms.common.ConnectionResult {
    int SUCCESS;
}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient {
    com.google.android.gms.ads.identifier.AdvertisingIdClient$Info getAdvertisingIdInfo(android.content.Context);
}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info {
    java.lang.String getId();
    boolean isLimitAdTrackingEnabled();
}
-keep public class com.android.installreferrer.** { *; }
# END Adjust

