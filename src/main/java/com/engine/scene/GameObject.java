package com.engine.scene;

public class GameObject {
    public enum ObjectType {
        CUBE,
        CAMERA
    }

    public String name;
    public ObjectType type;

    // Position im 3D-Raum
    public float posX = 0.0f;
    public float posY = 0.0f;
    public float posZ = 0.0f;

    public GameObject(String name, ObjectType type) {
        this.name = name;
        this.type = type;
    }
}
