package com.engine.core.renderer;

import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

public class RenderSnapshot {
    // Die aus der Szene extrahierten Zeichendaten
    public final List<RenderElement> elements = new ArrayList<>();

    // Globale Kamera-Daten für dieses Frame
    public final Vector2f cameraPosition = new Vector2f();
    public float cameraZoom = 1.0f;

    public static class RenderElement {
        public final Vector2f position = new Vector2f();
        public final Vector2f scale = new Vector2f();
        public float rotation;
        public final Vector4f color = new Vector4f(1, 1, 1, 1);
        public String textureUuid;

        public RenderElement(Vector2f pos, Vector2f scale, float rot, Vector4f col, String texUuid) {
            this.position.set(pos);
            this.scale.set(scale);
            this.rotation = rot;
            this.color.set(col);
            this.textureUuid = texUuid;
        }
    }

    public void clear() {
        elements.clear();
    }
}
