package com.engine.core;

import org.joml.Vector3f;

public class EditorCamera {
    private final Vector3f position = new Vector3f(0.0f, 1.0f, 4.0f);
    private final Vector3f forward = new Vector3f(0.0f, 0.0f, -1.0f);
    private final Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);

    private float yaw = -90.0f; // Blickwinkel links/rechts
    private float pitch = 0.0f;  // Blickwinkel oben/unten
    private final float speed = 0.05f;
    private final float sensitivity = 0.15f;

    public void moveForward() {
        position.add(new Vector3f(forward).mul(speed));
    }

    public void moveBackward() {
        position.sub(new Vector3f(forward).mul(speed));
    }

    public void moveLeft() {
        Vector3f left = new Vector3f(forward).cross(up).normalize();
        position.sub(left.mul(speed));
    }

    public void moveRight() {
        Vector3f right = new Vector3f(forward).cross(up).normalize();
        position.add(right.mul(speed));
    }

    public void rotate(float deltaX, float deltaY) {
        yaw += deltaX * sensitivity;
        pitch -= deltaY * sensitivity;

        // Begrenzung, damit die Kamera sich nicht überschlägt
        if (pitch > 89.0f) pitch = 89.0f;
        if (pitch < -89.0f) pitch = -89.0f;

        updateVectors();
    }

    private void updateVectors() {
        Vector3f dir = new Vector3f();
        dir.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        dir.y = (float) Math.sin(Math.toRadians(pitch));
        dir.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        forward.set(dir).normalize();
    }

    public Vector3f getPosition() { return position; }
    public Vector3f getTarget() { return new Vector3f(position).add(forward); }
    public Vector3f getUp() { return up; }
    public Vector3f getForward() { return forward; }
}
