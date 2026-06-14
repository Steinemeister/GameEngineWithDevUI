package com.engine.editor;

import com.engine.core.Main;
import imgui.ImGui;

public class MenuBarPanel implements EditorPanel {
    private final Main engine;

    public MenuBarPanel(Main engine) {
        this.engine = engine;
    }

    @Override
    public void updateAndRender() {
        if (ImGui.beginMainMenuBar()) {

            if (ImGui.beginMenu("Datei")) {
                if (ImGui.menuItem("Projekt schliessen")) {
                    engine.closeCurrentProject();
                }
                ImGui.separator();
                if (ImGui.menuItem("Beenden")) {
                    System.exit(0);
                }
                ImGui.endMenu();
            }

            // --- DER NEUE RUN-TIME / PLAY BEREICH ---
            if (ImGui.beginMenu("Spiel")) {
                if (ImGui.menuItem("[ > ] Spiel starten (Play)")) {
                    engine.runGameRuntime();
                }
                ImGui.endMenu();
            }

            if (ImGui.beginMenu("Fenster")) {
                ImGui.textDisabled("Fenster-Optionen");
                ImGui.endMenu();
            }

            ImGui.endMainMenuBar();
        }
    }
}
