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
        // --- 1. HAUPTFENSTER FÜR DEN EDITOR ---
        ImGui.begin("Shader Node Editor");
        ImGui.text("Rechtsklick = Menue oeffnen | Rechtsklick auf Linie = Loeschen");
        ImGui.separator();

        // REINES EVENT-CHECKING VIA IMGUI (ABSTURZSICHER!)
        // Wir prüfen hier, ob der Nutzer in das aktuelle Fenster rechtsklickt,
        // BEVOR wir überhaupt mit ImNodes anfangen.
        boolean triggerSpawnMenu = false;
        if (ImGui.isWindowHovered(imgui.flag.ImGuiHoveredFlags.ChildWindows) && ImGui.isMouseClicked(1) && ImNodes.getHoveredLink() == -1 && ImNodes.getHoveredNode() == -1) {
            triggerSpawnMenu = true;
            spawnMenuPos.x = ImGui.getMousePosX();
            spawnMenuPos.y = ImGui.getMousePosY();
        }

        // Jetzt starten wir ImNodes ganz normal
        ImNodes.beginNodeEditor();

        // --- NODES SCHLEIFE ---
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

            // INPUTS ZEICHNEN
            if (!node.inputPins.isEmpty()) {
                for (int i = 0; i < node.inputPins.size(); i++) {
                    int inputPin = node.inputPins.get(i);
                    ImNodes.beginInputAttribute(inputPin);
                    if (node instanceof MultiplyNode) {
                        ImGui.text(i == 0 ? "In A" : "In B");
                    } else {
                        ImGui.text("In");
                    }
                    ImNodes.endInputAttribute();
                }
            }

            // OUTPUTS ZEICHNEN
            if (!node.outputPins.isEmpty()) {
                for (int i = 0; i < node.outputPins.size(); i++) {
                    int outputPin = node.outputPins.get(i);
                    ImNodes.beginOutputAttribute(outputPin);
                    ImGui.text("Out =>");
                    ImNodes.endOutputAttribute();
                }
            }

            ImNodes.endNode();
        }

        // --- VERBINDUNGEN ZEICHNEN ---
        for (int[] link : links) {
            ImNodes.link(link[0], link[1], link[2]);
        }

        // ImNodes sauber schließen. Da wir hier drin kein isEditorHovered() aufrufen,
        // bleibt der C++ Scope perfekt auf ImNodesScope_None.
        ImNodes.endNodeEditor();

        // --- LOGISCHE PRÜFUNGEN ---
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

        // Hauptfenster beenden
        ImGui.end();

        // --- 2. FLOATING SPAWN MENÜ (KOMPLETT UNABHÄNGIG) ---
        if (triggerSpawnMenu) {
            showSpawnWindow = !showSpawnWindow;
        }

        if (showSpawnWindow) {
            ImGui.setNextWindowPos(spawnMenuPos.x, spawnMenuPos.y, imgui.flag.ImGuiCond.Appearing);
            int windowFlags = imgui.flag.ImGuiWindowFlags.NoResize | imgui.flag.ImGuiWindowFlags.NoCollapse | imgui.flag.ImGuiWindowFlags.AlwaysAutoResize;

            if (ImGui.begin("Knoten erstellen", windowFlags)) {
                ImGui.textDisabled("Auswaehlen:");
                ImGui.separator();

                if (ImGui.button("RGB Color Node", 150, 25)) {
                    spawnColorNode(spawnMenuPos.x, spawnMenuPos.y);
                    showSpawnWindow = false;
                }
                if (ImGui.button("Float Constant", 150, 25)) {
                    spawnFloatNode(spawnMenuPos.x, spawnMenuPos.y);
                    showSpawnWindow = false;
                }
                if (ImGui.button("Multiply (Mathe)", 150, 25)) {
                    spawnMultiplyNode(spawnMenuPos.x, spawnMenuPos.y);
                    showSpawnWindow = false;
                }

                ImGui.separator();
                if (ImGui.button("Schliessen", 150, 20)) {
                    showSpawnWindow = false;
                }

                ImGui.end();
            }
        }
    }

    private void triggerShaderUpdate() {
        String generatedGLSL = outputNode.generateCode(context);
        engine.compileShader(generatedGLSL);
    }
}
