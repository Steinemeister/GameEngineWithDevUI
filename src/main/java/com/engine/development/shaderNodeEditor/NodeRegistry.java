package com.engine.development.shaderNodeEditor;

import com.engine.development.shaderNodeEditor.nodes.ColorNode;
import com.engine.development.shaderNodeEditor.nodes.FloatNode;
import com.engine.development.shaderNodeEditor.nodes.MultiplyNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class NodeRegistry {
    private static final List<NodeRegistryEntry> entries = new ArrayList<>();

    static {
        // HIER REGISTRIEREN SIE JEDEN NEUEN KNOTENTYPEN:
        register("RGB Color Node", "Eingabe", (id, pinId) -> new ColorNode(id, pinId, "1.0", "0.0", "0.0"));
        register("Float Constant", "Eingabe", (id, pinId) -> new FloatNode(id, pinId));

        // Die MultiplyNode braucht 4 IDs (NodeId, PinInA, PinInB, PinOut),
        // daher füllen wir die restlichen IDs einfach fortlaufend auf (+1, +2, +3)
        register("Multiply (Mathe)", "Mathematik", (id, pinId) -> new MultiplyNode(id, pinId, pinId + 1, pinId + 2));
    }

    private static void register(String name, String category, BiFunction<Integer, Integer, EngineNode> creator) {
        entries.add(new NodeRegistryEntry(name, category, creator));
    }

    public static List<NodeRegistryEntry> getEntries() {
        return entries;
    }
}
