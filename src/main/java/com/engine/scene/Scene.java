package com.engine.scene;

import java.util.ArrayList;
import java.util.List;

public class Scene {
    public String sceneName;
    public List<GameObject> gameObjects = new ArrayList<>();

    public Scene(String sceneName) {
        this.sceneName = sceneName;
    }
}
