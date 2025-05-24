enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  repositories {
    gradlePluginPortal()
  }
  plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.cadixdev.licenser") version "0.6.1"
    id("net.kyori.indra") version "3.1.3"
    id("net.kyori.indra.git") version "3.1.3"
    id("net.kyori.indra.publishing") version "3.1.3"
    id("net.kyori.blossom") version "2.1.0"
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
  // Damit Subprojekt-eigene `repositories {}` ignoriert werden und nur hier definiert wird:
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

  @Suppress("UnstableApiUsage")
  repositories {
    // Für MiniMessage, Adventure, PistonUtils, bStats, etc.
    mavenCentral()
    // Maven-Snapshots (falls du sie verwendest)
    maven("https://oss.sonatype.org/content/repositories/snapshots") {
      name = "Sonatype"
    }
    // Paper/Paper-API
    maven("https://papermc.io/repo/repository/maven-public/") {
      name = "PaperMC"
    }
    // Velocity-API
    maven("https://nexus.velocitypowered.com/repository/maven-public/") {
      name = "VelocityPowered"
    }
    // PistonUtils, PistonMOTD, etc.
    maven("https://repo.codemc.org/repository/maven-public") {
      name = "CodeMC"
    }
    // Dmulloy2 (ProtocolLib, etc.)
    maven("https://repo.dmulloy2.net/nexus/repository/public") {
      name = "dmulloy2"
    }
    // PlaceholderAPI
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
      name = "PlaceholderAPI"
    }
    // Spigot-API Snapshots (falls du sie brauchst)
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
      name = "SpigotMC"
    }
  }
}

rootProject.name = "PistonQueue"

setOf(
  "build-data",
  "placeholder",
  "shared",
  "bukkit",
  "velocity",
  "universal"
).forEach { setupPQSubproject(it) }

fun setupPQSubproject(name: String) {
  setupSubproject("pistonqueue-$name") {
    projectDir = file(name)
  }
}

inline fun setupSubproject(name: String, block: ProjectDescriptor.() -> Unit) {
  include(name)
  project(":$name").apply(block)
}
