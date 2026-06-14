    #version 330 core
    layout (location = 0) out vec3 gPosition;
    layout (location = 1) out vec3 gNormal;
    layout (location = 2) out vec4 gAlbedo;
    in vec3 FragPos;
    in vec3 Normal;
    void main() {
        gPosition = FragPos;
        gNormal = normalize(Normal);
        vec3 blendWeights = abs(gNormal);
        blendWeights = blendWeights / (blendWeights.x + blendWeights.y + blendWeights.z);
        vec3 colorX = vec3(0.5, 0.4, 0.3);
        vec3 colorY = vec3(0.2, 0.6, 0.2);
        vec3 colorZ = vec3(0.4, 0.4, 0.4);
        vec3 finalAlbedo = colorX * blendWeights.x + colorY * blendWeights.y + colorZ * blendWeights.z;
        gAlbedo = vec4(finalAlbedo, 1.0);
    }
