package com.engine.scene;

import com.engine.core.Shader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Material {
    private final String fragmentShaderPath;
    private Shader shaderProgram;
    private boolean isCompiled = false;

    public Material(String fragmentShaderPath) {
        this.fragmentShaderPath = fragmentShaderPath;
    }

    public void compile(String vertexShaderSource) {
        try {
            // Liest den individuellen Shader-Code live von der Festplatte ab
            String fragmentShaderSource = Files.readString(Paths.get(fragmentShaderPath));
            this.shaderProgram = new Shader(vertexShaderSource, fragmentShaderSource);
            this.isCompiled = true;
        } catch (IOException e) {
            System.err.println("Fehler beim Laden des Material-Shaders (" + fragmentShaderPath + "): " + e.getMessage());
            // Fallback: Einen einfachen, fehlerfreien Shader erstellen, falls die Datei korrupt ist
            this.shaderProgram = new Shader(vertexShaderSource, "#version 330 core\nout vec4 f; void main(){f=vec4(1,0,1,1);}");
        }
    }

    public void bind() {
        if (isCompiled && shaderProgram != null) {
            shaderProgram.bind();
        }
    }

    public void unbind() {
        if (shaderProgram != null) shaderProgram.unbind();
    }

    public Shader getShaderProgram() { return shaderProgram; }
    public String getFragmentShaderPath() { return fragmentShaderPath; }
}
