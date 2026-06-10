package com.engine.core;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

public class GameWindow {

    private int fboId, textureId, rboId;
    private int width = 800;
    private int height = 600;

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

    public void init() {
        setupFramebuffer();
        setupMesh();

        // Standard-Shader beim Start kompilieren
        customShader = new Shader(vertexShaderSource, """
            #version 330 core
            out vec4 FragColor;
            void main() { FragColor = vec4(1.0, 1.0, 1.0, 1.0); }
        """);
    }

    public void compileShader(String fragmentCode) {
        customShader.compileAndLink(vertexShaderSource, fragmentCode);
    }

    /**
     * Rendert die eigentliche Spielszene.
     * @param toFramebuffer Wenn true, wird in die Editor-Textur gerendert, sonst auf den Bildschirm.
     */
    public void render(boolean toFramebuffer) {
        if (toFramebuffer) {
            glBindFramebuffer(GL_FRAMEBUFFER, fboId);
        }

        glViewport(0, 0, width, height);
        glClearColor(0.08f, 0.09f, 0.12f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        customShader.bind();
        colorPulse = (float) (Math.sin(System.currentTimeMillis() * 0.003) * 0.5 + 0.5);
        customShader.setUniform("uColorPulse", colorPulse);

        glBindVertexArray(vaoId);
        glDrawArrays(GL_TRIANGLES, 0, 3);

        glBindVertexArray(0);
        customShader.unbind();

        if (toFramebuffer) {
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }
    }

    public void resize(int newWidth, int newHeight) {
        if (newWidth > 0 && newHeight > 0 && (newWidth != width || newHeight != height)) {
            this.width = newWidth;
            this.height = newHeight;
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, 0);
            glBindRenderbuffer(GL_RENDERBUFFER, rboId);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, width, height);
        }
    }

    // --- GETTER ---
    public int getTextureId() { return textureId; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Shader getCustomShader() { return customShader; }

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

    private void setupFramebuffer() {
        fboId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureId, 0);
        rboId = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rboId);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rboId);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
}
