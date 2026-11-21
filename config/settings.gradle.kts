pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}

rootProject.name = "CIM-DEFINITIVO"

include(":app-coordinador")
project(":app-coordinador").projectDir = file("../android/app-coordinador")
include(":app-plc")
project(":app-plc").projectDir = file("../android/app-plc")
include(":app-manufactura")
project(":app-manufactura").projectDir = file("../android/app-manufactura")
include(":app-calidad")
project(":app-calidad").projectDir = file("../android/app-calidad")
include(":app-almacen")
project(":app-almacen").projectDir = file("../android/app-almacen")
include(":wear-coordinador")
project(":wear-coordinador").projectDir = file("../android/wear-coordinador")
include(":core-network")
project(":core-network").projectDir = file("../android/core-network")

includeBuild("build-logic")
