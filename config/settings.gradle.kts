pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Practica_2"

// Core activo
include(":core-network")
project(":core-network").projectDir = file("../android/core-network")

// Apps principales del sistema
include(":app-coordinador")
project(":app-coordinador").projectDir = file("../android/apps/app-coordinador/app")

include(":app-plc")
project(":app-plc").projectDir = file("../android/apps/app-plc/app")

include(":app-calidad")
project(":app-calidad").projectDir = file("../android/apps/app-calidad/app")

include(":app-manufactura")
project(":app-manufactura").projectDir = file("../android/apps/app-manufactura/app")

include(":app-almacen")
project(":app-almacen").projectDir = file("../android/apps/app-almacen/app")
