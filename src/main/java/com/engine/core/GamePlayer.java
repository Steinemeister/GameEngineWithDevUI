package com.engine.core;

import com.engine.scene.GameObject;
import com.engine.scene.Scene;
import com.google.gson.Gson;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import java.io.File;
import java.io.FileReader;

public class GamePlayer {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Fehler: Kein Projektpfad an den Player uebergeben!");
            System.exit(1);
        }

        String projectPath = args[0];
        System.out.println("Starte Spiele-Runtime fuer: " + projectPath);

        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("GLFW konnte nicht initialisiert werden.");
        }

        long window = GLFW.glfwCreateWindow(1280, 720, "Spiele-Runtime (Spielmodus)", 0, 0);
        if (window == 0) {
            GLFW.glfwTerminate();
            throw new RuntimeException("Fenster konnte nicht erstellt werden.");
        }

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(0);
        GL.createCapabilities();

        Scene activeScene = null;
        Gson gson = new Gson();
        File sceneFile = new File(projectPath + "/main_scene.json");

        if (sceneFile.exists()) {
            try (FileReader reader = new FileReader(sceneFile)) {
                activeScene = gson.fromJson(reader, Scene.class);
            } catch (Exception e) {
                System.err.println("Fehler beim Laden der Szene im Player: " + e.getMessage());
            }
        }

        GameWindow gameWindow = new GameWindow();
        gameWindow.init();
        gameWindow.resize(1280, 720);

        File shaderFile = new File(projectPath + "/assets/shaders/default_material.glsl");
        if (shaderFile.exists()) {
            try {
                String shaderCode = java.nio.file.Files.readString(shaderFile.toPath());
                gameWindow.compileShader(shaderCode);
            } catch (Exception e) {
                System.err.println("Standard-Shader konnte nicht geladen werden.");
            }
        }

        org.joml.Vector3f runtimeCameraPos = new org.joml.Vector3f(0.0f, 0.0f, 3.0f); // Default Fallback

        if (activeScene != null) {
            for (GameObject obj : activeScene.gameObjects) {
                if (obj.type == GameObject.ObjectType.CAMERA) {
                    // Wir haben die Kamera aus dem Editor gefunden!
                    runtimeCameraPos.set(obj.posX, obj.posY, obj.posZ);
                    System.out.println("Spiel-Kamera erfolgreich auf Position gesetzt: " + runtimeCameraPos);
                    break; // Erste gefundene Kamera nutzen
                }
            }
        }

        while (!GLFW.glfwWindowShouldClose(window)) {
            gameWindow.getCamera().getPosition().set(runtimeCameraPos);
            // Sicherstellen, dass die Kamera nach vorne auf die Mitte (0,0,0) blickt
            gameWindow.getCamera().getTarget().set(0.0f, 0.0f, 0.0f);

            if (activeScene != null) {
                // WICHTIG: Im echten Spiel rendern wir die Kamera-Kiste SELBST natürlich nicht!
                // Wir filtern die Kamera temporär aus der Renderliste für den Spieler heraus:
                java.util.List<GameObject> renderOnlyCubes = new java.util.ArrayList<>();
                for (GameObject obj : activeScene.gameObjects) {
                    if (obj.type != GameObject.ObjectType.CAMERA) {
                        renderOnlyCubes.add(obj);
                    }
                }

                gameWindow.render(false, renderOnlyCubes);
            }

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }

        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }
}
