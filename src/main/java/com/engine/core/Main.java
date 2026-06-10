package com.engine.core;

import com.engine.editor.*;
import com.engine.project.ProjectConfig;
import com.engine.project.ProjectLauncher;
import com.engine.scene.Scene;
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

    private ShaderEditorPanel shaderEditorPanel;
    private boolean projectNeedsClose = false;

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

        // Eine leere Start-Szene für das geladene Projekt erstellen
        this.activeScene = new Scene(config.lastOpenedScene);

        // Panels aufbauen
        panels.clear();
        this.shaderEditorPanel = new ShaderEditorPanel(this, fragmentShaderInput);

        panels.add(new MenuBarPanel(this));
        panels.add(new ViewportPanel(this));
        panels.add(shaderEditorPanel);
        panels.add(new FileBrowserPanel(this));
        panels.add(new HierarchyPanel(this)); // <-- DAS NEUE HIERARCHY PANEL
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
            gameWindow.render(true); // In das Framebuffer-Objekt zeichnen

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
        this.currentProjectPath = null;
        this.activeProjectConfig = null;
        this.activeScene = null;
        this.shaderEditorPanel = null;
        this.panels.clear();
        this.launcher.scanForProjects();
    }

    // --- LEICHTE ANPASSUNGEN DER WEITERLEITUNGS-METHODEN ---
    public void compileShader(String code) { gameWindow.compileShader(code); }
    public int getTextureId() { return gameWindow.getTextureId(); }
    public int getViewportWidth() { return gameWindow.getWidth(); }
    public int getViewportHeight() { return gameWindow.getHeight(); }
    public void handleViewportResize(int w, int h) { gameWindow.resize(w, h); }
    public Shader getCustomShader() { return gameWindow.getCustomShader(); }

    // --- GETTER ---
    public String getCurrentProjectPath() { return currentProjectPath; }
    public ImString getFragmentShaderInput() { return fragmentShaderInput; }
    public ShaderEditorPanel getShaderEditorPanel() { return shaderEditorPanel; }
    public Scene getActiveScene() { return activeScene; }

    public static void main(String[] args) {
        launch(new Main());
    }
}
