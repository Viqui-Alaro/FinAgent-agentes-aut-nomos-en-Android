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

rootProject.name = "FinAgent"

// Clean Architecture: cada capa es un módulo Gradle. La regla de dependencias
// la impone el compilador:  :presentation -> :domain <- :data   (y :app las une)
include(":app", ":domain", ":data", ":presentation")
