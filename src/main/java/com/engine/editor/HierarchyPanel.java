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

        if (ImGui.button("[+] Objekt erstellen...", -1, 25)) {
            ImGui.openPopup("CreateObjectPopup");
        }

        if (ImGui.beginPopup("CreateObjectPopup")) {
            if (ImGui.menuItem("3D Wuerfel (Cube)")) {
                int count = engine.getActiveScene().gameObjects.size() + 1;
                engine.getActiveScene().gameObjects.add(new GameObject("Wuerfel_" + count, GameObject.ObjectType.CUBE));
            }
            if (ImGui.menuItem("Kamera (Camera)")) {
                int count = engine.getActiveScene().gameObjects.size() + 1;
                GameObject cam = new GameObject("SpielKamera_" + count, GameObject.ObjectType.CAMERA);
                cam.posZ = 3.0f;
                engine.getActiveScene().gameObjects.add(cam);
            }
            ImGui.endPopup();
        }

        ImGui.separator();

        for (GameObject obj : engine.getActiveScene().gameObjects) {
            String prefix = obj.type == GameObject.ObjectType.CAMERA ? "[CAM] " : "[OBJ] ";
            if (ImGui.selectable(prefix + obj.name)) {
                engine.setSelectedObject(obj);
            }
        }

        ImGui.end();
    }
}
