plugins {
    id("cim.android.application")
}

android {
    namespace = "com.industria.plc"
}

dependencies {
    implementation(project(":core-network"))
}
