import org.gradle.internal.os.OperatingSystem

plugins {
    java
    application
}

repositories {
    mavenCentral()
}

group = "com.myengine"
version = "1.0-SNAPSHOT"

// 1. Betriebssystem für die nativen Bibliotheken ermitteln
val osName = OperatingSystem.current()
val lwjglPlatform = when {
    osName.isWindows -> "natives-windows"
    osName.isMacOsX -> {
        // Unterscheidung zwischen Intel (x64) und Apple Silicon (arm64)
        if (System.getProperty("os.arch").contains("aarch64")) "natives-macos-arm64" else "natives-macos"
    }
    osName.isLinux -> {
        if (System.getProperty("os.arch").contains("aarch64")) "natives-linux-arm64" else "natives-linux"
    }
    else -> throw GradleException("Unsupported operating system")
}

// 2. Versionen der Bibliotheken definieren
val lwjglVersion = "3.3.6"
val jomlVersion = "1.10.8"
val imguiVersion = "1.87.3"

dependencies {
    // --- JOML (Mathematik für 3D) ---
    implementation("org.joml:joml:$jomlVersion")

    // --- LWJGL Core & Standard-Bindings ---
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-glfw")
    implementation("org.lwjgl:lwjgl-opengl")

    // --- LWJGL Native Binaries (automatisch zugewiesen) ---
    runtimeOnly("org.lwjgl:lwjgl::$lwjglPlatform")
    runtimeOnly("org.lwjgl:lwjgl-glfw::$lwjglPlatform")
    runtimeOnly("org.lwjgl:lwjgl-opengl::$lwjglPlatform")

    // --- Dear ImGui (Java Bindings) ---
    implementation("io.github.spair:imgui-java-app:${imguiVersion}")
}

configure<JavaApplication> {
    mainClass.set("com.Main")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}