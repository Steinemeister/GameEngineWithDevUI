package com.engine.development.shaderNodeEditor;

import java.util.ArrayList;
import java.util.List;

public abstract class EngineNode {
    public final int id;
    public final String title;

    // Listen für die IDs der Ein- und Ausgänge
    public final List<Integer> inputPins = new ArrayList<>();
    public final List<Integer> outputPins = new ArrayList<>();

    public EngineNode(int id, String title) {
        this.id = id;
        this.title = title;
    }

    // Jede Node muss definieren, welchen GLSL-Code sie generiert
    public abstract String generateCode(NodeGraphContext context);
}
