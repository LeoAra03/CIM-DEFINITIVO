plugins {
    id("cim.android.application")
}

android {
    namespace = "com.industria.wear"

    defaultConfig {
        minSdk = 30
    }
}

dependencies {
    implementation(project(":core-network"))
    implementation(libs.androidx.wear.compose.material)
    implementation(libs.androidx.wear.compose.foundation)
}
