plugins {
    id("cim.android.application")
}

android {
    namespace = "com.industria.manufactura"
}

dependencies {
    implementation(project(":core-network"))
}
