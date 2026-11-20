import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
}

extensions.configure<LibraryExtension> {
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles(rootProject.file("consumer-rules.pro"))
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
            )
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    buildFeatures {
        compose = true
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
    add("implementation", libs.androidx.compose.ui)
    add("implementation", libs.androidx.compose.ui.graphics)
    add("implementation", libs.androidx.compose.foundation)
    add("implementation", libs.androidx.compose.material3)
    add("implementation", libs.androidx.compose.material.icons.extended)
    add("implementation", libs.kotlinx.coroutines.android)
    add("implementation", libs.hilt.android)
    add("kapt", libs.hilt.compiler)
    add("testImplementation", libs.junit)
    add("testImplementation", libs.kotlinx.coroutines.test)
    add("androidTestImplementation", libs.androidx.junit)
    add("androidTestImplementation", libs.androidx.espresso.core)
}
