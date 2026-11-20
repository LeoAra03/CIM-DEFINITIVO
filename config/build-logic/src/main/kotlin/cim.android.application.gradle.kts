import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import com.android.build.gradle.AppExtension
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application")
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

extensions.configure<AppExtension> {
    compileSdkVersion(version("compileSdk").toInt())

    defaultConfig {
        minSdk = version("minSdk").toInt()
        targetSdk = version("targetSdk").toInt()
        versionName = version("appVersion")
        versionCode = version("appVersionCode").toInt()
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
                    project.findProperty("CIM_RELEASE_STORE_FILE") ?: "release.jks",
                )
                storePassword = project.findProperty("CIM_RELEASE_STORE_PASSWORD") as String
                keyAlias = project.findProperty("CIM_RELEASE_KEY_ALIAS") as String
                keyPassword = project.findProperty("CIM_RELEASE_KEY_PASSWORD") as String
            }
        }
    }

    buildTypes {
        getByName("debug").apply {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        getByName("release").apply {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                rootProject.file("proguard-rules.pro").path,
            )
            if (project.hasProperty("CIM_RELEASE_STORE_PASSWORD")) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures.apply {
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
    add("implementation", platform(library("androidx-compose-bom")))
    add("implementation", library("androidx-core-ktx"))
    add("implementation", library("androidx-lifecycle-runtime-ktx"))
    add("implementation", library("androidx-activity-compose"))
    add("implementation", library("androidx-compose-ui"))
    add("implementation", library("androidx-compose-ui-graphics"))
    add("implementation", library("androidx-compose-ui-tooling-preview"))
    add("implementation", library("androidx-compose-material3"))
    add("implementation", library("androidx-compose-material-icons-extended"))
    add("implementation", library("androidx-hilt-navigation-compose"))
    add("implementation", library("hilt-android"))
    add("kapt", library("hilt-compiler"))
    add("testImplementation", library("junit"))
    add("androidTestImplementation", library("androidx-junit"))
    add("androidTestImplementation", library("androidx-espresso-core"))
    add("androidTestImplementation", platform(library("androidx-compose-bom")))
    add("androidTestImplementation", library("androidx-compose-ui-test-junit4"))
    add("debugImplementation", library("androidx-compose-ui-tooling"))
    add("debugImplementation", library("androidx-compose-ui-test-manifest"))
}
