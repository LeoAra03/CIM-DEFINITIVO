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

include(":app-coordinador:app")
include(":app-plc:app")
include(":app-calidad:app")
include(":app-manufactura:app")
include(":app-almacen:app")
include(":core-network")

// Mapear nombres de proyectos para evitar confusión
project(":app-coordinador:app").name = "app-coordinador"
project(":app-plc:app").name = "app-plc"
project(":app-calidad:app").name = "app-calidad"
project(":app-manufactura:app").name = "app-manufactura"
project(":app-almacen:app").name = "app-almacen"
