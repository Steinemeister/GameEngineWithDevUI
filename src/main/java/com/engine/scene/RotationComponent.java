package com.engine.scene;

public class RotationComponent extends Component {
    public float rotationSpeed = 1.0f;
    public float currentAngle = 0.0f;

    @Override
    public void update(GameObject parent) {
        // Erhöht den Winkel basierend auf der Geschwindigkeit
        currentAngle += rotationSpeed;
        if (currentAngle >= 360.0f) currentAngle -= 360.0f;
    }
}
