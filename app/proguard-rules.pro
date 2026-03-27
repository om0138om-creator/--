# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Keep Parcelable classes
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# Keep data classes
-keep class com.omarsanjaq.shareapp.FileItem { *; }
-keep class com.omarsanjaq.shareapp.DeviceItem { *; }

# Bluetooth
-keep class android.bluetooth.** { *; }

# WiFi Direct
-keep class android.net.wifi.p2p.** { *; }

# Material Components
-keep class com.google.android.material.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
