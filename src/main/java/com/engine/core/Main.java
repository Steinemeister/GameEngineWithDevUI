package com.engine.core;

import com.engine.editor.*;
import com.engine.project.ProjectConfig;
import com.engine.project.ProjectLauncher;
import com.engine.scene.Component;
import com.engine.scene.ComponentAdapter;
import com.engine.scene.GameObject;
import com.engine.scene.Scene;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import imgui.app.Application;
import imgui.app.Configuration;
import imgui.type.ImString;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL30.*;

public class Main extends Application {
    // Liste aller aktiven Benutzeroberflächen-Fenster (Editor-Panels)
    private final List<EditorPanel> panels = new ArrayList<>();

    // Globaler Text-Speicher für den Shader-Editor
    private final ImString fragmentShaderInput = new ImString(4000);

    // Core-Systeme der Grafik und Projekt-Auswahl
    private GameWindow gameWindow;
    private ProjectLauncher launcher;

    // Speicher für IDs des Editor-Framebuffer-Objekts (IDE-Viewport)
    private int fboId, textureId, rboId;
    private int viewportWidth = 800;
    private int viewportHeight = 600;

    // Aktive Pfade, Szenen und Auswahlen
    private String currentProjectPath = null;
    private ProjectConfig activeProjectConfig = null;
    private Scene activeScene = null;
    private GameObject selectedObject = null;

    // Direkt-Referenz auf das Shader-Fenster zum Aktualisieren von Pfaden
    private ShaderEditorPanel shaderEditorPanel;

    // Sicherheits-Flags für stabilen Frame-Wechsel
    private boolean projectNeedsClose = false;
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Component.class, new ComponentAdapter())
            .setPrettyPrinting()
            .create();

    @Override
    protected void configure(Configuration config) {
        config.setTitle("Java Game Engine IDE - Architecture 2.0");
        config.setWidth(1400);
        config.setHeight(800);
    }

    @Override
    protected void init(Configuration config) {
        super.init(config);

        // Erstmaliges Aktivieren der nackten OpenGL-Schnittstellen
        org.lwjgl.opengl.GL.createCapabilities();

        // 1. Initialisierung der Render-Pipeline
        gameWindow = new GameWindow();
        gameWindow.init();

        // 2. Initialisierung des Projekt-Hubs (Launcher)
        launcher = new ProjectLauncher(this);

        // 3. Eigenes FBO für den Editor-Bildschirm vorbereiten
        setupFramebuffer();
    }

    /**
     * Wird vom ProjectLauncher aufgerufen, sobald ein valides Projekt ausgewählt wurde.
     */
    public void onProjectLoaded(String path, ProjectConfig config) {
        this.currentProjectPath = path;
        this.activeProjectConfig = config;

        // Versuche die letzte geöffnete Szene von der Festplatte zu laden
        File sceneFile = new File(path + "/" + config.lastOpenedScene);
        if (sceneFile.exists()) {
            try (FileReader reader = new FileReader(sceneFile)) {
                this.activeScene = gson.fromJson(reader, Scene.class);
                System.out.println("Szene erfolgreich geladen: " + config.lastOpenedScene);
            } catch (IOException e) {
                System.err.println("Fehler beim Laden der Szene: " + e.getMessage());
                this.activeScene = new Scene(config.lastOpenedScene);
            }
        } else {
            this.activeScene = new Scene(config.lastOpenedScene);
        }

        // Editor-Oberfläche dynamisch aufbauen und Instanzen zuweisen
        panels.clear();
        this.shaderEditorPanel = new ShaderEditorPanel(this, fragmentShaderInput);

        panels.add(new MenuBarPanel(this));
        panels.add(new ViewportPanel(this));
        panels.add(shaderEditorPanel);
        panels.add(new FileBrowserPanel(this));
        panels.add(new HierarchyPanel(this));
        panels.add(new InspectorPanel(this));
    }

    /**
     * Vormerken, dass das Projekt am Ende des Frames geschlossen werden soll.
     * Verhindert die ConcurrentModificationException.
     */
    public void closeCurrentProject() {
        this.projectNeedsClose = true;
    }

    /**
     * Speichert die aktuelle Szene und startet die nackte Spiele-Runtime
     * als unabhängigen Betriebssystem-Prozess.
     */
    public void runGameRuntime() {
        if (currentProjectPath == null || activeScene == null) return;

        // 1. Szene vor dem Start zwingend auf der Festplatte sichern
        String scenePath = currentProjectPath + "/" + activeProjectConfig.lastOpenedScene;
        try (java.io.FileWriter writer = new java.io.FileWriter(scenePath)) {
            gson.toJson(activeScene, writer);
            System.out.println("Szene vor Spielstart zwischengespeichert.");
        } catch (java.io.IOException e) {
            System.err.println("Fehler beim Sichern vor Spielstart: " + e.getMessage());
            return;
        }

        // 2. Den absolut isolierten Gradle-Prozess aufbauen
        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            String gradleCmd = isWindows ? "gradlew.bat" : "./gradlew";

            // HIER IST DER FIX:
            // -PmainClass zwingt Gradle dazu, die GamePlayer-Klasse auszuführen.
            // --args übergibt den Pfad zu deinem aktuellen Projektordner.
            ProcessBuilder pb = new ProcessBuilder(
                    gradleCmd,
                    "run",
                    "-PmainClass=com.engine.core.GamePlayer",
                    "--args=" + currentProjectPath
            );

            // Leitet alle Log- und Fehler-Ausgaben des Players in diese IDE-Konsole um
            pb.inheritIO();

            pb.start();
            System.out.println("Spiele-Runtime isoliert und zielgerichtet via Gradle gestartet!");

        } catch (java.io.IOException e) {
            System.err.println("Fehler beim Starten des Gradle-Prozesses: " + e.getMessage());
        }
    }

    @Override
    public void process() {
        if (activeProjectConfig == null) {
            // Zustand A: Kein Projekt offen -> Zeige den Launcher
            launcher.drawLauncherUI();
        } else {
            // Zustand B: Ein Projekt ist geladen -> Rendere das Spiel in das FBO
            // Wir übergeben das Flag, die ID unserer Viewport-Textur (fboId) und die Objektliste
            gameWindow.render(true, this.fboId, activeScene.gameObjects);

            // Editor-Panels zeichnen
            for (EditorPanel panel : panels) {
                panel.updateAndRender();
            }

            // Sicherer Projektaustritt nach Beendigung aller Zeichen-Schleifen
            if (projectNeedsClose) {
                executeActualProjectClose();
                projectNeedsClose = false;
            }
        }
    }

    /**
     * Führt das tatsächliche Schließen außerhalb der Render-Schleife aus.
     */
    private void executeActualProjectClose() {
        // Zuerst aktuelle Szene sichern
        if (currentProjectPath != null && activeScene != null) {
            String scenePath = currentProjectPath + "/" + activeProjectConfig.lastOpenedScene;
            try (FileWriter writer = new FileWriter(scenePath)) {
                gson.toJson(activeScene, writer);
                System.out.println("Szene erfolgreich automatisch gespeichert.");
            } catch (IOException e) {
                System.err.println("Fehler beim automatischen Speichern: " + e.getMessage());
            }
        }

        // Datenstrukturen leeren
        this.currentProjectPath = null;
        this.activeProjectConfig = null;
        this.activeScene = null;
        this.selectedObject = null;
        this.shaderEditorPanel = null;

        // Panels sicher freigeben
        this.panels.clear();

        // Launcher neu aufbauen
        this.launcher.scanForProjects();
        System.out.println("Projekt sicher geschlossen. Zurueck zum Launcher.");
    }

    // --- WEITERLEITUNGS-METHODEN AN DAS GAMEWINDOW ---
    public void compileShader(String code) {
        // Wenn ein Objekt ausgewählt ist, aktualisieren wir dessen Shader live
        if (selectedObject != null && selectedObject.material != null) {
            // Wir holen uns den Vertex-Shader-Quellcode aus dem GameWindow
            String vertexCode = gameWindow.getCachedVertexShaderSource();
            selectedObject.material.compile(vertexCode);
            System.out.println("Material-Shader fuer " + selectedObject.name + " live aktualisiert!");
        }
    }
    public void handleViewportResize(int w, int h) {
        gameWindow.resize(w, h);
        resizeEditorTexture(w, h);
    }

    // --- PRIVATE UTILITIES ---
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
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, viewportWidth, viewportHeight);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rboId);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private void resizeEditorTexture(int w, int h) {
        if (w > 0 && h > 0 && (w != viewportWidth || h != viewportHeight)) {
            this.viewportWidth = w;
            this.viewportHeight = h;
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, viewportWidth, viewportHeight, 0, GL_RGB, GL_UNSIGNED_BYTE, 0);
            glBindRenderbuffer(GL_RENDERBUFFER, rboId);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, viewportWidth, viewportHeight);
        }
    }

    // --- GETTER & SETTER ---
    public String getCurrentProjectPath() { return currentProjectPath; }
    public ImString getFragmentShaderInput() { return fragmentShaderInput; }
    public ShaderEditorPanel getShaderEditorPanel() { return shaderEditorPanel; }
    public Scene getActiveScene() { return activeScene; }
    public GameObject getSelectedObject() { return selectedObject; }
    public void setSelectedObject(GameObject obj) { this.selectedObject = obj; }
    public GameWindow getGameWindow() { return gameWindow; }
    public int getTextureId() { return textureId; }
    public int getViewportWidth() { return viewportWidth; }
    public int getViewportHeight() { return viewportHeight; }

    public static void main(String[] args) {
        launch(new Main());
    }
}
