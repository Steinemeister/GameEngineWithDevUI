val imguiVersion: String by project.extra
val lwjglTarget: String by project.extra

dependencies {
    implementation(project(":engine-core"))
    implementation(project(":game-runtime"))

    // ImGui Java Bindings (Diese drei Zeilen müssen exakt so heißen)
    implementation("io.github.spair:imgui-java-app:$imguiVersion")
    implementation("io.github.spair:imgui-java-binding:$imguiVersion")
    implementation("io.github.spair:imgui-java-lwjgl3:$imguiVersion")

    // Die nativen Bibliotheken
    runtimeOnly("io.github.spair:imgui-java-natives-$lwjglTarget:$imguiVersion")
}

tasks.jar {
    manifest {
        // Der Einstiegspunkt für dich als Entwickler (Editor-Start)
        attributes["Main-Class"] = "com.engine.editor.EditorMain"
    }
}