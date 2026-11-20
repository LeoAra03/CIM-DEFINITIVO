# Shared R8 rules for all CIM application modules.

# Keep line numbers for readable crash reports.
-keepattributes SourceFile,LineNumberTable

# Keep enum members used by serialization and reflection.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable creators.
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep generic signature and runtime annotations.
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault
