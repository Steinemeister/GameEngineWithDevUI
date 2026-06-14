package com.engine.editor;

import com.engine.core.Main;
import com.engine.core.Shader;
import com.engine.scene.GameObject;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.type.ImString;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ShaderEditorPanel implements EditorPanel {
    private final Main engine;
    private final ImString shaderInput;
    private String currentEditPath = null; // Speichert den Pfad der aktuell geöffneten Datei

    public ShaderEditorPanel(Main engine, ImString shaderInput) {
        this.engine = engine;
        this.shaderInput = shaderInput;
    }

    public void setCurrentEditPath(String path) {
        this.currentEditPath = path;
    }

    @Override
    public void updateAndRender() {
        ImGui.begin("Live GLSL Fragment Shader Editor");

        if (currentEditPath == null) {
            ImGui.textDisabled("Waehlen Sie eine Datei im Asset Browser aus, um sie zu editieren.");
        } else {
            ImGui.text("Editierte Datei: " + currentEditPath);
            ImGui.sameLine();
            if (ImGui.button("💾 Speichern", 100, 23)) {
                saveShaderToDisk();
            }
        }

        ImGui.separator();

        ImVec2 inputSize = new ImVec2(-1, 300);
        ImGui.inputTextMultiline("##ShaderCode", shaderInput, (int) inputSize.x, (int) inputSize.y);

        ImGui.separator();

        if (ImGui.button("⚡ Shader testen (GPU)", -1, 35)) {
            engine.compileShader(shaderInput.get());
        }

        ImGui.separator();

        GameObject selectedObj = engine.getSelectedObject();

        if (selectedObj != null && selectedObj.material != null && selectedObj.material.getShaderProgram() != null) {
            Shader shader = selectedObj.material.getShaderProgram();

            if (shader.hasErrors()) {
                ImGui.textColored(1.0f, 0.3f, 0.3f, 1.0f, "Kompilierungsfehler detektiert:");
                ImGui.textWrapped(shader.getErrorMessage());
            } else {
                ImGui.textColored(0.3f, 1.0f, 0.3f, 1.0f, "Shader Status: Bereit und aktiv auf der GPU.");
            }
        } else {
            ImGui.textDisabled("Waehlen Sie ein Objekt im Viewport aus, um dessen Shader-Status zu sehen.");
        }

        ImGui.end();
    }

    private void saveShaderToDisk() {
        if (currentEditPath == null) return;
        try {
            Files.writeString(Paths.get(currentEditPath), shaderInput.get());
            System.out.println("Shader erfolgreich ueberschrieben!");
        } catch (IOException e) {
            System.err.println("Fehler beim Schreiben der Datei: " + e.getMessage());
        }
    }
}
