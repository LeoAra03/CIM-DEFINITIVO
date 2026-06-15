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
include(":android:core-network")

// Apps principales del sistema
include(":android:apps:app-coordinador:app")
include(":android:apps:app-plc:app")
include(":android:apps:app-calidad:app")
include(":android:apps:app-manufactura:app")
include(":android:apps:app-almacen:app")

// Mapear nombres de proyectos
project(":android:core-network").name = "core-network"
project(":android:apps:app-coordinador:app").name = "app-coordinador"
project(":android:apps:app-plc:app").name = "app-plc"
project(":android:apps:app-calidad:app").name = "app-calidad"
project(":android:apps:app-manufactura:app").name = "app-manufactura"
project(":android:apps:app-almacen:app").name = "app-almacen"
