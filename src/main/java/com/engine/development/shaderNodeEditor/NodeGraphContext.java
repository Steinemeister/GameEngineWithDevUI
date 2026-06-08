package com.engine.development.shaderNodeEditor;

import java.util.HashMap;
import java.util.Map;

public class NodeGraphContext {
    // Key: InputPinID -> Value: OutputPinID
    private final Map<Integer, Integer> connectedLinks = new HashMap<>();
    // Key: PinID -> Value: Die Node, zu der der Pin gehört
    private final Map<Integer, EngineNode> pinToNodeMap = new HashMap<>();

    public void registerNodePins(EngineNode node) {
        for (int pin : node.inputPins) pinToNodeMap.put(pin, node);
        for (int pin : node.outputPins) pinToNodeMap.put(pin, node);
    }

    public void addLink(int fromOutputPin, int toInputPin) {
        connectedLinks.put(toInputPin, fromOutputPin);
    }

    public void clearLinks() {
        connectedLinks.clear();
    }

    /**
     * Sucht die Quell-Node, die an einem bestimmten Input-Pin angeschlossen ist,
     * und generiert rekursiv deren Code.
     */
    public String getConnectedCode(int inputPinId) {
        Integer outputPinSource = connectedLinks.get(inputPinId);
        if (outputPinSource == null) {
            return "vec3(1.0, 1.0, 1.0)"; // Standardwert (Weiß), falls nichts verbunden ist
        }
        EngineNode sourceNode = pinToNodeMap.get(outputPinSource);
        return sourceNode != null ? sourceNode.generateCode(this) : "vec3(1.0, 1.0, 1.0)";
    }
}
