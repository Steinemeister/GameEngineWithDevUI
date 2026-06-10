package com.engine.editor;

import com.engine.core.Main;
import imgui.ImGui;
import imgui.ImVec2;

public class ViewportPanel implements EditorPanel {
    private final Main engine; // Referenz auf die Hauptklasse, um an die Textur-ID zu kommen

    public ViewportPanel(Main engine) {
        this.engine = engine;
    }

    @Override
    public void updateAndRender() {
        ImGui.begin("Viewport");

        ImVec2 windowSize = ImGui.getContentRegionAvail();
        // Wir rufen die Größenänderung in der Hauptklasse auf
        engine.handleViewportResize((int) windowSize.x, (int) windowSize.y);

        // Textur aus der Engine im ImGui-Fenster anzeigen
        ImGui.image(engine.getTextureId(), engine.getViewportWidth(), engine.getViewportHeight(), 0, 1, 1, 0);

        ImGui.end();
    }
}
