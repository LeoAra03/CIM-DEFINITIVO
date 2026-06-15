# ProGuard rules for core-network module
# Keep commonly used Kotlin and reflection metadata
-keepclassmembers class ** {
    @kotlin.Metadata *;
}

# Keep public API
-keep public class com.sistema.distribuido.network.** {
    public *;
}
