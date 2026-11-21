# Consumer rules for the core-network library.

# Keep the public communication API because apps reference it through the
# shared module boundary.
-keep class com.industria.prefecto.** { *; }

# Keep Kotlin metadata for reflection.
-keep class kotlin.Metadata { *; }
