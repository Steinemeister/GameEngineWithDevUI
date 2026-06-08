package com.engine.development;

import com.Main;
import com.engine.rendering.Shader;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.type.ImString;

public class ShaderEditorPanel implements EditorPanel {
    private final Main engine;
    private final ImString shaderInput;

    public ShaderEditorPanel(Main engine, ImString shaderInput) {
        this.engine = engine;
        this.shaderInput = shaderInput;
    }

    @Override
    public void updateAndRender() {
        ImGui.begin("Live GLSL Fragment Shader Editor");
        ImGui.text("Modifiziere den GLSL-Code und klicke auf 'Shader kompilieren'.");
        ImGui.separator();

        ImVec2 inputSize = new ImVec2(-1, 350);
        ImGui.inputTextMultiline("##ShaderCode", shaderInput, (int) inputSize.x, (int) inputSize.y);

        ImGui.separator();

        if (ImGui.button("Shader kompilieren", -1, 40)) {
            engine.compileShader(shaderInput.get());
        }

        ImGui.separator();

        Shader shader = engine.getCustomShader();
        if (shader.hasErrors()) {
            ImGui.textColored(1.0f, 0.3f, 0.3f, 1.0f, "Kompilierungsfehler detektiert:");
            ImGui.textWrapped(shader.getErrorMessage());
        } else {
            ImGui.textColored(0.3f, 1.0f, 0.3f, 1.0f, "Shader Status: Bereit und aktiv auf der GPU.");
        }

        ImGui.end();
    }
}
