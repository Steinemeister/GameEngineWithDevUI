package com.engine.core;

import com.engine.core.rendering.RenderPipeline;
import com.engine.core.rendering.ScreenQuad;
import com.engine.scene.GameObject;
import com.engine.scene.Material;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

public class GameWindow {

    private RenderPipeline pipeline;
    private ScreenQuad screenQuad;
    private Shader lightingShader;

    private int width = 800;
    private int height = 600;
    private int vaoId, vboId;

    private final EditorCamera camera = new EditorCamera();
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();
    private final Matrix4f modelMatrix = new Matrix4f();
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    private String cachedVertexShaderSource;

    public void init() {
        // Pipeline Builder und ScreenQuad laden
        pipeline = new RenderPipeline(width, height);
        screenQuad = new ScreenQuad();
        screenQuad.init();

        setupCubeMesh();

        // --- DER FIX: AUTOMATISCHES ERSTELLEN DER SYSTEM-SHADER ---
        String shaderFolder = "projects/StandardShaders";
        java.io.File folder = new java.io.File(shaderFolder);
        if (!folder.exists()) {
            folder.mkdirs(); // Erstellt den Ordner, falls er fehlt
        }

        java.io.File vertFile = new java.io.File(shaderFolder + "/geometry_pass.vert");
        java.io.File fragFile = new java.io.File(shaderFolder + "/terrain_triplanar.frag");

        // Vertex-Shader schreiben, falls nicht da
        if (!vertFile.exists()) {
            try (java.io.FileWriter writer = new java.io.FileWriter(vertFile)) {
                writer.write("""
                    #version 330 core
                    layout (location = 0) in vec3 aPos;
                    layout (location = 1) in vec3 aNormal;
                    out vec3 FragPos;
                    out vec3 Normal;
                    uniform mat4 uProjection;
                    uniform mat4 uView;
                    uniform mat4 uModel;
                    void main() {
                        vec4 worldPos = uModel * vec4(aPos, 1.0);
                        FragPos = worldPos.xyz;
                        Normal = mat3(transpose(inverse(uModel))) * aNormal;
                        gl_Position = uProjection * uView * worldPos;
                    }
                """);
            } catch (java.io.IOException e) {
                System.err.println("Kritischer Fehler beim Erstellen von geometry_pass.vert: " + e.getMessage());
            }
        }

        // Fragment-Shader schreiben, falls nicht da
        if (!fragFile.exists()) {
            try (java.io.FileWriter writer = new java.io.FileWriter(fragFile)) {
                writer.write("""
                    #version 330 core
                    layout (location = 0) out vec3 gPosition;
                    layout (location = 1) out vec3 gNormal;
                    layout (location = 2) out vec4 gAlbedo;
                    in vec3 FragPos;
                    in vec3 Normal;
                    void main() {
                        gPosition = FragPos;
                        gNormal = normalize(Normal);
                        vec3 blendWeights = abs(gNormal);
                        blendWeights = blendWeights / (blendWeights.x + blendWeights.y + blendWeights.z);
                        vec3 colorX = vec3(0.5, 0.4, 0.3);
                        vec3 colorY = vec3(0.2, 0.6, 0.2);
                        vec3 colorZ = vec3(0.4, 0.4, 0.4);
                        vec3 finalAlbedo = colorX * blendWeights.x + colorY * blendWeights.y + colorZ * blendWeights.z;
                        gAlbedo = vec4(finalAlbedo, 1.0);
                    }
                """);
            } catch (java.io.IOException e) {
                System.err.println("Kritischer Fehler beim Erstellen von terrain_triplanar.frag: " + e.getMessage());
            }
        }

        // 1. Den Geometrie-Vertex-Shader von der Festplatte einlesen
        try {
            cachedVertexShaderSource = java.nio.file.Files.readString(java.nio.file.Paths.get(shaderFolder + "/geometry_pass.vert"));
        } catch (java.io.IOException e) {
            cachedVertexShaderSource = "#version 330 core\nlayout(location=0)in vec3 aPos;uniform mat4 uProjection;uniform mat4 uView;uniform mat4 uModel;void main(){gl_Position=uProjection*uView*uModel*vec4(aPos,1.0);}";
        }

        // 2. Den globalen Lighting-Shader bauen (bleibt unverändert)
        lightingShader = new Shader("""
            #version 330 core
            layout (location = 0) in vec3 aPos;
            layout (location = 1) in vec2 aTexCoords;
            out vec2 TexCoords;
            void main() { TexCoords = aTexCoords; gl_Position = vec4(aPos, 1.0); }
        """, """
            #version 330 core
            out vec4 FragColor;
            in vec2 TexCoords;
            uniform sampler2D gPosition;
            uniform sampler2D gNormal;
            uniform sampler2D gAlbedo;
            void main() {
                vec3 fragPos = texture(gPosition, TexCoords).rgb;
                vec3 normal = texture(gNormal, TexCoords).rgb;
                vec3 albedo = texture(gAlbedo, TexCoords).rgb;
                vec3 lightDir = normalize(vec3(0.5, 1.0, 0.3));
                float diffuseFactor = max(dot(normal, lightDir), 0.15);
                FragColor = vec4(albedo * diffuseFactor, 1.0);
            }
        """);
    }

    public void render(boolean toFramebuffer, int targetTextureFboId, List<GameObject> objects) {
        // --- PASS 1: DEFERRED GEOMETRY PASS ---
        pipeline.beginGeometryPass();

        float aspectRatio = (float) width / (float) height;
        projectionMatrix.setPerspective((float) Math.toRadians(45.0f), aspectRatio, 0.1f, 100.0f);
        viewMatrix.setLookAt(camera.getPosition(), camera.getTarget(), camera.getUp());

        glBindVertexArray(vaoId);

        for (GameObject obj : objects) {
            if (obj.material == null) {
                // Falls das Objekt kein Material besitzt, verpassen wir ihm den Tri-Planar-Shader
                obj.material = new Material("projects/StandardShaders/terrain_triplanar.frag");
            }

            // Jedes Objekt kompiliert und bindet seinen eigenen, maßgeschneiderten Shader!
            obj.material.compile(cachedVertexShaderSource);
            obj.material.bind();

            // Matrizen in den spezifischen Shader des Materials injizieren
            sendMatrixToShader(obj.material.getShaderProgram().getProgramId(), "uProjection", projectionMatrix);
            sendMatrixToShader(obj.material.getShaderProgram().getProgramId(), "uView", viewMatrix);

            modelMatrix.identity().translate(obj.posX, obj.posY, obj.posZ);
            sendMatrixToShader(obj.material.getShaderProgram().getProgramId(), "uModel", modelMatrix);

            glDrawArrays(GL_TRIANGLES, 0, 36);
            obj.material.unbind();
        }

        glBindVertexArray(0);
        pipeline.endGeometryPass();

        // --- PASS 2: LIGHTING & POST PROCESSING PASS ---
        lightingShader.bind();

        // Sampler-IDs den Textureinheiten zuweisen
        glUniform1i(glGetUniformLocation(lightingShader.getProgramId(), "gPosition"), 0);
        glUniform1i(glGetUniformLocation(lightingShader.getProgramId(), "gNormal"), 1);
        glUniform1i(glGetUniformLocation(lightingShader.getProgramId(), "gAlbedo"), 2);

        // Das berechnete Deferred-Bild in das Ziel-FBO (Ihren Viewport) werfen
        pipeline.renderLightingAndPostProcess(toFramebuffer ? targetTextureFboId : 0, screenQuad.getVaoId());

        lightingShader.unbind();
    }

    private void sendMatrixToShader(int programId, String uniformName, Matrix4f matrix) {
        int location = glGetUniformLocation(programId, uniformName);
        if (location != -1) {
            matrix.get(matrixBuffer);
            glUniformMatrix4fv(location, false, matrixBuffer);
        }
    }

    public void resize(int newWidth, int newHeight) {
        if (newWidth > 0 && newHeight > 0 && (newWidth != width || newHeight != height)) {
            this.width = newWidth;
            this.height = newHeight;
            pipeline.resize(width, height);
        }
    }

    public org.joml.Vector3f calculateMouseRay(float mouseX, float mouseY) {
        // 1. Koordinaten in normalisierte Bildschirmkoordinaten (-1 bis 1) konvertieren
        float x = (2.0f * mouseX) / width - 1.0f;
        float y = 1.0f - (2.0f * mouseY) / height;

        org.joml.Vector4f rayClip = new org.joml.Vector4f(x, y, -1.0f, 1.0f);

        // 2. Zurück durch die Projektions-Matrix (3D-Perspektive) rechnen
        org.joml.Matrix4f invProjection = new org.joml.Matrix4f(projectionMatrix).invert();
        org.joml.Vector4f rayEye = invProjection.transform(rayClip);
        rayEye.z = -1.0f;
        rayEye.w = 0.0f;

        // 3. Zurück durch die View-Matrix (Kamera-Ausrichtung) rechnen
        org.joml.Matrix4f invView = new org.joml.Matrix4f(viewMatrix).invert();
        org.joml.Vector4f rayWorld4 = invView.transform(rayEye);

        org.joml.Vector3f rayWorld = new org.joml.Vector3f(rayWorld4.x, rayWorld4.y, rayWorld4.z);
        return rayWorld.normalize(); // Gibt den Richtungsvektor aus, in den die Maus blickt
    }

    public int getTextureId() { return pipeline.getGBuffer().getAlbedoTexture(); }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public EditorCamera getCamera() { return camera; }
    public Vector3f getCameraPos() { return camera.getPosition(); }
    public String getCachedVertexShaderSource() { return cachedVertexShaderSource; }
    public org.joml.Matrix4f getViewMatrix() {
        return this.viewMatrix;
    }
    public org.joml.Matrix4f getProjectionMatrix() {
        return this.projectionMatrix;
    }

    private void setupCubeMesh() {
        float[] cubeVertices = {
                // Positionen (X,Y,Z)   // Normalen-Richtungsvektoren (NX, NY, NZ)
                -0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f,   0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f,   0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f,
                0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f,  -0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f,  -0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f,
                -0.5f, -0.5f,  0.5f,  0.0f,  0.0f,  1.0f,   0.5f, -0.5f,  0.5f,  0.0f,  0.0f,  1.0f,   0.5f,  0.5f,  0.5f,  0.0f,  0.0f,  1.0f,
                0.5f,  0.5f,  0.5f,  0.0f,  0.0f,  1.0f,  -0.5f,  0.5f,  0.5f,  0.0f,  0.0f,  1.0f,  -0.5f, -0.5f,  0.5f,  0.0f,  0.0f,  1.0f,
                -0.5f,  0.5f,  0.5f, -1.0f,  0.0f,  0.0f,  -0.5f,  0.5f, -0.5f, -1.0f,  0.0f,  0.0f,  -0.5f, -0.5f, -0.5f, -1.0f,  0.0f,  0.0f,
                -0.5f, -0.5f, -0.5f, -1.0f,  0.0f,  0.0f,  -0.5f, -0.5f,  0.5f, -1.0f,  0.0f,  0.0f,  -0.5f,  0.5f,  0.5f, -1.0f,  0.0f,  0.0f,
                0.5f,  0.5f,  0.5f,  1.0f,  0.0f,  0.0f,   0.5f,  0.5f, -0.5f,  1.0f,  0.0f,  0.0f,   0.5f, -0.5f, -0.5f,  1.0f,  0.0f,  0.0f,
                0.5f, -0.5f, -0.5f,  1.0f,  0.0f,  0.0f,   0.5f, -0.5f,  0.5f,  1.0f,  0.0f,  0.0f,   0.5f,  0.5f,  0.5f,  1.0f,  0.0f,  0.0f,
                -0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f,   0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f,   0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f,
                0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f,  -0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f,  -0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f,
                -0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f,   0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f,   0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f,
                0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f,  -0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f,  -0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f
        };

        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(cubeVertices.length);
        vertexBuffer.put(cubeVertices).flip();

        vaoId = glGenVertexArrays(); vboId = glGenBuffers();
        glBindVertexArray(vaoId); glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0); glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES); glEnableVertexAttribArray(1);
    }
}
