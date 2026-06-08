package com.engine.development.shaderNodeEditor.nodes;

import com.engine.development.shaderNodeEditor.EngineNode;
import com.engine.development.shaderNodeEditor.NodeGraphContext;
import imgui.ImGui;

public class FloatNode extends EngineNode {
    public final int outputPin;
    // Ein Array, da ImGui für Slider veränderbare Referenzen braucht
    private final float[] value = {1.0f};

    public FloatNode(int id, int outputPin) {
        super(id, "Float Constant");
        this.outputPin = outputPin;
        this.outputPins.add(outputPin);
    }

    // Neu: Eine Methode, um einen Slider direkt in der Node anzuzeigen!
    public void drawInternalUI() {
        ImGui.pushItemWidth(100);
        ImGui.sliderFloat("Wert", value, 0.0f, 5.0f);
        ImGui.popItemWidth();
    }

    public float getValue() {
        return value[0];
    }

    @Override
    public String generateCode(NodeGraphContext context) {
        return String.valueOf(value[0]);
    }
}
