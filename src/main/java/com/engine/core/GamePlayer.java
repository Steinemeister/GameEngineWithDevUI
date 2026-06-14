package com.engine.core;

import com.engine.scene.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class GamePlayer {
    private static final Vector3f cameraPos = new Vector3f(0.0f, 1.0f, 4.0f);
    private static final Vector3f forward = new Vector3f(0.0f, 0.0f, -1.0f);
    private static final Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);

    private static float yaw = -90.0f;
    private static float pitch = 0.0f;
    private static final float speed = 0.06f;
    private static final float sensitivity = 0.10f;

    // Für das manuelle Tracking der Teleportation
    private static double lastMouseX = 0;
    private static double lastMouseY = 0;
    private static boolean firstMouse = true;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Fehler: Kein Projektpfad uebergeben!");
            System.exit(1);
        }

        String projectPath = args[0];

        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("GLFW konnte nicht initialisiert werden.");
        }

        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_TRUE);

        long window = GLFW.glfwCreateWindow(1280, 720, "Spiele-Runtime (Spielmodus)", 0, 0);
        if (window == 0) {
            GLFW.glfwTerminate();
            throw new RuntimeException("Fenster konnte nicht erstellt werden.");
        }

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GLFW.glfwShowWindow(window);
        GLFW.glfwFocusWindow(window);

        // Wir nutzen den Standard-Cursor, zwingen ihn aber in die Mitte
        //GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);

        // --- DER ABSOLUTE FIX: RELATIVER MAUS-CALLBACK ---
        GLFW.glfwSetCursorPosCallback(window, (windowHandle, xpos, ypos) -> {
            int[] winW = new int[1];
            int[] winH = new int[1];
            GLFW.glfwGetWindowSize(windowHandle, winW, winH);
            double centerX = winW[0] / 2.0;
            double centerY = winH[0] / 2.0;

            if (firstMouse) {
                lastMouseX = centerX;
                lastMouseY = centerY;
                firstMouse = false;
                GLFW.glfwSetCursorPos(windowHandle, centerX, centerY);
                return;
            }

            // Berechne den echten Versatz zur Mitte
            float offsetX = (float) (xpos - centerX);
            float offsetY = (float) (centerY - ypos); // Invertiert

            if (offsetX != 0 || offsetY != 0) {
                yaw += offsetX * sensitivity;
                pitch += offsetY * sensitivity;

                if (pitch > 89.0f) pitch = 89.0f;
                if (pitch < -89.0f) pitch = -89.0f;

                // Vektor sofort updaten
                Vector3f newDirection = new Vector3f();
                newDirection.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
                newDirection.y = (float) Math.sin(Math.toRadians(pitch));
                newDirection.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
                forward.set(newDirection).normalize();

                // Teleportiere die Maus SOFORT zurück in die Mitte
                GLFW.glfwSetCursorPos(windowHandle, centerX, centerY);
            }
        });

        GL.createCapabilities();

        // Szenen-Daten laden
        Scene activeScene = null;
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Component.class, new ComponentAdapter())
                .create();
        File sceneFile = new File(projectPath + "/main_scene.json");
        if (sceneFile.exists()) {
            try (FileReader reader = new FileReader(sceneFile)) {
                activeScene = gson.fromJson(reader, Scene.class);
            } catch (Exception e) {
                System.err.println("Fehler beim Laden der Szene: " + e.getMessage());
            }
        }

        if (activeScene != null) {
            for (GameObject obj : activeScene.gameObjects) {
                if (obj.type == GameObject.ObjectType.CAMERA) {
                    cameraPos.set(obj.posX, obj.posY, obj.posZ);
                    break;
                }
            }
        }

        GameWindow gameWindow = new GameWindow();
        gameWindow.init();
        gameWindow.resize(1280, 720);

        while (!GLFW.glfwWindowShouldClose(window)) {
            GLFW.glfwPollEvents();

            // Nur noch Tastatur hier abfragen, Maus läuft vollautomatisch im Callback!
            processKeyboardInput(window);

            gameWindow.getCamera().getPosition().set(cameraPos);
            gameWindow.getCamera().getTarget().set(new Vector3f(cameraPos).add(forward));
            gameWindow.getCamera().getUp().set(up);

            if (activeScene != null) {
                List<GameObject> renderOnlyCubes = new ArrayList<>();
                for (GameObject obj : activeScene.gameObjects) {
                    if (obj.type != GameObject.ObjectType.CAMERA) {
                        renderOnlyCubes.add(obj);
                    }
                }
                gameWindow.render(false, 0, renderOnlyCubes);
            }

            GLFW.glfwSwapBuffers(window);
        }

        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    private static void processKeyboardInput(long window) {
        float currentSpeed = speed;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS) {
            currentSpeed = speed * 2.5f;
        }

        Vector3f moveForward = new Vector3f(forward.x, 0.0f, forward.z).normalize();
        Vector3f moveRight = new Vector3f(forward).cross(up).normalize();
        moveRight.y = 0.0f;
        moveRight.normalize();

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS) {
            cameraPos.add(new Vector3f(moveForward).mul(currentSpeed));
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS) {
            cameraPos.sub(new Vector3f(moveForward).mul(currentSpeed));
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS) {
            cameraPos.sub(new Vector3f(moveRight).mul(currentSpeed));
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS) {
            cameraPos.add(new Vector3f(moveRight).mul(currentSpeed));
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS) {
            cameraPos.y += currentSpeed;
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS) {
            cameraPos.y -= currentSpeed;
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
            GLFW.glfwSetWindowShouldClose(window, true);
        }
    }
}
