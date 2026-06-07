package com;

import com.engine.rendering.Shader;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.app.Application;
import imgui.app.Configuration;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImString;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;

import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL30.*;

public class Main extends Application {
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

    // --- DIE RETTUNG: Ein pures Byte-Array statt ImString ---
    // Wir reservieren 4000 Bytes Speicherplatz für unseren Text.
    private final byte[] shaderTextBuffer = new byte[4000];

    @Override
    protected void configure(Configuration config) {
        config.setTitle("Java Game Engine Editor - Fixed Input Engine");
        config.setWidth(1400);
        config.setHeight(800);
    }

    @Override
    protected void init(Configuration config) {
        super.init(config);
        GL.createCapabilities();
        setupFramebuffer();

        // Start-Code in das Byte-Array hineinschreiben
        String initialShaderCode = """
            #version 330 core
            in vec3 vertexColor;
            out vec4 FragColor;
            
            uniform float uColorPulse;
            
            void main() {
                FragColor = vec4(vertexColor.r * uColorPulse, vertexColor.g, vertexColor.b, 1.0);
            }
        """;
        byte[] initialBytes = initialShaderCode.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(initialBytes, 0, shaderTextBuffer, 0, Math.min(initialBytes.length, shaderTextBuffer.length));

        // Shader initial kompilieren
        customShader = new Shader(vertexShaderSource, getShaderStringFromBuffer());

        // Dreiecks-Setup
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

    private void renderSceneToFramebuffer() {
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);
        glViewport(0, 0, viewportWidth, viewportHeight);
        glClearColor(0.08f, 0.09f, 0.12f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        customShader.bind();
        colorPulse = (float) (Math.sin(System.currentTimeMillis() * 0.003) * 0.5 + 0.5);
        customShader.setUniform("uColorPulse", colorPulse);

        glBindVertexArray(vaoId);
        glDrawArrays(GL_TRIANGLES, 0, 3);

        glBindVertexArray(0);
        customShader.unbind();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    @Override
    public void process() {
        renderSceneToFramebuffer();

        // 1. Viewport-Fenster
        ImGui.begin("Spiel-Ansicht (Viewport)");
        ImVec2 windowSize = ImGui.getContentRegionAvail();
        handleViewportResize((int) windowSize.x, (int) windowSize.y);
        ImGui.image(textureId, viewportWidth, viewportHeight, 0, 1, 1, 0);
        ImGui.end();

        // 2. Live-Code-Editor Fenster
        ImGui.begin("Live GLSL Fragment Shader Editor");
        ImGui.text("Nutzen Sie Ihre Tastatur nun völlig frei für jegliche Zeichen!");
        ImGui.separator();

        ImVec2 inputSize = new ImVec2(-1, 350);

        // Wir nutzen die überladene Methode von ImGui, die ein rohes byte[] schluckt.
        // Das löst die Tastatur-Blockade augenblicklich!
        ImGui.inputTextMultiline("##ShaderCode", shaderTextBuffer, (int) inputSize.x, (int) inputSize.y);

        ImGui.separator();

        // Button zum Kompilieren
        if (ImGui.button("Shader kompilieren", -1, 40)) {
            customShader.compileAndLink(vertexShaderSource, getShaderStringFromBuffer());
        }

        ImGui.separator();

        if (customShader.hasErrors()) {
            ImGui.textColored(1.0f, 0.3f, 0.3f, 1.0f, "Kompilierungsfehler detektiert:");
            ImGui.textWrapped(customShader.getErrorMessage());
        } else {
            ImGui.textColored(0.3f, 1.0f, 0.3f, 1.0f, "Shader Status: Bereit und aktiv auf der GPU.");
        }

        ImGui.end();
    }

    /**
     * Hilfsfunktion, um das rohe C++-Byte-Array in einen sauberen Java-String zu konvertieren.
     * Schneidet automatisch nach dem Nullterminator (\0) ab.
     */
    private String getShaderStringFromBuffer() {
        String rawString = new String(shaderTextBuffer, StandardCharsets.UTF_8);
        int nullTerminatorIdx = rawString.indexOf('\0');
        if (nullTerminatorIdx != -1) {
            return rawString.substring(0, nullTerminatorIdx);
        }
        return rawString;
    }

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
        glViewport(0,0, viewportWidth, viewportHeight);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, viewportWidth, viewportHeight);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rboId);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private void handleViewportResize(int currentW, int currentH) {
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
