package com.engine.core.core;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.system.MemoryUtil;

public class Window {
    private long glfwWindowHandle;
    private final String title;
    private int width;
    private int height;

    public Window(String title, int width, int height) {
        this.title = title;
        this.width = width;
        this.height = height;
    }

    /**
     * Initialisiert GLFW auf dem MAIN-THREAD.
     */
    public void init() {
        // Fehler-Callback einrichten
        GLFWErrorCallback.createPrint(System.err).set();

        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("[Window] GLFW konnte nicht initialisiert werden!");
        }

        // Fenster-Konfiguration (OpenGL 3.3 Core Profile)
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);

        // Fenster erstellen
        glfwWindowHandle = GLFW.glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL);
        if (glfwWindowHandle == MemoryUtil.NULL) {
            throw new RuntimeException("[Window] GLFW-Fenster konnte nicht erstellt werden!");
        }

        // Callback für Größenänderung einrichten
        GLFW.glfwSetFramebufferSizeCallback(glfwWindowHandle, (window, newWidth, newHeight) -> {
            this.width = newWidth;
            this.height = newHeight;
            // Die eigentliche glViewport-Anpassung muss via ThreadManager in die Render-Queue geschoben werden!
            ThreadManager.getInstance().runOnRenderThread(() -> {
                org.lwjgl.opengl.GL11.glViewport(0, 0, newWidth, newHeight);
            });
        });

        // WICHTIG FÜR MULTITHREADING:
        // Den Kontext vom Main-Thread LÖSEN, damit der Render-Thread ihn gleich übernehmen darf!
        GLFW.glfwMakeContextCurrent(MemoryUtil.NULL);

        System.out.println("[Window] Fenster erfolgreich auf dem Main-Thread vorbereitet.");
    }

    /**
     * Macht das Fenster sichtbar. Wird aufgerufen, sobald der Render-Thread bereit ist.
     */
    public void show() {
        GLFW.glfwShowWindow(glfwWindowHandle);
    }

    /**
     * Prüft, ob das Fenster geschlossen werden soll. (Wird vom Main-Thread abgefragt)
     */
    public boolean shouldClose() {
        return GLFW.glfwWindowShouldClose(glfwWindowHandle);
    }

    /**
     * Gibt Ressourcen frei.
     */
    public void destroy() {
        GLFW.glfwDestroyWindow(glfwWindowHandle);
        GLFW.glfwTerminate();
        if (GLFW.glfwSetErrorCallback(null) != null) {
            GLFW.glfwSetErrorCallback(null).free();
        }
    }

    // Getter
    public long getHandle() { return glfwWindowHandle; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
