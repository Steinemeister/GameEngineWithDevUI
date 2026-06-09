package com.engine.development.shaderNodeEditor;

import imgui.ImGui;
import imgui.ImVec2;

public class NodeSpawnMenu {
    private boolean visible = false;
    private final ImVec2 position = new ImVec2();
    private float gridX = 0.0f;
    private float gridY = 0.0f;

    public void open(float screenX, float screenY, float gridX, float gridY) {
        this.visible = true;
        this.position.x = screenX;
        this.position.y = screenY;
        this.gridX = gridX;
        this.gridY = gridY;
    }

    public void close() {
        this.visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    public void updateAndRender(NodeEditorPanel panel) {
        if (!visible) return;

        ImGui.setNextWindowPos(position.x, position.y, imgui.flag.ImGuiCond.Appearing);
        int windowFlags = imgui.flag.ImGuiWindowFlags.NoResize | imgui.flag.ImGuiWindowFlags.NoCollapse | imgui.flag.ImGuiWindowFlags.AlwaysAutoResize;

        if (ImGui.begin("Knoten erstellen", windowFlags)) {
            // Absturzsicheres Schließen bei Klick ins Leere
            boolean mouseClicked = ImGui.isMouseClicked(0) || ImGui.isMouseClicked(1);
            boolean isHovered = ImGui.isWindowHovered(imgui.flag.ImGuiHoveredFlags.ChildWindows);

            if (!isHovered && mouseClicked && !ImGui.isWindowAppearing()) {
                close();
            }

            ImGui.textDisabled("Verfuegbare Knoten:");
            ImGui.separator();

            // Wir loopen dynamisch durch alle registrierten Einträge der Registry!
            for (NodeRegistryEntry entry : NodeRegistry.getEntries()) {
                if (ImGui.button(entry.name(), 150, 25)) {
                    // Wir rufen eine neue Methode im Hauptpanel auf, um den Knoten zu spawnen
                    panel.spawnNodeFromRegistry(entry, gridX, gridY);
                    close();
                }
            }

            ImGui.separator();
            if (ImGui.button("Schliessen", 150, 20)) {
                close();
            }
            ImGui.end();
        }
    }
}
