package com.engine.development.shaderNodeEditor.nodes;

import com.engine.development.shaderNodeEditor.EngineNode;
import com.engine.development.shaderNodeEditor.NodeGraphContext;

public class MultiplyNode extends EngineNode {
    public final int inputPinA;
    public final int inputPinB;
    public final int outputPin;

    public MultiplyNode(int id, int inputPinA, int inputPinB, int outputPin) {
        super(id, "Multiply (Mathe)");
        this.inputPinA = inputPinA;
        this.inputPinB = inputPinB;
        this.outputPin = outputPin;

        // Pins registrieren
        this.inputPins.add(inputPinA);
        this.inputPins.add(inputPinB);
        this.outputPins.add(outputPin);
    }

    @Override
    public String generateCode(NodeGraphContext context) {
        // Holt sich den Code von Eingang A und Eingang B
        String codeA = context.getConnectedCode(inputPinA);
        String codeB = context.getConnectedCode(inputPinB);

        // Generiert z.B. (vec3(1.0, 0.5, 0.0) * 2.5)
        return "(" + codeA + " * " + codeB + ")";
    }
}
