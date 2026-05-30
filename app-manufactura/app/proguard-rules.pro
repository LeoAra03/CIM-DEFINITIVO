# Keep CIM-specific classes
-keep class com.sistema.distribuido.network.** { *; }
-keep class com.industria.manufactura.** { *; }

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable

# Keep all enum types
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Generic configurations
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault
-keep public class * {
    public protected *;
}
