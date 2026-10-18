# ProGuard / R8 rules for Motion Studio v3.0 Release Build

# Keep MainActivity & ViewModel entry points
-keep class com.motionphoto.converter.MainActivity { *; }
-keep class com.motionphoto.converter.ui.MainViewModel { *; }

# Keep Adobe XMP Core classes & internal parsers
-keep class com.adobe.xmp.** { *; }
-keep class com.adobe.internal.xmp.** { *; }
-dontwarn com.adobe.xmp.**
-dontwarn com.adobe.internal.xmp.**

# Keep Android Media & ExifInterface classes
-keep class android.media.** { *; }
-keep class androidx.exifinterface.** { *; }

# Keep Media3 ExoPlayer constructors and interfaces
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep application core models & enums
-keep class com.motionphoto.converter.core.** { *; }

# Preserve source file and line numbers for crash diagnostics
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
