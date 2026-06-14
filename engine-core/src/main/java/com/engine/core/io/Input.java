package com.engine.core.io;

import org.lwjgl.glfw.GLFW;

public class Input {
    private static Input instance;

    // Arrays zur Speicherung des aktuellen Zustands (true = gedrückt)
    private final boolean[] keyPressed = new boolean[GLFW.GLFW_KEY_LAST + 1];
    private final boolean[] mouseButtonPressed = new boolean[GLFW.GLFW_MOUSE_BUTTON_LAST + 1];

    // Mausposition und Scroll-Daten
    private double mouseX, mouseY;
    private double scrollX, scrollY;
    private boolean isDragging;

    private Input() {}

    public static Input getInstance() {
        if (instance == null) {
            instance = new Input();
        }
        return instance;
    }

    /**
     * Registriert die Callbacks für Tastatur und Maus an einem GLFW-Fenster.
     * WIRD EXKLUSIV AUF DEM MAIN-THREAD AUFGERUFEN!
     */
    public void registerCallbacks(long windowHandle) {
        // 1. Tastatur-Callback
        GLFW.glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (key >= 0 && key < keyPressed.length) {
                if (action == GLFW.GLFW_PRESS) {
                    keyPressed[key] = true;
                } else if (action == GLFW.GLFW_RELEASE) {
                    keyPressed[key] = false;
                }
            }
        });

        // 2. Maus-Klick-Callback
        GLFW.glfwSetMouseButtonCallback(windowHandle, (window, button, action, mods) -> {
            if (button >= 0 && button < mouseButtonPressed.length) {
                if (action == GLFW.GLFW_PRESS) {
                    mouseButtonPressed[button] = true;
                } else if (action == GLFW.GLFW_RELEASE) {
                    mouseButtonPressed[button] = false;
                    isDragging = false;
                }
            }
        });

        // 3. Maus-Positions-Callback
        GLFW.glfwSetCursorPosCallback(windowHandle, (window, xpos, ypos) -> {
            mouseX = xpos;
            mouseY = ypos;

            // Prüfen, ob die Maus gezogen wird (z. B. linke Maustaste gedrückt gehalten)
            if (mouseButtonPressed[GLFW.GLFW_MOUSE_BUTTON_LEFT] || mouseButtonPressed[GLFW.GLFW_MOUSE_BUTTON_RIGHT]) {
                isDragging = true;
            }
        });

        // 4. Maus-Scroll-Callback
        GLFW.glfwSetScrollCallback(windowHandle, (window, xoffset, yoffset) -> {
            scrollX = xoffset;
            scrollY = yoffset;
        });
    }

    /**
     * Setzt Scroll-Daten am Ende jedes Frames zurück, da diese als Events zählen.
     */
    public void endFrame() {
        scrollX = 0;
        scrollY = 0;
    }

    // --- Faden-sichere Abfrage-Methoden für deine Spiellogik (Main-Thread) ---

    public boolean isKeyPressed(int keyCode) {
        if (keyCode >= 0 && keyCode < keyPressed.length) {
            return keyPressed[keyCode];
        }
        return false;
    }

    public boolean isMouseButtonPressed(int buttonCode) {
        if (buttonCode >= 0 && buttonCode < mouseButtonPressed.length) {
            return mouseButtonPressed[buttonCode];
        }
        return false;
    }

    public double getMouseX() { return mouseX; }
    public double getMouseY() { return mouseY; }
    public double getScrollX() { return scrollX; }
    public double getScrollY() { return scrollY; }
    public boolean isDragging() { return isDragging; }
}
