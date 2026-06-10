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
        // Öffnet die globale Menüleiste ganz oben am Bildschirmrand
        if (ImGui.beginMainMenuBar()) {

            if (ImGui.beginMenu("Datei")) {

                // Button zum Zurückkehren in den Projekt-Manager
                if (ImGui.menuItem("Projekt schliessen")) {
                    engine.closeCurrentProject();
                }

                ImGui.separator();

                // Button zum kompletten Beenden der Engine-Software
                if (ImGui.menuItem("Beenden")) {
                    System.exit(0);
                }

                ImGui.endMenu();
            }

            if (ImGui.beginMenu("Fenster")) {
                ImGui.textDisabled("Hier koennen spaeter Fenster ein-/ausgeblendet werden.");
                ImGui.endMenu();
            }

            ImGui.endMainMenuBar();
        }
    }
}
