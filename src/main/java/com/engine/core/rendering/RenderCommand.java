package com.engine.core.rendering;

public class RenderCommand {
    public int count;         // Anzahl der Vertices des Modells (z.B. 36 für Würfel)
    public int instanceCount; // Wie viele Kopien (Instanzen) gezeichnet werden sollen
    public int first;         // Start-Vertex im VBO
    public int baseInstance;  // Start-Index im Instanz-Buffer

    public RenderCommand(int count, int instanceCount, int first, int baseInstance) {
        this.count = count;
        this.instanceCount = instanceCount;
        this.first = first;
        this.baseInstance = baseInstance;
    }

    // Wandelt die Daten in ein int-Array für den GPU-Buffer um
    public int[] toIntArray() {
        return new int[]{ count, instanceCount, first, baseInstance };
    }
}
