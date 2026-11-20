plugins {
    id("cim.android.application")
}

android {
    namespace = "com.industria.almacenamiento"
}

dependencies {
    implementation(project(":core-network"))
}
