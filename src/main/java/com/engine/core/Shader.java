package com.engine.core;

import static org.lwjgl.opengl.GL20.*;

public class Shader {
    private int programId = 0;
    private String errorMessage = null;

    public Shader(String vertexCode, String fragmentCode) {
        compileAndLink(vertexCode, fragmentCode);
    }

    public void compileAndLink(String vertexCode, String fragmentCode) {
        errorMessage = null;

        // 1. Vertex Shader
        int vertexId = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexId, vertexCode);
        glCompileShader(vertexId);
        String vError = checkShaderCompileStatus(vertexId, "Vertex");
        if (vError != null) {
            this.errorMessage = vError;
            glDeleteShader(vertexId);
            return;
        }

        // 2. Fragment Shader
        int fragmentId = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentId, fragmentCode);
        glCompileShader(fragmentId);
        String fError = checkShaderCompileStatus(fragmentId, "Fragment");
        if (fError != null) {
            this.errorMessage = fError;
            glDeleteShader(vertexId);
            glDeleteShader(fragmentId);
            return;
        }

        // 3. Linken
        int newProgramId = glCreateProgram();
        glAttachShader(newProgramId, vertexId);
        glAttachShader(newProgramId, fragmentId);
        glLinkProgram(newProgramId);

        if (glGetProgrami(newProgramId, GL_LINK_STATUS) == GL_FALSE) {
            this.errorMessage = "Link-Fehler: " + glGetProgramInfoLog(newProgramId, 1024);
            glDeleteShader(vertexId);
            glDeleteShader(fragmentId);
            glDeleteProgram(newProgramId);
            return;
        }

        // Altes Programm löschen, falls vorhanden, und neues zuweisen
        if (this.programId != 0) {
            glDeleteProgram(this.programId);
        }
        this.programId = newProgramId;

        glDeleteShader(vertexId);
        glDeleteShader(fragmentId);
    }

    public void bind() {
        if (programId != 0) glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void setUniform(String name, float value) {
        if (programId == 0) return;
        int location = glGetUniformLocation(programId, name);
        if (location != -1) {
            glUniform1f(location, value);
        }
    }

    public boolean hasErrors() {
        return errorMessage != null;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    private String checkShaderCompileStatus(int shaderId, String type) {
        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            return type + "-Shader Fehler:\n" + glGetShaderInfoLog(shaderId, 1024);
        }
        return null;
    }

    public int getProgramId() {
        return programId;
    }
}
