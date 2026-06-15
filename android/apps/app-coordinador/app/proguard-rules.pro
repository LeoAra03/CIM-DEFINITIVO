# Keep CIM-specific classes
-keep class com.sistema.distribuido.network.** { *; }
-keep class com.industria.coordinacion.** { *; }

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable

# Keep all enum types
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Netty and Reactor - keep integration classes
-keep class reactor.blockhound.** { *; }
-keep class io.netty.** { *; }
-keepclassmembers class io.netty.util.internal.Hidden$NettyBlockHoundIntegration { *; }

# Ktor support
-keep class io.ktor.** { *; }

# Generic configurations
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault
-keep public class * {
    public protected *;
}