plugins {
    id("cim.android.library")
}

android {
    namespace = "com.industria.prefecto"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.conscrypt.android)
    implementation(libs.mlkit.barcode.scanning)
    api(libs.opencv)
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)
}
