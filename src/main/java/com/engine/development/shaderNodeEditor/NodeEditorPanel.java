package com.engine.development.shaderNodeEditor;

import com.Main;
import com.engine.development.EditorPanel;
import com.engine.development.shaderNodeEditor.nodes.ColorNode;
import com.engine.development.shaderNodeEditor.nodes.FloatNode;
import com.engine.development.shaderNodeEditor.nodes.MultiplyNode;
import com.engine.development.shaderNodeEditor.nodes.OutputNode;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.extension.imnodes.ImNodes;
import imgui.type.ImInt;

import java.util.ArrayList;
import java.util.List;

public class NodeEditorPanel implements EditorPanel {
    private final Main engine;
    private final NodeGraphContext context = new NodeGraphContext();

    private final List<EngineNode> activeNodes = new ArrayList<>();
    private final List<int[]> links = new ArrayList<>();

    private final OutputNode outputNode;
    private int idCounter = 1;
    private int linkIdCounter = 1000;

    private final ImInt startPin = new ImInt();
    private final ImInt endPin = new ImInt();

    // Variablen für unser eigenes, stabiles Spawn-Menü
    private boolean showSpawnWindow = false;
    private final ImVec2 spawnMenuPos = new ImVec2();

    public NodeEditorPanel(Main engine) {
        this.engine = engine;
        ImNodes.createContext();

        // Output-Knoten initialisieren
        outputNode = new OutputNode(idCounter++, idCounter++);
        activeNodes.add(outputNode);
        context.registerNodePins(outputNode);
        ImNodes.setNodeGridSpacePos(outputNode.id, 400, 200);
    }

    private void spawnColorNode(float x, float y) {
        ColorNode node = new ColorNode(idCounter++, idCounter++, "1.0", "0.0", "0.0");
        activeNodes.add(node);
        context.registerNodePins(node);
        ImNodes.setNodeGridSpacePos(node.id, x, y);
    }

    private void spawnFloatNode(float x, float y) {
        FloatNode node = new FloatNode(idCounter++, idCounter++);
        activeNodes.add(node);
        context.registerNodePins(node);
        ImNodes.setNodeGridSpacePos(node.id, x, y);
    }

    private void spawnMultiplyNode(float x, float y) {
        MultiplyNode node = new MultiplyNode(idCounter++, idCounter++, idCounter++, idCounter++);
        activeNodes.add(node);
        context.registerNodePins(node);
        ImNodes.setNodeGridSpacePos(node.id, x, y);
    }

    @Override
    public void updateAndRender() {
        ImGui.begin("Shader Node Editor");
        ImGui.text("Rechtsklick = Menü öffnen/schließen | Rechtsklick auf Linie = Löschen");
        ImGui.separator();

        ImNodes.beginNodeEditor();

        // --- 1. KNOTEN ZEICHNEN ---
        for (EngineNode node : activeNodes) {
            ImNodes.beginNode(node.id);
            ImNodes.beginNodeTitleBar();
            ImGui.text(node.title);
            ImNodes.endNodeTitleBar();

            if (node instanceof FloatNode) {
                float oldVal = ((FloatNode) node).getValue();
                ((FloatNode) node).drawInternalUI();
                if (oldVal != ((FloatNode) node).getValue()) {
                    triggerShaderUpdate();
                }
            }

            for (int inputPin : node.inputPins) {
                ImNodes.beginInputAttribute(inputPin);
                if (node instanceof MultiplyNode) {
                    ImGui.text(inputPin == ((MultiplyNode) node).inputPinA ? "In A" : "In B");
                } else {
                    ImGui.text("In");
                }
                ImNodes.endInputAttribute();
            }

            for (int outputPin : node.outputPins) {
                ImNodes.beginOutputAttribute(outputPin);
                ImGui.text("Out =>");
                ImNodes.endOutputAttribute();
            }

            ImNodes.endNode();
        }

        // --- 2. VERBINDUNGEN ZEICHNEN ---
        for (int[] link : links) {
            ImNodes.link(link[0], link[1], link[2]);
        }

        // --- 3. RECHTSKLICK-ABFANGEN IM RASTER ---
        // Wenn im Node-Editor rechtsgeklickt wird, aber KEIN Knoten und KEINE Linie getroffen wurde:
        if (ImNodes.isEditorHovered() && ImGui.isMouseClicked(1) && ImNodes.getHoveredLink() == -1 && ImNodes.getHoveredNode() == -1) {
            showSpawnWindow = !showSpawnWindow; // Fenster umschalten (An/Aus)

            // WICHTIG: Das fängt die echten Koordinaten im Node-Raster ab!
            spawnMenuPos.x = ImGui.getMousePosX();
            spawnMenuPos.y = ImGui.getMousePosY();
        }

        ImNodes.endNodeEditor(); // Node-Kontext wird sauber geschlossen!

        // --- 4. ABSTURZSICHERES EXTRA-FENSTER FÜR DAS MENÜ ---
        if (showSpawnWindow) {
            // Wir setzen das Fenster genau an die Position des Mausklicks
            ImGui.setNextWindowPos(spawnMenuPos.x, spawnMenuPos.y, imgui.flag.ImGuiCond.Appearing);

            // Ein kleines rahmenloses Pop-up-Fenster erstellen
            int windowFlags = imgui.flag.ImGuiWindowFlags.NoResize | imgui.flag.ImGuiWindowFlags.NoCollapse;
            if (ImGui.begin("Knoten erstellen##SpawnMenu", windowFlags)) {

                if (ImGui.button("RGB Color Node", -1, 25)) {
                    spawnColorNode(spawnMenuPos.x, spawnMenuPos.y);
                    showSpawnWindow = false; // Nach Klick schließen
                }
                if (ImGui.button("Float Constant", -1, 25)) {
                    spawnFloatNode(spawnMenuPos.x, spawnMenuPos.y);
                    showSpawnWindow = false;
                }
                if (ImGui.button("Multiply (Mathe)", -1, 25)) {
                    spawnMultiplyNode(spawnMenuPos.x, spawnMenuPos.y);
                    showSpawnWindow = false;
                }

                ImGui.separator();
                if (ImGui.button("Schließen", -1, 20)) {
                    showSpawnWindow = false;
                }

                ImGui.end();
            }
        }

        // --- 5. VERBINDUNGEN LOGISCH PRÜFEN ---
        if (ImNodes.isLinkCreated(startPin, endPin)) {
            boolean exists = false;
            for (int[] l : links) {
                if (l[2] == endPin.get()) { exists = true; break; }
            }
            if (!exists) {
                links.add(new int[]{linkIdCounter++, startPin.get(), endPin.get()});
                context.addLink(startPin.get(), endPin.get());
                triggerShaderUpdate();
            }
        }

        // --- 6. LINIEN PER RECHTSKLICK LÖSCHEN ---
        int hoveredLinkId = ImNodes.getHoveredLink();
        if (hoveredLinkId != -1 && ImGui.isMouseClicked(1)) {
            int[] toRemove = null;
            for (int[] link : links) {
                if (link[0] == hoveredLinkId) { toRemove = link; break; }
            }
            if (toRemove != null) {
                links.remove(toRemove);
                context.clearLinks();
                for (int[] link : links) {
                    context.addLink(link[1], link[2]);
                }
                triggerShaderUpdate();
            }
        }

        ImGui.end();
    }

    private void triggerShaderUpdate() {
        String generatedGLSL = outputNode.generateCode(context);
        engine.compileShader(generatedGLSL);
    }
}
