package com.engine.project;

import com.engine.core.Main;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import imgui.ImGui;
import imgui.type.ImString;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProjectLauncher {
    private final Main engine;
    private final String rootProjectsPath = "projects"; // Globaler Projektordner
    private final List<String> foundProjects = new ArrayList<>();

    // Eingabepuffer für das Erstellen eines neuen Projekts
    private final ImString newProjectName = new ImString("", 50);
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ProjectLauncher(Main engine) {
        this.engine = engine;
        scanForProjects();
    }

    /**
     * Scannt den Ordner 'projects/' nach Unterordnern, die eine project.json enthalten.
     */
    public void scanForProjects() {
        foundProjects.clear();
        File rootDir = new File(rootProjectsPath);
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }

        File[] subDirs = rootDir.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File dir : subDirs) {
                File configFile = new File(dir, "project.json");
                if (configFile.exists()) {
                    foundProjects.add(dir.getName());
                }
            }
        }
    }

    public void drawLauncherUI() {
        // Wir fixieren das Launcher-Fenster zentriert auf dem Bildschirm
        ImGui.setNextWindowPos(450, 200, imgui.flag.ImGuiCond.Appearing);
        ImGui.setNextWindowSize(500, 400, imgui.flag.ImGuiCond.Appearing);

        int flags = imgui.flag.ImGuiWindowFlags.NoCollapse | imgui.flag.ImGuiWindowFlags.NoResize;
        ImGui.begin("Java Game Engine - Project Manager", flags);

        ImGui.text("Willkommen! Erstellen Sie ein neues Projekt oder laden Sie ein bestehendes.");
        ImGui.separator();

        // --- BEREICH 1: NEUES PROJEKT ERSTELLEN ---
        ImGui.textColored(0.4f, 0.8f, 1.0f, 1.0f, "Neues Projekt anlegen");
        ImGui.inputText("Projektname", newProjectName);
        ImGui.sameLine();
        if (ImGui.button("Erstellen", 100, 23)) {
            String name = newProjectName.get().trim();
            if (!name.isEmpty()) {
                createNewProject(name);
                newProjectName.set(""); // Eingabe zurücksetzen
                scanForProjects();      // Liste aktualisieren
            }
        }

        ImGui.separator();

        // --- BEREICH 2: BESTEHENDE PROJEKTE LADEN ---
        ImGui.textColored(0.4f, 1.0f, 0.4f, 1.0f, "Vorhandene Projekte:");
        ImGui.beginChild("ProjectList", 0, 200, true);

        if (foundProjects.isEmpty()) {
            ImGui.textDisabled("Keine Projekte gefunden. Erstellen Sie eins von oben.");
        } else {
            for (String projectName : foundProjects) {
                if (ImGui.selectable("[=] " + projectName)) {
                    loadProject(projectName);
                }
            }
        }

        ImGui.endChild();
        ImGui.end();
    }

    private void createNewProject(String name) {
        String projectDirPath = rootProjectsPath + "/" + name;
        File projectDir = new File(projectDirPath);
        File shadersDir = new File(projectDir, "assets/shaders");

        // 1. Ordnerstruktur anlegen
        shadersDir.mkdirs();

        // 2. project.json generieren
        File configFile = new File(projectDir, "project.json");
        ProjectConfig config = new ProjectConfig(name);
        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            System.err.println("Fehler beim Erstellen der project.json: " + e.getMessage());
        }

        // 3. Einen Standard-Shader in den neuen Projektordner legen, damit es nicht leer ist
        File defaultShader = new File(shadersDir, "default_material.glsl");
        String defaultCode = """
            #version 330 core
            in vec3 vertexColor;
            out vec4 FragColor;
            uniform float uColorPulse;
            void main() {
                FragColor = vec4(vertexColor * uColorPulse, 1.0);
            }
        """;
        try (FileWriter writer = new FileWriter(defaultShader)) {
            writer.write(defaultCode);
            System.out.println("Projekt '" + name + "' erfolgreich generiert!");
        } catch (IOException e) {
            System.err.println("Fehler beim Erstellen des Standard-Shaders: " + e.getMessage());
        }
    }

    private void loadProject(String name) {
        String projectDirPath = rootProjectsPath + "/" + name;
        File configFile = new File(projectDirPath, "project.json");

        try (FileReader reader = new FileReader(configFile)) {
            ProjectConfig config = gson.fromJson(reader, ProjectConfig.class);
            // Wir sagen der Hauptklasse, welches Projekt geladen wurde und schalten in den Editor-Modus um!
            engine.onProjectLoaded(projectDirPath, config);
        } catch (IOException e) {
            System.err.println("Fehler beim Laden der project.json: " + e.getMessage());
        }
    }
}
