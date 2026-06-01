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

// Core
include(":core-network")

// Estaciones independientes (NEW v7.0)
include(":app-scorbot:app")
include(":app-vision:app")
include(":app-laser:app")
include(":app-conveyor:app")

// Coordinador y sistemas legados
include(":app-coordinador:app")
include(":app-plc:app")
include(":app-calidad:app")
include(":app-manufactura:app")
include(":app-almacen:app")

// Mapear nombres de proyectos
project(":core-network").name = "core-network"
project(":app-scorbot:app").name = "app-scorbot"
project(":app-vision:app").name = "app-vision"
project(":app-laser:app").name = "app-laser"
project(":app-conveyor:app").name = "app-conveyor"
project(":app-coordinador:app").name = "app-coordinador"
project(":app-plc:app").name = "app-plc"
project(":app-calidad:app").name = "app-calidad"
project(":app-manufactura:app").name = "app-manufactura"
project(":app-almacen:app").name = "app-almacen"
