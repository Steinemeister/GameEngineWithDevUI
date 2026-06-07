package com;

import imgui.ImGui;
import imgui.app.Application;
import imgui.app.Configuration;

public class Main extends Application {
    @Override
    protected void configure(Configuration config) {
        // Hier konfigurieren Sie das Hauptfenster Ihrer Engine
        config.setTitle("Java Game Engine Development Environment");
        config.setWidth(1280);
        config.setHeight(720);
    }

    @Override
    public void process() {
        // --- 1. Der Haupt-Viewport (Simulierter Spiele-Bildschirm) ---
        ImGui.begin("Spiel-Ansicht (Viewport)");
        ImGui.text("Hier wird später das gerenderte 3D-Spiel angezeigt.");
        ImGui.end();

        // --- 2. Ein Inspector-Panel (Objekt-Eigenschaften) ---
        ImGui.begin("Inspector");
        ImGui.text("Objekt-Eigenschaften");
        ImGui.separator();

        if (ImGui.button("Neues Objekt erstellen")) {
            System.out.println("Button im Editor geklickt!");
        }
        ImGui.end();

        // --- 3. Vorschau für den zukünftigen Shader-Node-Editor ---
        ImGui.begin("Shader Node Editor");
        ImGui.text("Zukünftiger Platz für Ihren Node-Editor.");
        ImGui.text("Tipp: Später nutzen Sie hier 'ImNodes.beginNodeEditor()'.");
        ImGui.end();
    }

    public static void main(String[] args) {
        // Startet die Engine, initialisiert LWJGL/GLFW und öffnet das Fenster
        launch(new Main());
    }
}
