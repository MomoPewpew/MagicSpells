rootProject.name = "MagicSpellsParent"

val localGradleProperties = java.util.Properties().also { properties ->
    val localGradlePropertiesFile = file(".gradle/gradle.properties")
    if (localGradlePropertiesFile.isFile) {
        localGradlePropertiesFile.inputStream().use { properties.load(it) }
    }
}

// Expose on each Project for findProperty() (signing.*, preflight in build.gradle).
gradle.beforeProject {
    localGradleProperties.forEach { key, value ->
        val name = key.toString()
        if (findProperty(name) == null) {
            extensions.extraProperties.set(name, value.toString())
        }
    }
}

include("core")
include("factions")
include("memory")
include("shop")
include("teams")
include("towny")

include(":nms:shared")
include(":nms:v1_21_4")

startParameter.isParallelProjectExecutionEnabled = true

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://papermc.io/repo/repository/maven-public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}