package com.engine.core.rendering;

import static org.lwjgl.opengl.GL30.*;

public class GBuffer {
    private int fboId;
    private int positionTexture;
    private int normalTexture;
    private int albedoTexture;
    private int depthRenderbuffer;
    private int width, height;

    public GBuffer(int width, int height) {
        resize(width, height);
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;

        if (fboId != 0) {
            glDeleteFramebuffers(fboId);
            glDeleteTextures(positionTexture);
            glDeleteTextures(normalTexture);
            glDeleteTextures(albedoTexture);
            glDeleteRenderbuffers(depthRenderbuffer);
        }

        fboId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);

        // 1. Positions-Textur (Hohe Präzision: 16-Bit Float pro Kanal)
        positionTexture = createTexture(GL_RGBA16F, GL_RGBA, GL_FLOAT);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, positionTexture, 0);

        // 2. Normalen-Textur (16-Bit Float für genaue Lichtberechnungen)
        normalTexture = createTexture(GL_RGBA16F, GL_RGBA, GL_FLOAT);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, normalTexture, 0);

        // 3. Albedo/Farbe-Textur (Standard 8-Bit pro Kanal)
        albedoTexture = createTexture(GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT2, GL_TEXTURE_2D, albedoTexture, 0);

        // OpenGL sagen, in welche Texturen gleichzeitig geschrieben werden soll
        int[] attachments = { GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2 };
        glDrawBuffers(attachments);

        // 4. Tiefenpuffer hinzufügen
        depthRenderbuffer = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, depthRenderbuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthRenderbuffer);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("G-Buffer unvollständig!");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private int createTexture(int internalFormat, int format, int type) {
        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        return textureId;
    }

    public void bind() { glBindFramebuffer(GL_FRAMEBUFFER, fboId); }
    public void unbind() { glBindFramebuffer(GL_FRAMEBUFFER, 0); }

    public int getPositionTexture() { return positionTexture; }
    public int getNormalTexture() { return normalTexture; }
    public int getAlbedoTexture() { return albedoTexture; }
}
