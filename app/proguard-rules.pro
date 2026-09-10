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

# Keep SQLDroid classes and its JDBC driver to prevent connection failure
-keep class org.sqldroid.** { *; }
-dontwarn org.sqldroid.**

# Keep WebView Javascript Interface methods
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep Volley classes and network clients to prevent serialization/reflection failures
-keep class com.android.volley.** { *; }
-dontwarn com.android.volley.**

# Crashlytics
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# Keep data models used for JSON reflection and API parsing
-keep class com.app.fourscontracting.data.** { *; }
-keepclassmembers class com.app.fourscontracting.data.** { *; }

# Keep Glide
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl { *; }
-dontwarn com.bumptech.glide.**