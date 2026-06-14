package com.engine.editor;

import com.engine.core.EditorCamera;
import com.engine.core.Main;
import com.engine.scene.GameObject;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.extension.imguizmo.ImGuizmo;
import imgui.extension.imguizmo.flag.Mode;
import imgui.extension.imguizmo.flag.Operation;
import org.joml.Vector3f;

public class ViewportPanel implements EditorPanel {
    private final Main engine;

    // Wir speichern die rohen Float-Arrays für ImGuizmo
    private final float[] viewArray = new float[16];
    private final float[] projArray = new float[16];
    private final float[] modelArray = new float[16];

    private final float[] deltaMatrix = new float[16];
    private final float[] snap = null;

    public ViewportPanel(Main engine) {
        this.engine = engine;
    }

    @Override
    public void updateAndRender() {

        int windowFlags = imgui.flag.ImGuiWindowFlags.None;

        if (ImGuizmo.isUsing() || ImGuizmo.isOver()) {
            windowFlags |= imgui.flag.ImGuiWindowFlags.NoMove;
        }

        ImGui.begin("Spiel-Ansicht (Viewport)", windowFlags);

        ImVec2 windowSize = ImGui.getContentRegionAvail();
        engine.handleViewportResize((int) windowSize.x, (int) windowSize.y);

        ImVec2 screenPos = ImGui.getCursorScreenPos();

        ImGui.image(engine.getTextureId(), engine.getViewportWidth(), engine.getViewportHeight(), 0, 1, 1, 0);

        // --- MAUSKLICK-PRÜFUNG FÜR OBJEKTAUSWAHL ---
        if (ImGui.isWindowHovered() && ImGui.isMouseClicked(0) && !ImGuizmo.isUsing() && !ImGuizmo.isOver()) {
            float relativeMouseX = ImGui.getMousePosX() - screenPos.x;
            float relativeMouseY = ImGui.getMousePosY() - screenPos.y;

            Vector3f rayOrigin = engine.getGameWindow().getCameraPos();
            Vector3f rayDirection = engine.getGameWindow().calculateMouseRay(relativeMouseX, relativeMouseY);

            GameObject hitObject = checkRaySelection(rayOrigin, rayDirection);
            if (hitObject != null) {
                engine.setSelectedObject(hitObject);
                System.out.println("Objekt ausgewaehlt: " + hitObject.name);
            }
        }

        // --- 3D GIZMO STEUERUNG (IMGUIZMO) ---
        GameObject selectedObj = engine.getSelectedObject();
        if (selectedObj != null) {
            ImGuizmo.setOrthographic(false);
            ImGuizmo.setDrawList();

            // DER ENTSCHEIDENDE FIX:
            // Wir lesen die exakten Rahmen-Kanten des inneren Viewports ab.
            float windowWidth = windowSize.x;
            float windowHeight = windowSize.y;

            // Wir sagen ImGuizmo exakt, wo die 3D-Welt auf Ihrem Monitor beginnt und wie groß sie ist!
            ImGuizmo.setRect(screenPos.x, screenPos.y, windowWidth, windowHeight);

            engine.getGameWindow().getViewMatrix().get(viewArray);
            engine.getGameWindow().getProjectionMatrix().get(projArray);

            org.joml.Matrix4f modelMat = new org.joml.Matrix4f().translation(selectedObj.posX, selectedObj.posY, selectedObj.posZ);
            modelMat.get(modelArray);

            // Zeichnet das Achsenkreuz passgenau auf das Objekt
            ImGuizmo.manipulate(viewArray, projArray, Operation.TRANSLATE, Mode.WORLD, modelArray, deltaMatrix, snap);

            if (ImGuizmo.isUsing()) {
                org.joml.Matrix4f updatedModelMat = new org.joml.Matrix4f().set(modelArray);
                Vector3f newPosition = new Vector3f();
                updatedModelMat.getTranslation(newPosition);

                selectedObj.posX = newPosition.x;
                selectedObj.posY = newPosition.y;
                selectedObj.posZ = newPosition.z;
            }
        }

        if (ImGui.isWindowHovered() && ImGui.isMouseDown(1) && !ImGuizmo.isUsing()) {
            EditorCamera cam = engine.getGameWindow().getCamera();

            if (ImGui.isKeyDown(imgui.flag.ImGuiKey.W)) cam.moveForward();
            if (ImGui.isKeyDown(imgui.flag.ImGuiKey.S)) cam.moveBackward();
            if (ImGui.isKeyDown(imgui.flag.ImGuiKey.A)) cam.moveLeft();
            if (ImGui.isKeyDown(imgui.flag.ImGuiKey.D)) cam.moveRight();

            float mouseDeltaX = ImGui.getIO().getMouseDeltaX();
            float mouseDeltaY = ImGui.getIO().getMouseDeltaY();

            if (mouseDeltaX != 0 || mouseDeltaY != 0) {
                cam.rotate(mouseDeltaX, mouseDeltaY);
            }
        }

        ImGui.end();
    }

    private GameObject checkRaySelection(Vector3f rayOrigin, Vector3f rayDirection) {
        GameObject closestHit = null;
        float closestDistance = Float.MAX_VALUE;
        float boundingRadius = 0.3f;

        for (GameObject obj : engine.getActiveScene().gameObjects) {
            Vector3f objPos = new Vector3f(obj.posX, obj.posY, obj.posZ);
            Vector3f originToObj = new Vector3f(objPos).sub(rayOrigin);

            float t = originToObj.dot(rayDirection);
            Vector3f closestPointOnRay = new Vector3f(rayDirection).mul(t).add(rayOrigin);
            float distanceToAxis = closestPointOnRay.distance(objPos);

            if (distanceToAxis <= boundingRadius && t > 0) {
                if (t < closestDistance) {
                    closestDistance = t;
                    closestHit = obj;
                }
            }
        }
        return closestHit;
    }
}
