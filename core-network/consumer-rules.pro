# Consumer ProGuard rules for core-network library
# Prevent stripping of public API used by apps (classes referenced via reflection or from other modules)

-keep class com.sistema.distribuido.network.** {
    *;
}

# Keep Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }
