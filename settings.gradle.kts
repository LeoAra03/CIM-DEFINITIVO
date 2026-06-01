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

// Módulos de soporte de manufactura
// support-scorbot, support-vision y support-laser son soporte para
// manufactura y no se entregan como APKs independientes.
include(":support-scorbot:app")
include(":support-vision:app")
include(":support-laser:app")
include(":support-conveyor:app")

// Coordinador y sistemas legados
include(":app-coordinador:app")
include(":app-plc:app")
include(":app-calidad:app")
include(":app-manufactura:app")
include(":app-almacen:app")

// Mapear nombres de proyectos
project(":core-network").name = "core-network"
project(":support-scorbot:app").name = "support-scorbot"
project(":support-vision:app").name = "support-vision"
project(":support-laser:app").name = "support-laser"
project(":support-conveyor:app").name = "support-conveyor"
project(":app-coordinador:app").name = "app-coordinador"
project(":app-plc:app").name = "app-plc"
project(":app-calidad:app").name = "app-calidad"
project(":app-manufactura:app").name = "app-manufactura"
project(":app-almacen:app").name = "app-almacen"
