package com.engine.editor;

import com.engine.core.Main;
import imgui.ImGui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileBrowserPanel implements EditorPanel {
    private final Main engine;

    public FileBrowserPanel(Main engine) {
        this.engine = engine;
    }

    @Override
    public void updateAndRender() {
        ImGui.begin("Projekt-Dateien (Asset Browser)");

        // Holt sich den Pfad des aktuell geladenen Projekts aus der Main-Klasse
        String currentProjectPath = engine.getCurrentProjectPath() + "/assets/shaders";
        ImGui.text("Ordner: " + currentProjectPath);
        ImGui.separator();

        File folder = new File(currentProjectPath);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile() && file.getName().endsWith(".glsl")) {
                    if (ImGui.selectable("[=] " + file.getName())) {
                        loadShaderFile(file.getAbsolutePath());
                    }
                }
            }
        }
        ImGui.end();
    }

    private void loadShaderFile(String absolutePath) {
        try {
            String content = Files.readString(Paths.get(absolutePath));
            engine.getFragmentShaderInput().set(content);
            engine.compileShader(content);
            // Wir merken uns im Editor, welche Datei gerade aktiv editiert wird!
            engine.getShaderEditorPanel().setCurrentEditPath(absolutePath);
        } catch (IOException e) {
            System.err.println("Fehler beim Laden der Datei: " + e.getMessage());
        }
    }
}
