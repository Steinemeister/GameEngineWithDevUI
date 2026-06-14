plugins {
    java
}

// Zentrale Versionsverwaltung
val lwjglVersion = "3.3.4"
val jomlVersion = "1.10.8"
val gsonVersion = "2.11.0"
val imguiVersion = "1.89.0"

// Automatische Betriebssystem-Erkennung für LWJGL-Natives
val osName = System.getProperty("os.name").lowercase()
val lwjglTarget = when {
    osName.contains("win") -> "windows"
    osName.contains("mac") -> "macos"
    else -> "linux"
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    // Variablen an die Subprojekte weitergeben
    extra["lwjglVersion"] = lwjglVersion
    extra["lwjglTarget"] = lwjglTarget
    extra["jomlVersion"] = jomlVersion
    extra["gsonVersion"] = gsonVersion
    extra["imguiVersion"] = imguiVersion
}