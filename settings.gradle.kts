import org.gradle.kotlin.dsl.maven

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")

            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id ("org.jetbrains.kotlin.android") version "2.1.20" apply false
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven {
          name = "TarsosDSP repository"
          url = uri("https://mvn.0110.be/releases")
        }
    }
}

rootProject.name = "NepTune"
include(":app")
 