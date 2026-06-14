package com.engine.scene;

import java.util.ArrayList;
import java.util.List;

public class GameObject {
    public enum ObjectType {
        CUBE,
        CAMERA
    }

    public String name;
    public ObjectType type;

    public float posX = 0.0f;
    public float posY = 0.0f;
    public float posZ = 0.0f;

    public Material material;

    // --- NEU: Die Komponenten-Liste ---
    public List<Component> components = new ArrayList<>();

    public GameObject(String name, ObjectType type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Hilfsmethode, um eine spezifische Komponente aus dem Objekt herauszusuchen.
     */
    public <T extends Component> T getComponent(Class<T> componentClass) {
        // DER FIX: Falls Gson die Liste beim Laden auf null gesetzt hat,
        // reparieren wir sie hier sofort im Speicher!
        if (this.components == null) {
            this.components = new ArrayList<>();
        }

        for (Component c : components) {
            if (componentClass.isAssignableFrom(c.getClass())) {
                return componentClass.cast(c);
            }
        }
        return null;
    }
}
