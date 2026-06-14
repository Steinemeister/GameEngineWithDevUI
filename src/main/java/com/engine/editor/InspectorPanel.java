package com.engine.editor;

import com.engine.core.Main;
import com.engine.scene.GameObject;
import imgui.ImGui;

public class InspectorPanel implements EditorPanel {
    private final Main engine;
    // Ein Puffer-Array, da ImGui flache float[]-Referenzen für Positionsdaten benötigt
    private final float[] positionBuffer = new float[3];

    public InspectorPanel(Main engine) {
        this.engine = engine;
    }

    @Override
    public void updateAndRender() {
        ImGui.begin("Eigenschaften (Inspector)");

        GameObject selectedObj = engine.getSelectedObject();
        if (selectedObj == null) {
            ImGui.textDisabled("Waehlen Sie ein Objekt aus der Hierarchie oder dem Viewport aus.");
        } else {
            ImGui.text("Name: " + selectedObj.name);
            ImGui.separator();

            // 1. Die aktuellen Werte des Objekts in unseren Puffer laden
            positionBuffer[0] = selectedObj.posX;
            positionBuffer[1] = selectedObj.posY;
            positionBuffer[2] = selectedObj.posZ;

            // 2. Ein interaktives 3er-Eingabefeld für die Position anzeigen (Schrittweite 0.05f)
            ImGui.text("Transformation:");
            if (ImGui.dragFloat3("Position (X/Y/Z)", positionBuffer, 0.05f)) {
                // 3. Wenn der Nutzer die Zahlen im Inspector ändert, schreiben wir sie zurück
                selectedObj.posX = positionBuffer[0];
                selectedObj.posY = positionBuffer[1];
                selectedObj.posZ = positionBuffer[2];
            }

            ImGui.separator();
            ImGui.textDisabled("Weitere Komponenten (z.B. Mesh, Material) koennen hier angedockt werden.");
        }

        ImGui.end();
    }
}
