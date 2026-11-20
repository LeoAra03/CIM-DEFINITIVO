plugins {
    id("cim.android.application")
}

android {
    namespace = "com.industria.coordinacion"
}

dependencies {
    implementation(project(":core-network"))
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)
}
