package com.engine.development.shaderNodeEditor.nodes;

import com.engine.development.shaderNodeEditor.EngineNode;
import com.engine.development.shaderNodeEditor.NodeGraphContext;

public class ColorNode extends EngineNode {
    private final String r, g, b;

    public ColorNode(int id, int outputPinId, String r, String g, String b) {
        super(id, "RGB Color");
        this.outputPins.add(outputPinId);
        this.r = r;
        this.g = g;
        this.b = b;
    }

    @Override
    public String generateCode(NodeGraphContext context) {
        // Gibt einfach den GLSL-Vektor für diese Farbe zurück
        return "vec3(" + r + ", " + g + ", " + b + ")";
    }
}
