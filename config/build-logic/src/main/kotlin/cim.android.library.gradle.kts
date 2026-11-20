import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
}

extensions.configure<DetektExtension> {
    config.setFrom(rootProject.file("../detekt.yml"))
    buildUponDefaultConfig = false
    parallel = true
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
fun version(name: String): String = libs.findVersion(name).get().requiredVersion
fun library(name: String) = libs.findLibrary(name).get()

extensions.configure<LibraryExtension> {
    compileSdkVersion(version("compileSdk").toInt())

    defaultConfig {
        minSdk = version("minSdk").toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles(rootProject.file("consumer-rules.pro"))
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        getByName("release").apply {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
            )
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    buildFeatures.apply {
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
    add("implementation", platform(library("androidx-compose-bom")))
    add("implementation", library("androidx-core-ktx"))
    add("implementation", library("androidx-compose-ui"))
    add("implementation", library("androidx-compose-ui-graphics"))
    add("implementation", library("androidx-compose-foundation"))
    add("implementation", library("androidx-compose-material3"))
    add("implementation", library("androidx-compose-material-icons-extended"))
    add("implementation", library("kotlinx-coroutines-android"))
    add("implementation", library("hilt-android"))
    add("kapt", library("hilt-compiler"))
    add("testImplementation", library("junit"))
    add("testImplementation", library("kotlinx-coroutines-test"))
    add("androidTestImplementation", library("androidx-junit"))
    add("androidTestImplementation", library("androidx-espresso-core"))
}
