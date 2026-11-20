import com.android.build.gradle.AppExtension
import org.gradle.api.JavaVersion
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
}

extensions.configure<AppExtension> {
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionName = libs.versions.appVersion.get()
        versionCode = libs.versions.appVersionCode.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        if (project.hasProperty("CIM_RELEASE_STORE_PASSWORD")) {
            create("release") {
                storeFile = rootProject.file(
                    project.findProperty("CIM_RELEASE_STORE_FILE") ?: "release.jks"
                )
                storePassword = project.findProperty("CIM_RELEASE_STORE_PASSWORD") as String
                keyAlias = project.findProperty("CIM_RELEASE_KEY_ALIAS") as String
                keyPassword = project.findProperty("CIM_RELEASE_KEY_PASSWORD") as String
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                rootProject.file("../config/proguard-rules.pro").path,
            )
            if (project.hasProperty("CIM_RELEASE_STORE_PASSWORD")) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

tasks.withType<Test>().configureEach {
    useJUnit()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    add("implementation", platform(libs.androidx.compose.bom))
    add("implementation", libs.androidx.core.ktx)
    add("implementation", libs.androidx.lifecycle.runtime.ktx)
    add("implementation", libs.androidx.activity.compose)
    add("implementation", libs.androidx.compose.ui)
    add("implementation", libs.androidx.compose.ui.graphics)
    add("implementation", libs.androidx.compose.ui.tooling.preview)
    add("implementation", libs.androidx.compose.material3)
    add("implementation", libs.androidx.compose.material.icons.extended)
    add("implementation", libs.androidx.hilt.navigation.compose)
    add("implementation", libs.hilt.android)
    add("kapt", libs.hilt.compiler)
    add("testImplementation", libs.junit)
    add("androidTestImplementation", libs.androidx.junit)
    add("androidTestImplementation", libs.androidx.espresso.core)
    add("androidTestImplementation", platform(libs.androidx.compose.bom))
    add("androidTestImplementation", libs.androidx.compose.ui.test.junit4)
    add("debugImplementation", libs.androidx.compose.ui.tooling)
    add("debugImplementation", libs.androidx.compose.ui.test.manifest)
}
