package com;

import com.engine.development.EditorPanel;
import com.engine.development.shaderNodeEditor.NodeEditorPanel;
import com.engine.development.ShaderEditorPanel;
import com.engine.development.ViewportPanel;
import com.engine.rendering.Shader;
import imgui.app.Application;
import imgui.app.Configuration;
import imgui.type.ImString;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL30.*;

public class Main extends Application {
    public static final boolean IS_EDITOR = true;

    private int fboId, textureId, rboId;
    private int viewportWidth = 800;
    private int viewportHeight = 600;

    private Shader customShader;
    private int vaoId, vboId;
    private float colorPulse = 0.0f;

    private final String vertexShaderSource = """
        #version 330 core
        layout (location = 0) in vec3 aPos;
        layout (location = 1) in vec3 aColor;
        out vec3 vertexColor;
        void main() {
            gl_Position = vec4(aPos, 1.0);
            vertexColor = aColor;
        }
    """;

    private final ImString fragmentShaderInput = new ImString(4000);

    // Liste aller GUI-Fenster
    private final List<EditorPanel> panels = new ArrayList<>();

    @Override
    protected void configure(Configuration config) {
        config.setTitle("Java Game Engine - Clean Architecture");
        config.setWidth(1400);
        config.setHeight(800);
    }

    @Override
    protected void init(Configuration config) {
        super.init(config);
        GL.createCapabilities();

        if (IS_EDITOR) {
            setupFramebuffer();

            // Shader-Text initialisieren
            String initialShaderCode = """
            #version 330 core
            in vec3 vertexColor;
            
            out vec4 FragColor;
            
            uniform float uColorPulse;
            
            void main() {
                FragColor = vec4(vertexColor.r * uColorPulse, vertexColor.g, vertexColor.b, 1.0);
            }
            """;
            fragmentShaderInput.set(initialShaderCode);

            // GUI-Panels registrieren und sich selbst (this) übergeben
            panels.add(new ViewportPanel(this));
            panels.add(new ShaderEditorPanel(this, fragmentShaderInput));
            panels.add(new NodeEditorPanel(this));
        }

        // Shader und Mesh-Daten laden (Core Engine)
        customShader = new Shader(vertexShaderSource, IS_EDITOR ? fragmentShaderInput.get() : "/* Ihr Release Shader */");
        setupMesh();
    }

    private void setupMesh() {
        float[] vertices = {
                0.0f,  0.5f, 0.0f,    1.0f, 0.0f, 0.0f,
                -0.5f, -0.5f, 0.0f,    0.0f, 1.0f, 0.0f,
                0.5f, -0.5f, 0.0f,    0.0f, 0.0f, 1.0f
        };
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices).flip();

        vaoId = glGenVertexArrays();
        vboId = glGenBuffers();
        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
    }

    public void compileShader(String fragmentCode) {
        customShader.compileAndLink(vertexShaderSource, fragmentCode);
    }

    private void renderGameScene() {
        glClearColor(0.08f, 0.09f, 0.12f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        customShader.bind();
        colorPulse = (float) (Math.sin(System.currentTimeMillis() * 0.003) * 0.5 + 0.5);
        customShader.setUniform("uColorPulse", colorPulse);

        glBindVertexArray(vaoId);
        glDrawArrays(GL_TRIANGLES, 0, 3);

        glBindVertexArray(0);
        customShader.unbind();
    }

    @Override
    public void process() {
        if (IS_EDITOR) {
            // Im Editor-Modus: In den unsichtbaren Buffer zeichnen
            glBindFramebuffer(GL_FRAMEBUFFER, fboId);
            glViewport(0, 0, viewportWidth, viewportHeight);

            renderGameScene();

            glBindFramebuffer(GL_FRAMEBUFFER, 0);

            // Alle registrierten UI-Fenster zeichnen
            for (EditorPanel panel : panels) {
                panel.updateAndRender();
            }
        } else {
            // Im Spiel-Modus: Direkt auf den ganzen Bildschirm zeichnen
            // Hier greifen wir auf die Fenstergröße der App zu
            glViewport(0, 0, 1400, 800);
            renderGameScene();
        }
    }

    // --- GETTER & UTILITIES FÜR DIE PANELS ---
    public int getTextureId() { return textureId; }
    public int getViewportWidth() { return viewportWidth; }
    public int getViewportHeight() { return viewportHeight; }
    public Shader getCustomShader() { return customShader; }

    private void setupFramebuffer() {
        fboId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, viewportWidth, viewportHeight, 0, GL_RGB, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureId, 0);
        rboId = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rboId);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, viewportWidth, viewportHeight);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rboId);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void handleViewportResize(int currentW, int currentH) {
        if (currentW > 0 && currentH > 0 && (currentW != viewportWidth || currentH != viewportHeight)) {
            viewportWidth = currentW;
            viewportHeight = currentH;
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, viewportWidth, viewportHeight, 0, GL_RGB, GL_UNSIGNED_BYTE, 0);
            glBindRenderbuffer(GL_RENDERBUFFER, rboId);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, viewportWidth, viewportHeight);
        }
    }

    public static void main(String[] args) {
        launch(new Main());
    }
}
