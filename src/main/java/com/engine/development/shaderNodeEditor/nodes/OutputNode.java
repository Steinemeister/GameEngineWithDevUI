package com.engine.development.shaderNodeEditor.nodes;

import com.engine.development.shaderNodeEditor.EngineNode;
import com.engine.development.shaderNodeEditor.NodeGraphContext;

public class OutputNode extends EngineNode {
    public final int inputPinId;

    public OutputNode(int id, int inputPinId) {
        super(id, "Material Output");
        this.inputPinId = inputPinId;
        this.inputPins.add(inputPinId);
    }

    @Override
    public String generateCode(NodeGraphContext context) {
        // Holt sich dynamisch den Code des Knotens, der am Eingang hängt
        String inputColorCode = context.getConnectedCode(inputPinId);

        return """
        #version 330 core
        in vec3 vertexColor;
        out vec4 FragColor;
        uniform float uColorPulse;
        
        void main() {
            // Der von den Nodes generierte Code wird hier eingesetzt:
            vec3 nodeColor = %s;
            FragColor = vec4(nodeColor * uColorPulse, 1.0);
        }
        """.formatted(inputColorCode);
    }
}
