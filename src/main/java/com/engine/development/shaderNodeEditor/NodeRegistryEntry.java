package com.engine.development.shaderNodeEditor;

import java.util.function.BiFunction;

public record NodeRegistryEntry(
        String name,
        String category,
        BiFunction<Integer, Integer, EngineNode> creator
) {}
