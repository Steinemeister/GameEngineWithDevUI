package com.engine.core;

import com.engine.editor.*;
import com.engine.project.ProjectConfig;
import com.engine.project.ProjectLauncher;
import com.engine.scene.GameObject;
import com.engine.scene.Scene;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
    private final List<EditorPanel> panels = new ArrayList<>();
    private final ImString fragmentShaderInput = new ImString(4000);

    // Core Engine Komponenten
    private GameWindow gameWindow;
    private ProjectLauncher launcher;

    // Aktive Projekt-Zustände
    private String currentProjectPath = null;
    private ProjectConfig activeProjectConfig = null;
    private Scene activeScene = null; // Die aktuell geladene Szene

    private GameObject selectedObject = null;


    private ShaderEditorPanel shaderEditorPanel;
    private boolean projectNeedsClose = false;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    protected void configure(Configuration config) {
        config.setTitle("Java Game Engine IDE - Architecture 2.0");
        config.setWidth(1400);
        config.setHeight(800);
    }

    @Override
    protected void init(Configuration config) {
        super.init(config);

        // Das ausgelagerte Rendering initialisieren
        gameWindow = new GameWindow();
        gameWindow.init();

        // Launcher für den Start initialisieren
        launcher = new ProjectLauncher(this);
    }

    public void onProjectLoaded(String path, ProjectConfig config) {
        this.currentProjectPath = path;
        this.activeProjectConfig = config;

        // PRÜFUNG: Gibt es bereits eine gespeicherte Szene auf der Festplatte?
        java.io.File sceneFile = new java.io.File(path + "/" + config.lastOpenedScene);
        if (sceneFile.exists()) {
            try (java.io.FileReader reader = new java.io.FileReader(sceneFile)) {
                this.activeScene = gson.fromJson(reader, Scene.class);
                System.out.println("Szene erfolgreich von Festplatte geladen!");
            } catch (java.io.IOException e) {
                System.err.println("Fehler beim Laden der Szene: " + e.getMessage());
                this.activeScene = new Scene(config.lastOpenedScene);
            }
        } else {
            // Wenn nein: Erstelle eine brandneue, leere Szene
            this.activeScene = new Scene(config.lastOpenedScene);
        }

        // Panels aufbauen
        panels.clear();
        this.shaderEditorPanel = new ShaderEditorPanel(this, fragmentShaderInput);
        panels.add(new MenuBarPanel(this));
        panels.add(new ViewportPanel(this));
        panels.add(shaderEditorPanel);
        panels.add(new FileBrowserPanel(this));
        panels.add(new HierarchyPanel(this));
        panels.add(new InspectorPanel(this));
    }

    public void closeCurrentProject() {
        this.projectNeedsClose = true;
    }

    @Override
    public void process() {
        if (activeProjectConfig == null) {
            launcher.drawLauncherUI();
        } else {
            // 1. Rendering an die GameWindow-Klasse delegieren
            gameWindow.render(true, activeScene.gameObjects); // In das Framebuffer-Objekt zeichnen

            // 2. Editor-Panels zeichnen
            for (EditorPanel panel : panels) {
                panel.updateAndRender();
            }

            // Sicherer Projektaustritt nach der Schleife
            if (projectNeedsClose) {
                executeActualProjectClose();
                projectNeedsClose = false;
            }
        }
    }

    private void executeActualProjectClose() {
        if (currentProjectPath != null && activeScene != null) {
            String scenePath = currentProjectPath + "/" + activeProjectConfig.lastOpenedScene;
            try (java.io.FileWriter writer = new java.io.FileWriter(scenePath)) {
                gson.toJson(activeScene, writer);
                System.out.println("Szene erfolgreich gespeichert unter: " + scenePath);
            } catch (java.io.IOException e) {
                System.err.println("Fehler beim automatischen Speichern der Szene: " + e.getMessage());
            }
        }

        this.currentProjectPath = null;
        this.activeProjectConfig = null;
        this.activeScene = null;
        this.shaderEditorPanel = null;
        this.panels.clear();
        this.launcher.scanForProjects();
    }

    public void runGameRuntime() {
        if (currentProjectPath == null || activeScene == null) return;

        String scenePath = currentProjectPath + "/" + activeProjectConfig.lastOpenedScene;
        try (java.io.FileWriter writer = new java.io.FileWriter(scenePath)) {
            gson.toJson(activeScene, writer);
            System.out.println("Szene vor Spielstart zwischengespeichert.");
        } catch (java.io.IOException e) {
            System.err.println("Fehler beim Sichern vor Spielstart: " + e.getMessage());
            return;
        }

        try {
            String javaHome = System.getProperty("java.home");
            String javaBin = javaHome + java.io.File.separator + "bin" + java.io.File.separator + "java";

            String classpath = System.getProperty("java.class.path");

            ProcessBuilder builder = new ProcessBuilder(
                    javaBin,
                    "-cp", classpath,
                    "com.engine.core.GamePlayer",
                    currentProjectPath
            );

            builder.inheritIO();
            builder.start();
            System.out.println("Spiele-Runtime erfolgreich im Hintergrund abgefeuert!");

        } catch (java.io.IOException e) {
            System.err.println("Fehler beim Starten der Spiele-Runtime: " + e.getMessage());
        }
    }

    public void compileShader(String code) { gameWindow.compileShader(code); }
    public int getTextureId() { return gameWindow.getTextureId(); }
    public int getViewportWidth() { return gameWindow.getWidth(); }
    public int getViewportHeight() { return gameWindow.getHeight(); }
    public void handleViewportResize(int w, int h) { gameWindow.resize(w, h); }
    public Shader getCustomShader() { return gameWindow.getCustomShader(); }

    public String getCurrentProjectPath() { return currentProjectPath; }
    public ImString getFragmentShaderInput() { return fragmentShaderInput; }
    public ShaderEditorPanel getShaderEditorPanel() { return shaderEditorPanel; }
    public Scene getActiveScene() { return activeScene; }

    public GameObject getSelectedObject() { return selectedObject; }
    public void setSelectedObject(GameObject obj) { this.selectedObject = obj; }
    public GameWindow getGameWindow() { return gameWindow; }

    public static void main(String[] args) {
        launch(new Main());
    }
}
