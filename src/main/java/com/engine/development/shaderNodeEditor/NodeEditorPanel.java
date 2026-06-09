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

    // Die ausgelagerte Menü-Klasse instanziieren
    private final NodeSpawnMenu spawnMenu = new NodeSpawnMenu();

    // Zustand für das Node-Kontextmenü (Löschen)
    private boolean showNodeMenu = false;
    private int selectedNodeMenuId = -1;
    private final ImVec2 nodeMenuPos = new ImVec2();

    public NodeEditorPanel(Main engine) {
        this.engine = engine;
        ImNodes.createContext();

        // Output-Knoten initialisieren
        outputNode = new OutputNode(idCounter++, idCounter++);
        activeNodes.add(outputNode);
        context.registerNodePins(outputNode);
        ImNodes.setNodeGridSpacePos(outputNode.id, 400, 200);
    }

    /**
     * Wird vom NodeSpawnMenu aufgerufen, wenn ein Button geklickt wurde
     */
    public void spawnNodeFromRegistry(NodeRegistryEntry entry, float gridX, float gridY) {
        // Wir erstellen die Node über die Lambda-Funktion der Registry und erhöhen die IDs
        EngineNode node = entry.creator().apply(idCounter, idCounter + 1);

        // Da manche Knoten mehr als einen Pin erzeugen (z.B. Multiply),
        // erhöhen wir unseren globalen Zähler um die Anzahl aller generierten Pins
        idCounter += 1 + node.inputPins.size() + node.outputPins.size();

        activeNodes.add(node);
        context.registerNodePins(node);
        ImNodes.setNodeGridSpacePos(node.id, gridX, gridY);
    }

    @Override
    public void updateAndRender() {
        ImGui.begin("Shader Node Editor");
        ImGui.text("Rechtsklick auf freien Raum = Erstellen | Rechtsklick auf Titel = Menue");
        ImGui.separator();

        int nextHoveredNodeId = -1;
        int nextHoveredLinkId = -1;
        boolean openSpawnRequest = false;
        boolean openNodeRequest = false;

        ImNodes.beginNodeEditor();

        // 1. KNOTEN ZEICHNEN
        for (EngineNode node : activeNodes) {
            ImNodes.beginNode(node.id);
            ImNodes.beginNodeTitleBar(); ImGui.text(node.title); ImNodes.endNodeTitleBar();

            if (node instanceof FloatNode) {
                float oldVal = ((FloatNode) node).getValue();
                ((FloatNode) node).drawInternalUI();
                if (oldVal != ((FloatNode) node).getValue()) triggerShaderUpdate();
            }

            for (int i = 0; i < node.inputPins.size(); i++) {
                ImNodes.beginInputAttribute(node.inputPins.get(i));
                ImGui.text(node instanceof MultiplyNode ? (i == 0 ? "In A" : "In B") : "In");
                ImNodes.endInputAttribute();
            }

            for (int i = 0; i < node.outputPins.size(); i++) {
                ImNodes.beginOutputAttribute(node.outputPins.get(i));
                ImGui.text("Out =>");
                ImNodes.endOutputAttribute();
            }
            ImNodes.endNode();
        }

        // 2. LINKS ZEICHNEN
        for (int[] link : links) { ImNodes.link(link[0], link[1], link[2]); }

        // 3. HOVER-ZUSTÄNDE SICHER INNEN ABFRAGEN
        int internalHoveredNode = ImNodes.getHoveredNode();
        int internalHoveredLink = ImNodes.getHoveredLink();

        if (ImGui.isWindowHovered(imgui.flag.ImGuiHoveredFlags.ChildWindows) && ImGui.isMouseClicked(1)) {
            if (internalHoveredLink == -1 && internalHoveredNode == -1) {
                openSpawnRequest = true;
            } else if (internalHoveredNode != -1 && internalHoveredNode != outputNode.id) {
                openNodeRequest = true;
                nextHoveredNodeId = internalHoveredNode;
            } else if (internalHoveredLink != -1) {
                nextHoveredLinkId = internalHoveredLink;
            }
        }

        ImNodes.endNodeEditor(); // Scope geschlossen!

        // --- 4. AKTIONEN AUSSERHALB VERARBEITEN ---
        if (openSpawnRequest) {
            imgui.ImVec2 panning = new imgui.ImVec2();
            ImNodes.editorContextGetPanning(panning);
            spawnMenu.open(
                    ImGui.getMousePosX() - 4.0f, ImGui.getMousePosY() - 4.0f,
                    ImGui.getMousePosX() - panning.x, ImGui.getMousePosY() - panning.y
            );
            showNodeMenu = false;
        }

        if (openNodeRequest) {
            showNodeMenu = !showNodeMenu;
            selectedNodeMenuId = nextHoveredNodeId;
            nodeMenuPos.x = ImGui.getMousePosX();
            nodeMenuPos.y = ImGui.getMousePosY();
            spawnMenu.close();
        }

        // ENTF-Taste verarbeiten
        if (ImGui.isWindowHovered(imgui.flag.ImGuiHoveredFlags.ChildWindows) &&
                (ImGui.isKeyPressed(imgui.flag.ImGuiKey.Delete) || ImGui.isKeyPressed(imgui.flag.ImGuiKey.Backspace))) {
            int[] selectedNodes = new int[ImNodes.numSelectedNodes()];
            if (selectedNodes.length > 0) {
                ImNodes.getSelectedNodes(selectedNodes);
                for (int nodeId : selectedNodes) {
                    if (nodeId != outputNode.id) deleteNode(nodeId);
                }
            }
        }

        // Neue Verbindungen erstellen
        if (ImNodes.isLinkCreated(startPin, endPin)) {
            boolean exists = false;
            for (int[] l : links) { if (l[2] == endPin.get()) { exists = true; break; } }
            if (!exists) {
                links.add(new int[]{linkIdCounter++, startPin.get(), endPin.get()});
                context.addLink(startPin.get(), endPin.get());
                triggerShaderUpdate();
            }
        }

        // Link löschen per Rechtsklick
        if (nextHoveredLinkId != -1) {
            int[] toRemove = null;
            for (int[] link : links) { if (link[0] == nextHoveredLinkId) { toRemove = link; break; } }
            if (toRemove != null) { links.remove(toRemove); rebuildGraphLinks(); }
        }

        ImGui.end(); // Hauptfenster schließen

        // --- 5. DIE EXTERNEN MENÜS UPDATE-SCHLEIFEN ---
        spawnMenu.updateAndRender(this);

        if (showNodeMenu && selectedNodeMenuId != -1) {
            ImGui.setNextWindowPos(nodeMenuPos.x, nodeMenuPos.y, imgui.flag.ImGuiCond.Appearing);
            int nodeWindowFlags = imgui.flag.ImGuiWindowFlags.NoTitleBar | imgui.flag.ImGuiWindowFlags.NoResize | imgui.flag.ImGuiWindowFlags.NoCollapse | imgui.flag.ImGuiWindowFlags.AlwaysAutoResize;
            if (ImGui.begin("Knoten-Aktionen##" + selectedNodeMenuId, nodeWindowFlags)) {
                if (!ImGui.isWindowHovered(imgui.flag.ImGuiHoveredFlags.ChildWindows) && (ImGui.isMouseClicked(0) || ImGui.isMouseClicked(1)) && !ImGui.isWindowAppearing()) {
                    showNodeMenu = false;
                }
                if (ImGui.button("Knoten loeschen", 150, 25)) {
                    deleteNode(selectedNodeMenuId);
                    showNodeMenu = false;
                    selectedNodeMenuId = -1;
                }
                ImGui.end();
            }
        }
    }

    private void deleteNode(int nodeId) {
        EngineNode nodeToRemove = null;
        for (EngineNode node : activeNodes) { if (node.id == nodeId) { nodeToRemove = node; break; } }
        if (nodeToRemove != null) {
            activeNodes.remove(nodeToRemove);
            List<int[]> linksToRemove = new ArrayList<>();
            for (int[] link : links) {
                if (nodeToRemove.inputPins.contains(link) || nodeToRemove.outputPins.contains(link)) linksToRemove.add(link);
            }
            links.removeAll(linksToRemove);
            rebuildGraphLinks();
        }
    }

    private void rebuildGraphLinks() {
        context.clearLinks();
        for (int[] link : links) { context.addLink(link[1], link[2]); }
        triggerShaderUpdate();
    }

    private void triggerShaderUpdate() {
        String generatedGLSL = outputNode.generateCode(context);
        engine.compileShader(generatedGLSL);
    }
}
