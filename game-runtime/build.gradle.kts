dependencies {
    // Bindet das Core-Fundament ein
    implementation(project(":engine-core"))
}

tasks.jar {
    manifest {
        // Der finale Einstiegspunkt für den Spieler (Standalone)
        attributes["Main-Class"] = "com.game.GameMain"
    }
}

// Der Spezial-Task für deine Export-Pipeline
tasks.register<Copy>("copyExportLibs") {
    // Kopiert alle externen Runtime-Bibliotheken (LWJGL, JOML, Gson)
    from(configurations.runtimeClasspath)

    // Kopiert das kompilierte JAR des Spiels und der Engine selbst
    from(tasks.jar)
    from(project(":engine-core").tasks.jar)

    // Speicherort im Build-Verzeichnis der game-runtime
    into(layout.buildDirectory.dir("export-libs"))
}

// Koppel den Task an den Standard-Build-Prozess
tasks.build {
    dependsOn("copyExportLibs")
}