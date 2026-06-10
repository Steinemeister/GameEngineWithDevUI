package com.engine.editor;

import com.engine.core.Main;
import com.engine.scene.GameObject;
import imgui.ImGui;

public class HierarchyPanel implements EditorPanel {
    private final Main engine;

    public HierarchyPanel(Main engine) {
        this.engine = engine;
    }

    @Override
    public void updateAndRender() {
        ImGui.begin("Szene-Hierarchie (Hierarchy)");

        if (ImGui.button("➕ Neues Objekt", -1, 25)) {
            // Ein neues Standard-Objekt in der aktiven Szene der Main-Klasse registrieren
            int objCount = engine.getActiveScene().gameObjects.size() + 1;
            engine.getActiveScene().gameObjects.add(new GameObject("GameObject_" + objCount));
        }

        ImGui.separator();

        // Alle Objekte auflisten
        for (GameObject obj : engine.getActiveScene().gameObjects) {
            // Selectable stellt die Objekte als klickbare Zeilen dar
            if (ImGui.selectable("🤖 " + obj.name)) {
                System.out.println(obj.name + " ausgewählt!");
                // Hier docken wir später das Inspector-Panel für die X/Y/Z-Verschiebung an
            }
        }

        ImGui.end();
    }
}
