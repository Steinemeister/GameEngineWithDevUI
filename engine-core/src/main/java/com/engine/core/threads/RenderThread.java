package com.engine.core.threads;

import com.engine.core.core.ThreadManager;
import com.engine.core.core.Window;
import com.engine.core.renderer.RenderSnapshot;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import java.util.concurrent.atomic.AtomicReference;

public class RenderThread implements Runnable {
    private final Window window;

    // Faden-sicherer Container für den aktuellsten Welt-Snapshot
    private final AtomicReference<RenderSnapshot> nextSnapshot = new AtomicReference<>(null);
    private RenderSnapshot activeSnapshot = null;

    public RenderThread(Window window) {
        this.window = window;
    }

    /**
     * Wird vom Main-Thread aufgerufen, um der Grafikkarte neue Modelldaten zu übergeben.
     */
    public void updateSnapshot(RenderSnapshot snapshot) {
        nextSnapshot.set(snapshot);
    }

    @Override
    public void run() {
        // 1. OpenGL-Kontext an DIESEN Thread binden
        GLFW.glfwMakeContextCurrent(window.getHandle());

        // WICHTIG: Erstellt die internen Funktionszeiger für OpenGL (z.B. glGenBuffers, glCompileShader)
        GL.createCapabilities();

        // V-Sync aktivieren (1) oder deaktivieren (0)
        GLFW.glfwSwapInterval(1);

        // Standard OpenGL-States setzen
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glClearColor(0.1f, 0.1f, 0.12f, 1.0f);

        // Fenster erst jetzt sichtbar machen, um "weißes Aufblitzen" beim Start zu verhindern
        window.show();

        System.out.println("[Render-Thread] OpenGL-Kontext erfolgreich übernommen. Render-Loop gestartet.");

        // Temporäre Platzhalter für spätere Subsysteme
        // Shader shader = new Shader("default.glsl");
        // RenderBatch batch = new RenderBatch();

        // 2. Der eigentliche Render-Loop
        while (ThreadManager.getInstance().isEngineRunning()) {
            // A) Externe Grafikbefehle (Texturen laden, FBO-Größen etc.) abarbeiten
            ThreadManager.getInstance().executeRenderQueue();

            // B) Den neuesten Welt-Snapshot atomar abholen
            RenderSnapshot freshSnapshot = nextSnapshot.getAndSet(null);
            if (freshSnapshot != null) {
                activeSnapshot = freshSnapshot; // Alten Snapshot durch den neuen ersetzen
            }

            // Bildpuffer leeren
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            // C) Den Welt-Snapshot zeichnen
            if (activeSnapshot != null) {
                renderScene(activeSnapshot);
            }

            // D) Bildpuffer tauschen (Double Buffering der Grafikkarte)
            GLFW.glfwSwapBuffers(window.getHandle());
        }

        System.out.println("[Render-Thread] Render-Loop beendet. Kontext wird freigegeben.");
    }

    /**
     * Iteriert durch den Snapshot und übergibt die Daten an die Grafikkarte.
     */
    private void renderScene(RenderSnapshot snapshot) {
        // HIER WIRD SPÄTER DEIN RENDER-BATCH ANGEPROCHEN:
        // batch.begin(snapshot.cameraPosition, snapshot.cameraZoom);

        for (RenderSnapshot.RenderElement element : snapshot.elements) {
            // Nutze die reinen Daten aus dem Snapshot (Kein ECS-Zugriff!)
            // Vector2f pos = element.position;
            // Vector4f color = element.color;
            // String textureId = element.textureUuid;

            // batch.drawQuad(pos, element.scale, element.rotation, color, textureId);
        }

        // batch.end();
    }
}
