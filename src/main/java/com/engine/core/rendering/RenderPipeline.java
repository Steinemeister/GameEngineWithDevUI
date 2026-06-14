package com.engine.core.rendering;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL40.*;

public class RenderPipeline {
    private GBuffer gBuffer;
    private int indirectBufferId;
    private int instanceVboId; // Für die Instanced-Matrizen

    public RenderPipeline(int width, int height) {
        this.gBuffer = new GBuffer(width, height);

        // Indirect Buffer (DIB) auf der GPU generieren
        this.indirectBufferId = glGenBuffers();
    }

    /**
     * Schritt 1: Geometry Pass (Deferred) aktivieren.
     * Alle folgenden 3D-Render-Befehle schreiben nun in den G-Buffer statt auf den Schirm.
     */
    public void beginGeometryPass() {
        gBuffer.bind();
        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    /**
     * High-End Zeichenbefehl: Rendert tausende Objekte via Multi-Draw-Indirect.
     */
    public void drawIndirect(RenderCommand[] commands) {
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectBufferId);

        // Java-Befehls-Array in Byte-Buffer für Grafikkarte konvertieren
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(commands.length * 16).order(ByteOrder.nativeOrder());
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        for (RenderCommand cmd : commands) {
            intBuffer.put(cmd.toIntArray());
        }
        intBuffer.flip();

        // Befehle an die GPU streamen
        glBufferData(GL_DRAW_INDIRECT_BUFFER, byteBuffer, GL_DYNAMIC_DRAW);

        // Der magische Indirect-Aufruf: Die GPU übernimmt das Loopen komplett eigenständig!
        // Parameter: (Modus, Start-Offset, Anzahl der Zeichenbefehle, Byte-Abstand zwischen Befehlen)
        glDrawArraysIndirect(GL_TRIANGLES, 0);

        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
    }

    public void endGeometryPass() {
        gBuffer.unbind();
    }

    /**
     * Schritt 2: Lighting & Post-Processing Pass.
     * Wir rendern ein flaches Vollbild-Quad. Der Shader liest die Texturen des
     * G-Buffers aus und berechnet die Beleuchtung und Effekte (Bloom, FXAA) pro Pixel.
     */
    public void renderLightingAndPostProcess(int targetFramebufferId, int vaoScreenQuadId) {
        // Auf Ziel-Framebuffer umschalten (z.B. die Editor-Viewport-Textur)
        glBindFramebuffer(GL_FRAMEBUFFER, targetFramebufferId);

        glDisable(GL_DEPTH_TEST); // Tiefe wird für 2D-Post-Processing nicht benötigt
        glClear(GL_COLOR_BUFFER_BIT);

        // G-Buffer Texturen an die Textur-Einheiten der GPU binden
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, gBuffer.getPositionTexture());

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, gBuffer.getNormalTexture());

        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, gBuffer.getAlbedoTexture());

        // Das Vollbild-Quad zeichnen
        glBindVertexArray(vaoScreenQuadId);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void resize(int width, int height) {
        gBuffer.resize(width, height);
    }

    public GBuffer getGBuffer() { return gBuffer; }
}
