package com.engine.core;

import com.engine.scene.GameObject;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

public class GameWindow {

    private int fboId, textureId, rboId;
    private int width = 800;
    private int height = 600;

    private Shader customShader;
    private int vaoId, vboId;

    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();
    private final Matrix4f modelMatrix = new Matrix4f();

    private final EditorCamera camera = new EditorCamera();

    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    private final String vertexShader3D = """
        #version 330 core
        layout (location = 0) in vec3 aPos;
        layout (location = 1) in vec3 aColor;
        
        out vec3 vertexColor;
        
        uniform mat4 uProjection;
        uniform mat4 uView;
        uniform mat4 uModel;
        
        void main() {
            gl_Position = uProjection * uView * uModel * vec4(aPos, 1.0);
            vertexColor = aColor;
        }
    """;

    public void init() {
        setupFramebuffer();
        setupCubeMesh();

        // 3D-Shader kompilieren
        customShader = new Shader(vertexShader3D, """
            #version 330 core
            in vec3 vertexColor;
            out vec4 FragColor;
            void main() {
                FragColor = vec4(vertexColor, 1.0);
            }
        """);
    }

    public void compileShader(String fragmentCode) {
        customShader.compileAndLink(vertexShader3D, fragmentCode);
    }

    public void render(boolean toFramebuffer, List<GameObject> objects) {
        if (toFramebuffer) glBindFramebuffer(GL_FRAMEBUFFER, fboId);

        glViewport(0, 0, width, height);
        glClearColor(0.1f, 0.11f, 0.15f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Tiefentest für 3D aktivieren
        glEnable(GL_DEPTH_TEST);

        // Matrizen berechnen
        float aspectRatio = (float) width / (float) height;
        projectionMatrix.setPerspective((float) Math.toRadians(45.0f), aspectRatio, 0.1f, 100.0f);

        // Kamera schaut von cameraPos auf die Mitte (0,0,0)
        viewMatrix.setLookAt(camera.getPosition(), camera.getTarget(), camera.getUp());

        customShader.bind();

        // Matrizen an den Shader senden
        sendMatrixToShader("uProjection", projectionMatrix);
        sendMatrixToShader("uView", viewMatrix);

        glBindVertexArray(vaoId);

        // ALLE OBJEKTE AUS DER HIERARCHIE RENDERN
        for (GameObject obj : objects) {
            // Model-Matrix für jedes Objekt anhand seiner Position aufbauen
            modelMatrix.identity().translate(obj.posX, obj.posY, obj.posZ);
            sendMatrixToShader("uModel", modelMatrix);

            // Einen Würfel/Objekt zeichnen (36 Vertices für 6 Würfelseiten)
            glDrawArrays(GL_TRIANGLES, 0, 36);
        }

        glBindVertexArray(0);
        customShader.unbind();

        if (toFramebuffer) glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public Vector3f calculateMouseRay(float mouseX, float mouseY) {
        // 1. Koordinaten in normalisierte Bildschirmkoordinaten (-1 bis 1) umrechnen
        float x = (2.0f * mouseX) / width - 1.0f;
        float y = 1.0f - (2.0f * mouseY) / height;

        Vector4f rayClip = new Vector4f(x, y, -1.0f, 1.0f);

        // 2. Zurück durch die Projektions-Matrix rechnen
        Matrix4f invProjection = new Matrix4f(projectionMatrix).invert();
        Vector4f rayEye = invProjection.transform(rayClip);
        rayEye.z = -1.0f;
        rayEye.w = 0.0f;

        // 3. Zurück durch die View-Kamera-Matrix rechnen
        Matrix4f invView = new Matrix4f(viewMatrix).invert();
        Vector4f rayWorld4 = invView.transform(rayEye);

        Vector3f rayWorld = new Vector3f(rayWorld4.x, rayWorld4.y, rayWorld4.z);
        return rayWorld.normalize(); // Gibt die Richtung an, in die die Maus schaut
    }

    private void sendMatrixToShader(String uniformName, Matrix4f matrix) {
        int location = glGetUniformLocation(customShader.getProgramId(), uniformName);
        if (location != -1) {
            matrix.get(matrixBuffer);
            glUniformMatrix4fv(location, false, matrixBuffer);
        }
    }

    public void resize(int newWidth, int newHeight) {
        if (newWidth > 0 && newHeight > 0 && (newWidth != width || newHeight != height)) {
            this.width = newWidth; this.height = newHeight;
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, 0);
            glBindRenderbuffer(GL_RENDERBUFFER, rboId);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, width, height);
        }
    }

    public int getTextureId() { return textureId; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Shader getCustomShader() { return customShader; }
    public org.joml.Matrix4f getViewMatrix() {
        return viewMatrix;
    }

    public org.joml.Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }
    public EditorCamera getCamera() { return camera; }
    public Vector3f getCameraPos() { return camera.getPosition(); }

    private void setupCubeMesh() {
        // Ein einfacher 3D-Würfel mit farbigen Seiten (Position X,Y,Z und Farbe R,G,B)
        float[] cubeVertices = {
                // Rückseite
                -0.2f, -0.2f, -0.2f,  0.3f, 0.3f, 0.3f,   0.2f, -0.2f, -0.2f,  0.3f, 0.3f, 0.3f,   0.2f,  0.2f, -0.2f,  0.3f, 0.3f, 0.3f,
                0.2f,  0.2f, -0.2f,  0.3f, 0.3f, 0.3f,  -0.2f,  0.2f, -0.2f,  0.3f, 0.3f, 0.3f,  -0.2f, -0.2f, -0.2f,  0.3f, 0.3f, 0.3f,
                // Vorderseite (Blau angehaucht)
                -0.2f, -0.2f,  0.2f,  0.4f, 0.5f, 0.8f,   0.2f, -0.2f,  0.2f,  0.4f, 0.5f, 0.8f,   0.2f,  0.2f,  0.2f,  0.4f, 0.5f, 0.8f,
                0.2f,  0.2f,  0.2f,  0.4f, 0.5f, 0.8f,  -0.2f,  0.2f,  0.2f,  0.4f, 0.5f, 0.8f,  -0.2f, -0.2f,  0.2f,  0.4f, 0.5f, 0.8f,
                // Linke Seite
                -0.2f,  0.2f,  0.2f,  0.5f, 0.5f, 0.5f,  -0.2f,  0.2f, -0.2f,  0.5f, 0.5f, 0.5f,  -0.2f, -0.2f, -0.2f,  0.5f, 0.5f, 0.5f,
                -0.2f, -0.2f, -0.2f,  0.5f, 0.5f, 0.5f,  -0.2f, -0.2f,  0.2f,  0.5f, 0.5f, 0.5f,  -0.2f,  0.2f,  0.2f,  0.5f, 0.5f, 0.5f,
                // Rechte Seite
                0.2f,  0.2f,  0.2f,  0.6f, 0.6f, 0.6f,   0.2f,  0.2f, -0.2f,  0.6f, 0.6f, 0.6f,   0.2f, -0.2f, -0.2f,  0.6f, 0.6f, 0.6f,
                0.2f, -0.2f, -0.2f,  0.6f, 0.6f, 0.6f,   0.2f, -0.2f,  0.2f,  0.6f, 0.6f, 0.6f,   0.2f,  0.2f,  0.2f,  0.6f, 0.6f, 0.6f,
                // Unterseite
                -0.2f, -0.2f, -0.2f,  0.2f, 0.2f, 0.2f,   0.2f, -0.2f, -0.2f,  0.2f, 0.2f, 0.2f,   0.2f, -0.2f,  0.2f,  0.2f, 0.2f, 0.2f,
                0.2f, -0.2f,  0.2f,  0.2f, 0.2f, 0.2f,  -0.2f, -0.2f,  0.2f,  0.2f, 0.2f, 0.2f,  -0.2f, -0.2f, -0.2f,  0.2f, 0.2f, 0.2f,
                // Oberseite
                -0.2f,  0.2f, -0.2f,  0.7f, 0.7f, 0.7f,   0.2f,  0.2f, -0.2f,  0.7f, 0.7f, 0.7f,   0.2f,  0.2f,  0.2f,  0.7f, 0.7f, 0.7f,
                0.2f,  0.2f,  0.2f,  0.7f, 0.7f, 0.7f,  -0.2f,  0.2f,  0.2f,  0.7f, 0.7f, 0.7f,  -0.2f,  0.2f, -0.2f,  0.7f, 0.7f, 0.7f
        };

        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(cubeVertices.length);
        vertexBuffer.put(cubeVertices).flip();

        vaoId = glGenVertexArrays(); vboId = glGenBuffers();
        glBindVertexArray(vaoId); glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0); glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES); glEnableVertexAttribArray(1);
    }

    private void setupFramebuffer() {
        // 1. Das Framebuffer-Objekt (FBO) erstellen
        fboId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);

        // 2. Die Textur erstellen, auf die OpenGL das Spiel rendert
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        // Die Textur an den Framebuffer hängen
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureId, 0);

        // 3. Den Tiefenpuffer (Renderbuffer Object / RBO) für echtes 3D hinzufügen
        rboId = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rboId);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, width, height);

        // Den Tiefenpuffer an den Framebuffer hängen
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rboId);

        // Prüfen, ob der Framebuffer komplett und fehlerfrei ist
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("Fehler: Der 3D-Framebuffer konnte nicht initialisiert werden!");
        }

        // Framebuffer wieder entkoppeln (zurück zum Standard-Bildschirm)
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
}
