#version 330 core
layout (location = 0) out vec3 gPosition;
layout (location = 1) out vec3 gNormal;
layout (location = 2) out vec4 gAlbedo;

in vec3 FragPos;
in vec3 Normal;

void main() {
    gPosition = FragPos;
    gNormal = normalize(Normal);

    // --- TRI-PLANAR MAPPING LOGIK (VEREINFACHT FÜR FARBEN) ---
    // Berechne die Gewichtung anhand der Steilheit der Oberfläche
    vec3 blendWeights = abs(gNormal);
    blendWeights = blendWeights / (blendWeights.x + blendWeights.y + blendWeights.z);

    // Drei unterschiedliche Textur-Farben je nach Ausrichtung der Wand
    vec3 colorX = vec3(0.5, 0.4, 0.3); // Klippe (Fels-Braun)
    vec3 colorY = vec3(0.2, 0.6, 0.2); // Flachland (Gras-Grün)
    vec3 colorZ = vec3(0.4, 0.4, 0.4); // Stein-Grau

    // Die finale Pixelfarbe wird nahtlos zusammengemischt
    vec3 finalAlbedo = colorX * blendWeights.x + colorY * blendWeights.y + colorZ * blendWeights.z;

    gAlbedo = vec4(finalAlbedo, 1.0);
}