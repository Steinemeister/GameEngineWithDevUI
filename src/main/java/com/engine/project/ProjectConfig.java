package com.engine.project;

public class ProjectConfig {
    public String projectName;
    public String engineVersion;
    public String lastOpenedScene;

    public ProjectConfig(String projectName) {
        this.projectName = projectName;
        this.engineVersion = "1.0.0";
        this.lastOpenedScene = "main_scene.json";
    }
}
